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

export function formatImportEvidenceDetails(row = {}) {
  const previewPath = row.importPreviewPath || "No preview path";

  return {
    label: row.importEvidenceLabel || "No import evidence",
    previewPath,
    hasPreviewPath: previewPath !== "No preview path"
  };
}

export function createImportDraftListModel(response = { drafts: [] }) {
  const drafts = Array.isArray(response.drafts) ? response.drafts : [];
  const summary = {
    total: drafts.length,
    ready: 0,
    blocked: 0,
    inProgress: 0,
    submitted: 0
  };

  const rows = drafts.map((draft) => {
    const status = draft.status ?? "in-progress";
    if (status === "ready") {
      summary.ready += 1;
    } else if (status === "blocked") {
      summary.blocked += 1;
    } else if (status === "submitted") {
      summary.submitted += 1;
    } else {
      summary.inProgress += 1;
    }

    const canSubmit = status === "ready";

    return {
      id: draft.id ?? "",
      petId: draft.petId ?? "",
      status,
      statusLabel: status,
      reason: draft.readiness?.reason ?? "",
      scoreReportId: draft.scoreReportId ?? "",
      submissionId: draft.submissionId ?? "",
      createdAt: draft.createdAt ?? "",
      canSubmit,
      actions: canSubmit ? ["submit"] : []
    };
  });

  return {
    summary,
    rows
  };
}

export function createApprovedPetRegistryModel(response = { items: [] }) {
  const items = Array.isArray(response.items) ? response.items : [];

  const rows = items.map((item) => {
    const sourceKind = item.source?.kind ?? "unknown-source";
    const totalScore = Number(item.totalScore ?? 0);
    const motionSheetCount = Number(item.assets?.motionSheetCount ?? 0);
    const submissionId = item.submissionId ?? "";
    const importDraftId = item.importDraftId ?? "";
    const scoreReportId = item.scoreReportId ?? "";
    const approvedAt = item.approvedAt ?? "";

    return {
      petId: item.petId ?? "",
      displayName: item.displayName ?? item.petId ?? "Unnamed pet",
      ownerUserId: item.ownerUserId ?? "",
      sourceKind,
      totalScore,
      motionSheetCount,
      previewPath: item.assets?.previewPath ?? "",
      submissionId,
      importDraftId,
      scoreReportId,
      approvedAt,
      submissionLabel: submissionId ? `Submission ${submissionId}` : "No submission",
      importDraftLabel: importDraftId ? `Draft ${importDraftId}` : "No import draft",
      scoreReportLabel: scoreReportId ? `Score ${scoreReportId}` : "No score report",
      approvedAtLabel: approvedAt ? `Approved ${approvedAt}` : "Approved time unknown",
      assetLabel: `${sourceKind} / score ${totalScore} / ${motionSheetCount} motion sheets`
    };
  });

  return {
    summary: {
      total: rows.length
    },
    rows
  };
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
    const publishedFeedPost = item.publishedFeedPost ?? null;
    const publishedFeedPostId = publishedFeedPost?.id ?? "";
    const importSummary = item.importDraft?.importSummary ?? {};
    const importSourceKind = importSummary.source?.kind ?? "";
    const importPreviewPath = importSummary.assets?.previewPath ?? "";
    const motionSheets = Array.isArray(importSummary.assets?.motionSheets)
      ? importSummary.assets.motionSheets
      : [];
    const motionSheetCount = motionSheets.length;

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
      riskItems: risks,
      riskLabel: risks.length === 1 ? "1 risk" : `${risks.length} risks`,
      latestReview: reviews.at(-1) ?? null,
      reviews,
      rewardLedgerEntries: Array.isArray(item.rewardLedgerEntries)
        ? item.rewardLedgerEntries
        : [],
      outstandingReward: Number(item.outstandingReward ?? 0),
      outstandingRewardLabel: formatReward(Number(item.outstandingReward ?? 0)),
      publishedFeedPostId,
      feedPublicationStatus: publishedFeedPostId ? "published" : "unpublished",
      feedPublicationLabel: publishedFeedPostId
        ? `Feed: ${publishedFeedPostId}`
        : "Feed: unpublished",
      importSourceKind,
      importPreviewPath,
      motionSheetCount,
      importEvidenceLabel: importSourceKind
        ? `${importSourceKind} / ${motionSheetCount} motion sheets`
        : "No import evidence",
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
