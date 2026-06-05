const STATUS_ORDER = ["pending", "held", "approved", "rejected", "revoked"];

export function formatReward(amount) {
  if (amount > 0) {
    return `+${amount} PC`;
  }

  return `${amount} PC`;
}

export function actionsForStatus(status) {
  if (status === "pending") {
    return ["approve", "held", "reject"];
  }

  if (status === "held") {
    return ["approve", "reject"];
  }

  if (status === "approved") {
    return ["revoke"];
  }

  return [];
}

export function createFantasyPetImportPayload(input = {}) {
  return {
    statePath: String(input.statePath ?? "").trim(),
    ownershipClaimId: String(input.ownershipClaimId ?? "").trim()
  };
}

export function formatImportDraftStatus(draft = {}) {
  const status = draft.status ?? "unknown";
  const id = draft.id ?? "unknown draft";
  const petId = draft.petId ?? "unknown pet";

  return `Created ${status} draft ${id} for ${petId}.`;
}

export function createReviewDashboardModel(queue = { items: [] }) {
  const items = Array.isArray(queue.items) ? queue.items : [];
  const counts = Object.fromEntries(STATUS_ORDER.map((status) => [status, 0]));
  let outstandingReward = 0;

  const rows = items.map((item) => {
    const submission = item.submission ?? {};
    const scoreReport = item.scoreReport ?? {};
    const reviews = Array.isArray(item.reviews) ? item.reviews : [];
    const risks = Array.isArray(scoreReport.risks) ? scoreReport.risks : [];
    const rewardRecommendation = scoreReport.rewardRecommendation ?? {};
    const status = submission.status ?? "pending";

    if (Object.hasOwn(counts, status)) {
      counts[status] += 1;
    }
    outstandingReward += Number(item.outstandingReward ?? 0);

    return {
      submissionId: submission.id ?? "",
      petId: submission.petId ?? "",
      userId: submission.userId ?? "",
      status,
      submittedAt: submission.submittedAt ?? "",
      totalScore: Number(scoreReport.totalScore ?? 0),
      breakdown: scoreReport.breakdown ?? {},
      recommendedReward: Number(rewardRecommendation.amount ?? 0),
      recommendedRewardLabel: formatReward(Number(rewardRecommendation.amount ?? 0)),
      recommendationReason: rewardRecommendation.reason ?? "",
      risks,
      riskLabel: risks.length === 1 ? "1 risk" : `${risks.length} risks`,
      latestReview: reviews.at(-1) ?? null,
      reviews,
      rewardLedgerEntries: Array.isArray(item.rewardLedgerEntries)
        ? item.rewardLedgerEntries
        : [],
      outstandingReward: Number(item.outstandingReward ?? 0),
      outstandingRewardLabel: formatReward(Number(item.outstandingReward ?? 0)),
      actions: actionsForStatus(status)
    };
  });

  return {
    summary: {
      total: items.length,
      ...counts,
      outstandingReward
    },
    rows
  };
}
