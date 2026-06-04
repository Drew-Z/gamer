# Phase 5b Android Live API Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Connect the Android pet shell to the local community API for feed, wallet, and daily check-in while preserving local fallback behavior.

**Architecture:** Keep `PetShellController` pure and add a small Android API/repository layer. Compose uses `LaunchedEffect` for first load and `rememberCoroutineScope` for check-in clicks. Remote failures return explicit local fallback results instead of blocking the pet-first shell.

**Tech Stack:** Android Gradle Plugin 9.2.0, Kotlin 2.2.10, Jetpack Compose BOM 2025.12.00, kotlinx-coroutines-android/test 1.11.0, kotlinx-serialization-json 1.11.0, `HttpURLConnection`, JUnit 4.

---

## File Structure

- Modify `apps/android-community/gradle/libs.versions.toml`: add coroutines, serialization runtime, and serialization compiler plugin aliases.
- Modify `apps/android-community/build.gradle`: expose the serialization plugin alias without changing the existing Android/Kotlin plugin baseline.
- Modify `apps/android-community/app/build.gradle`: apply serialization, add dependencies, enable `BuildConfig`, and define the local emulator API base URL.
- Modify `apps/android-community/app/src/main/AndroidManifest.xml`: add `android.permission.INTERNET`.
- Create `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt`: serializable DTOs matching `community-api`.
- Create `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt`: API DTO to pet shell domain mapping.
- Create `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiClient.kt`: narrow client interface plus response wrapper.
- Create `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityRepository.kt`: remote/fallback orchestration.
- Create `apps/android-community/app/src/main/java/com/gamer/community/api/HttpCommunityApiClient.kt`: minimal JSON HTTP implementation.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellController.kt`: add pure state application helpers for remote/fallback load and check-in.
- Modify `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`: wire repository loading and check-in orchestration.
- Create tests under `apps/android-community/app/src/test/java/com/gamer/community/api/`.
- Modify `apps/android-community/app/src/test/java/com/gamer/community/petshell/PetShellControllerTest.kt`: cover remote/fallback state helpers.

## Context7 References Used

- `/kotlin/kotlinx.coroutines`: current Android module guidance includes `kotlinx-coroutines-android` and `Dispatchers.IO` for background work.
- `/kotlin/kotlinx.serialization`: current setup uses the Kotlin serialization compiler plugin plus `kotlinx-serialization-json`, with `@Serializable` DTOs and `Json.decodeFromString`.
- `/websites/developer_android_develop_ui_compose`: current Compose side-effect guidance uses `LaunchedEffect` for suspend work entering composition and `rememberCoroutineScope` for event-handler coroutines.

---

### Task 1: Gradle And Manifest Network Setup

**Files:**
- Modify: `apps/android-community/gradle/libs.versions.toml`
- Modify: `apps/android-community/build.gradle`
- Modify: `apps/android-community/app/build.gradle`
- Modify: `apps/android-community/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add version catalog entries**

Edit `apps/android-community/gradle/libs.versions.toml` so the relevant sections include these entries:

```toml
[versions]
coroutines = "1.11.0"
serialization-json = "1.11.0"

[libraries]
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization-json" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Expose the serialization plugin at the root**

Edit `apps/android-community/build.gradle`:

```gradle
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 3: Apply app dependencies and BuildConfig**

Edit `apps/android-community/app/build.gradle`:

```gradle
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = 'com.gamer.community'
    compileSdk = libs.versions.android.compile.sdk.get().toInteger()

    defaultConfig {
        applicationId = 'com.gamer.community'
        minSdk = libs.versions.android.min.sdk.get().toInteger()
        targetSdk = libs.versions.android.target.sdk.get().toInteger()
        versionCode = 1
        versionName = '0.1.0'

        testInstrumentationRunner = 'androidx.test.runner.AndroidJUnitRunner'
        buildConfigField 'String', 'COMMUNITY_API_BASE_URL', '"http://10.0.2.2:4000"'
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation platform(libs.compose.bom)
    implementation libs.activity.compose
    implementation libs.compose.material3
    implementation libs.compose.ui
    implementation libs.compose.ui.tooling.preview
    implementation libs.core.ktx
    implementation libs.kotlinx.coroutines.android
    implementation libs.kotlinx.serialization.json

    debugImplementation libs.compose.ui.tooling

    testImplementation libs.junit
    testImplementation libs.kotlinx.coroutines.test
}
```

