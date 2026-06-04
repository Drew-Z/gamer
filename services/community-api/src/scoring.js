const clampReward = (score) => Math.min(80, Math.max(0, Math.round(score)));

const hasText = (value) => typeof value === "string" && value.trim() !== "";

export function createScoreReportFromImportDraft(draft) {
  const readiness = draft.readiness ?? {};
  const summary = draft.importSummary ?? {};
  const source = summary.source ?? {};
  const review = summary.review ?? {};
  const assets = summary.assets ?? {};
  const blockers = Array.isArray(review.blockers) ? review.blockers : [];

  const packageCompleteness = hasText(assets.exportArtifactPath)
    ? 20
    : hasText(assets.previewPath)
      ? 12
      : 4;
  const visualQuality =
    readiness.status === "community-ready"
      ? 18
      : readiness.status === "blocked"
        ? 0
        : 8;
  const actionCoverage = review.exportStatus === "ready" ? 12 : 6;
  const identityConsistency = source.baseIdentityStatus === "accepted" ? 16 : 8;
  const previewEvidence =
    review.previewDecision === "keep" && hasText(assets.previewPath) ? 10 : 0;
  const licenseReadiness = hasText(draft.ownershipClaimId) ? 8 : 0;

  const totalScore =
    packageCompleteness +
    visualQuality +
    actionCoverage +
    identityConsistency +
    previewEvidence +
    licenseReadiness;

  const risks = [];
  if (readiness.status === "blocked") {
    risks.push("fantasy-pet-rule state is blocked");
  }
  if (blockers.length > 0) {
    risks.push("state blockers are present");
  }
  if (!hasText(draft.ownershipClaimId)) {
    risks.push("ownership claim is missing");
  }

  const grant = totalScore >= 70 && blockers.length === 0 && readiness.status !== "blocked";

  return {
    schema: "gamer.pet-score-report.v1",
    reportId: `score-${draft.id}`,
    petId: draft.petId || source.petId || "",
    totalScore,
    breakdown: {
      packageCompleteness,
      visualQuality,
      actionCoverage,
      identityConsistency,
      previewEvidence,
      licenseReadiness
    },
    rewardRecommendation: {
      grant,
      amount: grant ? clampReward(totalScore) : 0,
      reason: grant
        ? "Community-ready import has enough accepted evidence for a starter reward."
        : "Import draft is not ready for reward grant."
    },
    risks
  };
}
