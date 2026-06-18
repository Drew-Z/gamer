package com.gamer.community.generation

import com.gamer.community.api.ApiCallResult
import com.gamer.community.api.ImportDraftDto
import com.gamer.community.api.ImportDraftSubmissionResponseDto
import com.gamer.community.api.InitialCommunityResult
import com.gamer.community.api.SubmissionDto
import java.io.File
import java.net.URI
import java.net.URLEncoder

data class CandidateGalleryItem(
    val targetDownloadId: String,
    val previewUrl: String,
    val title: String,
    val status: String,
    val reviewed: Boolean = false,
    val actionId: String = ""
)

data class GenerationProgressStepItem(
    val label: String,
    val status: String,
    val message: String
)

data class PetGenerationPackageImportCandidate(
    val appJobId: String,
    val targetDownloadId: String,
    val packageFileName: String,
    val packageByteCount: Long,
    val status: String,
    val summary: String
)

val GENERATION_BODY_SHAPE_OPTIONS = listOf("balanced", "wide", "wide-tail", "tall")
const val DEFAULT_GENERATION_MESSAGE = "Describe a desktop pet to start generation."
const val CONTRACT_DEMO_PROGRESS_MESSAGE = "Contract demo fixture loaded; no live generation worker has run."
private const val MAX_REFERENCE_URLS = 8

data class ClearedGenerationJobUiState(
    val appJobId: String,
    val selectedCandidateDownloadId: String,
    val reviewNotes: String,
    val message: String
)

fun clearedGenerationJobUiState(): ClearedGenerationJobUiState =
    ClearedGenerationJobUiState(
        appJobId = "",
        selectedCandidateDownloadId = "",
        reviewNotes = "",
        message = DEFAULT_GENERATION_MESSAGE
    )

