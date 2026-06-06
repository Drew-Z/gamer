package com.gamer.community.api

import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityApiMappersTest {
    @Test
    fun mapsFeedResponseToShellPosts() {
        val response = FeedResponseDto(
            items = listOf(
                FeedPostDto(
                    id = "post-live-001",
                    authorId = "user-demo-001",
                    petId = "pet-live-001",
                    title = "Live pet pose",
                    body = "Loaded from community-api.",
                    reactionCount = 42,
                    createdAt = "2026-06-05T00:00:00.000Z"
                )
            ),
            nextCursor = "page-2"
        )

        val posts = response.toFeedPosts()

        assertEquals(1, posts.size)
        assertEquals("post-live-001", posts[0].id)
        assertEquals("pet-live-001", posts[0].petId)
        assertEquals("Live pet pose", posts[0].title)
        assertEquals("Loaded from community-api.", posts[0].body)
        assertEquals("Demo Keeper", posts[0].authorName)
        assertEquals(42, posts[0].reactionCount)
    }

    @Test
    fun mapsUnknownAuthorToStableFallback() {
        val response = FeedResponseDto(
            items = listOf(
                FeedPostDto(
                    id = "post-live-002",
                    authorId = "user-new-999",
                    petId = "pet-live-002",
                    title = "New keeper",
                    body = "Author profile is not loaded in phase 5b.",
                    reactionCount = 3,
                    createdAt = "2026-06-05T00:01:00.000Z"
                )
            )
        )

        val posts = response.toFeedPosts()

        assertEquals("Keeper user-new-999", posts[0].authorName)
    }

    @Test
    fun mapsApprovedImportMetadataToDisplayLabels() {
        val response = FeedResponseDto(
            items = listOf(
                FeedPostDto(
                    id = "post-import-001",
                    authorId = "user-demo-001",
                    petId = "pet-import-001",
                    title = "Approved pet import: pet-import-001",
                    body = "preview accepted by user",
                    reactionCount = 0,
                    createdAt = "2026-06-05T00:02:00.000Z",
                    metadata = FeedPostMetadataDto(
                        sourceType = "approved-import",
                        importDraftId = "import-draft-local-001",
                        submissionId = "submission-local-002",
                        scoreReportId = "score-import-draft-local-001",
                        rewardAmount = 80,
                        importSourceKind = "fantasy-pet-rule",
                        importPreviewPath = "previews/overall-showcase.png",
                        motionSheetCount = 2
                    )
                )
            )
        )

        val posts = response.toFeedPosts()

        assertEquals("Approved import", posts[0].sourceLabel)
        assertEquals("+80 petcoin", posts[0].rewardLabel)
        assertEquals("Draft import-draft-local-001", posts[0].importDraftLabel)
        assertEquals("Submission submission-local-002", posts[0].submissionLabel)
        assertEquals("Score score-import-draft-local-001", posts[0].scoreReportLabel)
        assertEquals("Source fantasy-pet-rule", posts[0].importSourceLabel)
        assertEquals("Preview previews/overall-showcase.png", posts[0].importPreviewLabel)
        assertEquals("2 motion sheets", posts[0].motionSheetLabel)
    }

    @Test
    fun mapsApprovedPetRegistryToShellModels() {
        val response = ApprovedPetsResponseDto(
            items = listOf(
                ApprovedPetDto(
                    petId = "pet-stardust-001",
                    displayName = "Stardust Dragon",
                    ownerUserId = "user-demo-001",
                    source = ApprovedPetSourceDto(kind = "fantasy-pet-rule"),
                    assets = ApprovedPetAssetsDto(
                        previewPath = "previews/overall-showcase.png",
                        motionSheetCount = 2
                    ),
                    totalScore = 86
                )
            )
        )

        val pets = response.toApprovedPets()

        assertEquals(1, pets.size)
        assertEquals("pet-stardust-001", pets[0].petId)
        assertEquals("Stardust Dragon", pets[0].displayName)
        assertEquals("fantasy-pet-rule", pets[0].sourceKind)
        assertEquals("previews/overall-showcase.png", pets[0].previewPath)
        assertEquals(2, pets[0].motionSheetCount)
        assertEquals(86, pets[0].totalScore)
    }
}
