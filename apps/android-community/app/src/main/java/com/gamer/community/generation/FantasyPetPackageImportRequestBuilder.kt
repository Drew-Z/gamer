package com.gamer.community.generation

import com.gamer.community.api.ApiCallResult
import com.gamer.community.api.FantasyPetPackageFileDto
import com.gamer.community.api.FantasyPetPackageImportDraftRequestDto
import com.gamer.community.api.FantasyPetPackageManifestDto
import java.io.File
import java.util.zip.ZipFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class FantasyPetPackageImportRequestBuilder {
    fun buildRequest(
        packageFile: File,
        targetDownloadId: String,
        ownershipClaimId: String = ""
    ): ApiCallResult<FantasyPetPackageImportDraftRequestDto> {
        val packageFileName = packageFile.name.trim()
        if (!packageFileName.isSafePackageFileName()) {
            return ApiCallResult.Failure("package_file_name_must_be_safe")
        }
        if (!packageFile.isFile || packageFile.length() <= 0L) {
            return ApiCallResult.Failure("package_file_unavailable")
        }

        val normalizedTargetDownloadId = targetDownloadId.trim()
        if (!normalizedTargetDownloadId.isOpaquePublicToken()) {
            return ApiCallResult.Failure("target_download_id_required")
        }

        val normalizedOwnershipClaimId = ownershipClaimId.trim()
        if (
            normalizedOwnershipClaimId.isNotBlank() &&
            !normalizedOwnershipClaimId.isOpaquePublicToken()
        ) {
            return ApiCallResult.Failure("ownership_claim_id_must_be_safe")
        }

        val manifest = when (val manifestResult = readPackageManifest(packageFile)) {
            is ApiCallResult.Success -> manifestResult.value
            is ApiCallResult.Failure -> return manifestResult
        }
        val validationFailure = manifest.validationFailure(normalizedTargetDownloadId)
        if (validationFailure.isNotBlank()) {
            return ApiCallResult.Failure(validationFailure)
        }

        val safeFiles = manifest.files.map { file ->
            FantasyPetPackageFileDto(
                kind = file.kind.trim(),
                path = file.path.trim()
            )
        }

        return ApiCallResult.Success(
            FantasyPetPackageImportDraftRequestDto(
                packageManifest = FantasyPetPackageManifestDto(
                    runId = manifest.runId.trim(),
                    appJobId = manifest.appJobId.trim(),
                    acceptedBy = manifest.acceptedBy.trim(),
                    sourceDownloadId = manifest.sourceDownloadId.trim(),
                    sourceTaskId = manifest.sourceTaskId.trim().safeOptionalPublicToken(),
                    files = safeFiles
                ),
                packageFileName = packageFileName,
                packageByteCount = packageFile.length(),
                targetDownloadId = normalizedTargetDownloadId,
                ownershipClaimId = normalizedOwnershipClaimId
            )
        )
    }

    private fun readPackageManifest(
        packageFile: File
    ): ApiCallResult<FantasyPetPackageManifestInputDto> =
        try {
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry(PACKAGE_MANIFEST_NAME)
                    ?: return ApiCallResult.Failure("package_manifest_missing")
                if (entry.size > MAX_PACKAGE_MANIFEST_BYTES) {
                    return ApiCallResult.Failure("package_manifest_too_large")
                }
                val text = zip.getInputStream(entry).use { input ->
                    val bytes = input.readBytes()
                    if (bytes.size > MAX_PACKAGE_MANIFEST_BYTES) {
                        return ApiCallResult.Failure("package_manifest_too_large")
                    }
                    bytes.toString(Charsets.UTF_8)
                }
                ApiCallResult.Success(json.decodeFromString<FantasyPetPackageManifestInputDto>(text))
            }
        } catch (_: Exception) {
            ApiCallResult.Failure("package_manifest_invalid")
        }

    private fun FantasyPetPackageManifestInputDto.validationFailure(
        targetDownloadId: String
    ): String {
        if (schema != PACKAGE_MANIFEST_SCHEMA) {
            return "package_manifest_schema_invalid"
        }
        if (!runId.trim().isOpaquePublicToken() || !appJobId.trim().isOpaquePublicToken()) {
            return "package_manifest_job_identity_required"
        }
        if (acceptedBy.trim() != "human-review") {
            return "package_manifest_must_be_human_reviewed"
        }
        val sourceDownloadId = sourceDownloadId.trim()
        if (sourceDownloadId.isBlank()) {
            return "package_manifest_source_download_id_required"
        }
        if (!sourceDownloadId.isOpaquePublicToken() || sourceDownloadId != targetDownloadId) {
            return "package_manifest_source_download_id_mismatch"
        }
        if (files.isEmpty()) {
            return "package_manifest_files_required"
        }
        if (
            files.any { file ->
                file.kind.trim().isBlank() ||
                    !file.path.trim().isSafePackageRelativePath()
            }
        ) {
            return "package_manifest_files_must_be_safe"
        }
        if (files.none { file -> file.kind.trim() == "candidate" }) {
            return "package_manifest_candidate_file_required"
        }
        return ""
    }

    private companion object {
        const val PACKAGE_MANIFEST_NAME = "package-manifest.json"
        const val PACKAGE_MANIFEST_SCHEMA = "fantasy-pet.package-manifest.v1"
        const val MAX_PACKAGE_MANIFEST_BYTES = 128 * 1024L

        val json = Json {
            ignoreUnknownKeys = true
        }
    }
}

@Serializable
private data class FantasyPetPackageManifestInputDto(
    val schema: String = "",
    val runId: String = "",
    val appJobId: String = "",
    val acceptedBy: String = "",
    val sourceDownloadId: String = "",
    val sourceTaskId: String = "",
    val files: List<FantasyPetPackageFileDto> = emptyList()
)

private val INTERNAL_PACKAGE_MARKERS = listOf(
    "server_run.json",
    "artifact-index.json",
    "resolution-map",
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

private fun String.isOpaquePublicToken(): Boolean {
    val trimmed = trim()
    val lower = trimmed.lowercase()
    return trimmed.isNotBlank() &&
        !lower.startsWith("file:") &&
        !Regex("^[A-Za-z]:[\\\\/]").containsMatchIn(trimmed) &&
        !trimmed.contains("/") &&
        !trimmed.contains("\\") &&
        !trimmed.contains(":") &&
        INTERNAL_PACKAGE_MARKERS.none { marker -> lower.contains(marker) }
}

private fun String.safeOptionalPublicToken(): String =
    trim().takeIf { it.isBlank() || it.isOpaquePublicToken() }.orEmpty()

private fun String.isSafePackageFileName(): Boolean {
    val trimmed = trim()
    val lower = trimmed.lowercase()
    return lower.endsWith(".zip") &&
        trimmed.isOpaquePublicToken() &&
        INTERNAL_PACKAGE_MARKERS.none { marker -> lower.contains(marker) }
}

private fun String.isSafePackageRelativePath(): Boolean {
    val trimmed = trim()
    val lower = trimmed.lowercase()
    val segments = trimmed.split("/")
    return trimmed.isNotBlank() &&
        !lower.startsWith("file:") &&
        !Regex("^[A-Za-z]:[\\\\/]").containsMatchIn(trimmed) &&
        !trimmed.startsWith("/") &&
        !trimmed.contains("\\") &&
        !trimmed.contains(":") &&
        !segments.includesParentTraversal() &&
        INTERNAL_PACKAGE_MARKERS.none { marker -> lower.contains(marker) }
}

private fun List<String>.includesParentTraversal(): Boolean =
    any { it == ".." }
