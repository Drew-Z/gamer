package com.gamer.community.api

sealed interface ApiCallResult<out T> {
    data class Success<T>(val value: T) : ApiCallResult<T>
    data class Failure(val reason: String) : ApiCallResult<Nothing>
}

interface CommunityApiClient {
    suspend fun getFeed(): ApiCallResult<FeedResponseDto>
    suspend fun getWallet(): ApiCallResult<WalletDto>
    suspend fun getApprovedPets(): ApiCallResult<ApprovedPetsResponseDto>
    suspend fun claimDailyCheckIn(): ApiCallResult<CheckInResponseDto>
}
