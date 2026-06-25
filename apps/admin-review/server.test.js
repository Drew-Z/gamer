import assert from "node:assert/strict";
import fs from "node:fs";
import http from "node:http";
import os from "node:os";
import path from "node:path";
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

test("admin-review challenges requests when built-in basic auth is configured", async () => {
  const upstreamRequests = [];
  const upstream = http.createServer((request, response) => {
    upstreamRequests.push({ method: request.method, url: request.url });
    response.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ ok: true }));
  });
  const upstreamPort = await listen(upstream);

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`,
      env: {
        PRIVATE_OPS_BASIC_AUTH_USER: "operator",
        PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password"
      }
    })
  );
  const port = await listen(server);

  try {
    const response = await fetch(`http://127.0.0.1:${port}/v1/sla`);

    assert.equal(response.status, 401);
    assert.match(response.headers.get("www-authenticate") ?? "", /^Basic /u);
    assert.deepEqual(await response.json(), { error: "admin_basic_auth_required" });
    assert.deepEqual(upstreamRequests, []);
  } finally {
    await close(server);
    await close(upstream);
  }
});

test("admin-review accepts built-in basic auth without forwarding credentials", async () => {
  const upstreamRequests = [];
  const upstream = http.createServer((request, response) => {
    upstreamRequests.push({
      method: request.method,
      url: request.url,
      headers: request.headers
    });
    response.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ ok: true }));
  });
  const upstreamPort = await listen(upstream);

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`,
      env: {
        PRIVATE_OPS_BASIC_AUTH_USER: "operator",
        PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password"
      }
    })
  );
  const port = await listen(server);

  try {
    const credentials = Buffer.from("operator:private-password").toString("base64");
    const response = await fetch(`http://127.0.0.1:${port}/v1/sla`, {
      headers: {
        Authorization: `Basic ${credentials}`
      }
    });

    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { ok: true });
    assert.equal(upstreamRequests.length, 1);
    assert.equal(upstreamRequests[0].url, "/v1/sla");
    assert.equal(upstreamRequests[0].headers.authorization, undefined);
  } finally {
    await close(server);
    await close(upstream);
  }
});

test("admin-review ops check verifies raw community auth from the target host", async () => {
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
      response.writeHead(401, { "Content-Type": "application/json; charset=utf-8" });
      response.end(JSON.stringify({ error: "unauthorized_demo_request" }));
    });
  });
  const upstreamPort = await listen(upstream);

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`,
      env: {
        PRIVATE_OPS_BASIC_AUTH_USER: "operator",
        PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password"
      }
    })
  );
  const port = await listen(server);

  try {
    const credentials = Buffer.from("operator:private-password").toString("base64");
    const response = await fetch(
      `http://127.0.0.1:${port}/ops/internal-community-auth-check`,
      {
        headers: {
          Authorization: `Basic ${credentials}`
        }
      }
    );

    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), {
      schema: "desktop-pet.ops.internal-community-auth-check.v1",
      ok: true,
      communityWriteWithoutToken: {
        status: 401,
        error: "unauthorized_demo_request"
      }
    });
    assert.equal(upstreamRequests.length, 1);
    assert.equal(upstreamRequests[0].method, "POST");
    assert.equal(upstreamRequests[0].url, "/v1/check-in");
    assert.equal(upstreamRequests[0].headers["x-demo-token"], undefined);
    assert.equal(upstreamRequests[0].headers.authorization, undefined);
  } finally {
    await close(server);
    await close(upstream);
  }
});

test("admin-review ops database readiness reports missing postgres without secrets", async () => {
  const upstream = http.createServer((request, response) => {
    response.writeHead(404, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ error: "not_found" }));
  });
  const upstreamPort = await listen(upstream);

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`,
      env: {
        PRIVATE_OPS_BASIC_AUTH_USER: "operator",
        PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password",
        DATABASE_URL: ""
      }
    })
  );
  const port = await listen(server);

  try {
    const credentials = Buffer.from("operator:private-password").toString("base64");
    const response = await fetch(
      `http://127.0.0.1:${port}/ops/community-db-readiness`,
      {
        headers: {
          Authorization: `Basic ${credentials}`
        }
      }
    );

    assert.equal(response.status, 424);
    assert.deepEqual(await response.json(), {
      schema: "desktop-pet.ops.community-db-readiness.v1",
      ok: false,
      database: {
        mode: "memory",
        postgresConfigured: false,
        migrationCount: 5
      },
      error: "postgres_database_url_required"
    });
  } finally {
    await close(server);
    await close(upstream);
  }
});

