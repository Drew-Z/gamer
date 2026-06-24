import assert from "node:assert/strict";
import http from "node:http";
import test from "node:test";
import { createAdminReviewHttpHandler } from "./server.js";

const listen = (server) =>
  new Promise((resolve) => {
    server.listen(0, "127.0.0.1", () => resolve(server.address().port));
  });

const close = (server) =>
  new Promise((resolve, reject) => {
    server.close((error) => (error ? reject(error) : resolve()));
  });

test("admin-review proxies fantasy pet review pages through the community API", async () => {
  const upstreamRequests = [];
  const upstream = http.createServer((request, response) => {
    upstreamRequests.push({ method: request.method, url: request.url });

    if (
      request.method === "GET" &&
      request.url === "/admin/pet-generation-jobs/job-123/review"
    ) {
      response.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
      response.end("<main>Review job-123</main>");
      return;
    }

    response.writeHead(404, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ error: "not_found" }));
  });
  const upstreamPort = await listen(upstream);

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`
    })
  );
  const port = await listen(server);

  try {
    const response = await fetch(
      `http://127.0.0.1:${port}/admin/pet-generation-jobs/job-123/review`
    );

    assert.equal(response.status, 200);
    assert.match(response.headers.get("content-type") ?? "", /^text\/html/u);
    assert.equal(await response.text(), "<main>Review job-123</main>");
    assert.deepEqual(upstreamRequests, [
      { method: "GET", url: "/admin/pet-generation-jobs/job-123/review" }
    ]);
  } finally {
    await close(server);
    await close(upstream);
  }
});

test("admin-review proxies the fantasy pet review queue overview", async () => {
  const upstreamRequests = [];
  const upstream = http.createServer((request, response) => {
    upstreamRequests.push({ method: request.method, url: request.url });

    if (
      request.method === "GET" &&
      request.url === "/admin/pet-generation-jobs?status=all"
    ) {
      response.writeHead(200, { "Content-Type": "text/html; charset=utf-8" });
      response.end("<main>Review Queue</main>");
      return;
    }

    response.writeHead(404, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ error: "not_found" }));
  });
  const upstreamPort = await listen(upstream);

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`
    })
  );
  const port = await listen(server);

  try {
    const response = await fetch(
      `http://127.0.0.1:${port}/admin/pet-generation-jobs?status=all`
    );

    assert.equal(response.status, 200);
    assert.match(response.headers.get("content-type") ?? "", /^text\/html/u);
    assert.equal(await response.text(), "<main>Review Queue</main>");
    assert.deepEqual(upstreamRequests, [
      { method: "GET", url: "/admin/pet-generation-jobs?status=all" }
    ]);
  } finally {
    await close(server);
    await close(upstream);
  }
});

test("admin-review proxies community writes with server token and browser origin metadata", async () => {
  const upstreamRequests = [];
  const upstream = http.createServer((request, response) => {
    let body = "";
    request.on("data", (chunk) => {
      body += chunk;
    });
    request.on("end", () => {
      upstreamRequests.push({
        method: request.method,
        url: request.url,
        headers: request.headers,
        body
      });
      response.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
      response.end(JSON.stringify({ ok: true }));
    });
  });
  const upstreamPort = await listen(upstream);

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`,
      communityDemoToken: "server-community-demo-token"
    })
  );
  const port = await listen(server);

  try {
    const response = await fetch(`http://127.0.0.1:${port}/api/v1/admin/reviews`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Origin: "https://desktop-pet.example.internal",
        Referer: "https://desktop-pet.example.internal/admin",
        "X-Demo-Token": "client-token-must-not-forward"
      },
      body: JSON.stringify({
        submissionId: "submission-local-002",
        status: "approved",
        reviewer: "admin-ui"
      })
    });

    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { ok: true });
    assert.equal(upstreamRequests.length, 1);
    assert.equal(upstreamRequests[0].url, "/v1/admin/reviews");
    assert.equal(
      upstreamRequests[0].headers.origin,
      "https://desktop-pet.example.internal"
    );
    assert.equal(
      upstreamRequests[0].headers.referer,
      "https://desktop-pet.example.internal/admin"
    );
    assert.equal(
      upstreamRequests[0].headers["x-demo-token"],
      "server-community-demo-token"
    );
  } finally {
    await close(server);
    await close(upstream);
  }
});

test("admin-review does not proxy fantasy pet admin worker routes", async () => {
  const upstreamRequests = [];
  const upstream = http.createServer((request, response) => {
    upstreamRequests.push({ method: request.method, url: request.url });
    response.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ ok: true }));
  });
  const upstreamPort = await listen(upstream);

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`
    })
  );
  const port = await listen(server);

  try {
    const response = await fetch(`http://127.0.0.1:${port}/admin/server-worker-cycle`, {
      method: "POST"
    });

    assert.equal(response.status, 404);
    assert.deepEqual(await response.json(), { error: "not_found" });
    assert.deepEqual(upstreamRequests, []);
  } finally {
    await close(server);
    await close(upstream);
  }
});
