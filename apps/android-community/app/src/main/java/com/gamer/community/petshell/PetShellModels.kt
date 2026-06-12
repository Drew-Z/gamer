package com.gamer.community.petshell

enum class ShellPhase {
    DesktopPet,
    Community
}

enum class PetAction {
    Idle,
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

data class DefaultDesktopPetMotionSheet(
    val assetPath: String,
    val frameCount: Int,
    val loop: Boolean = true
)

data class DefaultDesktopPet(
    val id: String,
    val displayName: String,
    val elementLabel: String,
    val previewAssetPath: String,
    val sourceLabel: String,
    val motionLabel: String,
    val idleMotionSheetAssetPath: String = "",
    val idleMotionFrameCount: Int = 0,
    val actionMotionSheets: Map<PetAction, DefaultDesktopPetMotionSheet> = emptyMap()
)

data class PetShellState(
    val phase: ShellPhase,
    val petAction: PetAction,
    val speechBubble: String,
    val feedIndex: Int,
    val walletBalance: Int,
    val checkInClaimed: Boolean,
    val pendingSubmissionCount: Int = 0,
    val remoteCommunitySynced: Boolean = false,
    val approvedPets: List<ApprovedPet>,
    val approvedPetIndex: Int,
    val defaultDesktopPets: List<DefaultDesktopPet>,
    val selectedDefaultDesktopPetId: String,
    val posts: List<FeedPost>
)

fun defaultDesktopPets(): List<DefaultDesktopPet> = listOf(
    DefaultDesktopPet(
        id = "electric-dormouse-hd",
        displayName = "\u7535\u7cfb\u7075\u9f20",
        elementLabel = "\u7535\u7cfb",
        previewAssetPath = "default-pets/electric-dormouse-hd/preview.png",
        sourceLabel = "\u672c\u5730\u9ed8\u8ba4",
        motionLabel = "12 motion sheets",
        idleMotionSheetAssetPath = "default-pets/electric-dormouse-hd/motion_sheets/idle.png",
        idleMotionFrameCount = 24,
        actionMotionSheets = electricDormouseMotionSheets()
    ),
    DefaultDesktopPet(
        id = "moon-dew-fox-v0",
        displayName = "\u6708\u9732\u56e2\u72d0",
        elementLabel = "\u6708\u7cfb",
        previewAssetPath = "default-pets/moon-dew-fox-v0/preview.png",
        sourceLabel = "\u672c\u5730\u9ed8\u8ba4",
        motionLabel = "4 motion sheets",
        idleMotionSheetAssetPath = "default-pets/moon-dew-fox-v0/motion_sheets/idle.png",
        idleMotionFrameCount = 16,
        actionMotionSheets = moonDewFoxMotionSheets()
    ),
    DefaultDesktopPet(
        id = "fire-spirit-cat-demo",
        displayName = "\u706b\u7075\u732b",
        elementLabel = "\u706b\u7cfb",
        previewAssetPath = "default-pets/fire-spirit-cat-demo/preview.png",
        sourceLabel = "\u672c\u5730\u9ed8\u8ba4",
        motionLabel = "static poses"
    )
)

fun PetShellState.selectedDefaultDesktopPet(): DefaultDesktopPet? =
    defaultDesktopPets.firstOrNull { it.id == selectedDefaultDesktopPetId }
        ?: defaultDesktopPets.firstOrNull()

fun DefaultDesktopPet.motionSheetFor(action: PetAction): DefaultDesktopPetMotionSheet? =
    actionMotionSheets[action]
        ?: actionMotionSheets[PetAction.Idle]
        ?: idleMotionSheetAssetPath
            .takeIf { it.isNotBlank() && idleMotionFrameCount > 1 }
            ?.let { DefaultDesktopPetMotionSheet(it, idleMotionFrameCount) }

private fun electricDormouseMotionSheets(): Map<PetAction, DefaultDesktopPetMotionSheet> {
    val base = "default-pets/electric-dormouse-hd/motion_sheets"
    return mapOf(
        PetAction.Idle to motionSheet("$base/idle.png", frameCount = 24),
        PetAction.FeedNext to motionSheet("$base/running.png", frameCount = 16),
        PetAction.FeedPrevious to motionSheet("$base/curious_sniff.png", frameCount = 20, loop = false),
        PetAction.FeedSkip to motionSheet("$base/jumping.png", frameCount = 16, loop = false),
        PetAction.ShowcaseNext to motionSheet("$base/waving.png", frameCount = 18, loop = false),
        PetAction.ShowcasePrevious to motionSheet("$base/curious_sniff.png", frameCount = 20, loop = false),
        PetAction.Reward to motionSheet("$base/eat.png", frameCount = 24, loop = false),
        PetAction.Review to motionSheet("$base/review.png", frameCount = 16)
    )
}

private fun moonDewFoxMotionSheets(): Map<PetAction, DefaultDesktopPetMotionSheet> {
    val base = "default-pets/moon-dew-fox-v0/motion_sheets"
    return mapOf(
        PetAction.Idle to motionSheet("$base/idle.png", frameCount = 16),
        PetAction.FeedNext to motionSheet("$base/signature.png", frameCount = 18, loop = false),
        PetAction.FeedPrevious to motionSheet("$base/happy_click.png", frameCount = 14, loop = false),
        PetAction.FeedSkip to motionSheet("$base/signature.png", frameCount = 18, loop = false),
        PetAction.ShowcaseNext to motionSheet("$base/signature.png", frameCount = 18, loop = false),
        PetAction.ShowcasePrevious to motionSheet("$base/happy_click.png", frameCount = 14, loop = false),
        PetAction.Reward to motionSheet("$base/happy_click.png", frameCount = 14, loop = false),
        PetAction.Review to motionSheet("$base/sleepy.png", frameCount = 20)
    )
}

private fun motionSheet(
    assetPath: String,
    frameCount: Int,
    loop: Boolean = true
): DefaultDesktopPetMotionSheet =
    DefaultDesktopPetMotionSheet(
        assetPath = assetPath,
        frameCount = frameCount,
        loop = loop
    )