- [ ] **Step 4: Add internet permission**

Edit `apps/android-community/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:theme="@style/AppTheme"
        android:label="Gamer">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 5: Verify Gradle sync via tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest
```

Expected: existing `PetShellControllerTest` tests pass.

- [ ] **Step 6: Commit**

```powershell
git -C D:\workspace4Codex\gamer add apps/android-community
git -C D:\workspace4Codex\gamer commit -m "Add Android API integration dependencies"
```

---

### Task 2: API DTOs And Feed Mapping

**Files:**
- Create: `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiDtos.kt`
- Create: `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiMappers.kt`
- Test: `apps/android-community/app/src/test/java/com/gamer/community/api/CommunityApiMappersTest.kt`

- [ ] **Step 1: Write failing mapper tests**

Create `CommunityApiMappersTest.kt`:

```kotlin
package com.gamer.community.api

import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityApiMappersTest {
    @Test
    fun mapsFeedResponseToShellPosts() {
        val response = FeedResponseDto(
            items = listOf(
                FeedPostDto(
                    id = "post-live-001",
                    authorId = "user-demo-001",
                    petId = "pet-live-001",
                    title = "Live pet pose",
                    body = "Loaded from community-api.",
                    reactionCount = 42,
                    createdAt = "2026-06-05T00:00:00.000Z"
                )
            ),
            nextCursor = "page-2"
        )

        val posts = response.toFeedPosts()

        assertEquals(1, posts.size)
        assertEquals("post-live-001", posts[0].id)
        assertEquals("pet-live-001", posts[0].petId)
        assertEquals("Live pet pose", posts[0].title)
        assertEquals("Loaded from community-api.", posts[0].body)
        assertEquals("Demo Keeper", posts[0].authorName)
        assertEquals(42, posts[0].reactionCount)
    }

    @Test
    fun mapsUnknownAuthorToStableFallback() {
        val response = FeedResponseDto(
            items = listOf(
                FeedPostDto(
                    id = "post-live-002",
                    authorId = "user-new-999",
                    petId = "pet-live-002",
                    title = "New keeper",
                    body = "Author profile is not loaded in phase 5b.",
                    reactionCount = 3,
                    createdAt = "2026-06-05T00:01:00.000Z"
                )
            )
        )

        val posts = response.toFeedPosts()

        assertEquals("Keeper user-new-999", posts[0].authorName)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.api.CommunityApiMappersTest
```

Expected: FAIL because `FeedResponseDto` and `toFeedPosts` do not exist.

- [ ] **Step 3: Add DTOs**

Create `CommunityApiDtos.kt`:

```kotlin
package com.gamer.community.api

import kotlinx.serialization.Serializable

@Serializable
data class FeedResponseDto(
    val items: List<FeedPostDto> = emptyList(),
    val nextCursor: String? = null
)

@Serializable
data class FeedPostDto(
    val id: String,
    val authorId: String,
    val petId: String,
    val title: String,
    val body: String,
    val reactionCount: Int,
    val createdAt: String
)

@Serializable
data class WalletDto(
    val userId: String,
    val balance: Int,
    val currencyCode: String,
    val ledgerEntries: List<LedgerEntryDto> = emptyList()
)

@Serializable
data class LedgerEntryDto(
    val entryId: String,
    val userId: String,
    val amount: Int,
    val sourceType: String,
    val sourceId: String,
    val status: String,
    val createdAt: String
)

@Serializable
data class CheckInResponseDto(
    val checkIn: CheckInDto,
    val wallet: WalletDto,
    val ledgerEntry: LedgerEntryDto? = null
)

@Serializable
data class CheckInDto(
    val userId: String,
    val date: String,
    val claimed: Boolean,
    val rewardAmount: Int,
    val ledgerEntryId: String
)
```

- [ ] **Step 4: Add mapper implementation**

Create `CommunityApiMappers.kt`:

```kotlin
package com.gamer.community.api

import com.gamer.community.petshell.FeedPost

fun FeedResponseDto.toFeedPosts(): List<FeedPost> =
    items.map { item ->
        FeedPost(
            id = item.id,
            petId = item.petId,
            title = item.title,
            body = item.body,
            authorName = item.authorDisplayName(),
            reactionCount = item.reactionCount
        )
    }

private fun FeedPostDto.authorDisplayName(): String =
    when (authorId) {
        "user-demo-001" -> "Demo Keeper"
        else -> "Keeper $authorId"
    }
```

