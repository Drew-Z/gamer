import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";
import {
  createFantasyPetRuleImportSummary,
  summarizeFantasyPetRuleState
} from "./adapter.js";
import {
  resolveFantasyPetRuleState,
  StateSourceError
} from "./state-source.js";

const port = Number.parseInt(process.env.PORT ?? "4100", 10);

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

export function createPetGeneratorHttpHandler(options = {}) {
  return async (request, response) => {
    try {
      const url = new URL(request.url ?? "/", "http://localhost");

      if (request.method === "GET" && url.pathname === "/health") {
        writeJson(response, 200, { ok: true, service: "pet-generator" });
        return;
      }

      const rawBody = await readBody(request);
      let payload = {};

      if (rawBody.trim() !== "") {
        try {
          payload = JSON.parse(rawBody);
        } catch {
          writeJson(response, 400, {
            error: "invalid_json"
          });
          return;
        }
      }

      if (request.method === "POST" && url.pathname === "/v1/fantasy-pet-rule/summarize") {
        const state = await resolveFantasyPetRuleState(payload, options);
        const summary = summarizeFantasyPetRuleState(state);

        writeJson(response, 200, summary);
        return;
      }

      if (request.method === "POST" && url.pathname === "/v1/fantasy-pet-rule/import-summary") {
        const state = await resolveFantasyPetRuleState(payload, options);
        const summary = createFantasyPetRuleImportSummary(state);

        writeJson(response, 200, summary);
        return;
      }

      writeJson(response, 404, { error: "not_found", path: url.pathname });
    } catch (error) {
      if (error instanceof StateSourceError) {
        writeJson(response, error.status, {
          error: error.code,
          message: error.message
        });
        return;
      }

      writeJson(response, 500, {
        error: "internal_error",
        message: error instanceof Error ? error.message : "Unknown server error"
      });
    }
  };
}

const isDirectRun =
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isDirectRun) {
  const server = http.createServer(createPetGeneratorHttpHandler());
  server.listen(port, "0.0.0.0", () => {
    console.log(`pet-generator listening on ${port}`);
  });
}