class FantasyPetGenerationService(
    private val client: FantasyPetGenerationClient,
    private val apiBaseUrl: String = "http://127.0.0.1:8765"
) {
    suspend fun createJob(
        description: String,
        appJobId: String = "",
        bodyShape: String = "",
        referencesText: String = ""
    ): ApiCallResult<PetGenerationJobResponseDto> {
        val validationFailure = generationCreateValidationFailureReason(
            description = description,
            bodyShape = bodyShape,
            referencesText = referencesText,
            appJobId = appJobId
        )
        if (validationFailure.isNotBlank()) {
            return ApiCallResult.Failure(validationFailure)
        }

        val trimmedDescription = description.trim()
        val normalizedBodyShape = bodyShape.trim().ifBlank { null }
        val references = parseReferences(referencesText)

        return client.createJob(
            PetGenerationJobCreateRequestDto(
                description = trimmedDescription,
                appJobId = appJobId.trim().ifBlank { null },
                bodyShape = normalizedBodyShape,
                references = references
            )
        )
    }

    suspend fun pollJob(appJobId: String): ApiCallResult<PetGenerationJobResponseDto> {
        val validationFailure = pollGenerationJobValidationFailureReason(appJobId)
        if (validationFailure.isNotBlank()) {
            return ApiCallResult.Failure(validationFailure)
        }
        return client.getJob(appJobId.trim())
    }

    suspend fun checkWorkerReadiness(): ApiCallResult<WorkerReadinessResponseDto> =
        client.getWorkerReadiness()

    suspend fun checkAppApiContract(): ApiCallResult<PetGenerationAppApiContractDto> =
        client.getAppApiContract()

    suspend fun checkGenerationServiceStatusMessage(): String {
        val contractMessage = when (val contractResult = checkAppApiContract()) {
            is ApiCallResult.Success -> appApiContractMessage(contractResult.value)
            is ApiCallResult.Failure -> "Generation API contract check failed: ${contractResult.reason}"
        }
        if (contractMessage.contains("blocked", ignoreCase = true)) {
            return contractMessage
        }

        return when (val readinessResult = checkWorkerReadiness()) {
            is ApiCallResult.Success -> "$contractMessage ${workerReadinessMessage(readinessResult.value)}"
            is ApiCallResult.Failure -> "$contractMessage Generation service check failed: ${readinessResult.reason}"
        }
    }

    suspend fun refreshJobArtifacts(
        job: PetGenerationJobResponseDto
    ): ApiCallResult<PetGenerationJobResponseDto> {
        if (!shouldFetchArtifacts(job)) {
            return ApiCallResult.Success(job)
        }

        return when (val result = client.getArtifacts(job.appJobId)) {
            is ApiCallResult.Success -> ApiCallResult.Success(
                job.copy(artifacts = result.value.artifacts)
            )
            is ApiCallResult.Failure -> result
        }
    }

    suspend fun submitReviewDecision(
        appJobId: String,
        targetDownloadId: String,
        decision: String,
        notesText: String
    ): ApiCallResult<PetGenerationJobResponseDto> {
        if (isContractDemoGenerationJobId(appJobId)) {
            return ApiCallResult.Failure("contract_demo_job_review_disabled")
        }

        val normalizedDecision = decision.trim()
        if (normalizedDecision !in ALLOWED_REVIEW_DECISIONS) {
            return ApiCallResult.Failure("invalid_review_decision")
        }
        val normalizedTarget = targetDownloadId.trim()
        if (normalizedTarget.isBlank()) {
            return ApiCallResult.Failure("target_download_id_required")
        }
        val notes = parseNotes(notesText)
        val reviewNotesFailure = reviewNotesValidationFailureReason(normalizedDecision, notesText)
        if (reviewNotesFailure.isNotBlank()) {
            return ApiCallResult.Failure(reviewNotesFailure)
        }

        val request = ReviewDecisionRequestDto(
            decisionId = "decision-${appJobId.pathToken()}-${normalizedDecision}-${normalizedTarget.pathToken()}",
            decision = normalizedDecision,
            targetDownloadId = normalizedTarget,
            notes = notes.ifEmpty {
                listOf("User visually accepted this candidate in the app.")
            }
        )

        return client.submitReviewDecision(appJobId, request)
    }

    suspend fun submitReviewDecisionForJob(
        job: PetGenerationJobResponseDto,
        targetDownloadId: String,
        decision: String,
        notesText: String
    ): ApiCallResult<PetGenerationJobResponseDto> {
        if (isContractDemoGenerationJob(job)) {
            return ApiCallResult.Failure("contract_demo_job_review_disabled")
        }

        val normalizedTarget = targetDownloadId.trim()
        val targetArtifact = job.artifacts.firstOrNull { artifact ->
            artifact.downloadId == normalizedTarget
        }
        if (targetArtifact?.isReviewableCandidateArtifact() != true) {
            return ApiCallResult.Failure("review_target_must_be_candidate")
        }
        if (targetArtifact.reviewDecision.isNotBlank()) {
            return ApiCallResult.Failure("review_target_already_decided")
        }

        return submitReviewDecision(
            appJobId = job.appJobId,
            targetDownloadId = normalizedTarget,
            decision = decision,
            notesText = notesText
        )
    }

    suspend fun downloadPackage(job: PetGenerationJobResponseDto): ApiCallResult<ByteArray> {
        if (!canShowPackageDownload(job)) {
            return ApiCallResult.Failure("package_not_ready")
        }
        return client.downloadPackage(job.appJobId)
    }

    suspend fun downloadPackageToFile(
        job: PetGenerationJobResponseDto,
        outputDirectory: File
    ): ApiCallResult<File> {
        val packageBytes = when (val result = downloadPackage(job)) {
            is ApiCallResult.Success -> result.value
            is ApiCallResult.Failure -> return result
        }

        return try {
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                return ApiCallResult.Failure("package_output_directory_unavailable")
            }
            if (!outputDirectory.isDirectory) {
                return ApiCallResult.Failure("package_output_directory_unavailable")
            }

            val outputFile = File(outputDirectory, "pet-${job.appJobId.pathToken()}.zip")
            outputFile.writeBytes(packageBytes)
            ApiCallResult.Success(outputFile)
        } catch (error: Exception) {
            ApiCallResult.Failure(error.message ?: error::class.java.simpleName)
        }
    }

    fun packageImportCandidate(
        job: PetGenerationJobResponseDto,
        selectedCandidateDownloadId: String,
        packageFileName: String,
        packageByteCount: Long
    ): PetGenerationPackageImportCandidate? {
        if (!canShowPackageDownload(job) || packageByteCount <= 0L) {
            return null
        }

        val safeAppJobId = job.appJobId.trim()
            .takeIf { it.isSafePackageDownloadDisplayText() }
            ?: return null
        val safeTargetDownloadId = selectedCandidateDownloadId.trim()
            .takeIf { it.isOpaqueCandidateDownloadIdForUi() }
            ?: return null
        val safePackageFileName = packageFileName.trim()
            .takeIf { it.endsWith(".zip", ignoreCase = true) }
            ?.takeIf { it.isSafePackageDownloadDisplayText() }
            ?: return null

        return PetGenerationPackageImportCandidate(
            appJobId = safeAppJobId,
            targetDownloadId = safeTargetDownloadId,
            packageFileName = safePackageFileName,
            packageByteCount = packageByteCount,
            status = "waiting-for-community-import",
            summary = "Package downloaded; preparing community import draft."
        )
    }

    fun candidateGalleryItems(job: PetGenerationJobResponseDto): List<CandidateGalleryItem> =
        job.artifacts
            .filter { it.isReviewableCandidateArtifact() }
            .mapIndexed { index, artifact ->
                CandidateGalleryItem(
                    targetDownloadId = artifact.downloadId,
                    previewUrl = publicPreviewUrl(job.appJobId, artifact),
                    title = "Candidate ${index + 1}",
                    status = safeDisplayText(artifact.status.ifBlank { "waiting-for-review" }),
                    reviewed = artifact.reviewDecision.isNotBlank(),
                    actionId = artifact.actionId.publicCandidateActionId()
                )
            }

    fun generationProgressMessage(job: PetGenerationJobResponseDto): String {
        val progressStatus = effectiveProgressStatus(job)
        if (job.generationProgress.security.hasUnsafeProgressBoundary()) {
            return "Generation status blocked: unsafe progress report."
        }
        if (isContractDemoGenerationJob(job)) {
            return CONTRACT_DEMO_PROGRESS_MESSAGE
        }

        val safeErrors = job.errors
            .map { safeDisplayText(it).trim() }
            .filter { it.isNotBlank() }
        if (progressStatus == "failed" && safeErrors.isNotEmpty()) {
            return "Failed: ${safeErrors.joinToString("; ")}"
        }

        val progressMessage = safeProgressMessage(job.generationProgress.message).trim()
        return progressMessage.ifBlank {
            generationStatusLabel(progressStatus, job.nextAction)
        }
    }

    fun generationProgressStepItems(job: PetGenerationJobResponseDto): List<GenerationProgressStepItem> {
        if (job.generationProgress.security.hasUnsafeProgressBoundary()) {
            return emptyList()
        }

        return job.generationProgress.steps.mapNotNull { step ->
            val label = publicProgressLabel(step.label).trim().ifBlank {
                publicProgressLabel(step.id).trim()
            }
            val status = safeDisplayText(step.status).trim()
            val message = safeProgressMessage(step.message).trim()
            if (label.isBlank() && status.isBlank() && message.isBlank()) {
                null
            } else {
                GenerationProgressStepItem(
                    label = label,
                    status = status,
                    message = message
                )
            }
        }
    }

    private fun shouldFetchArtifacts(job: PetGenerationJobResponseDto): Boolean =
        job.appJobId.isNotBlank() &&
            (
                job.artifacts.isEmpty() &&
                    (job.artifactCount > 0 || job.progressStatus == "waiting-for-review") ||
                    job.artifactCount > job.artifacts.size ||
                    (
                        (job.progressStatus == "waiting-for-review" || job.nextAction == "human-review") &&
                            job.artifacts.none { it.isReviewableCandidateArtifact() }
                        )
                )

    private fun publicPreviewUrl(appJobId: String, artifact: PetGenerationArtifactDto): String {
        val artifactRoute = "/pet-generation-jobs/${appJobId.pathSegment()}/artifacts/${artifact.downloadId.pathSegment()}"
        val url = artifact.downloadUrl
            .trim()
            .takeIf { it.isPublicDownloadUrl() }
            ?: artifactRoute
        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://") -> url
            url.startsWith("/") -> apiBaseUrl.trimEnd('/') + url
            else -> apiBaseUrl.trimEnd('/') + "/" + url
        }
    }

    private fun parseNotes(text: String): List<String> =
        text.split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun String.isPublicDownloadUrl(): Boolean {
        val lower = lowercase()
        if (lower.startsWith("file:")) {
            return false
        }
        if (PUBLIC_URL_INTERNAL_MARKERS.any { marker -> lower.contains(marker) }) {
            return false
        }
        if (WINDOWS_DRIVE_PREFIX.containsMatchIn(this)) {
            return false
        }
        return lower.startsWith("http://") || lower.startsWith("https://") || startsWith("/")
    }

    private fun safeDisplayText(text: String): String =
        if (INTERNAL_MARKERS.any { marker -> text.contains(marker, ignoreCase = true) }) {
            ""
        } else {
            text
        }

    private fun publicProgressLabel(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("codex") -> "Candidate generation"
            lower.contains("genericagent") ||
                lower.contains("generic agent") -> "Generation orchestration"
            lower.contains("route policy") ||
                lower.contains("task packet") ||
                (lower.contains("route") && lower.contains("task")) -> "Planning"
            else -> safeDisplayText(text)
        }
    }

    private fun safeProgressMessage(text: String): String =
        if (text.containsUnsafeProgressDetail()) {
            ""
        } else {
            safeDisplayText(text)
        }

    private fun String.containsUnsafeProgressDetail(): Boolean {
        val lower = lowercase()
        return PROGRESS_INTERNAL_DETAIL_MARKERS.any { marker -> lower.contains(marker) }
    }

    private fun PetGenerationArtifactDto.isReviewableCandidateArtifact(): Boolean =
        kind == "candidate" &&
            downloadId.isOpaqueDownloadId() &&
            !listOf(label, downloadId, taskId, actionId, downloadUrl).any { text ->
                INTERNAL_REVIEW_TARGET_MARKERS.any { marker ->
                    text.contains(marker, ignoreCase = true)
                }
            }

    private fun String.isOpaqueDownloadId(): Boolean {
        val trimmed = trim()
        val lower = trimmed.lowercase()
        return trimmed.isNotBlank() &&
            !lower.startsWith("file:") &&
            !WINDOWS_DRIVE_PREFIX.containsMatchIn(trimmed) &&
            !trimmed.contains("/") &&
            !trimmed.contains("\\") &&
            !trimmed.contains(":")
    }

    private fun String.publicCandidateActionId(): String {
        val safeText = safeDisplayText(trim()).trim()
        return safeText
            .takeIf { it.isOpaqueDownloadId() }
            ?.take(48)
            .orEmpty()
    }

    private fun String.pathSegment(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

    private fun String.pathToken(): String =
        replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "job" }

    private companion object {
        val ALLOWED_BODY_SHAPES = GENERATION_BODY_SHAPE_OPTIONS.toSet()
        val ALLOWED_REVIEW_DECISIONS = setOf("accept", "revise", "reject")
        val INTERNAL_MARKERS = listOf(
            ":/",
            ":\\",
            "server_run.json",
            "artifact-index.json",
            "resolution-map",
            "desktop-pet-casebook-audit.json",
            "desktop-pet-stage-gate-report.json",
            "desktop-pet-learning-memory.json",
            "human-feedback-context.json",
            "genericagent-orchestrator-task.json",
            "codex-worker-task.json",
            "codex-worker-task.output.json",
            "*.invocation.json",
            ".invocation.json",
            "*.execution.json",
            ".execution.json",
            "*.output.json.adapterprovenance",
            ".output.json.adapterprovenance",
            "adapterprovenance",
            "directcodexcli",
            "strategy-plan.json",
            "codex-generation-directives.json",
            "server-proof-summary.json",
            "server-proof-summary",
            "realadapterlaunch",
            "humanacceptance",
            "server-generation-learning-drill.json",
            "server-generation-regression-report.json",
            "learning-ledger.jsonl",
            "route-policy-decision.json",
            "genericagent-ledger-suggestions.json",
            "genericagent-ledger-import.json",
            "stage-gate-ledger-import.json",
            "learning-drill",
            "learningprogress",
            "learningmemoryresponse",
            "codexgenerationdirectiveresponse",
            "codexgenerationdirectiveresponsepresentcount",
            "codexgenerationdirectiveresponsesummary",
            "codexqaevidence",
            "directivehistoryresponse",
            "narrowedrepairfocus",
            "gadirectivehistoryresponse",
            "gadirectivehistoryresponsepresentcount",
            "gadirectivehistoryaddressedgenerationdirectivetext",
            "gadirectivehistorynarrowedrepairfocus",
            "gadirectivehistorynarrowedrepairfocuscounts",
            "directivehistorynarrowedrepairfocuscountdeltas",
            "repeateddirectivehistorynarrowedrepairfocus",
            "casebookreferencesused",
            "repairstrategiesused",
            "desktoppetlearningmemorysummary",
            "servergenerationlearningprogresssummary",
            "qualitygatestatus",
            "qualitygatetrend",
            "learningassessment",
            "nextrepairfocus",
            "memorycarryforward",
            "learningmemoryinput",
            "learningmemoryoutput",
            "repeatedneedsrevisionstages",
            "repeatedhardfailuresobserved",
            "missingneedsrevisioncoverage",
            "missinghardfailurecoverage",
            "repaircoverage",
            "repairstrategyusecounts",
            "codex-action-attempt-n-server-imagegen-001",
            "priormemorypresent",
            "priormemoryqualitygatestatus",
            "priormemoryscenariocount",
            "qualitygatestatuscounts",
            "stagegatereport",
            "stagegaterepair",
            "stagegaterepairrequests",
            "stagegatestatus",
            "learningledgersuggestions",
            "routeswitchrequired",
            "disabledroutes",
            "caseid",
            "referencetype",
            "strengthstopreserve",
            "reviewlessons",
            "regression-report",
            "agent-review.json",
            "orchestration-review.json",
            "secret/",
            "secret\\",
            "runs/",
            "runs\\"
        )
        val PROGRESS_INTERNAL_DETAIL_MARKERS = listOf(
            "genericagent",
            "generic agent",
            "codex",
            "directive",
            "route policy",
            "task packet",
            "worker command"
        )
        val PUBLIC_URL_INTERNAL_MARKERS = listOf(
            "\\",
            "server_run.json",
            "artifact-index.json",
            "resolution-map",
            "desktop-pet-casebook-audit.json",
            "desktop-pet-stage-gate-report.json",
            "desktop-pet-learning-memory.json",
            "human-feedback-context.json",
            "genericagent-orchestrator-task.json",
            "codex-worker-task.json",
            "codex-worker-task.output.json",
            "*.invocation.json",
            ".invocation.json",
            "*.execution.json",
            ".execution.json",
            "*.output.json.adapterprovenance",
            ".output.json.adapterprovenance",
            "adapterprovenance",
            "directcodexcli",
            "strategy-plan.json",
            "codex-generation-directives.json",
            "server-proof-summary.json",
            "server-proof-summary",
            "realadapterlaunch",
            "humanacceptance",
            "server-generation-learning-drill.json",
            "server-generation-regression-report.json",
            "learning-ledger.jsonl",
            "route-policy-decision.json",
            "genericagent-ledger-suggestions.json",
            "genericagent-ledger-import.json",
            "stage-gate-ledger-import.json",
            "learning-drill",
            "learningprogress",
            "learningmemoryresponse",
            "codexgenerationdirectiveresponse",
            "codexgenerationdirectiveresponsepresentcount",
            "codexgenerationdirectiveresponsesummary",
            "codexqaevidence",
            "directivehistoryresponse",
            "narrowedrepairfocus",
            "gadirectivehistoryresponse",
            "gadirectivehistoryresponsepresentcount",
            "gadirectivehistoryaddressedgenerationdirectivetext",
            "gadirectivehistorynarrowedrepairfocus",
            "gadirectivehistorynarrowedrepairfocuscounts",
            "directivehistorynarrowedrepairfocuscountdeltas",
            "repeateddirectivehistorynarrowedrepairfocus",
            "casebookreferencesused",
            "repairstrategiesused",
            "desktoppetlearningmemorysummary",
            "servergenerationlearningprogresssummary",
            "qualitygatestatus",
            "qualitygatetrend",
            "learningassessment",
            "nextrepairfocus",
            "memorycarryforward",
            "learningmemoryinput",
            "learningmemoryoutput",
            "repeatedneedsrevisionstages",
            "repeatedhardfailuresobserved",
            "missingneedsrevisioncoverage",
            "missinghardfailurecoverage",
            "repaircoverage",
            "repairstrategyusecounts",
            "codex-action-attempt-n-server-imagegen-001",
            "priormemorypresent",
            "priormemoryqualitygatestatus",
            "priormemoryscenariocount",
            "qualitygatestatuscounts",
            "stagegatereport",
            "stagegaterepair",
            "stagegaterepairrequests",
            "stagegatestatus",
            "learningledgersuggestions",
            "routeswitchrequired",
            "disabledroutes",
            "caseid",
            "referencetype",
            "strengthstopreserve",
            "reviewlessons",
            "regression-report",
            "agent-review.json",
            "orchestration-review.json",
            "secret/",
            "runs/"
        )
        val INTERNAL_REVIEW_TARGET_MARKERS = listOf(
            "desktop-pet-casebook-audit.json",
            "casebook-audit",
            "desktop-pet-stage-gate-report.json",
            "stage-gate-report",
            "desktop-pet-learning-memory.json",
            "learning-memory",
            "human-feedback-context.json",
            "genericagent-orchestrator-task.json",
            "codex-worker-task.json",
            "codex-worker-task.output.json",
            "*.invocation.json",
            ".invocation.json",
            "*.execution.json",
            ".execution.json",
            "*.output.json.adapterprovenance",
            ".output.json.adapterprovenance",
            "adapterprovenance",
            "directcodexcli",
            "strategy-plan.json",
            "codex-generation-directives.json",
            "server-proof-summary.json",
            "server-proof-summary",
            "realadapterlaunch",
            "humanacceptance",
            "server-generation-learning-drill.json",
            "learning-drill",
            "learningprogress",
            "learningmemoryresponse",
            "codexgenerationdirectiveresponse",
            "codexgenerationdirectiveresponsepresentcount",
            "codexgenerationdirectiveresponsesummary",
            "codexqaevidence",
            "directivehistoryresponse",
            "narrowedrepairfocus",
            "gadirectivehistoryresponse",
            "gadirectivehistoryresponsepresentcount",
            "gadirectivehistoryaddressedgenerationdirectivetext",
            "gadirectivehistorynarrowedrepairfocus",
            "gadirectivehistorynarrowedrepairfocuscounts",
            "directivehistorynarrowedrepairfocuscountdeltas",
            "repeateddirectivehistorynarrowedrepairfocus",
            "casebookreferencesused",
            "repairstrategiesused",
            "desktoppetlearningmemorysummary",
            "servergenerationlearningprogresssummary",
            "qualitygatestatus",
            "qualitygatetrend",
            "learningassessment",
            "nextrepairfocus",
            "memorycarryforward",
            "learningmemoryinput",
            "learningmemoryoutput",
            "repeatedneedsrevisionstages",
            "repeatedhardfailuresobserved",
            "missingneedsrevisioncoverage",
            "missinghardfailurecoverage",
            "repaircoverage",
            "repairstrategyusecounts",
            "codex-action-attempt-n-server-imagegen-001",
            "priormemorypresent",
            "priormemoryqualitygatestatus",
            "priormemoryscenariocount",
            "qualitygatestatuscounts",
            "stagegatereport",
            "stagegaterepair",
            "stagegaterepairrequests",
            "stagegatestatus",
            "learningledgersuggestions",
            "routeswitchrequired",
            "disabledroutes",
            "caseid",
            "referencetype",
            "strengthstopreserve",
            "reviewlessons",
            "server-generation-regression-report.json",
            "regression-report",
            "learning-ledger.jsonl",
            "route-policy-decision.json",
            "server-proof-summary.json",
            "server-proof-summary",
            "realadapterlaunch",
            "humanacceptance",
            "codexqaevidence",
            "genericagent-ledger-suggestions.json",
            "genericagent-ledger-import.json",
            "stage-gate-ledger-import.json",
            "agent-review.json",
            "orchestration-review.json"
        )
        val WINDOWS_DRIVE_PREFIX = Regex("^[A-Za-z]:[\\\\/]")
    }
}

