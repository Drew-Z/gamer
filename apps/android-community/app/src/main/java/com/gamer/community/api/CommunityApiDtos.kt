package com.gamer.community.api

import kotlinx.serialization.Serializable

@Serializable
data class FeedResponseDto(
    val items: List<FeedPostDto> = emptyList(),
    val nextCursor: String? = null
)

@Serializable
data class CommunityHomeResponseDto(
    val schema: String = "",
    val userId: String = "",
    val feed: FeedResponseDto = FeedResponseDto(),
    val wallet: WalletDto = WalletDto(userId = "", balance = 0, currencyCode = "petcoin"),
    val approvedPets: ApprovedPetsResponseDto = ApprovedPetsResponseDto(),
    val dailyCheckIn: CommunityHomeDailyCheckInDto = CommunityHomeDailyCheckInDto(),
    val submissionsSummary: CommunityHomeSubmissionsSummaryDto =
        CommunityHomeSubmissionsSummaryDto()
)

@Serializable
data class CommunityHomeDailyCheckInDto(
    val date: String = "",
    val claimed: Boolean = false,
    val rewardAmount: Int = 10,
    val ledgerEntryId: String = ""
)

@Serializable
data class CommunityHomeSubmissionsSummaryDto(
    val pendingCount: Int = 0,
    val approvedCount: Int = 0,
    val heldCount: Int = 0,
    val rejectedCount: Int = 0,
    val revokedCount: Int = 0,
    val latest: SubmissionDto? = null
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
    val rewardAmount: Int? = null,
    val importSourceKind: String? = null,
    val importPreviewPath: String? = null,
    val exportArtifactPath: String? = null,
    val motionSheetCount: Int? = null
)

@Serializable
data class ApprovedPetsResponseDto(
    val items: List<ApprovedPetDto> = emptyList()
)

@Serializable
data class ApprovedPetDto(
    val petId: String,
    val displayName: String,
    val ownerUserId: String,
    val source: ApprovedPetSourceDto = ApprovedPetSourceDto(),
    val assets: ApprovedPetAssetsDto = ApprovedPetAssetsDto(),
    val totalScore: Int = 0
)

@Serializable
data class ApprovedPetSourceDto(
    val kind: String = "",
    val appJobId: String = ""
)

@Serializable
data class ApprovedPetAssetsDto(
    val previewPath: String = "",
    val targetDownloadId: String = "",
    val previewUrl: String = "",
    val exportArtifactPath: String = "",
    val motionSheetCount: Int = 0
)

@Serializable
data class ApprovedPetPackageDto(
    val petId: String,
    val displayName: String,
    val ownerUserId: String,
    val `package`: ApprovedPetPackageArtifactDto = ApprovedPetPackageArtifactDto(),
    val assets: ApprovedPetPackageAssetsDto = ApprovedPetPackageAssetsDto(),
    val source: ApprovedPetPackageSourceDto = ApprovedPetPackageSourceDto(),
    val submissionId: String = "",
    val importDraftId: String = "",
    val scoreReportId: String = ""
)

@Serializable
data class ApprovedPetPackageArtifactDto(
    val exportArtifactPath: String = "",
    val status: String = ""
)

@Serializable
data class ApprovedPetPackageAssetsDto(
    val previewPath: String = "",
    val targetDownloadId: String = "",
    val previewUrl: String = "",
    val motionSheetCount: Int = 0
)

@Serializable
data class ApprovedPetPackageSourceDto(
    val kind: String = "",
    val runId: String = "",
    val statePath: String = ""
)

@Serializable
data class FantasyPetPackageImportDraftRequestDto(
    val packageManifest: FantasyPetPackageManifestDto,
    val packageFileName: String,
    val packageByteCount: Long,
    val targetDownloadId: String,
    val ownershipClaimId: String = ""
)

@Serializable
data class FantasyPetPackageManifestDto(
    val schema: String = "fantasy-pet.package-manifest.v1",
    val runId: String,
    val appJobId: String,
    val acceptedBy: String,
    val sourceDownloadId: String = "",
    val sourceTaskId: String = "",
    val files: List<FantasyPetPackageFileDto> = emptyList()
)

@Serializable
data class FantasyPetPackageFileDto(
    val kind: String,
    val path: String
)

@Serializable
data class ImportDraftDto(
    val id: String,
    val userId: String,
    val status: String,
    val petId: String,
    val ownershipClaimId: String = "",
    val scoreReportId: String = "",
    val submissionId: String = ""
)

@Serializable
data class ImportDraftSubmitRequestDto(
    val draftId: String
)

@Serializable
data class ImportDraftSubmissionResponseDto(
    val draft: ImportDraftDto,
    val submission: SubmissionDto
)

@Serializable
data class SubmissionDto(
    val id: String,
    val petId: String,
    val userId: String,
    val status: String,
    val scoreReportId: String = "",
    val ownershipClaimId: String = "",
    val importDraftId: String = "",
    val submittedAt: String = ""
)

@Serializable
data class SubmissionsResponseDto(
    val submissions: List<SubmissionDto> = emptyList()
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
