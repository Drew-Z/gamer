import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
  isFantasyPetPublicProxyRequest,
  proxyFantasyPetPublicRequest
} from "./fantasy-pet-proxy.js";
import { createConfiguredCommunityStore } from "./configured-store.js";
import { requireCommunityDemoAuth } from "./demo-auth.js";
import { formatRequestLog } from "./logging.js";
import { createRateLimiterPolicyFromEnv, resolveClientIp } from "./rate-limit.js";
import { handleCommunityRequest } from "./routes.js";
import { handleMetricsRequest } from "./metrics.js";

export function resolveCommunityApiPort(env = process.env) {
  return Number.parseInt(env.PORT ?? env.SERVER_PORT ?? "4000", 10);
}

const readBody = (request) =>
  new Promise((resolve, reject) => {
    let body = "";
    request.on("data", (chunk) => {
      body += chunk;
    });
    request.on("end", () => resolve(body));
    request.on("error", reject);
  });

const writeJson = (response, status, body) => {
  response.writeHead(status, {
    "Content-Type": "application/json"
  });
  response.end(JSON.stringify(body));
};

const writeRaw = (response, result) => {
  response.writeHead(result.status, result.headers);
  response.end(result.body);
};

const unsafeRequestMethods = new Set(["DELETE", "PATCH", "POST", "PUT"]);

function splitOriginList(value) {
  return String(value ?? "")
    .split(",")
    .map((origin) => origin.trim())
    .filter(Boolean);
}

function headerValue(headers = {}, name) {
  const value = headers[name] ?? headers[name.toLowerCase()] ?? headers[name.toUpperCase()];

  if (Array.isArray(value)) {
    return value.join(", ");
  }

  return typeof value === "string" ? value : "";
}

function trustedAdminReviewOrigins(env) {
  const explicit = splitOriginList(env.COMMUNITY_ADMIN_REVIEW_TRUSTED_ORIGINS);
  return explicit.length > 0 ? explicit : splitOriginList(env.COMMUNITY_CORS_ALLOWED_ORIGINS);
}

function originFromReferer(value) {
  if (!value) {
    return "";
  }

  try {
    return new URL(value).origin;
  } catch {
    return "";
  }
}

function isAdminReviewWrite(method, requestUrl) {
  const url = new URL(requestUrl, "http://localhost");
  return method.toUpperCase() === "POST" && url.pathname === "/v1/admin/reviews";
}

function validateAdminReviewOrigin(method, requestUrl, headers, env) {
  if (!isAdminReviewWrite(method, requestUrl)) {
    return null;
  }

  const allowedOrigins = trustedAdminReviewOrigins(env);
  if (allowedOrigins.length === 0) {
    return null;
  }

  const origin = headerValue(headers, "origin").trim();
  if (origin) {
    return allowedOrigins.includes(origin)
      ? null
      : {
          error: "admin_origin_not_allowed"
        };
  }

  const refererOrigin = originFromReferer(headerValue(headers, "referer").trim());
  if (refererOrigin) {
    return allowedOrigins.includes(refererOrigin)
      ? null
      : {
          error: "admin_origin_not_allowed"
        };
  }

  return {
    error: "admin_origin_required"
  };
}

export function createCorsHeaders(method, headers = {}, env = process.env) {
  const allowedOrigins = splitOriginList(env.COMMUNITY_CORS_ALLOWED_ORIGINS);
  if (allowedOrigins.length === 0) {
    return {};
  }

  const origin = headerValue(headers, "origin").trim();
  if (!origin || !allowedOrigins.includes(origin)) {
    return {};
  }

  const requestedHeaders = String(headers["access-control-request-headers"] ?? "").trim();
  return {
    "Access-Control-Allow-Origin": origin,
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Allow-Headers": requestedHeaders || "Content-Type, Authorization, X-Demo-Token",
    "Access-Control-Max-Age": "600",
    Vary: "Origin"
  };
}

function writeCorsPreflight(response, method, headers, env) {
  if (method !== "OPTIONS") {
    return false;
  }

  const corsHeaders = createCorsHeaders(method, headers, env);
  if (corsHeaders["Access-Control-Allow-Origin"]) {
    response.writeHead(204, corsHeaders);
    response.end();
    return true;
  }

  if (String(env.COMMUNITY_CORS_ALLOWED_ORIGINS ?? "").trim()) {
    writeJson(response, 403, {
      error: "cors_origin_not_allowed"
    });
    return true;
  }

  return false;
}

