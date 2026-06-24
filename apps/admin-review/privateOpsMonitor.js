const statusSchema = "desktop-pet.ops.private-ops-monitor-status.v1";
const logSchema = "desktop-pet.ops.private-ops-monitor-log.v1";

const trimString = (value) => (typeof value === "string" ? value.trim() : "");

const isEnabled = (value) =>
  /^(1|true|yes|on)$/iu.test(String(value ?? "").trim());

const isDisabled = (value) =>
  /^(0|false|no|off)$/iu.test(String(value ?? "").trim());

const parsePositiveInt = (value, fallback, max) => {
  const parsed = Number.parseInt(trimString(value), 10);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return fallback;
  }
  return Math.min(parsed, max);
};

export function resolvePrivateOpsMonitorConfig(env = process.env) {
  const explicitEnabled = trimString(env.PRIVATE_OPS_MONITOR_ENABLED);
  const role = trimString(env.PRIVATE_OPS_DEPLOYMENT_ROLE).toLowerCase();
  const enabled = explicitEnabled
    ? isEnabled(explicitEnabled) && !isDisabled(explicitEnabled)
    : role === "community";

  return {
    enabled,
    intervalMs: parsePositiveInt(env.PRIVATE_OPS_MONITOR_INTERVAL_MS, 300000, 3600000),
    staleAfterMs: parsePositiveInt(env.PRIVATE_OPS_MONITOR_STALE_AFTER_MS, 900000, 7200000),
    historyLimit: parsePositiveInt(env.PRIVATE_OPS_MONITOR_HISTORY_LIMIT, 12, 288),
    knownAppJobId: trimString(env.PRIVATE_OPS_MONITOR_KNOWN_APP_JOB_ID)
  };
}

