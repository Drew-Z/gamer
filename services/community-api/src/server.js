import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
  isFantasyPetPublicProxyRequest,
  proxyFantasyPetPublicRequest
} from "./fantasy-pet-proxy.js";
import { createFileBackedCommunityStore } from "./file-store.js";
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
  return async (request, response) => {
    try {
      const rawBody = await readBody(request);
      const method = request.method ?? "GET";
      const requestUrl = request.url ?? "/";

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
  const port = resolveCommunityApiPort(options.env);
  const store =
    options.store ??
    createFileBackedCommunityStore({
      env: options.env
    });
  const server = http.createServer(
    createCommunityHttpHandler({
      env: options.env,
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
