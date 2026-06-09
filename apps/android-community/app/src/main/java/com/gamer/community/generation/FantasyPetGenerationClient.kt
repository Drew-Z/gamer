package com.gamer.community.generation

import com.gamer.community.api.ApiCallResult

interface FantasyPetGenerationClient {
    suspend fun createJob(
        request: PetGenerationJobCreateRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto>

    suspend fun getJob(appJobId: String): ApiCallResult<PetGenerationJobResponseDto>

    suspend fun getArtifacts(
        appJobId: String
    ): ApiCallResult<PetGenerationArtifactIndexResponseDto>

    suspend fun submitReviewDecision(
        appJobId: String,
        request: ReviewDecisionRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto>

    suspend fun downloadPackage(appJobId: String): ApiCallResult<ByteArray>

    suspend fun getWorkerReadiness(): ApiCallResult<WorkerReadinessResponseDto>

    suspend fun getAppApiContract(): ApiCallResult<PetGenerationAppApiContractDto>
}
