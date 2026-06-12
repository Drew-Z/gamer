import {
  createApprovedPetRegistryModel,
  createFantasyPetJobModel,
  createFantasyPetImportPayload,
  createFantasyPetPackageImportPayload,
  createImportDraftListModel,
  createReviewDecisionPayload,
  createReviewDashboardModel,
  formatHealthStatus,
  formatImportDraftStatus,
  formatImportEvidenceDetails,
  formatReward
} from "/src/reviewQueuePresenter.js";

const state = {
  filter: "all",
  approvedPetModel: createApprovedPetRegistryModel({ items: [] }),
  draftModel: createImportDraftListModel({ drafts: [] }),
  generationJobModel: createFantasyPetJobModel({}),
  gaReviewModel: {
    runRoot: "",
    count: 0,
    candidates: []
  },
  healthStatus: formatHealthStatus({}),
  selectedGenerationCandidateId: "",
  model: createReviewDashboardModel({ items: [] })
};

const elements = {
  list: document.querySelector("#queue-list"),
  statusLine: document.querySelector("#status-line"),
  refreshButton: document.querySelector("#refresh-button"),
  generationJobForm: document.querySelector("#generation-job-form"),
  generationJobIdInput: document.querySelector("#generation-job-id-input"),
  generationLoadButton: document.querySelector("#generation-load-button"),
  generationStatus: document.querySelector("#generation-status"),
  generationJobDetail: document.querySelector("#generation-job-detail"),
  generationJobId: document.querySelector("#generation-job-id"),
  generationJobProgress: document.querySelector("#generation-job-progress"),
  generationJobPackage: document.querySelector("#generation-job-package"),
  generationSecurity: document.querySelector("#generation-security"),
  generationCandidateList: document.querySelector("#generation-candidate-list"),
  generationAcceptButton: document.querySelector("#generation-accept-button"),
  generationReviseButton: document.querySelector("#generation-revise-button"),
  generationRejectButton: document.querySelector("#generation-reject-button"),
  generationPackageLink: document.querySelector("#generation-package-link"),
  generationImportClaimInput: document.querySelector("#generation-import-claim-input"),
  generationImportPackageButton: document.querySelector(
    "#generation-import-package-button"
  ),
  generationImportStatus: document.querySelector("#generation-import-status"),
  gaReviewStatus: document.querySelector("#ga-review-status"),
  gaReviewRefreshButton: document.querySelector("#ga-review-refresh-button"),
  gaReviewSummaryMetrics: document.querySelector("#ga-review-summary-metrics"),
  gaReviewList: document.querySelector("#ga-review-list"),
  importForm: document.querySelector("#import-form"),
  importButton: document.querySelector("#import-button"),
  importStatus: document.querySelector("#import-status"),
  statePathInput: document.querySelector("#state-path-input"),
  ownershipClaimInput: document.querySelector("#ownership-claim-input"),
  draftSummary: document.querySelector("#draft-summary"),
  draftList: document.querySelector("#draft-list"),
  approvedPetSummary: document.querySelector("#approved-pet-summary"),
  approvedPetList: document.querySelector("#approved-pet-list"),
  filters: [...document.querySelectorAll(".filter-button")],
  summary: {
    total: document.querySelector("#summary-total"),
    pending: document.querySelector("#summary-pending"),
    approved: document.querySelector("#summary-approved"),
    outstanding: document.querySelector("#summary-outstanding")
  },
  template: document.querySelector("#queue-item-template")
};

const labelForField = (field) =>
  field.replace(/[A-Z]/g, (letter) => ` ${letter.toLowerCase()}`);

const labelForAction = (action) => (action === "held" ? "hold" : action);

const reviewStatusForAction = (action) => {
  if (action === "approve") {
    return "approved";
  }

  if (action === "reject") {
    return "rejected";
  }

  return action;
};

const pathSegment = (value) => encodeURIComponent(String(value ?? "").trim());

const proxiedUrl = (path) => `/api${path}`;

const ZIP_SIGNATURES = {
  endOfCentralDirectory: 0x06054b50,
  centralDirectoryFileHeader: 0x02014b50,
  localFileHeader: 0x04034b50
};

const zipTextDecoder = new TextDecoder();

const safeZipFileName = (value) => {
  const baseName = String(value ?? "")
    .trim()
    .replace(/[^a-zA-Z0-9._-]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 80);

  return `${baseName || "fantasy-pet-package"}.zip`;
};

const GA_ISSUE_TAGS = [
  ["identity-drift", "Identity drift"],
  ["static-frames", "Static frames"],
  ["scale-pop", "Scale pop"],
  ["bad-transparency", "Bad transparency"],
  ["white-matte", "White matte"],
  ["cropped-body", "Cropped body"],
  ["wrong-action", "Wrong action"],
  ["too-noisy", "Too noisy"],
  ["weak-silhouette", "Weak silhouette"],
  ["style-mismatch", "Style mismatch"]
];

function findZipEndOfCentralDirectory(view) {
  const minimumSize = 22;
  const maximumCommentSize = 0xffff;
  const firstPossibleOffset = Math.max(
    0,
    view.byteLength - minimumSize - maximumCommentSize
  );

  for (let offset = view.byteLength - minimumSize; offset >= firstPossibleOffset; offset -= 1) {
    if (view.getUint32(offset, true) === ZIP_SIGNATURES.endOfCentralDirectory) {
      return offset;
    }
  }

  throw new Error("Package manifest was not found in the zip.");
}

