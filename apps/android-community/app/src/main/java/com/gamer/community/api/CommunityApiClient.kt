package com.gamer.community.api

sealed interface ApiCallResult<out T> {
    data class Success<T>(val value: T) : ApiCallResult<T>
    data class Failure(val reason: String) : ApiCallResult<Nothing>
}

interface CommunityApiClient {
    suspend fun getCommunityHome(): ApiCallResult<CommunityHomeResponseDto>
    suspend fun getCommunitySla(): ApiCallResult<CommunitySlaDto>
    suspend fun getFeed(): ApiCallResult<FeedResponseDto>
    suspend fun getWallet(): ApiCallResult<WalletDto>
    suspend fun getApprovedPets(): ApiCallResult<ApprovedPetsResponseDto>
    suspend fun getApprovedPetPackage(petId: String): ApiCallResult<ApprovedPetPackageDto>
    suspend fun createImportDraftFromFantasyPetPackage(
        request: FantasyPetPackageImportDraftRequestDto
    ): ApiCallResult<ImportDraftDto>
    suspend fun submitImportDraft(draftId: String): ApiCallResult<ImportDraftSubmissionResponseDto>
    suspend fun getSubmission(submissionId: String): ApiCallResult<SubmissionDto>
    suspend fun getSubmissions(): ApiCallResult<SubmissionsResponseDto>
    suspend fun claimDailyCheckIn(): ApiCallResult<CheckInResponseDto>
}
