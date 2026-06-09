package com.gamer.community.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.gamer.community.api.ApiCallResult
import com.gamer.community.api.ApprovedPetPackageDto
import com.gamer.community.api.ApprovedPetsResponseDto
import com.gamer.community.api.CheckInDto
import com.gamer.community.api.CheckInResponseDto
import com.gamer.community.api.CommunityApiClient
import com.gamer.community.api.CommunityRepository
import com.gamer.community.api.FantasyPetPackageImportDraftRequestDto
import com.gamer.community.api.FeedResponseDto
import com.gamer.community.api.ImportDraftDto
import com.gamer.community.api.ImportDraftSubmissionResponseDto
import com.gamer.community.api.LedgerEntryDto
import com.gamer.community.api.SubmissionDto
import com.gamer.community.api.SubmissionsResponseDto
import com.gamer.community.api.WalletDto
import com.gamer.community.generation.FantasyPetGenerationClient
import com.gamer.community.generation.FantasyPetGenerationService
import com.gamer.community.generation.PetGenerationAppApiContractDto
import com.gamer.community.generation.PetGenerationArtifactDto
import com.gamer.community.generation.PetGenerationArtifactIndexResponseDto
import com.gamer.community.generation.PetGenerationJobCreateRequestDto
import com.gamer.community.generation.PetGenerationJobResponseDto
import com.gamer.community.generation.ReviewDecisionRequestDto
import com.gamer.community.generation.WorkerReadinessResponseDto
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PetShellAppFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun clearPersistedUiState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        context.getSharedPreferences("pet-shell-ui", 0)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("fantasy-pet-generation", 0)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun launchBubbleHasStableAutomationEntryPoint() {
        composeRule.setContent {
            PetShellApp(
                repository = CommunityRepository(FakeCommunityApiClient()),
                generationService = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
            )
        }

        composeRule.onNodeWithContentDescription("launch-bubble-enter")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithContentDescription("gamer-community-home")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("gamer-tab-generate")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithContentDescription("generation-app-job-id-input")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun communityShellUsesBottomTabsForCommunityGenerationAndProfile() {
        composeRule.setContent {
            PetShellApp(
                repository = CommunityRepository(FakeCommunityApiClient()),
                generationService = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
            )
        }

        composeRule.onNodeWithContentDescription("launch-bubble-enter")
            .performClick()

        composeRule.onNodeWithContentDescription("gamer-community-home")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("gamer-tab-community")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("gamer-tab-generate")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithContentDescription("gamer-generation-workspace")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("generation-app-job-id-input")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("gamer-tab-profile")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithContentDescription("gamer-profile-workspace")
            .assertIsDisplayed()
    }

    @Test
    fun communityHomePresentsGameCommunityChrome() {
        composeRule.setContent {
            PetShellApp(
                repository = CommunityRepository(FakeCommunityApiClient()),
                generationService = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
            )
        }

        composeRule.onNodeWithContentDescription("launch-bubble-enter")
            .performClick()

        composeRule.onNodeWithContentDescription("community-channel-rail")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("community-quick-actions")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("community-pet-companion-strip")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("community-showcase-panel")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("community-post-card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("community-feed-controls")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun profileWorkspacePresentsKeeperHomeSections() {
        composeRule.setContent {
            PetShellApp(
                repository = CommunityRepository(FakeCommunityApiClient()),
                generationService = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
            )
        }

        composeRule.onNodeWithContentDescription("launch-bubble-enter")
            .performClick()
        composeRule.onNodeWithContentDescription("gamer-tab-profile")
            .performClick()

        composeRule.onNodeWithContentDescription("profile-keeper-hero")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("profile-wallet-summary")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("profile-pet-shelf")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("profile-action-dock")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun petCompanionAvatarHasStableEntryPointAcrossShellSurfaces() {
        composeRule.setContent {
            PetShellApp(
                repository = CommunityRepository(FakeCommunityApiClient()),
                generationService = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
            )
        }

        composeRule.onAllNodesWithContentDescription("gamer-pet-avatar")
            .onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("launch-bubble-enter")
            .performClick()
        composeRule.onAllNodesWithContentDescription("gamer-pet-avatar")
            .onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("gamer-tab-generate")
            .performClick()
        composeRule.onAllNodesWithContentDescription("gamer-pet-avatar")
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun generationWorkspacePresentsCreatorWorkbenchSections() {
        composeRule.setContent {
            PetShellApp(
                repository = CommunityRepository(FakeCommunityApiClient()),
                generationService = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
            )
        }

        composeRule.onNodeWithContentDescription("launch-bubble-enter")
            .performClick()
        composeRule.onNodeWithContentDescription("gamer-tab-generate")
            .performClick()

        composeRule.onNodeWithContentDescription("generation-studio-hero")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("generation-flow-rail")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("generation-studio-status-dock")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("generation-brief-panel")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("generation-prompt-canvas")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("generation-runtime-console")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("generation-review-desk")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("generation-review-action-dock")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("generation-review-waiting-candidate")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("generation-review-notes-input")
            .assertCountEquals(0)
    }

    @Test
    fun launchBubbleLanguageToggleSwitchesBetweenChineseAndEnglish() {
        composeRule.setContent {
            PetShellApp(
                repository = CommunityRepository(FakeCommunityApiClient()),
                generationService = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
            )
        }

        composeRule.onNodeWithText("正在加载应用").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("切换到英文").performClick()

        composeRule.onNodeWithText("AppLoading").assertIsDisplayed()
        composeRule.onNodeWithText("Tap the bubble to enter").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Switch to Chinese").performClick()

        composeRule.onNodeWithText("正在加载应用").assertIsDisplayed()
        composeRule.onNodeWithText("点击气泡进入").assertIsDisplayed()
    }

    @Test
    fun generationPanelCreatesJobAcceptsCandidateAndDownloadsPetPackage() {
        val generationClient = RecordingFantasyPetGenerationClient()
        composeRule.setContent {
            PetShellApp(
                repository = CommunityRepository(FakeCommunityApiClient()),
                generationService = FantasyPetGenerationService(
                    client = generationClient,
                    apiBaseUrl = "file:///preview-disabled"
                ),
                initialGenerationDescription = "tiny stardust dragon"
            )
        }

        composeRule.onNodeWithText("本地兜底数据已启用。").performClick()
        composeRule.onNodeWithContentDescription("gamer-tab-generate").performClick()
        composeRule.onNodeWithContentDescription("generation-public-api-boundary-notice")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("generation-create")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            generationClient.createdRequest != null
        }
        assertEquals("fantasy-pet.app-job-create-request.v1", generationClient.createdRequest?.schema)
        assertEquals("tiny stardust dragon", generationClient.createdRequest?.description)

        composeRule.onNodeWithText("候选图画廊")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("candidate-select-candidate-1")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("已选为审核对象")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("review-accept")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            generationClient.reviewRequest != null
        }
        val reviewRequest = generationClient.reviewRequest
        assertNotNull(reviewRequest)
        assertEquals("fantasy-pet.review-decision.v1", reviewRequest?.schema)
        assertEquals("human-review", reviewRequest?.reviewer)
        assertEquals("accept", reviewRequest?.decision)
        assertEquals("candidate-1", reviewRequest?.targetDownloadId)

        composeRule.onNodeWithTag("package-download")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            generationClient.downloadedPackageJobId != null
        }
        assertEquals("job-ui-001", generationClient.downloadedPackageJobId)
    }

    @Test
    fun generationPanelDownloadsPackageCreatesImportDraftAndSubmitsCommunityReview() {
        val communityClient = RecordingCommunityApiClient()
        val generationClient = RecordingFantasyPetGenerationClient()
        composeRule.setContent {
            PetShellApp(
                repository = CommunityRepository(communityClient),
                generationService = FantasyPetGenerationService(
                    client = generationClient,
                    apiBaseUrl = "file:///preview-disabled"
                ),
                initialGenerationDescription = "tiny stardust dragon"
            )
        }

        composeRule.onNodeWithText("本地兜底数据已启用。").performClick()
        composeRule.onNodeWithContentDescription("gamer-tab-generate").performClick()
        composeRule.onNodeWithTag("generation-create")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            generationClient.createdRequest != null
        }
        composeRule.onNodeWithTag("candidate-select-candidate-1")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("review-accept")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            generationClient.reviewRequest != null
        }

        composeRule.onNodeWithTag("package-download")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            communityClient.importDraftRequest != null
        }

        val importDraftRequest = communityClient.importDraftRequest
        assertNotNull(importDraftRequest)
        assertEquals("fantasy-pet.package-manifest.v1", importDraftRequest?.packageManifest?.schema)
        assertEquals("job-ui-001", importDraftRequest?.packageManifest?.appJobId)
        assertEquals("human-review", importDraftRequest?.packageManifest?.acceptedBy)
        assertEquals("candidate-1", importDraftRequest?.targetDownloadId)
        assertEquals("candidate-1", importDraftRequest?.packageManifest?.sourceDownloadId)

        composeRule.onNodeWithContentDescription("generation-submit-community-review-button")
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            communityClient.submittedDraftId != null
        }
        assertEquals("import-draft-ui-001", communityClient.submittedDraftId)
        composeRule.onNodeWithContentDescription("generation-refresh-community-submission-button")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun contractDemoResumeShowsNoLiveWorkerCopyAndKeepsActionsDisabled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("fantasy-pet-generation", 0)
            .edit()
            .putString("appJobId", "public-lifecycle-smoke")
            .commit()

        composeRule.setContent {
            PetShellApp(
                repository = CommunityRepository(FakeCommunityApiClient()),
                generationService = FantasyPetGenerationService(
                    client = ContractDemoFantasyPetGenerationClient(),
                    apiBaseUrl = "file:///preview-disabled"
                )
            )
        }

        composeRule.onNodeWithContentDescription("launch-bubble-enter")
            .performClick()
        composeRule.onNodeWithContentDescription("gamer-tab-generate")
            .performClick()

        composeRule.onNodeWithText("契约演示资源已载入；真实生成 worker 尚未运行。")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("generation-contract-demo-notice")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("review-accept")
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithTag("package-download")
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun queuedJobResumeShowsServerWorkerWaitNoticeAndKeepsActionsDisabled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("fantasy-pet-generation", 0)
            .edit()
            .putString("appJobId", "queued-job-ui")
            .commit()
        val generationClient = QueuedFantasyPetGenerationClient()

        composeRule.setContent {
            PetShellApp(
                repository = CommunityRepository(FakeCommunityApiClient()),
                generationService = FantasyPetGenerationService(
                    client = generationClient,
                    apiBaseUrl = "file:///preview-disabled"
                )
            )
        }

        composeRule.onNodeWithContentDescription("launch-bubble-enter")
            .performClick()
        composeRule.onNodeWithContentDescription("gamer-tab-generate")
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            generationClient.pollCount > 0
        }

        composeRule.onNodeWithContentDescription("generation-server-worker-wait-notice")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("正在等待可信服务端 worker；app 只负责创建和轮询任务。")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("review-accept")
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithTag("package-download")
            .performScrollTo()
            .assertIsNotEnabled()
    }
}

