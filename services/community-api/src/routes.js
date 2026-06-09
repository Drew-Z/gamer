import {
  users
} from "../../../packages/community-contracts/src/index.js";
import { validatePetPackageBundle } from "../../../packages/pet-package-spec/src/index.js";
import { createFantasyPetRuleImportSummary } from "../../pet-generator/src/adapter.js";
import {
  resolveFantasyPetRuleState,
  StateSourceError
} from "../../pet-generator/src/state-source.js";
import { createCommunityStore } from "./store.js";

const json = (status, body) => ({ status, body });
const defaultStore = createCommunityStore();

export function handleCommunityRequest(method, requestUrl, options = {}) {
  const url = new URL(requestUrl, "http://localhost");
  const store = options.store ?? defaultStore;
  const body = options.body ?? {};
  const currentUserId = body.userId ?? users[0].id;

  if (method === "GET" && url.pathname === "/health") {
    return json(200, {
      ok: true,
      service: "community-api"
    });
  }

  if (method === "GET" && url.pathname === "/v1/feed") {
    return json(200, store.getFeed());
  }

  if (method === "GET" && url.pathname === "/v1/community-home") {
    const date = url.searchParams.get("date") ?? new Date().toISOString().slice(0, 10);
    return json(200, store.getCommunityHome(currentUserId, date));
  }

  if (method === "GET" && url.pathname === "/v1/pets/approved") {
    return json(200, store.listApprovedPets());
  }

  if (
    method === "GET" &&
    url.pathname.startsWith("/v1/pets/approved/") &&
    url.pathname.endsWith("/package")
  ) {
    const petId = decodeURIComponent(
      url.pathname.slice("/v1/pets/approved/".length, -"/package".length)
    );
    const descriptor = store.getApprovedPetPackage(petId);

    if (!descriptor) {
      return json(404, {
        error: "approved_pet_package_not_found",
        petId
      });
    }

    return json(200, descriptor);
  }

  if (method === "GET" && url.pathname === "/v1/wallet/me") {
    return json(200, store.getWallet(currentUserId));
  }

  if (method === "POST" && url.pathname === "/v1/check-in") {
    const date = body.date ?? new Date().toISOString().slice(0, 10);
    return json(200, store.claimDailyCheckIn(currentUserId, date));
  }

  if (method === "GET" && url.pathname === "/v1/submissions") {
    return json(200, store.listSubmissions());
  }

  if (method === "GET" && url.pathname.startsWith("/v1/submissions/")) {
    const submissionId = decodeURIComponent(
      url.pathname.slice("/v1/submissions/".length)
    );
    const submission = store.getSubmission(submissionId);

    if (!submission) {
      return json(404, {
        error: "submission_not_found",
        submissionId
      });
    }

    return json(200, submission);
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
    const report = store.getScoreReport(scoreReportId);

    if (!report) {
      return json(404, {
        error: "score_report_not_found",
        scoreReportId
      });
    }

    return json(200, report);
  }

  if (method === "GET" && url.pathname === "/v1/import-drafts") {
    return json(200, store.listImportDrafts(currentUserId));
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

    if (draft.error === "bundle_owner_mismatch") {
      return json(403, draft);
    }

    if (draft.error === "duplicate_import_draft") {
      return json(409, draft);
    }

    return json(201, draft);
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

    if (draft.error === "invalid_fantasy_pet_package") {
      return json(400, draft);
    }

    if (draft.error === "duplicate_import_draft") {
      return json(409, draft);
    }

    return json(201, draft);
  }

  if (method === "POST" && url.pathname === "/v1/import-drafts") {
    const draft = store.createImportDraft({
      userId: currentUserId,
      readiness: body.readiness,
      importSummary: body.importSummary,
      ownershipClaimId: body.ownershipClaimId,
      scoreReportId: body.scoreReportId
    });

    return json(201, draft);
  }

  if (method === "POST" && url.pathname === "/v1/import-drafts/from-fantasy-pet-rule") {
    return resolveFantasyPetRuleState(body, options)
      .then((state) => {
        const result = createFantasyPetRuleImportSummary(state);
        const draft = store.createImportDraft({
          userId: currentUserId,
          readiness: result.readiness,
          importSummary: result.importSummary,
          ownershipClaimId: body.ownershipClaimId,
          scoreReportId: body.scoreReportId
        });

        return json(201, draft);
      })
      .catch((error) => {
        if (error instanceof StateSourceError) {
          return json(error.status, {
            error: error.code,
            message: error.message
          });
        }

        throw error;
      });
  }

  if (method === "POST" && url.pathname === "/v1/import-drafts/submit") {
    const result = store.submitImportDraft({
      draftId: body.draftId,
      userId: currentUserId
    });

    if (result.error === "draft_not_found") {
      return json(404, result);
    }

    if (result.error === "draft_not_ready") {
      return json(409, result);
    }

    return json(201, result);
  }

  if (method === "POST" && url.pathname === "/v1/submissions") {
    const submission = store.createSubmission({
      petId: body.petId,
      userId: currentUserId,
      ownershipClaimId: body.ownershipClaimId,
      scoreReportId: body.scoreReportId
    });
    return json(201, submission);
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

    if (review.error) {
      if (review.error === "invalid_review_status") {
        return json(400, review);
      }

      if (review.error === "invalid_reward_amount") {
        return json(400, review);
      }

      if (review.error === "submission_terminal") {
        return json(409, review);
      }

      return json(404, review);
    }

    return json(200, review);
  }

  if (method === "GET" && url.pathname === "/v1/admin/review-queue") {
    return json(200, store.listAdminReviewQueue());
  }

  if (method === "GET" && url.pathname === "/v1/me") {
    return json(200, store.getMe());
  }

  return json(404, {
    error: "not_found",
    method,
    path: url.pathname
  });
}
