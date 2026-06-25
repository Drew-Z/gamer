package com.gamer.community.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityRepositoryTest {
    @Test
    fun loadInitialCommunityPrefersCommunityHomeSummary() = runTest {
        val fakeClient = FakeCommunityApiClient(
            communitySlaResponse = ApiCallResult.Success(
                CommunitySlaDto(
                    hatch = CommunitySlaHatchDto(customHatchMaxMs = 777_000),
                    polling = CommunitySlaPollingDto(suggestedIntervalMs = 5_000),
                    failureThresholds = CommunitySlaFailureThresholdsDto(
                        consecutivePollFailuresBeforeSlowNotice = 2
                    )
                )
            ),
            communityHomeResponse = ApiCallResult.Success(
                CommunityHomeResponseDto(
                    schema = "gamer.community-home.v1",
                    userId = "user-demo-001",
                    feed = FeedResponseDto(
                        items = listOf(
                            FeedPostDto(
                                id = "post-home-001",
                                authorId = "user-demo-001",
                                petId = "pet-home-001",
                                title = "Home feed",
                                body = "Loaded from community home.",
                                reactionCount = 22,
                                createdAt = "2026-06-09T00:00:00.000Z"
                            )
                        )
                    ),
                    wallet = WalletDto(
                        userId = "user-demo-001",
                        balance = 144,
                        currencyCode = "petcoin"
                    ),
                    approvedPets = ApprovedPetsResponseDto(
                        items = listOf(
                            ApprovedPetDto(
                                petId = "pet-home-approved-001",
                                displayName = "Home Pet",
                                ownerUserId = "user-demo-001",
                                source = ApprovedPetSourceDto(kind = "fantasy-pet-rule"),
                                assets = ApprovedPetAssetsDto(previewPath = "previews/home.png"),
                                totalScore = 88
                            )
                        )
                    ),
                    dailyCheckIn = CommunityHomeDailyCheckInDto(
                        date = "2026-06-09",
                        claimed = true,
                        rewardAmount = 10,
                        ledgerEntryId = "ledger-checkin-2026-06-09"
                    ),
                    submissionsSummary = CommunityHomeSubmissionsSummaryDto(
                        pendingCount = 2,
                        approvedCount = 1,
                        latest = SubmissionDto(
                            id = "submission-home-001",
                            petId = "pet-home-001",
                            userId = "user-demo-001",
                            status = "pending"
                        )
                    )
                )
            )
        )
        val repository = CommunityRepository(client = fakeClient)

        val result = repository.loadInitialCommunity()

        assertFalse(result.usedFallback)
        assertEquals("Community home ready.", result.message)
        assertEquals("Home feed", result.posts[0].title)
        assertEquals(144, result.walletBalance)
        assertEquals(true, result.checkInClaimed)
        assertEquals(2, result.pendingSubmissionCount)
        assertEquals("submission-home-001", result.latestSubmission?.id)
        assertEquals("pending", result.latestSubmission?.status)
        assertEquals("pet-home-001", result.latestSubmission?.petId)
        assertEquals(777_000L, result.hatchSla.customHatchMaxMs)
        assertEquals(5_000L, result.hatchSla.suggestedPollIntervalMs)
        assertEquals(2, result.hatchSla.consecutivePollFailuresBeforeSlowNotice)
        assertEquals("Home Pet", result.approvedPets[0].displayName)
        assertFalse(fakeClient.feedRequested)
        assertFalse(fakeClient.walletRequested)
        assertFalse(fakeClient.approvedPetsRequested)
    }

    @Test
    fun loadInitialCommunityHidesUnsafeLatestSubmissionSummary() = runTest {
        val repository = CommunityRepository(
            client = FakeCommunityApiClient(
                communityHomeResponse = ApiCallResult.Success(
                    CommunityHomeResponseDto(
                        schema = "gamer.community-home.v1",
                        userId = "user-demo-001",
                        wallet = WalletDto(
                            userId = "user-demo-001",
                            balance = 144,
                            currencyCode = "petcoin"
                        ),
                        submissionsSummary = CommunityHomeSubmissionsSummaryDto(
                            pendingCount = 1,
                            latest = SubmissionDto(
                                id = "runs/job/submission.json",
                                petId = "pet-home-001",
                                userId = "user-demo-001",
                                status = "pending"
                            )
                        )
                    )
                )
            )
        )

        val result = repository.loadInitialCommunity()

        assertEquals(1, result.pendingSubmissionCount)
        assertNull(result.latestSubmission)
    }

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
                ),
                approvedPetsResponse = ApiCallResult.Success(
                    ApprovedPetsResponseDto(
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
                )
            )
        )

        val result = repository.loadInitialCommunity()

        assertFalse(result.usedFallback)
        assertEquals(1, result.posts.size)
        assertEquals("Live feed", result.posts[0].title)
        assertEquals(123, result.walletBalance)
        assertEquals(1, result.approvedPets.size)
        assertEquals("Stardust Dragon", result.approvedPets[0].displayName)
    }

    @Test
    fun loadInitialCommunityReturnsEmptyStateWhenRemoteFails() = runTest {
        val repository = CommunityRepository(
            client = FakeCommunityApiClient(
                feedResponse = ApiCallResult.Failure("network_down"),
                walletResponse = ApiCallResult.Failure("network_down")
            )
        )

        val result = repository.loadInitialCommunity()

        assertTrue(result.usedFallback)
        assertEquals("Remote community unavailable.", result.message)
        assertTrue(result.posts.isEmpty())
        assertEquals(90, result.walletBalance)
        assertTrue(result.approvedPets.isEmpty())
    }

    @Test
    fun loadInitialCommunityFallsBackButKeepsRemotePostsWhenWalletFails() = runTest {
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
                walletResponse = ApiCallResult.Failure("network_down")
            )
        )

        val result = repository.loadInitialCommunity()

        assertTrue(result.usedFallback)
        assertEquals("Remote community unavailable.", result.message)
        assertEquals(1, result.posts.size)
        assertEquals("Live feed", result.posts[0].title)
        assertEquals(90, result.walletBalance)
    }

    @Test
    fun loadInitialCommunityFallsBackButKeepsRemoteWalletWhenFeedFails() = runTest {
        val repository = CommunityRepository(
            client = FakeCommunityApiClient(
                feedResponse = ApiCallResult.Failure("network_down"),
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

        assertTrue(result.usedFallback)
        assertEquals("Remote community unavailable.", result.message)
        assertTrue(result.posts.isEmpty())
        assertEquals(123, result.walletBalance)
    }

    @Test
    fun loadInitialCommunityKeepsEmptyFeedWhenRemoteFeedIsEmpty() = runTest {
        val repository = CommunityRepository(
            client = FakeCommunityApiClient(
                feedResponse = ApiCallResult.Success(FeedResponseDto(items = emptyList())),
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
        assertEquals("Community ready.", result.message)
        assertTrue(result.posts.isEmpty())
        assertEquals(123, result.walletBalance)
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
        assertEquals("Remote check-in unavailable.", result.message)
        assertNull(result.walletBalance)
        assertEquals(0, result.rewardAmount)
        assertFalse(result.claimed)
    }

    @Test
    fun createImportDraftFromFantasyPetPackageUsesPublicCommunityEndpoint() = runTest {
        val request = FantasyPetPackageImportDraftRequestDto(
            packageManifest = FantasyPetPackageManifestDto(
                runId = "run-public-lifecycle-smoke",
                appJobId = "public-lifecycle-smoke",
                acceptedBy = "human-review",
                sourceDownloadId = "artifact-1",
                files = listOf(
                    FantasyPetPackageFileDto(
                        kind = "candidate",
                        path = "artifacts/candidates/final-preview.png"
                    )
                )
            ),
            packageFileName = "pet-public-lifecycle-smoke.zip",
            packageByteCount = 664L,
            targetDownloadId = "artifact-1"
        )
        val fakeClient = FakeCommunityApiClient(
            importDraftResponse = ApiCallResult.Success(
                ImportDraftDto(
                    id = "import-draft-local-001",
                    userId = "user-demo-001",
                    status = "ready",
                    petId = "public-lifecycle-smoke",
                    scoreReportId = "score-import-draft-local-001"
                )
            )
        )
        val repository = CommunityRepository(client = fakeClient)

        val result = repository.createImportDraftFromFantasyPetPackage(request)

        assertTrue(result is ApiCallResult.Success<*>)
        assertEquals(request, fakeClient.importDraftRequest)
        val draft = (result as ApiCallResult.Success<ImportDraftDto>).value
        assertEquals("import-draft-local-001", draft.id)
        assertEquals("public-lifecycle-smoke", draft.petId)
    }

    @Test
    fun submitReadyImportDraftUsesPublicCommunitySubmitEndpoint() = runTest {
        val draft = ImportDraftDto(
            id = "import-draft-local-001",
            userId = "user-demo-001",
            status = "ready",
            petId = "public-lifecycle-smoke",
            ownershipClaimId = "claim-public-lifecycle-smoke",
            scoreReportId = "score-import-draft-local-001"
        )
        val response = ImportDraftSubmissionResponseDto(
            draft = draft.copy(status = "submitted", submissionId = "submission-local-001"),
            submission = SubmissionDto(
                id = "submission-local-001",
                petId = "public-lifecycle-smoke",
                userId = "user-demo-001",
                status = "pending",
                scoreReportId = "score-import-draft-local-001",
                ownershipClaimId = "claim-public-lifecycle-smoke",
                importDraftId = "import-draft-local-001",
                submittedAt = "2026-06-08T00:00:00.000Z"
            )
        )
        val fakeClient = FakeCommunityApiClient(
            submitImportDraftResponse = ApiCallResult.Success(response)
        )
        val repository = CommunityRepository(client = fakeClient)

        val result = repository.submitImportDraftToCommunity(draft)

        assertEquals(ApiCallResult.Success(response), result)
        assertEquals("import-draft-local-001", fakeClient.submittedImportDraftId)
    }

    @Test
    fun submitImportDraftRejectsNonReadyDraftsBeforeCallingApi() = runTest {
        val fakeClient = FakeCommunityApiClient()
        val repository = CommunityRepository(client = fakeClient)

        val result = repository.submitImportDraftToCommunity(
            ImportDraftDto(
                id = "import-draft-local-001",
                userId = "user-demo-001",
                status = "submitted",
                petId = "public-lifecycle-smoke"
            )
        )

        assertEquals(ApiCallResult.Failure("import_draft_not_ready"), result)
        assertEquals(null, fakeClient.submittedImportDraftId)
    }

    @Test
    fun getSubmissionStatusFetchesSinglePublicSubmission() = runTest {
        val submission = SubmissionDto(
            id = "submission-local-001",
            petId = "public-lifecycle-smoke",
            userId = "user-demo-001",
            status = "pending",
            scoreReportId = "score-import-draft-local-001",
            importDraftId = "import-draft-local-001"
        )
        val fakeClient = FakeCommunityApiClient(
            submissionResponse = ApiCallResult.Success(submission)
        )
        val repository = CommunityRepository(client = fakeClient)

        val result = repository.getSubmissionStatus("submission-local-001")

        assertEquals(ApiCallResult.Success(submission), result)
        assertEquals("submission-local-001", fakeClient.requestedSubmissionId)
        assertFalse(fakeClient.submissionsRequested)
    }

    @Test
    fun getSubmissionStatusRejectsUnsafeIdsBeforeCallingApi() = runTest {
        val fakeClient = FakeCommunityApiClient()
        val repository = CommunityRepository(client = fakeClient)

        val result = repository.getSubmissionStatus("D:/secret/runs/submission.json")

        assertEquals(ApiCallResult.Failure("submission_id_required"), result)
        assertNull(fakeClient.requestedSubmissionId)
        assertFalse(fakeClient.submissionsRequested)
    }
}

private class FakeCommunityApiClient(
    private val communityHomeResponse: ApiCallResult<CommunityHomeResponseDto> =
        ApiCallResult.Failure("not_configured"),
    private val communitySlaResponse: ApiCallResult<CommunitySlaDto> =
        ApiCallResult.Failure("not_configured"),
    private val feedResponse: ApiCallResult<FeedResponseDto> = ApiCallResult.Failure("not_configured"),
    private val walletResponse: ApiCallResult<WalletDto> = ApiCallResult.Failure("not_configured"),
    private val approvedPetsResponse: ApiCallResult<ApprovedPetsResponseDto> = ApiCallResult.Failure("not_configured"),
    private val checkInResponse: ApiCallResult<CheckInResponseDto> = ApiCallResult.Failure("not_configured"),
    private val importDraftResponse: ApiCallResult<ImportDraftDto> = ApiCallResult.Failure("not_configured"),
    private val submitImportDraftResponse: ApiCallResult<ImportDraftSubmissionResponseDto> =
        ApiCallResult.Failure("not_configured"),
    private val submissionResponse: ApiCallResult<SubmissionDto> =
        ApiCallResult.Failure("not_configured"),
    private val submissionsResponse: ApiCallResult<SubmissionsResponseDto> =
        ApiCallResult.Failure("not_configured")
) : CommunityApiClient {
    var importDraftRequest: FantasyPetPackageImportDraftRequestDto? = null
    var submittedImportDraftId: String? = null
    var requestedSubmissionId: String? = null
    var submissionsRequested: Boolean = false
    var feedRequested: Boolean = false
    var walletRequested: Boolean = false
    var approvedPetsRequested: Boolean = false

    override suspend fun getCommunityHome(): ApiCallResult<CommunityHomeResponseDto> =
        communityHomeResponse

    override suspend fun getCommunitySla(): ApiCallResult<CommunitySlaDto> =
        communitySlaResponse

    override suspend fun getFeed(): ApiCallResult<FeedResponseDto> =
        feedResponse.also {
            feedRequested = true
        }

    override suspend fun getWallet(): ApiCallResult<WalletDto> =
        walletResponse.also {
            walletRequested = true
        }

    override suspend fun getApprovedPets(): ApiCallResult<ApprovedPetsResponseDto> =
        approvedPetsResponse.also {
            approvedPetsRequested = true
        }

    override suspend fun getApprovedPetPackage(petId: String): ApiCallResult<ApprovedPetPackageDto> =
        ApiCallResult.Failure("not_configured")

    override suspend fun createImportDraftFromFantasyPetPackage(
        request: FantasyPetPackageImportDraftRequestDto
    ): ApiCallResult<ImportDraftDto> =
        importDraftResponse.also {
            importDraftRequest = request
        }

    override suspend fun submitImportDraft(
        draftId: String
    ): ApiCallResult<ImportDraftSubmissionResponseDto> =
        submitImportDraftResponse.also {
            submittedImportDraftId = draftId
        }

    override suspend fun getSubmission(submissionId: String): ApiCallResult<SubmissionDto> =
        submissionResponse.also {
            requestedSubmissionId = submissionId
        }

    override suspend fun getSubmissions(): ApiCallResult<SubmissionsResponseDto> =
        submissionsResponse.also {
            submissionsRequested = true
        }

    override suspend fun claimDailyCheckIn(): ApiCallResult<CheckInResponseDto> = checkInResponse
}
