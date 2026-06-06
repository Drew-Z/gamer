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

test("score report rejects negative score fields", () => {
  const report = {
    ...validScoreReport,
    totalScore: -1,
    breakdown: {
      ...validScoreReport.breakdown,
      visualQuality: -2
    }
  };

  const validation = validateScoreReport(report);

  assert.equal(validation.ok, false);
  assert.ok(validation.errors.includes("totalScore must be a non-negative number"));
  assert.ok(
    validation.errors.includes(
      "breakdown.visualQuality must be a non-negative number"
    )
  );
});

test("score report rejects invalid reward recommendation amount", () => {
  const report = {
    ...validScoreReport,
    rewardRecommendation: {
      ...validScoreReport.rewardRecommendation,
      grant: false,
      amount: 10
    }
  };

  const validation = validateScoreReport(report);

  assert.equal(validation.ok, false);
  assert.ok(
    validation.errors.includes(
      "rewardRecommendation.amount must be 0 when grant is false"
    )
  );
});

test("valid currency ledger entry passes", () => {
  assert.equal(validateCurrencyLedgerEntry(validCurrencyLedgerEntry).ok, true);
});

test("ledger entry rejects invalid amount and status", () => {
  const entry = {
    ...validCurrencyLedgerEntry,
    amount: 1.5,
    status: "unknown"
  };

  const validation = validateCurrencyLedgerEntry(entry);

  assert.equal(validation.ok, false);
  assert.ok(validation.errors.includes("amount must be an integer"));
  assert.ok(validation.errors.includes("status must be one of posted, pending, voided"));
});

test("ledger entry requires reversal amounts to be negative", () => {
  const entry = {
    ...validCurrencyLedgerEntry,
    sourceType: "submission-reward-reversal",
    amount: 80
  };

  const validation = validateCurrencyLedgerEntry(entry);

  assert.equal(validation.ok, false);
  assert.ok(
    validation.errors.includes(
      "amount must be negative for submission-reward-reversal"
    )
  );
});
