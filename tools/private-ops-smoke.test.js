import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const repoRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);

test("private ops smoke checks a configured known job package gate", async () => {
  const requests = [];
  const server = http.createServer((request, response) => {
    requests.push({
      method: request.method,
      url: request.url,
      headers: request.headers
    });

    const sendJson = (status, body) => {
      response.writeHead(status, {
        "Content-Type": "application/json"
      });
      response.end(JSON.stringify(body));
    };

    if (request.method === "GET" && request.url === "/health") {
      sendJson(200, {
        ok: true,
        service: "community-api"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/v1/sla") {
      sendJson(200, {
        schema: "gamer.sla.v1"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/worker-readiness") {
      sendJson(200, {
        schema: "fantasy-pet.worker-readiness-public.v1"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/app-api-contract") {
      sendJson(200, {
        schema: "fantasy-pet.app-api-contract.v1"
      });
      return;
    }

    if (request.method === "POST" && request.url === "/v1/check-in") {
      if (request.headers["x-demo-token"] !== "community-token-123") {
        sendJson(401, {
          error: "unauthorized_demo_request"
        });
        return;
      }

      sendJson(200, {
        checkIn: {
          date: "2026-06-24"
        }
      });
      return;
    }

    if (request.method === "POST" && request.url === "/pet-generation-jobs") {
      sendJson(401, {
        error: "unauthorized_demo_request"
      });
      return;
    }

    if (
      request.method === "GET" &&
      request.url === "/pet-generation-jobs/known%20job%2F001/package"
    ) {
      sendJson(409, {
        schema: "fantasy-pet.package-download-response.v1",
        status: "blocked",
        errors: ["package artifact not found"]
      });
      return;
    }

    sendJson(404, {
      error: "not_found"
    });
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const result = await runSmoke({
      COMMUNITY_BASE_URL: `http://127.0.0.1:${server.address().port}`,
      COMMUNITY_DEMO_TOKEN: "community-token-123",
      FANTASY_PET_UPSTREAM_TOKEN: "upstream-token-123",
      PRIVATE_OPS_KNOWN_APP_JOB_ID: "known job/001"
    });

    assert.equal(result.exitCode, 0, result.stderr);
    const output = JSON.parse(result.stdout);
    assert.equal(output.checkedKnownPackageGate, true);
    assert(
      output.checks.some(
        (entry) =>
          entry.name === "known job package gate is observable" &&
          entry.status === "pass",
      ),
    );
    assert(
      requests.some(
        (request) =>
          request.method === "GET" &&
          request.url === "/pet-generation-jobs/known%20job%2F001/package",
      ),
    );
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("private ops smoke supports admin-review basic auth surface", async () => {
  const requests = [];
  const validBasicAuth = `Basic ${Buffer.from("operator:private-password").toString("base64")}`;
  const server = http.createServer((request, response) => {
    requests.push({
      method: request.method,
      url: request.url,
      headers: request.headers
    });

    const sendJson = (status, body) => {
      response.writeHead(status, {
        "Content-Type": "application/json"
      });
      response.end(JSON.stringify(body));
    };

    if (request.method === "GET" && request.url === "/health") {
      sendJson(200, {
        ok: true,
        service: "community-api"
      });
      return;
    }

    if (request.headers.authorization !== validBasicAuth) {
      sendJson(401, {
        error: "admin_basic_auth_required"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/v1/sla") {
      sendJson(200, {
        schema: "gamer.sla.v1"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/worker-readiness") {
      sendJson(200, {
        schema: "fantasy-pet.worker-readiness-public.v1"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/app-api-contract") {
      sendJson(200, {
        schema: "fantasy-pet.app-api-contract.v1"
      });
      return;
    }

    if (request.method === "POST" && request.url === "/v1/check-in") {
      sendJson(200, {
        checkIn: {
          date: "2026-06-24"
        }
      });
      return;
    }

    if (request.method === "GET" && request.url === "/ops/internal-community-auth-check") {
      sendJson(200, {
        schema: "desktop-pet.ops.internal-community-auth-check.v1",
        ok: true,
        communityWriteWithoutToken: {
          status: 401,
          error: "unauthorized_demo_request"
        }
      });
      return;
    }

    sendJson(404, {
      error: "not_found"
    });
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const result = await runSmoke({
      COMMUNITY_BASE_URL: `http://127.0.0.1:${server.address().port}`,
      COMMUNITY_DEMO_TOKEN: "community-token-123",
      FANTASY_PET_UPSTREAM_TOKEN: "upstream-token-123",
      PRIVATE_OPS_BASIC_AUTH_USER: "operator",
      PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password",
      PRIVATE_OPS_SMOKE_SURFACE: "admin-review"
    });

    assert.equal(result.exitCode, 0, result.stderr);
    const output = JSON.parse(result.stdout);
    assert.equal(output.smokeSurface, "admin-review");
    assert(
      output.checks.some(
        (entry) =>
          entry.name === "admin-review rejects missing basic auth" &&
          entry.status === "pass",
      ),
    );
    assert(
      output.checks.some(
        (entry) =>
          entry.name === "internal community auth rejects missing token" &&
          entry.status === "pass",
      ),
    );
    assert(
      requests.some(
        (request) =>
          request.method === "POST" &&
          request.url === "/v1/check-in" &&
          request.headers.authorization === validBasicAuth &&
          request.headers["x-demo-token"] === undefined,
      ),
    );
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("private ops smoke can require admin-review postgres readiness", async () => {
  const requests = [];
  const validBasicAuth = `Basic ${Buffer.from("operator:private-password").toString("base64")}`;
  const server = http.createServer((request, response) => {
    requests.push({
      method: request.method,
      url: request.url,
      headers: request.headers
    });

    const sendJson = (status, body) => {
      response.writeHead(status, {
        "Content-Type": "application/json"
      });
      response.end(JSON.stringify(body));
    };

    if (request.method === "GET" && request.url === "/health") {
      sendJson(200, {
        ok: true,
        service: "community-api"
      });
      return;
    }

    if (request.headers.authorization !== validBasicAuth) {
      sendJson(401, {
        error: "admin_basic_auth_required"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/v1/sla") {
      sendJson(200, {
        schema: "gamer.sla.v1"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/worker-readiness") {
      sendJson(200, {
        schema: "fantasy-pet.worker-readiness-public.v1"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/app-api-contract") {
      sendJson(200, {
        schema: "fantasy-pet.app-api-contract.v1"
      });
      return;
    }

    if (request.method === "POST" && request.url === "/v1/check-in") {
      sendJson(200, {
        checkIn: {
          date: "2026-06-24"
        }
      });
      return;
    }

    if (request.method === "GET" && request.url === "/ops/internal-community-auth-check") {
      sendJson(200, {
        schema: "desktop-pet.ops.internal-community-auth-check.v1",
        ok: true,
        communityWriteWithoutToken: {
          status: 401,
          error: "unauthorized_demo_request"
        }
      });
      return;
    }

    if (request.method === "GET" && request.url === "/ops/community-db-readiness") {
      sendJson(200, {
        schema: "desktop-pet.ops.community-db-readiness.v1",
        ok: true,
        database: {
          mode: "postgres",
          postgresConfigured: true,
          migrationCount: 5,
          dryRun: {
            pending: 0,
            applied: 0,
            skipped: 5
          }
        }
      });
      return;
    }

    sendJson(404, {
      error: "not_found"
    });
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const result = await runSmoke({
      COMMUNITY_BASE_URL: `http://127.0.0.1:${server.address().port}`,
      COMMUNITY_DEMO_TOKEN: "community-token-123",
      FANTASY_PET_UPSTREAM_TOKEN: "upstream-token-123",
      PRIVATE_OPS_BASIC_AUTH_USER: "operator",
      PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password",
      PRIVATE_OPS_REQUIRE_POSTGRES: "1",
      PRIVATE_OPS_SMOKE_SURFACE: "admin-review"
    });

    assert.equal(result.exitCode, 0, result.stderr);
    const output = JSON.parse(result.stdout);
    assert.equal(output.requiredPostgres, true);
    assert(
      output.checks.some(
        (entry) =>
          entry.name === "community postgres readiness is verified" &&
          entry.status === "pass",
      ),
    );
    assert(
      requests.some(
        (request) =>
          request.method === "GET" &&
          request.url === "/ops/community-db-readiness" &&
          request.headers.authorization === validBasicAuth,
      ),
    );
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("private ops smoke rejects basic auth password leaks", async () => {
  const validBasicAuth = `Basic ${Buffer.from("operator:private-password").toString("base64")}`;
  const server = http.createServer((request, response) => {
    const sendJson = (status, body) => {
      response.writeHead(status, {
        "Content-Type": "application/json"
      });
      response.end(JSON.stringify(body));
    };

    if (request.method === "GET" && request.url === "/health") {
      sendJson(200, {
        ok: true,
        service: "community-api"
      });
      return;
    }

    if (request.headers.authorization !== validBasicAuth) {
      sendJson(401, {
        error: "admin_basic_auth_required"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/v1/sla") {
      sendJson(200, {
        schema: "gamer.sla.v1",
        leaked: "private-password"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/worker-readiness") {
      sendJson(200, {
        schema: "fantasy-pet.worker-readiness-public.v1"
      });
      return;
    }

    if (request.method === "GET" && request.url === "/app-api-contract") {
      sendJson(200, {
        schema: "fantasy-pet.app-api-contract.v1"
      });
      return;
    }

    if (request.method === "POST" && request.url === "/v1/check-in") {
      sendJson(200, {
        checkIn: {
          date: "2026-06-24"
        }
      });
      return;
    }

    sendJson(404, {
      error: "not_found"
    });
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));

  try {
    const result = await runSmoke({
      COMMUNITY_BASE_URL: `http://127.0.0.1:${server.address().port}`,
      COMMUNITY_DEMO_TOKEN: "community-token-123",
      FANTASY_PET_UPSTREAM_TOKEN: "upstream-token-123",
      PRIVATE_OPS_BASIC_AUTH_USER: "operator",
      PRIVATE_OPS_BASIC_AUTH_PASSWORD: "private-password",
      PRIVATE_OPS_SMOKE_SURFACE: "admin-review"
    });

    assert.equal(result.exitCode, 1);
    assert.match(result.stderr, /response leaked a configured secret fragment/);
    assert.doesNotMatch(result.stderr, /private-password/);
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

function runSmoke(env) {
  return new Promise((resolve, reject) => {
    const child = spawn(
      process.execPath,
      [path.join(repoRoot, "tools/private-ops-smoke.js")],
      {
        cwd: repoRoot,
        env: {
          ...process.env,
          ...env
        },
        stdio: ["ignore", "pipe", "pipe"]
      },
    );
    let stdout = "";
    let stderr = "";

    child.stdout.setEncoding("utf8");
    child.stderr.setEncoding("utf8");
    child.stdout.on("data", (chunk) => {
      stdout += chunk;
    });
    child.stderr.on("data", (chunk) => {
      stderr += chunk;
    });
    child.on("error", reject);
    child.on("close", (exitCode) => {
      resolve({
        exitCode,
        stdout,
        stderr
      });
    });
  });
}
