import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
  isFantasyPetPublicProxyRequest,
  proxyFantasyPetPublicRequest
} from "./fantasy-pet-proxy.js";
import { createConfiguredCommunityStore } from "./configured-store.js";
import { formatRequestLog } from "./logging.js";
import { resolveClientIp } from "./rate-limit.js";
import { handleCommunityRequest } from "./routes.js";

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

export function createCommunityHttpHandler(options = {}) {
  const env = options.env ?? process.env;
  const log =
    options.log ??
    (process.env.NODE_ENV === "test"
      ? null
      : (entry) => console.log(formatRequestLog(entry)));

  return async (request, response) => {
    const started = Date.now();
    const method = request.method ?? "GET";
    const requestUrl = request.url ?? "/";
    const rateLimit = options.rateLimit ?? null;

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

      const rawBody = await readBody(request);

      if (isFantasyPetPublicProxyRequest(method, requestUrl)) {
        const result = await proxyFantasyPetPublicRequest(method, requestUrl, {
          env: options.env,
          fantasyPetApiBaseUrl: options.fantasyPetApiBaseUrl,
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
