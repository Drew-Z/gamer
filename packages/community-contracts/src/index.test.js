import assert from "node:assert/strict";
import test from "node:test";
import { feedPosts, ledgerEntries, users, wallet } from "./index.js";

test("fixture users use stable demo IDs", () => {
  assert.equal(users[0].id, "user-demo-001");
  assert.equal(users[0].equippedPetId, "pet-stardust-001");
});

test("feed posts reference pet IDs", () => {
  assert.ok(feedPosts.length >= 2);
  assert.ok(feedPosts.every((post) => post.petId.startsWith("pet-")));
});

test("wallet balance equals posted ledger sum", () => {
  const postedTotal = ledgerEntries
    .filter((entry) => entry.status === "posted")
    .reduce((sum, entry) => sum + entry.amount, 0);

  assert.equal(wallet.balance, postedTotal);
});