- [ ] **Step 5: Run mapper tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.api.CommunityApiMappersTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git -C D:\workspace4Codex\gamer add apps/android-community
git -C D:\workspace4Codex\gamer commit -m "Add Android community API DTO mapping"
```

---

### Task 3: Repository Remote Success And Fallback

**Files:**
- Create: `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityApiClient.kt`
- Create: `apps/android-community/app/src/main/java/com/gamer/community/api/CommunityRepository.kt`
- Test: `apps/android-community/app/src/test/java/com/gamer/community/api/CommunityRepositoryTest.kt`

- [ ] **Step 1: Write failing repository tests**

Create `CommunityRepositoryTest.kt`:

```kotlin
package com.gamer.community.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityRepositoryTest {
    @Test
    fun loadInitialCommunityReturnsRemoteFeedAndWallet() = runTest {
        val repository = CommunityRepository(
            client = FakeCommunityApiClient(
                feedResponse = ApiCallResult.Success(
                    FeedResponseDto(
                        items = listOf(
                            FeedPostDto(
                                id = "post-live-001",
                                authorId = "user-demo-001",
                                petId = "pet-live-001",
                                title = "Live feed",
                                body = "Remote body",
                                reactionCount = 18,
                                createdAt = "2026-06-05T00:00:00.000Z"
                            )
                        )
                    )
                ),
                walletResponse = ApiCallResult.Success(
                    WalletDto(
                        userId = "user-demo-001",
                        balance = 123,
                        currencyCode = "petcoin"
                    )
                )
            )
        )

        val result = repository.loadInitialCommunity()

        assertFalse(result.usedFallback)
        assertEquals(1, result.posts.size)
        assertEquals("Live feed", result.posts[0].title)
        assertEquals(123, result.walletBalance)
    }

    @Test
    fun loadInitialCommunityFallsBackWhenRemoteFails() = runTest {
        val repository = CommunityRepository(
            client = FakeCommunityApiClient(
                feedResponse = ApiCallResult.Failure("network_down"),
                walletResponse = ApiCallResult.Failure("network_down")
            )
        )

        val result = repository.loadInitialCommunity()

        assertTrue(result.usedFallback)
        assertEquals("Local fallback active.", result.message)
        assertEquals("Stardust dragon launch pose", result.posts[0].title)
        assertEquals(90, result.walletBalance)
    }

    @Test
    fun claimDailyCheckInReturnsRemoteWalletAndRewardAmount() = runTest {
        val repository = CommunityRepository(
            client = FakeCommunityApiClient(
                checkInResponse = ApiCallResult.Success(
                    CheckInResponseDto(
                        checkIn = CheckInDto(
                            userId = "user-demo-001",
                            date = "2026-06-05",
                            claimed = true,
                            rewardAmount = 10,
                            ledgerEntryId = "ledger-checkin-2026-06-05"
                        ),
                        wallet = WalletDto(
                            userId = "user-demo-001",
                            balance = 133,
                            currencyCode = "petcoin"
                        ),
                        ledgerEntry = null
                    )
                )
            )
        )

        val result = repository.claimDailyCheckIn()

        assertFalse(result.usedFallback)
        assertEquals(133, result.walletBalance)
        assertEquals(10, result.rewardAmount)
        assertTrue(result.claimed)
    }

    @Test
    fun claimDailyCheckInReturnsFallbackWhenRemoteFails() = runTest {
        val repository = CommunityRepository(
            client = FakeCommunityApiClient(
                checkInResponse = ApiCallResult.Failure("network_down")
            )
        )

        val result = repository.claimDailyCheckIn()

        assertTrue(result.usedFallback)
        assertEquals("Local check-in fallback active.", result.message)
        assertEquals(10, result.rewardAmount)
        assertTrue(result.claimed)
    }
}

private class FakeCommunityApiClient(
    private val feedResponse: ApiCallResult<FeedResponseDto> = ApiCallResult.Failure("not_configured"),
    private val walletResponse: ApiCallResult<WalletDto> = ApiCallResult.Failure("not_configured"),
    private val checkInResponse: ApiCallResult<CheckInResponseDto> = ApiCallResult.Failure("not_configured")
) : CommunityApiClient {
    override suspend fun getFeed(): ApiCallResult<FeedResponseDto> = feedResponse

    override suspend fun getWallet(): ApiCallResult<WalletDto> = walletResponse

    override suspend fun claimDailyCheckIn(): ApiCallResult<CheckInResponseDto> = checkInResponse
}
```

- [ ] **Step 2: Run repository tests to verify they fail**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.api.CommunityRepositoryTest
```

