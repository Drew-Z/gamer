package com.gamer.community.api

import com.gamer.community.petshell.ApprovedPet
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
            scoreReportLabel = item.metadata.scoreReportLabel(),
            importSourceLabel = item.metadata.importSourceLabel(),
            importPreviewLabel = item.metadata.importPreviewLabel(),
            exportArtifactLabel = item.metadata.exportArtifactLabel(),
            motionSheetLabel = item.metadata.motionSheetLabel()
        )
    }

fun ApprovedPetsResponseDto.toApprovedPets(): List<ApprovedPet> =
    items.map { item ->
        ApprovedPet(
            petId = item.petId,
            displayName = item.displayName,
            sourceKind = item.source.kind,
            sourceAppJobId = item.source.appJobId,
            previewPath = item.assets.previewPath,
            targetDownloadId = item.assets.targetDownloadId,
            previewUrl = item.assets.previewUrl,
            exportArtifactPath = item.assets.exportArtifactPath,
            motionSheetCount = item.assets.motionSheetCount,
            totalScore = item.totalScore
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
    return if (id.isNotBlank()) "Draft ready" else null
}

private fun FeedPostMetadataDto?.submissionLabel(): String? {
    val id = this?.submissionId ?: return null
    return if (id.isNotBlank()) "Review submitted" else null
}

private fun FeedPostMetadataDto?.scoreReportLabel(): String? {
    val id = this?.scoreReportId ?: return null
    return if (id.isNotBlank()) "Score ready" else null
}

private fun FeedPostMetadataDto?.importSourceLabel(): String? {
    val kind = this?.importSourceKind ?: return null
    return if (kind.isNotBlank()) "Reviewed source" else null
}

private fun FeedPostMetadataDto?.importPreviewLabel(): String? {
    val path = this?.importPreviewPath ?: return null
    return if (path.isNotBlank()) "Preview ready" else null
}

private fun FeedPostMetadataDto?.exportArtifactLabel(): String? {
    val path = this?.exportArtifactPath ?: return null
    return if (path.isNotBlank()) "Package ready" else null
}

private fun FeedPostMetadataDto?.motionSheetLabel(): String? {
    val count = this?.motionSheetCount ?: return null
    return if (count > 0) "$count motion sheets" else null
}
