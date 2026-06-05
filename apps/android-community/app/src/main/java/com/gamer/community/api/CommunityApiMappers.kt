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
            reactionCount = item.reactionCount,
            sourceLabel = item.metadata.sourceLabel(),
            rewardLabel = item.metadata.rewardLabel(),
            importDraftLabel = item.metadata.importDraftLabel(),
            submissionLabel = item.metadata.submissionLabel(),
            scoreReportLabel = item.metadata.scoreReportLabel()
        )
    }

private fun FeedPostDto.authorDisplayName(): String =
    when (authorId) {
        "user-demo-001" -> "Demo Keeper"
        else -> "Keeper $authorId"
    }

private fun FeedPostMetadataDto?.sourceLabel(): String? =
    when (this?.sourceType) {
        "approved-import" -> "Approved import"
        else -> null
    }

private fun FeedPostMetadataDto?.rewardLabel(): String? {
    val amount = this?.rewardAmount ?: return null
    return if (amount > 0) "+$amount petcoin" else null
}

private fun FeedPostMetadataDto?.importDraftLabel(): String? {
    val id = this?.importDraftId ?: return null
    return if (id.isNotBlank()) "Draft $id" else null
}

private fun FeedPostMetadataDto?.submissionLabel(): String? {
    val id = this?.submissionId ?: return null
    return if (id.isNotBlank()) "Submission $id" else null
}

private fun FeedPostMetadataDto?.scoreReportLabel(): String? {
    val id = this?.scoreReportId ?: return null
    return if (id.isNotBlank()) "Score $id" else null
}
