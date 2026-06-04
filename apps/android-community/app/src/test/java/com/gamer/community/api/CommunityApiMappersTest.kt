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
}
