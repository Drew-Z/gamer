# Community API

The community API currently runs on local in-memory state and exposes stable JSON
contracts for the Android community shell, admin review console, and pet package
integration services.

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
