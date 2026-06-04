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