function readZipCentralDirectoryEntries(buffer) {
  const view = new DataView(buffer);
  const endOffset = findZipEndOfCentralDirectory(view);
  const entryCount = view.getUint16(endOffset + 10, true);
  let offset = view.getUint32(endOffset + 16, true);
  const entries = [];

  for (let index = 0; index < entryCount; index += 1) {
    if (view.getUint32(offset, true) !== ZIP_SIGNATURES.centralDirectoryFileHeader) {
      throw new Error("Package zip central directory is not readable.");
    }

    const compressionMethod = view.getUint16(offset + 10, true);
    const compressedSize = view.getUint32(offset + 20, true);
    const uncompressedSize = view.getUint32(offset + 24, true);
    const fileNameLength = view.getUint16(offset + 28, true);
    const extraFieldLength = view.getUint16(offset + 30, true);
    const fileCommentLength = view.getUint16(offset + 32, true);
    const localHeaderOffset = view.getUint32(offset + 42, true);
    const fileNameBytes = new Uint8Array(buffer, offset + 46, fileNameLength);

    entries.push({
      name: zipTextDecoder.decode(fileNameBytes),
      compressionMethod,
      compressedSize,
      uncompressedSize,
      localHeaderOffset
    });

    offset += 46 + fileNameLength + extraFieldLength + fileCommentLength;
  }

  return entries;
}

async function inflateRawZipEntry(compressedBytes) {
  if (!globalThis.DecompressionStream) {
    throw new Error("This browser cannot decompress zip entries.");
  }

  const stream = new Blob([compressedBytes])
    .stream()
    .pipeThrough(new DecompressionStream("deflate-raw"));

  return new Uint8Array(await new Response(stream).arrayBuffer());
}

async function readZipTextEntry(buffer, entryName) {
  const view = new DataView(buffer);
  const entries = readZipCentralDirectoryEntries(buffer);
  const entry =
    entries.find((item) => item.name === entryName) ||
    entries.find((item) => item.name.endsWith(`/${entryName}`));

  if (!entry) {
    throw new Error(`${entryName} was not found in the package.`);
  }

  if (
    view.getUint32(entry.localHeaderOffset, true) !== ZIP_SIGNATURES.localFileHeader
  ) {
    throw new Error(`${entryName} has an unreadable zip entry header.`);
  }

  const fileNameLength = view.getUint16(entry.localHeaderOffset + 26, true);
  const extraFieldLength = view.getUint16(entry.localHeaderOffset + 28, true);
  const dataOffset = entry.localHeaderOffset + 30 + fileNameLength + extraFieldLength;
  const compressedBytes = new Uint8Array(buffer, dataOffset, entry.compressedSize);
  let bytes;

  if (entry.compressionMethod === 0) {
    bytes = compressedBytes;
  } else if (entry.compressionMethod === 8) {
    bytes = await inflateRawZipEntry(compressedBytes);
  } else {
    throw new Error(`${entryName} uses an unsupported zip compression method.`);
  }

  if (entry.uncompressedSize > 0 && bytes.byteLength !== entry.uncompressedSize) {
    throw new Error(`${entryName} did not decompress to the expected size.`);
  }

  return zipTextDecoder.decode(bytes);
}

function setActiveFilter(filter) {
  state.filter = filter;
  for (const filterButton of elements.filters) {
    filterButton.classList.toggle("is-active", filterButton.dataset.filter === filter);
  }
}

async function requestJson(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    headers: {
      "Content-Type": "application/json"
    },
    ...options
  });
  const body = await response.json();

  if (!response.ok) {
    throw new Error(body.message ?? body.error ?? `Request failed: ${response.status}`);
  }

  return body;
}

async function requestLocalJson(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json"
    },
    ...options
  });
  const body = await response.json();

  if (!response.ok) {
    throw new Error(body.message ?? body.error ?? `Request failed: ${response.status}`);
  }

  return body;
}

function renderSummary() {
  elements.summary.total.textContent = state.model.summary.total;
  elements.summary.pending.textContent = state.model.summary.pending;
  elements.summary.approved.textContent = state.model.summary.approved;
  elements.summary.outstanding.textContent = formatReward(
    state.model.summary.outstandingReward
  );
}

function renderDraftList() {
  elements.draftSummary.textContent =
    `${state.draftModel.summary.total} drafts / ${state.draftModel.summary.blocked} blocked`;
  elements.draftList.replaceChildren();

  if (state.draftModel.rows.length === 0) {
    const empty = document.createElement("div");
    empty.className = "draft-empty";
    empty.textContent = "No import drafts yet.";
    elements.draftList.append(empty);
    return;
  }

  for (const row of state.draftModel.rows) {
    const node = document.createElement("article");
    node.className = "draft-item";
    node.dataset.status = row.status;

    const heading = document.createElement("div");
    heading.className = "draft-item-heading";

    const title = document.createElement("strong");
    title.textContent = row.petId || row.id;

    const status = document.createElement("span");
    status.className = "draft-status";
    status.dataset.status = row.status;
    status.textContent = row.statusLabel;

    const reason = document.createElement("p");
    reason.textContent = row.reason || "No readiness reason.";

    const meta = document.createElement("small");
    meta.textContent = row.submissionId
      ? `${row.id} -> ${row.submissionId}`
      : row.scoreReportId
        ? `${row.id} -> ${row.scoreReportId}`
        : row.id;

    const actions = document.createElement("div");
    actions.className = "draft-actions";
    for (const action of row.actions) {
      const button = document.createElement("button");
      button.className = "action-button primary";
      button.type = "button";
      button.textContent = labelForAction(action);
      button.addEventListener("click", () => submitImportDraft(row.id));
      actions.append(button);
    }

    heading.append(title, status);
    node.append(heading, reason, meta, actions);
    elements.draftList.append(node);
  }
}

