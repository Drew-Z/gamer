package com.gamer.community.generation

import com.gamer.community.api.ApiCallResult
import com.gamer.community.api.FantasyPetPackageImportDraftRequestDto
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FantasyPetPackageImportRequestBuilderTest {
    @Test
    fun buildRequestReadsPublicPackageManifestFromDownloadedZip() {
        val packageFile = createPackageZip(
            """
            {
              "schema": "fantasy-pet.package-manifest.v1",
              "runId": "run-public-lifecycle-smoke",
              "appJobId": "public-lifecycle-smoke",
              "sourceOutput": "D:/workspace4Codex/fantasy-pet-rule/runs/job/tasks/output.json",
              "acceptedBy": "human-review",
              "sourceDownloadId": "artifact-1",
              "sourceTaskId": "codex-worker-task",
              "files": [
                {
                  "kind": "candidate",
                  "path": "artifacts/candidates/final-preview.png"
                }
              ]
            }
            """.trimIndent()
        )

        val result = FantasyPetPackageImportRequestBuilder().buildRequest(
            packageFile = packageFile,
            targetDownloadId = "artifact-1",
            ownershipClaimId = "claim-public-lifecycle-smoke"
        )

        assertTrue(result is ApiCallResult.Success<*>)
        val request = (result as ApiCallResult.Success<FantasyPetPackageImportDraftRequestDto>).value
        assertEquals("pet-public-lifecycle-smoke.zip", request.packageFileName)
        assertTrue(request.packageByteCount > 0)
        assertEquals("artifact-1", request.targetDownloadId)
        assertEquals("claim-public-lifecycle-smoke", request.ownershipClaimId)
        assertEquals("fantasy-pet.package-manifest.v1", request.packageManifest.schema)
        assertEquals("run-public-lifecycle-smoke", request.packageManifest.runId)
        assertEquals("public-lifecycle-smoke", request.packageManifest.appJobId)
        assertEquals("human-review", request.packageManifest.acceptedBy)
        assertEquals("artifact-1", request.packageManifest.sourceDownloadId)
        assertEquals("codex-worker-task", request.packageManifest.sourceTaskId)
        assertEquals("candidate", request.packageManifest.files.single().kind)
        assertEquals(
            "artifacts/candidates/final-preview.png",
            request.packageManifest.files.single().path
        )
        assertFalse(request.toString().contains("sourceOutput"))
        assertFalse(request.toString().contains("workspace4Codex"))
    }

    @Test
    fun buildRequestRejectsUnsafeManifestFilePathsWithoutEchoingThem() {
        val packageFile = createPackageZip(
            """
            {
              "schema": "fantasy-pet.package-manifest.v1",
              "runId": "run-public-lifecycle-smoke",
              "appJobId": "public-lifecycle-smoke",
              "acceptedBy": "human-review",
              "sourceDownloadId": "artifact-1",
              "files": [
                {
                  "kind": "candidate",
                  "path": "D:/workspace4Codex/fantasy-pet-rule/runs/job/output.png"
                }
              ]
            }
            """.trimIndent()
        )

        val result = FantasyPetPackageImportRequestBuilder().buildRequest(
            packageFile = packageFile,
            targetDownloadId = "artifact-1"
        )

        assertEquals(ApiCallResult.Failure("package_manifest_files_must_be_safe"), result)
        assertFalse(result.toString().contains("workspace4Codex"))
    }

    @Test
    fun buildRequestRejectsCurrentHandoffInternalArtifactsWithoutEchoingThem() {
        val packageFile = createPackageZip(
            """
            {
              "schema": "fantasy-pet.package-manifest.v1",
              "runId": "run-public-lifecycle-smoke",
              "appJobId": "public-lifecycle-smoke",
              "acceptedBy": "human-review",
              "sourceDownloadId": "artifact-1",
              "files": [
                {
                  "kind": "candidate",
                  "path": "artifacts/candidates/final-preview.png"
                },
                {
                  "kind": "metadata",
                  "path": "review/human-feedback-context.json"
                },
                {
                  "kind": "metadata",
                  "path": "tasks/codex-worker-task.output.json"
                },
                {
                  "kind": "metadata",
                  "path": "orchestration/codex-generation-directives.json"
                },
                {
                  "kind": "metadata",
                  "path": "reports/server-generation-learning-drill.json"
                }
              ]
            }
            """.trimIndent()
        )

        val result = FantasyPetPackageImportRequestBuilder().buildRequest(
            packageFile = packageFile,
            targetDownloadId = "artifact-1"
        )

        assertEquals(ApiCallResult.Failure("package_manifest_files_must_be_safe"), result)
        assertFalse(result.toString().contains("human-feedback-context"))
        assertFalse(result.toString().contains("codex-worker-task"))
        assertFalse(result.toString().contains("codex-generation-directives"))
        assertFalse(result.toString().contains("learning-drill"))
    }

    @Test
    fun buildRequestRejectsCurrentLedgerInternalArtifactsWithoutEchoingThem() {
        val packageFile = createPackageZip(
            """
            {
              "schema": "fantasy-pet.package-manifest.v1",
              "runId": "run-public-lifecycle-smoke",
              "appJobId": "public-lifecycle-smoke",
              "acceptedBy": "human-review",
              "sourceDownloadId": "artifact-1",
              "files": [
                {
                  "kind": "candidate",
                  "path": "artifacts/candidates/final-preview.png"
                },
                {
                  "kind": "metadata",
                  "path": "learning-ledger.jsonl"
                },
                {
                  "kind": "metadata",
                  "path": "route-policy-decision.json"
                },
                {
                  "kind": "metadata",
                  "path": "ledger-suggestions/genericagent-ledger-suggestions.json"
                },
                {
                  "kind": "metadata",
                  "path": "ledger-suggestions/genericagent-ledger-import.json"
                },
                {
                  "kind": "metadata",
                  "path": "review/stage-gate-ledger-import.json"
                }
              ]
            }
            """.trimIndent()
        )

        val result = FantasyPetPackageImportRequestBuilder().buildRequest(
            packageFile = packageFile,
            targetDownloadId = "artifact-1"
        )

        assertEquals(ApiCallResult.Failure("package_manifest_files_must_be_safe"), result)
        assertFalse(result.toString().contains("learning-ledger"))
        assertFalse(result.toString().contains("route-policy"))
        assertFalse(result.toString().contains("genericagent-ledger"))
        assertFalse(result.toString().contains("stage-gate-ledger"))
    }

    @Test
    fun buildRequestRejectsInternalAuditTraceFieldsWithoutEchoingThem() {
        val packageFile = createPackageZip(
            """
            {
              "schema": "fantasy-pet.package-manifest.v1",
              "runId": "run-public-lifecycle-smoke",
              "appJobId": "public-lifecycle-smoke",
              "acceptedBy": "human-review",
              "sourceDownloadId": "artifact-1",
              "files": [
                {
                  "kind": "candidate",
                  "path": "artifacts/candidates/final-preview.png"
                },
                {
                  "kind": "metadata",
                  "path": "review/learningMemoryResponse.json"
                },
                {
                  "kind": "metadata",
                  "path": "review/repairStrategiesUsed.json"
                }
              ]
            }
            """.trimIndent()
        )

        val result = FantasyPetPackageImportRequestBuilder().buildRequest(
            packageFile = packageFile,
            targetDownloadId = "artifact-1"
        )

        assertEquals(ApiCallResult.Failure("package_manifest_files_must_be_safe"), result)
        assertFalse(result.toString().contains("learningMemoryResponse"))
        assertFalse(result.toString().contains("repairStrategiesUsed"))
    }

    @Test
    fun buildRequestRejectsInternalLearningMemoryFieldPathsWithoutEchoingThem() {
        val packageFile = createPackageZip(
            """
            {
              "schema": "fantasy-pet.package-manifest.v1",
              "runId": "run-public-lifecycle-smoke",
              "appJobId": "public-lifecycle-smoke",
              "acceptedBy": "human-review",
              "sourceDownloadId": "artifact-1",
              "files": [
                {
                  "kind": "candidate",
                  "path": "artifacts/candidates/final-preview.png"
                },
                {
                  "kind": "metadata",
                  "path": "review/priorMemoryPresent.json"
                }
              ]
            }
            """.trimIndent()
        )

        val result = FantasyPetPackageImportRequestBuilder().buildRequest(
            packageFile = packageFile,
            targetDownloadId = "artifact-1"
        )

        assertEquals(ApiCallResult.Failure("package_manifest_files_must_be_safe"), result)
        assertFalse(result.toString().contains("priorMemoryPresent"))
    }

    @Test
    fun buildRequestRejectsPackagesWithoutHumanReviewAccept() {
        val packageFile = createPackageZip(
            """
            {
              "schema": "fantasy-pet.package-manifest.v1",
              "runId": "run-public-lifecycle-smoke",
              "appJobId": "public-lifecycle-smoke",
              "acceptedBy": "genericagent-review",
              "sourceDownloadId": "artifact-1",
              "files": [
                {
                  "kind": "candidate",
                  "path": "artifacts/candidates/final-preview.png"
                }
              ]
            }
            """.trimIndent()
        )

        val result = FantasyPetPackageImportRequestBuilder().buildRequest(
            packageFile = packageFile,
            targetDownloadId = "artifact-1"
        )

        assertEquals(ApiCallResult.Failure("package_manifest_must_be_human_reviewed"), result)
    }

    @Test
    fun buildRequestRequiresReviewTargetToMatchPackageSourceDownloadId() {
        val packageFile = createPackageZip(
            """
            {
              "schema": "fantasy-pet.package-manifest.v1",
              "runId": "run-public-lifecycle-smoke",
              "appJobId": "public-lifecycle-smoke",
              "acceptedBy": "human-review",
              "sourceDownloadId": "artifact-1",
              "files": [
                {
                  "kind": "candidate",
                  "path": "artifacts/candidates/final-preview.png"
                }
              ]
            }
            """.trimIndent()
        )

        val result = FantasyPetPackageImportRequestBuilder().buildRequest(
            packageFile = packageFile,
            targetDownloadId = "artifact-2"
        )

        assertEquals(ApiCallResult.Failure("package_manifest_source_download_id_mismatch"), result)
    }

    @Test
    fun buildRequestRequiresCandidateFileInPackageManifest() {
        val packageFile = createPackageZip(
            """
            {
              "schema": "fantasy-pet.package-manifest.v1",
              "runId": "run-public-lifecycle-smoke",
              "appJobId": "public-lifecycle-smoke",
              "acceptedBy": "human-review",
              "sourceDownloadId": "artifact-1",
              "files": [
                {
                  "kind": "qa",
                  "path": "artifacts/review/report.json"
                }
              ]
            }
            """.trimIndent()
        )

        val result = FantasyPetPackageImportRequestBuilder().buildRequest(
            packageFile = packageFile,
            targetDownloadId = "artifact-1"
        )

        assertEquals(ApiCallResult.Failure("package_manifest_candidate_file_required"), result)
    }

    private fun createPackageZip(manifestJson: String): File {
        val directory = Files.createTempDirectory("fantasy-pet-import-request").toFile()
        val packageFile = File(directory, "pet-public-lifecycle-smoke.zip")
        ZipOutputStream(packageFile.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("package-manifest.json"))
            zip.write(manifestJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return packageFile
    }
}
