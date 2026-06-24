import http from "node:http";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createGaPetReviewStore, decodeBody } from "./src/gaPetReviewStore.js";
import { createDatabaseConfig } from "../../services/community-api/src/database/config.js";
import { listCommunityMigrations } from "../../services/community-api/src/database/migrations.js";
import { createPgClientOptions } from "../../services/community-api/src/database/pg-options.js";
import { runCommunityMigrations } from "../../services/community-api/src/database/runner.js";

const rootDir = path.dirname(fileURLToPath(import.meta.url));
const publicDir = path.join(rootDir, "public");
const srcDir = path.join(rootDir, "src");

const contentTypes = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8"
};

const writeJson = (response, status, body) => {
  response.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  response.end(JSON.stringify(body));
};

const writeJsonWithHeaders = (response, status, headers, body) => {
  response.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    ...headers
  });
  response.end(JSON.stringify(body));
};

const readRequestBody = async (request) =>
  new Promise((resolve, reject) => {
    const chunks = [];
    request.on("data", (chunk) => chunks.push(chunk));
    request.on("end", () => resolve(Buffer.concat(chunks)));
    request.on("error", reject);
  });

const directProxyPrefixes = [
  "/health",
  "/v1/",
  "/pet-generation-jobs",
  "/worker-readiness",
  "/app-api-contract"
];

const directProxyEndpoints = [
  {
    method: "GET",
    pattern: /^\/admin\/pet-generation-jobs$/u
  },
  {
    method: "GET",
    pattern: /^\/admin\/pet-generation-jobs\/[^/]+\/review$/u
  }
];

const shouldProxyDirect = (method, pathname) =>
  directProxyEndpoints.some(
    (endpoint) => endpoint.method === method.toUpperCase() && endpoint.pattern.test(pathname)
  ) ||
  directProxyPrefixes.some((prefix) => {
    if (prefix.endsWith("/")) {
      return pathname.startsWith(prefix);
    }

    return pathname === prefix || pathname.startsWith(`${prefix}/`);
  });

const proxyTargetPath = (url) => {
  if (url.pathname.startsWith("/api/")) {
    return url.pathname.replace(/^\/api/, "") + url.search;
  }

  return url.pathname + url.search;
};

const trimString = (value) => (typeof value === "string" ? value.trim() : "");

const resolveBuiltInBasicAuth = (env) => {
  const user = trimString(env.PRIVATE_OPS_BASIC_AUTH_USER);
  const password = trimString(env.PRIVATE_OPS_BASIC_AUTH_PASSWORD);

  if (!user && !password) {
    return {
      enabled: false,
      misconfigured: false,
      user: "",
      password: ""
    };
  }

  return {
    enabled: Boolean(user && password),
    misconfigured: !user || !password,
    user,
    password
  };
};

const requestBasicAuthCredentials = (headers = {}) => {
  const header = trimString(headers.authorization);
  const match = /^Basic\s+(.+)$/iu.exec(header);
  if (!match) {
    return null;
  }

  try {
    const decoded = Buffer.from(match[1], "base64").toString("utf8");
    const separator = decoded.indexOf(":");
    if (separator < 0) {
      return null;
    }

    return {
      user: decoded.slice(0, separator),
      password: decoded.slice(separator + 1)
    };
  } catch {
    return null;
  }
};

const validateBuiltInBasicAuth = (request, response, basicAuth) => {
  if (!basicAuth.enabled && !basicAuth.misconfigured) {
    return false;
  }

  if (basicAuth.misconfigured) {
    writeJson(response, 503, {
      error: "admin_basic_auth_misconfigured"
    });
    return true;
  }

  const credentials = requestBasicAuthCredentials(request.headers);
  if (
    credentials?.user === basicAuth.user &&
    credentials.password === basicAuth.password
  ) {
    return false;
  }

  writeJsonWithHeaders(
    response,
    401,
    {
      "WWW-Authenticate": 'Basic realm="Desktop Pet Private Ops"'
    },
    {
      error: "admin_basic_auth_required"
    }
  );
  return true;
};

