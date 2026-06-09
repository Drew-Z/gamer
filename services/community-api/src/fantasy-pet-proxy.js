const PUBLIC_FANTASY_PET_PROXY_ENDPOINTS = [
  {
    method: "POST",
    pattern: /^\/pet-generation-jobs$/u
  },
  {
    method: "GET",
    pattern: /^\/pet-generation-jobs\/[^/]+$/u
  },
  {
    method: "GET",
    pattern: /^\/pet-generation-jobs\/[^/]+\/artifacts$/u
  },
  {
    method: "GET",
    pattern: /^\/pet-generation-jobs\/[^/]+\/artifacts\/[^/]+$/u
  },
  {
    method: "POST",
    pattern: /^\/pet-generation-jobs\/[^/]+\/review-decisions$/u
  },
  {
    method: "GET",
    pattern: /^\/pet-generation-jobs\/[^/]+\/package$/u
  },
  {
    method: "GET",
    pattern: /^\/worker-readiness$/u
  },
  {
    method: "GET",
    pattern: /^\/app-api-contract$/u
  }
];

const jsonProxyResponse = (status, body) => ({
  status,
  headers: {
    "Content-Type": "application/json"
  },
  body: Buffer.from(JSON.stringify(body))
});

export function isFantasyPetPublicProxyRequest(method, requestUrl) {
  const url = new URL(requestUrl, "http://localhost");
  const normalizedMethod = method.toUpperCase();

  return PUBLIC_FANTASY_PET_PROXY_ENDPOINTS.some(
    (endpoint) =>
      endpoint.method === normalizedMethod &&
      endpoint.pattern.test(url.pathname)
  );
}

export function resolveFantasyPetApiBaseUrl(options = {}) {
  const env = options.env ?? process.env;
  const candidate =
    options.fantasyPetApiBaseUrl ?? env.FANTASY_PET_API_BASE_URL ?? "";

  if (typeof candidate !== "string") {
    return "";
  }

  return candidate.trim().replace(/\/+$/u, "");
}

export async function proxyFantasyPetPublicRequest(method, requestUrl, options = {}) {
  const baseUrl = resolveFantasyPetApiBaseUrl(options);
  if (!baseUrl) {
    return jsonProxyResponse(503, {
      error: "fantasy_pet_api_unconfigured"
    });
  }

  const fetchImpl = options.fetch ?? globalThis.fetch;
  if (typeof fetchImpl !== "function") {
    return jsonProxyResponse(503, {
      error: "fantasy_pet_api_fetch_unavailable"
    });
  }

  const incomingUrl = new URL(requestUrl, "http://localhost");
  const upstreamUrl = new URL(`${incomingUrl.pathname}${incomingUrl.search}`, baseUrl);
  const normalizedMethod = method.toUpperCase();

  try {
    const upstreamResponse = await fetchImpl(upstreamUrl, {
      method: normalizedMethod,
      headers: forwardedRequestHeaders(options.headers),
      body: methodAllowsBody(normalizedMethod) ? options.rawBody ?? "" : undefined
    });
    const responseBody = Buffer.from(await upstreamResponse.arrayBuffer());
    const contentType = upstreamResponse.headers.get("content-type");

    return {
      status: upstreamResponse.status,
      headers: contentType
        ? {
            "Content-Type": contentType
          }
        : {},
      body: responseBody
    };
  } catch {
    return jsonProxyResponse(502, {
      error: "fantasy_pet_api_unreachable"
    });
  }
}

function forwardedRequestHeaders(headers = {}) {
  const forwarded = {};
  const contentType = headerValue(headers, "content-type");
  const accept = headerValue(headers, "accept");

  if (contentType) {
    forwarded["Content-Type"] = contentType;
  }
  if (accept) {
    forwarded.Accept = accept;
  }

  return forwarded;
}

function headerValue(headers, name) {
  const value = headers[name] ?? headers[name.toLowerCase()] ?? headers[name.toUpperCase()];

  if (Array.isArray(value)) {
    return value.join(", ");
  }

  return typeof value === "string" ? value : "";
}

function methodAllowsBody(method) {
  return method !== "GET" && method !== "HEAD";
}
