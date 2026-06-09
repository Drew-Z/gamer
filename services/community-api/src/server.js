import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";
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

export function createCommunityHttpHandler(options = {}) {
  return async (request, response) => {
    try {
      const rawBody = await readBody(request);
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
        request.method ?? "GET",
        request.url ?? "/",
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
  const server = http.createServer(
    createCommunityHttpHandler({
      store: options.store
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