fun generationStatusLabel(progressStatus: String, nextAction: String): String =
    when (progressStatus.ifBlank { nextAction }) {
        "queued" -> "Queued"
        "processing" -> "Generating"
        "waiting-for-worker-output" -> "Waiting for worker output"
        "waiting-for-review" -> "Ready for human review"
        "packaging" -> "Packaging pet.zip"
        "ready-for-download" -> "Ready for download"
        "revision-requested" -> "Revision requested"
        "candidate-rejected" -> "Candidate rejected"
        "failed" -> "Failed"
        else -> when (nextAction) {
            "wait" -> "Waiting"
            "human-review" -> "Ready for human review"
            "processing-package" -> "Packaging pet.zip"
            "download-package" -> "Ready for download"
            "await-revision" -> "Revision requested"
            "await-new-candidate" -> "Candidate rejected"
            else -> "Waiting"
        }
    }

fun generationProgressSummaryLine(job: PetGenerationJobResponseDto): String {
    if (isContractDemoGenerationJob(job)) {
        return CONTRACT_DEMO_PROGRESS_MESSAGE
    }

    if (canShowPackageDownload(job)) {
        return "pet.zip is ready to download."
    }

    return when (effectiveProgressStatus(job)) {
        "queued" -> "Waiting for generation worker."
        "processing" -> "Generating candidate assets."
        "waiting-for-worker-output" -> "Waiting for worker output."
        "waiting-for-review" -> {
            val candidateCount = job.generationProgress.summary.candidateCount
                .takeIf { it > 0 }
                ?: job.artifacts.count { artifact -> artifact.kind == "candidate" }
            if (candidateCount == 1) {
                "1 candidate ready for human review."
            } else {
                "$candidateCount candidates ready for human review."
            }
        }
        "packaging" -> "Packaging pet.zip."
        "revision-requested" -> "Revision requested; waiting for a revised candidate."
        "candidate-rejected" -> "Candidate rejected; waiting for a new candidate."
        "failed" -> "Generation failed."
        else -> ""
    }
}

