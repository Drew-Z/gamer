package com.gamer.community.api

import com.gamer.community.petshell.FeedPost

fun FeedResponseDto.toFeedPosts(): List<FeedPost> =
    items.map { item ->
        FeedPost(
            id = item.id,
            petId = item.petId,
            title = item.title,
            body = item.body,
            authorName = item.authorDisplayName(),
            reactionCount = item.reactionCount
        )
    }

private fun FeedPostDto.authorDisplayName(): String =
    when (authorId) {
        "user-demo-001" -> "Demo Keeper"
        else -> "Keeper $authorId"
    }
