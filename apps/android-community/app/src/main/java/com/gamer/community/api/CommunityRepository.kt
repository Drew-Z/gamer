package com.gamer.community.api

import com.gamer.community.petshell.ApprovedPet
import com.gamer.community.petshell.FeedPost
import com.gamer.community.petshell.fixtureFeedPosts

data class InitialCommunityResult(
    val posts: List<FeedPost>,
    val approvedPets: List<ApprovedPet>,
    val walletBalance: Int,
    val message: String,
    val usedFallback: Boolean
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
        val hasRemotePosts = remotePosts.isNotEmpty()
        val hasRemoteWallet = walletResult is ApiCallResult.Success

        return if (hasRemotePosts && hasRemoteWallet) {
                InitialCommunityResult(
                    posts = remotePosts,
                    approvedPets = approvedPets,
                    walletBalance = walletBalance,
                    message = "Community ready.",
                    usedFallback = false
                )
        } else {
                InitialCommunityResult(
                    posts = remotePosts.ifEmpty { fixtureFeedPosts },
                    approvedPets = approvedPets,
                    walletBalance = walletBalance,
                    message = "Local fallback active.",
                    usedFallback = true
                )
        }
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
                    rewardAmount = FALLBACK_CHECK_IN_REWARD,
                    claimed = true,
                    message = "Local check-in fallback active.",
                    usedFallback = true
                )
        }

    private companion object {
        const val FALLBACK_WALLET_BALANCE = 90
        const val FALLBACK_CHECK_IN_REWARD = 10
    }
}
