export function summarizeFantasyPetRuleState(state) {
  if (!state || typeof state !== "object") {
    return {
      status: "blocked",
      reason: "state is missing",
      evidence: []
    };
  }

  if (Array.isArray(state.blockers) && state.blockers.length > 0) {
    return {
      status: "blocked",
      reason: state.blockers[0].reason ?? "state has blockers",
      evidence: state.blockers
    };
  }

  if (state.preview?.userDecision === "keep") {
    return {
      status: "community-ready",
      reason: "preview accepted by user",
      evidence: [state.preview.urlOrPath].filter(Boolean)
    };
  }

  return {
    status: "in-progress",
    reason: `current stage is ${state.currentStage ?? "unknown"}`,
    evidence: []
  };
}

export function createFantasyPetRuleImportSummary(state) {
  const readiness = summarizeFantasyPetRuleState(state);

  return {
    readiness,
    importSummary: {
      source: {
        schema: state?.schema ?? "",
        petId: state?.petId ?? "",
        currentStage: state?.currentStage ?? "",
        baseIdentityStatus: state?.baseIdentity?.status ?? ""
      },
      review: {
        blockers: Array.isArray(state?.blockers) ? state.blockers : [],
        previewDecision: state?.preview?.userDecision ?? "",
        exportDecision: state?.export?.decision ?? "",
        exportStatus: state?.export?.status ?? ""
      },
      assets: {
        previewPath: state?.preview?.urlOrPath ?? "",
        exportArtifactPath: state?.export?.artifactPath ?? ""
      },
      notes: [
        "read-only summary; no fantasy-pet-rule files were mutated",
        "not a gamer.pet-package.v1 manifest"
      ]
    }
  };
}
