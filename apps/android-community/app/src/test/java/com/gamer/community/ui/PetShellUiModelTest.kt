package com.gamer.community.ui

import com.gamer.community.petshell.FeedPost
import com.gamer.community.petshell.ApprovedPet
import org.junit.Assert.assertEquals
import org.junit.Test

class PetShellUiModelTest {
    @Test
    fun feedPostMetadataLabelsReturnsImportAndRewardLabels() {
        val post = FeedPost(
            id = "post-import-001",
            petId = "pet-import-001",
            title = "Approved pet import: pet-import-001",
            body = "preview accepted by user",
            authorName = "Demo Keeper",
            reactionCount = 0,
            sourceLabel = "Approved import",
            rewardLabel = "+80 petcoin"
        )

        assertEquals(listOf("Approved import", "+80 petcoin"), feedPostMetadataLabels(post))
    }

    @Test
    fun feedPostMetadataLabelsOmitsMissingLabels() {
        val post = FeedPost(
            id = "post-demo-001",
            petId = "pet-stardust-001",
            title = "Stardust dragon launch pose",
            body = "Fixture post.",
            authorName = "Demo Keeper",
            reactionCount = 12
        )

        assertEquals(emptyList<String>(), feedPostMetadataLabels(post))
    }

    @Test
    fun feedPostAuditLabelsReturnsImportReferences() {
        val post = FeedPost(
            id = "post-import-001",
            petId = "pet-import-001",
            title = "Approved pet import: pet-import-001",
            body = "preview accepted by user",
            authorName = "Demo Keeper",
            reactionCount = 0,
            importDraftLabel = "Draft import-draft-local-001",
            submissionLabel = "Submission submission-local-002",
            scoreReportLabel = "Score score-import-draft-local-001"
        )

        assertEquals(
            listOf(
                "Draft import-draft-local-001",
                "Submission submission-local-002",
                "Score score-import-draft-local-001"
            ),
            feedPostAuditLabels(post)
        )
    }

    @Test
    fun feedPostAuditLabelsOmitsMissingReferences() {
        val post = FeedPost(
            id = "post-demo-001",
            petId = "pet-stardust-001",
            title = "Stardust dragon launch pose",
            body = "Fixture post.",
            authorName = "Demo Keeper",
            reactionCount = 12
        )

        assertEquals(emptyList<String>(), feedPostAuditLabels(post))
    }

    @Test
    fun approvedPetRegistrySummaryShowsEmptyState() {
        assertEquals("No approved pets yet", approvedPetRegistrySummary(emptyList()))
    }

    @Test
    fun approvedPetRegistrySummaryShowsCount() {
        val pet = ApprovedPet(
            petId = "pet-stardust-001",
            displayName = "Stardust Dragon",
            sourceKind = "fantasy-pet-rule",
            previewPath = "previews/overall-showcase.png",
            motionSheetCount = 2,
            totalScore = 86
        )

        assertEquals("1 approved pet", approvedPetRegistrySummary(listOf(pet)))
        assertEquals("2 approved pets", approvedPetRegistrySummary(listOf(pet, pet)))
    }

    @Test
    fun approvedPetShowcaseTitleUsesFirstApprovedPet() {
        val pet = ApprovedPet(
            petId = "pet-stardust-001",
            displayName = "Stardust Dragon",
            sourceKind = "fantasy-pet-rule",
            previewPath = "previews/overall-showcase.png",
            motionSheetCount = 2,
            totalScore = 86
        )

        assertEquals("Stardust Dragon", approvedPetShowcaseTitle(listOf(pet)))
        assertEquals("Awaiting approved pet", approvedPetShowcaseTitle(emptyList()))
    }

    @Test
    fun approvedPetShowcaseDetailSummarizesScoreAndMotionSheets() {
        val pet = ApprovedPet(
            petId = "pet-stardust-001",
            displayName = "Stardust Dragon",
            sourceKind = "fantasy-pet-rule",
            previewPath = "previews/overall-showcase.png",
            motionSheetCount = 2,
            totalScore = 86
        )

        assertEquals(
            "fantasy-pet-rule / score 86 / 2 motion sheets",
            approvedPetShowcaseDetail(listOf(pet))
        )
        assertEquals(
            "Approved imports will appear here.",
            approvedPetShowcaseDetail(emptyList())
        )
    }
}
