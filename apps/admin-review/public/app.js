import {
  createFantasyPetImportPayload,
  createImportDraftListModel,
  createReviewDashboardModel,
  formatImportDraftStatus,
  formatReward
} from "/src/reviewQueuePresenter.js";

const state = {
  filter: "all",
  draftModel: createImportDraftListModel({ drafts: [] }),
  model: createReviewDashboardModel({ items: [] })
};

const elements = {
  list: document.querySelector("#queue-list"),
  statusLine: document.querySelector("#status-line"),
  refreshButton: document.querySelector("#refresh-button"),
  importForm: document.querySelector("#import-form"),
  importButton: document.querySelector("#import-button"),
  importStatus: document.querySelector("#import-status"),
  statePathInput: document.querySelector("#state-path-input"),
  ownershipClaimInput: document.querySelector("#ownership-claim-input"),
  draftSummary: document.querySelector("#draft-summary"),
  draftList: document.querySelector("#draft-list"),
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

    heading.append(title, status);
    node.append(heading, reason, meta);
    elements.draftList.append(node);
  }
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
      button.addEventListener("click", () => reviewSubmission(row.submissionId, action));
      actionBar.append(button);
    }

    elements.list.append(node);
  }
}

function render() {
  renderSummary();
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
  const [drafts, queue] = await Promise.all([
    requestJson("/v1/import-drafts"),
    requestJson("/v1/admin/review-queue")
  ]);
  state.draftModel = createImportDraftListModel(drafts);
  state.model = createReviewDashboardModel(queue);
  elements.statusLine.textContent = `Loaded ${state.model.summary.total} submissions`;
  render();
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
  await loadQueue();
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

elements.importForm.addEventListener("submit", (event) => {
  importFantasyPetState(event).catch((error) => {
    elements.importStatus.textContent =
      error instanceof Error ? error.message : "Unable to create import draft";
  });
});

for (const button of elements.filters) {
  button.addEventListener("click", () => {
    state.filter = button.dataset.filter;
    for (const filterButton of elements.filters) {
      filterButton.classList.toggle("is-active", filterButton === button);
    }
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
