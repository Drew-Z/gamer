package com.gamer.community.api

import com.gamer.community.petshell.ApprovedPet
import com.gamer.community.petshell.HatchSla
import com.gamer.community.petshell.FeedPost

data class InitialCommunityResult(
    val posts: List<FeedPost>,
    val approvedPets: List<ApprovedPet>,
    val walletBalance: Int,
    val message: String,
    val usedFallback: Boolean,
    val checkInClaimed: Boolean = false,
    val pendingSubmissionCount: Int = 0,
    val hatchSla: HatchSla = HatchSla()
)

data class CheckInResult(
    val walletBalance: Int?,
    val rewardAmount: Int,
    val claimed: Boolean,
    val message: String,
    val usedFallback: Boolean
)

class CommunityRepository(
    private val client: CommunityApiClient
) {
    suspend fun loadInitialCommunity(): InitialCommunityResult {
        val hatchSla = loadHatchSla()

        when (val homeResult = client.getCommunityHome()) {
            is ApiCallResult.Success -> {
                return homeResult.value.toInitialCommunityResult(hatchSla)
            }
            is ApiCallResult.Failure -> Unit
        }

        val feedResult = client.getFeed()
        val walletResult = client.getWallet()
        val approvedPetsResult = client.getApprovedPets()
        val remotePosts = when (feedResult) {
            is ApiCallResult.Success -> feedResult.value.toFeedPosts()
            is ApiCallResult.Failure -> emptyList()
        }
        val approvedPets = when (approvedPetsResult) {
            is ApiCallResult.Success -> approvedPetsResult.value.toApprovedPets()
            is ApiCallResult.Failure -> emptyList()
        }
        val walletBalance = when (walletResult) {
            is ApiCallResult.Success -> walletResult.value.balance
            is ApiCallResult.Failure -> FALLBACK_WALLET_BALANCE
        }
        val hasRemoteFeed = feedResult is ApiCallResult.Success
        val hasRemoteWallet = walletResult is ApiCallResult.Success

        return if (hasRemoteFeed && hasRemoteWallet) {
                InitialCommunityResult(
                    posts = remotePosts,
                    approvedPets = approvedPets,
                    walletBalance = walletBalance,
                    message = "Community ready.",
                    usedFallback = false,
                    hatchSla = hatchSla
                )
        } else {
                InitialCommunityResult(
                    posts = remotePosts,
                    approvedPets = approvedPets,
                    walletBalance = walletBalance,
                    message = "Remote community unavailable.",
                    usedFallback = true,
                    hatchSla = hatchSla
                )
        }
    }

    private suspend fun loadHatchSla(): HatchSla =
        when (val result = client.getCommunitySla()) {
            is ApiCallResult.Success -> result.value.toHatchSla()
            is ApiCallResult.Failure -> HatchSla()
        }

    suspend fun claimDailyCheckIn(): CheckInResult =
        when (val checkInResult = client.claimDailyCheckIn()) {
            is ApiCallResult.Success -> {
                val response = checkInResult.value
                CheckInResult(
                    walletBalance = response.wallet.balance,
                    rewardAmount = response.checkIn.rewardAmount,
                    claimed = response.checkIn.claimed,
                    message = "Daily reward claimed: +${response.checkIn.rewardAmount} petcoin.",
                    usedFallback = false
                )
            }
            is ApiCallResult.Failure ->
                CheckInResult(
                    walletBalance = null,
                    rewardAmount = 0,
                    claimed = false,
                    message = "Remote check-in unavailable.",
                    usedFallback = true
                )
        }

    suspend fun createImportDraftFromFantasyPetPackage(
        request: FantasyPetPackageImportDraftRequestDto
    ): ApiCallResult<ImportDraftDto> =
        client.createImportDraftFromFantasyPetPackage(request)

    suspend fun submitImportDraftToCommunity(
        draft: ImportDraftDto
    ): ApiCallResult<ImportDraftSubmissionResponseDto> {
        val draftId = draft.id.trim()
        if (!draftId.isSafePublicToken()) {
            return ApiCallResult.Failure("import_draft_id_required")
        }
        if (draft.status != "ready") {
            return ApiCallResult.Failure("import_draft_not_ready")
        }

        return client.submitImportDraft(draftId)
    }

    suspend fun getSubmissionStatus(submissionId: String): ApiCallResult<SubmissionDto> {
        val safeSubmissionId = submissionId.trim()
        if (!safeSubmissionId.isSafePublicToken()) {
            return ApiCallResult.Failure("submission_id_required")
        }

        return client.getSubmission(safeSubmissionId)
    }

    private companion object {
        const val FALLBACK_WALLET_BALANCE = 90
    }
}

private fun CommunityHomeResponseDto.toInitialCommunityResult(hatchSla: HatchSla): InitialCommunityResult {
    val remotePosts = feed.toFeedPosts()
    return InitialCommunityResult(
        posts = remotePosts,
        approvedPets = approvedPets.toApprovedPets(),
        walletBalance = wallet.balance,
        message = "Community home ready.",
        usedFallback = false,
        checkInClaimed = dailyCheckIn.claimed,
        pendingSubmissionCount = submissionsSummary.pendingCount,
        hatchSla = hatchSla
    )
}

private fun CommunitySlaDto.toHatchSla(): HatchSla =
    HatchSla(
        reserveEggMaxMs = hatch.reserveEggMaxMs,
        mysteryEggMaxMs = hatch.mysteryEggMaxMs,
        customHatchMaxMs = hatch.customHatchMaxMs,
        suggestedPollIntervalMs = polling.suggestedIntervalMs,
        consecutivePollFailuresBeforeSlowNotice =
            failureThresholds.consecutivePollFailuresBeforeSlowNotice
    )

private fun String.isSafePublicToken(): Boolean {
    val trimmed = trim()
    val lower = trimmed.lowercase()
    return trimmed.isNotBlank() &&
        !lower.startsWith("file:") &&
        !Regex("^[A-Za-z]:[\\\\/]").containsMatchIn(trimmed) &&
        !trimmed.contains("/") &&
        !trimmed.contains("\\") &&
        !trimmed.contains(":")
}