function renderApprovedPetList() {
  elements.approvedPetSummary.textContent =
    `${state.approvedPetModel.summary.total} approved`;
  elements.approvedPetList.replaceChildren();

  if (state.approvedPetModel.rows.length === 0) {
    const empty = document.createElement("div");
    empty.className = "approved-pet-empty";
    empty.textContent = "No approved pets yet.";
    elements.approvedPetList.append(empty);
    return;
  }

  for (const row of state.approvedPetModel.rows) {
    const node = document.createElement("article");
    node.className = "approved-pet-item";

    const title = document.createElement("strong");
    title.textContent = row.displayName;

    const petMeta = document.createElement("small");
    petMeta.textContent = `${row.petId} by ${row.ownerUserId}`;

    const assetLabel = document.createElement("span");
    assetLabel.className = "approved-pet-asset";
    assetLabel.textContent = row.assetLabel;

    const previewPath = document.createElement("code");
    previewPath.textContent = row.previewPath || "No preview path";

    const previewLink = document.createElement(row.canOpenPreview ? "a" : "span");
    previewLink.className = "approved-pet-preview-link";
    previewLink.textContent = row.previewLinkLabel;
    if (row.canOpenPreview) {
      previewLink.href = proxiedUrl(row.previewUrl);
      previewLink.target = "_blank";
      previewLink.rel = "noreferrer";
    }

    const packageArtifact = document.createElement("code");
    packageArtifact.className = "approved-pet-package-path";
    packageArtifact.textContent = row.packageArtifactLabel;

    const traceList = document.createElement("ul");
    traceList.className = "approved-pet-trace";
    for (const label of [
      row.approvedAtLabel,
      row.scoreReportLabel,
      row.importDraftLabel,
      row.submissionLabel
    ]) {
      const item = document.createElement("li");
      item.textContent = label;
      traceList.append(item);
    }

    const actions = document.createElement("div");
    actions.className = "approved-pet-actions";
    if (row.canFocusSubmission) {
      const button = document.createElement("button");
      button.className = "approved-pet-focus-button";
      button.type = "button";
      button.textContent = row.focusSubmissionLabel;
      button.addEventListener("click", () => focusSubmission(row.submissionId));
      actions.append(button);
    }
    if (row.canRevokeSubmission) {
      const button = document.createElement("button");
      button.className = "approved-pet-revoke-button";
      button.type = "button";
      button.textContent = row.revokeSubmissionLabel;
      button.addEventListener("click", () => revokeApprovedPet(row.submissionId));
      actions.append(button);
    }

    node.append(
      title,
      petMeta,
      assetLabel,
      previewPath,
      previewLink,
      packageArtifact,
      traceList,
      actions
    );
    elements.approvedPetList.append(node);
  }
}

function selectedGenerationCandidate() {
  return state.generationJobModel.candidates.find(
    (candidate) => candidate.downloadId === state.selectedGenerationCandidateId
  );
}

function renderGenerationJob() {
  const model = state.generationJobModel;
  const hasJob = Boolean(model.appJobId);
  elements.generationJobDetail.hidden = !hasJob;

  if (!hasJob) {
    return;
  }

  const selected = selectedGenerationCandidate();
  elements.generationJobId.textContent = model.appJobId;
  elements.generationJobProgress.textContent =
    `${model.progressStatus || model.status} / ${model.currentStage || "unknown"}`;
  elements.generationJobPackage.textContent = model.downloadReady
    ? `Package ${model.packageStatus || "ready"}`
    : `Next ${model.nextAction || "wait"}`;
  elements.generationSecurity.textContent = model.hasSafeSecurity
    ? "Security clean: no paths, no worker commands, no agent promotion."
    : "Security report needs review.";

  elements.generationCandidateList.replaceChildren();
  if (model.candidates.length === 0) {
    const empty = document.createElement("div");
    empty.className = "generation-empty";
    empty.textContent = "No candidate artifacts yet.";
    elements.generationCandidateList.append(empty);
  }

  for (const candidate of model.candidates) {
    const node = document.createElement("article");
    node.className = "generation-candidate";
    node.classList.toggle(
      "is-selected",
      candidate.downloadId === state.selectedGenerationCandidateId
    );

    const image = document.createElement("img");
    image.alt = `${candidate.actionId || "candidate"} ${candidate.downloadId}`;
    image.src = proxiedUrl(candidate.downloadUrl);
    image.loading = "lazy";

    const body = document.createElement("div");
    body.className = "generation-candidate-body";

    const title = document.createElement("strong");
    title.textContent = `${candidate.actionId || "candidate"} / ${candidate.downloadId}`;

    const meta = document.createElement("small");
    meta.textContent = `${candidate.status} ${candidate.reviewDecision || ""}`.trim();

    const button = document.createElement("button");
    button.className = "secondary-button";
    button.type = "button";
    button.textContent = "Select";
    button.addEventListener("click", () => {
      state.selectedGenerationCandidateId = candidate.downloadId;
      renderGenerationJob();
    });

    body.append(title, meta, button);
    node.append(image, body);
    elements.generationCandidateList.append(node);
  }

  const canReview = Boolean(selected?.canReview);
  elements.generationAcceptButton.disabled = !canReview;
  elements.generationReviseButton.disabled = !canReview;
  elements.generationRejectButton.disabled = !canReview;

  const packageHref = model.packageLink ? proxiedUrl(model.packageLink) : "";
  elements.generationPackageLink.hidden = !model.downloadReady || !packageHref;
  elements.generationPackageLink.href = packageHref || "#";
  elements.generationImportPackageButton.disabled = !model.downloadReady || !packageHref;
  elements.generationImportClaimInput.placeholder = model.appJobId
    ? `claim-${model.appJobId}`
    : "claim-app-job-id";
}

function formatGaFileSize(sizeBytes) {
  const bytes = Number(sizeBytes) || 0;
  if (bytes >= 1024 * 1024) {
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }
  if (bytes >= 1024) {
    return `${Math.round(bytes / 1024)} KB`;
  }
  return `${bytes} B`;
}

function formatGaDate(value) {
  const time = String(value || "").trim();
  return time ? time.replace("T", " ").replace(/\.\d{3}Z$/u, "Z") : "unknown time";
}

