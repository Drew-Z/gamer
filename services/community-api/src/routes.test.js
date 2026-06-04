import assert from "node:assert/strict";
import test from "node:test";
import { handleCommunityRequest } from "./routes.js";

test("health route reports service status", () => {
  const response = handleCommunityRequest("GET", "/health");

  assert.equal(response.status, 200);
  assert.equal(response.body.ok, true);
  assert.equal(response.body.service, "community-api");
});

test("feed route returns fixture posts", () => {
  const response = handleCommunityRequest("GET", "/v1/feed");

  assert.equal(response.status, 200);
  assert.ok(response.body.items.length >= 2);
  assert.ok(response.body.items.every((post) => post.petId));
});

test("unsupported route returns 404", () => {
  const response = handleCommunityRequest("GET", "/missing");

  assert.equal(response.status, 404);
  assert.equal(response.body.error, "not_found");
  assert.equal(response.body.path, "/missing");
});
