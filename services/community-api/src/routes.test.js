import assert from "node:assert/strict";
import test from "node:test";
import { validPetPackageBundle } from "../../../packages/pet-package-spec/src/index.js";
import { handleCommunityRequest } from "./routes.js";
import { createCommunityStore } from "./store.js";

const validFantasyPetPackageManifest = {
  schema: "fantasy-pet.package-manifest.v1",
  runId: "run-public-lifecycle-smoke",
  appJobId: "public-lifecycle-smoke",
  acceptedBy: "human-review",
  sourceDownloadId: "artifact-1",
  sourceTaskId: "codex-worker-task",
  files: [
    {
      kind: "candidate",
      path: "artifacts/candidates/final-preview.png"
    }
  ]
};

test("health route reports service status", () => {
  const response = handleCommunityRequest("GET", "/health", {
    env: {
      GIT_COMMIT: "abc123"
    }
  });

  assert.equal(response.status, 200);
  assert.equal(response.body.ok, true);
  assert.equal(response.body.service, "community-api");
  assert.equal(response.body.release.commit, "abc123");
});

test("feed route returns fixture posts", () => {
  const response = handleCommunityRequest("GET", "/v1/feed", {
    store: createCommunityStore()
  });

  assert.equal(response.status, 200);
  assert.ok(response.body.items.length >= 2);
  assert.ok(response.body.items.every((post) => post.petId));
});

test("community home route returns public home summary", () => {
  const store = createCommunityStore();
  store.createSubmission({
    petId: "pet-home-pending-001",
    userId: "user-demo-001",
    ownershipClaimId: "claim-home-pending-001",
    scoreReportId: "score-home-pending-001"
  });
  store.claimDailyCheckIn("user-demo-001", "2026-06-09");

  const response = handleCommunityRequest("GET", "/v1/community-home?date=2026-06-09", {
    store
  });

  assert.equal(response.status, 200);
  assert.equal(response.body.schema, "gamer.community-home.v1");
  assert.ok(response.body.feed.items.length >= 2);
  assert.equal(response.body.wallet.balance, 100);
  assert.equal(response.body.dailyCheckIn.claimed, true);
  assert.equal(response.body.submissionsSummary.pendingCount, 1);
  assert.equal(response.body.approvedPets.items.length, 0);
  assert.equal(response.body.submissionsSummary.latest.petId, "pet-home-pending-001");
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

test("submission detail route returns a single public submission", () => {
  const store = createCommunityStore();
  const created = handleCommunityRequest("POST", "/v1/submissions", {
    store,
    body: {
      petId: "pet-new-001",
      ownershipClaimId: "claim-pet-new-001",
      scoreReportId: "score-pet-new-001"
    }
  });

  const response = handleCommunityRequest(
    "GET",
    `/v1/submissions/${encodeURIComponent(created.body.id)}`,
    { store }
  );

  assert.equal(response.status, 200);
  assert.equal(response.body.id, created.body.id);
  assert.equal(response.body.petId, "pet-new-001");
  assert.equal(response.body.status, "pending");
});

test("submission detail route returns 404 for unknown submissions", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest(
    "GET",
    "/v1/submissions/submission-missing-001",
    { store }
  );

  assert.equal(response.status, 404);
  assert.equal(response.body.error, "submission_not_found");
  assert.equal(response.body.submissionId, "submission-missing-001");
});

test("pet package bundle validation route accepts valid bundle", () => {
  const response = handleCommunityRequest(
    "POST",
    "/v1/pet-package-bundles/validate",
    {
      body: {
        bundle: validPetPackageBundle
      }
    }
  );

  assert.equal(response.status, 200);
  assert.deepEqual(response.body.validation, {
    ok: true,
    errors: []
  });
});

test("pet package bundle validation route rejects invalid bundle", () => {
  const response = handleCommunityRequest(
    "POST",
    "/v1/pet-package-bundles/validate",
    {
      body: {
        bundle: {
          ...validPetPackageBundle,
          scoreReport: {
            ...validPetPackageBundle.scoreReport,
            petId: "pet-other-route-001"
          }
        }
      }
    }
  );

  assert.equal(response.status, 400);
  assert.equal(response.body.error, "invalid_pet_package_bundle");
  assert.equal(response.body.validation.ok, false);
  assert.ok(
    response.body.validation.errors.includes(
      "manifest.petId must match scoreReport.petId"
    )
  );
});

test("pet package bundle route creates ready import draft", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: validPetPackageBundle
      }
    }
  );
  const report = store.getScoreReport(response.body.scoreReportId);

  assert.equal(response.status, 201);
  assert.equal(response.body.status, "ready");
  assert.equal(response.body.petId, "pet-stardust-001");
  assert.equal(response.body.ownershipClaimId, "claim-pet-stardust-001");
  assert.equal(report.totalScore, validPetPackageBundle.scoreReport.totalScore);
});