fun generationServerWorkerWaitNotice(job: PetGenerationJobResponseDto): String =
    when (effectiveProgressStatus(job)) {
        "queued",
        "processing",
        "waiting-for-worker-output" ->
            "Waiting for a trusted server worker; this app only created and polls the job."
        "revision-requested",
        "candidate-rejected" ->
            "Feedback recorded; a trusted server worker must publish the next candidate."
        else -> ""
    }

fun workerReadinessMessage(readiness: WorkerReadinessResponseDto): String {
    if (readiness.security.hasUnsafeReadinessBoundary()) {
        return "Generation service blocked: unsafe readiness report."
    }

    val status = readiness.status.publicReadinessToken().ifBlank { "blocked" }
    if (status == "ready") {
        return "Generation service ready."
    }

    val adapterSummary = readiness.adapters
        .mapNotNull { adapter ->
            val adapterName = adapter.adapter.publicReadinessToken()
            val adapterStatus = adapter.status.publicReadinessToken()
            if (adapterName.isBlank() || adapterStatus.isBlank()) {
                null
            } else {
                "$adapterName $adapterStatus"
            }
        }
        .joinToString("; ")

    return if (adapterSummary.isBlank()) {
        "Generation service $status."
    } else {
        "Generation service $status: $adapterSummary."
    }
}

