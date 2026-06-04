import assert from "node:assert/strict";
import test from "node:test";
import {
  validCurrencyLedgerEntry,
  validOwnershipClaim,
  validPetPackageManifest,
  validScoreReport,
  validateCurrencyLedgerEntry,
  validateOwnershipClaim,
  validatePetPackageManifest,
  validateScoreReport
} from "./index.js";

test("valid pet package manifest passes", () => {
  assert.deepEqual(validatePetPackageManifest(validPetPackageManifest), {
    ok: true,
    errors: []
  });
});

test("manifest without petId fails", () => {
  const manifest = { ...validPetPackageManifest, petId: "" };
  const validation = validatePetPackageManifest(manifest);

  assert.equal(validation.ok, false);
  assert.ok(validation.errors.includes("petId must be a non-empty string"));
});

test("valid ownership claim passes", () => {
  assert.equal(validateOwnershipClaim(validOwnershipClaim).ok, true);
});

test("valid score report passes", () => {
  assert.equal(validateScoreReport(validScoreReport).ok, true);
});

test("valid currency ledger entry passes", () => {
  assert.equal(validateCurrencyLedgerEntry(validCurrencyLedgerEntry).ok, true);
});
