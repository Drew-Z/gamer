package com.gamer.community.petshell

enum class ShellPhase {
    LaunchBubble,
    DesktopPet,
    Community
}

enum class PetAction {
    Idle,
    AppLoading,
    BubbleOpen,
    FeedNext,
    FeedPrevious,
    FeedSkip,
    ShowcaseNext,
    ShowcasePrevious,
    Reward,
    Review
}

enum class FeedDirection {
    Next,
    Previous,
    Skip
}

data class FeedPost(
    val id: String,
    val petId: String,
    val title: String,
    val body: String,
    val authorName: String,
    val reactionCount: Int,
    val sourceLabel: String? = null,
    val rewardLabel: String? = null,
    val importDraftLabel: String? = null,
    val submissionLabel: String? = null,
    val scoreReportLabel: String? = null,
    val importSourceLabel: String? = null,
    val importPreviewLabel: String? = null,
    val exportArtifactLabel: String? = null,
    val motionSheetLabel: String? = null
)

data class ApprovedPet(
    val petId: String,
    val displayName: String,
    val sourceKind: String,
    val sourceAppJobId: String = "",
    val previewPath: String,
    val targetDownloadId: String = "",
    val previewUrl: String = "",
    val exportArtifactPath: String,
    val motionSheetCount: Int,
    val totalScore: Int
)

data class PetShellState(
    val phase: ShellPhase,
    val petAction: PetAction,
    val speechBubble: String,
    val feedIndex: Int,
    val walletBalance: Int,
    val checkInClaimed: Boolean,
    val pendingSubmissionCount: Int = 0,
    val approvedPets: List<ApprovedPet>,
    val approvedPetIndex: Int,
    val posts: List<FeedPost>
)