fun appApiContractMessage(contract: PetGenerationAppApiContractDto): String {
    if (
        contract.security.hasUnsafeAppApiContractBoundary() ||
        contract.publicEndpoints.any { endpoint ->
            endpoint.isPublic && endpoint.path.trim().isAdminEndpointPath()
        }
    ) {
        return "Generation API contract blocked: unsafe public boundary."
    }

    val schema = contract.schema.publicReadinessToken()
        .ifBlank { "unknown-contract" }
    val publicEndpointCount = contract.publicEndpoints.count { it.isPublic }
    val endpointLabel = if (publicEndpointCount == 1) {
        "1 public endpoint"
    } else {
        "$publicEndpointCount public endpoints"
    }
    return "Generation API contract $schema exposes $endpointLabel."
}

fun generationCreateValidationFailureReason(
    description: String,
    bodyShape: String,
    referencesText: String,
    appJobId: String = ""
): String {
    val trimmedDescription = description.trim()
    if (trimmedDescription.isBlank()) {
        return "description_required"
    }
    if (trimmedDescription.length > 4000) {
        return "description_too_long"
    }

    val normalizedBodyShape = bodyShape.trim().ifBlank { null }
    if (normalizedBodyShape != null && normalizedBodyShape !in GENERATION_BODY_SHAPE_OPTIONS) {
        return "invalid_body_shape"
    }

    val references = parseReferences(referencesText)
    if (references.size > MAX_REFERENCE_URLS) {
        return "too_many_reference_urls"
    }
    if (references.any { !it.isHttpReferenceUrl() }) {
        return "reference_urls_must_be_http_or_https"
    }
    if (!appJobId.isOptionalPublicAppJobId()) {
        return "invalid_app_job_id"
    }

    return ""
}

fun generationCreateValidationMessage(
    description: String,
    bodyShape: String,
    referencesText: String,
    appJobId: String = ""
): String =
    when (generationCreateValidationFailureReason(description, bodyShape, referencesText, appJobId)) {
        "description_required" -> "Description is required."
        "description_too_long" -> "Description must be 4000 characters or fewer."
        "invalid_body_shape" -> "Choose a supported body shape."
        "too_many_reference_urls" -> "Use at most 8 reference URLs."
        "reference_urls_must_be_http_or_https" -> "Reference URLs must use HTTP or HTTPS."
        "invalid_app_job_id" -> "Task name can use letters, numbers, dot, underscore, or dash."
        else -> ""
    }

fun canCreateGenerationJob(
    description: String,
    bodyShape: String,
    referencesText: String,
    appJobId: String = ""
): Boolean =
    generationCreateValidationFailureReason(description, bodyShape, referencesText, appJobId).isBlank()

fun pollGenerationJobValidationFailureReason(appJobId: String): String {
    val normalizedAppJobId = appJobId.trim()
    if (normalizedAppJobId.isBlank()) {
        return "app_job_id_required"
    }
    if (!normalizedAppJobId.isPublicAppJobId()) {
        return "invalid_app_job_id"
    }
    return ""
}

fun pollGenerationJobValidationMessage(appJobId: String): String =
    when (pollGenerationJobValidationFailureReason(appJobId)) {
        "app_job_id_required" -> "Enter a task name to poll."
        "invalid_app_job_id" -> "Task name can use letters, numbers, dot, underscore, or dash."
        else -> ""
    }

fun canPollGenerationJob(appJobId: String): Boolean =
    pollGenerationJobValidationFailureReason(appJobId).isBlank()

fun isContractDemoGenerationJob(job: PetGenerationJobResponseDto): Boolean =
    isContractDemoGenerationJobId(job.appJobId) ||
        isContractDemoGenerationJobId(job.runId)

fun isContractDemoGenerationJobId(appJobId: String): Boolean =
    appJobId.trim().lowercase() in CONTRACT_DEMO_APP_JOB_IDS

fun generationContractDemoNotice(job: PetGenerationJobResponseDto): String =
    if (isContractDemoGenerationJob(job)) {
        "Contract demo task: this candidate is pre-seeded for public API validation; it is not a live pet generation run."
    } else {
        ""
    }

fun canShowPackageDownload(job: PetGenerationJobResponseDto): Boolean =
    !isContractDemoGenerationJob(job) && (job.downloadReady || job.nextAction == "download-package")

fun packageDownloadStartedMessage(): String =
    "Downloading pet.zip..."

fun packageDownloadSuccessMessage(fileName: String): String {
    val safeFileName = fileName.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        ?: "pet.zip"
    return "Downloaded $safeFileName to app downloads."
}

fun packageDownloadFailureMessage(reason: String): String {
    val safeReason = reason.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        .orEmpty()
    return if (safeReason.isBlank()) {
        "Package download blocked."
    } else {
        "Package download blocked: $safeReason"
    }
}

fun packageImportInProgressCandidate(
    candidate: PetGenerationPackageImportCandidate
): PetGenerationPackageImportCandidate =
    candidate.copy(
        status = "creating-community-import",
        summary = "Creating community import draft..."
    )

fun packageImportDraftSuccessCandidate(
    candidate: PetGenerationPackageImportCandidate,
    draft: ImportDraftDto
): PetGenerationPackageImportCandidate {
    val safeDraftId = draft.id.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        ?: "import-draft"
    val safePetId = draft.petId.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        ?: candidate.appJobId

    return candidate.copy(
        status = "community-import-ready",
        summary = "Community import draft $safeDraftId ready for $safePetId."
    )
}

fun packageImportDraftFailureCandidate(
    candidate: PetGenerationPackageImportCandidate,
    reason: String
): PetGenerationPackageImportCandidate {
    val safeReason = reason.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        .orEmpty()
    val summary = if (safeReason.isBlank()) {
        "Community import blocked."
    } else {
        "Community import blocked because $safeReason."
    }

    return candidate.copy(
        status = "community-import-blocked",
        summary = summary
    )
}

fun canSubmitPackageImportDraft(draft: ImportDraftDto?): Boolean =
    draft?.status == "ready" &&
        draft.id.trim().isSafePackageDownloadDisplayText()

fun packageImportSubmissionStartedMessage(): String =
    "Submitting community import draft..."

fun packageImportSubmissionSuccessMessage(
    response: ImportDraftSubmissionResponseDto
): String {
    val safeSubmissionId = response.submission.id.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        ?: "submission"
    val safeSubmissionStatus = response.submission.status.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        ?: "pending"
    val safePetId = response.submission.petId.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        ?: "pet"

    return "Community submission $safeSubmissionId $safeSubmissionStatus for $safePetId."
}

fun packageImportSubmissionFailureMessage(reason: String): String {
    val safeReason = reason.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        .orEmpty()
    return if (safeReason.isBlank()) {
        "Community submission blocked."
    } else {
        "Community submission blocked because $safeReason."
    }
}