Expected: FAIL because `CommunityRepository`, `CommunityApiClient`, and `ApiCallResult` do not exist.

- [ ] **Step 3: Add client contract**

Create `CommunityApiClient.kt`:

```kotlin
package com.gamer.community.api

sealed interface ApiCallResult<out T> {
    data class Success<T>(val value: T) : ApiCallResult<T>
    data class Failure(val reason: String) : ApiCallResult<Nothing>
}

interface CommunityApiClient {
    suspend fun getFeed(): ApiCallResult<FeedResponseDto>
    suspend fun getWallet(): ApiCallResult<WalletDto>
    suspend fun claimDailyCheckIn(): ApiCallResult<CheckInResponseDto>
}
```

- [ ] **Step 4: Add repository implementation**

Create `CommunityRepository.kt`:

```kotlin
package com.gamer.community.api

import com.gamer.community.petshell.FeedPost
import com.gamer.community.petshell.fixtureFeedPosts

data class InitialCommunityResult(
    val posts: List<FeedPost>,
    val walletBalance: Int,
    val usedFallback: Boolean,
    val message: String
)

data class CheckInResult(
    val walletBalance: Int?,
    val claimed: Boolean,
    val rewardAmount: Int,
    val usedFallback: Boolean,
    val message: String
)

class CommunityRepository(
    private val client: CommunityApiClient
) {
    suspend fun loadInitialCommunity(): InitialCommunityResult {
        val feed = client.getFeed()
        val wallet = client.getWallet()

        val remotePosts = (feed as? ApiCallResult.Success)?.value?.toFeedPosts().orEmpty()
        val remoteWallet = (wallet as? ApiCallResult.Success)?.value
        val hasUsableRemoteFeed = remotePosts.isNotEmpty()

        if (hasUsableRemoteFeed && remoteWallet != null) {
            return InitialCommunityResult(
                posts = remotePosts,
                walletBalance = remoteWallet.balance,
                usedFallback = false,
                message = "Community ready."
            )
        }

        return InitialCommunityResult(
            posts = if (hasUsableRemoteFeed) remotePosts else fixtureFeedPosts,
            walletBalance = remoteWallet?.balance ?: 90,
            usedFallback = true,
            message = "Local fallback active."
        )
    }

    suspend fun claimDailyCheckIn(): CheckInResult {
        val result = client.claimDailyCheckIn()
        val response = (result as? ApiCallResult.Success)?.value

        if (response != null) {
            return CheckInResult(
                walletBalance = response.wallet.balance,
                claimed = response.checkIn.claimed,
                rewardAmount = response.checkIn.rewardAmount,
                usedFallback = false,
                message = "Daily reward claimed: +${response.checkIn.rewardAmount} petcoin."
            )
        }

        return CheckInResult(
            walletBalance = null,
            claimed = true,
            rewardAmount = 10,
            usedFallback = true,
            message = "Local check-in fallback active."
        )
    }
}
```

- [ ] **Step 5: Run repository tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.api.CommunityRepositoryTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git -C D:\workspace4Codex\gamer add apps/android-community
git -C D:\workspace4Codex\gamer commit -m "Add Android community repository fallback"
```

---

### Task 4: Pure Pet Shell Remote State Application

**Files:**
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/petshell/PetShellController.kt`
- Modify: `apps/android-community/app/src/test/java/com/gamer/community/petshell/PetShellControllerTest.kt`

- [ ] **Step 1: Add failing controller tests**

Append these tests to `PetShellControllerTest.kt`:

