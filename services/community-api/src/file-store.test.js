import assert from "node:assert/strict";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { createFileBackedCommunityStore } from "./file-store.js";

test("file-backed community store restores written state", () => {
  const filePath = path.join(
    mkdtempSync(path.join(tmpdir(), "gamer-community-store-")),
    "community-store.json"
  );
  const firstStore = createFileBackedCommunityStore({ filePath });

  firstStore.claimDailyCheckIn("user-demo-001", "2026-06-11");

  const restoredStore = createFileBackedCommunityStore({ filePath });
  const wallet = restoredStore.getWallet("user-demo-001");
  const home = restoredStore.getCommunityHome("user-demo-001", "2026-06-11");

  assert.equal(wallet.balance, 100);
  assert.equal(home.dailyCheckIn.claimed, true);
  assert.equal(home.dailyCheckIn.ledgerEntryId, "ledger-checkin-2026-06-11");
});

test("file-backed community store can be disabled for memory mode", () => {
  const store = createFileBackedCommunityStore({
    env: {
      COMMUNITY_API_STORE_FILE: "memory"
    }
  });

  store.claimDailyCheckIn("user-demo-001", "2026-06-11");

  assert.equal(store.getWallet("user-demo-001").balance, 100);
});
