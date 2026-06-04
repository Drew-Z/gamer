const isObject = (value) =>
  value !== null && typeof value === "object" && !Array.isArray(value);

const requireString = (errors, value, field) => {
  if (typeof value !== "string" || value.trim() === "") {
    errors.push(`${field} must be a non-empty string`);
  }
};

const requireNumber = (errors, value, field) => {
  if (typeof value !== "number" || Number.isNaN(value)) {
    errors.push(`${field} must be a number`);
  }
};

const result = (errors) => ({ ok: errors.length === 0, errors });

export function validatePetPackageManifest(manifest) {
  const errors = [];

  if (!isObject(manifest)) {
    return result(["manifest must be an object"]);
  }

  requireString(errors, manifest.schema, "schema");
  requireString(errors, manifest.petId, "petId");
  requireString(errors, manifest.displayName, "displayName");
  requireString(errors, manifest.ownerUserId, "ownerUserId");

  if (!isObject(manifest.source)) {
    errors.push("source must be an object");
  } else {
    requireString(errors, manifest.source.kind, "source.kind");
    requireString(errors, manifest.source.runId, "source.runId");
    requireString(errors, manifest.source.statePath, "source.statePath");
  }

  if (!isObject(manifest.assets)) {
    errors.push("assets must be an object");
  } else {
    requireString(errors, manifest.assets.baseImage, "assets.baseImage");
    requireString(errors, manifest.assets.previewImage, "assets.previewImage");
    if (!Array.isArray(manifest.assets.motionSheets)) {
      errors.push("assets.motionSheets must be an array");
    }
  }

  requireString(errors, manifest.license, "license");
  requireString(errors, manifest.scoreReport, "scoreReport");

  return result(errors);
}

export function validateOwnershipClaim(claim) {
  const errors = [];

  if (!isObject(claim)) {
    return result(["claim must be an object"]);
  }

  requireString(errors, claim.schema, "schema");
  requireString(errors, claim.claimId, "claimId");
  requireString(errors, claim.userId, "userId");
  requireString(errors, claim.petId, "petId");
  requireString(errors, claim.claimType, "claimType");
  requireString(errors, claim.attestation, "attestation");
  requireString(errors, claim.submittedAt, "submittedAt");
  requireString(errors, claim.reviewStatus, "reviewStatus");

  if (!Array.isArray(claim.sourceReferences)) {
    errors.push("sourceReferences must be an array");
  }

  return result(errors);
}

export function validateScoreReport(report) {
  const errors = [];

  if (!isObject(report)) {
    return result(["report must be an object"]);
  }

  requireString(errors, report.schema, "schema");
  requireString(errors, report.petId, "petId");
  requireNumber(errors, report.totalScore, "totalScore");

  if (!isObject(report.breakdown)) {
    errors.push("breakdown must be an object");
  } else {
    for (const field of [
      "packageCompleteness",
      "visualQuality",
      "actionCoverage",
      "identityConsistency",
      "previewEvidence",
      "licenseReadiness"
    ]) {
      requireNumber(errors, report.breakdown[field], `breakdown.${field}`);
    }
  }

  if (!isObject(report.rewardRecommendation)) {
    errors.push("rewardRecommendation must be an object");
  } else {
    if (typeof report.rewardRecommendation.grant !== "boolean") {
      errors.push("rewardRecommendation.grant must be a boolean");
    }
    requireNumber(errors, report.rewardRecommendation.amount, "rewardRecommendation.amount");
    requireString(errors, report.rewardRecommendation.reason, "rewardRecommendation.reason");
  }

  if (!Array.isArray(report.risks)) {
    errors.push("risks must be an array");
  }

  return result(errors);
}

export function validateCurrencyLedgerEntry(entry) {
  const errors = [];

  if (!isObject(entry)) {
    return result(["entry must be an object"]);
  }

  requireString(errors, entry.schema, "schema");
  requireString(errors, entry.entryId, "entryId");
  requireString(errors, entry.userId, "userId");
  requireNumber(errors, entry.amount, "amount");
  requireString(errors, entry.sourceType, "sourceType");
  requireString(errors, entry.sourceId, "sourceId");
  requireString(errors, entry.status, "status");
  requireString(errors, entry.createdAt, "createdAt");

  return result(errors);
}
