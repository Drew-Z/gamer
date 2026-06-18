import assert from "node:assert/strict";
import test from "node:test";
import { createRequestLogger, formatRequestLog } from "./logging.js";

test("formatRequestLog emits structured JSON with required fields", () => {
  const line = formatRequestLog({
    method: "GET",
    path: "/health",
    status: 200,
    durationMs: 5,
    ts: "2026-06-18T00:00:00.000Z"
  });
  const parsed = JSON.parse(line);
  assert.equal(parsed.method, "GET");
  assert.equal(parsed.path, "/health");
  assert.equal(parsed.status, 200);
  assert.equal(parsed.durationMs, 5);
  assert.equal(parsed.ts, "2026-06-18T00:00:00.000Z");
});

test("formatRequestLog defaults timestamp to now when omitted", () => {
  const parsed = JSON.parse(
    formatRequestLog({
      method: "POST",
      path: "/v1/check-in",
      status: 200,
      durationMs: 1
    })
  );
  assert.ok(typeof parsed.ts === "string" && parsed.ts.endsWith("Z"));
});

test("createRequestLogger writes formatted line via injected writer", () => {
  const lines = [];
  const logger = createRequestLogger((line) => lines.push(line));
  logger({ method: "GET", path: "/health", status: 200, durationMs: 2 });
  assert.equal(lines.length, 1);
  assert.match(lines[0], /"path":"\/health"/);
  assert.match(lines[0], /"status":200/);
});
