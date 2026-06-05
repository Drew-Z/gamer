package com.gamer.community.api

import kotlinx.serialization.Serializable

@Serializable
data class FeedResponseDto(
    val items: List<FeedPostDto> = emptyList(),
    val nextCursor: String? = null
)

@Serializable
data class FeedPostDto(
    val id: String,
    val authorId: String,
    val petId: String,
    val title: String,
    val body: String,
    val reactionCount: Int,
    val createdAt: String,
    val metadata: FeedPostMetadataDto? = null
)

@Serializable
data class FeedPostMetadataDto(
    val sourceType: String? = null,
    val importDraftId: String? = null,
    val submissionId: String? = null,
    val scoreReportId: String? = null,
    val rewardAmount: Int? = null
)

@Serializable
data class WalletDto(
    val userId: String,
    val balance: Int,
    val currencyCode: String,
    val ledgerEntries: List<LedgerEntryDto> = emptyList()
)

@Serializable
data class LedgerEntryDto(
    val entryId: String,
    val userId: String,
    val amount: Int,
    val sourceType: String,
    val sourceId: String,
    val status: String,
    val createdAt: String
)

@Serializable
data class CheckInResponseDto(
    val checkIn: CheckInDto,
    val wallet: WalletDto,
    val ledgerEntry: LedgerEntryDto? = null
)

@Serializable
data class CheckInDto(
    val userId: String,
    val date: String,
    val claimed: Boolean,
    val rewardAmount: Int,
    val ledgerEntryId: String
)
