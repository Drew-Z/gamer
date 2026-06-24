const PUBLIC_SAFE_DEMO_PATHS = new Set([
  "/health",
  "/v1/sla",
  "/app-api-contract",
  "/worker-readiness"
]);

export function resolveCommunityDemoToken(options = {}) {
  const env = options.env ?? process.env;
  const token = options.communityDemoToken ?? env.COMMUNITY_DEMO_TOKEN ?? "";

  return typeof token === "string" ? token.trim() : "";
}

export function isCommunityDemoAuthExempt(method, requestUrl) {
  const normalizedMethod = String(method ?? "GET").toUpperCase();
  if (normalizedMethod === "OPTIONS") {
    return true;
  }

  const url = new URL(requestUrl, "http://localhost");
  return PUBLIC_SAFE_DEMO_PATHS.has(url.pathname);
}

export function requestDemoToken(headers = {}) {
  const headerToken = headerValue(headers, "x-demo-token").trim();
  if (headerToken) {
    return headerToken;
  }

  const authorization = headerValue(headers, "authorization").trim();
  const match = /^Bearer\s+(.+)$/iu.exec(authorization);
  return match ? match[1].trim() : "";
}

export function requireCommunityDemoAuth(method, requestUrl, headers, options = {}) {
  const requiredToken = resolveCommunityDemoToken(options);
  if (!requiredToken || isCommunityDemoAuthExempt(method, requestUrl)) {
    return null;
  }

  if (requestDemoToken(headers) === requiredToken) {
    return null;
  }

  return {
    status: 401,
    headers: {
      "Content-Type": "application/json"
    },
    body: Buffer.from(
      JSON.stringify({
        error: "unauthorized_demo_request"
      })
    )
  };
}

function headerValue(headers, name) {
  const value = headers[name] ?? headers[name.toLowerCase()] ?? headers[name.toUpperCase()];

  if (Array.isArray(value)) {
    return value.join(", ");
  }

  return typeof value === "string" ? value : "";
}
