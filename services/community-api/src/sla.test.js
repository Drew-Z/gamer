import assert from "node:assert/strict";
import test from "node:test";
import { createSlaConfig } from "./sla.js";

test("sla config exposes documented defaults", () => {
  const sla = createSlaConfig({});
  assert.equal(sla.schema, "gamer.sla.v1");
  assert.equal(sla.hatch.reserveEggMaxMs, 120_000);
  assert.equal(sla.hatch.mysteryEggMaxMs, 600_000);
  assert.equal(sla.hatch.customHatchMaxMs, 900_000);
  assert.equal(sla.polling.suggestedIntervalMs, 3_000);
  assert.equal(sla.polling.maxAttempts, 3);
  assert.equal(sla.polling.baseBackoffMs, 1_000);
  assert.equal(sla.failureThresholds.consecutivePollFailuresBeforeSlowNotice, 3);
});

test("env overrides each sla knob", () => {
  const sla = createSlaConfig({
    SLA_HATCH_RESERVE_MS: "60000",
    SLA_HATCH_MYSTERY_MS: "300000",
    SLA_HATCH_CUSTOM_MS: "500000",
    SLA_POLL_INTERVAL_MS: "5000",
    SLA_POLL_MAX_ATTEMPTS: "5",
    SLA_POLL_BASE_BACKOFF_MS: "2000",
    SLA_POLL_FAIL_THRESHOLD: "2"
  });
  assert.equal(sla.hatch.reserveEggMaxMs, 60_000);
  assert.equal(sla.hatch.mysteryEggMaxMs, 300_000);
  assert.equal(sla.hatch.customHatchMaxMs, 500_000);
  assert.equal(sla.polling.suggestedIntervalMs, 5_000);
  assert.equal(sla.polling.maxAttempts, 5);
  assert.equal(sla.polling.baseBackoffMs, 2_000);
  assert.equal(sla.failureThresholds.consecutivePollFailuresBeforeSlowNotice, 2);
});

test("invalid env values fall back to defaults", () => {
  const sla = createSlaConfig({
    SLA_HATCH_RESERVE_MS: "not-a-number",
    SLA_POLL_INTERVAL_MS: "-5",
    SLA_POLL_MAX_ATTEMPTS: "0"
  });
  assert.equal(sla.hatch.reserveEggMaxMs, 120_000);
  assert.equal(sla.polling.suggestedIntervalMs, 3_000);
  assert.equal(sla.polling.maxAttempts, 3);
});

test("reserve egg is the fastest hatch path", () => {
  const sla = createSlaConfig({});
  assert.ok(sla.hatch.reserveEggMaxMs < sla.hatch.mysteryEggMaxMs);
  assert.ok(sla.hatch.mysteryEggMaxMs < sla.hatch.customHatchMaxMs);
});
