# Phase 6d Android Approved Pet Registry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Load approved imported pet assets from the community API into the Android pet-first shell.

**Architecture:** Add a small approved pet DTO/model path beside the existing feed DTO path. The repository loads approved pets during initial community load and falls back to an empty registry when the API is unavailable. The shell state carries the approved pets and the UI shows a compact registry summary in the community screen.

**Tech Stack:** Kotlin 2.2.10, kotlinx.serialization, Android/JUnit unit tests, Node.js community API for contract source.

---

## Files

- Modify `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt`: add `ApprovedPet` model and `approvedPets` state field.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellController.kt`: preserve and apply approved pet registry state.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt`: add approved pets response DTOs.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt`: map approved pet DTOs to shell models.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiClient.kt`: add `getApprovedPets()`.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/api/HttpCommunityApiClient.kt`: implement `GET /v1/pets/approved` and JSON decode helper.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityRepository.kt`: include approved pets in `InitialCommunityResult`.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`: render compact approved pet registry summary.
- Modify Android tests for mapper, repository, HTTP client, controller/UI helpers.

## Task 1: DTO And Mapper

- [x] **Step 1: Write failing mapper test**

Add to `CommunityApiMappersTest.kt`:

```kotlin
@Test
fun mapsApprovedPetRegistryToShellModels() {
    val response = ApprovedPetsResponseDto(
        items = listOf(
            ApprovedPetDto(
                petId = "pet-stardust-001",
                displayName = "Stardust Dragon",
                ownerUserId = "user-demo-001",
                source = ApprovedPetSourceDto(kind = "fantasy-pet-rule"),
                assets = ApprovedPetAssetsDto(
                    previewPath = "previews/overall-showcase.png",
                    motionSheetCount = 2
                ),
                totalScore = 86
            )
        )
    )

    val pets = response.toApprovedPets()

    assertEquals(1, pets.size)
    assertEquals("pet-stardust-001", pets[0].petId)
    assertEquals("Stardust Dragon", pets[0].displayName)
    assertEquals("fantasy-pet-rule", pets[0].sourceKind)
    assertEquals("previews/overall-showcase.png", pets[0].previewPath)
    assertEquals(2, pets[0].motionSheetCount)
    assertEquals(86, pets[0].totalScore)
}
```

- [x] **Step 2: Run mapper test to verify RED**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.api.CommunityApiMappersTest --console=plain
```

Expected: FAIL because approved pet DTOs and mapper do not exist.

- [x] **Step 3: Add DTOs, model, and mapper**

Add `ApprovedPet` to `PetShellModels.kt`:

```kotlin
data class ApprovedPet(
    val petId: String,
    val displayName: String,
    val sourceKind: String,
    val previewPath: String,
    val motionSheetCount: Int,
    val totalScore: Int
)
```

Add DTOs to `CommunityApiDtos.kt`:

```kotlin
@Serializable
data class ApprovedPetsResponseDto(val items: List<ApprovedPetDto> = emptyList())

@Serializable
data class ApprovedPetDto(
    val petId: String,
    val displayName: String,
    val ownerUserId: String,
    val source: ApprovedPetSourceDto = ApprovedPetSourceDto(),
    val assets: ApprovedPetAssetsDto = ApprovedPetAssetsDto(),
    val totalScore: Int = 0
)

@Serializable
data class ApprovedPetSourceDto(val kind: String = "")

@Serializable
data class ApprovedPetAssetsDto(
    val previewPath: String = "",
    val motionSheetCount: Int = 0
)
```

Add mapper:

```kotlin
fun ApprovedPetsResponseDto.toApprovedPets(): List<ApprovedPet> =
    items.map { item ->
        ApprovedPet(
            petId = item.petId,
            displayName = item.displayName,
            sourceKind = item.source.kind,
            previewPath = item.assets.previewPath,
            motionSheetCount = item.assets.motionSheetCount,
            totalScore = item.totalScore
        )
    }
```

- [x] **Step 4: Run mapper test to verify GREEN**

Run the mapper test command again.

Expected: PASS.

## Task 2: Client And Repository

- [x] **Step 1: Write failing HTTP decode test**

Add to `HttpCommunityApiClientTest.kt`:

```kotlin
@Test
fun decodesApprovedPetsJson() {
    val json = """
        {
          "items": [
            {
              "petId": "pet-stardust-001",
              "displayName": "Stardust Dragon",
              "ownerUserId": "user-demo-001",
              "source": {"kind": "fantasy-pet-rule"},
              "assets": {"previewPath": "previews/overall-showcase.png", "motionSheetCount": 2},
              "totalScore": 86
            }
          ]
        }
    """.trimIndent()

    val response = HttpCommunityApiClient.decodeApprovedPets(json)

    assertEquals("Stardust Dragon", response.items[0].displayName)
    assertEquals(2, response.items[0].assets.motionSheetCount)
}
```

