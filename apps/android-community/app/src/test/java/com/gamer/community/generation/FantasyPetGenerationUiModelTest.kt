package com.gamer.community.generation

import com.gamer.community.api.ApiCallResult
import com.gamer.community.api.ImportDraftDto
import com.gamer.community.api.ImportDraftSubmissionResponseDto
import com.gamer.community.api.InitialCommunityResult
import com.gamer.community.api.SubmissionDto
import com.gamer.community.petshell.ApprovedPet
import com.gamer.community.petshell.FeedPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FantasyPetGenerationUiModelTest {
    @Test
    fun bodyShapeOptionsMatchFantasyPetContractOrder() {
        assertEquals(
            listOf("balanced", "wide", "wide-tail", "tall"),
            GENERATION_BODY_SHAPE_OPTIONS
        )
    }

    @Test
    fun createJobValidationExplainsDescriptionAndReferenceProblems() {
        assertEquals(
            "Description is required.",
            generationCreateValidationMessage(
                description = "",
                bodyShape = "balanced",
                referencesText = ""
            )
        )
        assertEquals(
            "Reference URLs must use HTTP or HTTPS.",
            generationCreateValidationMessage(
                description = "tiny dragon",
                bodyShape = "balanced",
                referencesText = "file:///C:/secret/ref.png"
            )
        )
        assertEquals(
            "Use at most 8 reference URLs.",
            generationCreateValidationMessage(
                description = "tiny dragon",
                bodyShape = "balanced",
                referencesText = (1..9).joinToString("\n") { index ->
                    "https://example.com/ref-$index.png"
                }
            )
        )
        assertEquals(
            "",
            generationCreateValidationMessage(
                description = "tiny dragon",
                bodyShape = "wide-tail",
                referencesText = "https://example.com/ref.png"
            )
        )
        assertTrue(
            canCreateGenerationJob(
                description = "tiny dragon",
                bodyShape = "wide-tail",
                referencesText = "https://example.com/ref.png"
            )
        )
    }

    @Test
    fun appJobIdValidationBlocksUnsafeCreateAndPollIds() {
        assertEquals(
            "App job id can use letters, numbers, dot, underscore, or dash.",
            generationCreateValidationMessage(
                description = "tiny dragon",
                bodyShape = "balanced",
                referencesText = "",
                appJobId = "C:/secret/runs/job-123/server_run.json"
            )
        )
        assertFalse(
            canCreateGenerationJob(
                description = "tiny dragon",
                bodyShape = "balanced",
                referencesText = "",
                appJobId = "job 123"
            )
        )
        assertTrue(
            canCreateGenerationJob(
                description = "tiny dragon",
                bodyShape = "balanced",
                referencesText = "",
                appJobId = "job-123.preview_a"
            )
        )
        assertFalse(canPollGenerationJob("job 123"))
        assertTrue(canPollGenerationJob("job-123.preview_a"))
        assertEquals(
            "App job id can use letters, numbers, dot, underscore, or dash.",
            pollGenerationJobValidationMessage("https://example.com/job")
        )
    }

    @Test
    fun statusLabelsFollowProgressStatusAndNextAction() {
        assertEquals(
            "Waiting for worker output",
            generationStatusLabel("waiting-for-worker-output", "wait")
        )
        assertEquals(
            "Ready for human review",
            generationStatusLabel("waiting-for-review", "human-review")
        )
        assertEquals(
            "Ready for download",
            generationStatusLabel("", "download-package")
        )
    }

    @Test
    fun generationProgressSummaryLineHighlightsReviewRevisionAndDownloadStates() {
        assertEquals(
            "2 candidates ready for human review.",
            generationProgressSummaryLine(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "waiting-for-review",
                    nextAction = "human-review",
                    generationProgress = PetGenerationProgressDto(
                        summary = PetGenerationProgressSummaryDto(candidateCount = 2)
                    )
                )
            )
        )
        assertEquals(
            "Revision requested; waiting for a revised candidate.",
            generationProgressSummaryLine(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "revision-requested",
                    nextAction = "await-revision"
                )
            )
        )
        assertEquals(
            "pet.zip is ready to download.",
            generationProgressSummaryLine(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "ready-for-download",
                    nextAction = "download-package",
                    downloadReady = true
                )
            )
        )
    }

    @Test
    fun generationServerWorkerWaitNoticeClarifiesThatAppDoesNotStartImageGeneration() {
        assertEquals(
            "Waiting for a trusted server worker; this app only created and polls the job.",
            generationServerWorkerWaitNotice(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "queued",
                    nextAction = "wait"
                )
            )
        )
        assertEquals(
            "Waiting for a trusted server worker; this app only created and polls the job.",
            generationServerWorkerWaitNotice(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "waiting-for-worker-output",
                    nextAction = "wait"
                )
            )
        )
        assertEquals(
            "Feedback recorded; a trusted server worker must publish the next candidate.",
            generationServerWorkerWaitNotice(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "revision-requested",
                    nextAction = "await-revision"
                )
            )
        )
        assertEquals(
            "Feedback recorded; a trusted server worker must publish the next candidate.",
            generationServerWorkerWaitNotice(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "candidate-rejected",
                    nextAction = "await-new-candidate"
                )
            )
        )
        assertEquals(
            "",
            generationServerWorkerWaitNotice(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "waiting-for-review",
                    nextAction = "human-review"
                )
            )
        )
        assertEquals(
            "",
            generationServerWorkerWaitNotice(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "ready-for-download",
                    nextAction = "download-package",
                    downloadReady = true
                )
            )
        )
    }

    @Test
    fun packageDownloadButtonUsesPublicReadinessGate() {
        assertTrue(
            canShowPackageDownload(
                PetGenerationJobResponseDto(appJobId = "job-123", downloadReady = true)
            )
        )
        assertTrue(
            canShowPackageDownload(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    downloadReady = false,
                    nextAction = "download-package"
                )
            )
        )
        assertFalse(
            canShowPackageDownload(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    downloadReady = false,
                    nextAction = "wait"
                )
            )
        )
    }

    @Test
    fun packageDownloadStatusMessagesStayNearDownloadControlAndHideUnsafeDetails() {
        assertEquals(
            "Downloading pet.zip...",
            packageDownloadStartedMessage()
        )
        assertEquals(
            "Downloaded pet-public-lifecycle-smoke.zip to app downloads.",
            packageDownloadSuccessMessage("pet-public-lifecycle-smoke.zip")
        )
        assertEquals(
            "Downloaded pet.zip to app downloads.",
            packageDownloadSuccessMessage("C:/secret/runs/job/pet.zip")
        )
        assertEquals(
            "Package download blocked: package_not_ready",
            packageDownloadFailureMessage("package_not_ready")
        )
        assertEquals(
            "Package download blocked.",
            packageDownloadFailureMessage("C:/secret/runs/job/server_run.json")
        )
    }

    @Test
    fun packageImportCandidateMessageMakesCommunityImportPending() {
        assertEquals(
            "Community import pending: pet-job-123.zip / 664 bytes / review target artifact-1.",
            packageImportCandidateMessage(
                PetGenerationPackageImportCandidate(
                    appJobId = "job-123",
                    targetDownloadId = "artifact-1",
                    packageFileName = "pet-job-123.zip",
                    packageByteCount = 664L,
                    status = "waiting-for-community-import",
                    summary = "Package downloaded; preparing community import draft."
                )
            )
        )
        assertEquals("", packageImportCandidateMessage(null))
    }

    @Test
    fun packageImportCandidateMessageShowsCommunityImportDraftOutcomeSafely() {
        val pendingCandidate = PetGenerationPackageImportCandidate(
            appJobId = "public-lifecycle-smoke",
            targetDownloadId = "artifact-1",
            packageFileName = "pet-public-lifecycle-smoke.zip",
            packageByteCount = 664L,
            status = "waiting-for-community-import",
            summary = "Package downloaded; preparing community import draft."
        )

        assertEquals(
            "Creating community import draft...",
            packageImportCandidateMessage(packageImportInProgressCandidate(pendingCandidate))
        )
        assertEquals(
            "Community import draft import-draft-local-001 ready for public-lifecycle-smoke.",
            packageImportCandidateMessage(
                packageImportDraftSuccessCandidate(
                    pendingCandidate,
                    ImportDraftDto(
                        id = "import-draft-local-001",
                        userId = "user-demo-001",
                        status = "ready",
                        petId = "public-lifecycle-smoke",
                        scoreReportId = "score-import-draft-local-001"
                    )
                )
            )
        )
        assertEquals(
            "Community import blocked.",
            packageImportCandidateMessage(
                packageImportDraftFailureCandidate(
                    pendingCandidate,
                    "D:/workspace4Codex/fantasy-pet-rule/runs/job/server_run.json"
                )
            )
        )
    }

    @Test
    fun packageImportDraftSubmissionUiRequiresReadyDraftAndSafeMessages() {
        val readyDraft = ImportDraftDto(
            id = "import-draft-local-001",
            userId = "user-demo-001",
            status = "ready",
            petId = "public-lifecycle-smoke",
            scoreReportId = "score-import-draft-local-001"
        )

        assertTrue(canSubmitPackageImportDraft(readyDraft))
        assertFalse(canSubmitPackageImportDraft(readyDraft.copy(status = "submitted")))
        assertFalse(canSubmitPackageImportDraft(readyDraft.copy(id = "D:/secret/runs/draft.json")))
        assertFalse(canSubmitPackageImportDraft(null))
        assertEquals("Submitting community import draft...", packageImportSubmissionStartedMessage())
        assertEquals(
            "Community submission submission-local-001 pending for public-lifecycle-smoke.",
            packageImportSubmissionSuccessMessage(
                ImportDraftSubmissionResponseDto(
                    draft = readyDraft.copy(status = "submitted", submissionId = "submission-local-001"),
                    submission = SubmissionDto(
                        id = "submission-local-001",
                        petId = "public-lifecycle-smoke",
                        userId = "user-demo-001",
                        status = "pending",
                        scoreReportId = "score-import-draft-local-001",
                        importDraftId = "import-draft-local-001"
                    )
                )
            )
        )
        assertEquals(
            "Community submission blocked.",
            packageImportSubmissionFailureMessage(
                "D:/workspace4Codex/fantasy-pet-rule/runs/job/server_run.json"
            )
        )
    }

    @Test
    fun packageImportSubmissionStatusUiUsesSafeSubmissionIdsAndStatuses() {
        val pendingSubmission = SubmissionDto(
            id = "submission-local-001",
            petId = "public-lifecycle-smoke",
            userId = "user-demo-001",
            status = "pending",
            scoreReportId = "score-import-draft-local-001",
            importDraftId = "import-draft-local-001"
        )

        assertTrue(canRefreshPackageImportSubmission("submission-local-001"))
        assertFalse(canRefreshPackageImportSubmission("D:/secret/runs/submission.json"))
        assertFalse(canRefreshPackageImportSubmission("submission local 001"))
        assertEquals(
            "Community submission submission-local-001 is pending review for public-lifecycle-smoke.",
            packageImportSubmissionStatusMessage(pendingSubmission)
        )
        assertEquals(
            "Community submission submission-local-001 approved for public-lifecycle-smoke.",
            packageImportSubmissionStatusMessage(pendingSubmission.copy(status = "approved"))
        )
        assertEquals(
            "Community submission status unavailable.",
            packageImportSubmissionStatusMessage(
                pendingSubmission.copy(
                    id = "D:/workspace4Codex/fantasy-pet-rule/runs/job/submission.json"
                )
            )
        )
    }

    @Test
    fun packageImportSubmissionResumeKeepsOnlyPublicSubmissionIds() {
        assertEquals(
            "submission-local-001",
            packageImportSubmissionIdForResume(" submission-local-001 ")
        )
        assertEquals(null, packageImportSubmissionIdForResume(""))
        assertEquals(null, packageImportSubmissionIdForResume("submission local 001"))
        assertEquals(null, packageImportSubmissionIdForResume("D:/secret/runs/submission.json"))
        assertEquals(null, packageImportSubmissionIdForResume("https://example.com/submission-local-001"))
    }

    @Test
    fun packageImportSubmissionResumeMessageHidesUnsafeIds() {
        assertEquals(
            "Community submission submission-local-001 is ready to refresh.",
            packageImportSubmissionResumeMessage("submission-local-001")
        )
        assertEquals("", packageImportSubmissionResumeMessage("D:/secret/runs/submission.json"))
        assertEquals("", packageImportSubmissionResumeMessage("submission local 001"))
    }

    @Test
    fun packageImportSubmissionCommunityRefreshMessageHighlightsApprovalRewardAndShowcase() {
        val approvedSubmission = SubmissionDto(
            id = "submission-local-001",
            petId = "public-lifecycle-smoke",
            userId = "user-demo-001",
            status = "approved",
            scoreReportId = "score-import-draft-local-001",
            importDraftId = "import-draft-local-001"
        )
        val refreshedCommunity = InitialCommunityResult(
            posts = listOf(
                FeedPost(
                    id = "post-import-public-lifecycle-smoke",
                    petId = "public-lifecycle-smoke",
                    title = "Public lifecycle smoke imported",
                    body = "Approved generated pet.",
                    authorName = "Demo Keeper",
                    reactionCount = 0,
                    rewardLabel = "+55 petcoin",
                    submissionLabel = "Submission submission-local-001"
                )
            ),
            approvedPets = listOf(
                ApprovedPet(
                    petId = "public-lifecycle-smoke",
                    displayName = "Public Lifecycle Smoke",
                    sourceKind = "fantasy-pet-rule",
                    previewPath = "previews/public-lifecycle-smoke.png",
                    exportArtifactPath = "exports/public-lifecycle-smoke.zip",
                    motionSheetCount = 2,
                    totalScore = 86
                )
            ),
            walletBalance = 145,
            message = "Community ready.",
            usedFallback = false
        )

        assertEquals(
            "Community submission submission-local-001 approved for public-lifecycle-smoke. Showcase updated; reward +55 petcoin posted; wallet balance 145 petcoin.",
            packageImportSubmissionCommunityRefreshMessage(approvedSubmission, refreshedCommunity)
        )
    }

    @Test
    fun packageImportSubmissionCommunityRefreshMessageHidesUnsafeCommunitySnapshotDetails() {
        val approvedSubmission = SubmissionDto(
            id = "submission-local-001",
            petId = "public-lifecycle-smoke",
            userId = "user-demo-001",
            status = "approved",
            importDraftId = "import-draft-local-001"
        )
        val refreshedCommunity = InitialCommunityResult(
            posts = listOf(
                FeedPost(
                    id = "post-import-public-lifecycle-smoke",
                    petId = "public-lifecycle-smoke",
                    title = "Public lifecycle smoke imported",
                    body = "Approved generated pet.",
                    authorName = "Demo Keeper",
                    reactionCount = 0,
                    rewardLabel = "+55 petcoin",
                    submissionLabel = "Submission submission-local-001",
                    exportArtifactLabel = "Package D:/secret/runs/job/server_run.json"
                )
            ),
            approvedPets = listOf(
                ApprovedPet(
                    petId = "public-lifecycle-smoke",
                    displayName = "Public Lifecycle Smoke",
                    sourceKind = "fantasy-pet-rule",
                    previewPath = "D:/secret/runs/job/preview.png",
                    exportArtifactPath = "D:/secret/runs/job/server_run.json",
                    motionSheetCount = 2,
                    totalScore = 86
                )
            ),
            walletBalance = 145,
            message = "Community ready.",
            usedFallback = false
        )

        val message = packageImportSubmissionCommunityRefreshMessage(
            approvedSubmission,
            refreshedCommunity
        )

        assertEquals(
            "Community submission submission-local-001 approved for public-lifecycle-smoke. Showcase updated; reward +55 petcoin posted; wallet balance 145 petcoin.",
            message
        )
        assertFalse(message.contains("D:/secret"))
        assertFalse(message.contains("server_run.json"))
    }

    @Test
    fun pollDelayBacksOffForLongWaitingStates() {
        assertEquals(
            3_000L,
            generationPollDelayMillis(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "processing",
                    nextAction = "wait"
                )
            )
        )
        assertEquals(
            8_000L,
            generationPollDelayMillis(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "revision-requested",
                    nextAction = "await-revision"
                )
            )
        )
        assertEquals(
            8_000L,
            generationPollDelayMillis(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    progressStatus = "candidate-rejected",
                    nextAction = "await-new-candidate"
                )
            )
        )
    }

    @Test
    fun createResponseStatusDrivesPollingWhenProgressStatusIsMissing() {
        assertTrue(
            shouldPollGenerationJob(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    status = "queued",
                    progressStatus = "",
                    nextAction = "wait"
                )
            )
        )
    }

    @Test
    fun statusFallbackCanEnableHumanReviewControls() {
        assertTrue(
            canSubmitHumanReview(
                PetGenerationJobResponseDto(
                    appJobId = "job-123",
                    status = "waiting-for-review",
                    progressStatus = "",
                    nextAction = "wait",
                    artifacts = listOf(
                        PetGenerationArtifactDto(
                            kind = "candidate",
                            downloadId = "artifact-1",
                            downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1"
                        )
                    )
                ),
                selectedCandidateDownloadId = "artifact-1"
            )
        )
    }

    @Test
    fun contractDemoJobExplainsItIsNotALiveGenerationRun() {
        val job = PetGenerationJobResponseDto(
            appJobId = "public-lifecycle-smoke",
            runId = "public-lifecycle-smoke",
            progressStatus = "waiting-for-review",
            nextAction = "human-review"
        )

        assertTrue(isContractDemoGenerationJob(job))
        assertEquals(
            "Contract demo task: this candidate is pre-seeded for public API validation; it is not a live pet generation run.",
            generationContractDemoNotice(job)
        )
    }

    @Test
    fun contractDemoJobAcceptsAndroidSmokeSafeAlias() {
        val job = PetGenerationJobResponseDto(
            appJobId = "publicdemo1",
            runId = "publicdemo1",
            progressStatus = "waiting-for-review",
            nextAction = "human-review"
        )

        assertTrue(isContractDemoGenerationJob(job))
        assertTrue(isContractDemoGenerationJobId("public_lifecycle_smoke"))
        assertFalse(isContractDemoGenerationJobId("ordinary_generation_job"))
    }

    @Test
    fun contractDemoJobUsesFixtureProgressCopyInsteadOfLiveGenerationCopy() {
        val job = PetGenerationJobResponseDto(
            appJobId = "public-lifecycle-smoke",
            runId = "public-lifecycle-smoke",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            generationProgress = PetGenerationProgressDto(
                summary = PetGenerationProgressSummaryDto(candidateCount = 1)
            )
        )
        val service = FantasyPetGenerationService(NoopFantasyPetGenerationClient())

        val progressMessage = service.generationProgressMessage(job)
        val summaryLine = generationProgressSummaryLine(job)

        assertEquals(
            "Contract demo fixture loaded; no live generation worker has run.",
            progressMessage
        )
        assertEquals(
            "Contract demo fixture loaded; no live generation worker has run.",
            summaryLine
        )
        assertFalse(progressMessage.contains("human review", ignoreCase = true))
        assertFalse(summaryLine.contains("candidate ready", ignoreCase = true))
    }

    @Test
    fun contractDemoJobCannotSubmitHumanReviewDecision() {
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

        assertFalse(canSubmitHumanReview(job, selectedCandidateDownloadId = "artifact-1"))
        assertFalse(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "artifact-1",
                decision = "accept",
                notesText = ""
            )
        )
    }

    @Test
    fun contractDemoJobCannotDownloadPackageEvenIfServerReportsReady() {
        val job = PetGenerationJobResponseDto(
            appJobId = "public-lifecycle-smoke",
            runId = "public-lifecycle-smoke",
            progressStatus = "ready-for-download",
            nextAction = "download-package",
            downloadReady = true
        )

        assertFalse(canShowPackageDownload(job))
    }

    @Test
    fun reviewDecisionButtonsRequireNotesForReviseAndReject() {
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "artifact-1",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1"
                )
            )
        )

        assertTrue(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "artifact-1",
                decision = "accept",
                notesText = ""
            )
        )
        assertFalse(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "artifact-1",
                decision = "revise",
                notesText = ""
            )
        )
        assertFalse(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "artifact-1",
                decision = "reject",
                notesText = " "
            )
        )
        assertTrue(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "artifact-1",
                decision = "revise",
                notesText = "idle action jumps vertically"
            )
        )
    }

    @Test
    fun reviewDecisionButtonsBlockUnsafeReviewNotesBeforeSubmit() {
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "artifact-1",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1"
                )
            )
        )
        val unsafeNotes = "D:/workspace4Codex/fantasy-pet-rule/runs/job/server_run.json"

        assertEquals(
            "Revise and reject need specific visual notes.",
            reviewNotesValidationMessage("revise", "")
        )
        assertEquals(
            "Review notes cannot include internal paths or worker details.",
            reviewNotesValidationMessage("revise", unsafeNotes)
        )
        assertEquals(
            "Review notes cannot include internal paths or worker details.",
            reviewNotesValidationMessage("accept", unsafeNotes)
        )
        assertFalse(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "artifact-1",
                decision = "accept",
                notesText = unsafeNotes
            )
        )
        assertFalse(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "artifact-1",
                decision = "revise",
                notesText = unsafeNotes
            )
        )
        assertTrue(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "artifact-1",
                decision = "revise",
                notesText = "idle action jumps vertically"
            )
        )
    }

    @Test
    fun reviewDecisionButtonsRequireSelectedCandidateFromCurrentJob() {
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "artifact-1",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/artifact-1"
                )
            )
        )

        assertFalse(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "artifact-9",
                decision = "accept",
                notesText = ""
            )
        )
        assertTrue(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "artifact-1",
                decision = "accept",
                notesText = ""
            )
        )
    }

    @Test
    fun candidateSelectionIsPreservedAcrossJobRefreshWhenStillAvailable() {
        val candidates = listOf(
            CandidateGalleryItem(
                targetDownloadId = "artifact-1",
                previewUrl = "https://example.com/1.png",
                title = "Candidate artifact-1",
                status = "waiting-for-review"
            ),
            CandidateGalleryItem(
                targetDownloadId = "artifact-2",
                previewUrl = "https://example.com/2.png",
                title = "Candidate artifact-2",
                status = "waiting-for-review"
            )
        )

        assertEquals(
            "artifact-2",
            selectedCandidateAfterJobRefresh(
                candidates = candidates,
                currentSelectedCandidateDownloadId = "artifact-2"
            )
        )
        assertEquals(
            "artifact-1",
            selectedCandidateAfterJobRefresh(
                candidates = candidates,
                currentSelectedCandidateDownloadId = "artifact-9"
            )
        )
        assertEquals(
            "",
            selectedCandidateAfterJobRefresh(
                candidates = emptyList(),
                currentSelectedCandidateDownloadId = "artifact-2"
            )
        )
    }

    @Test
    fun candidateSelectionPrefersUnreviewedCandidateWhenPreviousSelectionWasReviewed() {
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "old-candidate",
                    reviewDecision = "revise",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/old-candidate"
                ),
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "new-candidate",
                    reviewDecision = "",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/new-candidate"
                )
            )
        )
        val service = FantasyPetGenerationService(NoopFantasyPetGenerationClient())
        val candidates = service.candidateGalleryItems(job)

        assertEquals(
            "new-candidate",
            selectedCandidateAfterJobRefresh(
                candidates = candidates,
                currentSelectedCandidateDownloadId = "old-candidate"
            )
        )
    }

    @Test
    fun reviewDecisionButtonsBlockPreviouslyReviewedCandidates() {
        val job = PetGenerationJobResponseDto(
            appJobId = "job-123",
            progressStatus = "waiting-for-review",
            nextAction = "human-review",
            artifacts = listOf(
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "old-candidate",
                    reviewDecision = "reject",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/old-candidate"
                ),
                PetGenerationArtifactDto(
                    kind = "candidate",
                    downloadId = "new-candidate",
                    downloadUrl = "/pet-generation-jobs/job-123/artifacts/new-candidate"
                )
            )
        )

        assertFalse(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "old-candidate",
                decision = "accept",
                notesText = ""
            )
        )
        assertTrue(
            canSubmitReviewDecision(
                job = job,
                selectedCandidateDownloadId = "new-candidate",
                decision = "accept",
                notesText = ""
            )
        )
    }

    @Test
    fun savedGenerationJobIdCanResumeWhenNoJobIsLoaded() {
        assertEquals("job-123", generationJobIdForResume(" job-123 ", null))
        assertEquals(null, generationJobIdForResume(" ", null))
        assertEquals(
            null,
            generationJobIdForResume(
                "job-123",
                PetGenerationJobResponseDto(appJobId = "job-123")
            )
        )
    }

    @Test
    fun clearGenerationJobControlAppearsForSavedOrLoadedJobs() {
        assertFalse(canClearGenerationJob("", null))
        assertTrue(canClearGenerationJob(" job-123 ", null))
        assertTrue(canClearGenerationJob("", PetGenerationJobResponseDto(appJobId = "job-123")))
    }

    @Test
    fun clearedGenerationJobStateResetsResumeReviewAndSelectionFields() {
        val cleared = clearedGenerationJobUiState()

        assertEquals("", cleared.appJobId)
        assertEquals("", cleared.selectedCandidateDownloadId)
        assertEquals("", cleared.reviewNotes)
        assertEquals(DEFAULT_GENERATION_MESSAGE, cleared.message)
    }

    @Test
    fun persistedGenerationJobIdPrefersResponseAndFallsBackToRequestedId() {
        assertEquals(
            "job-from-response",
            persistedGenerationJobId(
                requestedAppJobId = "job-from-input",
                job = PetGenerationJobResponseDto(appJobId = "job-from-response")
            )
        )
        assertEquals(
            "job-from-input",
            persistedGenerationJobId(
                requestedAppJobId = " job-from-input ",
                job = PetGenerationJobResponseDto(appJobId = "")
            )
        )
        assertEquals(
            "",
            persistedGenerationJobId(
                requestedAppJobId = " ",
                job = PetGenerationJobResponseDto(appJobId = "")
            )
        )
    }

    @Test
    fun generationJobHistoryKeepsSafeRecentDistinctIds() {
        val history = generationJobHistoryAfterPersist(
            existingAppJobIds = listOf(
                "job-old",
                "job-123",
                "D:/secret/runs/job/server_run.json",
                "job-extra-1",
                "job-extra-2",
                "job-extra-3"
            ),
            requestedAppJobId = "job-from-input",
            job = PetGenerationJobResponseDto(appJobId = "job-123")
        )

        assertEquals(
            listOf("job-123", "job-old", "job-extra-1", "job-extra-2", "job-extra-3"),
            history
        )
    }

    @Test
    fun persistedGenerationJobHistoryFiltersUnsafeAndDuplicateIds() {
        val history = persistedGenerationJobHistory(
            " job-1 \nhttps://example.com/job\njob-2,D:/secret/runs/job/server_run.json\njob-1\njob_3.preview"
        )

        assertEquals(listOf("job-1", "job-2", "job_3.preview"), history)
        assertEquals("job-1\njob-2\njob_3.preview", serializedGenerationJobHistory(history))
    }

    @Test
    fun initialGenerationJobHistoryMigratesLegacySavedJobId() {
        val history = initialGenerationJobHistory(
            savedAppJobId = " legacy-job-1 ",
            rawHistory = "job-2\nlegacy-job-1\nD:/secret/runs/job/server_run.json"
        )

        assertEquals(listOf("legacy-job-1", "job-2"), history)
    }

    @Test
    fun generationJobHistoryAfterRemoveDropsOneSafeJobAndFiltersUnsafeEntries() {
        val history = generationJobHistoryAfterRemove(
            existingAppJobIds = listOf(
                "job-1",
                "job-2",
                "D:/secret/runs/job/server_run.json",
                "job-3",
                "job-2"
            ),
            appJobIdToRemove = " job-2 "
        )

        assertEquals(listOf("job-1", "job-3"), history)
    }

    @Test
    fun recentGenerationJobResumeIdRequiresPublicAppJobId() {
        assertEquals("job-123.preview_a", recentGenerationJobResumeId(" job-123.preview_a "))
        assertEquals(null, recentGenerationJobResumeId(""))
        assertEquals(null, recentGenerationJobResumeId("job 123"))
        assertEquals(null, recentGenerationJobResumeId("D:/secret/runs/job/server_run.json"))
        assertEquals(null, recentGenerationJobResumeId("https://example.com/job"))
    }

    @Test
    fun reviewNoteSuggestionsAppendAsSpecificLines() {
        assertEquals(
            "idle action jumps vertically",
            appendReviewNoteSuggestion("", "idle action jumps vertically")
        )
        assertEquals(
            "idle action jumps vertically\nrunning-right is nearly static",
            appendReviewNoteSuggestion(
                "idle action jumps vertically",
                "running-right is nearly static"
            )
        )
        assertEquals(
            "identity drift between first and last frame",
            appendReviewNoteSuggestion("identity drift between first and last frame", "")
        )
    }
}

private class NoopFantasyPetGenerationClient : FantasyPetGenerationClient {
    override suspend fun createJob(
        request: PetGenerationJobCreateRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun getJob(appJobId: String): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun getArtifacts(
        appJobId: String
    ): ApiCallResult<PetGenerationArtifactIndexResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun submitReviewDecision(
        appJobId: String,
        request: ReviewDecisionRequestDto
    ): ApiCallResult<PetGenerationJobResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun downloadPackage(appJobId: String): ApiCallResult<ByteArray> =
        ApiCallResult.Failure("not_supported")

    override suspend fun getWorkerReadiness(): ApiCallResult<WorkerReadinessResponseDto> =
        ApiCallResult.Failure("not_supported")

    override suspend fun getAppApiContract(): ApiCallResult<PetGenerationAppApiContractDto> =
        ApiCallResult.Failure("not_supported")
}