```kotlin
    @Test
    fun applyingRemoteCommunityLoadReplacesPostsAndWallet() {
        val state = PetShellController.initialState()
        val remotePost = FeedPost(
            id = "post-live-001",
            petId = "pet-live-001",
            title = "Live feed",
            body = "Remote body",
            authorName = "Demo Keeper",
            reactionCount = 18
        )

        val loaded = PetShellController.applyCommunityLoad(
            state = state,
            posts = listOf(remotePost),
            walletBalance = 123,
            usedFallback = false,
            message = "Community ready."
        )

        assertEquals(0, loaded.feedIndex)
        assertEquals(123, loaded.walletBalance)
        assertEquals("Live feed", loaded.currentPost.title)
        assertEquals(PetAction.Idle, loaded.petAction)
        assertEquals("Community ready.", loaded.speechBubble)
    }

    @Test
    fun applyingFallbackCommunityLoadKeepsExistingPostsWhenInputIsEmpty() {
        val state = PetShellController.initialState()

        val loaded = PetShellController.applyCommunityLoad(
            state = state,
            posts = emptyList(),
            walletBalance = 90,
            usedFallback = true,
            message = "Local fallback active."
        )

        assertEquals("Stardust dragon launch pose", loaded.currentPost.title)
        assertEquals(PetAction.AppLoading, loaded.petAction)
        assertEquals("Local fallback active.", loaded.speechBubble)
    }

    @Test
    fun applyingRemoteCheckInUsesRemoteWalletBalance() {
        val open = PetShellController.onBubbleTapped(PetShellController.initialState())

        val checkedIn = PetShellController.applyCheckInResult(
            state = open,
            walletBalance = 133,
            claimed = true,
            rewardAmount = 10,
            usedFallback = false,
            message = "Daily reward claimed: +10 petcoin."
        )

        assertEquals(133, checkedIn.walletBalance)
        assertTrue(checkedIn.checkInClaimed)
        assertEquals(PetAction.Reward, checkedIn.petAction)
        assertEquals("Daily reward claimed: +10 petcoin.", checkedIn.speechBubble)
    }

    @Test
    fun applyingFallbackCheckInUsesLocalIncrement() {
        val open = PetShellController.onBubbleTapped(PetShellController.initialState())

        val checkedIn = PetShellController.applyCheckInResult(
            state = open,
            walletBalance = null,
            claimed = true,
            rewardAmount = 10,
            usedFallback = true,
            message = "Local check-in fallback active."
        )

        assertEquals(100, checkedIn.walletBalance)
        assertTrue(checkedIn.checkInClaimed)
        assertEquals(PetAction.Reward, checkedIn.petAction)
        assertEquals("Local check-in fallback active.", checkedIn.speechBubble)
    }
```

- [ ] **Step 2: Run controller tests to verify they fail**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.petshell.PetShellControllerTest
```

Expected: FAIL because `applyCommunityLoad` and `applyCheckInResult` do not exist.

- [ ] **Step 3: Add pure controller helpers**

Add these methods inside `object PetShellController` in `PetShellController.kt`:

```kotlin
    fun applyCommunityLoad(
        state: PetShellState,
        posts: List<FeedPost>,
        walletBalance: Int,
        usedFallback: Boolean,
        message: String
    ): PetShellState {
        val nextPosts = posts.ifEmpty { state.posts }
        return state.copy(
            petAction = if (usedFallback) state.petAction else PetAction.Idle,
            speechBubble = message,
            feedIndex = 0,
            walletBalance = walletBalance,
            posts = nextPosts
        )
    }

    fun applyCheckInResult(
        state: PetShellState,
        walletBalance: Int?,
        claimed: Boolean,
        rewardAmount: Int,
        usedFallback: Boolean,
        message: String
    ): PetShellState {
        if (state.checkInClaimed) {
            return state.copy(
                petAction = PetAction.Idle,
                speechBubble = "Daily reward already claimed."
            )
        }

        val nextWalletBalance = walletBalance ?: state.walletBalance + rewardAmount
        return state.copy(
            walletBalance = nextWalletBalance,
            checkInClaimed = claimed,
            petAction = PetAction.Reward,
            speechBubble = message
        )
    }
```

- [ ] **Step 4: Run controller tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.petshell.PetShellControllerTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git -C D:\workspace4Codex\gamer add apps/android-community
git -C D:\workspace4Codex\gamer commit -m "Apply remote community data to pet shell state"
```

---

### Task 5: HTTP Community API Client

**Files:**
- Create: `apps/android-community/app/src/main/java/com/gamer/community/api/HttpCommunityApiClient.kt`
- Test: `apps/android-community/app/src/test/java/com/gamer/community/api/HttpCommunityApiClientTest.kt`

- [ ] **Step 1: Write JSON parsing and failure tests**

Create `HttpCommunityApiClientTest.kt`:

