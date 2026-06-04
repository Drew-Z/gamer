import assert from "node:assert/strict";
import { mkdtemp, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import {
  resolveFantasyPetRuleState,
  StateSourceError
} from "./state-source.js";

test("inline state is returned without reading a file", async () => {
  const state = {
    petId: "inline-pet",
    currentStage: "preview-review"
  };

  const result = await resolveFantasyPetRuleState({
    state,
    statePath: "D:/workspace4Codex/fantasy-pet-rule/runs/demo/state.json"
  });

  assert.equal(result, state);
});

test("statePath reads and parses local JSON", async () => {
  const tempDir = await mkdtemp(path.join(os.tmpdir(), "gamer-state-source-"));
  const statePath = path.join(tempDir, "state.json");

  try {
    await writeFile(
      statePath,
      JSON.stringify({
        schema: "fantasy-pet.codex-state.v1",
        petId: "file-pet"
      }),
      "utf8"
    );

    const result = await resolveFantasyPetRuleState({ statePath });

    assert.equal(result.schema, "fantasy-pet.codex-state.v1");
    assert.equal(result.petId, "file-pet");
  } finally {
    await rm(tempDir, { recursive: true, force: true });
  }
});

test("missing state and statePath throws state_missing", async () => {
  await assert.rejects(
    () => resolveFantasyPetRuleState({}),
    (error) =>
      error instanceof StateSourceError &&
      error.code === "state_missing" &&
      error.status === 400
  );
});

test("unreadable statePath throws state_file_unreadable", async () => {
  await assert.rejects(
    () =>
      resolveFantasyPetRuleState({
        statePath: "D:/workspace4Codex/fantasy-pet-rule/runs/missing/state.json"
      }),
    (error) =>
      error instanceof StateSourceError &&
      error.code === "state_file_unreadable" &&
      error.status === 400
  );
});

test("invalid JSON statePath throws state_file_invalid_json", async () => {
  const tempDir = await mkdtemp(path.join(os.tmpdir(), "gamer-state-source-"));
  const statePath = path.join(tempDir, "state.json");

  try {
    await writeFile(statePath, "{bad-json", "utf8");

    await assert.rejects(
      () => resolveFantasyPetRuleState({ statePath }),
      (error) =>
        error instanceof StateSourceError &&
        error.code === "state_file_invalid_json" &&
        error.status === 400
    );
  } finally {
    await rm(tempDir, { recursive: true, force: true });
  }
});
