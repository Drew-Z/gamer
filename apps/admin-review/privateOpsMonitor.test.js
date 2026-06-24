import assert from "node:assert/strict";
import test from "node:test";
import {
  createPrivateOpsMonitor,
  resolvePrivateOpsMonitorConfig
} from "./privateOpsMonitor.js";

const jsonResponse = (status, body, contentType = "application/json") => ({
  status,
  headers: {
    get(name) {
      return name.toLowerCase() === "content-type" ? contentType : "";
    }
  },
  async text() {
    return JSON.stringify(body);
  },
  async arrayBuffer() {
    return Buffer.from(JSON.stringify(body));
  }
});

test("private ops monitor defaults on for community deployments", () => {
  assert.deepEqual(
    resolvePrivateOpsMonitorConfig({
      PRIVATE_OPS_DEPLOYMENT_ROLE: "community"
    }),
    {
      enabled: true,
      intervalMs: 300000,
      staleAfterMs: 900000,
      historyLimit: 12,
      knownAppJobId: ""
    }
  );
  assert.equal(
    resolvePrivateOpsMonitorConfig({
      PRIVATE_OPS_DEPLOYMENT_ROLE: "community",
      PRIVATE_OPS_MONITOR_ENABLED: "0"
    }).enabled,
    false
  );
});

test("private ops monitor runs synthetic checks and records bounded status", async () => {
  const requests = [];
  const logs = [];
  const monitor = createPrivateOpsMonitor({
    env: {
      PRIVATE_OPS_DEPLOYMENT_ROLE: "community",
      PRIVATE_OPS_MONITOR_KNOWN_APP_JOB_ID: "known job/001"
    },
    communityApiUrl: "http://community.internal:4000",
    now: (() => {
      const values = [
        new Date("2026-06-24T10:00:00.000Z"),
        new Date("2026-06-24T10:00:01.250Z"),
        new Date("2026-06-24T10:00:01.250Z")
      ];
      return () => values.shift() ?? new Date("2026-06-24T10:00:01.250Z");
    })(),
    writeLog(entry) {
      logs.push(entry);
    },
    async fetchImpl(url, options = {}) {
      const parsed = new URL(String(url));
      requests.push({
        method: options.method ?? "GET",
        path: `${parsed.pathname}${parsed.search}`,
        headers: options.headers ?? {}
      });

      if (parsed.pathname === "/health") {
        return jsonResponse(200, {
          ok: true,
          service: "community-api"
        });
      }
      if (parsed.pathname === "/v1/sla") {
        return jsonResponse(200, {
          schema: "gamer.sla.v1"
        });
      }
      if (parsed.pathname === "/worker-readiness") {
        return jsonResponse(200, {
          schema: "fantasy-pet.worker-readiness-public.v1"
        });
      }
      if (parsed.pathname === "/app-api-contract") {
        return jsonResponse(200, {
          schema: "fantasy-pet.app-api-contract.v1"
        });
      }
      if (parsed.pathname === "/v1/check-in") {
        return jsonResponse(401, {
          error: "unauthorized_demo_request"
        });
      }
      if (parsed.pathname === "/pet-generation-jobs/known%20job%2F001/package") {
        return jsonResponse(409, {
          schema: "fantasy-pet.package-download-response.v1",
          status: "blocked"
        });
      }

      return jsonResponse(404, {
        error: "not_found"
      });
    }
  });

  const run = await monitor.runOnce();
  const status = monitor.getStatus();

  assert.equal(run.ok, true);
  assert.equal(run.status, "pass");
  assert.equal(run.checks.length, 6);
  assert.deepEqual(status, {
    schema: "desktop-pet.ops.private-ops-monitor-status.v1",
    ok: true,
    monitor: {
      enabled: true,
      intervalMs: 300000,
      staleAfterMs: 900000,
      historyLimit: 12,
      knownPackageGate: true,
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
      durationMs: 1250,
      checks: [
        { name: "community health is public-safe", status: "pass" },
        { name: "community SLA is readable", status: "pass" },
        { name: "agent worker readiness is proxied", status: "pass" },
        { name: "agent app API contract is proxied", status: "pass" },
        { name: "internal community auth rejects missing token", status: "pass" },
        { name: "known job package gate is observable", status: "pass" }
      ]
    },
    history: [
      {
        at: "2026-06-24T10:00:00.000Z",
        ok: true,
        status: "pass",
        durationMs: 1250,
        checkCount: 6
      }
    ]
  });
  assert.equal(logs.length, 1);
  assert.equal(logs[0].schema, "desktop-pet.ops.private-ops-monitor-log.v1");
  assert.equal(JSON.stringify(status).includes("community.internal"), false);
  assert(
    requests.some(
      (request) =>
        request.method === "POST" &&
        request.path === "/v1/check-in" &&
        request.headers.authorization === undefined &&
        request.headers["x-demo-token"] === undefined
    )
  );
});
