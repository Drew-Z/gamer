# Community API

The community API currently runs on local in-memory state and exposes stable JSON
contracts for the Android community shell, admin review console, and pet package
integration services.

## Proxied fantasy-pet public app endpoints

When `FANTASY_PET_API_BASE_URL` is configured, the community API can also act as
the Android app's single backend entry point for the `fantasy-pet-rule` public
app lifecycle. These routes are proxied to the upstream public app API:

- `POST /pet-generation-jobs`
- `GET /pet-generation-jobs/{appJobId}`
- `GET /pet-generation-jobs/{appJobId}/artifacts`
- `GET /pet-generation-jobs/{appJobId}/artifacts/{downloadId}`
- `POST /pet-generation-jobs/{appJobId}/review-decisions`
- `GET /pet-generation-jobs/{appJobId}/package`
- `GET /worker-readiness`
- `GET /app-api-contract`

`GET /pet-generation-jobs/{appJobId}/package` streams the upstream zip response
without JSON wrapping. The proxy allowlist does not include `/admin/*`,
`server-worker-cycle`, worker command routes, Codex routes, GenericAgent routes,
or direct image-generation controls. If the upstream is not configured, public
generation proxy calls return:

```json
{
  "error": "fantasy_pet_api_unconfigured"
}
```

## GET /v1/community-home

Returns the public home summary used by the Android community shell. This is a
read-only aggregation endpoint so the app can load the first community screen
without separately requesting feed, wallet, approved pet shelf, daily check-in
state, and submission counters.

Optional query parameters:

- `date`: `YYYY-MM-DD`, used to resolve the daily check-in state. If omitted,
  the service uses the current server date.

The response schema is `gamer.community-home.v1`. `dailyCheckIn` tells the app
whether the current day has already been claimed. `submissionsSummary.pendingCount`
drives the compact review shortcut in the community home UI.

Example response:

```json
{
  "schema": "gamer.community-home.v1",
  "userId": "user-demo-001",
  "feed": {
    "items": [],
    "nextCursor": "fixture-page-2"
  },
  "wallet": {
    "userId": "user-demo-001",
    "balance": 100,
    "currencyCode": "petcoin",
    "ledgerEntries": []
  },
  "approvedPets": {
    "items": []
  },
  "dailyCheckIn": {
    "date": "2026-06-09",
    "claimed": true,
    "rewardAmount": 10,
    "ledgerEntryId": "ledger-checkin-2026-06-09"
  },
  "submissionsSummary": {
    "pendingCount": 1,
    "approvedCount": 1,
    "heldCount": 0,
    "rejectedCount": 0,
    "revokedCount": 0,
    "latest": {
      "id": "submission-local-002",
      "petId": "pet-home-pending-001",
      "userId": "user-demo-001",
      "status": "pending",
      "scoreReportId": "score-home-pending-001",
      "ownershipClaimId": "claim-home-pending-001",
      "importDraftId": "",
      "submittedAt": "2026-06-09T00:00:00.000Z"
    }
  }
}
```

## GET /v1/feed

Returns community feed posts. Approved pet imports include audit metadata that
lets clients show source, reward, preview, motion sheet, and export package
evidence.

`feed.items[].metadata.exportArtifactPath` is the approved pet package archive
path carried from the import summary. Android renders it as `Package <path>` in
feed audit labels.

Example response:

```json
{
  "items": [
    {
      "id": "post-submission-local-001",
      "authorId": "user-demo-001",
      "petId": "pet-stardust-001",
      "title": "Approved pet import: pet-stardust-001",
      "body": "validated pet package bundle",
      "reactionCount": 0,
      "createdAt": "2026-06-07T00:00:00.000Z",
      "metadata": {
        "sourceType": "approved-import",
        "importDraftId": "import-draft-local-001",
        "submissionId": "submission-local-001",
        "scoreReportId": "score-import-draft-local-001",
        "rewardAmount": 80,
        "importSourceKind": "fantasy-pet-rule",
        "importPreviewPath": "previews/overall-showcase.png",
        "exportArtifactPath": "exports/stardust-package.zip",
        "motionSheetCount": 2
      }
    }
  ],
  "nextCursor": "fixture-page-2"
}
```

## GET /v1/pets/approved

Returns the approved pet registry used by Android showcase views and admin
review trace panels.

`approvedPets.items[].assets.exportArtifactPath` is the approved package archive
path for loading, auditing, or later package download flows.

Example response:

```json
{
  "items": [
    {
      "petId": "pet-stardust-001",
      "displayName": "Stardust Dragon",
      "ownerUserId": "user-demo-001",
      "source": {
        "kind": "fantasy-pet-rule",
        "runId": "stardust-chinese-dragon-codex-02",
        "statePath": "D:/workspace4Codex/fantasy-pet-rule/runs/stardust-chinese-dragon-codex-02/state.json"
      },
      "assets": {
        "previewPath": "previews/overall-showcase.png",
        "exportArtifactPath": "exports/stardust-package.zip",
        "motionSheetCount": 2
      },
      "submissionId": "submission-local-001",
      "importDraftId": "import-draft-local-001",
      "scoreReportId": "score-import-draft-local-001",
      "totalScore": 86,
      "approvedAt": "2026-06-07T00:00:00.000Z"
    }
  ]
}
```