```kotlin
package com.gamer.community.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpCommunityApiClientTest {
    @Test
    fun decodesFeedJson() {
        val json = """
            {
              "items": [
                {
                  "id": "post-live-001",
                  "authorId": "user-demo-001",
                  "petId": "pet-live-001",
                  "title": "Live feed",
                  "body": "Remote body",
                  "reactionCount": 18,
                  "createdAt": "2026-06-05T00:00:00.000Z"
                }
              ],
              "nextCursor": "page-2"
            }
        """.trimIndent()

        val feed = HttpCommunityApiClient.decodeFeed(json)

        assertEquals(1, feed.items.size)
        assertEquals("Live feed", feed.items[0].title)
        assertEquals("page-2", feed.nextCursor)
    }

    @Test
    fun decodesWalletJson() {
        val json = """
            {
              "userId": "user-demo-001",
              "balance": 123,
              "currencyCode": "petcoin",
              "ledgerEntries": []
            }
        """.trimIndent()

        val wallet = HttpCommunityApiClient.decodeWallet(json)

        assertEquals("user-demo-001", wallet.userId)
        assertEquals(123, wallet.balance)
        assertEquals("petcoin", wallet.currencyCode)
    }

    @Test
    fun invalidJsonBecomesFailure() {
        val result = HttpCommunityApiClient.decodeCatching("not-json") {
            HttpCommunityApiClient.decodeFeed(it)
        }

        assertTrue(result is ApiCallResult.Failure)
    }
}
```

- [ ] **Step 2: Run HTTP client tests to verify they fail**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.api.HttpCommunityApiClientTest
```

Expected: FAIL because `HttpCommunityApiClient` does not exist.

- [ ] **Step 3: Add HTTP client implementation**

Create `HttpCommunityApiClient.kt`:

```kotlin
package com.gamer.community.api

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class HttpCommunityApiClient(
    private val baseUrl: String
) : CommunityApiClient {
    override suspend fun getFeed(): ApiCallResult<FeedResponseDto> =
        get("/v1/feed", Companion::decodeFeed)

    override suspend fun getWallet(): ApiCallResult<WalletDto> =
        get("/v1/wallet/me", Companion::decodeWallet)

    override suspend fun claimDailyCheckIn(): ApiCallResult<CheckInResponseDto> =
        post("/v1/check-in", "{}", Companion::decodeCheckIn)

    private suspend fun <T> get(
        path: String,
        decode: (String) -> T
    ): ApiCallResult<T> = request(method = "GET", path = path, body = null, decode = decode)

    private suspend fun <T> post(
        path: String,
        body: String,
        decode: (String) -> T
    ): ApiCallResult<T> = request(method = "POST", path = path, body = body, decode = decode)

    private suspend fun <T> request(
        method: String,
        path: String,
        body: String?,
        decode: (String) -> T
    ): ApiCallResult<T> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection)
            connection.requestMethod = method
            connection.connectTimeout = 2_000
            connection.readTimeout = 2_000
            connection.setRequestProperty("Accept", "application/json")

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { output ->
                    output.write(body.toByteArray(Charsets.UTF_8))
                }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            connection.disconnect()

            if (status in 200..299) {
                decodeCatching(text, decode)
            } else {
                ApiCallResult.Failure("http_$status")
            }
        } catch (error: Exception) {
            ApiCallResult.Failure(error.message ?: error::class.java.simpleName)
        }
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }

        fun decodeFeed(text: String): FeedResponseDto =
            json.decodeFromString<FeedResponseDto>(text)

        fun decodeWallet(text: String): WalletDto =
            json.decodeFromString<WalletDto>(text)

        fun decodeCheckIn(text: String): CheckInResponseDto =
            json.decodeFromString<CheckInResponseDto>(text)

        fun <T> decodeCatching(
            text: String,
            decode: (String) -> T
        ): ApiCallResult<T> =
            try {
                ApiCallResult.Success(decode(text))
            } catch (error: Exception) {
                ApiCallResult.Failure(error.message ?: error::class.java.simpleName)
            }
    }
}
```

- [ ] **Step 4: Run HTTP client tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest --tests com.gamer.community.api.HttpCommunityApiClientTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git -C D:\workspace4Codex\gamer add apps/android-community
git -C D:\workspace4Codex\gamer commit -m "Add Android HTTP community API client"
```

---

### Task 6: Compose App Integration