test("admin-review ops database readiness runs postgres dry-run without leaking url", async () => {
  const upstream = http.createServer((request, response) => {
    response.writeHead(404, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ error: "not_found" }));
  });
  const upstreamPort = await listen(upstream);
  const queries = [];
  const fakeClient = {
    async connect() {},
    async end() {},
    async query(sql, params = []) {
      queries.push({ sql, params });
      if (/select id from schema_migrations/iu.test(sql)) {
        return { rows: [{ id: "001_initial_community_schema" }] };
      }
      return { rows: [] };
    }
  };

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`,
      createOpsPgClient: () => fakeClient,
      env: {
        PRIVATE_OPS_BASIC_AUTH_USER: "operator",
        PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password",
        DATABASE_URL: "postgres://db.example.invalid:5432/community?sslmode=require"
      }
    })
  );
  const port = await listen(server);

  try {
    const credentials = Buffer.from("operator:private-password").toString("base64");
    const response = await fetch(
      `http://127.0.0.1:${port}/ops/community-db-readiness`,
      {
        headers: {
          Authorization: `Basic ${credentials}`
        }
      }
    );
    const body = await response.json();

    assert.equal(response.status, 200);
    assert.deepEqual(body, {
      schema: "desktop-pet.ops.community-db-readiness.v1",
      ok: true,
      database: {
        mode: "postgres",
        postgresConfigured: true,
        migrationCount: 5,
        dryRun: {
          pending: 4,
          applied: 0,
          skipped: 1
        }
      }
    });
    assert.equal(JSON.stringify(body).includes("db.example.invalid"), false);
    assert.ok(queries.some((query) => /select id from schema_migrations/iu.test(query.sql)));
    assert.equal(queries.some((query) => query.sql === "BEGIN"), false);
  } finally {
    await close(server);
    await close(upstream);
  }
});

test("admin-review ops database backup drill reports missing postgres without secrets", async () => {
  const upstream = http.createServer((request, response) => {
    response.writeHead(404, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ error: "not_found" }));
  });
  const upstreamPort = await listen(upstream);

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`,
      env: {
        PRIVATE_OPS_BASIC_AUTH_USER: "operator",
        PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password",
        DATABASE_URL: ""
      }
    })
  );
  const port = await listen(server);

  try {
    const credentials = Buffer.from("operator:private-password").toString("base64");
    const response = await fetch(
      `http://127.0.0.1:${port}/ops/community-db-backup-drill`,
      {
        headers: {
          Authorization: `Basic ${credentials}`
        }
      }
    );

    assert.equal(response.status, 424);
    assert.deepEqual(await response.json(), {
      schema: "desktop-pet.ops.community-db-backup-drill.v1",
      ok: false,
      database: {
        mode: "memory",
        postgresConfigured: false
      },
      error: "postgres_database_url_required"
    });
  } finally {
    await close(server);
    await close(upstream);
  }
});

test("admin-review ops database backup drill restores samples into temp tables without leaking url", async () => {
  const upstream = http.createServer((request, response) => {
    response.writeHead(404, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ error: "not_found" }));
  });
  const upstreamPort = await listen(upstream);
  const queries = [];
  const fakeClient = {
    async connect() {},
    async end() {},
    async query(sql, params = []) {
      queries.push({ sql, params });
      if (/information_schema\.tables/iu.test(sql)) {
        return {
          rows: [
            { table_name: "schema_migrations" },
            { table_name: "users" }
          ]
        };
      }
      if (/count\(\*\).*public\."schema_migrations"/isu.test(sql)) {
        return { rows: [{ row_count: "5" }] };
      }
      if (/count\(\*\).*public\."users"/isu.test(sql)) {
        return { rows: [{ row_count: "40" }] };
      }
      if (/count\(\*\).*"private_ops_backup_drill_0"/isu.test(sql)) {
        return { rows: [{ row_count: "5" }] };
      }
      if (/count\(\*\).*"private_ops_backup_drill_1"/isu.test(sql)) {
        return { rows: [{ row_count: "25" }] };
      }
      return { rows: [] };
    }
  };

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`,
      createOpsPgClient: () => fakeClient,
      env: {
        PRIVATE_OPS_BASIC_AUTH_USER: "operator",
        PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password",
        DATABASE_URL: "postgres://db.example.invalid:5432/community?sslmode=require"
      }
    })
  );
  const port = await listen(server);

  try {
    const credentials = Buffer.from("operator:private-password").toString("base64");
    const response = await fetch(
      `http://127.0.0.1:${port}/ops/community-db-backup-drill`,
      {
        headers: {
          Authorization: `Basic ${credentials}`
        }
      }
    );
    const body = await response.json();

    assert.equal(response.status, 200);
    assert.deepEqual(body, {
      schema: "desktop-pet.ops.community-db-backup-drill.v1",
      ok: true,
      database: {
        mode: "postgres",
        postgresConfigured: true
      },
      drill: {
        strategy: "temporary-table-snapshot",
        sourceTableCount: 2,
        restoredTableCount: 2,
        sampleRowLimit: 25,
        tables: [
          {
            name: "schema_migrations",
            sourceRows: 5,
            restoredRows: 5
          },
          {
            name: "users",
            sourceRows: 40,
            restoredRows: 25
          }
        ]
      }
    });
    assert.equal(JSON.stringify(body).includes("db.example.invalid"), false);
    assert.ok(queries.some((query) => query.sql === "BEGIN"));
    assert.ok(queries.some((query) => query.sql === "COMMIT"));
    assert.ok(
      queries.some((query) =>
        /create temp table "private_ops_backup_drill_0" on commit drop as select \* from public\."schema_migrations" limit \$1/iu.test(query.sql)
      )
    );
    assert.ok(
      queries.some((query) =>
        /create temp table "private_ops_backup_drill_1" on commit drop as select \* from public\."users" limit \$1/iu.test(query.sql)
      )
    );
  } finally {
    await close(server);
    await close(upstream);
  }
});