test("pet package bundle route rejects invalid import draft bundle", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: {
          ...validPetPackageBundle,
          ownershipClaim: {
            ...validPetPackageBundle.ownershipClaim,
            petId: "pet-other-route-002"
          }
        }
      }
    }
  );

  assert.equal(response.status, 400);
  assert.equal(response.body.error, "invalid_pet_package_bundle");
  assert.equal(response.body.validation.ok, false);
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 0);
});

test("pet package bundle route rejects bundle owned by another user", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        userId: "user-other-001",
        bundle: validPetPackageBundle
      }
    }
  );

  assert.equal(response.status, 403);
  assert.equal(response.body.error, "bundle_owner_mismatch");
  assert.equal(response.body.ownerUserId, "user-demo-001");
  assert.equal(store.listImportDrafts("user-other-001").drafts.length, 0);
});

test("pet package bundle route rejects duplicate active import draft", () => {
  const store = createCommunityStore();
  const first = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: validPetPackageBundle
      }
    }
  );
  const second = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: validPetPackageBundle
      }
    }
  );

  assert.equal(first.status, 201);
  assert.equal(second.status, 409);
  assert.equal(second.body.error, "duplicate_import_draft");
  assert.equal(second.body.existingDraftId, first.body.id);
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 1);
});

test("pet package bundle route rejects duplicate import after submission", () => {
  const store = createCommunityStore();
  const first = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: validPetPackageBundle
      }
    }
  );
  handleCommunityRequest("POST", "/v1/import-drafts/submit", {
    store,
    body: {
      draftId: first.body.id
    }
  });
  const second = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: validPetPackageBundle
      }
    }
  );

  assert.equal(first.status, 201);
  assert.equal(second.status, 409);
  assert.equal(second.body.error, "duplicate_import_draft");
  assert.equal(second.body.existingDraftId, first.body.id);
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 1);
});

test("fantasy pet package route creates ready import draft from public package manifest", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-fantasy-pet-package",
    {
      store,
      body: {
        packageManifest: validFantasyPetPackageManifest,
        packageFileName: "pet-public-lifecycle-smoke.zip",
        packageByteCount: 664,
        targetDownloadId: "artifact-1",
        ownershipClaimId: "claim-public-lifecycle-smoke"
      }
    }
  );
  const report = store.getScoreReport(response.body.scoreReportId);

  assert.equal(response.status, 201);
  assert.equal(response.body.status, "ready");
  assert.equal(response.body.petId, "public-lifecycle-smoke");
  assert.equal(response.body.importSummary.source.kind, "fantasy-pet-rule");
  assert.equal(response.body.importSummary.review.targetDownloadId, "artifact-1");
  assert.equal(report.rewardRecommendation.amount, 80);
});

test("fantasy pet package route rejects internal paths from public package manifest", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-fantasy-pet-package",
    {
      store,
      body: {
        packageManifest: {
          ...validFantasyPetPackageManifest,
          files: [
            {
              kind: "candidate",
              path: "D:/workspace4Codex/fantasy-pet-rule/runs/job/output.png"
            }
          ]
        },
        packageFileName: "pet-public-lifecycle-smoke.zip",
        packageByteCount: 664,
        targetDownloadId: "artifact-1",
        ownershipClaimId: "claim-public-lifecycle-smoke"
      }
    }
  );

  assert.equal(response.status, 400);
  assert.equal(response.body.error, "invalid_fantasy_pet_package");
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 0);
});

