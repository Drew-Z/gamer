package com.gamer.community.generation

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class PetGenerationJobCreateRequestDto(
    val schema: String = "fantasy-pet.app-job-create-request.v1",
    val description: String,
    val appJobId: String? = null,
    val bodyShape: String? = null,
    val references: List<String> = emptyList()
)

@Serializable
data class PetGenerationJobResponseDto(
    val schema: String = "",
    val appJobId: String = "",
    val runId: String = "",
    val status: String = "",
    val progressStatus: String = "",
    val qualityTarget: String = "",
    val nextAction: String = "",
    val requiresHumanReview: Boolean = true,
    val downloadReady: Boolean = false,
    val generationProgress: PetGenerationProgressDto = PetGenerationProgressDto(),
    val packageStatus: String = "",
    val packagePlanStatus: String = "",
    val artifactIndexStatus: String = "",
    val artifactCount: Int = 0,
    val artifacts: List<PetGenerationArtifactDto> = emptyList(),
    val links: PetGenerationLinksDto = PetGenerationLinksDto(),
    val errors: List<String> = emptyList()
)

@Serializable
data class PetGenerationProgressDto(
    val schema: String = "",
    val currentStage: String = "",
    val message: String = "",
    val steps: List<PetGenerationProgressStepDto> = emptyList(),
    val summary: PetGenerationProgressSummaryDto = PetGenerationProgressSummaryDto(),
    val security: PetGenerationProgressSecurityDto = PetGenerationProgressSecurityDto()
)

@Serializable
data class PetGenerationProgressStepDto(
    val id: String = "",
    val label: String = "",
    val status: String = "",
    val message: String = ""
)

@Serializable
data class PetGenerationProgressSummaryDto(
    val candidateCount: Int = 0,
    val reviewArtifactCount: Int = 0,
    val latestHumanDecision: String = "",
    val downloadReady: Boolean = false,
    val requiresHumanReview: Boolean = true
)

@Serializable
data class PetGenerationProgressSecurityDto(
    val exposesInternalPaths: Boolean = false,
    val exposesWorkerCommands: Boolean = false
)

@Serializable
data class PetGenerationArtifactDto(
    val downloadId: String = "",
    val kind: String = "",
    val status: String = "",
    val label: String = "",
    val agent: String = "",
    val taskId: String = "",
    val actionId: String = "",
    val reviewDecision: String = "",
    val reviewStatus: String = "",
    val reviewStage: String = "",
    val previewKind: String = "",
    val mediaType: String = "",
    val frameCount: Int = 0,
    val fps: Int = 0,
    val packageReady: Boolean = false,
    val downloadUrl: String = "",
    val errors: List<String> = emptyList()
)

@Serializable
data class PetGenerationArtifactIndexResponseDto(
    val schema: String = "",
    val appJobId: String = "",
    val runId: String = "",
    val artifacts: List<PetGenerationArtifactDto> = emptyList(),
    val errors: List<String> = emptyList()
)

@Serializable
data class PetGenerationLinksDto(
    val self: String = "",
    val artifacts: String = "",
    val reviewDecisions: String = "",
    val `package`: String = ""
)

@Serializable
data class ReviewDecisionRequestDto(
    val schema: String = "fantasy-pet.review-decision.v1",
    val decisionId: String,
    val reviewer: String = "human-review",
    val decision: String,
    val targetDownloadId: String,
    val stage: String = "human-review",
    val notes: List<String> = emptyList()
)

@Serializable
data class WorkerReadinessResponseDto(
    val schema: String = "",
    val status: String = "",
    val adapters: List<WorkerReadinessAdapterDto> = emptyList(),
    val security: WorkerReadinessSecurityDto = WorkerReadinessSecurityDto()
)

@Serializable
data class WorkerReadinessAdapterDto(
    val adapter: String = "",
    val configured: Boolean = false,
    val status: String = "",
    val checks: Map<String, String> = emptyMap()
)

@Serializable
data class WorkerReadinessSecurityDto(
    val secretsInReport: Boolean = false,
    val executesAgentProcesses: Boolean = false,
    val appMayInvokeAgentsDirectly: Boolean = false,
    val executesReadinessProbe: Boolean = false
)

@Serializable
data class PetGenerationAppApiContractDto(
    val schema: String = "",
    val publicEndpoints: List<PetGenerationPublicEndpointDto> = emptyList(),
    val adminEndpoints: List<PetGenerationPublicEndpointDto> = emptyList(),
    val security: PetGenerationAppApiContractSecurityDto = PetGenerationAppApiContractSecurityDto()
)

@Serializable
data class PetGenerationPublicEndpointDto(
    val method: String = "",
    val path: String = "",
    @SerialName("public")
    val isPublic: Boolean = true,
    val purpose: String = "",
    val requestSchema: String = "",
    val responseSchema: String = ""
)

@Serializable
data class PetGenerationAppApiContractSecurityDto(
    val exposesInternalPaths: Boolean = false,
    val exposesRawPrompt: Boolean = false,
    val exposesWorkerCommands: Boolean = false,
    val exposesSecrets: Boolean = false,
    val appMayInvokeAgentsDirectly: Boolean = false,
    val requiresHumanReview: Boolean = true,
    val adminEndpointsDisabledByDefault: Boolean = true
)
