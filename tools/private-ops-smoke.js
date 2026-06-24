#!/usr/bin/env node

const baseUrl = normalizeBaseUrl(process.env.COMMUNITY_BASE_URL ?? "http://127.0.0.1:4000");
const communityDemoToken = requireSecretEnv("COMMUNITY_DEMO_TOKEN");
const fantasyPetUpstreamToken = requireSecretEnv("FANTASY_PET_UPSTREAM_TOKEN");
const basicAuthHeader = privateOpsBasicAuthHeader(process.env);
const smokeSurface = resolveSmokeSurface(process.env);
const knownAppJobId = String(process.env.PRIVATE_OPS_KNOWN_APP_JOB_ID ?? "").trim();
const forbiddenFragments = [
  communityDemoToken,
  fantasyPetUpstreamToken,
  process.env.SUPABASE_SERVICE_ROLE_KEY,
  process.env.DATABASE_URL,
  process.env.PRIVATE_OPS_BASIC_AUTH_PASSWORD
]
  .map((value) => (typeof value === "string" ? value.trim() : ""))
  .filter((value) => value.length >= 8);

const checks = [];

if (smokeSurface === "admin-review" && !basicAuthHeader) {
  throw new Error(
    "PRIVATE_OPS_BASIC_AUTH_USER and PRIVATE_OPS_BASIC_AUTH_PASSWORD are required for admin-review smoke surface."
  );
}

await runCheck("community health is public-safe", async () => {
  const response = await requestJson("/health", {
    basicAuth: smokeSurface !== "admin-review"
  });
  assertStatus(response, 200);
  assertEqual(response.body.ok, true, "health.ok");
  assertEqual(response.body.service, "community-api", "health.service");
});

await runCheck("community SLA is readable", async () => {
  const response = await requestJson("/v1/sla");
  assertStatus(response, 200);
  assertObject(response.body, "sla");
});

await runCheck("agent worker readiness is proxied", async () => {
  const response = await requestJson("/worker-readiness");
  assertStatus(response, 200);
  assertObject(response.body, "worker readiness");
  assertEqual(response.body.schema, "fantasy-pet.worker-readiness-public.v1", "worker readiness schema");
});

await runCheck("agent app API contract is proxied", async () => {
  const response = await requestJson("/app-api-contract");
  assertStatus(response, 200);
  assertObject(response.body, "app api contract");
  assertEqual(response.body.schema, "fantasy-pet.app-api-contract.v1", "app api contract schema");
});

if (smokeSurface === "admin-review") {
  await runCheck("admin-review rejects missing basic auth", async () => {
    const response = await requestJson("/v1/check-in", {
      method: "POST",
      basicAuth: false,
      body: {
        date: "2026-06-24"
      }
    });
    assertStatus(response, 401);
    assertEqual(response.body.error, "admin_basic_auth_required", "missing-basic-auth error");
  });

  await runCheck("protected community write accepts admin-review basic auth", async () => {
    const response = await requestJson("/v1/check-in", {
      method: "POST",
      body: {
        date: "2026-06-24"
      }
    });
    assertStatus(response, 200);
    assertEqual(response.body.checkIn?.date, "2026-06-24", "check-in date");
  });
} else {
  await runCheck("protected community write rejects missing token", async () => {
    const response = await requestJson("/v1/check-in", {
      method: "POST",
      body: {
        date: "2026-06-24"
      }
    });
    assertStatus(response, 401);
    assertEqual(response.body.error, "unauthorized_demo_request", "missing-token error");
  });

  await runCheck("protected community write accepts demo token", async () => {
    const response = await requestJson("/v1/check-in", {
      method: "POST",
      demoToken: true,
      body: {
        date: "2026-06-24"
      }
    });
    assertStatus(response, 200);
    assertEqual(response.body.checkIn?.date, "2026-06-24", "check-in date");
  });

  await runCheck("fantasy-pet job creation rejects missing community token", async () => {
    const response = await requestJson("/pet-generation-jobs", {
      method: "POST",
      body: createDemoJobRequest()
    });
    assertStatus(response, 401);
    assertEqual(response.body.error, "unauthorized_demo_request", "fantasy-pet missing-token error");
  });
}

if (isEnabled(process.env.PRIVATE_OPS_CREATE_JOB)) {
  await runCheck("fantasy-pet job creation accepts demo token", async () => {
    const response = await requestJson("/pet-generation-jobs", {
      method: "POST",
      demoToken: smokeSurface !== "admin-review",
      body: createDemoJobRequest()
    });
    assertStatus(response, 201);
    assertObject(response.body, "created fantasy-pet job");
  });
}

if (knownAppJobId) {
  await runCheck("known job package gate is observable", async () => {
    const response = await requestPackageGate(
      `/pet-generation-jobs/${encodeURIComponent(knownAppJobId)}/package`,
      {
        demoToken: smokeSurface !== "admin-review"
      }
    );

    if (response.status === 200) {
      if (!response.contentType.includes("application/zip")) {
        throw new Error(`ready package must be application/zip, got ${response.contentType}`);
      }
      if (response.bytes.length < 2 || response.bytes[0] !== 0x50 || response.bytes[1] !== 0x4b) {
        throw new Error("ready package did not start with ZIP magic bytes");
      }
      return;
    }

    if (response.status === 409) {
      assertObject(response.body, "package gate");
      assertEqual(
        response.body.schema,
        "fantasy-pet.package-download-response.v1",
        "package gate schema"
      );
      assertEqual(response.body.status, "blocked", "package gate status");
      return;
    }

    throw new Error(`expected package ready 200 or gated 409, got HTTP ${response.status}`);
  });
}

