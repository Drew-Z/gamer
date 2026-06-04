import http from "node:http";
import { handleCommunityRequest } from "./routes.js";

const port = Number.parseInt(process.env.PORT ?? "4000", 10);

const server = http.createServer((request, response) => {
  const result = handleCommunityRequest(request.method ?? "GET", request.url ?? "/");

  response.writeHead(result.status, {
    "Content-Type": "application/json"
  });
  response.end(JSON.stringify(result.body));
});

server.listen(port, "0.0.0.0", () => {
  console.log(`community-api listening on ${port}`);
});