## GET /v1/pets/approved/:petId/package

Returns the package descriptor for one approved pet. This endpoint does not
stream or copy the package archive yet; it exposes the approved export artifact
path and trace IDs that future download or import flows can use.

`approvedPetPackage.package.exportArtifactPath` is the package archive path
registered during review approval.

Example response:

```json
{
  "petId": "pet-stardust-001",
  "displayName": "Stardust Dragon",
  "ownerUserId": "user-demo-001",
  "package": {
    "exportArtifactPath": "exports/stardust-package.zip",
    "status": "available"
  },
  "assets": {
    "previewPath": "previews/overall-showcase.png",
    "motionSheetCount": 2
  },
  "source": {
    "kind": "fantasy-pet-rule",
    "runId": "stardust-chinese-dragon-codex-02",
    "statePath": "D:/workspace4Codex/fantasy-pet-rule/runs/stardust-chinese-dragon-codex-02/state.json"
  },
  "submissionId": "submission-local-001",
  "importDraftId": "import-draft-local-001",
  "scoreReportId": "score-import-draft-local-001"
}
```

Unknown approved pets return:

```json
{
  "error": "approved_pet_package_not_found",
  "petId": "pet-missing-001"
}
```

## GET /v1/submissions/:submissionId

Returns one public submission status for Android's generated-pet handoff and
community review tracking. This endpoint does not expose admin review queue
details; it only returns the public submission record.

Example response:

```json
{
  "id": "submission-local-001",
  "petId": "public-lifecycle-smoke",
  "userId": "user-demo-001",
  "status": "pending",
  "scoreReportId": "score-import-draft-local-001",
  "ownershipClaimId": "claim-public-lifecycle-smoke",
  "importDraftId": "import-draft-local-001",
  "submittedAt": "2026-06-08T00:00:00.000Z"
}
```

Unknown submissions return:

```json
{
  "error": "submission_not_found",
  "submissionId": "submission-missing-001"
}
```

## POST /v1/import-drafts/from-fantasy-pet-package

Creates a community import draft from the public package manifest inside a
downloaded `fantasy-pet-rule` package. This endpoint is for the Android
generation handoff after human review and package download.

The request uses the app-safe `fantasy-pet.package-manifest.v1` data and does
not accept server run paths, local filesystem paths, or `file://` references.
Every `packageManifest.files[].path` must be a safe package-relative path from
inside the ZIP.

To verify the route with a real package produced by `fantasy-pet-rule`, run:

```powershell
tools\smoke-fantasy-pet-community-import.cmd
```

The smoke uses only public app and community endpoints: it runs the public
generation lifecycle, reads the downloaded `pet.zip`, creates an import draft
through this route, then submits it through `/v1/import-drafts/submit`.

Example request:

```json
{
  "packageManifest": {
    "schema": "fantasy-pet.package-manifest.v1",
    "runId": "run-public-lifecycle-smoke",
    "appJobId": "public-lifecycle-smoke",
    "acceptedBy": "human-review",
    "sourceDownloadId": "artifact-1",
    "sourceTaskId": "codex-worker-task",
    "files": [
      {
        "kind": "candidate",
        "path": "artifacts/candidates/final-preview.png"
      }
    ]
  },
  "packageFileName": "pet-public-lifecycle-smoke.zip",
  "packageByteCount": 664,
  "targetDownloadId": "artifact-1",
  "ownershipClaimId": "claim-public-lifecycle-smoke"
}
```

Example response:

```json
{
  "id": "import-draft-local-001",
  "userId": "user-demo-001",
  "status": "ready",
  "petId": "public-lifecycle-smoke",
  "ownershipClaimId": "claim-public-lifecycle-smoke",
  "scoreReportId": "score-import-draft-local-001",
  "readiness": {
    "status": "community-ready",
    "reason": "human-reviewed fantasy pet package downloaded"
  },
  "importSummary": {
    "source": {
      "petId": "public-lifecycle-smoke",
      "displayName": "Generated pet public-lifecycle-smoke",
      "schema": "fantasy-pet.package-manifest.v1",
      "kind": "fantasy-pet-rule",
      "runId": "run-public-lifecycle-smoke",
      "appJobId": "public-lifecycle-smoke",
      "statePath": "",
      "baseIdentityStatus": "accepted"
    },
    "review": {
      "blockers": [],
      "previewDecision": "keep",
      "exportStatus": "ready",
      "acceptedBy": "human-review",
      "targetDownloadId": "artifact-1"
    },
    "assets": {
      "previewPath": "artifact-1",
      "exportArtifactPath": "pet-public-lifecycle-smoke.zip",
      "packageByteCount": 664,
      "motionSheets": [
        "artifacts/candidates/final-preview.png"
      ]
    }
  }
}
```

Invalid requests return:

```json
{
  "error": "invalid_fantasy_pet_package",
  "validation": {
    "ok": false,
    "errors": [
      "files[0].path must be a safe package-relative path"
    ]
  }
}
```