test("fantasy pet package route rejects internal ledger and route artifacts", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-fantasy-pet-package",
    {
      store,
      body: {
        packageManifest: {
          ...validFantasyPetPackageManifest,
          files: [
            {
              kind: "candidate",
              path: "artifacts/candidates/final-preview.png"
            },
            {
              kind: "metadata",
              path: "learning-ledger.jsonl"
            },
            {
              kind: "metadata",
              path: "route-policy-decision.json"
            },
            {
              kind: "metadata",
              path: "ledger-suggestions/genericagent-ledger-import.json"
            },
            {
              kind: "metadata",
              path: "review/stage-gate-ledger-import.json"
            }
          ]
        },
        packageFileName: "pet-public-lifecycle-smoke.zip",
        packageByteCount: 664,
        targetDownloadId: "artifact-1",
        ownershipClaimId: "claim-public-lifecycle-smoke"
      }
    }
  );

  assert.equal(response.status, 400);
  assert.equal(response.body.error, "invalid_fantasy_pet_package");
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 0);
});

test("fantasy pet package route rejects stage gate ledger import artifact", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-fantasy-pet-package",
    {
      store,
      body: {
        packageManifest: {
          ...validFantasyPetPackageManifest,
          files: [
            {
              kind: "candidate",
              path: "artifacts/candidates/final-preview.png"
            },
            {
              kind: "metadata",
              path: "review/stage-gate-ledger-import.json"
            }
          ]
        },
        packageFileName: "pet-public-lifecycle-smoke.zip",
        packageByteCount: 664,
        targetDownloadId: "artifact-1",
        ownershipClaimId: "claim-public-lifecycle-smoke"
      }
    }
  );

  assert.equal(response.status, 400);
  assert.equal(response.body.error, "invalid_fantasy_pet_package");
  assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 0);
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

test("admin review route rejects invalid review status", () => {
  const store = createCommunityStore();
  const created = handleCommunityRequest("POST", "/v1/submissions", {
    store,
    body: {
      petId: "pet-invalid-route-001",
      ownershipClaimId: "claim-invalid-route-001",
      scoreReportId: "score-invalid-route-001"
    }
  });

  const response = handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: created.body.id,
      status: "published",
      reviewer: "admin-demo"
    }
  });

  assert.equal(response.status, 400);
  assert.equal(response.body.error, "invalid_review_status");
  assert.deepEqual(response.body.allowedStatuses, [
    "approved",
    "held",
    "rejected",
    "revoked"
  ]);
});

test("admin review route rejects invalid reward amount", () => {
  const store = createCommunityStore();
  const created = handleCommunityRequest("POST", "/v1/submissions", {
    store,
    body: {
      petId: "pet-invalid-reward-route-001",
      ownershipClaimId: "claim-invalid-reward-route-001",
      scoreReportId: "score-invalid-reward-route-001"
    }
  });

  const response = handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: created.body.id,
      status: "approved",
      reviewer: "admin-demo",
      rewardAmount: -5
    }
  });

  assert.equal(response.status, 400);
  assert.equal(response.body.error, "invalid_reward_amount");
  assert.equal(response.body.rewardAmount, -5);
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

test("approved pets route returns registered imported pet assets", () => {
  const store = createCommunityStore();
  const draft = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: validPetPackageBundle
      }
    }
  );
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

  const response = handleCommunityRequest("GET", "/v1/pets/approved", { store });

  assert.equal(response.status, 200);
  assert.equal(response.body.items.length, 1);
  assert.equal(response.body.items[0].petId, "pet-stardust-001");
  assert.equal(response.body.items[0].displayName, "Stardust Dragon");
});

test("approved pet package route returns export artifact descriptor", () => {
  const store = createCommunityStore();
  const draft = handleCommunityRequest(
    "POST",
    "/v1/import-drafts/from-pet-package-bundle",
    {
      store,
      body: {
        bundle: validPetPackageBundle
      }
    }
  );
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

  const response = handleCommunityRequest(
    "GET",
    "/v1/pets/approved/pet-stardust-001/package",
    { store }
  );

  assert.equal(response.status, 200);
  assert.equal(response.body.petId, "pet-stardust-001");
  assert.equal(response.body.displayName, "Stardust Dragon");
  assert.equal(response.body.package.exportArtifactPath, "exports/stardust-package.zip");
  assert.equal(response.body.package.status, "available");
  assert.equal(response.body.submissionId, submitted.body.submission.id);
});

test("approved pet package route returns 404 for unknown pet", () => {
  const response = handleCommunityRequest(
    "GET",
    "/v1/pets/approved/pet-missing-001/package",
    {
      store: createCommunityStore()
    }
  );

  assert.equal(response.status, 404);
  assert.equal(response.body.error, "approved_pet_package_not_found");
  assert.equal(response.body.petId, "pet-missing-001");
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
