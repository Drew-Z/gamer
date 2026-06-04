import assert from "node:assert/strict";
import test from "node:test";
import {
  getActionForFeedNavigation,
  getActionForLaunchStage
} from "./index.js";

test("launch loading maps to app-loading action", () => {
  assert.equal(getActionForLaunchStage("loading").id, "app-loading");
});

test("launch ready maps to bubble-open action", () => {
  assert.equal(getActionForLaunchStage("ready").id, "bubble-open");
});

test("feed directions map to pet navigation actions", () => {
  assert.equal(getActionForFeedNavigation("next").id, "feed-next");
  assert.equal(getActionForFeedNavigation("previous").id, "feed-previous");
  assert.equal(getActionForFeedNavigation("skip").id, "feed-skip");
});