private class FakeCommunityApiClient : CommunityApiClient {
    override suspend fun getFeed(): ApiCallResult<FeedResponseDto> =
        ApiCallResult.Success(FeedResponseDto())

    override suspend fun getWallet(): ApiCallResult<WalletDto> =
        ApiCallResult.Success(
            WalletDto(
                userId = "user-test",
                balance = 100,
                currencyCode = "petcoin"
            )
        )

    override suspend fun getApprovedPets(): ApiCallResult<ApprovedPetsResponseDto> =
        ApiCallResult.Success(ApprovedPetsResponseDto())

    override suspend fun getApprovedPetPackage(petId: String): ApiCallResult<ApprovedPetPackageDto> =
        ApiCallResult.Failure("not_found")

    override suspend fun createImportDraftFromFantasyPetPackage(
        request: FantasyPetPackageImportDraftRequestDto
    ): ApiCallResult<ImportDraftDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun submitImportDraft(draftId: String): ApiCallResult<ImportDraftSubmissionResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun getSubmissions(): ApiCallResult<SubmissionsResponseDto> =
        ApiCallResult.Success(SubmissionsResponseDto())

    override suspend fun claimDailyCheckIn(): ApiCallResult<CheckInResponseDto> =
        ApiCallResult.Success(
            CheckInResponseDto(
                checkIn = CheckInDto(
                    userId = "user-test",
                    date = "2026-06-08",
                    claimed = true,
                    rewardAmount = 10,
                    ledgerEntryId = "ledger-test"
                ),
                wallet = WalletDto(
                    userId = "user-test",
                    balance = 110,
                    currencyCode = "petcoin",
                    ledgerEntries = listOf(
                        LedgerEntryDto(
                            entryId = "ledger-test",
                            userId = "user-test",
                            amount = 10,
                            sourceType = "daily-check-in",
                            sourceId = "2026-06-08",
                            status = "confirmed",
                            createdAt = "2026-06-08T00:00:00Z"
                        )
                    )
                )
            )
        )
}

