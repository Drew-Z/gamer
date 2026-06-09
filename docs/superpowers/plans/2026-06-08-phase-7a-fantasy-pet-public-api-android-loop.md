# Phase 7a Fantasy Pet Public API Android Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect the Android app to the `fantasy-pet-rule` public app API for the minimum text prompt -> job -> polling -> candidate review -> final `pet.zip` download loop.

**Architecture:** Add a dedicated `generation` API layer for `fantasy-pet-rule` public endpoints, separate from the community API. Keep all public-safety rules in a testable `FantasyPetGenerationService`, and wire a compact Compose panel into the existing community screen. Do not call admin endpoints, worker endpoints, Codex, GenericAgent, shell commands, or image generation from app code.

**Tech Stack:** Kotlin, kotlinx.serialization, HttpURLConnection, Jetpack Compose, Android JVM unit tests.

---

### Task 1: Contract and Safety RED Tests

**Files:**
- Create: `apps/android-community/app/src/test/java/com/gamer/community/generation/FantasyPetGenerationServiceTest.kt`
- Create: `apps/android-community/app/src/test/java/com/gamer/community/generation/HttpFantasyPetGenerationClientTest.kt`

- [x] **Step 1: Test create-job schema and reference validation**

`FantasyPetGenerationServiceTest` should verify:

```kotlin
val service = FantasyPetGenerationService(fakeClient)
val result = service.createJob(
    description = "tiny stardust dragon",
    appJobId = "job-123",
    bodyShape = "wide-tail",
    referencesText = "https://example.com/ref.png"
)

assertTrue(result is ApiCallResult.Success)
assertEquals("fantasy-pet.app-job-create-request.v1", fakeClient.createdRequest?.schema)
assertEquals("tiny stardust dragon", fakeClient.createdRequest?.description)
assertEquals("job-123", fakeClient.createdRequest?.appJobId)
assertEquals("wide-tail", fakeClient.createdRequest?.bodyShape)
assertEquals(listOf("https://example.com/ref.png"), fakeClient.createdRequest?.references)
```

Also test that `file://C:/secret/ref.png` returns `ApiCallResult.Failure("reference_urls_must_be_http_or_https")`.

- [x] **Step 2: Test app-safe artifact display**

Verify candidate gallery items use only `downloadUrl` or `{appJobId}/artifacts/{downloadId}`, and do not expose internal-looking labels or paths:

```kotlin
val job = PetGenerationJobResponseDto(
    appJobId = "job-123",
    progressStatus = "waiting-for-review",
    nextAction = "human-review",
    artifacts = listOf(
        PetGenerationArtifactDto(
            kind = "candidate",
            downloadId = "artifact-1",
            downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1",
            label = "C:/secret/runs/job-123/output.png"
        ),
        PetGenerationArtifactDto(
            kind = "qa",
            downloadId = "artifact-2",
            downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-2"
        )
    )
)

val items = service.candidateGalleryItems(job)

assertEquals(1, items.size)
assertEquals("artifact-1", items[0].targetDownloadId)
assertEquals("http://127.0.0.1:8765/pet-generation-jobs/job-123/artifacts/artifact-1", items[0].previewUrl)
assertEquals("Candidate artifact-1", items[0].title)
assertFalse(items[0].title.contains("C:/secret"))
```

- [x] **Step 3: Test human review decision safety**

Verify `submitReviewDecision` sends `schema = fantasy-pet.review-decision.v1`, `reviewer = human-review`, and `targetDownloadId`, and has no `targetOutput` property.

```kotlin
service.submitReviewDecision(
    appJobId = "job-123",
    targetDownloadId = "artifact-1",
    decision = "accept",
    notesText = ""
)

assertEquals("fantasy-pet.review-decision.v1", fakeClient.reviewRequest?.schema)
assertEquals("human-review", fakeClient.reviewRequest?.reviewer)
assertEquals("accept", fakeClient.reviewRequest?.decision)
assertEquals("artifact-1", fakeClient.reviewRequest?.targetDownloadId)
```

- [x] **Step 4: Test notes and package download gates**

Verify:

```kotlin
assertEquals(
    ApiCallResult.Failure("review_notes_required"),
    service.submitReviewDecision("job-123", "artifact-1", "revise", "")
)
assertEquals(
    ApiCallResult.Failure("review_notes_required"),
    service.submitReviewDecision("job-123", "artifact-1", "reject", " ")
)
assertEquals(
    ApiCallResult.Failure("package_not_ready"),
    service.downloadPackage(PetGenerationJobResponseDto(appJobId = "job-123", downloadReady = false, nextAction = "wait"))
)
```

Also verify package download succeeds when `downloadReady = true` or `nextAction = "download-package"`.

- [x] **Step 5: Test HTTP client paths and schemas**

`HttpFantasyPetGenerationClientTest` should verify:

```kotlin
val client = HttpFantasyPetGenerationClient(server.baseUrl)
client.createJob(PetGenerationJobCreateRequestDto(description = "tiny dragon"))
assertEquals("POST", recordedRequest.method)
assertEquals("/pet-generation-jobs", recordedRequest.path)
assertTrue(recordedRequest.body.contains("\"schema\":\"fantasy-pet.app-job-create-request.v1\""))
```

Also verify:

```kotlin
client.getJob("Job 123/A") -> "/pet-generation-jobs/Job%20123%2FA"
client.submitReviewDecision("Job 123/A", request) -> "/pet-generation-jobs/Job%20123%2FA/review-decisions"
client.downloadPackage("Job 123/A") -> "/pet-generation-jobs/Job%20123%2FA/package"
```