test("admin-review ops monitor status can trigger a synthetic monitor run", async () => {
  const upstream = http.createServer((request, response) => {
    response.writeHead(404, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ error: "not_found" }));
  });
  const upstreamPort = await listen(upstream);
  let runCount = 0;
  const privateOpsMonitor = {
    async runOnce() {
      runCount += 1;
      return {
        ok: true,
        status: "pass"
      };
    },
    getStatus() {
      return {
        schema: "desktop-pet.ops.private-ops-monitor-status.v1",
        ok: true,
        monitor: {
          enabled: true,
          intervalMs: 300000,
          staleAfterMs: 900000,
          historyLimit: 12,
          knownPackageGate: false,
          consecutiveFailures: 0,
          lastRunAt: "2026-06-24T10:00:00.000Z",
          lastOkAt: "2026-06-24T10:00:00.000Z",
          lastStatus: "pass",
          stale: false
        },
        lastRun: {
          at: "2026-06-24T10:00:00.000Z",
          ok: true,
          status: "pass",
          durationMs: 12,
          checks: [
            { name: "community health is public-safe", status: "pass" }
          ]
        },
        history: [
          {
            at: "2026-06-24T10:00:00.000Z",
            ok: true,
            status: "pass",
            durationMs: 12,
            checkCount: 1
          }
        ]
      };
    }
  };

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`,
      privateOpsMonitor,
      env: {
        PRIVATE_OPS_BASIC_AUTH_USER: "operator",
        PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password"
      }
    })
  );
  const port = await listen(server);

  try {
    const credentials = Buffer.from("operator:private-password").toString("base64");
    const response = await fetch(
      `http://127.0.0.1:${port}/ops/private-ops-monitor-status?run=1`,
      {
        headers: {
          Authorization: `Basic ${credentials}`
        }
      }
    );

    assert.equal(response.status, 200);
    assert.equal(runCount, 1);
    assert.deepEqual(await response.json(), privateOpsMonitor.getStatus());
  } finally {
    await close(server);
    await close(upstream);
  }
});

test("admin-review ops hooks audit reports hiden user hook state without secrets", async () => {
  const repoRoot = fs.mkdtempSync(path.join(os.tmpdir(), "admin-review-hooks-audit-"));
  const logDir = path.join(repoRoot, ".private-ops", "logs");
  fs.mkdirSync(logDir, { recursive: true });
  fs.writeFileSync(
    path.join(logDir, "private-ops-user-hooks.json"),
    JSON.stringify({
      schema: "desktop-pet.ops.user-hooks-state.v1",
      enabled: true,
      smokeLogConfigured: true,
      scheduler: {
        mode: "user-process",
        intervalMs: 300000,
        smokeCommand: "node tools/private-ops-smoke.js"
      },
      logRotation: {
        rotate: 14,
        maxBytes: 1048576,
        compress: false
      }
    })
  );
  fs.writeFileSync(
    path.join(logDir, "private-ops-smoke.log"),
    JSON.stringify({
      ok: true,
      checks: [{ name: "community health is public-safe", status: "pass" }]
    })
  );
  const upstream = http.createServer((request, response) => {
    response.writeHead(404, { "Content-Type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ error: "not_found" }));
  });
  const upstreamPort = await listen(upstream);

  const server = http.createServer(
    createAdminReviewHttpHandler({
      communityApiUrl: `http://127.0.0.1:${upstreamPort}`,
      repoRoot,
      env: {
        PRIVATE_OPS_BASIC_AUTH_USER: "operator",
        PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password",
        PRIVATE_OPS_HOOKS_MODE: "user",
        PRIVATE_OPS_REQUIRE_FRESH_SMOKE_LOG: "1",
        COMMUNITY_DEMO_TOKEN: "community-private-token-123",
        FANTASY_PET_UPSTREAM_TOKEN: "agent-private-token-123"
      }
    })
  );
  const port = await listen(server);

  try {
    const unauthenticated = await fetch(
      `http://127.0.0.1:${port}/ops/private-ops-hooks-audit`
    );
    assert.equal(unauthenticated.status, 401);

    const credentials = Buffer.from("operator:private-password").toString("base64");
    const response = await fetch(
      `http://127.0.0.1:${port}/ops/private-ops-hooks-audit`,
      {
        headers: {
          Authorization: `Basic ${credentials}`
        }
      }
    );
    const text = await response.text();
    const body = JSON.parse(text);

    assert.equal(response.status, 200);
    assert.equal(body.schema, "desktop-pet.ops.target-hooks-audit.v1");
    assert.equal(body.ok, true);
    assert.equal(body.targetHooks.hooksMode, "user");
    assert.match(JSON.stringify(body.checks), /user-level scheduler is enabled/);
    assert.doesNotMatch(text, /private-password|community-private-token-123|agent-private-token-123/);
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
