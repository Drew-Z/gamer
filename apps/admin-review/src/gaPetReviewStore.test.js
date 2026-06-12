import assert from "node:assert/strict";
import { mkdtemp, mkdir, readFile, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { createGaPetReviewStore } from "./gaPetReviewStore.js";

test("GA pet review store lists candidates and writes learning feedback", async () => {
  const runRoot = await mkdtemp(path.join(os.tmpdir(), "ga-pet-review-"));
  const runId = "ga-test-001";
  const runDir = path.join(runRoot, runId);
  await mkdir(path.join(runDir, "source", "generation"), { recursive: true });
  await mkdir(path.join(runDir, "previews"), { recursive: true });
  await mkdir(path.join(runDir, "meta"), { recursive: true });
  await mkdir(path.join(runDir, "exports"), { recursive: true });
  await writeFile(path.join(runDir, "previews", "preview.png"), "png");
  await writeFile(path.join(runDir, "exports", `${runId}-full-resource-candidate.zip`), "zip");
  await writeFile(
    path.join(runDir, "source", "generation", "prompt-plan.json"),
    JSON.stringify({
      name: "Test Pet",
      summary: "A test pet",
      species: "mouse",
      element: "electric teal",
      packageMode: "full",
      backgroundMode: "transparent"
    })
  );
  await writeFile(
    path.join(runDir, "source", "generation", "api-trace.json"),
    JSON.stringify({
      model: "test-model",
      redacted: true
    })
  );
  await writeFile(
    path.join(runDir, "package-manifest.json"),
    JSON.stringify({
      resourceStatus: "full-resource-candidate-ready",
      generatedBy: {
        createdAt: "2026-06-12T00:00:00.000Z"
      }
    })
  );
  await writeFile(
    path.join(runDir, "meta", "runtime.json"),
    JSON.stringify({
      packageMode: "full"
    })
  );
  await writeFile(
    path.join(runDir, "meta", "motion_map.json"),
    JSON.stringify({
      actions: {
        idle: {
          sheet: "motion/sheets/idle.png",
          frames: 16,
          loop: true,
          status: "generated"
        }
      }
    })
  );
  await writeFile(path.join(runDir, "review-card.md"), "# Test Pet\n");

  const store = createGaPetReviewStore({ runRoot });
  const list = await store.listCandidates();
  assert.equal(list.count, 1);
  assert.equal(list.summary.totalCandidates, 1);
  assert.equal(list.candidates[0].displayName, "Test Pet");
  assert.equal(list.candidates[0].motionSheets[0].actionId, "idle");
  assert.equal(list.candidates[0].packagePath, `exports/${runId}-full-resource-candidate.zip`);
  assert.ok(
    list.candidates[0].evidenceFiles.some(
      (file) => file.path === "source/generation/prompt-plan.json"
    )
  );
  assert.ok(
    list.candidates[0].evidenceFiles.every((file) =>
      file.url.startsWith(`/ga-review/files/${runId}?path=`)
    )
  );

  const result = await store.writeFeedback({
    runId,
    body: {
      decision: "rework",
      severity: "high",
      actionId: "idle",
      tags: "identity drift, static frames",
      notes: "Keep the ears, but fix the body scale.",
      promptPatch: "Lock the head/body ratio."
    }
  });
  assert.equal(result.feedback.decision, "rework");
  assert.equal(result.reworkRequest.status, "requested");

  const learningText = await readFile(path.join(runRoot, "ga-learning-notes.jsonl"), "utf8");
  assert.match(learningText, /identity-drift/);
  assert.match(learningText, /Lock the base identity/);
  const queueText = await readFile(path.join(runRoot, "ga-rework-queue.jsonl"), "utf8");
  assert.match(queueText, /ga-pet-rework-request/);

  const updated = await store.listCandidates();
  assert.equal(updated.summary.feedbackCount, 1);
  assert.equal(updated.summary.learningNoteCount, 1);
  assert.equal(updated.summary.rework.queued, 1);
  assert.deepEqual(updated.summary.topTags[0], {
    label: "identity-drift",
    count: 1
  });
  assert.equal(updated.candidates[0].feedback.history[0].decision, "rework");
  assert.equal(updated.candidates[0].rework.requests[0].mode, "rework");
  assert.ok(
    updated.candidates[0].evidenceFiles.some(
      (file) => file.path === "human-feedback-latest.json"
    )
  );
});
