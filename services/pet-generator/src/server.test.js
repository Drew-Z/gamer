import assert from "node:assert/strict";
import http from "node:http";
import test from "node:test";
import { createPetGeneratorHttpHandler } from "./server.js";

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

test("summarize route accepts fantasy-pet-rule statePath", async () => {
  const server = http.createServer(
    createPetGeneratorHttpHandler({
      readFile: async () =>
        JSON.stringify({
          petId: "demo-pet",
          currentStage: "preview-review",
          blockers: [],
          preview: {
            userDecision: "keep",
            urlOrPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html"
          }
        })
    })
  );
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(
      server,
      "POST",
      "/v1/fantasy-pet-rule/summarize",
      {
        statePath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/state.json"
      }
    );

    assert.equal(response.status, 200);
    assert.equal(response.body.status, "community-ready");
    assert.equal(response.body.reason, "preview accepted by user");
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("import-summary route returns readiness and import summary", async () => {
  const server = http.createServer(createPetGeneratorHttpHandler());
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const response = await requestJson(
      server,
      "POST",
      "/v1/fantasy-pet-rule/import-summary",
      {
        state: {
          schema: "fantasy-pet.codex-state.v1",
          petId: "demo-pet",
          currentStage: "preview-review",
          baseIdentity: {
            status: "accepted"
          },
          blockers: [],
          preview: {
            userDecision: "keep",
            urlOrPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html"
          },
          export: {
            decision: "asked",
            status: "ready",
            artifactPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/export.zip"
          }
        }
      }
    );

    assert.equal(response.status, 200);
    assert.equal(response.body.readiness.status, "community-ready");
    assert.equal(response.body.importSummary.source.petId, "demo-pet");
    assert.equal(response.body.importSummary.review.exportStatus, "ready");
    assert.equal(
      response.body.importSummary.assets.exportArtifactPath,
      "D:/workspace4Codex/fantasy-pet-rule/runs/demo/export.zip"
    );
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("HTTP server rejects invalid JSON body", async () => {
  const server = http.createServer(createPetGeneratorHttpHandler());
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const address = server.address();
    const response = await new Promise((resolve, reject) => {
      const request = http.request(
        {
          hostname: "127.0.0.1",
          port: address.port,
          path: "/v1/fantasy-pet-rule/summarize",
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