fun canRefreshPackageImportSubmission(submissionId: String): Boolean =
    packageImportSubmissionIdForResume(submissionId) != null

fun packageImportSubmissionIdForResume(submissionId: String): String? =
    submissionId.trim().takeIf { it.isPublicSubmissionId() }

fun packageImportSubmissionResumeMessage(submissionId: String): String {
    val safeSubmissionId = packageImportSubmissionIdForResume(submissionId)
        ?: return ""
    return "Community submission $safeSubmissionId is ready to refresh."
}

fun packageImportSubmissionStatusMessage(submission: SubmissionDto): String {
    val safeSubmissionId = submission.id.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        ?: return "Community submission status unavailable."
    val safePetId = submission.petId.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        ?: return "Community submission status unavailable."

    return when (val status = submission.status.trim()) {
        "pending" -> "Community submission $safeSubmissionId is pending review for $safePetId."
        "approved" -> "Community submission $safeSubmissionId approved for $safePetId."
        "held" -> "Community submission $safeSubmissionId is held for $safePetId."
        "rejected" -> "Community submission $safeSubmissionId rejected for $safePetId."
        "revoked" -> "Community submission $safeSubmissionId revoked for $safePetId."
        else -> {
            val safeStatus = status
                .takeIf { it.isSafePackageDownloadDisplayText() }
                ?: return "Community submission status unavailable."
            "Community submission $safeSubmissionId is $safeStatus for $safePetId."
        }
    }
}

fun packageImportSubmissionCommunityRefreshMessage(
    submission: SubmissionDto,
    community: InitialCommunityResult
): String {
    val baseMessage = packageImportSubmissionStatusMessage(submission)
    if (
        baseMessage == "Community submission status unavailable." ||
        submission.status.trim() != "approved"
    ) {
        return baseMessage
    }

    val safePetId = submission.petId.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        ?: return "Community submission status unavailable."
    val safeSubmissionId = submission.id.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        ?: return "Community submission status unavailable."

    val details = buildList {
        val approvedPetVisible = community.approvedPets.any { pet ->
            pet.petId == safePetId
        }
        if (approvedPetVisible) {
            add("Showcase updated")
        }

        val rewardLabel = community.posts
            .firstOrNull { post ->
                post.petId == safePetId &&
                    post.submissionLabel.orEmpty().contains(safeSubmissionId)
            }
            ?.rewardLabel
            ?.trim()
            ?.takeIf { it.isSafePackageDownloadDisplayText() }
        if (rewardLabel != null) {
            add("reward $rewardLabel posted")
        }

        if (!community.usedFallback && community.walletBalance >= 0) {
            add("wallet balance ${community.walletBalance} petcoin")
        }
    }

    return if (details.isEmpty()) {
        baseMessage
    } else {
        "$baseMessage ${details.joinToString("; ")}."
    }
}

fun packageImportCandidateMessage(candidate: PetGenerationPackageImportCandidate?): String {
    candidate ?: return ""
    if (candidate.status != "waiting-for-community-import") {
        return candidate.summary.trim()
            .takeIf { it.isSafePackageDownloadDisplayText() }
            ?: "Community import blocked."
    }

    val safePackageFileName = candidate.packageFileName.trim()
        .takeIf { it.isSafePackageDownloadDisplayText() }
        ?: "pet.zip"
    val safeTargetDownloadId = candidate.targetDownloadId.trim()
        .takeIf { it.isOpaqueCandidateDownloadIdForUi() }
        ?: "candidate"
    val safeByteCount = candidate.packageByteCount.coerceAtLeast(0L)
    return "Community import pending: $safePackageFileName / $safeByteCount bytes / review target $safeTargetDownloadId."
}

fun generationPollDelayMillis(job: PetGenerationJobResponseDto): Long =
    when (effectiveProgressStatus(job)) {
        "revision-requested",
        "candidate-rejected" -> 8_000L
        else -> 3_000L
    }

fun shouldPollGenerationJob(job: PetGenerationJobResponseDto): Boolean =
    job.appJobId.isNotBlank() &&
        effectiveProgressStatus(job) in setOf(
            "queued",
            "processing",
            "waiting-for-worker-output",
            "packaging",
            "revision-requested",
            "candidate-rejected"
        )

fun canSubmitHumanReview(
    job: PetGenerationJobResponseDto?,
    selectedCandidateDownloadId: String
): Boolean =
    selectedCandidateDownloadId.isOpaqueCandidateDownloadIdForUi() &&
        job?.let { currentJob ->
            !isContractDemoGenerationJob(currentJob) &&
            (
                effectiveProgressStatus(currentJob) == "waiting-for-review" ||
                    currentJob.nextAction == "human-review"
                ) &&
                currentJob.artifacts.any { artifact ->
                    artifact.isSelectedReviewButtonCandidate(selectedCandidateDownloadId)
                }
        } == true

fun canSubmitReviewDecision(
    job: PetGenerationJobResponseDto?,
    selectedCandidateDownloadId: String,
    decision: String,
    notesText: String
): Boolean {
    if (!canSubmitHumanReview(job, selectedCandidateDownloadId)) {
        return false
    }
    if (reviewNotesValidationMessage(decision, notesText).isNotBlank()) {
        return false
    }

    return when (decision.trim()) {
        "accept" -> true
        "revise",
        "reject" -> true
        else -> false
    }
}

fun reviewNotesValidationMessage(decision: String, notesText: String): String =
    when (reviewNotesValidationFailureReason(decision, notesText)) {
        "review_notes_required" -> "Revise and reject need specific visual notes."
        "review_notes_must_not_include_internal_paths" ->
            "Review notes cannot include internal paths or worker details."
        else -> ""
    }

private fun reviewNotesValidationFailureReason(decision: String, notesText: String): String {
    val normalizedDecision = decision.trim()
    val notes = notesText.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    if (notes.any { it.containsUnsafeReviewNoteDetail() }) {
        return "review_notes_must_not_include_internal_paths"
    }
    if (normalizedDecision in setOf("revise", "reject") && notes.isEmpty()) {
        return "review_notes_required"
    }

    return ""
}

fun selectedCandidateAfterJobRefresh(
    candidates: List<CandidateGalleryItem>,
    currentSelectedCandidateDownloadId: String
): String {
    val currentSelection = currentSelectedCandidateDownloadId.trim()
    if (
        currentSelection.isNotBlank() &&
        candidates.any { it.targetDownloadId == currentSelection && !it.reviewed }
    ) {
        return currentSelection
    }

    return candidates.firstOrNull { !it.reviewed }?.targetDownloadId
        ?: candidates.firstOrNull()?.targetDownloadId.orEmpty()
}

fun effectiveProgressStatus(job: PetGenerationJobResponseDto): String =
    job.progressStatus.ifBlank { job.status }

fun generationJobIdForResume(
    savedAppJobId: String,
    currentJob: PetGenerationJobResponseDto?
): String? =
    savedAppJobId.trim().takeIf { it.isNotBlank() && currentJob == null }