**Files:**
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/ui/PetShellApp.kt`
- Modify: `apps/android-community/app/src/main/java/com/gamer/community/MainActivity.kt`

- [ ] **Step 1: Add repository injection to app entry**

Edit `MainActivity.kt`:

```kotlin
package com.gamer.community

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gamer.community.api.CommunityRepository
import com.gamer.community.api.HttpCommunityApiClient
import com.gamer.community.ui.PetShellApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = CommunityRepository(
            client = HttpCommunityApiClient(BuildConfig.COMMUNITY_API_BASE_URL)
        )

        setContent {
            PetShellApp(repository = repository)
        }
    }
}
```

- [ ] **Step 2: Wire launch loading and check-in in Compose**

Edit the top of `PetShellApp.kt` imports:

```kotlin
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.gamer.community.api.CommunityRepository
import kotlinx.coroutines.launch
```

Change `PetShellApp`:

```kotlin
@Composable
fun PetShellApp(repository: CommunityRepository) {
    var state by remember { mutableStateOf(PetShellController.initialState()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository) {
        val result = repository.loadInitialCommunity()
        state = PetShellController.applyCommunityLoad(
            state = state,
            posts = result.posts,
            walletBalance = result.walletBalance,
            usedFallback = result.usedFallback,
            message = result.message
        )
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F8FA)) {
            when (state.phase) {
                ShellPhase.LaunchBubble -> LaunchBubbleScreen(
                    state = state,
                    onBubbleTapped = {
                        state = PetShellController.onBubbleTapped(state)
                    }
                )

                ShellPhase.Community -> CommunityScreen(
                    state = state,
                    onNavigate = { direction ->
                        state = PetShellController.navigateFeed(state, direction)
                    },
                    onCheckIn = {
                        scope.launch {
                            val result = repository.claimDailyCheckIn()
                            state = PetShellController.applyCheckInResult(
                                state = state,
                                walletBalance = result.walletBalance,
                                claimed = result.claimed,
                                rewardAmount = result.rewardAmount,
                                usedFallback = result.usedFallback,
                                message = result.message
                            )
                        }
                    }
                )
            }
        }
    }
}
```

- [ ] **Step 3: Add no-argument app overload**

Add this overload below the injected version so previews and simple local call sites still have a no-argument entry point:

```kotlin
@Composable
fun PetShellApp() {
    PetShellApp(
        repository = CommunityRepository(
            client = HttpCommunityApiClient(com.gamer.community.BuildConfig.COMMUNITY_API_BASE_URL)
        )
    )
}
```

Keep `MainActivity` using the injected `repository` value.

- [ ] **Step 4: Run Android unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 5: Assemble debug APK**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community assembleDebug
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git -C D:\workspace4Codex\gamer add apps/android-community
git -C D:\workspace4Codex\gamer commit -m "Connect Android pet shell to community repository"
```

---

### Task 7: End-To-End Local Verification

**Files:**
- No required code files.

- [ ] **Step 1: Run backend tests**

Run:

```powershell
npm.cmd test
```

Expected: PASS for Node packages and services.

- [ ] **Step 2: Run Android unit tests**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 3: Assemble Android app**

Run:

```powershell
D:\workspace4Codex\floating-pet-android\gradlew.bat -p D:\workspace4Codex\gamer\apps\android-community assembleDebug
```

Expected: PASS.

- [ ] **Step 4: Verify Docker config**

Run:

```powershell
docker compose -f D:\workspace4Codex\gamer\compose.yaml config
```

Expected: config renders services for `community-api`, `pet-generator`, and `admin-review`.

- [ ] **Step 5: Run the community API locally for manual Android verification**

Run:

```powershell
npm.cmd --prefix D:\workspace4Codex\gamer\services\community-api start
```

Expected: terminal prints `community-api listening on 4000`.

- [ ] **Step 6: Manual emulator smoke test**

Install or run the debug APK on an Android emulator. Expected behavior:

- App opens with pet bubble immediately.
- When the API is running, speech becomes `Community ready.`
- Feed title matches API fixture `Stardust dragon launch pose`.
- Wallet balance shows `90 petcoin`.
- Daily check-in either reports already claimed for the current date or adds the API reward and updates wallet from the API response.

- [ ] **Step 7: Manual fallback smoke test**

Stop `community-api`, relaunch the app on the emulator. Expected behavior:

- App opens with pet bubble immediately.
- Speech becomes `Local fallback active.`
- Feed remains usable with fixture posts.
- Daily check-in uses the local fallback message `Local check-in fallback active.`

- [ ] **Step 8: Final status check**

Run:

```powershell
git -C D:\workspace4Codex\gamer status --short
```

Expected: clean working tree.
