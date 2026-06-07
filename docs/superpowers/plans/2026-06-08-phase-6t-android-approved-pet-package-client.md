# Phase 6t Android Approved Pet Package Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the Android community API layer fetch the approved pet package descriptor exposed by `GET /v1/pets/approved/:petId/package`.

**Architecture:** Add serializable DTOs for the approved pet package descriptor and extend `CommunityApiClient` with `getApprovedPetPackage(petId)`. Implement decoding and HTTP GET routing in `HttpCommunityApiClient`, leaving repository/UI adoption for a later phase.

**Tech Stack:** Kotlin, kotlinx.serialization, Android JVM unit tests, existing local Gradle wrapper.

---

### Task 1: Android Client RED Tests

**Files:**
- Modify: `apps/android-community/app/src/test/java/com/gamer/community/api/HttpCommunityApiClientTest.kt`

- [ ] **Step 1: Add decoder test**

Add a test named `decodesApprovedPetPackageJson`:

```kotlin
@Test
fun decodesApprovedPetPackageJson() {
    val json = """
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
            "runId": "stardust-run-001",
            "statePath": "D:/workspace4Codex/fantasy-pet-rule/runs/stardust/state.json"
          },
          "submissionId": "submission-local-001",
          "importDraftId": "import-draft-local-001",
          "scoreReportId": "score-import-draft-local-001"
        }
    """.trimIndent()

    val descriptor = HttpCommunityApiClient.decodeApprovedPetPackage(json)

    assertEquals("pet-stardust-001", descriptor.petId)
    assertEquals("Stardust Dragon", descriptor.displayName)
    assertEquals("exports/stardust-package.zip", descriptor.`package`.exportArtifactPath)
    assertEquals("available", descriptor.`package`.status)
    assertEquals("stardust-run-001", descriptor.source.runId)
    assertEquals("submission-local-001", descriptor.submissionId)
}
```

- [ ] **Step 2: Add HTTP path test**

Add a test named `getApprovedPetPackageRequestsEncodedPetPackagePath`:

```kotlin
@Test
fun getApprovedPetPackageRequestsEncodedPetPackagePath() = runTest {
    val recordedRequest = AtomicReference<RecordedRequest>()
    val responseBody = """
        {
          "petId": "pet stardust/001",
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
            "kind": "fantasy-pet-rule"
          },
          "submissionId": "submission-local-001",
          "importDraftId": "import-draft-local-001",
          "scoreReportId": "score-import-draft-local-001"
        }
    """.trimIndent()

    TestServer(
        responseBody = responseBody,
        handler = { recordedRequest.set(it) }
    ).use { server ->
        val result = HttpCommunityApiClient(server.baseUrl)
            .getApprovedPetPackage("pet stardust/001")

        assertTrue(result is ApiCallResult.Success)
        assertEquals("GET", recordedRequest.get()?.method)
        assertEquals(
            "/v1/pets/approved/pet%20stardust%2F001/package",
            recordedRequest.get()?.path
        )
    }
}
```

- [ ] **Step 3: Run Android unit tests to verify RED**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: FAIL because `decodeApprovedPetPackage` and `getApprovedPetPackage` do not exist yet.

### Task 2: Android DTO and Client Implementation

**Files:**
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt`
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiClient.kt`
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/api/HttpCommunityApiClient.kt`
- Modify: `apps/android-community/app/src/test/java/com/gamer/community/api/CommunityRepositoryTest.kt`

- [ ] **Step 1: Add DTOs**

Add these DTOs to `CommunityApiDtos.kt` after `ApprovedPetAssetsDto`:

```kotlin
@Serializable
data class ApprovedPetPackageDto(
    val petId: String,
    val displayName: String,
    val ownerUserId: String,
    val `package`: ApprovedPetPackageArtifactDto = ApprovedPetPackageArtifactDto(),
    val assets: ApprovedPetPackageAssetsDto = ApprovedPetPackageAssetsDto(),
    val source: ApprovedPetPackageSourceDto = ApprovedPetPackageSourceDto(),
    val submissionId: String = "",
    val importDraftId: String = "",
    val scoreReportId: String = ""
)

@Serializable
data class ApprovedPetPackageArtifactDto(
    val exportArtifactPath: String = "",
    val status: String = ""
)

@Serializable
data class ApprovedPetPackageAssetsDto(
    val previewPath: String = "",
    val motionSheetCount: Int = 0
)

@Serializable
data class ApprovedPetPackageSourceDto(
    val kind: String = "",
    val runId: String = "",
    val statePath: String = ""
)
```

- [ ] **Step 2: Extend the interface**

Add to `CommunityApiClient`:

```kotlin
suspend fun getApprovedPetPackage(petId: String): ApiCallResult<ApprovedPetPackageDto>
```

- [ ] **Step 3: Implement HTTP method and decoder**

Add an override to `HttpCommunityApiClient`:

```kotlin
override suspend fun getApprovedPetPackage(petId: String): ApiCallResult<ApprovedPetPackageDto> =
    get("/v1/pets/approved/${petId.pathSegment()}/package", Companion::decodeApprovedPetPackage)
```

Add to the companion object:

```kotlin
fun decodeApprovedPetPackage(text: String): ApprovedPetPackageDto =
    json.decodeFromString<ApprovedPetPackageDto>(text)
```

Add a private extension near the request helpers:

```kotlin
private fun String.pathSegment(): String =
    java.net.URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
```

- [ ] **Step 4: Update test fake client**

In `CommunityRepositoryTest.kt`, implement the new interface method:

```kotlin
override suspend fun getApprovedPetPackage(petId: String): ApiCallResult<ApprovedPetPackageDto> =
    ApiCallResult.Failure("not_configured")
```

- [ ] **Step 5: Run Android unit tests to verify GREEN**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

Expected: PASS.

### Task 3: Full Verification and Commit

**Files:**
- Verify all files above

- [ ] **Step 1: Run Node tests**

```powershell
npm.cmd test
```

- [ ] **Step 2: Run Android tests**

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --console=plain
```

- [ ] **Step 3: Validate Docker compose**

```powershell
docker compose config
```

- [ ] **Step 4: Check whitespace and git status**

```powershell
git diff --check
git status --short
```

- [ ] **Step 5: Commit**

```powershell
git add docs/superpowers/plans/2026-06-08-phase-6t-android-approved-pet-package-client.md apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiClient.kt apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt apps/android-community/app/src/main/java/com/gamer/community/api/HttpCommunityApiClient.kt apps/android-community/app/src/test/java/com/gamer/community/api/HttpCommunityApiClientTest.kt apps/android-community/app/src/test/java/com/gamer/community/api/CommunityRepositoryTest.kt
git commit -m "Add Android approved pet package client"
```

---

## Self-Review

- Spec coverage: Moves the Android community shell closer to importing approved pet packages by exposing the approved package descriptor endpoint at the client layer.
- Placeholder scan: No `TBD`, `TODO`, or unspecified implementation steps remain.
- Type consistency: DTO field names match the Community API descriptor: `package.exportArtifactPath`, `assets.previewPath`, `source.runId`, and trace IDs.