function createGaEvidenceLink(file) {
  const link = document.createElement("a");
  link.className = "ga-evidence-link";
  link.href = file.url || "#";
  link.target = "_blank";
  link.rel = "noreferrer";

  const title = document.createElement("strong");
  title.textContent = file.label || file.path || "Evidence";

  const pathLabel = document.createElement("code");
  pathLabel.textContent = file.path || "missing path";

  const meta = document.createElement("small");
  meta.textContent = `${formatGaFileSize(file.sizeBytes)} / ${formatGaDate(file.updatedAt)}`;

  link.append(title, pathLabel, meta);
  return link;
}

function createGaHistoryItem(item, emptyLabel) {
  const node = document.createElement("li");
  if (!item) {
    node.textContent = emptyLabel;
    return node;
  }

  const heading = document.createElement("strong");
  heading.textContent = [
    item.decision || item.status || item.mode || "note",
    item.severity,
    item.actionId
  ]
    .filter(Boolean)
    .join(" / ");

  const meta = document.createElement("small");
  meta.textContent = [
    item.feedbackId || item.requestId,
    formatGaDate(item.createdAt),
    Array.isArray(item.tags) && item.tags.length > 0
      ? `tags: ${item.tags.join(", ")}`
      : ""
  ]
    .filter(Boolean)
    .join(" / ");

  node.append(heading, meta);
  if (item.notes) {
    const notes = document.createElement("p");
    notes.textContent = item.notes;
    node.append(notes);
  }
  if (item.promptPatch) {
    const patch = document.createElement("code");
    patch.textContent = item.promptPatch;
    node.append(patch);
  }
  return node;
}

function createGaCandidateDetails(candidate) {
  const details = document.createElement("details");
  details.className = "ga-review-details";

  const summary = document.createElement("summary");
  summary.textContent = "Evidence and history";
  details.append(summary);

  const grid = document.createElement("div");
  grid.className = "ga-review-detail-grid";

  const evidenceSection = document.createElement("section");
  const evidenceTitle = document.createElement("h4");
  evidenceTitle.textContent = "Evidence files";
  const evidenceList = document.createElement("div");
  evidenceList.className = "ga-evidence-list";
  const evidenceFiles = Array.isArray(candidate.evidenceFiles)
    ? candidate.evidenceFiles
    : [];
  if (evidenceFiles.length === 0) {
    const empty = document.createElement("p");
    empty.textContent = "No evidence files found.";
    evidenceList.append(empty);
  } else {
    for (const file of evidenceFiles) {
      evidenceList.append(createGaEvidenceLink(file));
    }
  }
  evidenceSection.append(evidenceTitle, evidenceList);

  const feedbackSection = document.createElement("section");
  const feedbackTitle = document.createElement("h4");
  feedbackTitle.textContent = "Feedback history";
  const feedbackList = document.createElement("ul");
  feedbackList.className = "ga-history-list";
  const feedbackHistory = Array.isArray(candidate.feedback?.history)
    ? candidate.feedback.history
    : [];
  if (feedbackHistory.length === 0) {
    feedbackList.append(createGaHistoryItem(null, "No feedback entries yet."));
  } else {
    for (const entry of feedbackHistory) {
      feedbackList.append(createGaHistoryItem(entry, "No feedback entries yet."));
    }
  }
  feedbackSection.append(feedbackTitle, feedbackList);

  const reworkSection = document.createElement("section");
  const reworkTitle = document.createElement("h4");
  reworkTitle.textContent = "Rework queue";
  const reworkList = document.createElement("ul");
  reworkList.className = "ga-history-list";
  const reworkRequests = Array.isArray(candidate.rework?.requests)
    ? candidate.rework.requests
    : [];
  if (reworkRequests.length === 0) {
    reworkList.append(createGaHistoryItem(null, "No rework requests yet."));
  } else {
    for (const request of reworkRequests) {
      reworkList.append(createGaHistoryItem(request, "No rework requests yet."));
    }
  }
  reworkSection.append(reworkTitle, reworkList);

  grid.append(evidenceSection, feedbackSection, reworkSection);
  details.append(grid);
  return details;
}

