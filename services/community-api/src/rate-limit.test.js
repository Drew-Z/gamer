import assert from "node:assert/strict";
import test from "node:test";
import {
  createRateLimiterPolicy,
  createRateLimiterPolicyFromEnv,
  resolveClientIp
} from "./rate-limit.js";

test("write limiter allows up to writeMax then rejects", () => {
  const policy = createRateLimiterPolicy({ windowMs: 60_000, writeMax: 4, readMax: 60 });
  const t = 1_000;
  assert.equal(policy.check("POST", "1.2.3.4", t).limited, false);
  assert.equal(policy.check("POST", "1.2.3.4", t).limited, false);
  assert.equal(policy.check("POST", "1.2.3.4", t).limited, false);
  assert.equal(policy.check("POST", "1.2.3.4", t).limited, false);
  const blocked = policy.check("POST", "1.2.3.4", t);
  assert.equal(blocked.limited, true);
  assert.ok(blocked.retryAfterMs > 0 && blocked.retryAfterMs <= 60_000);
});

test("write limiter resets after the window elapses", () => {
  const policy = createRateLimiterPolicy({ windowMs: 1_000, writeMax: 2 });
  assert.equal(policy.check("POST", "ip", 0).limited, false);
  assert.equal(policy.check("POST", "ip", 0).limited, false);
  assert.equal(policy.check("POST", "ip", 0).limited, true);
  assert.equal(policy.check("POST", "ip", 1_500).limited, false);
});

test("read limiter is independent and more generous", () => {
  const policy = createRateLimiterPolicy({ windowMs: 60_000, writeMax: 4, readMax: 60 });
  for (let i = 0; i < 4; i += 1) {
    policy.check("POST", "1.2.3.4", 0);
  }
  assert.equal(policy.check("POST", "1.2.3.4", 0).limited, true);

  for (let i = 0; i < 60; i += 1) {
    assert.equal(policy.check("GET", "1.2.3.4", 0).limited, false);
  }
  assert.equal(policy.check("GET", "1.2.3.4", 0).limited, true);
});

test("buckets are isolated per ip", () => {
  const policy = createRateLimiterPolicy({ windowMs: 60_000, writeMax: 1 });
  assert.equal(policy.check("POST", "a", 0).limited, false);
  assert.equal(policy.check("POST", "a", 0).limited, true);
  assert.equal(policy.check("POST", "b", 0).limited, false);
});

test("exempt paths bypass limiting", () => {
  const policy = createRateLimiterPolicy({ exemptPaths: ["/health"] });
  assert.equal(policy.isExempt("/health"), true);
  assert.equal(policy.isExempt("/v1/feed"), false);
});

test("remaining decrements within the window", () => {
  const policy = createRateLimiterPolicy({ windowMs: 60_000, writeMax: 4 });
  assert.equal(policy.check("POST", "ip", 0).remaining, 3);
  assert.equal(policy.check("POST", "ip", 0).remaining, 2);
  assert.equal(policy.check("POST", "ip", 0).remaining, 1);
  assert.equal(policy.check("POST", "ip", 0).remaining, 0);
  assert.equal(policy.check("POST", "ip", 0).remaining, 0);
});

test("resolveClientIp prefers X-Forwarded-For then falls back to socket", () => {
  assert.equal(
    resolveClientIp({ headers: { "x-forwarded-for": "5.6.7.8, 9.10.11.12" } }),
    "5.6.7.8"
  );
  assert.equal(
    resolveClientIp({ headers: {}, socket: { remoteAddress: "127.0.0.1" } }),
    "127.0.0.1"
  );
  assert.equal(resolveClientIp({ headers: {}, socket: {} }), "unknown");
});

test("env rate limiter is disabled by default", () => {
  assert.equal(createRateLimiterPolicyFromEnv({}), null);
  assert.equal(
    createRateLimiterPolicyFromEnv({
      COMMUNITY_RATE_LIMIT_ENABLED: "0",
      COMMUNITY_RATE_LIMIT_WRITE_MAX: "1"
    }),
    null
  );
});

test("env rate limiter uses configured positive limits", () => {
  const policy = createRateLimiterPolicyFromEnv({
    COMMUNITY_RATE_LIMIT_ENABLED: " yes ",
    COMMUNITY_RATE_LIMIT_WINDOW_MS: "1000",
    COMMUNITY_RATE_LIMIT_WRITE_MAX: "1",
    COMMUNITY_RATE_LIMIT_READ_MAX: "2"
  });

  assert.ok(policy);
  assert.equal(policy.check("POST", "ip", 0).limited, false);
  assert.equal(policy.check("POST", "ip", 0).limited, true);
  assert.equal(policy.check("GET", "ip", 0).limited, false);
  assert.equal(policy.check("GET", "ip", 0).limited, false);
  assert.equal(policy.check("GET", "ip", 0).limited, true);
});

test("env rate limiter falls back for invalid numeric values", () => {
  const policy = createRateLimiterPolicyFromEnv({
    COMMUNITY_RATE_LIMIT_ENABLED: "TRUE",
    COMMUNITY_RATE_LIMIT_WINDOW_MS: "-1",
    COMMUNITY_RATE_LIMIT_WRITE_MAX: "1bad",
    COMMUNITY_RATE_LIMIT_READ_MAX: "0"
  });

  assert.ok(policy);
  for (let i = 0; i < 4; i += 1) {
    assert.equal(policy.check("POST", "ip", 0).limited, false);
  }
  assert.equal(policy.check("POST", "ip", 0).limited, true);
});