private class RecordingCommunityApiClient : CommunityApiClient {
    var importDraftRequest: FantasyPetPackageImportDraftRequestDto? = null
        private set
    var submittedDraftId: String? = null
        private set

    private val readyDraft = ImportDraftDto(
        id = "import-draft-ui-001",
        userId = "user-test",
        status = "ready",
        petId = "job-ui-001",
        ownershipClaimId = "claim-job-ui-001",
        scoreReportId = "score-import-draft-ui-001"
    )
    private val submittedDraft = readyDraft.copy(
        status = "submitted",
        submissionId = "submission-ui-001"
    )
    private val pendingSubmission = SubmissionDto(
        id = "submission-ui-001",
        petId = "job-ui-001",
        userId = "user-test",
        status = "pending",
        scoreReportId = "score-import-draft-ui-001",
        ownershipClaimId = "claim-job-ui-001",
        importDraftId = "import-draft-ui-001",
        submittedAt = "2026-06-08T00:00:00Z"
    )

    override suspend fun getFeed(): ApiCallResult<FeedResponseDto> =
        ApiCallResult.Success(FeedResponseDto())

    override suspend fun getWallet(): ApiCallResult<WalletDto> =
        ApiCallResult.Success(
            WalletDto(
                userId = "user-test",
                balance = 100,
                currencyCode = "petcoin"
            )
        )

