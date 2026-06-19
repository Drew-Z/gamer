import http from "node:http";
import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { createGaPetReviewStore, decodeBody } from "./src/gaPetReviewStore.js";

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

const readRequestBody = async (request) =>
  new Promise((resolve, reject) => {
    const chunks = [];
    request.on("data", (chunk) => chunks.push(chunk));
    request.on("end", () => resolve(Buffer.concat(chunks)));
    request.on("error", reject);
  });

const communityProxyPrefixes = [
  "/health",
  "/v1/"
];

const fantasyPetProxyPrefixes = [
  "/pet-generation-jobs",
  "/worker-readiness",
  "/app-api-contract"
];

const fantasyPetProxyEndpoints = [
  {
    method: "GET",
    pattern: /^\/admin\/pet-generation-jobs$/u
  },
  {
    method: "GET",
    pattern: /^\/admin\/pet-generation-jobs\/[^/]+\/review$/u
  }
];

const matchesProxyPrefix = (prefixes, pathname) =>
  prefixes.some((prefix) => {
    if (prefix.endsWith("/")) {
      return pathname.startsWith(prefix);
    }

    return pathname === prefix || pathname.startsWith(`${prefix}/`);
  });

const shouldProxyFantasyPet = (method, pathname) =>
  fantasyPetProxyEndpoints.some(
    (endpoint) => endpoint.method === method.toUpperCase() && endpoint.pattern.test(pathname)
  ) || matchesProxyPrefix(fantasyPetProxyPrefixes, pathname);

const shouldProxyCommunity = (pathname) => matchesProxyPrefix(communityProxyPrefixes, pathname);

const proxyTargetPath = (url) => {
  if (url.pathname.startsWith("/api/")) {
    return url.pathname.replace(/^\/api/, "") + url.search;
  }

  return url.pathname + url.search;
};

const proxyRequest = async (request, response, url, communityApiUrl) => {
  const target = new URL(proxyTargetPath(url), communityApiUrl);
  const body = await readRequestBody(request);

  const upstream = await fetch(target, {
    method: request.method,
    headers: {
      "Content-Type": request.headers["content-type"] ?? "application/json"
    },
    body: body.length > 0 ? body : undefined
  });

  response.writeHead(upstream.status, {
    "Content-Type": upstream.headers.get("content-type") ?? "application/json"
  });
  response.end(Buffer.from(await upstream.arrayBuffer()));
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
  const communityApiUrl = options.communityApiUrl ?? "http://127.0.0.1:4000";
  const fantasyPetApiBaseUrl = options.fantasyPetApiBaseUrl ?? communityApiUrl;
  const gaPetReviewStore = createGaPetReviewStore({
    runRoot: options.gaPetRunRoot
  });

  return async (request, response) => {
    try {
      const url = new URL(request.url ?? "/", "http://localhost");

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

      if (url.pathname.startsWith("/api/") || shouldProxyCommunity(url.pathname)) {
        await proxyRequest(request, response, url, communityApiUrl);
        return;
      }

      if (shouldProxyFantasyPet(request.method ?? "GET", url.pathname)) {
        await proxyRequest(request, response, url, fantasyPetApiBaseUrl);
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
  const fantasyPetApiBaseUrl =
    options.fantasyPetApiBaseUrl ?? env.FANTASY_PET_API_BASE_URL ?? communityApiUrl;
  const gaPetRunRoot = options.gaPetRunRoot ?? env.GA_PET_RUN_ROOT;
  const server = http.createServer(
    createAdminReviewHttpHandler({ communityApiUrl, fantasyPetApiBaseUrl, gaPetRunRoot })
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
