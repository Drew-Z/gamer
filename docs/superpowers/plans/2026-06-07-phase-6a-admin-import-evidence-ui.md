# Phase 6a Admin Import Evidence UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show import draft source, preview path, and motion sheet count in each admin review queue card.

**Architecture:** Keep the admin UI as a static browser app. Add a small presenter helper for import evidence view text, test it with `node:test`, then wire the existing row fields into the HTML template and DOM render function.

**Tech Stack:** Node.js 22 ESM, built-in `node:test`, static HTML/CSS/JS admin app, existing community API proxy.

---

## Files

- Modify `apps/admin-review/src/reviewQueuePresenter.test.js`: add RED tests for a display helper that formats import evidence details.
- Modify `apps/admin-review/src/reviewQueuePresenter.js`: export `formatImportEvidenceDetails(row)`.
- Modify `apps/admin-review/public/index.html`: add an import evidence section to the queue item template.
- Modify `apps/admin-review/public/app.js`: render import evidence text and path from each row.
- Modify `apps/admin-review/public/styles.css`: style the import evidence block without disrupting the existing dense admin layout.

## Task 1: Presenter Helper

- [x] **Step 1: Write failing helper test**

Add a test to `apps/admin-review/src/reviewQueuePresenter.test.js`:

```js
test("formatImportEvidenceDetails summarizes import evidence fields", () => {
  assert.deepEqual(
    formatImportEvidenceDetails({
      importEvidenceLabel: "fantasy-pet-rule / 2 motion sheets",
      importPreviewPath: "previews/overall-showcase.png"
    }),
    {
      label: "fantasy-pet-rule / 2 motion sheets",
      previewPath: "previews/overall-showcase.png",
      hasPreviewPath: true
    }
  );

  assert.deepEqual(formatImportEvidenceDetails({}), {
    label: "No import evidence",
    previewPath: "No preview path",
    hasPreviewPath: false
  });
});
```

Also import `formatImportEvidenceDetails` from `./reviewQueuePresenter.js`.

- [x] **Step 2: Run presenter test to verify RED**

Run:

```powershell
node --test apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: FAIL because `formatImportEvidenceDetails` is not exported.

- [x] **Step 3: Implement helper**

Add this export to `apps/admin-review/src/reviewQueuePresenter.js`:

```js
export function formatImportEvidenceDetails(row = {}) {
  const previewPath = row.importPreviewPath || "No preview path";

  return {
    label: row.importEvidenceLabel || "No import evidence",
    previewPath,
    hasPreviewPath: previewPath !== "No preview path"
  };
}
```

- [x] **Step 4: Run presenter test to verify GREEN**

Run:

```powershell
node --test apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: PASS.

## Task 2: Static UI Wiring

- [x] **Step 1: Add template nodes**

In `apps/admin-review/public/index.html`, add an import evidence block after `.feed-publication`:

```html
<div class="import-evidence">
  <span class="import-evidence-label"></span>
  <code class="import-preview-path"></code>
</div>
```

- [x] **Step 2: Render evidence fields**

In `apps/admin-review/public/app.js`, import `formatImportEvidenceDetails` and populate the template nodes inside `renderList()`:

```js
const importEvidence = formatImportEvidenceDetails(row);
const importEvidenceBlock = node.querySelector(".import-evidence");
const importEvidenceLabel = node.querySelector(".import-evidence-label");
const importPreviewPath = node.querySelector(".import-preview-path");
importEvidenceLabel.textContent = importEvidence.label;
importPreviewPath.textContent = importEvidence.previewPath;
importEvidenceBlock.dataset.hasPreviewPath = String(importEvidence.hasPreviewPath);
```

- [x] **Step 3: Style evidence block**

In `apps/admin-review/public/styles.css`, add compact admin styling:

```css
.import-evidence {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 8px;
  margin-top: 8px;
}

.import-evidence-label {
  border: 1px solid #b8d4d8;
  border-radius: 999px;
  background: #edf7f8;
  color: var(--accent-strong);
  font-size: 12px;
  font-weight: 700;
  padding: 4px 8px;
}

.import-preview-path {
  min-width: 0;
  max-width: 100%;
  overflow-wrap: anywhere;
  border-radius: 6px;
  background: var(--surface-soft);
  color: var(--muted);
  font-family: "Cascadia Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  line-height: 1.5;
  padding: 3px 6px;
}

.import-evidence[data-has-preview-path="false"] .import-preview-path {
  display: none;
}
```

- [x] **Step 4: Run admin presenter test**

Run:

```powershell
node --test apps/admin-review/src/reviewQueuePresenter.test.js
```

Expected: PASS.

## Task 3: Verification

- [x] **Step 1: Run Node tests**

Run:

```powershell
npm.cmd test
```

- [x] **Step 2: Run Android unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

- [x] **Step 3: Run Docker config validation**

Run:

```powershell
docker compose config
```

- [x] **Step 4: Run diff checks**

Run:

```powershell
git diff --check
git status --short
```

- [x] **Step 5: Commit**

Run:

```powershell
git add docs/superpowers/plans/2026-06-07-phase-6a-admin-import-evidence-ui.md apps/admin-review/src/reviewQueuePresenter.js apps/admin-review/src/reviewQueuePresenter.test.js apps/admin-review/public/index.html apps/admin-review/public/app.js apps/admin-review/public/styles.css
git commit -m "Show import evidence in admin review UI"
```

## Self-Review

- Spec coverage: Admin review UI now exposes the import evidence fields added in Phase 5z.
- Placeholder scan: No TBD/TODO placeholders.
- Type consistency: Uses existing row field names `importEvidenceLabel` and `importPreviewPath`.
