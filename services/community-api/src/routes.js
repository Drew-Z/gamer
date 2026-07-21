import {
  users
} from "../../../packages/community-contracts/src/index.js";
import { validatePetPackageBundle } from "../../../packages/pet-package-spec/src/index.js";
import { createFantasyPetRuleImportSummary } from "../../pet-generator/src/adapter.js";
import {
  resolveFantasyPetRuleState,
  StateSourceError
} from "../../pet-generator/src/state-source.js";
import { releaseCommit } from "./release.js";
import { createSlaConfig } from "./sla.js";
import { createCommunityStore } from "./store.js";
import { handleMetricsRequest } from "./metrics.js";
import { parsePaginationFromUrl } from "./pagination.js";

const json = (status, body) => ({ status, body });
const defaultStore = createCommunityStore();
const isPromiseLike = (value) =>
  value !== null && typeof value === "object" && typeof value.then === "function";
const withResult = (value, mapper) =>
  isPromiseLike(value) ? value.then(mapper) : mapper(value);

export function handleCommunityRequest(method, requestUrl, options = {}) {
  const url = new URL(requestUrl, "http://localhost");
  const store = options.store ?? defaultStore;
  const body = options.body ?? {};
  const env = options.env ?? process.env;
  const currentUserId = body.userId ?? users[0].id;

  if (method === "GET" && url.pathname === "/readyz") {
    return withResult(store.getFeed({ limit: 1 }), () =>
      json(200, {
        ok: true,
        service: "community-api",
        storage: String(env.DATABASE_URL ?? "").trim() ? "postgres" : "local"
      })
    );
  }

  if (method === "GET" && url.pathname === "/health") {
    return json(200, {
      ok: true,
      service: "community-api",
      release: {
        commit: releaseCommit(env)
      }
    });
  }

  if (method === "GET" && url.pathname === "/v1/sla") {
    return json(200, createSlaConfig(env));
  }

  if (method === "GET" && url.pathname === "/metrics") {
    // Metrics endpoint for Prometheus scraping
    // No auth required for monitoring
    return { 
      status: 200, 
      body: null, 
      isMetrics: true 
    };
  }

  if (method === "GET" && url.pathname === "/v1/feed") {
    return withResult(store.getFeed(parsePaginationFromUrl(url)), (feed) => json(200, feed));
  }

  if (method === "GET" && url.pathname === "/v1/community-home") {
    const date = url.searchParams.get("date") ?? new Date().toISOString().slice(0, 10);
    return withResult(
      store.getCommunityHome(currentUserId, date),
      (home) => json(200, home)
    );
  }

  if (method === "GET" && url.pathname === "/v1/pets/approved") {
    return withResult(store.listApprovedPets(parsePaginationFromUrl(url)), (pets) => json(200, pets));
  }

  if (
    method === "GET" &&
    url.pathname.startsWith("/v1/pets/approved/") &&
    url.pathname.endsWith("/package")
  ) {
    const petId = decodeURIComponent(
      url.pathname.slice("/v1/pets/approved/".length, -"/package".length)
    );
    return withResult(store.getApprovedPetPackage(petId), (descriptor) => {
      if (!descriptor) {
        return json(404, {
          error: "approved_pet_package_not_found",
          petId
        });
      }

      return json(200, descriptor);
    });
  }

  if (method === "GET" && url.pathname === "/v1/wallet/me") {
    return withResult(store.getWallet(currentUserId), (wallet) => json(200, wallet));
  }

  if (method === "POST" && url.pathname === "/v1/check-in") {
    const date = body.date ?? new Date().toISOString().slice(0, 10);
    return withResult(
      store.claimDailyCheckIn(currentUserId, date),
      (checkIn) => json(200, checkIn)
    );
  }

  if (method === "GET" && url.pathname === "/v1/submissions") {
    return withResult(
      store.listSubmissions(),
      (submissionsResponse) => json(200, submissionsResponse)
    );
  }

  if (method === "GET" && url.pathname.startsWith("/v1/submissions/")) {
    const submissionId = decodeURIComponent(
      url.pathname.slice("/v1/submissions/".length)
    );
    return withResult(store.getSubmission(submissionId), (submission) => {
      if (!submission) {
        return json(404, {
          error: "submission_not_found",
          submissionId
        });
      }

      return json(200, submission);
    });
  }

  if (method === "POST" && url.pathname === "/v1/pet-package-bundles/validate") {
    const validation = validatePetPackageBundle(body.bundle);

    if (!validation.ok) {
      return json(400, {
        error: "invalid_pet_package_bundle",
        validation
      });
    }

    return json(200, {
      validation
    });
  }

  if (method === "GET" && url.pathname.startsWith("/v1/score-reports/")) {
    const scoreReportId = decodeURIComponent(
      url.pathname.slice("/v1/score-reports/".length)
    );
    return withResult(store.getScoreReport(scoreReportId), (report) => {
      if (!report) {
        return json(404, {
          error: "score_report_not_found",
          scoreReportId
        });
      }

      return json(200, report);
    });
  }

  if (method === "GET" && url.pathname === "/v1/import-drafts") {
    return withResult(
      store.listImportDrafts(currentUserId),
      (drafts) => json(200, drafts)
    );
  }

  if (method === "POST" && url.pathname === "/v1/import-drafts/from-pet-package-bundle") {
    const validation = validatePetPackageBundle(body.bundle);

    if (!validation.ok) {
      return json(400, {
        error: "invalid_pet_package_bundle",
        validation
      });
    }

    const draft = store.createImportDraftFromPetPackageBundle({
      userId: currentUserId,
      bundle: body.bundle
    });

    return withResult(draft, (result) => {
      if (result.error === "bundle_owner_mismatch") {
        return json(403, result);
      }

      if (result.error === "duplicate_import_draft") {
        return json(409, result);
      }

      return json(201, result);
    });
  }

  if (method === "POST" && url.pathname === "/v1/import-drafts/from-fantasy-pet-package") {
    const draft = store.createImportDraftFromFantasyPetPackage({
      userId: currentUserId,
      packageManifest: body.packageManifest,
      packageFileName: body.packageFileName,
      packageByteCount: body.packageByteCount,
      targetDownloadId: body.targetDownloadId,
      ownershipClaimId: body.ownershipClaimId
    });

    return withResult(draft, (result) => {
      if (result.error === "invalid_fantasy_pet_package") {
        return json(400, result);
      }

      if (result.error === "duplicate_import_draft") {
        return json(409, result);
      }

      return json(201, result);
    });
  }

  if (method === "POST" && url.pathname === "/v1/import-drafts") {
    const draft = store.createImportDraft({
      userId: currentUserId,
      readiness: body.readiness,
      importSummary: body.importSummary,
      ownershipClaimId: body.ownershipClaimId,
      scoreReportId: body.scoreReportId
    });

    return withResult(draft, (result) => json(201, result));
  }

  if (method === "POST" && url.pathname === "/v1/import-drafts/from-fantasy-pet-rule") {
    return resolveFantasyPetRuleState(body, options)
      .then((state) => {
        const result = createFantasyPetRuleImportSummary(state);
        return store.createImportDraft({
          userId: currentUserId,
          readiness: result.readiness,
          importSummary: result.importSummary,
          ownershipClaimId: body.ownershipClaimId,
          scoreReportId: body.scoreReportId
        });
      })
      .then((draft) => json(201, draft))
      .catch((error) => {
        if (error instanceof StateSourceError) {
          return json(error.status, {
            error: error.code,
            message: error.message
          });
        }

        throw error;
      })
  }

  if (method === "POST" && url.pathname === "/v1/import-drafts/submit") {
    const result = store.submitImportDraft({
      draftId: body.draftId,
      userId: currentUserId
    });

    return withResult(result, (submitResult) => {
      if (submitResult.error === "draft_not_found") {
        return json(404, submitResult);
      }

      if (submitResult.error === "draft_not_ready") {
        return json(409, submitResult);
      }

      return json(201, submitResult);
    });
  }

  if (method === "POST" && url.pathname === "/v1/submissions") {
    const submission = store.createSubmission({
      petId: body.petId,
      userId: currentUserId,
      ownershipClaimId: body.ownershipClaimId,
      scoreReportId: body.scoreReportId
    });
    return withResult(submission, (result) => json(201, result));
  }

  if (method === "POST" && url.pathname === "/v1/admin/reviews") {
    const review = store.reviewSubmission({
      submissionId: body.submissionId,
      status: body.status,
      reviewer: body.reviewer ?? "admin-local",
      rewardAmount: Object.hasOwn(body, "rewardAmount")
        ? Number(body.rewardAmount)
        : undefined
    });

    return withResult(review, (result) => {
      if (result.error) {
        if (result.error === "invalid_review_status") {
          return json(400, result);
        }

        if (result.error === "invalid_reward_amount") {
          return json(400, result);
        }

        if (result.error === "submission_terminal") {
          return json(409, result);
        }

        return json(404, result);
      }

      return json(200, result);
    });
  }

  if (method === "GET" && url.pathname === "/v1/admin/review-queue") {
    return withResult(
      store.listAdminReviewQueue(),
      (queue) => json(200, queue)
    );
  }

  if (method === "GET" && url.pathname === "/v1/me") {
    return withResult(store.getMe(), (me) => json(200, me));
  }

  return json(404, {
    error: "not_found",
    method,
    path: url.pathname
  });
}