function renderGaReviewList() {
  renderGaReviewSummary();
  elements.gaReviewList.replaceChildren();

  const candidates = state.gaReviewModel.candidates || [];
  if (candidates.length === 0) {
    const empty = document.createElement("div");
    empty.className = "ga-review-empty";
    empty.textContent = "No GA random pet candidates found yet.";
    elements.gaReviewList.append(empty);
    return;
  }

  for (const candidate of candidates) {
    const node = document.createElement("article");
    node.className = "ga-review-card";
    node.dataset.runId = candidate.runId;

    const preview = document.createElement("img");
    preview.className = "ga-review-preview";
    preview.alt = `${candidate.displayName} preview`;
    preview.loading = "lazy";
    preview.src = candidate.previewUrl || "";

    const body = document.createElement("div");
    body.className = "ga-review-body";

    const heading = document.createElement("div");
    heading.className = "ga-review-card-heading";

    const titleBlock = document.createElement("div");
    const title = document.createElement("h3");
    title.textContent = candidate.displayName || candidate.runId;
    const meta = document.createElement("p");
    meta.textContent = [
      candidate.status,
      candidate.packageMode,
      candidate.backgroundMode,
      `${candidate.motionSheets?.length || 0} motions`
    ]
      .filter(Boolean)
      .join(" / ");
    titleBlock.append(title, meta);

    const packageLink = document.createElement(candidate.packageUrl ? "a" : "span");
    packageLink.className = "ga-review-package-link";
    packageLink.textContent = candidate.packageUrl ? "Package" : "No package";
    if (candidate.packageUrl) {
      packageLink.href = candidate.packageUrl;
      packageLink.download = "";
    }
    heading.append(titleBlock, packageLink);

    const summary = document.createElement("p");
    summary.className = "ga-review-summary";
    summary.textContent = candidate.summary || candidate.runId;

    const feedback = document.createElement("p");
    feedback.className = "ga-review-feedback-summary";
    const latest = candidate.feedback?.latest;
    feedback.textContent = latest
      ? `Latest: ${latest.decision} / ${latest.tags?.join(", ") || "no tags"}`
      : "No human notes yet.";

    const motionStrip = document.createElement("div");
    motionStrip.className = "ga-motion-strip";
    for (const motion of candidate.motionSheets || []) {
      const motionCell = document.createElement("div");
      motionCell.className = "ga-motion-cell";

      const motionItem = document.createElement("button");
      motionItem.className = "ga-motion-item";
      motionItem.type = "button";
      motionItem.dataset.actionId = motion.actionId;
      motionItem.title = motion.trigger || motion.actionId;
      if (motion.imageUrl) {
        const image = document.createElement("img");
        image.alt = motion.actionId;
        image.loading = "lazy";
        image.src = motion.imageUrl;
        motionItem.append(image);
      }
      const label = document.createElement("span");
      label.textContent = motion.actionId;
      motionItem.append(label);
      motionItem.addEventListener("click", () => {
        const actionInput = form.querySelector('[name="actionId"]');
        actionInput.value = motion.actionId;
        for (const item of motionStrip.querySelectorAll(".ga-motion-item.is-selected")) {
          item.classList.remove("is-selected");
        }
        motionItem.classList.add("is-selected");
      });
      motionCell.append(motionItem);
      if (motion.imageUrl) {
        const originalLink = document.createElement("a");
        originalLink.className = "ga-motion-original-link";
        originalLink.href = motion.imageUrl;
        originalLink.target = "_blank";
        originalLink.rel = "noreferrer";
        originalLink.textContent = "Open original";
        motionCell.append(originalLink);
      }
      motionStrip.append(motionCell);
    }

    const form = document.createElement("form");
    form.className = "ga-feedback-form";
    form.dataset.runId = candidate.runId;

    const decision = document.createElement("select");
    decision.name = "decision";
    for (const [value, label] of [
      ["hold", "Hold"],
      ["accept-candidate", "Looks good"],
      ["rework", "Rework"],
      ["regenerate", "Regenerate pet"],
      ["reject", "Reject"]
    ]) {
      const option = document.createElement("option");
      option.value = value;
      option.textContent = label;
      decision.append(option);
    }

    const severity = document.createElement("select");
    severity.name = "severity";
    for (const value of ["low", "medium", "high", "blocker"]) {
      const option = document.createElement("option");
      option.value = value;
      option.textContent = value;
      severity.append(option);
    }
    severity.value = "medium";

    const actionId = document.createElement("input");
    actionId.name = "actionId";
    actionId.placeholder = "action id";
    actionId.autocomplete = "off";

    const tagGroup = document.createElement("fieldset");
    tagGroup.className = "ga-issue-tags";
    const tagLegend = document.createElement("legend");
    tagLegend.textContent = "Issue tags";
    tagGroup.append(tagLegend);
    for (const [value, labelText] of GA_ISSUE_TAGS) {
      const label = document.createElement("label");
      const input = document.createElement("input");
      input.name = "tags";
      input.type = "checkbox";
      input.value = value;
      const text = document.createElement("span");
      text.textContent = labelText;
      label.append(input, text);
      tagGroup.append(label);
    }

    const customTags = document.createElement("input");
    customTags.name = "customTags";
    customTags.placeholder = "extra tags";
    customTags.autocomplete = "off";

    const notes = document.createElement("textarea");
    notes.name = "notes";
    notes.placeholder = "What is wrong, what should GA preserve, and what should change?";
    notes.rows = 3;

    const promptPatch = document.createElement("textarea");
    promptPatch.name = "promptPatch";
    promptPatch.placeholder = "Optional prompt patch for rework/regeneration";
    promptPatch.rows = 2;

    const submit = document.createElement("button");
    submit.className = "action-button primary";
    submit.type = "submit";
    submit.textContent = "Save note";

    form.append(decision, severity, actionId, customTags, tagGroup, notes, promptPatch, submit);
    form.addEventListener("submit", submitGaFeedback);

    body.append(
      heading,
      summary,
      feedback,
      motionStrip,
      createGaCandidateDetails(candidate),
      form
    );
    node.append(preview, body);
    elements.gaReviewList.append(node);
  }
}

function renderGaReviewSummary() {
  const summary = state.gaReviewModel.summary || {};
  const rework = summary.rework || {};
  const topTags = Array.isArray(summary.topTags) ? summary.topTags : [];
  const metrics = [
    ["Candidates", summary.totalCandidates ?? state.gaReviewModel.count ?? 0],
    ["Shown", summary.shownCandidates ?? state.gaReviewModel.count ?? 0],
    ["Notes", summary.learningNoteCount ?? 0],
    ["Feedback", summary.feedbackCount ?? 0],
    ["Queued", rework.queued ?? 0],
    ["Running", rework.running ?? 0],
    ["Done", rework.completed ?? 0],
    ["Failed", rework.failed ?? 0]
  ];

  elements.gaReviewSummaryMetrics.replaceChildren();
  for (const [label, value] of metrics) {
    const item = document.createElement("div");
    item.className = "ga-review-metric";
    const labelNode = document.createElement("span");
    labelNode.textContent = label;
    const valueNode = document.createElement("strong");
    valueNode.textContent = value;
    item.append(labelNode, valueNode);
    elements.gaReviewSummaryMetrics.append(item);
  }

  const tagSummary = document.createElement("div");
  tagSummary.className = "ga-review-top-tags";
  tagSummary.textContent = topTags.length > 0
    ? `Top issues: ${topTags.map((tag) => `${tag.label} ${tag.count}`).join(" / ")}`
    : "Top issues: none yet";
  elements.gaReviewSummaryMetrics.append(tagSummary);
}

