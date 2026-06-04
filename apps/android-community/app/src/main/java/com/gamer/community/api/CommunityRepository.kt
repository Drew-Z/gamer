package com.gamer.community.api

import com.gamer.community.petshell.FeedPost
import com.gamer.community.petshell.fixtureFeedPosts

data class InitialCommunityResult(
    val posts: List<FeedPost>,
    val walletBalance: Int,
    val message: String,
    val usedFallback: Boolean
)

data class CheckInResult(
    val walletBalance: Int,
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

        return when {
            feedResult is ApiCallResult.Success && walletResult is ApiCallResult.Success ->
                InitialCommunityResult(
                    posts = feedResult.value.toFeedPosts(),
                    walletBalance = walletResult.value.balance,
                    message = "Community ready.",
                    usedFallback = false
                )
            else ->
                InitialCommunityResult(
                    posts = fixtureFeedPosts,
                    walletBalance = FALLBACK_WALLET_BALANCE,
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
                    walletBalance = FALLBACK_WALLET_BALANCE,
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