fun canClearGenerationJob(
    savedAppJobId: String,
    currentJob: PetGenerationJobResponseDto?
): Boolean =
    savedAppJobId.trim().isNotBlank() || currentJob?.appJobId?.trim().orEmpty().isNotBlank()

fun persistedGenerationJobId(
    requestedAppJobId: String,
    job: PetGenerationJobResponseDto
): String =
    job.appJobId.trim().ifBlank { requestedAppJobId.trim() }

private const val GENERATION_JOB_HISTORY_LIMIT = 5
private val CONTRACT_DEMO_APP_JOB_IDS = setOf(
    "publicdemo1",
    "public-lifecycle-smoke",
    "public_lifecycle_smoke"
)

fun generationJobHistoryAfterPersist(
    existingAppJobIds: List<String>,
    requestedAppJobId: String,
    job: PetGenerationJobResponseDto
): List<String> {
    val persistedJobId = persistedGenerationJobId(requestedAppJobId, job)
        .takeIf { it.isPublicAppJobId() }

    return (listOfNotNull(persistedJobId) + existingAppJobIds)
        .map { it.trim() }
        .filter { it.isPublicAppJobId() }
        .distinct()
        .take(GENERATION_JOB_HISTORY_LIMIT)
}

fun generationJobHistoryAfterRemove(
    existingAppJobIds: List<String>,
    appJobIdToRemove: String
): List<String> {
    val normalizedAppJobIdToRemove = appJobIdToRemove.trim()
    return existingAppJobIds
        .map { it.trim() }
        .filter { it.isPublicAppJobId() }
        .filter { it != normalizedAppJobIdToRemove }
        .distinct()
        .take(GENERATION_JOB_HISTORY_LIMIT)
}

fun persistedGenerationJobHistory(rawHistory: String): List<String> =
    rawHistory.split('\n', ',')
        .map { it.trim() }
        .filter { it.isPublicAppJobId() }
        .distinct()
        .take(GENERATION_JOB_HISTORY_LIMIT)

fun initialGenerationJobHistory(savedAppJobId: String, rawHistory: String): List<String> =
    (listOf(savedAppJobId.trim()) + persistedGenerationJobHistory(rawHistory))
        .filter { it.isPublicAppJobId() }
        .distinct()
        .take(GENERATION_JOB_HISTORY_LIMIT)

fun serializedGenerationJobHistory(appJobIds: List<String>): String =
    appJobIds
        .map { it.trim() }
        .filter { it.isPublicAppJobId() }
        .distinct()
        .take(GENERATION_JOB_HISTORY_LIMIT)
        .joinToString("\n")

fun recentGenerationJobResumeId(appJobId: String): String? =
    appJobId.trim().takeIf { it.isPublicAppJobId() }

val REVIEW_NOTE_SUGGESTIONS = listOf(
    "idle action jumps vertically",
    "running-right is nearly static",
    "first and last frame mismatch",
    "main identity drifts",
    "detached effect particles",
    "wrong facing direction",
    "loop boundary is abrupt"
)

fun appendReviewNoteSuggestion(existingNotes: String, suggestion: String): String {
    val normalizedSuggestion = suggestion.trim()
    if (normalizedSuggestion.isBlank()) {
        return existingNotes.trim()
    }

    return (existingNotes.lines().map { it.trim() }.filter { it.isNotBlank() } + normalizedSuggestion)
        .distinct()
        .joinToString("\n")
}

private fun String.publicReadinessToken(): String =
    trim()
        .lowercase()
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-')

private fun WorkerReadinessSecurityDto.hasUnsafeReadinessBoundary(): Boolean =
    secretsInReport ||
        executesAgentProcesses ||
        appMayInvokeAgentsDirectly ||
        executesReadinessProbe

private fun PetGenerationAppApiContractSecurityDto.hasUnsafeAppApiContractBoundary(): Boolean =
    exposesInternalPaths ||
        exposesRawPrompt ||
        exposesWorkerCommands ||
        exposesSecrets ||
        appMayInvokeAgentsDirectly ||
        !requiresHumanReview ||
        !adminEndpointsDisabledByDefault

private fun String.isAdminEndpointPath(): Boolean =
    trim().split('/').getOrNull(1).equals("admin", ignoreCase = true)

private fun PetGenerationProgressSecurityDto.hasUnsafeProgressBoundary(): Boolean =
    exposesInternalPaths || exposesWorkerCommands

private fun PetGenerationArtifactDto.isSelectedReviewButtonCandidate(
    selectedCandidateDownloadId: String
): Boolean =
    kind == "candidate" &&
        downloadId == selectedCandidateDownloadId &&
        reviewDecision.isBlank() &&
        downloadId.isOpaqueCandidateDownloadIdForUi() &&
        !listOf(label, downloadId, taskId, actionId, downloadUrl).any { text ->
            REVIEW_BUTTON_BLOCKED_ARTIFACT_MARKERS.any { marker ->
                text.contains(marker, ignoreCase = true)
            }
        }

private fun String.isOpaqueCandidateDownloadIdForUi(): Boolean {
    val trimmed = trim()
    val lower = trimmed.lowercase()
    return trimmed.isNotBlank() &&
        !lower.startsWith("file:") &&
        !Regex("^[A-Za-z]:[\\\\/]").containsMatchIn(trimmed) &&
        !trimmed.contains("/") &&
        !trimmed.contains("\\") &&
        !trimmed.contains(":")
}

private fun String.isOptionalPublicAppJobId(): Boolean {
    val normalizedAppJobId = trim()
    return normalizedAppJobId.isBlank() || normalizedAppJobId.isPublicAppJobId()
}

private fun String.isPublicAppJobId(): Boolean =
    trim().matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,79}"))

private fun String.isPublicSubmissionId(): Boolean =
    trim().matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,119}"))

private fun String.containsUnsafeReviewNoteDetail(): Boolean {
    val lower = lowercase()
    return lower.startsWith("file:") ||
        Regex("(^|\\s)[A-Za-z]:[\\\\/]").containsMatchIn(this) ||
        REVIEW_NOTE_BLOCKED_MARKERS.any { marker -> lower.contains(marker) } ||
        REVIEW_NOTE_BLOCKED_WORDS.any { word ->
            Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(lower)
        }
}

private fun parseReferences(text: String): List<String> =
    text.split(',', '\n')
        .map { it.trim() }
        .filter { it.isNotBlank() }

private val REVIEW_NOTE_BLOCKED_MARKERS = listOf(
    "server_run.json",
    "runs/",
    "runs\\",
    "secret/",
    "secret\\",
    "prompt" + "-pack",
    "adapter" + "-config",
    "adapterprovenance",
    "directcodexcli",
    "target" + "output",
    "server-worker" + "-cycle",
    "agent" + "-outputs",
    "learning-ledger.jsonl",
    "route-policy-decision.json",
    "server-proof-summary.json",
    "server-proof-summary",
    "realadapterlaunch",
    "humanacceptance",
    "codexqaevidence",
    "genericagent-ledger-suggestions.json",
    "genericagent-ledger-import.json",
    "stage-gate-ledger-import.json",
    "codex-action-attempt-n-server-imagegen-001",
    "stagegaterepair",
    "stagegaterepairrequests",
    "stagegatestatus",
    "*.invocation.json",
    ".invocation.json",
    "*.execution.json",
    ".execution.json",
    "*.output.json.adapterprovenance",
    ".output.json.adapterprovenance"
)