export function createCommunityHttpHandler(options = {}) {
  const env = options.env ?? process.env;
  const rateLimit = options.rateLimit ?? createRateLimiterPolicyFromEnv(env);
  const log =
    options.log ??
    (process.env.NODE_ENV === "test"
      ? null
      : (entry) => console.log(formatRequestLog(entry)));

  return async (request, response) => {
    const started = Date.now();
    const method = request.method ?? "GET";
    const requestUrl = request.url ?? "/";

    response.on("finish", () => {
      try {
        log?.({
          method,
          path: requestUrl,
          status: response.statusCode,
          durationMs: Date.now() - started
        });
      } catch {
        // Logging must never affect a response that has already been sent.
      }
    });

    try {
      if (rateLimit) {
        const pathname = new URL(requestUrl, "http://localhost").pathname;
        if (!rateLimit.isExempt(pathname)) {
          const decision = rateLimit.check(method, resolveClientIp(request));
          if (decision.limited) {
            response.writeHead(429, {
              "Content-Type": "application/json",
              "Retry-After": String(Math.max(1, Math.ceil(decision.retryAfterMs / 1000)))
            });
            response.end(
              JSON.stringify({
                error: "rate_limit_exceeded",
                retryAfterMs: decision.retryAfterMs
              })
            );
            return;
          }
        }
      }

      const corsHeaders = createCorsHeaders(method, request.headers, env);
      for (const [name, value] of Object.entries(corsHeaders)) {
        response.setHeader(name, value);
      }
      if (writeCorsPreflight(response, method, request.headers, env)) {
        return;
      }
      if (
        unsafeRequestMethods.has(method.toUpperCase()) &&
        String(env.COMMUNITY_CORS_ALLOWED_ORIGINS ?? "").trim() &&
        headerValue(request.headers, "origin").trim() &&
        !corsHeaders["Access-Control-Allow-Origin"]
      ) {
        writeJson(response, 403, {
          error: "cors_origin_not_allowed"
        });
        return;
      }
      const adminOriginError = validateAdminReviewOrigin(method, requestUrl, request.headers, env);
      if (adminOriginError) {
        writeJson(response, 403, adminOriginError);
        return;
      }

      const authError = requireCommunityDemoAuth(method, requestUrl, request.headers, {
        env,
        communityDemoToken: options.communityDemoToken
      });
      if (authError) {
        writeRaw(response, authError);
        return;
      }

      const rawBody = await readBody(request);

      if (isFantasyPetPublicProxyRequest(method, requestUrl)) {
        const result = await proxyFantasyPetPublicRequest(method, requestUrl, {
          env: options.env,
          fantasyPetApiBaseUrl: options.fantasyPetApiBaseUrl,
          fantasyPetUpstreamToken: options.fantasyPetUpstreamToken,
          fetch: options.fetch,
          headers: request.headers,
          rawBody
        });
        writeRaw(response, result);
        return;
      }

      let body = {};

      if (rawBody.trim() !== "") {
        try {
          body = JSON.parse(rawBody);
        } catch {
          writeJson(response, 400, {
            error: "invalid_json"
          });
          return;
        }
      }

      const result = await handleCommunityRequest(
        method,
        requestUrl,
        {
          ...options,
          body
        }
      );

      // Handle metrics endpoint specially
      if (result.isMetrics) {
        handleMetricsRequest(request, response);
        return;
      }

      writeJson(response, result.status, result.body);
    } catch (error) {
      writeJson(response, 500, {
        error: "internal_error",
        message: error instanceof Error ? error.message : "Unknown server error"
      });
    }
  };
}

export function startCommunityApiServer(options = {}) {
  const env = options.env ?? process.env;
  const port = resolveCommunityApiPort(env);
  const store =
    options.store ??
    createConfiguredCommunityStore({
      env
    });
  const server = http.createServer(
    createCommunityHttpHandler({
      env,
      fantasyPetApiBaseUrl: options.fantasyPetApiBaseUrl,
      fantasyPetUpstreamToken: options.fantasyPetUpstreamToken,
      communityDemoToken: options.communityDemoToken,
      rateLimit: options.rateLimit,
      store
    })
  );

  server.listen(port, "0.0.0.0", () => {
    console.log(`community-api listening on ${port}`);
  });

  return server;
}

const isDirectRun =
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isDirectRun) {
  startCommunityApiServer();
}