    override suspend fun getApprovedPets(): ApiCallResult<ApprovedPetsResponseDto> =
        ApiCallResult.Success(ApprovedPetsResponseDto())

    override suspend fun getApprovedPetPackage(petId: String): ApiCallResult<ApprovedPetPackageDto> =
        ApiCallResult.Failure("not_found")

    override suspend fun createImportDraftFromFantasyPetPackage(
        request: FantasyPetPackageImportDraftRequestDto
    ): ApiCallResult<ImportDraftDto> {
        importDraftRequest = request
        return ApiCallResult.Success(readyDraft)
    }

    override suspend fun submitImportDraft(draftId: String): ApiCallResult<ImportDraftSubmissionResponseDto> {
        submittedDraftId = draftId
        return ApiCallResult.Success(
            ImportDraftSubmissionResponseDto(
                draft = submittedDraft,
                submission = pendingSubmission
            )
        )
    }

    override suspend fun getSubmissions(): ApiCallResult<SubmissionsResponseDto> =
        ApiCallResult.Success(
            SubmissionsResponseDto(
                submissions = listOf(pendingSubmission)
            )
        )

    override suspend fun claimDailyCheckIn(): ApiCallResult<CheckInResponseDto> =
        ApiCallResult.Failure("not_supported")
}

private class FakeFantasyPetGenerationClient : FantasyPetGenerationClient {
    override suspend fun createJob(
        request: PetGenerationJobCreateRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun getJob(appJobId: String): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun getArtifacts(
        appJobId: String
    ): ApiCallResult<PetGenerationArtifactIndexResponseDto> =
        ApiCallResult.Success(PetGenerationArtifactIndexResponseDto(appJobId = appJobId))

    override suspend fun submitReviewDecision(
        appJobId: String,
        request: ReviewDecisionRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun downloadPackage(appJobId: String): ApiCallResult<ByteArray> =
        ApiCallResult.Failure("not_supported")

    override suspend fun getWorkerReadiness(): ApiCallResult<WorkerReadinessResponseDto> =
        ApiCallResult.Success(WorkerReadinessResponseDto(status = "ready"))

    override suspend fun getAppApiContract(): ApiCallResult<PetGenerationAppApiContractDto> =
        ApiCallResult.Success(PetGenerationAppApiContractDto(schema = "fantasy-pet.app-api-contract.v1"))
}

private class ContractDemoFantasyPetGenerationClient : FantasyPetGenerationClient {
    private val demoJob = PetGenerationJobResponseDto(
        schema = "fantasy-pet.app-job-response.v1",
        appJobId = "public-lifecycle-smoke",
        runId = "public-lifecycle-smoke",
        progressStatus = "waiting-for-review",
        nextAction = "human-review",
        artifactCount = 1,
        artifacts = listOf(
            PetGenerationArtifactDto(
                downloadId = "artifact-1",
                kind = "candidate",
                status = "waiting-for-review",
                downloadUrl = "/pet-generation-jobs/public-lifecycle-smoke/artifacts/artifact-1"
            )
        )
    )

    override suspend fun createJob(
        request: PetGenerationJobCreateRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun getJob(appJobId: String): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Success(demoJob)

    override suspend fun getArtifacts(
        appJobId: String
    ): ApiCallResult<PetGenerationArtifactIndexResponseDto> =
        ApiCallResult.Success(
            PetGenerationArtifactIndexResponseDto(
                appJobId = appJobId,
                artifacts = demoJob.artifacts
            )
        )

    override suspend fun submitReviewDecision(
        appJobId: String,
        request: ReviewDecisionRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun downloadPackage(appJobId: String): ApiCallResult<ByteArray> =
        ApiCallResult.Failure("not_supported")

    override suspend fun getWorkerReadiness(): ApiCallResult<WorkerReadinessResponseDto> =
        ApiCallResult.Success(WorkerReadinessResponseDto(status = "ready"))

    override suspend fun getAppApiContract(): ApiCallResult<PetGenerationAppApiContractDto> =
        ApiCallResult.Success(PetGenerationAppApiContractDto(schema = "fantasy-pet.app-api-contract.v1"))
}

private class QueuedFantasyPetGenerationClient : FantasyPetGenerationClient {
    var pollCount: Int = 0
        private set

    private val queuedJob = PetGenerationJobResponseDto(
        schema = "fantasy-pet.app-job-response.v1",
        appJobId = "queued-job-ui",
        runId = "queued-job-ui",
        progressStatus = "queued",
        nextAction = "wait",
        artifactCount = 0
    )

    override suspend fun createJob(
        request: PetGenerationJobCreateRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Success(queuedJob)

    override suspend fun getJob(appJobId: String): ApiCallResult<PetGenerationJobResponseDto> {
        pollCount += 1
        return ApiCallResult.Success(queuedJob)
    }

    override suspend fun getArtifacts(
        appJobId: String
    ): ApiCallResult<PetGenerationArtifactIndexResponseDto> =
        ApiCallResult.Success(PetGenerationArtifactIndexResponseDto(appJobId = appJobId))

    override suspend fun submitReviewDecision(
        appJobId: String,
        request: ReviewDecisionRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun downloadPackage(appJobId: String): ApiCallResult<ByteArray> =
        ApiCallResult.Failure("not_supported")

    override suspend fun getWorkerReadiness(): ApiCallResult<WorkerReadinessResponseDto> =
        ApiCallResult.Success(WorkerReadinessResponseDto(status = "ready"))

    override suspend fun getAppApiContract(): ApiCallResult<PetGenerationAppApiContractDto> =
        ApiCallResult.Success(PetGenerationAppApiContractDto(schema = "fantasy-pet.app-api-contract.v1"))
}

private class RecordingFantasyPetGenerationClient : FantasyPetGenerationClient {
    var createdRequest: PetGenerationJobCreateRequestDto? = null
        private set
    var reviewRequest: ReviewDecisionRequestDto? = null
        private set
    var downloadedPackageJobId: String? = null
        private set

    override suspend fun createJob(
        request: PetGenerationJobCreateRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> {
        createdRequest = request
        return ApiCallResult.Success(reviewableJob)
    }

    override suspend fun getJob(appJobId: String): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Success(reviewableJob)

    override suspend fun getArtifacts(
        appJobId: String
    ): ApiCallResult<PetGenerationArtifactIndexResponseDto> =
        ApiCallResult.Success(
            PetGenerationArtifactIndexResponseDto(
                appJobId = appJobId,
                artifacts = reviewableJob.artifacts
            )
        )

    override suspend fun submitReviewDecision(
        appJobId: String,
        request: ReviewDecisionRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> {
        reviewRequest = request
        return ApiCallResult.Success(
            reviewableJob.copy(
                progressStatus = "ready-for-download",
                nextAction = "download-package",
                downloadReady = true
            )
        )
    }

    override suspend fun downloadPackage(appJobId: String): ApiCallResult<ByteArray> {
        downloadedPackageJobId = appJobId
        return ApiCallResult.Success(minimalFantasyPetPackageZip())
    }

    override suspend fun getWorkerReadiness(): ApiCallResult<WorkerReadinessResponseDto> =
        ApiCallResult.Success(WorkerReadinessResponseDto(status = "ready"))

    override suspend fun getAppApiContract(): ApiCallResult<PetGenerationAppApiContractDto> =
        ApiCallResult.Success(PetGenerationAppApiContractDto(schema = "fantasy-pet.app-api-contract.v1"))

    private val reviewableJob = PetGenerationJobResponseDto(
        schema = "fantasy-pet.app-job-response.v1",
        appJobId = "job-ui-001",
        progressStatus = "waiting-for-review",
        nextAction = "human-review",
        artifactCount = 1,
        artifacts = listOf(
            PetGenerationArtifactDto(
                downloadId = "candidate-1",
                kind = "candidate",
                status = "waiting-for-review",
                downloadUrl = "/pet-generation-jobs/job-ui-001/artifacts/candidate-1"
            )
        )
    )
}

private fun minimalFantasyPetPackageZip(): ByteArray {
    val manifestJson = """
        {
          "schema": "fantasy-pet.package-manifest.v1",
          "runId": "job-ui-001",
          "appJobId": "job-ui-001",
          "acceptedBy": "human-review",
          "sourceDownloadId": "candidate-1",
          "sourceTaskId": "codex-worker-task",
          "files": [
            { "kind": "candidate", "path": "candidates/candidate-1.png" }
          ]
        }
    """.trimIndent()
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        zip.putNextEntry(ZipEntry("package-manifest.json"))
        zip.write(manifestJson.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        zip.putNextEntry(ZipEntry("candidates/candidate-1.png"))
        zip.write(byteArrayOf(1, 2, 3))
        zip.closeEntry()
    }
    return output.toByteArray()
}
