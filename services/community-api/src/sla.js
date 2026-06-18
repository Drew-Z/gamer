const DEFAULTS = {
  SLA_HATCH_RESERVE_MS: 2 * 60_000,
  SLA_HATCH_MYSTERY_MS: 10 * 60_000,
  SLA_HATCH_CUSTOM_MS: 15 * 60_000,
  SLA_POLL_INTERVAL_MS: 3_000,
  SLA_POLL_MAX_ATTEMPTS: 3,
  SLA_POLL_BASE_BACKOFF_MS: 1_000,
  SLA_POLL_FAIL_THRESHOLD: 3
};

export function createSlaConfig(env = process.env) {
  const num = (key) => {
    const raw = env[key];
    if (raw === undefined || raw === "") {
      return DEFAULTS[key];
    }
    const parsed = Number.parseInt(raw, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : DEFAULTS[key];
  };

  return {
    schema: "gamer.sla.v1",
    hatch: {
      reserveEggMaxMs: num("SLA_HATCH_RESERVE_MS"),
      mysteryEggMaxMs: num("SLA_HATCH_MYSTERY_MS"),
      customHatchMaxMs: num("SLA_HATCH_CUSTOM_MS")
    },
    polling: {
      suggestedIntervalMs: num("SLA_POLL_INTERVAL_MS"),
      maxAttempts: num("SLA_POLL_MAX_ATTEMPTS"),
      baseBackoffMs: num("SLA_POLL_BASE_BACKOFF_MS")
    },
    failureThresholds: {
      consecutivePollFailuresBeforeSlowNotice: num("SLA_POLL_FAIL_THRESHOLD")
    }
  };
}