- [x] **Step 2: Write failing repository test**

In `CommunityRepositoryTest.kt`, add `approvedPetsResponse` to `FakeCommunityApiClient`, implement `getApprovedPets()`, and assert:

```kotlin
assertEquals(1, result.approvedPets.size)
assertEquals("Stardust Dragon", result.approvedPets[0].displayName)
```

for a successful response, plus fallback assertion:

```kotlin
assertTrue(result.approvedPets.isEmpty())
```

when approved pets fail.

- [x] **Step 3: Run targeted Android tests to verify RED**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.api.HttpCommunityApiClientTest --tests com.gamer.community.api.CommunityRepositoryTest --console=plain
```

Expected: FAIL because client and repository approved pet support does not exist.

- [x] **Step 4: Implement client and repository**

Add to `CommunityApiClient`:

```kotlin
suspend fun getApprovedPets(): ApiCallResult<ApprovedPetsResponseDto>
```

Add to `HttpCommunityApiClient`:

```kotlin
override suspend fun getApprovedPets(): ApiCallResult<ApprovedPetsResponseDto> =
    get("/v1/pets/approved", Companion::decodeApprovedPets)

fun decodeApprovedPets(text: String): ApprovedPetsResponseDto =
    json.decodeFromString<ApprovedPetsResponseDto>(text)
```

Add `approvedPets: List<ApprovedPet>` to `InitialCommunityResult`.

In `loadInitialCommunity()`, call `client.getApprovedPets()`, map success with `toApprovedPets()`, fallback to `emptyList()`, and keep existing fallback behavior based on feed/wallet.

- [x] **Step 5: Run targeted Android tests to verify GREEN**

Run the same targeted Android command again.

Expected: PASS.

## Task 3: Shell State And UI

- [x] **Step 1: Write failing controller/UI model tests**

Update or add tests so:

```kotlin
assertEquals(1, updated.approvedPets.size)
assertEquals("1 approved pet", approvedPetRegistrySummary(listOf(approvedPet)))
assertEquals("No approved pets yet", approvedPetRegistrySummary(emptyList()))
```

- [x] **Step 2: Run relevant Android tests to verify RED**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.petshell.PetShellControllerTest --tests com.gamer.community.ui.PetShellUiModelTest --console=plain
```

- [x] **Step 3: Implement shell state and UI summary**

Add `approvedPets: List<ApprovedPet>` to `PetShellState`.

Update `PetShellController.initialState()` and `applyCommunityLoad()` to carry approved pets.

Update `PetShellApp` call:

```kotlin
approvedPets = result.approvedPets
```

Add a compact UI row in `CommunityScreen()` under the wallet/header or speech area using:

```kotlin
Text(text = approvedPetRegistrySummary(state.approvedPets), ...)
```

Add helper:

```kotlin
internal fun approvedPetRegistrySummary(pets: List<ApprovedPet>): String =
    if (pets.isEmpty()) "No approved pets yet" else "${pets.size} approved pet${if (pets.size == 1) "" else "s"}"
```

- [x] **Step 4: Run relevant Android tests to verify GREEN**

Run the same controller/UI targeted command again.

Expected: PASS.

## Task 4: Verification

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
git add docs/superpowers/plans/2026-06-07-phase-6d-android-approved-pet-registry.md apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiClient.kt apps/android-community/app/src/main/java/com/gamer/community/api/HttpCommunityApiClient.kt apps/android-community/app/src/main/java/com/gamer/community/api/CommunityRepository.kt apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellModels.kt apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellController.kt apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt apps/android-community/app/src/test/java/com/gamer/community/api/CommunityApiMappersTest.kt apps/android-community/app/src/test/java/com/gamer/community/api/HttpCommunityApiClientTest.kt apps/android-community/app/src/test/java/com/gamer/community/api/CommunityRepositoryTest.kt apps/android-community/app/src/test/java/com/gamer/community/petshell/PetShellControllerTest.kt apps/android-community/app/src/test/java/com/gamer/community/ui/PetShellUiModelTest.kt
git commit -m "Load approved pets in Android shell"
```

## Self-Review

- Spec coverage: Connects approved pet registry to Android, supporting the community showcase path.
- Placeholder scan: No TODO/TBD placeholders.
- Type consistency: Uses `ApprovedPet` and approved registry DTO names consistently.
