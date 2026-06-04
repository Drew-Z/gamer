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
