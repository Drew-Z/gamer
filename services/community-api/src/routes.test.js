import assert from "node:assert/strict";
import test from "node:test";
import { handleCommunityRequest } from "./routes.js";
import { createCommunityStore } from "./store.js";

test("health route reports service status", () => {
  const response = handleCommunityRequest("GET", "/health");

  assert.equal(response.status, 200);
  assert.equal(response.body.ok, true);
  assert.equal(response.body.service, "community-api");
});

test("feed route returns fixture posts", () => {
  const response = handleCommunityRequest("GET", "/v1/feed", {
    store: createCommunityStore()
  });

  assert.equal(response.status, 200);
  assert.ok(response.body.items.length >= 2);
  assert.ok(response.body.items.every((post) => post.petId));
});

test("check-in route accepts date body and updates wallet", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest("POST", "/v1/check-in", {
    store,
    body: {
      date: "2026-06-05"
    }
  });

  assert.equal(response.status, 200);
  assert.equal(response.body.checkIn.date, "2026-06-05");
  assert.equal(response.body.wallet.balance, 100);
});

test("submission route creates pending submission", () => {
  const store = createCommunityStore();
  const response = handleCommunityRequest("POST", "/v1/submissions", {
    store,
    body: {
      petId: "pet-new-001",
      ownershipClaimId: "claim-pet-new-001",
      scoreReportId: "score-pet-new-001"
    }
  });

  assert.equal(response.status, 201);
  assert.equal(response.body.status, "pending");
  assert.equal(response.body.petId, "pet-new-001");
});

test("admin review route approves submission and updates wallet", () => {
  const store = createCommunityStore();
  const created = handleCommunityRequest("POST", "/v1/submissions", {
    store,
    body: {
      petId: "pet-new-001",
      ownershipClaimId: "claim-pet-new-001",
      scoreReportId: "score-pet-new-001"
    }
  });

  const reviewed = handleCommunityRequest("POST", "/v1/admin/reviews", {
    store,
    body: {
      submissionId: created.body.id,
      status: "approved",
      reviewer: "admin-demo",
      rewardAmount: 55
    }
  });
  const wallet = handleCommunityRequest("GET", "/v1/wallet/me", { store });

  assert.equal(reviewed.status, 200);
  assert.equal(reviewed.body.status, "approved");
  assert.equal(reviewed.body.rewardEntry.amount, 55);
  assert.equal(wallet.body.balance, 145);
});

test("unsupported route returns 404", () => {
  const response = handleCommunityRequest("GET", "/missing");

  assert.equal(response.status, 404);
  assert.equal(response.body.error, "not_found");
  assert.equal(response.body.path, "/missing");
});
