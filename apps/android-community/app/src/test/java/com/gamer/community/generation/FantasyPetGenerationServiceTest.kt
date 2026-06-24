package com.gamer.community.generation

import com.gamer.community.api.ApiCallResult
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FantasyPetGenerationServiceTest {
    @Test
    fun createJobUsesAppSchemaAndHttpReferences() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)

        val result = service.createJob(
            description = "tiny stardust dragon",
            appJobId = "job-123",
            bodyShape = "wide-tail",
            referencesText = "https://example.com/ref.png"
        )

        assertTrue(result is ApiCallResult.Success)
        assertEquals("fantasy-pet.app-job-create-request.v1", fakeClient.createdRequest?.schema)
        assertEquals("tiny stardust dragon", fakeClient.createdRequest?.description)
        assertEquals("job-123", fakeClient.createdRequest?.appJobId)
        assertEquals("wide-tail", fakeClient.createdRequest?.bodyShape)
        assertEquals(listOf("https://example.com/ref.png"), fakeClient.createdRequest?.references)
    }

    @Test
    fun createJobRejectsFileReferences() = runTest {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())

        val result = service.createJob(
            description = "tiny stardust dragon",
            referencesText = "file://C:/secret/ref.png"
        )

        assertEquals(ApiCallResult.Failure("reference_urls_must_be_http_or_https"), result)
    }

    @Test
    fun createJobRejectsUnsafeAppJobIds() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)

        val result = service.createJob(
            description = "tiny stardust dragon",
            appJobId = "C:/secret/runs/job-123/server_run.json"
        )

        assertEquals(ApiCallResult.Failure("invalid_app_job_id"), result)
        assertEquals(null, fakeClient.createdRequest)
    }

    @Test
    fun createJobRejectsReferenceUrlsWithoutHost() = runTest {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())

        val result = service.createJob(
            description = "tiny stardust dragon",
            referencesText = "https://"
        )

        assertEquals(ApiCallResult.Failure("reference_urls_must_be_http_or_https"), result)
    }

    @Test
    fun createJobAcceptsUppercaseHttpReferenceSchemes() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)

        val result = service.createJob(
            description = "tiny stardust dragon",
            referencesText = "HTTPS://example.com/ref.png"
        )

        assertTrue(result is ApiCallResult.Success)
        assertEquals(listOf("HTTPS://example.com/ref.png"), fakeClient.createdRequest?.references)
    }

    @Test
    fun createJobRejectsMoreThanEightReferences() = runTest {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val references = (1..9).joinToString("\n") { index ->
            "https://example.com/ref-$index.png"
        }

        val result = service.createJob(
            description = "tiny stardust dragon",
            referencesText = references
        )

        assertEquals(ApiCallResult.Failure("too_many_reference_urls"), result)
    }

    @Test
    fun candidateGalleryItemsHideInternalPathsAndFilterNonCandidates() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "artifact-1",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1",
                    actionId = "idle-breathe",
                    label = "C:/secret/runs/job-123/output.png"
                ),
                PetGenerationArtifactDto(
                    kind = "qa",
                    downloadId = "artifact-2",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-2"
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(1, items.size)
        assertEquals("artifact-1", items[0].targetDownloadId)
        assertEquals(
            "http://127.0.0.1:8765/pet-generation-jobs/job-123/artifacts/artifact-1",
            items[0].previewUrl
        )
        assertEquals("Candidate 1", items[0].title)
        assertEquals("idle-breathe", items[0].actionId)
        assertFalse(items[0].title.contains("C:/secret"))
    }

    @Test
    fun candidateGalleryItemsHideUnsafeActionIds() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "artifact-1",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1",
                    actionId = "C:/secret/runs/job-123/idle.png"
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(emptyList<CandidateGalleryItem>(), items)
    }

    @Test
    fun candidateGalleryItemsUseDisplayOrderInsteadOfOpaqueDownloadIds() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "artifact-secret-a",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-secret-a"
                ),
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "artifact-secret-b",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-secret-b"
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(listOf("Candidate 1", "Candidate 2"), items.map { it.title })
        assertFalse(items.any { item -> item.title.contains("artifact-secret") })
    }

    @Test
    fun candidateGalleryItemsDoNotUseInternalLookingDownloadUrls() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "artifact-1",
                    downloadUrl = "C:/secret/runs/job-123/output.png"
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(1, items.size)
        assertEquals(
            "http://127.0.0.1:8765/pet-generation-jobs/job-123/artifacts/artifact-1",
            items[0].previewUrl
        )
        assertFalse(items[0].previewUrl.contains("C:/secret"))
    }

    @Test
    fun candidateGalleryItemsHideInternalLookingDownloadIds() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "C:/secret/runs/job-123/output.png",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1"
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(emptyList<CandidateGalleryItem>(), items)
    }

    @Test
    fun candidateGalleryItemsHideInternalCasebookAuditArtifacts() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "audit-1",
                    label = "review/desktop-pet-casebook-audit.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/audit-1"
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(emptyList<CandidateGalleryItem>(), items)
    }

    @Test
    fun candidateGalleryItemsHideInternalLearningMemoryArtifacts() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "memory-1",
                    label = "desktop-pet-learning-memory.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/memory-1"
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(emptyList<CandidateGalleryItem>(), items)
    }

    @Test
    fun candidateGalleryItemsHideInternalLearningDrillArtifacts() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "learning-drill-1",
                    label = "runs/server-generation-learning-drill.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/learning-drill-1"
                ),
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "regression-report-1",
                    label = "runs/server-generation-regression-report.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/regression-report-1"
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(emptyList<CandidateGalleryItem>(), items)
    }

    @Test
    fun candidateGalleryItemsHideInternalLedgerAndRouteArtifacts() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "learning-ledger-1",
                    label = "learning-ledger.jsonl",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/learning-ledger-1"
                ),
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "route-policy-1",
                    label = "route-policy-decision.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/route-policy-1"
                ),
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "ledger-suggestion-1",
                    label = "ledger-suggestions/genericagent-ledger-suggestions.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/ledger-suggestion-1"
                ),
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "stage-gate-ledger-1",
                    label = "review/stage-gate-ledger-import.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/stage-gate-ledger-1"
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(emptyList<CandidateGalleryItem>(), items)
    }

    @Test
    fun candidateGalleryItemsHideInternalStageGateReportArtifacts() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "stage-gate-1",
                    label = "review/desktop-pet-stage-gate-report.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/stage-gate-1"
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(emptyList<CandidateGalleryItem>(), items)
    }

    @Test
    fun candidateGalleryItemsHideInternalGenericAgentReviewArtifacts() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "agent-review-1",
                    label = "review/agent-review.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/agent-review-1"
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(emptyList<CandidateGalleryItem>(), items)
    }

    @Test
    fun candidateGalleryItemsPreferRuntimeActionReviewPlaybackOverSourceCandidate() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "artifact-1",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1",
                    actionId = "idle",
                    reviewStage = "source-candidate-review",
                    previewKind = "source-candidate-image",
                    mediaType = "image/png"
                ),
                PetGenerationArtifactDto(
                    kind = "review",
                    downloadId = "artifact-11",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-11",
                    actionId = "idle",
                    reviewStage = "runtime-action-review",
                    previewKind = "runtime-action-playback",
                    mediaType = "text/html",
                    frameCount = 6,
                    fps = 8
                )
            )
        )

        val items = service.candidateGalleryItems(job)

        assertEquals(1, items.size)
        assertEquals("artifact-11", items.single().targetDownloadId)
        assertEquals("runtime-action-review", items.single().reviewStage)
        assertEquals("runtime-action-playback", items.single().previewKind)
        assertEquals("text/html", items.single().mediaType)
        assertEquals(6, items.single().frameCount)
        assertEquals(8, items.single().fps)
        assertEquals(
            "http://127.0.0.1:8765/pet-generation-jobs/job-123/artifacts/artifact-11",
            items.single().previewUrl
        )
    }

    @Test
    fun refreshJobArtifactsFetchesPublicArtifactIndexWhenResponseHasNoArtifacts() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient(
            artifactResponse = PetGenerationArtifactIndexResponseDto(
                appJobId = "job-123",
                artifacts = listOf(
                    PetGenerationArtifactDto(
                        kind = "candidate",
                        downloadId = "artifact-1",
                        downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1"
                    )
                )
            )
        )
        val service = FantasyPetGenerationService(fakeClient)

        val result = service.refreshJobArtifacts(
            PetGenerationJobResponseDto(
                appJobId = "job-123",
                progressStatus = "waiting-for-review",
                nextAction = "human-review",
                artifactCount = 1,
                artifacts = emptyList()
            )
        )

        assertTrue(result is ApiCallResult.Success)
        val refreshedJob = (result as ApiCallResult.Success<PetGenerationJobResponseDto>).value
        assertEquals("job-123", fakeClient.artifactFetchJobId)
        assertEquals("artifact-1", refreshedJob.artifacts.first().downloadId)
    }

    @Test
    fun refreshJobArtifactsFetchesPublicArtifactIndexWhenResponseHasOnlyPartialNonCandidateArtifacts() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient(
            artifactResponse = PetGenerationArtifactIndexResponseDto(
                appJobId = "job-123",
                artifacts = listOf(
                    PetGenerationArtifactDto(
                        kind = "qa",
                        downloadId = "qa-1",
                        downloadUrl = "/pet-generation-jobs/job-123/artifacts/qa-1"
                    ),
                    PetGenerationArtifactDto(
                        kind = "candidate",
                        downloadId = "artifact-1",
                        downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1"
                    )
                )
            )
        )
        val service = FantasyPetGenerationService(fakeClient)

        val result = service.refreshJobArtifacts(
            PetGenerationJobResponseDto(
                appJobId = "job-123",
                progressStatus = "waiting-for-review",
                nextAction = "human-review",
                artifactCount = 2,
                artifacts = listOf(
                    PetGenerationArtifactDto(
                        kind = "qa",
                        downloadId = "qa-1",
                        downloadUrl = "/pet-generation-jobs/job-123/artifacts/qa-1"
                    )
                )
            )
        )

        assertTrue(result is ApiCallResult.Success)
        val refreshedJob = (result as ApiCallResult.Success<PetGenerationJobResponseDto>).value
        assertEquals("job-123", fakeClient.artifactFetchJobId)
        assertEquals(listOf("qa-1", "artifact-1"), refreshedJob.artifacts.map { it.downloadId })
        assertEquals("artifact-1", service.candidateGalleryItems(refreshedJob).single().targetDownloadId)
    }

    @Test
    fun generationProgressMessageUsesSafeProgressMessage() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())

        assertEquals(
            "Ready for human review",
            service.generationProgressMessage(
                PetGenerationJobResponseDto(
                    progressStatus = "waiting-for-review",
                    nextAction = "human-review",
                    generationProgress = PetGenerationProgressDto(
                        currentStage = "human-review",
                        message = "GenericAgent review finished; waiting for human review."
                    )
                )
            )
        )

        assertEquals(
            "Ready for human review",
            service.generationProgressMessage(
                PetGenerationJobResponseDto(
                    progressStatus = "waiting-for-review",
                    nextAction = "human-review",
                    generationProgress = PetGenerationProgressDto(
                        currentStage = "human-review",
                        message = "C:/secret/runs/job-123/output.png"
                    )
                )
            )
        )

        assertEquals(
            "Ready for human review",
            service.generationProgressMessage(
                PetGenerationJobResponseDto(
                    progressStatus = "waiting-for-review",
                    nextAction = "human-review",
                    generationProgress = PetGenerationProgressDto(
                        currentStage = "human-review",
                        message = "runs\\job-123\\output.png"
                    )
                )
            )
        )
    }

    @Test
    fun generationProgressMessageFallsBackToCreateResponseStatus() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())

        assertEquals(
            "Queued",
            service.generationProgressMessage(
                PetGenerationJobResponseDto(
                    status = "queued",
                    progressStatus = "",
                    nextAction = "wait"
                )
            )
        )
    }

    @Test
    fun generationProgressMessageShowsSafePublicErrorsForFailedJobs() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())

        assertEquals(
            "Failed: Candidate render timed out",
            service.generationProgressMessage(
                PetGenerationJobResponseDto(
                    progressStatus = "failed",
                    nextAction = "wait",
                    errors = listOf(
                        "Candidate render timed out",
                        "C:/secret/runs/job-123/server_run.json"
                    )
                )
            )
        )
    }

    @Test
    fun generationProgressMessageBlocksUnsafeProgressSecurityReports() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val unsafeProgressJob = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "processing",
            nextAction = "wait",
            generationProgress = PetGenerationProgressDto(
                currentStage = "planning",
                message = "Generating assets.",
                steps = listOf(
                    PetGenerationProgressStepDto(
                        id = "planning",
                        label = "Planning",
                        status = "active",
                        message = "Worker command prepared."
                    )
                ),
                security = PetGenerationProgressSecurityDto(
                    exposesInternalPaths = true,
                    exposesWorkerCommands = true
                )
            )
        )

        assertEquals(
            "Generation status blocked: unsafe progress report.",
            service.generationProgressMessage(unsafeProgressJob)
        )
        assertEquals(
            emptyList<GenerationProgressStepItem>(),
            service.generationProgressStepItems(unsafeProgressJob)
        )
    }

    @Test
    fun generationProgressMessageHidesInternalAuditTraceFields() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "processing",
            nextAction = "wait",
            generationProgress = PetGenerationProgressDto(
                message = "learningMemoryResponse covered repeated failures",
                steps = listOf(
                    PetGenerationProgressStepDto(
                        id = "ga-review",
                        label = "repairStrategiesUsed",
                        status = "active",
                        message = "casebookReferencesUsed matched stage gate"
                    )
                )
            )
        )

        assertEquals("Generating", service.generationProgressMessage(job))

        val items = service.generationProgressStepItems(job)
        assertEquals(1, items.size)
        assertEquals("ga-review", items[0].label)
        assertEquals("active", items[0].status)
        assertEquals("", items[0].message)
    }

    @Test
    fun generationProgressStepItemsHideInternalMessages() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            generationProgress = PetGenerationProgressDto(
                steps = listOf(
                    PetGenerationProgressStepDto(
                        id = "planning",
                        label = "Planning",
                        status = "complete",
                        message = "Plan ready"
                    ),
                    PetGenerationProgressStepDto(
                        id = "codex-generation",
                        label = "Codex generation",
                        status = "active",
                        message = "runs\\job-123\\output.png"
                    ),
                    PetGenerationProgressStepDto(
                        id = "genericagent-orchestration",
                        label = "GenericAgent orchestration",
                        status = "pending",
                        message = "GenericAgent strategy and Codex directives."
                    ),
                    PetGenerationProgressStepDto(
                        id = "route-planning",
                        label = "Route and task planning",
                        status = "active",
                        message = "Route policy and task packets are prepared."
                    )
                )
            )
        )

        val items = service.generationProgressStepItems(job)

        assertEquals(4, items.size)
        assertEquals("Planning", items[0].label)
        assertEquals("complete", items[0].status)
        assertEquals("Plan ready", items[0].message)
        assertEquals("Candidate generation", items[1].label)
        assertEquals("active", items[1].status)
        assertEquals("", items[1].message)
        assertEquals("Generation orchestration", items[2].label)
        assertEquals("pending", items[2].status)
        assertEquals("", items[2].message)
        assertEquals("Planning", items[3].label)
        assertEquals("active", items[3].status)
        assertEquals("", items[3].message)
        assertFalse(items.any { item ->
            item.label.contains("GenericAgent") ||
                item.label.contains("Codex") ||
                item.message.contains("GenericAgent") ||
                item.message.contains("Codex") ||
                item.message.contains("directives", ignoreCase = true) ||
                item.message.contains("route policy", ignoreCase = true)
        })
    }

    @Test
    fun reviewDecisionUsesTargetDownloadIdAndHumanReviewer() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)

        val result = service.submitReviewDecision(
            appJobId = "job-123",
            targetDownloadId = "artifact-1",
            decision = "accept",
            notesText = ""
        )

        assertTrue(result is ApiCallResult.Success)
        assertEquals("fantasy-pet.review-decision.v1", fakeClient.reviewRequest?.schema)
        assertEquals("human-review", fakeClient.reviewRequest?.reviewer)
        assertEquals("accept", fakeClient.reviewRequest?.decision)
        assertEquals("artifact-1", fakeClient.reviewRequest?.targetDownloadId)
        assertEquals("human-review", fakeClient.reviewRequest?.stage)
    }

    @Test
    fun reviewDecisionRejectsContractDemoJobId() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)

        val result = service.submitReviewDecision(
            appJobId = "public-lifecycle-smoke",
            targetDownloadId = "artifact-1",
            decision = "accept",
            notesText = ""
        )

        assertEquals(ApiCallResult.Failure("contract_demo_job_review_disabled"), result)
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun reviewDecisionForJobRejectsNonCandidateTarget() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "qa",
                    downloadId = "artifact-qa",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-qa"
                )
            )
        )

        val result = service.submitReviewDecisionForJob(
            job = job,
            targetDownloadId = "artifact-qa",
            decision = "accept",
            notesText = ""
        )

        assertEquals(ApiCallResult.Failure("review_target_must_be_candidate"), result)
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun reviewDecisionForJobRejectsPreviouslyReviewedCandidateTarget() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "artifact-1",
                    reviewDecision = "revise",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1"
                )
            )
        )

        val result = service.submitReviewDecisionForJob(
            job = job,
            targetDownloadId = "artifact-1",
            decision = "accept",
            notesText = ""
        )

        assertEquals(ApiCallResult.Failure("review_target_already_decided"), result)
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun reviewDecisionForJobUsesRuntimeActionReviewStage() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "review",
                    downloadId = "artifact-11",
                    actionId = "idle",
                    reviewStage = "runtime-action-review",
                    previewKind = "runtime-action-playback",
                    mediaType = "text/html",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-11"
                )
            )
        )

        val result = service.submitReviewDecisionForJob(
            job = job,
            targetDownloadId = "artifact-11",
            decision = "accept",
            notesText = ""
        )

        assertTrue(result is ApiCallResult.Success)
        assertEquals("artifact-11", fakeClient.reviewRequest?.targetDownloadId)
        assertEquals("runtime-action-review", fakeClient.reviewRequest?.stage)
    }

    @Test
    fun reviewDecisionForJobRejectsContractDemoJob() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)
        val job = PetGenerationJobResponseDto(
            appJobId = "public-lifecycle-smoke",
            runId = "public-lifecycle-smoke",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "artifact-1",
                    downloadUrl = "/pet-generation-jobs/public-lifecycle-smoke/artifacts/artifact-1"
                )
            )
        )

        val result = service.submitReviewDecisionForJob(
            job = job,
            targetDownloadId = "artifact-1",
            decision = "accept",
            notesText = ""
        )

        assertEquals(ApiCallResult.Failure("contract_demo_job_review_disabled"), result)
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun reviewDecisionForJobRejectsInternalCasebookAuditTarget() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "audit-1",
                    label = "review/desktop-pet-casebook-audit.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/audit-1"
                )
            )
        )

        val result = service.submitReviewDecisionForJob(
            job = job,
            targetDownloadId = "audit-1",
            decision = "accept",
            notesText = ""
        )

        assertEquals(ApiCallResult.Failure("review_target_must_be_candidate"), result)
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun reviewDecisionForJobRejectsInternalLearningMemoryTarget() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "memory-1",
                    label = "desktop-pet-learning-memory.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/memory-1"
                )
            )
        )

        val result = service.submitReviewDecisionForJob(
            job = job,
            targetDownloadId = "memory-1",
            decision = "accept",
            notesText = ""
        )

        assertEquals(ApiCallResult.Failure("review_target_must_be_candidate"), result)
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun reviewDecisionForJobRejectsInternalLearningDrillTarget() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "learning-drill-1",
                    label = "runs/server-generation-learning-drill.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/learning-drill-1"
                )
            )
        )

        val result = service.submitReviewDecisionForJob(
            job = job,
            targetDownloadId = "learning-drill-1",
            decision = "accept",
            notesText = ""
        )

        assertEquals(ApiCallResult.Failure("review_target_must_be_candidate"), result)
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun reviewDecisionForJobRejectsInternalLedgerTarget() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "ledger-suggestion-1",
                    label = "ledger-suggestions/genericagent-ledger-import.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/ledger-suggestion-1"
                ),
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "stage-gate-ledger-1",
                    label = "review/stage-gate-ledger-import.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/stage-gate-ledger-1"
                )
            )
        )

        val result = service.submitReviewDecisionForJob(
            job = job,
            targetDownloadId = "stage-gate-ledger-1",
            decision = "accept",
            notesText = ""
        )

        assertEquals(ApiCallResult.Failure("review_target_must_be_candidate"), result)
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun reviewDecisionForJobRejectsInternalLookingDownloadIdTarget() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "C:/secret/runs/job-123/output.png",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1"
                )
            )
        )

        val result = service.submitReviewDecisionForJob(
            job = job,
            targetDownloadId = "C:/secret/runs/job-123/output.png",
            decision = "accept",
            notesText = ""
        )

        assertEquals(ApiCallResult.Failure("review_target_must_be_candidate"), result)
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun reviewDecisionForJobRejectsInternalStageGateReportTarget() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "stage-gate-1",
                    label = "review/desktop-pet-stage-gate-report.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/stage-gate-1"
                )
            )
        )

        val result = service.submitReviewDecisionForJob(
            job = job,
            targetDownloadId = "stage-gate-1",
            decision = "accept",
            notesText = ""
        )

        assertEquals(ApiCallResult.Failure("review_target_must_be_candidate"), result)
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun reviewDecisionForJobRejectsInternalGenericAgentReviewTarget() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "agent-review-1",
                    label = "review/agent-review.json",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/agent-review-1"
                )
            )
        )

        val result = service.submitReviewDecisionForJob(
            job = job,
            targetDownloadId = "agent-review-1",
            decision = "accept",
            notesText = ""
        )

        assertEquals(ApiCallResult.Failure("review_target_must_be_candidate"), result)
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun reviseAndRejectRequireHumanNotes() = runTest {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())

        assertEquals(
            ApiCallResult.Failure("review_notes_required"),
            service.submitReviewDecision("job-123", "artifact-1", "revise", "")
        )
        assertEquals(
            ApiCallResult.Failure("review_notes_required"),
            service.submitReviewDecision("job-123", "artifact-1", "reject", " ")
        )
    }

    @Test
    fun reviewDecisionRejectsInternalPathOrWorkerNotes() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient()
        val service = FantasyPetGenerationService(fakeClient)

        assertEquals(
            ApiCallResult.Failure("review_notes_must_not_include_internal_paths"),
            service.submitReviewDecision(
                appJobId = "job-123",
                targetDownloadId = "artifact-1",
                decision = "revise",
                notesText = "idle pops in C:/secret/runs/job-123/server_run.json"
            )
        )
        assertEquals(
            ApiCallResult.Failure("review_notes_must_not_include_internal_paths"),
            service.submitReviewDecision(
                appJobId = "job-123",
                targetDownloadId = "artifact-1",
                decision = "accept",
                notesText = "looks okay but mentions adapter-config and lease"
            )
        )
        assertEquals(
            ApiCallResult.Failure("review_notes_must_not_include_internal_paths"),
            service.submitReviewDecision(
                appJobId = "job-123",
                targetDownloadId = "artifact-1",
                decision = "revise",
                notesText = "server-proof-summary.json says package is ready"
            )
        )
        assertEquals(null, fakeClient.reviewRequest)
    }

    @Test
    fun packageDownloadRequiresReadyJob() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient(packageBytes = "pet.zip".toByteArray())
        val service = FantasyPetGenerationService(fakeClient)

        assertEquals(
            ApiCallResult.Failure("package_not_ready"),
            service.downloadPackage(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    downloadReady = false,
                    nextAction = "wait"
                )
            )
        )

        val readyResult = service.downloadPackage(
            PetGenerationJobResponseDto(
                appJobId = "job-123",
                downloadReady = true,
                nextAction = "download-package"
            )
        )

        assertTrue(readyResult is ApiCallResult.Success)
        assertEquals("job-123", fakeClient.packageDownloadJobId)

        val nextActionOnlyResult = service.downloadPackage(
            PetGenerationJobResponseDto(
                appJobId = "job-456",
                downloadReady = false,
                nextAction = "download-package"
            )
        )

        assertEquals(ApiCallResult.Failure("package_not_ready"), nextActionOnlyResult)

        val summaryReadyResult = service.downloadPackage(
            PetGenerationJobResponseDto(
                appJobId = "job-789",
                downloadReady = false,
                nextAction = "download-package",
                generationProgress = PetGenerationProgressDto(
                    summary = PetGenerationProgressSummaryDto(downloadReady = true)
                )
            )
        )

        assertTrue(summaryReadyResult is ApiCallResult.Success)
        assertEquals("job-789", fakeClient.packageDownloadJobId)
    }

    @Test
    fun downloadPackageToFileWritesFinalZipWithSafeFileName() = runTest {
        val outputDirectory = Files.createTempDirectory("fantasy-pet-package").toFile()
        val fakeClient = FakeFantasyPetGenerationClient(packageBytes = "pet.zip".toByteArray())
        val service = FantasyPetGenerationService(fakeClient)

        try {
            val result = service.downloadPackageToFile(
                job = PetGenerationJobResponseDto(
                    appJobId = "Job 123/A",
                    downloadReady = true,
                    nextAction = "download-package"
                ),
                outputDirectory = outputDirectory
            )

            assertTrue(result is ApiCallResult.Success)
            val file = (result as ApiCallResult.Success<java.io.File>).value
            assertEquals("pet-Job-123-A.zip", file.name)
            assertEquals("pet.zip", file.readText())
            assertEquals("Job 123/A", fakeClient.packageDownloadJobId)
        } finally {
            outputDirectory.deleteRecursively()
        }
    }

    @Test
    fun packageImportCandidateUsesSafeDownloadedPackageHandoffFields() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())

        val candidate = service.packageImportCandidate(
            job = PetGenerationJobResponseDto(
                appJobId = "job-123",
                downloadReady = true,
                nextAction = "download-package"
            ),
            selectedCandidateDownloadId = "artifact-1",
            packageFileName = "pet-job-123.zip",
            packageByteCount = 664
        )

        assertEquals("job-123", candidate?.appJobId)
        assertEquals("artifact-1", candidate?.targetDownloadId)
        assertEquals("pet-job-123.zip", candidate?.packageFileName)
        assertEquals(664L, candidate?.packageByteCount)
        assertEquals("waiting-for-community-import", candidate?.status)
        assertEquals("Package downloaded; preparing community import draft.", candidate?.summary)
        assertFalse(candidate?.summary.orEmpty().contains("C:/secret"))
        assertFalse(candidate?.summary.orEmpty().contains("targetOutput"))
    }

    @Test
    fun packageImportCandidateRejectsUnsafeOrPrematureHandoffFields() {
        val service = FantasyPetGenerationService(FakeFantasyPetGenerationClient())
        val readyJob = PetGenerationJobResponseDto(
            appJobId = "job-123",
            downloadReady = true,
            nextAction = "download-package"
        )

        assertEquals(
            null,
            service.packageImportCandidate(
                job = readyJob,
                selectedCandidateDownloadId = "C:/secret/runs/job/output.png",
                packageFileName = "pet-job-123.zip",
                packageByteCount = 664
            )
        )
        assertEquals(
            null,
            service.packageImportCandidate(
                job = readyJob,
                selectedCandidateDownloadId = "artifact-1",
                packageFileName = "C:/secret/runs/job/pet.zip",
                packageByteCount = 664
            )
        )
        assertEquals(
            null,
            service.packageImportCandidate(
                job = readyJob.copy(downloadReady = false, nextAction = "wait"),
                selectedCandidateDownloadId = "artifact-1",
                packageFileName = "pet-job-123.zip",
                packageByteCount = 664
            )
        )
    }

    @Test
    fun checkWorkerReadinessUsesPublicClient() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient(
            readinessResponse = WorkerReadinessResponseDto(status = "ready")
        )
        val service = FantasyPetGenerationService(fakeClient)

        val result = service.checkWorkerReadiness()

        assertTrue(result is ApiCallResult.Success)
        assertEquals("ready", (result as ApiCallResult.Success<WorkerReadinessResponseDto>).value.status)
        assertEquals(true, fakeClient.workerReadinessRequested)
    }

    @Test
    fun checkAppApiContractUsesPublicClient() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient(
            appApiContractResponse = PetGenerationAppApiContractDto(
                schema = "fantasy-pet.app-api-contract.v1",
                publicEndpoints = listOf(
                    PetGenerationPublicEndpointDto(
                        method = "POST",
                        path = "/pet-generation-jobs",
                        isPublic = true,
                        requestSchema = "fantasy-pet.app-job-create-request.v1",
                        responseSchema = "fantasy-pet.app-job-create-response.v1"
                    )
                )
            )
        )
        val service = FantasyPetGenerationService(fakeClient)

        val result = service.checkAppApiContract()

        assertTrue(result is ApiCallResult.Success)
        val contract = (result as ApiCallResult.Success<PetGenerationAppApiContractDto>).value
        assertEquals("fantasy-pet.app-api-contract.v1", contract.schema)
        assertEquals("/pet-generation-jobs", contract.publicEndpoints.single().path)
        assertEquals(true, fakeClient.appApiContractRequested)
    }

    @Test
    fun checkGenerationServiceStatusMessageStopsAtUnsafeContract() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient(
            readinessResponse = WorkerReadinessResponseDto(status = "ready"),
            appApiContractResponse = PetGenerationAppApiContractDto(
                schema = "fantasy-pet.app-api-contract.v1",
                security = PetGenerationAppApiContractSecurityDto(
                    exposesInternalPaths = true
                )
            )
        )
        val service = FantasyPetGenerationService(fakeClient)

        val message = service.checkGenerationServiceStatusMessage()

        assertEquals("Generation API contract blocked: unsafe public boundary.", message)
        assertEquals(true, fakeClient.appApiContractRequested)
        assertEquals(false, fakeClient.workerReadinessRequested)
    }

    @Test
    fun checkGenerationServiceStatusMessageCombinesContractAndReadiness() = runTest {
        val fakeClient = FakeFantasyPetGenerationClient(
            appApiContractResponse = PetGenerationAppApiContractDto(
                schema = "fantasy-pet.app-api-contract.v1",
                publicEndpoints = listOf(
                    PetGenerationPublicEndpointDto(
                        method = "POST",
                        path = "/pet-generation-jobs",
                        isPublic = true
                    ),
                    PetGenerationPublicEndpointDto(
                        method = "GET",
                        path = "/worker-readiness",
                        isPublic = true
                    )
                )
            ),
            readinessResponse = WorkerReadinessResponseDto(
                status = "blocked",
                adapters = listOf(
                    WorkerReadinessAdapterDto(
                        adapter = "codex-cli",
                        status = "missing-command"
                    )
                )
            )
        )
        val service = FantasyPetGenerationService(fakeClient)

        val message = service.checkGenerationServiceStatusMessage()

        assertEquals(
            "Generation API contract fantasy-pet.app-api-contract.v1 exposes 2 public endpoints. " +
                "Generation service blocked: codex-cli missing-command.",
            message
        )
        assertEquals(true, fakeClient.appApiContractRequested)
        assertEquals(true, fakeClient.workerReadinessRequested)
    }

    @Test
    fun workerReadinessMessageSummarizesPublicAdapterStatuses() {
        assertEquals(
            "Generation service ready.",
            workerReadinessMessage(WorkerReadinessResponseDto(status = "ready"))
        )

        assertEquals(
            "Generation service blocked: codex-cli disabled; genericagent missing-command.",
            workerReadinessMessage(
                WorkerReadinessResponseDto(
                    status = "blocked",
                    adapters = listOf(
                        WorkerReadinessAdapterDto(
                            adapter = "codex-cli",
                            status = "disabled"
                        ),
                        WorkerReadinessAdapterDto(
                            adapter = "genericagent",
                            status = "missing-command"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun workerReadinessMessageBlocksUnsafeSecurityReports() {
        assertEquals(
            "Generation service blocked: unsafe readiness report.",
            workerReadinessMessage(
                WorkerReadinessResponseDto(
                    status = "ready",
                    security = WorkerReadinessSecurityDto(
                        secretsInReport = true
                    )
                )
            )
        )
        assertEquals(
            "Generation service blocked: unsafe readiness report.",
            workerReadinessMessage(
                WorkerReadinessResponseDto(
                    status = "ready",
                    security = WorkerReadinessSecurityDto(
                        executesAgentProcesses = true,
                        appMayInvokeAgentsDirectly = true,
                        executesReadinessProbe = true
                    )
                )
            )
        )
    }

    @Test
    fun appApiContractMessageSummarizesPublicEndpointCoverageAndBlocksUnsafeContracts() {
        assertEquals(
            "Generation API contract fantasy-pet.app-api-contract.v1 exposes 2 public endpoints.",
            appApiContractMessage(
                PetGenerationAppApiContractDto(
                    schema = "fantasy-pet.app-api-contract.v1",
                    publicEndpoints = listOf(
                        PetGenerationPublicEndpointDto(
                            method = "POST",
                            path = "/pet-generation-jobs",
                            isPublic = true
                        ),
                        PetGenerationPublicEndpointDto(
                            method = "GET",
                            path = "/worker-readiness",
                            isPublic = true
                        ),
                        PetGenerationPublicEndpointDto(
                            method = "POST",
                            path = "/admin/server-worker-cycle",
                            isPublic = false
                        )
                    )
                )
            )
        )

        assertEquals(
            "Generation API contract blocked: unsafe public boundary.",
            appApiContractMessage(
                PetGenerationAppApiContractDto(
                    schema = "fantasy-pet.app-api-contract.v1",
                    security = PetGenerationAppApiContractSecurityDto(
                        exposesInternalPaths = true,
                        exposesWorkerCommands = true,
                        appMayInvokeAgentsDirectly = true,
                        requiresHumanReview = false,
                        adminEndpointsDisabledByDefault = false
                    )
                )
            )
        )
    }
}

private class FakeFantasyPetGenerationClient(
    private val createResponse: PetGenerationJobResponseDto = PetGenerationJobResponseDto(
        appJobId = "job-123",
        status = "queued",
        nextAction = "wait"
    ),
    private val reviewResponse: PetGenerationJobResponseDto = PetGenerationJobResponseDto(
        appJobId = "job-123",
        progressStatus = "packaging",
        nextAction = "processing-package"
    ),
    private val artifactResponse: PetGenerationArtifactIndexResponseDto = PetGenerationArtifactIndexResponseDto(),
    private val packageBytes: ByteArray = ByteArray(0),
    private val readinessResponse: WorkerReadinessResponseDto = WorkerReadinessResponseDto(
        status = "blocked"
    ),
    private val appApiContractResponse: PetGenerationAppApiContractDto = PetGenerationAppApiContractDto()
) : FantasyPetGenerationClient {
    var createdRequest: PetGenerationJobCreateRequestDto? = null
    var reviewRequest: ReviewDecisionRequestDto? = null
    var artifactFetchJobId: String? = null
    var packageDownloadJobId: String? = null
    var workerReadinessRequested: Boolean = false
    var appApiContractRequested: Boolean = false

    override suspend fun createJob(request: PetGenerationJobCreateRequestDto): ApiCallResult<PetGenerationJobResponseDto> {
        createdRequest = request
        return ApiCallResult.Success(createResponse)
    }

    override suspend fun getJob(appJobId: String): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Failure("not_configured")

    override suspend fun getArtifacts(appJobId: String): ApiCallResult<PetGenerationArtifactIndexResponseDto> =
        ApiCallResult.Success(artifactResponse).also {
            artifactFetchJobId = appJobId
        }

    override suspend fun submitReviewDecision(
        appJobId: String,
        request: ReviewDecisionRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> {
        reviewRequest = request
        return ApiCallResult.Success(reviewResponse)
    }

    override suspend fun downloadPackage(appJobId: String): ApiCallResult<ByteArray> {
        packageDownloadJobId = appJobId
        return ApiCallResult.Success(packageBytes)
    }

    override suspend fun getWorkerReadiness(): ApiCallResult<WorkerReadinessResponseDto> =
        ApiCallResult.Success(readinessResponse).also {
            workerReadinessRequested = true
        }

    override suspend fun getAppApiContract(): ApiCallResult<PetGenerationAppApiContractDto> =
        ApiCallResult.Success(appApiContractResponse).also {
            appApiContractRequested = true
        }
}