console.log(
  JSON.stringify(
    {
      ok: true,
      baseUrl,
      smokeSurface,
      checks,
      createdLiveJob: isEnabled(process.env.PRIVATE_OPS_CREATE_JOB),
      checkedKnownPackageGate: Boolean(knownAppJobId)
    },
    null,
    2
  )
);

function normalizeBaseUrl(value) {
  const trimmed = String(value ?? "").trim();
  if (!trimmed) {
    throw new Error("COMMUNITY_BASE_URL is blank");
  }
  return trimmed.replace(/\/+$/u, "");
}

function requireSecretEnv(name) {
  const value = String(process.env[name] ?? "").trim();
  if (value.length < 8) {
    throw new Error(`${name} must be set to a private demo token before running private ops smoke.`);
  }
  return value;
}

function resolveSmokeSurface(env) {
  const value = String(env.PRIVATE_OPS_SMOKE_SURFACE ?? "community-api").trim();
  if (!["community-api", "admin-review"].includes(value)) {
    throw new Error("PRIVATE_OPS_SMOKE_SURFACE must be community-api or admin-review.");
  }
  return value;
}

function createDemoJobRequest() {
  return {
    schema: "fantasy-pet.app-job-create-request.v1",
    description: "private ops smoke pet",
    bodyShape: "small quadruped",
    style: "desktop pet"
  };
}

async function requestJson(path, options = {}) {
  const method = options.method ?? "GET";
  const headers = {
    Accept: "application/json"
  };
  let body;

  if (basicAuthHeader && options.basicAuth !== false) {
    headers.Authorization = basicAuthHeader;
  }
  if (options.demoToken) {
    headers["X-Demo-Token"] = communityDemoToken;
  }
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.body);
  }

  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body
  });
  const text = await response.text();
  assertNoLeaks(`${method} ${path}`, text);
  const contentType = response.headers.get("content-type") ?? "";

  return {
    status: response.status,
    body: contentType.includes("json") && text ? JSON.parse(text) : text
  };
}

async function requestPackageGate(path, options = {}) {
  const headers = {
    Accept: "application/zip, application/json"
  };

  if (basicAuthHeader && options.basicAuth !== false) {
    headers.Authorization = basicAuthHeader;
  }
  if (options.demoToken) {
    headers["X-Demo-Token"] = communityDemoToken;
  }

  const response = await fetch(`${baseUrl}${path}`, {
    method: "GET",
    headers
  });
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/zip")) {
    return {
      status: response.status,
      contentType,
      bytes: new Uint8Array(await response.arrayBuffer())
    };
  }

  const text = await response.text();
  assertNoLeaks(`GET ${path}`, text);

  return {
    status: response.status,
    contentType,
    body: contentType.includes("json") && text ? JSON.parse(text) : text
  };
}

async function runCheck(name, callback) {
  try {
    await callback();
    checks.push({
      name,
      status: "pass"
    });
  } catch (error) {
    checks.push({
      name,
      status: "fail"
    });
    const detail = error instanceof Error ? error.message : String(error);
    throw new Error(`${name}: ${detail}`);
  }
}

function assertStatus(response, expected) {
  if (response.status !== expected) {
    throw new Error(`expected HTTP ${expected}, got ${response.status}`);
  }
}

function assertEqual(actual, expected, label) {
  if (actual !== expected) {
    throw new Error(`${label} expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
  }
}

function assertObject(value, label) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error(`${label} must be a JSON object`);
  }
}

function assertNoLeaks(label, text) {
  for (const fragment of forbiddenFragments) {
    if (text.includes(fragment)) {
      throw new Error(`${label} response leaked a configured secret fragment`);
    }
  }

  const forbiddenPatterns = [
    /\/home\/[A-Za-z0-9_.-]+/u,
    /[A-Za-z]:\\/u,
    /adapter-config/u,
    /agent-outputs/u,
    /server-worker-cycle/u,
    /task-packet/u,
    /x-demo-token/iu,
    /authorization/iu
  ];
  for (const pattern of forbiddenPatterns) {
    if (pattern.test(text)) {
      throw new Error(`${label} response matched forbidden leak pattern ${pattern}`);
    }
  }
}

function privateOpsBasicAuthHeader(env) {
  const user = String(env.PRIVATE_OPS_BASIC_AUTH_USER ?? "").trim();
  const password = String(env.PRIVATE_OPS_BASIC_AUTH_PASSWORD ?? "").trim();
  if (!user && !password) {
    return "";
  }
  if (!user || !password) {
    throw new Error("PRIVATE_OPS_BASIC_AUTH_USER and PRIVATE_OPS_BASIC_AUTH_PASSWORD must be set together.");
  }
  return `Basic ${Buffer.from(`${user}:${password}`).toString("base64")}`;
}

function isEnabled(value) {
  return /^(1|true|yes)$/iu.test(String(value ?? "").trim());
}