function renderList() {
  elements.list.replaceChildren();

  const rows = state.model.rows.filter(
    (row) => state.filter === "all" || row.status === state.filter
  );

  if (rows.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty-state";
    empty.textContent = "No submissions match this queue filter.";
    elements.list.append(empty);
    return;
  }

  for (const row of rows) {
    const node = elements.template.content.firstElementChild.cloneNode(true);
    node.dataset.submissionId = row.submissionId;
    node.querySelector("h3").textContent = row.petId;
    node.querySelector(".item-meta").textContent =
      `${row.submissionId} by ${row.userId}`;
    const feedPublication = node.querySelector(".feed-publication");
    feedPublication.textContent = row.feedPublicationLabel;
    feedPublication.dataset.status = row.feedPublicationStatus;
    const importEvidence = formatImportEvidenceDetails(row);
    const importEvidenceBlock = node.querySelector(".import-evidence");
    const importEvidenceLabel = node.querySelector(".import-evidence-label");
    const importPreviewPath = node.querySelector(".import-preview-path");
    importEvidenceLabel.textContent = importEvidence.label;
    importPreviewPath.textContent = importEvidence.previewPath;
    importEvidenceBlock.dataset.hasPreviewPath = String(importEvidence.hasPreviewPath);

    const pill = node.querySelector(".status-pill");
    pill.textContent = row.status;
    pill.dataset.status = row.status;

    node.querySelector(".score-value").textContent = row.totalScore;
    node.querySelector(".recommended-value").textContent = row.recommendedRewardLabel;
    node.querySelector(".outstanding-value").textContent = row.outstandingRewardLabel;
    node.querySelector(".risk-value").textContent = row.riskLabel;
    node.querySelector(".recommendation-reason").textContent =
      row.recommendationReason || "No recommendation reason.";

    const riskList = node.querySelector(".risk-list");
    const riskItems = Array.isArray(row.riskItems) ? row.riskItems : [];
    if (riskItems.length === 0) {
      const item = document.createElement("li");
      item.textContent = "No risk evidence.";
      riskList.append(item);
    } else {
      for (const risk of riskItems) {
        const item = document.createElement("li");
        item.textContent = risk;
        riskList.append(item);
      }
    }

    const breakdown = node.querySelector(".breakdown-list");
    for (const [field, value] of Object.entries(row.breakdown)) {
      const dt = document.createElement("dt");
      dt.textContent = labelForField(field);
      const dd = document.createElement("dd");
      dd.textContent = value;
      breakdown.append(dt, dd);
    }

    const history = node.querySelector(".history-list");
    if (row.reviews.length === 0) {
      const item = document.createElement("li");
      item.textContent = "No reviews yet.";
      history.append(item);
    } else {
      for (const review of row.reviews) {
        const item = document.createElement("li");
        item.textContent = `${review.status} by ${review.reviewer}`;
        history.append(item);
      }
    }

    const ledger = node.querySelector(".ledger-list");
    if (row.rewardLedgerEntries.length === 0) {
      const item = document.createElement("li");
      item.textContent = "No reward ledger entries.";
      ledger.append(item);
    } else {
      for (const entry of row.rewardLedgerEntries) {
        const item = document.createElement("li");
        item.textContent = `${formatReward(entry.amount)} ${entry.sourceType}`;
        ledger.append(item);
      }
    }

    const actionBar = node.querySelector(".action-bar");
    for (const action of row.actions) {
      const button = document.createElement("button");
      button.className = `action-button ${action === "approve" ? "primary" : ""} ${
        action === "reject" || action === "revoke" ? "danger" : ""
      }`;
      button.type = "button";
      button.textContent = labelForAction(action);
      button.addEventListener("click", () => {
        reviewSubmission(row.submissionId, reviewStatusForAction(action)).catch(
          (error) => {
            elements.statusLine.textContent =
              error instanceof Error ? error.message : "Unable to post review";
          }
        );
      });
      actionBar.append(button);
    }

    elements.list.append(node);
  }
}

function render() {
  renderGenerationJob();
  renderGaReviewList();
  renderSummary();
  renderApprovedPetList();
  renderDraftList();
  renderList();
}

async function loadQueue() {
  elements.statusLine.textContent = "Loading review queue...";
  const queue = await requestJson("/v1/admin/review-queue");
  state.model = createReviewDashboardModel(queue);
  elements.statusLine.textContent = `Loaded ${state.model.summary.total} submissions`;
  render();
}

async function loadDashboard() {
  elements.statusLine.textContent = "Loading review queue...";
  const [health, drafts, queue, approvedPets, gaReview] = await Promise.all([
    requestJson("/health"),
    requestJson("/v1/import-drafts"),
    requestJson("/v1/admin/review-queue"),
    requestJson("/v1/pets/approved"),
    loadGaReviewCandidates({ silent: true })
  ]);
  state.healthStatus = formatHealthStatus(health);
  state.draftModel = createImportDraftListModel(drafts);
  state.model = createReviewDashboardModel(queue);
  state.approvedPetModel = createApprovedPetRegistryModel(approvedPets);
  state.gaReviewModel = gaReview;
  elements.statusLine.textContent =
    `Loaded ${state.model.summary.total} submissions / ${state.healthStatus}`;
  render();
}

