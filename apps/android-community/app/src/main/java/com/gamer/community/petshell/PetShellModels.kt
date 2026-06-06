package com.gamer.community.petshell

enum class ShellPhase {
    LaunchBubble,
    Community
}

enum class PetAction {
    Idle,
    AppLoading,
    BubbleOpen,
    FeedNext,
    FeedPrevious,
    FeedSkip,
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
    val motionSheetLabel: String? = null
)

data class ApprovedPet(
    val petId: String,
    val displayName: String,
    val sourceKind: String,
    val previewPath: String,
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
    val approvedPets: List<ApprovedPet>,
    val posts: List<FeedPost>
) {
    val currentPost: FeedPost
        get() = posts[feedIndex.coerceIn(posts.indices)]
}

val fixtureFeedPosts = listOf(
    FeedPost(
        id = "post-demo-001",
        petId = "pet-stardust-001",
        title = "Stardust dragon launch pose",
        body = "First preview package imported from a gated fantasy-pet-rule run.",
        authorName = "Demo Keeper",
        reactionCount = 12
    ),
    FeedPost(
        id = "post-demo-002",
        petId = "pet-moonfox-001",
        title = "Moon fox sleepy loop",
        body = "A fixture post for testing pet-first feed navigation.",
        authorName = "Demo Keeper",
        reactionCount = 7
    )
)