- [x] **Step 6: Run Android tests to verify RED**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Observed: FAIL because generation DTO/client/service classes did not exist yet.

### Task 2: Generation DTO, Client, and Service Implementation

**Files:**
- Create: `apps/android-community/app/src/main/java/com/gamer/community/generation/FantasyPetGenerationDtos.kt`
- Create: `apps/android-community/app/src/main/java/com/gamer/community/generation/FantasyPetGenerationClient.kt`
- Create: `apps/android-community/app/src/main/java/com/gamer/community/generation/HttpFantasyPetGenerationClient.kt`
- Create: `apps/android-community/app/src/main/java/com/gamer/community/generation/FantasyPetGenerationService.kt`
- Modify: `apps/android-community/app/build.gradle`
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/MainActivity.kt`

- [x] **Step 1: Add BuildConfig generation base URL**

Read `FANTASY_PET_API_BASE_URL` from the Gradle environment, defaulting to `http://127.0.0.1:8765`, and expose it as `BuildConfig.FANTASY_PET_API_BASE_URL`.

- [x] **Step 2: Add DTOs**

Add serializable request/response DTOs for:

- `PetGenerationJobCreateRequestDto`
- `PetGenerationJobResponseDto`
- `PetGenerationArtifactDto`
- `PetGenerationArtifactIndexResponseDto`
- `PetGenerationLinksDto`
- `ReviewDecisionRequestDto`

The create request default schema must be `fantasy-pet.app-job-create-request.v1`. The review request default schema must be `fantasy-pet.review-decision.v1`, reviewer must default to `human-review`, and review target must be `targetDownloadId`.

- [x] **Step 3: Add public client interface**

Expose only public app endpoints:

- `createJob`
- `getJob`
- `getArtifacts`
- `submitReviewDecision`
- `downloadPackage`

Do not include any `/admin/*`, worker, Codex, GenericAgent, shell, or image-generation methods.

- [x] **Step 4: Implement HTTP client**

Use `HttpURLConnection`, JSON encoding/decoding, and path-segment encoding. The package download method returns `ByteArray`.

- [x] **Step 5: Implement service safety rules**

Implement:

- create request validation for description, body shape, and HTTP/HTTPS references.
- status labels from `progressStatus` and `nextAction`.
- candidate gallery filtering: only `kind == "candidate"` and nonblank `downloadId`.
- public preview URL resolution using `downloadUrl` or `/pet-generation-jobs/{appJobId}/artifacts/{downloadId}`.
- no display of internal-looking labels.
- human review submission with `targetDownloadId`.
- `revise` / `reject` notes required.
- final package download only when `downloadReady == true` or `nextAction == "download-package"`.

- [x] **Step 6: Run Android tests to verify GREEN**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: PASS.

### Task 3: Compose UI Integration

**Files:**
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`
- Create: `apps/android-community/app/src/test/java/com/gamer/community/generation/FantasyPetGenerationUiModelTest.kt`

- [x] **Step 1: Add UI model tests**

Verify status copy and button gating:

```kotlin
assertEquals("Waiting for worker output", generationStatusLabel("waiting-for-worker-output", "wait"))
assertEquals("Ready for human review", generationStatusLabel("waiting-for-review", "human-review"))
assertTrue(canShowPackageDownload(PetGenerationJobResponseDto(appJobId = "job-123", downloadReady = true)))
assertFalse(canShowPackageDownload(PetGenerationJobResponseDto(appJobId = "job-123", downloadReady = false, nextAction = "wait")))
```

- [x] **Step 2: Add generation service injection**

`PetShellApp(repository = ..., generationService = ...)` should accept a `FantasyPetGenerationService`.

- [x] **Step 3: Add generation panel**

Add a compact full-width generation panel to the community screen with:

- description input.
- public API boundary notice that the app only creates/polls jobs and does not
  start live generation workers.
- optional app job id input.
- body shape selector with `balanced`, `wide`, `wide-tail`, `tall`.
- reference URL input accepting comma/newline-separated HTTP/HTTPS URLs.
- create job button.
- status line driven by `progressStatus` and `nextAction`.
- candidate gallery using only `downloadUrl`/artifact endpoint.
- selected candidate target download id.
- accept / revise / reject controls.
- notes input for revise/reject.
- package download button only when service says package is ready.

- [x] **Step 4: Add polling**

After job creation, poll `GET /pet-generation-jobs/{appJobId}` while the job is queued, processing, waiting for worker output, packaging, revision-requested, or candidate-rejected. Use a small coroutine delay; do not call worker/admin endpoints.

- [x] **Step 5: Run Android tests**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: PASS.

### Task 4: Full Verification and Commit

**Files:**
- Verify all files above

- [x] **Step 1: Run Node tests**

```powershell
npm.cmd test
```

- [x] **Step 2: Run Android tests**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

- [x] **Step 3: Validate Docker compose**

```powershell
docker compose config
```

- [x] **Step 4: Check whitespace and git status**

```powershell
git diff --check
git status --short
```

- [ ] **Step 5: Commit**

```powershell
git add docs/superpowers/plans/2026-06-08-phase-7a-fantasy-pet-public-api-android-loop.md apps/android-community/app
git commit -m "Connect Android app to fantasy pet generation API"
```

---

## Self-Review

- Spec coverage: Covers the requested public API base URL, job creation, polling state, candidate gallery, targetDownloadId review decisions, notes gates, and final package download gate.
- Placeholder scan: No `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: Uses public `downloadId`, `downloadUrl`, `progressStatus`, `nextAction`, `downloadReady`, and `targetDownloadId` field names from the `fantasy-pet-rule` integration docs.
