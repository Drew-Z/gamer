package com.gamer.community.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals("Local fallback active.", result.message)
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
        assertEquals("Local fallback active.", result.message)
        assertEquals("Stardust dragon launch pose", result.posts[0].title)
        assertEquals(123, result.walletBalance)
    }

    @Test
    fun loadInitialCommunityFallsBackWhenRemoteFeedIsEmpty() = runTest {
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

        assertTrue(result.usedFallback)
        assertEquals("Local fallback active.", result.message)
        assertEquals("Stardust dragon launch pose", result.posts[0].title)
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
        assertEquals("Local check-in fallback active.", result.message)
        assertNull(result.walletBalance)
        assertEquals(10, result.rewardAmount)
        assertTrue(result.claimed)
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
    fun getSubmissionStatusFindsSubmissionFromPublicList() = runTest {
        val submission = SubmissionDto(
            id = "submission-local-001",
            petId = "public-lifecycle-smoke",
            userId = "user-demo-001",
            status = "pending",
            scoreReportId = "score-import-draft-local-001",
            importDraftId = "import-draft-local-001"
        )
        val fakeClient = FakeCommunityApiClient(
            submissionsResponse = ApiCallResult.Success(
                SubmissionsResponseDto(submissions = listOf(submission))
            )
        )
        val repository = CommunityRepository(client = fakeClient)

        val result = repository.getSubmissionStatus("submission-local-001")

        assertEquals(ApiCallResult.Success(submission), result)
        assertTrue(fakeClient.submissionsRequested)
    }

    @Test
    fun getSubmissionStatusRejectsUnsafeIdsBeforeCallingApi() = runTest {
        val fakeClient = FakeCommunityApiClient()
        val repository = CommunityRepository(client = fakeClient)

        val result = repository.getSubmissionStatus("D:/secret/runs/submission.json")

        assertEquals(ApiCallResult.Failure("submission_id_required"), result)
        assertFalse(fakeClient.submissionsRequested)
    }
}

private class FakeCommunityApiClient(
    private val feedResponse: ApiCallResult<FeedResponseDto> = ApiCallResult.Failure("not_configured"),
    private val walletResponse: ApiCallResult<WalletDto> = ApiCallResult.Failure("not_configured"),
    private val approvedPetsResponse: ApiCallResult<ApprovedPetsResponseDto> = ApiCallResult.Failure("not_configured"),
    private val checkInResponse: ApiCallResult<CheckInResponseDto> = ApiCallResult.Failure("not_configured"),
    private val importDraftResponse: ApiCallResult<ImportDraftDto> = ApiCallResult.Failure("not_configured"),
    private val submitImportDraftResponse: ApiCallResult<ImportDraftSubmissionResponseDto> =
        ApiCallResult.Failure("not_configured"),
    private val submissionsResponse: ApiCallResult<SubmissionsResponseDto> =
        ApiCallResult.Failure("not_configured")
) : CommunityApiClient {
    var importDraftRequest: FantasyPetPackageImportDraftRequestDto? = null
    var submittedImportDraftId: String? = null
    var submissionsRequested: Boolean = false

    override suspend fun getFeed(): ApiCallResult<FeedResponseDto> = feedResponse

    override suspend fun getWallet(): ApiCallResult<WalletDto> = walletResponse

    override suspend fun getApprovedPets(): ApiCallResult<ApprovedPetsResponseDto> =
        approvedPetsResponse

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

    override suspend fun getSubmissions(): ApiCallResult<SubmissionsResponseDto> =
        submissionsResponse.also {
            submissionsRequested = true
        }

    override suspend fun claimDailyCheckIn(): ApiCallResult<CheckInResponseDto> = checkInResponse
}