const proxyRequest = async (request, response, url, communityApiUrl, options = {}) => {
  const target = new URL(proxyTargetPath(url), communityApiUrl);
  const body = await readRequestBody(request);
  const headers = {
    "Content-Type": request.headers["content-type"] ?? "application/json"
  };
  const origin = trimString(request.headers.origin);
  const referer = trimString(request.headers.referer);
  const communityDemoToken = trimString(options.communityDemoToken);

  if (origin) {
    headers.Origin = origin;
  }
  if (referer) {
    headers.Referer = referer;
  }
  if (communityDemoToken) {
    headers["X-Demo-Token"] = communityDemoToken;
  }

  const upstream = await fetch(target, {
    method: request.method,
    headers,
    body: body.length > 0 ? body : undefined
  });

  response.writeHead(upstream.status, {
    "Content-Type": upstream.headers.get("content-type") ?? "application/json"
  });
  response.end(Buffer.from(await upstream.arrayBuffer()));
};

const safeJsonBody = (text) => {
  try {
    return text ? JSON.parse(text) : {};
  } catch {
    return {};
  }
};

const createDefaultOpsPgClient = async (config) => {
  const pg = await import("pg");
  const { Client } = pg.default ?? pg;
  return new Client(createPgClientOptions(config));
};

const databaseReadinessError = (response, status, body) => {
  writeJson(response, status, {
    schema: "desktop-pet.ops.community-db-readiness.v1",
    ok: false,
    ...body
  });
};

const handleCommunityDbReadiness = async (response, env, createOpsPgClient) => {
  const migrations = listCommunityMigrations();
  const migrationCount = migrations.length;
  let config;

  try {
    config = createDatabaseConfig(env);
  } catch {
    databaseReadinessError(response, 424, {
      database: {
        mode: "invalid",
        postgresConfigured: false,
        migrationCount
      },
      error: "database_url_invalid"
    });
    return;
  }

  if (config.mode !== "postgres") {
    databaseReadinessError(response, 424, {
      database: {
        mode: config.mode,
        postgresConfigured: false,
        migrationCount
      },
      error: "postgres_database_url_required"
    });
    return;
  }

  let client;
  try {
    client = await createOpsPgClient(config);
    await client.connect();
    const result = await runCommunityMigrations({
      client,
      migrations,
      dryRun: true
    });

    writeJson(response, 200, {
      schema: "desktop-pet.ops.community-db-readiness.v1",
      ok: true,
      database: {
        mode: "postgres",
        postgresConfigured: true,
        migrationCount,
        dryRun: {
          pending: result.pending.length,
          applied: result.applied.length,
          skipped: result.skipped.length
        }
      }
    });
  } catch {
    databaseReadinessError(response, 502, {
      database: {
        mode: "postgres",
        postgresConfigured: true,
        migrationCount
      },
      error: "postgres_migration_dry_run_failed"
    });
  } finally {
    try {
      await client?.end?.();
    } catch {
      // Closing a failed readiness probe must not change the reported cause.
    }
  }
};

const handleOpsRequest = async (request, response, url, options) => {
  if (request.method !== "GET") {
    return false;
  }

  if (url.pathname === "/ops/community-db-readiness") {
    await handleCommunityDbReadiness(
      response,
      options.env,
      options.createOpsPgClient ?? createDefaultOpsPgClient
    );
    return true;
  }

  if (url.pathname !== "/ops/internal-community-auth-check") {
    return false;
  }

  try {
    const upstream = await fetch(new URL("/v1/check-in", options.communityApiUrl), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json"
      },
      body: JSON.stringify({
        date: "2026-06-24"
      })
    });
    const body = safeJsonBody(await upstream.text());
    const error = typeof body.error === "string" ? body.error : "";
    const ok = upstream.status === 401 && error === "unauthorized_demo_request";

    writeJson(response, ok ? 200 : 502, {
      schema: "desktop-pet.ops.internal-community-auth-check.v1",
      ok,
      communityWriteWithoutToken: {
        status: upstream.status,
        error
      }
    });
  } catch {
    writeJson(response, 502, {
      schema: "desktop-pet.ops.internal-community-auth-check.v1",
      ok: false,
      error: "community_api_unreachable"
    });
  }

  return true;
};

