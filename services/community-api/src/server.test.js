import assert from "node:assert/strict";
import http from "node:http";
import test from "node:test";
import { createCommunityHttpHandler } from "./server.js";
import { createCommunityStore } from "./store.js";

const requestJson = (server, method, path, body) =>
  new Promise((resolve, reject) => {
    const address = server.address();
    const payload = body ? JSON.stringify(body) : "";
    const request = http.request(
      {
        hostname: "127.0.0.1",
        port: address.port,
        path,
        method,
        headers: {
          "Content-Type": "application/json",
          "Content-Length": Buffer.byteLength(payload)
        }
      },
      (response) => {
        let data = "";
        response.on("data", (chunk) => {
          data += chunk;
        });
        response.on("end", () => {
          resolve({
            status: response.statusCode,
            body: JSON.parse(data)
          });
        });
      }
    );
    request.on("error", reject);
    request.end(payload);
  });

test("HTTP server parses JSON body for check-in", async () => {
  const server = http.createServer(
    createCommunityHttpHandler({
      store: createCommunityStore()
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(server, "POST", "/v1/check-in", {
      date: "2026-06-05"
    });

    assert.equal(response.status, 200);
    assert.equal(response.body.checkIn.date, "2026-06-05");
    assert.equal(response.body.wallet.balance, 100);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server rejects invalid JSON body", async () => {
  const server = http.createServer(createCommunityHttpHandler());
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const address = server.address();
    const response = await new Promise((resolve, reject) => {
      const request = http.request(
        {
          hostname: "127.0.0.1",
          port: address.port,
          path: "/v1/check-in",
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          }
        },
        (incoming) => {
          let data = "";
          incoming.on("data", (chunk) => {
            data += chunk;
          });
          incoming.on("end", () => {
            resolve({
              status: incoming.statusCode,
              body: JSON.parse(data)
            });
          });
        }
      );
      request.on("error", reject);
      request.end("{bad-json");
    });

    assert.equal(response.status, 400);
    assert.equal(response.body.error, "invalid_json");
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server creates import draft from pet-generator summary", async () => {
  const server = http.createServer(
    createCommunityHttpHandler({
      store: createCommunityStore()
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(server, "POST", "/v1/import-drafts", {
      readiness: {
        status: "in-progress",
        reason: "current stage is base-review"
      },
      importSummary: {
        source: {
          petId: "pet-draft-001"
        }
      }
    });

    assert.equal(response.status, 201);
    assert.equal(response.body.status, "in-progress");
    assert.equal(response.body.petId, "pet-draft-001");
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server submits community-ready import draft", async () => {
  const store = createCommunityStore();
  const server = http.createServer(
    createCommunityHttpHandler({
      store
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const draftResponse = await requestJson(server, "POST", "/v1/import-drafts", {
      readiness: {
        status: "community-ready",
        reason: "preview accepted by user"
      },
      importSummary: {
        source: {
          petId: "pet-ready-http-001"
        }
      },
      ownershipClaimId: "claim-pet-ready-http-001",
      scoreReportId: "score-pet-ready-http-001"
    });

    const submitResponse = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/submit",
      {
        draftId: draftResponse.body.id
      }
    );

    assert.equal(submitResponse.status, 201);
    assert.equal(submitResponse.body.draft.status, "submitted");
    assert.equal(submitResponse.body.submission.status, "pending");
    assert.equal(submitResponse.body.submission.petId, "pet-ready-http-001");
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server returns generated score report", async () => {
  const store = createCommunityStore();
  const server = http.createServer(
    createCommunityHttpHandler({
      store
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const draftResponse = await requestJson(server, "POST", "/v1/import-drafts", {
      readiness: {
        status: "community-ready",
        reason: "preview accepted by user"
      },
      importSummary: {
        source: {
          petId: "pet-score-http-001",
          baseIdentityStatus: "accepted"
        },
        review: {
          blockers: [],
          previewDecision: "keep",
          exportStatus: "ready"
        },
        assets: {
          previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html",
          exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/export.zip"
        }
      },
      ownershipClaimId: "claim-pet-score-http-001"
    });

    const reportResponse = await requestJson(
      server,
      "GET",
      `/v1/score-reports/${draftResponse.body.scoreReportId}`
    );

    assert.equal(reportResponse.status, 200);
    assert.equal(reportResponse.body.schema, "gamer.pet-score-report.v1");
    assert.equal(reportResponse.body.petId, "pet-score-http-001");
    assert.equal(reportResponse.body.rewardRecommendation.amount, 80);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP admin review uses score recommendation when reward amount is omitted", async () => {
  const store = createCommunityStore();
  const server = http.createServer(
    createCommunityHttpHandler({
      store
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const draftResponse = await requestJson(server, "POST", "/v1/import-drafts", {
      readiness: {
        status: "community-ready",
        reason: "preview accepted by user"
      },
      importSummary: {
        source: {
          petId: "pet-review-http-001",
          baseIdentityStatus: "accepted"
        },
        review: {
          blockers: [],
          previewDecision: "keep",
          exportStatus: "ready"
        },
        assets: {
          previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html",
          exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/export.zip"
        }
      },
      ownershipClaimId: "claim-pet-review-http-001"
    });
    const submitResponse = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/submit",
      {
        draftId: draftResponse.body.id
      }
    );

    const reviewResponse = await requestJson(server, "POST", "/v1/admin/reviews", {
      submissionId: submitResponse.body.submission.id,
      status: "approved",
      reviewer: "admin-demo"
    });

    assert.equal(reviewResponse.status, 200);
    assert.equal(reviewResponse.body.rewardEntry.amount, 80);

    const walletResponse = await requestJson(server, "GET", "/v1/wallet/me");
    assert.equal(walletResponse.body.balance, 170);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP admin review revokes posted reward with reversal ledger entry", async () => {
  const store = createCommunityStore();
  const server = http.createServer(
    createCommunityHttpHandler({
      store
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const draftResponse = await requestJson(server, "POST", "/v1/import-drafts", {
      readiness: {
        status: "community-ready",
        reason: "preview accepted by user"
      },
      importSummary: {
        source: {
          petId: "pet-revoke-http-001",
          baseIdentityStatus: "accepted"
        },
        review: {
          blockers: [],
          previewDecision: "keep",
          exportStatus: "ready"
        },
        assets: {
          previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html",
          exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/export.zip"
        }
      },
      ownershipClaimId: "claim-pet-revoke-http-001"
    });
    const submitResponse = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/submit",
      {
        draftId: draftResponse.body.id
      }
    );
    await requestJson(server, "POST", "/v1/admin/reviews", {
      submissionId: submitResponse.body.submission.id,
      status: "approved",
      reviewer: "admin-demo"
    });

    const revokeResponse = await requestJson(server, "POST", "/v1/admin/reviews", {
      submissionId: submitResponse.body.submission.id,
      status: "revoked",
      reviewer: "admin-demo"
    });

    assert.equal(revokeResponse.status, 200);
    assert.equal(revokeResponse.body.rewardReversalEntry.amount, -80);
    assert.equal(
      revokeResponse.body.rewardReversalEntry.sourceType,
      "submission-reward-reversal"
    );

    const walletResponse = await requestJson(server, "GET", "/v1/wallet/me");
    assert.equal(walletResponse.body.balance, 90);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});