export function createPrivateOpsMonitor(options = {}) {
  const env = options.env ?? process.env;
  const config = options.config ?? resolvePrivateOpsMonitorConfig(env);
  const communityApiUrl = normalizeBaseUrl(
    options.communityApiUrl ?? env.COMMUNITY_API_URL ?? "http://127.0.0.1:4000"
  );
  const fetchImpl = options.fetchImpl ?? fetch;
  const now = options.now ?? (() => new Date());
  const writeLog =
    options.writeLog ??
    ((entry) => {
      console.log(JSON.stringify(entry));
    });
  const setIntervalImpl = options.setIntervalImpl ?? setInterval;
  const clearIntervalImpl = options.clearIntervalImpl ?? clearInterval;
  let timer = null;
  let lastRun = null;
  let lastOkAt = "";
  let consecutiveFailures = 0;
  const history = [];

  const recordRun = (run) => {
    lastRun = run;
    if (run.ok) {
      lastOkAt = run.at;
      consecutiveFailures = 0;
    } else {
      consecutiveFailures += 1;
    }
    history.unshift({
      at: run.at,
      ok: run.ok,
      status: run.status,
      durationMs: run.durationMs,
      checkCount: run.checks.length
    });
    history.splice(config.historyLimit);
    writeLog({
      schema: logSchema,
      at: run.at,
      ok: run.ok,
      status: run.status,
      durationMs: run.durationMs,
      checkCount: run.checks.length,
      consecutiveFailures
    });
  };

  const runOnce = async () => {
    const startedAt = now();
    const checks = [];
    let ok = true;
    let error;

    const runCheck = async (name, callback) => {
      try {
        await callback();
        checks.push({ name, status: "pass" });
      } catch (checkError) {
        checks.push({ name, status: "fail" });
        throw new Error(`${name}: ${safeErrorMessage(checkError)}`);
      }
    };

    try {
      await runCheck("community health is public-safe", async () => {
        const response = await requestJson(fetchImpl, communityApiUrl, "/health");
        assertStatus(response, 200);
        assertEqual(response.body.ok, true, "health.ok");
        assertEqual(response.body.service, "community-api", "health.service");
      });

      await runCheck("community SLA is readable", async () => {
        const response = await requestJson(fetchImpl, communityApiUrl, "/v1/sla");
        assertStatus(response, 200);
        assertObject(response.body, "sla");
      });

      await runCheck("agent worker readiness is proxied", async () => {
        const response = await requestJson(fetchImpl, communityApiUrl, "/worker-readiness");
        assertStatus(response, 200);
        assertObject(response.body, "worker readiness");
        assertEqual(
          response.body.schema,
          "fantasy-pet.worker-readiness-public.v1",
          "worker readiness schema"
        );
      });

      await runCheck("agent app API contract is proxied", async () => {
        const response = await requestJson(fetchImpl, communityApiUrl, "/app-api-contract");
        assertStatus(response, 200);
        assertObject(response.body, "app api contract");
        assertEqual(
          response.body.schema,
          "fantasy-pet.app-api-contract.v1",
          "app api contract schema"
        );
      });

      await runCheck("internal community auth rejects missing token", async () => {
        const response = await requestJson(fetchImpl, communityApiUrl, "/v1/check-in", {
          method: "POST",
          body: {
            date: "2026-06-24"
          }
        });
        assertStatus(response, 401);
        assertEqual(
          response.body.error,
          "unauthorized_demo_request",
          "internal missing-token error"
        );
      });

      if (config.knownAppJobId) {
        await runCheck("known job package gate is observable", async () => {
          const response = await requestPackageGate(
            fetchImpl,
            communityApiUrl,
            `/pet-generation-jobs/${encodeURIComponent(config.knownAppJobId)}/package`
          );
          if (response.status === 200) {
            if (!response.contentType.includes("application/zip")) {
              throw new Error(`ready package content type was ${response.contentType}`);
            }
            if (
              response.bytes.length < 2 ||
              response.bytes[0] !== 0x50 ||
              response.bytes[1] !== 0x4b
            ) {
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
    } catch (runError) {
      ok = false;
      error = safeErrorMessage(runError);
    }

    const finishedAt = now();
    const run = {
      at: startedAt.toISOString(),
      ok,
      status: ok ? "pass" : "fail",
      durationMs: Math.max(0, finishedAt.getTime() - startedAt.getTime()),
      checks
    };
    if (error) {
      run.error = error;
    }
    recordRun(run);
    return run;
  };

  return {
    runOnce,
    start() {
      if (!config.enabled || timer) {
        return;
      }
      void runOnce();
      timer = setIntervalImpl(() => {
        void runOnce();
      }, config.intervalMs);
      timer?.unref?.();
    },
    stop() {
      if (!timer) {
        return;
      }
      clearIntervalImpl(timer);
      timer = null;
    },
    getStatus() {
      const currentTime = now();
      const lastRunAt = lastRun?.at ?? "";
      const stale =
        config.enabled &&
        (!lastRunAt || currentTime.getTime() - Date.parse(lastRunAt) > config.staleAfterMs);
      const ok = !config.enabled || (Boolean(lastRun?.ok) && !stale);

      return {
        schema: statusSchema,
        ok,
        monitor: {
          enabled: config.enabled,
          intervalMs: config.intervalMs,
          staleAfterMs: config.staleAfterMs,
          historyLimit: config.historyLimit,
          knownPackageGate: Boolean(config.knownAppJobId),
          consecutiveFailures,
          lastRunAt,
          lastOkAt,
          lastStatus: lastRun?.status ?? "never",
          stale
        },
        lastRun,
        history: [...history]
      };
    }
  };
}

async function requestJson(fetchImpl, baseUrl, path, options = {}) {
  const headers = {
    Accept: "application/json"
  };
  let body;
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.body);
  }

  const response = await fetchImpl(new URL(path, baseUrl), {
    method: options.method ?? "GET",
    headers,
    body
  });
  const text = await response.text();
  const contentType = response.headers.get("content-type") ?? "";
  return {
    status: response.status,
    body: contentType.includes("json") && text ? JSON.parse(text) : text
  };
}

async function requestPackageGate(fetchImpl, baseUrl, path) {
  const response = await fetchImpl(new URL(path, baseUrl), {
    method: "GET",
    headers: {
      Accept: "application/zip, application/json"
    }
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
  return {
    status: response.status,
    contentType,
    body: contentType.includes("json") && text ? JSON.parse(text) : text
  };
}

function normalizeBaseUrl(value) {
  const trimmed = trimString(value);
  if (!trimmed) {
    throw new Error("community api url is blank");
  }
  return trimmed.endsWith("/") ? trimmed : `${trimmed}/`;
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

function safeErrorMessage(error) {
  const message = error instanceof Error ? error.message : String(error);
  return message.replace(/https?:\/\/\S+/giu, "[url-redacted]");
}
