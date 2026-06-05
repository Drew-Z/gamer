import assert from "node:assert/strict";
import test from "node:test";
import { handleCommunityRequest } from "./routes.js";
import { createCommunityStore } from "./store.js";

test("health route reports service status", () => {
  const response = handleCommunityRequest("GET", "/health");

  assert.equal(response.status, 200);
  assert.equal(response.body.ok, true);
  assert.equal(response.body.service, "community-api");
});

test("feed route returns fixture posts", () => {
  const response = handleCommunityRequest("GET", "/v1/feed", {
    store: createCommunityStore()
  });

  assert.equal(response.status, 200);
  assert.ok(response.body.items.length >= 2);
  assert.ok(response.body.items.every((post) => post.petId));
});

test("check-in route accepts date body and updates wallet", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest("POST", "/v1/check-in", {
    store,
    body: {
      date: "2026-06-05"
    }
  });

  assert.equal(response.status, 200);
  assert.equal(response.body.checkIn.date, "2026-06-05");
  assert.equal(response.body.wallet.balance, 100);
});

test("submission route creates pending submission", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest("POST", "/v1/submissions", {
    store,
    body: {
      petId: "pet-new-001",
      ownershipClaimId: "claim-pet-new-001",
      scoreReportId: "score-pet-new-001"
    }
  });

  assert.equal(response.status, 201);
  assert.equal(response.body.status, "pending");
  assert.equal(response.body.petId, "pet-new-001");
});

test("admin review route approves submission and updates wallet", () => {
  const store = createCommunityStore();
  const created = handleCommunityRequest("POST", "/v1/submissions", {
    store,
    body: {
      petId: "pet-new-001",
      ownershipClaimId: "claim-pet-new-001",
      scoreReportId: "score-pet-new-001"
    }
  });

  const reviewed = handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: created.body.id,
      status: "approved",
      reviewer: "admin-demo",
      rewardAmount: 55
    }
  });
  const wallet = handleCommunityRequest("GET", "/v1/wallet/me", { store });

  assert.equal(reviewed.status, 200);
  assert.equal(reviewed.body.status, "approved");
  assert.equal(reviewed.body.rewardEntry.amount, 55);
  assert.equal(wallet.body.balance, 145);
});

test("admin review route rejects terminal submissions", () => {
  const store = createCommunityStore();
  const created = handleCommunityRequest("POST", "/v1/submissions", {
    store,
    body: {
      petId: "pet-terminal-route-001",
      ownershipClaimId: "claim-terminal-route-001",
      scoreReportId: "score-terminal-route-001"
    }
  });
  handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: created.body.id,
      status: "rejected",
      reviewer: "admin-demo"
    }
  });

  const response = handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: created.body.id,
      status: "approved",
      reviewer: "admin-demo",
      rewardAmount: 40
    }
  });

  assert.equal(response.status, 409);
  assert.equal(response.body.error, "submission_terminal");
  assert.equal(response.body.status, "rejected");
});

test("approved import draft appears in feed through route flow", () => {
  const store = createCommunityStore();
  const draft = handleCommunityRequest("POST", "/v1/import-drafts", {
    store,
    body: {
      readiness: {
        status: "community-ready",
        reason: "preview accepted by user"
      },
      importSummary: {
        source: {
          petId: "pet-route-feed-001",
          baseIdentityStatus: "accepted"
        },
        review: {
          blockers: [],
          previewDecision: "keep",
          exportStatus: "ready"
        },
        assets: {
          previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/route-feed/preview.html",
          exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/route-feed/export.zip"
        }
      },
      ownershipClaimId: "claim-pet-route-feed-001"
    }
  });
  const submitted = handleCommunityRequest("POST", "/v1/import-drafts/submit", {
    store,
    body: {
      draftId: draft.body.id
    }
  });

  handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: submitted.body.submission.id,
      status: "approved",
      reviewer: "admin-demo"
    }
  });
  const feed = handleCommunityRequest("GET", "/v1/feed", { store });
  const published = feed.body.items.find((post) => post.petId === "pet-route-feed-001");

  assert.equal(draft.status, 201);
  assert.equal(submitted.status, 201);
  assert.equal(feed.status, 200);
  assert.equal(published.title, "Approved pet import: pet-route-feed-001");
});

test("fantasy pet rule bridge creates import draft from inline state", async () => {
  const store = createCommunityStore();
  const response = await handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-fantasy-pet-rule",
    {
      store,
      body: {
        ownershipClaimId: "claim-pet-bridge-001",
        state: {
          schema: "fantasy-pet.codex-state.v1",
          petId: "pet-bridge-001",
          currentStage: "preview-review",
          baseIdentity: {
            status: "accepted"
          },
          blockers: [],
          preview: {
            userDecision: "keep",
            urlOrPath: "D:/workspace4Codex/fantasy-pet-rule/runs/bridge/preview.html"
          },
          export: {
            decision: "asked",
            status: "ready",
            artifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/bridge/export.zip"
          }
        }
      }
    }
  );
  const report = store.getScoreReport(response.body.scoreReportId);

  assert.equal(response.status, 201);
  assert.equal(response.body.status, "ready");
  assert.equal(response.body.petId, "pet-bridge-001");
  assert.equal(response.body.readiness.status, "community-ready");
  assert.equal(response.body.importSummary.source.schema, "fantasy-pet.codex-state.v1");
  assert.equal(report.petId, "pet-bridge-001");
  assert.equal(report.rewardRecommendation.amount, 80);
});

test("unsupported route returns 404", () => {
  const response = handleCommunityRequest("GET", "/missing");

  assert.equal(response.status, 404);
  assert.equal(response.body.error, "not_found");
  assert.equal(response.body.path, "/missing");
});
