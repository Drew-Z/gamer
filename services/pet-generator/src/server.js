import http from "node:http";
import { summarizeFantasyPetRuleState } from "./adapter.js";

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

const server = http.createServer(async (request, response) => {
  const url = new URL(request.url ?? "/", "http://localhost");

  if (request.method === "GET" && url.pathname === "/health") {
    response.writeHead(200, { "Content-Type": "application/json" });
    response.end(JSON.stringify({ ok: true, service: "pet-generator" }));
    return;
  }

  if (request.method === "POST" && url.pathname === "/v1/fantasy-pet-rule/summarize") {
    try {
      const body = await readBody(request);
      const payload = body ? JSON.parse(body) : {};
      const summary = summarizeFantasyPetRuleState(payload.state);

      response.writeHead(200, { "Content-Type": "application/json" });
      response.end(JSON.stringify(summary));
    } catch (error) {
      response.writeHead(400, { "Content-Type": "application/json" });
      response.end(
        JSON.stringify({
          error: "invalid_request",
          message: error instanceof Error ? error.message : "Unable to parse request"
        })
      );
    }
    return;
  }

  response.writeHead(404, { "Content-Type": "application/json" });
  response.end(JSON.stringify({ error: "not_found", path: url.pathname }));
});

server.listen(port, "0.0.0.0", () => {
  console.log(`pet-generator listening on ${port}`);
});