async function loadGaReviewCandidates({ silent = false } = {}) {
  if (!silent) {
    elements.gaReviewRefreshButton.disabled = true;
    elements.gaReviewStatus.textContent = "Loading GA random pet candidates...";
  }

  try {
    const model = await requestLocalJson("/ga-review/candidates?limit=30");
    state.gaReviewModel = model;
    const rework = model.summary?.rework || {};
    elements.gaReviewStatus.textContent =
      `${model.count} shown / ${model.summary?.totalCandidates ?? model.count} total / ${rework.queued ?? 0} queued reworks`;
    renderGaReviewList();
    return model;
  } catch (error) {
    elements.gaReviewStatus.textContent =
      error instanceof Error ? error.message : "Unable to load GA candidates";
    return state.gaReviewModel;
  } finally {
    elements.gaReviewRefreshButton.disabled = false;
  }
}

async function loadGenerationJob(appJobId) {
  const normalizedJobId = String(appJobId ?? "").trim();
  if (!normalizedJobId) {
    return;
  }

  elements.generationLoadButton.disabled = true;
  elements.generationStatus.textContent = `Loading ${normalizedJobId}...`;

  try {
    const job = await requestJson(`/pet-generation-jobs/${pathSegment(normalizedJobId)}`);
    state.generationJobModel = createFantasyPetJobModel(job);
    const currentSelection = state.selectedGenerationCandidateId;
    const candidates = state.generationJobModel.candidates;
    state.selectedGenerationCandidateId = candidates.some(
      (candidate) => candidate.downloadId === currentSelection
    )
      ? currentSelection
      : candidates.find((candidate) => candidate.canReview)?.downloadId ||
        candidates[0]?.downloadId ||
        "";
    elements.generationStatus.textContent =
      `${state.generationJobModel.progressStatus || "loaded"} / ${state.generationJobModel.nextAction || "wait"}`;
    renderGenerationJob();
  } finally {
    elements.generationLoadButton.disabled = false;
  }
}

async function reviewSubmission(submissionId, status) {
  elements.statusLine.textContent = `Posting ${status} review...`;
  await requestJson("/v1/admin/reviews", {
    method: "POST",
    body: JSON.stringify({
      submissionId,
      status,
      reviewer: "admin-ui"
    })
  });
  await loadDashboard();
}

async function reviewGenerationCandidate(decision) {
  const candidate = selectedGenerationCandidate();
  const appJobId = state.generationJobModel.appJobId;
  if (!candidate || !appJobId) {
    return;
  }

  elements.generationStatus.textContent = `Posting ${decision} for ${candidate.downloadId}...`;
  const response = await requestJson(
    `/pet-generation-jobs/${pathSegment(appJobId)}/review-decisions`,
    {
      method: "POST",
      body: JSON.stringify(
        createReviewDecisionPayload({
          decision,
          targetDownloadId: candidate.downloadId,
          notes: [`${decision} from gamer admin review console`]
        })
      )
    }
  );
  state.generationJobModel = createFantasyPetJobModel(response);
  state.selectedGenerationCandidateId =
    state.generationJobModel.candidates.find(
      (item) => item.downloadId === candidate.downloadId
    )?.downloadId ||
    state.generationJobModel.candidates[0]?.downloadId ||
    "";
  elements.generationStatus.textContent =
    `${state.generationJobModel.progressStatus || "updated"} / ${state.generationJobModel.nextAction || "wait"}`;
  renderGenerationJob();
}

async function submitGaFeedback(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const runId = form.dataset.runId;
  if (!runId) return;

  const submit = form.querySelector("button[type='submit']");
  submit.disabled = true;
  elements.gaReviewStatus.textContent = `Saving feedback for ${runId}...`;

  try {
    const formData = new FormData(form);
    const selectedTags = [
      ...formData.getAll("tags").map((value) => String(value)),
      ...String(formData.get("customTags") || "")
        .split(",")
        .map((value) => value.trim())
    ].filter(Boolean);
    const payload = {
      decision: String(formData.get("decision") || "hold"),
      severity: String(formData.get("severity") || "medium"),
      actionId: String(formData.get("actionId") || ""),
      tags: selectedTags,
      notes: String(formData.get("notes") || ""),
      promptPatch: String(formData.get("promptPatch") || ""),
      reworkMode:
        String(formData.get("decision") || "") === "regenerate"
          ? "regenerate"
          : String(formData.get("decision") || "") === "rework"
            ? "repair"
            : ""
    };
    const response = await requestLocalJson(
      `/ga-review/candidates/${pathSegment(runId)}/feedback`,
      {
        method: "POST",
        body: JSON.stringify(payload)
      }
    );
    const reworkLabel = response.reworkRequest
      ? ` Rework queued: ${response.reworkRequest.requestId}.`
      : "";
    elements.gaReviewStatus.textContent = `Saved ${response.feedback.decision} for ${runId}.${reworkLabel}`;
    form.reset();
    await loadGaReviewCandidates({ silent: true });
  } catch (error) {
    elements.gaReviewStatus.textContent =
      error instanceof Error ? error.message : "Unable to save GA feedback";
  } finally {
    submit.disabled = false;
  }
}

async function submitImportDraft(draftId) {
  elements.importStatus.textContent = `Submitting ${draftId}...`;
  const result = await requestJson("/v1/import-drafts/submit", {
    method: "POST",
    body: JSON.stringify({
      draftId
    })
  });
  elements.importStatus.textContent = `Submitted ${result.submission.id}.`;
  await loadDashboard();
}

function targetDownloadIdForGenerationPackage(packageManifest) {
  const manifestDownloadId = String(packageManifest?.sourceDownloadId ?? "").trim();
  if (manifestDownloadId) {
    return manifestDownloadId;
  }

  const selected = selectedGenerationCandidate();
  if (selected?.downloadId) {
    return selected.downloadId;
  }

  const accepted = state.generationJobModel.candidates.find(
    (candidate) =>
      candidate.status === "human-accepted" || candidate.reviewDecision === "accept"
  );

  return accepted?.downloadId ?? "";
}

