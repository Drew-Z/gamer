import assert from "node:assert/strict";
import http from "node:http";
import test from "node:test";
import { validPetPackageBundle } from "../../../packages/pet-package-spec/src/index.js";
import { createCommunityHttpHandler, resolveCommunityApiPort } from "./server.js";
import { createRateLimiterPolicy } from "./rate-limit.js";
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

const requestRaw = (server, method, path, body = "") =>
  new Promise((resolve, reject) => {
    const address = server.address();
    const payload = Buffer.isBuffer(body) ? body : Buffer.from(body);
    const request = http.request(
      {
        hostname: "127.0.0.1",
        port: address.port,
        path,
        method,
        headers: {
          "Content-Length": payload.byteLength
        }
      },
      (response) => {
        const chunks = [];
        response.on("data", (chunk) => {
          chunks.push(chunk);
        });
        response.on("end", () => {
          resolve({
            status: response.statusCode,
            headers: response.headers,
            body: Buffer.concat(chunks)
          });
        });
      }
    );
    request.on("error", reject);
    request.end(payload);
  });

const createFantasyPetUpstream = async (handler) => {
  const requests = [];
  const server = http.createServer((request, response) => {
    let body = "";
    request.on("data", (chunk) => {
      body += chunk;
    });
    request.on("end", () => {
      const entry = {
        method: request.method,
        url: request.url,
        headers: request.headers,
        body
      };
      requests.push(entry);
      handler(entry, response);
    });
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  return {
    baseUrl: `http://127.0.0.1:${server.address().port}`,
    requests,
    server
  };
};

test("community API port prefers PORT then SERVER_PORT then default", () => {
  assert.equal(resolveCommunityApiPort({ PORT: "5123", SERVER_PORT: "6123" }), 5123);
  assert.equal(resolveCommunityApiPort({ SERVER_PORT: "6123" }), 6123);
  assert.equal(resolveCommunityApiPort({}), 4000);
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

test("HTTP server reports unconfigured fantasy pet public proxy", async () => {
  const server = http.createServer(
    createCommunityHttpHandler({
      env: {}
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(server, "POST", "/pet-generation-jobs", {
      schema: "fantasy-pet.app-job-create-request.v1",
      description: "tiny dragon"
    });

    assert.equal(response.status, 503);
    assert.equal(response.body.error, "fantasy_pet_api_unconfigured");
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server proxies fantasy pet public job creation", async () => {
  const upstream = await createFantasyPetUpstream((request, response) => {
    response.writeHead(201, {
      "Content-Type": "application/json"
    });
    response.end(
      JSON.stringify({
        schema: "fantasy-pet.app-job-response.v1",
        appJobId: "job-123",
        status: "queued",
        progressStatus: "queued",
        nextAction: "wait"
      })
    );
  });
  const server = http.createServer(
    createCommunityHttpHandler({
      env: {
        FANTASY_PET_API_BASE_URL: upstream.baseUrl
      }
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(server, "POST", "/pet-generation-jobs", {
      schema: "fantasy-pet.app-job-create-request.v1",
      description: "tiny dragon"
    });

    assert.equal(response.status, 201);
    assert.equal(response.body.appJobId, "job-123");
    assert.equal(upstream.requests.length, 1);
    assert.equal(upstream.requests[0].method, "POST");
    assert.equal(upstream.requests[0].url, "/pet-generation-jobs");
    assert.ok(
      upstream.requests[0].body.includes(
        "\"schema\":\"fantasy-pet.app-job-create-request.v1\""
      )
    );
  } finally {
    await new Promise((resolve) => server.close(resolve));
    await new Promise((resolve) => upstream.server.close(resolve));
  }
});

test("HTTP server proxies fantasy pet package bytes without JSON wrapping", async () => {
  const upstream = await createFantasyPetUpstream((request, response) => {
    response.writeHead(200, {
      "Content-Type": "application/zip"
    });
    response.end(Buffer.from("PK fantasy pet"));
  });
  const server = http.createServer(
    createCommunityHttpHandler({
      env: {
        FANTASY_PET_API_BASE_URL: upstream.baseUrl
      }
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestRaw(
      server,
      "GET",
      "/pet-generation-jobs/job-123/package"
    );

    assert.equal(response.status, 200);
    assert.equal(response.headers["content-type"], "application/zip");
    assert.equal(response.body.toString("utf8"), "PK fantasy pet");
    assert.equal(upstream.requests[0].method, "GET");
    assert.equal(upstream.requests[0].url, "/pet-generation-jobs/job-123/package");
  } finally {
    await new Promise((resolve) => server.close(resolve));
    await new Promise((resolve) => upstream.server.close(resolve));
  }
});

test("HTTP server proxies fantasy pet admin review page", async () => {
  const upstream = await createFantasyPetUpstream((request, response) => {
    response.writeHead(200, {
      "Content-Type": "text/html; charset=utf-8"
    });
    response.end("<main>Review job-123</main>");
  });
  const server = http.createServer(
    createCommunityHttpHandler({
      env: {
        FANTASY_PET_API_BASE_URL: upstream.baseUrl
      }
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestRaw(
      server,
      "GET",
      "/admin/pet-generation-jobs/job-123/review"
    );

    assert.equal(response.status, 200);
    assert.equal(response.headers["content-type"], "text/html; charset=utf-8");
    assert.equal(response.body.toString("utf8"), "<main>Review job-123</main>");
    assert.equal(upstream.requests[0].method, "GET");
    assert.equal(upstream.requests[0].url, "/admin/pet-generation-jobs/job-123/review");
  } finally {
    await new Promise((resolve) => server.close(resolve));
    await new Promise((resolve) => upstream.server.close(resolve));
  }
});

test("HTTP server proxies fantasy pet admin review overview", async () => {
  const upstream = await createFantasyPetUpstream((request, response) => {
    response.writeHead(200, {
      "Content-Type": "text/html; charset=utf-8"
    });
    response.end("<main>Review Queue</main>");
  });
  const server = http.createServer(
    createCommunityHttpHandler({
      env: {
        FANTASY_PET_API_BASE_URL: upstream.baseUrl
      }
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestRaw(
      server,
      "GET",
      "/admin/pet-generation-jobs?status=waiting-review"
    );

    assert.equal(response.status, 200);
    assert.equal(response.body.toString("utf8"), "<main>Review Queue</main>");
    assert.equal(upstream.requests[0].method, "GET");
    assert.equal(upstream.requests[0].url, "/admin/pet-generation-jobs?status=waiting-review");
  } finally {
    await new Promise((resolve) => server.close(resolve));
    await new Promise((resolve) => upstream.server.close(resolve));
  }
});

test("HTTP server does not proxy fantasy pet admin worker routes", async () => {
  const upstream = await createFantasyPetUpstream((request, response) => {
    response.writeHead(500, {
      "Content-Type": "application/json"
    });
    response.end(JSON.stringify({ error: "admin_route_should_not_be_proxied" }));
  });
  const server = http.createServer(
    createCommunityHttpHandler({
      env: {
        FANTASY_PET_API_BASE_URL: upstream.baseUrl
      }
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(server, "POST", "/admin/server-worker-cycle", {});

    assert.equal(response.status, 404);
    assert.equal(response.body.error, "not_found");
    assert.equal(upstream.requests.length, 0);
  } finally {
    await new Promise((resolve) => server.close(resolve));
    await new Promise((resolve) => upstream.server.close(resolve));
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

test("HTTP server creates import draft from fantasy pet rule statePath", async () => {
  const store = createCommunityStore();
  const server = http.createServer(
    createCommunityHttpHandler({
      store,
      readFile: async () =>
        JSON.stringify({
          schema: "fantasy-pet.codex-state.v1",
          petId: "pet-statepath-http-001",
          currentStage: "preview-review",
          baseIdentity: {
            status: "accepted"
          },
          blockers: [],
          preview: {
            userDecision: "keep",
            urlOrPath: "D:/workspace4Codex/fantasy-pet-rule/runs/statepath/preview.html"
          },
          export: {
            decision: "asked",
            status: "ready",
            artifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/statepath/export.zip"
          }
        })
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/from-fantasy-pet-rule",
      {
        statePath: "D:/workspace4Codex/fantasy-pet-rule/runs/statepath/state.json",
        ownershipClaimId: "claim-pet-statepath-http-001"
      }
    );
    const reportResponse = await requestJson(
      server,
      "GET",
      `/v1/score-reports/${response.body.scoreReportId}`
    );

    assert.equal(response.status, 201);
    assert.equal(response.body.status, "ready");
    assert.equal(response.body.petId, "pet-statepath-http-001");
    assert.equal(response.body.importSummary.assets.exportArtifactPath, "D:/workspace4Codex/fantasy-pet-rule/runs/statepath/export.zip");
    assert.equal(reportResponse.status, 200);
    assert.equal(reportResponse.body.rewardRecommendation.amount, 80);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server rejects pet package bundle owned by another user", async () => {
  const store = createCommunityStore();
  const server = http.createServer(
    createCommunityHttpHandler({
      store
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/from-pet-package-bundle",
      {
        userId: "user-other-001",
        bundle: validPetPackageBundle
      }
    );

    assert.equal(response.status, 403);
    assert.equal(response.body.error, "bundle_owner_mismatch");
    assert.equal(response.body.ownerUserId, "user-demo-001");
    assert.equal(store.listImportDrafts("user-other-001").drafts.length, 0);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server rejects duplicate pet package bundle import draft", async () => {
  const store = createCommunityStore();
  const server = http.createServer(
    createCommunityHttpHandler({
      store
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const first = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/from-pet-package-bundle",
      {
        bundle: validPetPackageBundle
      }
    );
    const second = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/from-pet-package-bundle",
      {
        bundle: validPetPackageBundle
      }
    );

    assert.equal(first.status, 201);
    assert.equal(second.status, 409);
    assert.equal(second.body.error, "duplicate_import_draft");
    assert.equal(second.body.existingDraftId, first.body.id);
    assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 1);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server rejects duplicate pet package bundle import after submission", async () => {
  const store = createCommunityStore();
  const server = http.createServer(
    createCommunityHttpHandler({
      store
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const first = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/from-pet-package-bundle",
      {
        bundle: validPetPackageBundle
      }
    );
    await requestJson(server, "POST", "/v1/import-drafts/submit", {
      draftId: first.body.id
    });
    const second = await requestJson(
      server,
      "POST",
      "/v1/import-drafts/from-pet-package-bundle",
      {
        bundle: validPetPackageBundle
      }
    );

    assert.equal(first.status, 201);
    assert.equal(second.status, 409);
    assert.equal(second.body.error, "duplicate_import_draft");
    assert.equal(second.body.existingDraftId, first.body.id);
    assert.equal(store.listImportDrafts("user-demo-001").drafts.length, 1);
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

test("HTTP admin review queue returns aggregated submission evidence", async () => {
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
          petId: "pet-review-queue-http-001",
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
      ownershipClaimId: "claim-pet-review-queue-http-001"
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

    const queueResponse = await requestJson(server, "GET", "/v1/admin/review-queue");
    const item = queueResponse.body.items.find(
      (entry) => entry.submission.id === submitResponse.body.submission.id
    );

    assert.equal(queueResponse.status, 200);
    assert.equal(item.submission.petId, "pet-review-queue-http-001");
    assert.equal(item.scoreReport.petId, "pet-review-queue-http-001");
    assert.equal(item.reviews.length, 1);
    assert.equal(item.rewardLedgerEntries.length, 1);
    assert.equal(item.outstandingReward, 80);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server returns approved imported pet registry", async () => {
  const store = createCommunityStore();
  const server = http.createServer(
    createCommunityHttpHandler({
      store
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const draftResponse = await requestJson(
      server,
      "POST",
      "/v1/import-drafts",
      {
        readiness: {
          status: "community-ready",
          reason: "preview accepted by user"
        },
        importSummary: {
          source: {
            petId: "pet-approved-registry-001",
            displayName: "Approved Registry Pet",
            kind: "fantasy-pet-rule",
            baseIdentityStatus: "accepted"
          },
          review: {
            blockers: [],
            previewDecision: "keep",
            exportStatus: "ready"
          },
          assets: {
            previewPath: "D:/workspace4Codex/fantasy-pet-rule/runs/approved-registry/preview.html",
            motionSheets: ["motion/sheets/idle.png", "motion/sheets/happy_click.png"],
            exportArtifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/approved-registry/export.zip"
          }
        },
        ownershipClaimId: "claim-pet-approved-registry-001"
      }
    );
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

    const response = await requestJson(server, "GET", "/v1/pets/approved");

    assert.equal(response.status, 200);
    assert.equal(response.body.items[0].petId, "pet-approved-registry-001");
    assert.equal(response.body.items[0].assets.motionSheetCount, 2);
    assert.equal(
      response.body.items[0].assets.exportArtifactPath,
      "D:/workspace4Codex/fantasy-pet-rule/runs/approved-registry/export.zip"
    );
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server emits structured request log on finish", async () => {
  const logs = [];
  const server = http.createServer(
    createCommunityHttpHandler({
      store: createCommunityStore(),
      log: (entry) => logs.push(entry)
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(server, "GET", "/health");
    assert.equal(response.status, 200);

    await new Promise((resolve) => setTimeout(resolve, 20));

    const healthLog = logs.find((entry) => entry.path === "/health");
    assert.ok(healthLog, "health request should be logged on finish");
    assert.equal(healthLog.method, "GET");
    assert.equal(healthLog.status, 200);
    assert.ok(typeof healthLog.durationMs === "number" && healthLog.durationMs >= 0);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server rate-limits writes but not reads when policy is injected", async () => {
  const policy = createRateLimiterPolicy({ windowMs: 60_000, writeMax: 1, readMax: 60 });
  const server = http.createServer(
    createCommunityHttpHandler({
      store: createCommunityStore(),
      rateLimit: policy
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const first = await requestJson(server, "POST", "/v1/check-in", {
      date: "2026-06-18"
    });
    assert.equal(first.status, 200);

    const second = await requestJson(server, "POST", "/v1/check-in", {
      date: "2026-06-19"
    });
    assert.equal(second.status, 429);
    assert.equal(second.body.error, "rate_limit_exceeded");
    assert.ok(second.body.retryAfterMs > 0);

    const read = await requestJson(server, "GET", "/v1/wallet/me");
    assert.equal(read.status, 200);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server leaves rate limiting disabled unless env enables it", async () => {
  const server = http.createServer(
    createCommunityHttpHandler({
      env: {
        COMMUNITY_RATE_LIMIT_WRITE_MAX: "1"
      },
      store: createCommunityStore()
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const first = await requestJson(server, "POST", "/v1/check-in", {
      date: "2026-06-18"
    });
    const second = await requestJson(server, "POST", "/v1/check-in", {
      date: "2026-06-19"
    });

    assert.equal(first.status, 200);
    assert.equal(second.status, 200);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server uses env-gated rate limiting when enabled", async () => {
  const server = http.createServer(
    createCommunityHttpHandler({
      env: {
        COMMUNITY_RATE_LIMIT_ENABLED: "1",
        COMMUNITY_RATE_LIMIT_WINDOW_MS: "60000",
        COMMUNITY_RATE_LIMIT_WRITE_MAX: "1",
        COMMUNITY_RATE_LIMIT_READ_MAX: "60"
      },
      store: createCommunityStore()
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const first = await requestJson(server, "POST", "/v1/check-in", {
      date: "2026-06-18"
    });
    const second = await requestJson(server, "POST", "/v1/check-in", {
      date: "2026-06-19"
    });

    assert.equal(first.status, 200);
    assert.equal(second.status, 429);
    assert.equal(second.body.error, "rate_limit_exceeded");
    assert.ok(second.body.retryAfterMs > 0);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server exposes configurable SLA via /v1/sla", async () => {
  const server = http.createServer(
    createCommunityHttpHandler({
      store: createCommunityStore(),
      env: { SLA_HATCH_CUSTOM_MS: "777000" }
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(server, "GET", "/v1/sla");
    assert.equal(response.status, 200);
    assert.equal(response.body.schema, "gamer.sla.v1");
    assert.equal(response.body.hatch.customHatchMaxMs, 777000);
    assert.equal(response.body.polling.suggestedIntervalMs, 3000);
    assert.equal(response.body.failureThresholds.consecutivePollFailuresBeforeSlowNotice, 3);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});
