import assert from "node:assert/strict";
import test from "node:test";
import { summarizeFantasyPetRuleState } from "./adapter.js";

test("missing state is blocked", () => {
  const summary = summarizeFantasyPetRuleState(null);

  assert.equal(summary.status, "blocked");
  assert.equal(summary.reason, "state is missing");
});

test("accepted preview is community-ready", () => {
  const summary = summarizeFantasyPetRuleState({
    currentStage: "preview-review",
    blockers: [],
    preview: {
      userDecision: "keep",
      urlOrPath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/preview.html"
    }
  });

  assert.equal(summary.status, "community-ready");
  assert.equal(summary.evidence.length, 1);
});

test("blockers keep state blocked", () => {
  const summary = summarizeFantasyPetRuleState({
    currentStage: "base-review",
    blockers: [
      {
        stage: "base-review",
        reason: "base identity candidate has not been accepted by user"
      }
    ]
  });

  assert.equal(summary.status, "blocked");
  assert.equal(summary.reason, "base identity candidate has not been accepted by user");
});

test("active state without blockers is in-progress", () => {
  const summary = summarizeFantasyPetRuleState({
    currentStage: "action-design",
    blockers: [],
    preview: {
      userDecision: "missing"
    }
  });

  assert.equal(summary.status, "in-progress");
  assert.equal(summary.reason, "current stage is action-design");
});