const handleGaReviewRequest = async (request, response, url, store) => {
  if (request.method === "GET" && url.pathname === "/ga-review/candidates") {
    const limit = Number.parseInt(url.searchParams.get("limit") || "40", 10);
    writeJson(response, 200, await store.listCandidates({ limit }));
    return true;
  }

  if (request.method === "GET" && url.pathname.startsWith("/ga-review/files/")) {
    const runId = decodeURIComponent(url.pathname.slice("/ga-review/files/".length));
    const relativePath = url.searchParams.get("path") || "";
    const asset = await store.readAsset({ runId, relativePath });
    response.writeHead(200, {
      "Content-Type": asset.contentType,
      "Cache-Control": "no-store"
    });
    response.end(asset.file);
    return true;
  }

  const feedbackPrefix = "/ga-review/candidates/";
  if (
    request.method === "POST" &&
    url.pathname.startsWith(feedbackPrefix) &&
    url.pathname.endsWith("/feedback")
  ) {
    const runId = decodeURIComponent(
      url.pathname.slice(feedbackPrefix.length, -"/feedback".length)
    );
    const body = decodeBody(await readRequestBody(request));
    writeJson(response, 200, await store.writeFeedback({ runId, body }));
    return true;
  }

  return false;
};

const resolveStaticPath = (url) => {
  if (url.pathname.startsWith("/src/")) {
    return path.join(srcDir, url.pathname.slice("/src/".length));
  }

  const relativePath = url.pathname === "/" ? "index.html" : url.pathname.slice(1);
  return path.join(publicDir, relativePath);
};

const isInside = (target, parent) => {
  const relative = path.relative(parent, target);
  return relative === "" || (!relative.startsWith("..") && !path.isAbsolute(relative));
};

export function createAdminReviewHttpHandler(options = {}) {
  const env = options.env ?? process.env;
  const communityApiUrl = options.communityApiUrl ?? "http://127.0.0.1:4000";
  const communityDemoToken =
    options.communityDemoToken ?? env.COMMUNITY_DEMO_TOKEN ?? "";
  const builtInBasicAuth = resolveBuiltInBasicAuth(env);
  const gaPetReviewStore = createGaPetReviewStore({
    runRoot: options.gaPetRunRoot
  });

  return async (request, response) => {
    try {
      const url = new URL(request.url ?? "/", "http://localhost");

      if (
        url.pathname !== "/health" &&
        validateBuiltInBasicAuth(request, response, builtInBasicAuth)
      ) {
        return;
      }

      if (url.pathname.startsWith("/ga-review/")) {
        const handled = await handleGaReviewRequest(
          request,
          response,
          url,
          gaPetReviewStore
        );
        if (!handled) {
          writeJson(response, 404, { error: "not_found" });
        }
        return;
      }

      if (url.pathname.startsWith("/ops/")) {
        const handled = await handleOpsRequest(request, response, url, {
          communityApiUrl,
          createOpsPgClient: options.createOpsPgClient,
          env
        });
        if (!handled) {
          writeJson(response, 404, { error: "not_found" });
        }
        return;
      }

      if (url.pathname.startsWith("/api/") || shouldProxyDirect(request.method ?? "GET", url.pathname)) {
        await proxyRequest(request, response, url, communityApiUrl, {
          communityDemoToken
        });
        return;
      }

      const filePath = resolveStaticPath(url);
      const allowedRoot = url.pathname.startsWith("/src/") ? srcDir : publicDir;

      if (!isInside(filePath, allowedRoot)) {
        writeJson(response, 403, { error: "forbidden" });
        return;
      }

      const file = await readFile(filePath);
      response.writeHead(200, {
        "Content-Type": contentTypes[path.extname(filePath)] ?? "application/octet-stream"
      });
      response.end(file);
    } catch (error) {
      if (error?.code === "ENOENT") {
        writeJson(response, 404, { error: "not_found" });
        return;
      }

      writeJson(response, 500, {
        error: "internal_error",
        message: error instanceof Error ? error.message : "Unknown error"
      });
    }
  };
}

export function startAdminReviewServer(options = {}) {
  const env = options.env ?? process.env;
  const port = Number.parseInt(options.port ?? env.PORT ?? "4200", 10);
  const communityApiUrl =
    options.communityApiUrl ?? env.COMMUNITY_API_URL ?? "http://127.0.0.1:4000";
  const gaPetRunRoot = options.gaPetRunRoot ?? env.GA_PET_RUN_ROOT;
  const server = http.createServer(
    createAdminReviewHttpHandler({
      env,
      communityApiUrl,
      gaPetRunRoot
    })
  );

  server.listen(port, "0.0.0.0", () => {
    console.log(`admin-review listening on ${port}`);
  });

  return server;
}

const isDirectRun =
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isDirectRun) {
  startAdminReviewServer();
}
