import assert from "node:assert/strict";
import test from "node:test";
import {
  validCurrencyLedgerEntry,
  validOwnershipClaim,
  validPetPackageBundle,
  validPetPackageManifest,
  validScoreReport,
  validateCurrencyLedgerEntry,
  validateOwnershipClaim,
  validatePetPackageBundle,
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

test("manifest rejects unsupported source kind and invalid motion sheets", () => {
  const manifest = {
    ...validPetPackageManifest,
    source: {
      ...validPetPackageManifest.source,
      kind: "manual-upload"
    },
    assets: {
      ...validPetPackageManifest.assets,
      motionSheets: ["motion/sheets/idle.png", "", 42]
    }
  };

  const validation = validatePetPackageManifest(manifest);

  assert.equal(validation.ok, false);
  assert.ok(validation.errors.includes("source.kind must be one of fantasy-pet-rule"));
  assert.ok(
    validation.errors.includes("assets.motionSheets[1] must be a non-empty string")
  );
  assert.ok(
    validation.errors.includes("assets.motionSheets[2] must be a non-empty string")
  );
});

test("valid pet package bundle passes", () => {
  const validation = validatePetPackageBundle(validPetPackageBundle);

  assert.deepEqual(validation, {
    ok: true,
    errors: []
  });
});

test("pet package bundle rejects mismatched pet and owner identities", () => {
  const bundle = {
    ...validPetPackageBundle,
    ownershipClaim: {
      ...validPetPackageBundle.ownershipClaim,
      userId: "user-other-001",
      petId: "pet-other-001"
    },
    scoreReport: {
      ...validPetPackageBundle.scoreReport,
      petId: "pet-score-other-001"
    }
  };

  const validation = validatePetPackageBundle(bundle);

  assert.equal(validation.ok, false);
  assert.ok(
    validation.errors.includes("manifest.petId must match ownershipClaim.petId")
  );
  assert.ok(
    validation.errors.includes("manifest.ownerUserId must match ownershipClaim.userId")
  );
  assert.ok(validation.errors.includes("manifest.petId must match scoreReport.petId"));
});

test("valid ownership claim passes", () => {
  assert.equal(validateOwnershipClaim(validOwnershipClaim).ok, true);
});

test("ownership claim rejects invalid claim type status and source references", () => {
  const claim = {
    ...validOwnershipClaim,
    claimType: "unknown",
    reviewStatus: "maybe",
    sourceReferences: ["", 42]
  };

  const validation = validateOwnershipClaim(claim);

  assert.equal(validation.ok, false);
  assert.ok(
    validation.errors.includes(
      "claimType must be one of original-created, licensed, derivative-permitted"
    )
  );
  assert.ok(
    validation.errors.includes(
      "reviewStatus must be one of pending, approved, rejected, revoked"
    )
  );
  assert.ok(
    validation.errors.includes("sourceReferences[0] must be a non-empty string")
  );
  assert.ok(
    validation.errors.includes("sourceReferences[1] must be a non-empty string")
  );
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