async function importGenerationPackage() {
  const model = state.generationJobModel;
  if (!model.appJobId || !model.downloadReady || !model.packageLink) {
    return;
  }

  elements.generationImportPackageButton.disabled = true;
  elements.generationImportStatus.textContent = `Downloading package for ${model.appJobId}...`;

  try {
    const packageResponse = await fetch(proxiedUrl(model.packageLink));
    if (!packageResponse.ok) {
      throw new Error(`Package download failed: ${packageResponse.status}`);
    }

    const packageBuffer = await packageResponse.arrayBuffer();
    const manifestText = await readZipTextEntry(
      packageBuffer,
      "package-manifest.json"
    );
    const packageManifest = JSON.parse(manifestText);
    const targetDownloadId = targetDownloadIdForGenerationPackage(packageManifest);
    const ownershipClaimId =
      elements.generationImportClaimInput.value.trim() || `claim-${model.appJobId}`;

    elements.generationImportStatus.textContent = "Creating import draft from package...";
    const draft = await requestJson("/v1/import-drafts/from-fantasy-pet-package", {
      method: "POST",
      body: JSON.stringify(
        createFantasyPetPackageImportPayload({
          packageManifest,
          packageFileName: safeZipFileName(model.appJobId),
          packageByteCount: packageBuffer.byteLength,
          targetDownloadId,
          ownershipClaimId
        })
      )
    });

    let message = formatImportDraftStatus(draft);
    if (draft.status === "ready") {
      const result = await requestJson("/v1/import-drafts/submit", {
        method: "POST",
        body: JSON.stringify({
          draftId: draft.id
        })
      });
      message = `${message} Submitted ${result.submission.id}.`;
    }

    elements.generationImportStatus.textContent = message;
    await loadDashboard();
  } finally {
    elements.generationImportPackageButton.disabled = !model.downloadReady;
  }
}

function focusSubmission(submissionId) {
  setActiveFilter("all");
  renderList();

  const target = [...elements.list.querySelectorAll(".queue-item")].find(
    (node) => node.dataset.submissionId === submissionId
  );

  if (!target) {
    elements.statusLine.textContent = `Submission ${submissionId} is not in the loaded queue.`;
    return;
  }

  for (const node of elements.list.querySelectorAll(".queue-item.is-focused")) {
    node.classList.remove("is-focused");
  }

  target.classList.add("is-focused");
  target.scrollIntoView({ behavior: "smooth", block: "center" });
  elements.statusLine.textContent = `Focused ${submissionId}.`;
}

async function revokeApprovedPet(submissionId) {
  elements.statusLine.textContent = `Revoking ${submissionId}...`;
  await requestJson("/v1/admin/reviews", {
    method: "POST",
    body: JSON.stringify({
      submissionId,
      status: "revoked",
      reviewer: "admin-ui"
    })
  });
  await loadDashboard();
}

async function importFantasyPetState(event) {
  event.preventDefault();
  elements.importButton.disabled = true;
  elements.importStatus.textContent = "Creating import draft...";

  try {
    const draft = await requestJson("/v1/import-drafts/from-fantasy-pet-rule", {
      method: "POST",
      body: JSON.stringify(
        createFantasyPetImportPayload({
          statePath: elements.statePathInput.value,
          ownershipClaimId: elements.ownershipClaimInput.value
        })
      )
    });

    let message = formatImportDraftStatus(draft);

    if (draft.status === "ready") {
      const result = await requestJson("/v1/import-drafts/submit", {
        method: "POST",
        body: JSON.stringify({
          draftId: draft.id
        })
      });
      message = `${message} Submitted ${result.submission.id}.`;
    }

    elements.importStatus.textContent = message;
    await loadDashboard();
  } finally {
    elements.importButton.disabled = false;
  }
}

elements.refreshButton.addEventListener("click", () => {
  loadDashboard().catch(showError);
});

elements.gaReviewRefreshButton.addEventListener("click", () => {
  loadGaReviewCandidates().catch(showError);
});

elements.importForm.addEventListener("submit", (event) => {
  importFantasyPetState(event).catch((error) => {
    elements.importStatus.textContent =
      error instanceof Error ? error.message : "Unable to create import draft";
  });
});

elements.generationJobForm.addEventListener("submit", (event) => {
  event.preventDefault();
  loadGenerationJob(elements.generationJobIdInput.value).catch((error) => {
    elements.generationStatus.textContent =
      error instanceof Error ? error.message : "Unable to load generation job";
  });
});

elements.generationAcceptButton.addEventListener("click", () => {
  reviewGenerationCandidate("accept").catch((error) => {
    elements.generationStatus.textContent =
      error instanceof Error ? error.message : "Unable to accept candidate";
  });
});

elements.generationReviseButton.addEventListener("click", () => {
  reviewGenerationCandidate("revise").catch((error) => {
    elements.generationStatus.textContent =
      error instanceof Error ? error.message : "Unable to revise candidate";
  });
});

elements.generationRejectButton.addEventListener("click", () => {
  reviewGenerationCandidate("reject").catch((error) => {
    elements.generationStatus.textContent =
      error instanceof Error ? error.message : "Unable to reject candidate";
  });
});

elements.generationImportPackageButton.addEventListener("click", () => {
  importGenerationPackage().catch((error) => {
    elements.generationImportStatus.textContent =
      error instanceof Error ? error.message : "Unable to import package";
    renderGenerationJob();
  });
});

for (const button of elements.filters) {
  button.addEventListener("click", () => {
    setActiveFilter(button.dataset.filter);
    renderList();
  });
}

function showError(error) {
  elements.statusLine.textContent = "Unable to load review queue";
  elements.list.replaceChildren();
  const node = document.createElement("div");
  node.className = "error-state";
  node.textContent = error instanceof Error ? error.message : "Unknown error";
  elements.list.append(node);
}

loadDashboard().catch(showError);
