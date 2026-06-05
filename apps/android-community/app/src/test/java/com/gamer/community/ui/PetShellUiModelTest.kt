package com.gamer.community.ui

import com.gamer.community.petshell.FeedPost
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
}