private val REVIEW_NOTE_BLOCKED_WORDS = listOf("lea" + "se", "s" + "sh")

private val REVIEW_BUTTON_BLOCKED_ARTIFACT_MARKERS = listOf(
    "desktop-pet-casebook-audit.json",
    "casebook-audit",
    "desktop-pet-stage-gate-report.json",
    "stage-gate-report",
    "desktop-pet-learning-memory.json",
    "learning-memory",
    "human-feedback-context.json",
    "genericagent-orchestrator-task.json",
    "codex-worker-task.json",
    "codex-worker-task.output.json",
    "strategy-plan.json",
    "codex-generation-directives.json",
    "server-proof-summary.json",
    "server-proof-summary",
    "realadapterlaunch",
    "humanacceptance",
    "server-generation-learning-drill.json",
    "learning-drill",
    "learningprogress",
    "learningmemoryresponse",
    "codexgenerationdirectiveresponse",
    "codexgenerationdirectiveresponsepresentcount",
    "codexgenerationdirectiveresponsesummary",
    "codexqaevidence",
    "directivehistoryresponse",
    "narrowedrepairfocus",
    "gadirectivehistoryresponse",
    "gadirectivehistoryresponsepresentcount",
    "gadirectivehistoryaddressedgenerationdirectivetext",
    "gadirectivehistorynarrowedrepairfocus",
    "gadirectivehistorynarrowedrepairfocuscounts",
    "directivehistorynarrowedrepairfocuscountdeltas",
    "repeateddirectivehistorynarrowedrepairfocus",
    "casebookreferencesused",
    "repairstrategiesused",
    "desktoppetlearningmemorysummary",
    "servergenerationlearningprogresssummary",
    "qualitygatestatus",
    "qualitygatetrend",
    "learningassessment",
    "nextrepairfocus",
    "memorycarryforward",
    "learningmemoryinput",
    "learningmemoryoutput",
    "repeatedneedsrevisionstages",
    "repeatedhardfailuresobserved",
    "missingneedsrevisioncoverage",
    "missinghardfailurecoverage",
    "repaircoverage",
    "repairstrategyusecounts",
    "codex-action-attempt-n-server-imagegen-001",
    "priormemorypresent",
    "priormemoryqualitygatestatus",
    "priormemoryscenariocount",
    "qualitygatestatuscounts",
    "stagegatereport",
    "stagegaterepair",
    "stagegaterepairrequests",
    "stagegatestatus",
    "learningledgersuggestions",
    "routeswitchrequired",
    "disabledroutes",
    "caseid",
    "referencetype",
    "strengthstopreserve",
    "reviewlessons",
    "server-generation-regression-report.json",
    "regression-report",
    "learning-ledger.jsonl",
    "route-policy-decision.json",
    "server-proof-summary.json",
    "server-proof-summary",
    "realadapterlaunch",
    "humanacceptance",
    "genericagent-ledger-suggestions.json",
    "genericagent-ledger-import.json",
    "stage-gate-ledger-import.json",
    "agent-review.json",
    "orchestration-review.json"
)

private val PACKAGE_DOWNLOAD_BLOCKED_TEXT_MARKERS = listOf(
    "server_run.json",
    "artifact-index.json",
    "resolution-map",
    "desktop-pet-casebook-audit.json",
    "desktop-pet-stage-gate-report.json",
    "desktop-pet-learning-memory.json",
    "human-feedback-context.json",
    "genericagent-orchestrator-task.json",
    "codex-worker-task.json",
    "codex-worker-task.output.json",
    "strategy-plan.json",
    "codex-generation-directives.json",
    "server-generation-learning-drill.json",
    "server-generation-regression-report.json",
    "learning-ledger.jsonl",
    "route-policy-decision.json",
    "genericagent-ledger-suggestions.json",
    "genericagent-ledger-import.json",
    "stage-gate-ledger-import.json",
    "learning-drill",
    "learningprogress",
    "learningmemoryresponse",
    "codexgenerationdirectiveresponse",
    "codexgenerationdirectiveresponsepresentcount",
    "codexgenerationdirectiveresponsesummary",
    "codexqaevidence",
    "directivehistoryresponse",
    "narrowedrepairfocus",
    "gadirectivehistoryresponse",
    "gadirectivehistoryresponsepresentcount",
    "gadirectivehistoryaddressedgenerationdirectivetext",
    "gadirectivehistorynarrowedrepairfocus",
    "gadirectivehistorynarrowedrepairfocuscounts",
    "directivehistorynarrowedrepairfocuscountdeltas",
    "repeateddirectivehistorynarrowedrepairfocus",
    "casebookreferencesused",
    "repairstrategiesused",
    "desktoppetlearningmemorysummary",
    "servergenerationlearningprogresssummary",
    "qualitygatestatus",
    "qualitygatetrend",
    "learningassessment",
    "nextrepairfocus",
    "memorycarryforward",
    "learningmemoryinput",
    "learningmemoryoutput",
    "repeatedneedsrevisionstages",
    "repeatedhardfailuresobserved",
    "missingneedsrevisioncoverage",
    "missinghardfailurecoverage",
    "repaircoverage",
    "repairstrategyusecounts",
    "codex-action-attempt-n-server-imagegen-001",
    "priormemorypresent",
    "priormemoryqualitygatestatus",
    "priormemoryscenariocount",
    "qualitygatestatuscounts",
    "stagegatereport",
    "stagegaterepair",
    "stagegaterepairrequests",
    "stagegatestatus",
    "learningledgersuggestions",
    "routeswitchrequired",
    "disabledroutes",
    "caseid",
    "referencetype",
    "strengthstopreserve",
    "reviewlessons",
    "regression-report",
    "agent-review.json",
    "orchestration-review.json",
    "runs/",
    "runs\\",
    "secret/",
    "secret\\",
    "target" + "output",
    "prompt" + "-pack",
    "adapter" + "-config"
)

private fun String.isSafePackageDownloadDisplayText(): Boolean {
    val trimmed = trim()
    val lower = trimmed.lowercase()
    return trimmed.isNotBlank() &&
        !lower.startsWith("file:") &&
        !Regex("^[A-Za-z]:[\\\\/]").containsMatchIn(trimmed) &&
        !trimmed.contains("/") &&
        !trimmed.contains("\\") &&
        !trimmed.contains(":") &&
        PACKAGE_DOWNLOAD_BLOCKED_TEXT_MARKERS.none { marker ->
            lower.contains(marker)
        }
}

private fun String.isHttpReferenceUrl(): Boolean =
    try {
        val uri = URI(this)
        uri.scheme?.lowercase() in setOf("http", "https") &&
            !uri.host.isNullOrBlank()
    } catch (_: Exception) {
        false
    }
