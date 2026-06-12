import assert from "node:assert/strict";
import test from "node:test";
import { selectNextReworkRequest } from "./ga-random-pet-worker.js";

const request = (requestId) => ({
  schema: "gamer.ga-pet-rework-request.v1",
  requestId,
  sourceRunId: "ga-source",
  status: "requested"
});

const status = ({ requestId, value, createdAt }) => ({
  schema: "gamer.ga-pet-rework-status.v1",
  requestId,
  sourceRunId: "ga-source",
  targetRunId: "ga-target",
  status: value,
  createdAt
});

test("selectNextReworkRequest skips active started requests", () => {
  const nowMs = Date.parse("2026-06-12T04:00:00.000Z");
  const selected = selectNextReworkRequest(
    [
      request("rework-active"),
      status({
        requestId: "rework-active",
        value: "started",
        createdAt: "2026-06-12T03:30:00.000Z"
      }),
      request("rework-next")
    ],
    {
      nowMs,
      startedTimeoutMinutes: 180
    }
  );

  assert.equal(selected.requestId, "rework-next");
});

test("selectNextReworkRequest retries stale started requests", () => {
  const nowMs = Date.parse("2026-06-12T04:00:00.000Z");
  const selected = selectNextReworkRequest(
    [
      request("rework-stale"),
      status({
        requestId: "rework-stale",
        value: "started",
        createdAt: "2026-06-12T00:30:00.000Z"
      })
    ],
    {
      nowMs,
      startedTimeoutMinutes: 180
    }
  );

  assert.equal(selected.requestId, "rework-stale");
});

test("selectNextReworkRequest treats completed and failed requests as terminal", () => {
  const selected = selectNextReworkRequest(
    [
      request("rework-done"),
      status({
        requestId: "rework-done",
        value: "completed",
        createdAt: "2026-06-12T01:00:00.000Z"
      }),
      request("rework-failed"),
      status({
        requestId: "rework-failed",
        value: "failed",
        createdAt: "2026-06-12T01:00:00.000Z"
      })
    ],
    {
      nowMs: Date.parse("2026-06-12T04:00:00.000Z"),
      startedTimeoutMinutes: 180
    }
  );

  assert.equal(selected, null);
});
