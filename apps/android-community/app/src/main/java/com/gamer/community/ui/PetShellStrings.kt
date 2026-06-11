package com.gamer.community.ui

import com.gamer.community.generation.DEFAULT_GENERATION_MESSAGE
import com.gamer.community.petshell.ApprovedPet
import com.gamer.community.petshell.PetAction

enum class PetShellLanguage(val preferenceValue: String) {
    Chinese("zh"),
    English("en")
}

fun parsePetShellLanguage(rawValue: String?): PetShellLanguage =
    when (rawValue?.trim()?.lowercase()) {
        PetShellLanguage.English.preferenceValue,
        "english" -> PetShellLanguage.English
        else -> PetShellLanguage.Chinese
    }

fun defaultPetShellStrings(): PetShellStrings =
    petShellStrings(PetShellLanguage.Chinese)

fun petShellStrings(language: PetShellLanguage): PetShellStrings =
    PetShellStrings(language)

class PetShellStrings internal constructor(
    val language: PetShellLanguage
) {
    val communityTabLabel: String get() = text("\u793E\u533A", "Community")
    val generateTabLabel: String get() = text("孵化", "Hatch")
    val profileTabLabel: String get() = text("\u6211\u7684", "Mine")
    val communityTabContentDescription: String get() = "gamer-tab-community"
    val generateTabContentDescription: String get() = "gamer-tab-generate"
    val profileTabContentDescription: String get() = "gamer-tab-profile"
    val communityTabIconLabel: String get() = "community"
    val generateTabIconLabel: String get() = "generate"
    val profileTabIconLabel: String get() = "profile"
    val petAvatarContentDescription: String get() = "gamer-pet-avatar"
    val communityHomeContentDescription: String get() = "gamer-community-home"
    val generationWorkspaceContentDescription: String get() = "gamer-generation-workspace"
    val profileWorkspaceContentDescription: String get() = "gamer-profile-workspace"
    val headerUtilityDockContentDescription: String get() = "header-utility-dock"
    val communityChannelRailContentDescription: String get() = "community-channel-rail"
    val communityStatusSummaryContentDescription: String get() = "community-status-summary"
    val communityQuickActionsContentDescription: String get() = "community-quick-actions"
    val communityPetCompanionStripContentDescription: String get() = "community-pet-companion-strip"
    val communityShowcasePanelContentDescription: String get() = "community-showcase-panel"
    val communityPostCardContentDescription: String get() = "community-post-card"
    val communityFeedControlsContentDescription: String get() = "community-feed-controls"
    val generationStudioHeroContentDescription: String get() = "generation-studio-hero"
    val generationStudioStatusDockContentDescription: String get() = "generation-studio-status-dock"
    val generationPromptCanvasContentDescription: String get() = "generation-prompt-canvas"
    val hatcheryOverviewContentDescription: String get() = "generation-hatchery-overview"
    val generationRuntimeConsoleContentDescription: String get() = "generation-runtime-console"
    val generationReviewActionDockContentDescription: String get() = "generation-review-action-dock"
    val desktopPetModeContentDescription: String get() = "gamer-desktop-pet-mode"
    val directPetLaunchToggleContentDescription: String get() = "direct-pet-launch-toggle"
    val desktopPetOverlayToggleContentDescription: String get() = "desktop-pet-overlay-toggle"
    val desktopPetOverlayPermissionContentDescription: String get() = "desktop-pet-overlay-permission-button"
    val desktopPetNotificationPermissionContentDescription: String get() = "desktop-pet-notification-permission-button"
    val desktopPetOverlayResetPositionContentDescription: String get() = "desktop-pet-overlay-reset-position-button"
    val desktopPetOverlayActivePreviewContentDescription: String get() = "desktop-pet-overlay-active-preview"
    val desktopPetOverlayStartContentDescription: String get() = "desktop-pet-overlay-start-button"
    val desktopPetOverlayStopContentDescription: String get() = "desktop-pet-overlay-stop-button"
    val profileKeeperHeroContentDescription: String get() = "profile-keeper-hero"
    val profileWalletSummaryContentDescription: String get() = "profile-wallet-summary"
    val profilePetShelfContentDescription: String get() = "profile-pet-shelf"
    val profileActionDockContentDescription: String get() = "profile-action-dock"
    val generationFlowRailContentDescription: String get() = "generation-flow-rail"
    val generationBriefPanelContentDescription: String get() = "generation-brief-panel"
    val generationReviewDeskContentDescription: String get() = "generation-review-desk"
    val generationReviewWaitingContentDescription: String get() = "generation-review-waiting-candidate"
    val communityHomeTitle: String get() = text("\u793E\u533A\u52A8\u6001", "Community Feed")
    val communityPetCommandTitle: String get() = text("\u684C\u5BA0\u5BFC\u822A\u53F0", "Pet navigator")
    val communityPetCommandDetail: String
        get() = text(
            "\u4ECA\u65E5\u793E\u533A\u7531\u684C\u5BA0\u5E26\u8DEF\uFF1A\u7B7E\u5230\u3001\u751F\u6210\u3001\u5BA1\u6838\u548C\u5C55\u793A\u90FD\u4ECE\u8FD9\u91CC\u51FA\u53D1\u3002",
            "Your desktop pet leads today's check-in, creation, review, and showcase loop."
        )
    val communityPetCommandNextPost: String get() = text("\u770B\u4E0B\u4E00\u6761", "Next post")
    val communityPetCommandShowcase: String get() = text("\u53BB\u5C55\u793A", "Showcase")
    val communityStatusTitle: String get() = text("今日社区状态", "Today's community status")
    val communityStatusRemoteSynced: String get() = text("远端同步", "Remote synced")
    val communityStatusLocalFallback: String get() = text("本地兜底", "Local fallback")
    val communityStatusHumanReview: String get() = text("人审展示", "Human review")
    val communityFeedEmptyTitle: String get() = text("等待远端社区内容", "Waiting for remote community posts")
    val communityFeedEmptyDetail: String
        get() = text(
            "没有展示假帖子；远端同步后会显示真实社区内容。",
            "No placeholder posts are shown; real community content appears after remote sync."
        )
    val desktopPetModeTitle: String get() = text("桌宠模式", "Desktop pet mode")
    val desktopPetModeSubtitle: String
        get() = text(
            "让桌宠先出现，再进入社区、生成或我的主页。",
            "Let the pet appear first, then enter community, generation, or profile."
        )
    val desktopPetReadyLine: String
        get() = text(
            "桌宠已待机，远端数据会在后台同步到展示和宠物架。",
            "Desktop pet is idle; remote data syncs into showcase and shelf."
        )
    val desktopPetSyncTitle: String get() = text("正在同步远端桌宠", "Syncing remote pet")
    val desktopPetSyncDetail: String
        get() = text(
            "已连接 HidenCloud，预览图和评分会在社区数据返回后显示。",
            "Connected to HidenCloud; preview and score appear after community data returns."
        )
    val desktopPetHomeTitle: String get() = text("今日桌宠首页", "Today's pet home")
    val desktopPetHomeDetail: String
        get() = text(
            "从这里确认当前桌宠、奖励和人审状态。",
            "Check the active pet, rewards, and review state from here."
        )
    val desktopPetActivePetMetric: String get() = text("当前桌宠", "Active pet")
    val desktopPetActivePetReady: String get() = text("已通过桌宠", "Approved pet")
    val desktopPetActivePetMissing: String get() = text("等待已通过", "Awaiting approval")
    val desktopPetWalletMetric: String get() = text("钱包", "Wallet")
    val desktopPetCheckInMetric: String get() = text("签到", "Check-in")
    val desktopPetCheckInReady: String get() = text("可领取", "Ready")
    val desktopPetCheckInDone: String get() = text("已完成", "Done")
    val desktopPetPendingReviewMetric: String get() = text("待审", "Review")
    val desktopPetRemoteStatus: String get() = text("远端同步", "Remote sync")
    val desktopPetReviewStatus: String get() = text("人审后展示", "Human reviewed")
    fun directPetLaunchStatus(enabled: Boolean): String =
        if (enabled) {
            text("直达已开", "Direct on")
        } else {
            text("常规开屏", "Bubble first")
        }
    val desktopPetOpenCommunity: String get() = text("进入社区", "Community")
    val desktopPetOpenGenerate: String get() = text("孵化桌宠", "Hatch")
    val desktopPetOpenProfile: String get() = text("我的主页", "Profile")
    val desktopPetActionDockTitle: String get() = text("快速入口", "Quick entry")
    val desktopPetSettingsTitle: String get() = text("启动与桌宠设置", "Launch and pet settings")
    val directPetLaunchTitle: String get() = text("下次启动先显示 App 内桌宠页", "Open to in-app pet first")
    val directPetLaunchDetail: String
        get() = text(
            "关闭时保留常规气泡开屏；开启后点 App 图标会跳过气泡，先进入 App 内桌宠页。",
            "Off keeps the launch bubble; on skips the bubble and opens the in-app pet surface."
        )
    val enterDesktopPetMode: String get() = text("进入 App 内桌宠模式", "Enter in-app pet mode")
    val systemDesktopPetTitle: String get() = text("桌面悬浮桌宠", "Floating desktop pet")
    val systemDesktopPetDetail: String
        get() = text(
            "授权后，宠物可以显示在桌面和其他 App 上；关闭后仍可使用 App 内桌宠页。",
            "After permission is granted, the pet can float over the launcher and other apps."
        )
    val systemDesktopPetAutoShowTitle: String get() = text("点击图标直接唤出桌面桌宠", "App icon opens the floating pet")
    val systemDesktopPetAutoShowDetail: String
        get() = text(
            "下次从 App 图标进入时，如果已授权，会直接把宠物显示到桌面并让 App 退到后台。",
            "On the next icon launch, granted devices show the floating pet and send the app to the background."
        )
    fun systemDesktopPetAutoShowStatus(
        enabled: Boolean,
        ready: Boolean
    ): String =
        when {
            !enabled -> text("图标仍打开完整 App", "Icon opens the full app")
            ready -> text("下次点击图标会直接显示桌面桌宠", "Next icon tap shows the floating pet")
            else -> text("直达桌宠待授权", "Direct launch needs permission")
        }
    val desktopPetOverlayPermissionGranted: String get() = text("悬浮权限已开启", "Overlay allowed")
    val desktopPetOverlayPermissionMissing: String get() = text("需要悬浮权限", "Overlay permission needed")
    val desktopPetNotificationPermissionGranted: String get() = text("通知已开启", "Notifications allowed")
    val desktopPetNotificationPermissionMissing: String get() = text("需要通知权限", "Notification permission needed")
    val desktopPetOverlayRunning: String get() = text("桌宠显示中", "Pet showing")
    val desktopPetOverlayStopped: String get() = text("未显示", "Not showing")
    val desktopPetOverlayRequestPermission: String get() = text("授权悬浮窗", "Grant overlay")
    val desktopPetOverlayManagePermission: String get() = text("管理权限", "Manage permission")
    val desktopPetNotificationRequestPermission: String get() = text("授权通知", "Grant notifications")
    val desktopPetNotificationManagePermission: String get() = text("通知已授权", "Notifications granted")
    val desktopPetOverlayResetPosition: String get() = text("恢复默认位置", "Reset position")
    val desktopPetOverlayActivePreviewTitle: String get() = text("\u5f53\u524d\u684c\u9762\u684c\u5ba0", "Current desktop pet")
    val desktopPetOverlayActivePreviewMissing: String get() = text("\u7b49\u5f85\u5df2\u901a\u8fc7\u684c\u5ba0", "Awaiting approved pet")
    val desktopPetOverlayActivePreviewReady: String get() = text("\u8fdc\u7aef\u9884\u89c8\u53ef\u7528", "Remote preview ready")
    val desktopPetOverlayActivePreviewPending: String get() = text("\u9884\u89c8\u5f85\u540c\u6b65", "Preview pending")
    val desktopPetOverlayShow: String get() = text("显示到桌面", "Show on desktop")
    val desktopPetOverlayHide: String get() = text("关闭桌面桌宠", "Hide desktop pet")
    fun communityCommandStatus(
        approvedPetCount: Int,
        pendingSubmissionCount: Int,
        checkInClaimed: Boolean
    ): String {
        if (language == PetShellLanguage.English) {
            val checkIn = if (checkInClaimed) "checked in" else "check-in ready"
            return "$approvedPetCount approved / $pendingSubmissionCount pending / $checkIn"
        }

        val checkIn = if (checkInClaimed) "已签到" else "可签到"
        return "$approvedPetCount 个已通过 / $pendingSubmissionCount 个待审 / $checkIn"
    }
    val generationWorkspaceTitle: String get() = text("桌宠孵化室", "Pet Hatchery")
    val profileWorkspaceTitle: String get() = text("\u6211\u7684\u684C\u5BA0", "My Pets")
    val generationStudioHeroTitle: String get() = text("桌宠孵化室", "Pet Hatchery")
    val generationStudioHeroSubtitle: String
        get() = text(
            "从获得蛋、提示词孵化到人审入架，都在这里完成。",
            "Hatch an egg from prompt to human review and shelf delivery."
        )
    val hatcheryOverviewTitle: String get() = text("选择孵化方式", "Choose hatch mode")
    val hatcheryOverviewDetail: String
        get() = text(
            "积分换蛋后可随机孵化或自主孵化；当前开放神秘蛋和自主孵化。",
            "Spend petcoin for eggs, then hatch randomly or by prompt; mystery and custom hatch are available now."
        )
    val hatcheryWalletLabel: String get() = text("积分", "Petcoin")
    val hatcheryReserveRandomTitle: String get() = text("备用蛋", "Reserve egg")
    val hatcheryReserveRandomDetail: String get() = text("服务器备用桌宠，孵化短", "Server spare pet, short hatch")
    val hatcheryMysteryRandomTitle: String get() = text("神秘蛋", "Mystery egg")
    val hatcheryMysteryRandomDetail: String get() = text("随机提示词孵化，耗时长", "Random prompt, longer hatch")
    val hatcheryMysteryAction: String get() = text("随机孵化", "Random hatch")
    val hatcheryMysteryActionContentDescription: String get() = "generation-mystery-hatch-button"
    val hatcheryCustomTitle: String get() = text("自主孵化", "Custom hatch")
    val hatcheryCustomDetail: String get() = text("提示词、人审、资源包", "Prompt, review, package")
    val hatcheryModeActive: String get() = text("当前开放", "Available")
    val hatcheryModeComingSoon: String get() = text("待服务端开放", "Server pending")
    val hatcheryFinePathNotice: String
        get() = text(
            "精细孵化占位：以后可逐步返回人工审核，目前先使用标准自主孵化。",
            "Fine hatch placeholder: step-by-step review will come later; use standard custom hatch now."
        )
    val hatcheryProgressTitle: String get() = text("孵化进度", "Hatch progress")
    val hatcheryStepEgg: String get() = text("蛋", "Egg")
    val hatcheryStepPrompt: String get() = text("设定", "Prompt")
    val hatcheryStepIncubating: String get() = text("孵化", "Hatch")
    val hatcheryStepReview: String get() = text("人审", "Review")
    val hatcheryStepShelf: String get() = text("入架", "Shelf")
    fun hatcheryProgressStatus(activeStep: Int): String =
        when (activeStep) {
            0 -> text("等待选择孵化方式", "Choose a hatch mode")
            1 -> text("正在准备提示词", "Preparing prompt")
            2 -> text("服务端孵化中", "Server hatching")
            3 -> text("等待人工审核", "Waiting for review")
            else -> text("资源包就绪后入架", "Shelf after package is ready")
        }
    val generationFlowRailTitle: String get() = text("孵化流程", "Hatch flow")
    val generationBriefPanelTitle: String get() = text("自主孵化", "Custom hatch")
    val generationReviewDeskTitle: String get() = text("人审与入架", "Review and shelf")
    val generationFlowBriefStep: String get() = text("设定", "Prompt")
    val generationFlowCandidateStep: String get() = text("候选", "Candidate")
    val generationFlowReviewStep: String get() = text("人审", "Review")
    val generationFlowPackageStep: String get() = text("入架", "Shelf")
    val generationPromptStageTitle: String get() = text("桌宠提示", "Pet prompt")
    val generationPromptStageHint: String get() = text("用一句话描述身份、动作和气质。", "Describe identity, motion, and mood in one concise brief.")
    val generationPromptIdeaAction: String get() = text("随机设定", "Random prompt")
    val generationPromptIdeaContentDescription: String get() = "generation-prompt-idea-button"
    val generationPromptIdeaText: String
        get() = text(
            "薄荷色守护者桌宠，待机轻轻漂浮，跑动时尾巴弹跳。",
            "Mint guardian pet, gentle idle bob, springy tail run."
        )
    fun hatcheryMysteryPrompt(seed: Int): String = hatcheryRandomPrompt(seed)

    fun hatcheryRandomPrompt(seed: Int): String {
        val index = seed.coerceAtLeast(0) % 6
        return listOf(
            text(
                "星蓝色猫头鹰守护蛋，idle 时眨眼发光，跑动时羽翼拖出金色星尘。",
                "Star-blue owl guardian pet, glowing blink idle, golden stardust wing trail while running."
            ),
            text(
                "琥珀色小龙桌宠，蜷在蛋壳边醒来，跳跃时尾巴点亮火花。",
                "Amber baby dragon pet waking beside its shell, tail sparks during jumps."
            ),
            text(
                "水晶鹿桌宠，半透明角发出柔光，待机时像月光一样轻轻漂浮。",
                "Crystal deer pet with softly glowing antlers, moonlit floating idle."
            ),
            text(
                "云朵企鹅桌宠，抱着小蛋左右摇摆，跑动时脚下出现软软云影。",
                "Cloud penguin pet hugging a tiny egg, waddling with soft cloud shadows."
            ),
            text(
                "机械狐桌宠，耳朵有青色能量环，idle 时尾巴像小雷达摆动。",
                "Mechanical fox pet with cyan energy rings on ears and a radar-like tail idle."
            ),
            text(
                "樱粉色果冻兔桌宠，身体弹性轻晃，受到关注时冒出小星星。",
                "Cherry-pink jelly rabbit pet with bouncy body motion and tiny stars when noticed."
            )
        )[index]
    }
    val generationTaskStageTitle: String get() = text("孵化设置", "Hatch setup")
    val generationTaskStageHint: String get() = text("任务名称可选；参考图必须是 HTTP/HTTPS URL。", "Task name is optional; reference images must use HTTP/HTTPS URLs.")
    val generationBodyStageTitle: String get() = text("体型预设", "Body preset")
    val generationBodyStageHint: String get() = text("选择孵化服务支持的桌宠体型。", "Choose a supported desktop pet body shape.")
    val generationRunStageTitle: String get() = text("开始孵化", "Start hatch")
    val generationRunStageHint: String get() = text("App 只创建和轮询孵化任务，不启动生成 worker。", "The app only creates and polls hatch jobs; it never starts generation workers.")
    val generationReviewWaitingForCandidate: String
        get() = text(
            "候选图会在孵化完成后出现在这里，选择 candidate 后才能提交人审。",
            "Candidates appear here when hatching reaches review; select a candidate before review."
        )
    val generationWorkspaceSubtitle: String
        get() = text(
            "从蛋、提示词到人审入架的 public API 闭环。",
            "Public API loop from egg and prompt to review and shelf."
        )
    val profileWorkspaceSubtitle: String
        get() = text(
            "\u67E5\u770B\u94B1\u5305\u3001\u7B7E\u5230\u548C\u5DF2\u901A\u8FC7\u684C\u5BA0\u3002",
            "Wallet, check-in, and approved desktop pets."
        )
    val profileKeeperName: String get() = text("Demo Keeper", "Demo Keeper")
    val profileKeeperRole: String get() = text("\u684C\u5BA0\u5B88\u62A4\u8005", "Desktop pet keeper")
    val profileWalletSummaryTitle: String get() = text("\u8D44\u4EA7\u6982\u89C8", "Asset overview")
    val profileApprovedPetsMetric: String get() = text("\u5DF2\u901A\u8FC7", "Approved")
    val approvedPetScoreMetric: String get() = text("评分", "Score")
    val approvedPetMotionMetric: String get() = text("动作表", "Sheets")
    val approvedPetPreviewMetric: String get() = text("预览", "Preview")
    val approvedPetPreviewReady: String get() = text("远端可看", "Remote ready")
    val approvedPetPreviewPending: String get() = text("待生成", "Pending")
    val profilePetShelfTitle: String get() = text("\u684C\u5BA0\u5C55\u67B6", "Pet shelf")
    val profileActionDockTitle: String get() = text("\u5E38\u7528\u52A8\u4F5C", "Quick actions")
    val profileCreatePetAction: String get() = text("孵化新桌宠", "Hatch new pet")
    val showcasePathGenerate: String get() = text("孵化", "Hatch")
    val showcasePathReview: String get() = text("\u4EBA\u5BA1", "Review")
    val showcasePathPublish: String get() = text("\u5C55\u793A", "Showcase")
    val chineseLanguageLabel: String = "中文"
    val englishLanguageLabel: String = "EN"
    val chineseLanguageToggleContentDescription: String get() = text("切换到中文", "Switch to Chinese")
    val englishLanguageToggleContentDescription: String get() = text("切换到英文", "Switch to English")
    val petBadge: String get() = text("宠", "PET")
    val communityTitle: String get() = text("玩家社区", "Gamer Community")
    val communitySubtitle: String get() = text("桌宠带路的每日社区", "Daily community led by your desktop pet")
    val communityChannelRecommended: String get() = text("推荐", "For you")
    val communityChannelCreations: String get() = text("作品", "Creations")
    val communityChannelGuides: String get() = text("攻略", "Guides")
    val communityChannelEvents: String get() = text("活动", "Events")
    val communityFeedSignalTitle: String get() = text("\u684C\u5BA0\u89C6\u89D2", "Pet view")
    val communityFeedSignalDetail: String
        get() = text(
            "\u7528\u684C\u5BA0\u52A8\u4F5C\u6D4F\u89C8\u5E16\u5B50\uFF1A\u4E0A\u4E00\u9875\u3001\u4E0B\u4E00\u9875\u3001\u4E0B\u4E0B\u9875\u3002",
            "Browse posts with pet actions: previous, next, or skip ahead."
        )
    val feedReactionLabel: String get() = text("\u4E92\u52A8", "Reactions")
    val quickActionCheckIn: String get() = text("签到", "Check in")
    val quickActionCheckInDetail: String get() = text("+10 宠物币", "+10 petcoin")
    val quickActionGenerate: String get() = text("孵化", "Hatch")
    val quickActionGenerateDetail: String get() = text("新桌宠", "New pet")
    val quickActionReview: String get() = text("审核", "Review")
    val quickActionReviewDetail: String get() = text("候选图", "Candidates")
    fun quickActionReviewStatus(pendingSubmissionCount: Int): String =
        if (pendingSubmissionCount > 0) {
            text("待审 $pendingSubmissionCount", "$pendingSubmissionCount pending")
        } else {
            quickActionReviewDetail
        }
    val quickActionShowcase: String get() = text("展示", "Showcase")
    val quickActionShowcaseDetail: String get() = text("广场", "Gallery")
    val launchEnterHint: String get() = text("点击气泡进入", "Tap the bubble to enter")
    val generatePanelTitle: String get() = text("孵化桌宠", "Hatch Desktop Pet")
    val generationPublicApiBoundaryNotice: String
        get() = text(
            "公共 API 只创建和轮询孵化任务；真实生成 worker 需要在服务端单独启动。",
            "Public API only creates and polls hatch jobs; live generation workers must be started on the server side."
        )
    val descriptionLabel: String get() = text("文字描述", "Description")
    val requiredFieldSuffix: String get() = text("（必填）", " (required)")
    val appJobIdLabel: String get() = text("任务名称", "Task name")
    val referenceUrlsLabel: String get() = text("参考图 URL", "Reference URLs")
    val createGenerationJob: String get() = text("开始自主孵化", "Start custom hatch")
    val checkGenerationService: String get() = text("检查孵化服务", "Check hatch service")
    val pollJob: String get() = text("刷新孵化状态", "Poll hatch")
    val recentJobs: String get() = text("最近任务", "Recent jobs")
    val resume: String get() = text("继续", "Resume")
    val remove: String get() = text("移除", "Remove")
    val clearSavedJob: String get() = text("清除已保存任务", "Clear saved job")
    val candidateGalleryTitle: String get() = text("候选图画廊", "Candidate gallery")
    val candidateInspectionTitle: String get() = text("候选检查", "Candidate inspection")
    val reviewNotesStageTitle: String get() = text("人审备注", "Human review notes")
    val deliveryActionsTitle: String get() = text("交付动作", "Delivery actions")
    val candidateReadyForInspection: String get() = text("候选图已就绪，请选择一个作为人审对象。", "Candidates are ready; select one for human review.")
    val candidateWaitingForInspection: String get() = text("等待服务端发布 candidate 后进入人审。", "Waiting for the server to publish a candidate.")
    val candidateSelectedStatus: String get() = text("已选中", "Selected")
    val candidateAvailableStatus: String get() = text("待选择", "Available")
    val deliveryActionsHint: String get() = text("先完成人审，只有资源包就绪后才能接收 pet.zip。", "Review first; pet.zip is available only after the package is ready.")
    val deliveryReviewTargetStatus: String get() = text("审核对象", "Review target")
    val deliveryPackageStatus: String get() = text("资源包", "Package")
    val deliveryCommunityStatus: String get() = text("社区提交", "Community")
    val deliveryStatusSelected: String get() = text("已选择", "Selected")
    val deliveryStatusWaiting: String get() = text("等待", "Waiting")
    val deliveryStatusPackageReady: String get() = text("可下载", "Ready")
    val deliveryStatusPackageLocked: String get() = text("未就绪", "Locked")
    val deliveryStatusCommunityReady: String get() = text("可提交", "Ready")
    val deliveryStatusCommunityWaiting: String get() = text("待草稿", "Draft pending")
    val selectedForReview: String get() = text("已选为审核对象", "Selected for review")
    val selectCandidate: String get() = text("选择候选图", "Select candidate")
    val reviewNotesLabel: String get() = text("审核备注", "Review notes")
    val reviewNotesPlaceholder: String
        get() = text(
            "例如：idle 动作上下跳动明显、主体身份漂移、朝向错误",
            "idle jumps, motion static, identity drift, wrong facing"
        )
    val reviewAccept: String get() = text("接受", "Accept")
    val reviewRevise: String get() = text("要求修订", "Revise")
    val reviewReject: String get() = text("拒绝", "Reject")
    val downloadPetZip: String get() = text("接收 pet.zip", "Receive pet.zip")
    val submitToCommunityReview: String get() = text("提交到社区审核", "Submit to community review")
    val refreshCommunitySubmission: String get() = text("刷新社区提交", "Refresh community submission")
    val petPrev: String get() = text("上一个桌宠", "Pet Prev")
    val petNext: String get() = text("下一个桌宠", "Pet Next")
    val prev: String get() = text("上一页", "Prev")
    val next: String get() = text("下一页", "Next")
    val skip: String get() = text("下下页", "Skip")
    val checkedIn: String get() = text("已签到", "Checked in")
    val dailyCheckIn: String get() = text("每日签到", "Daily check-in")
    val candidatePreviewContentDescription: String get() = text("候选图预览", "Candidate preview")
    val launchBubbleEnterContentDescription: String get() = "launch-bubble-enter"
    val generationDescriptionContentDescription: String get() = "generation-description-input"
    val appJobIdContentDescription: String get() = "generation-app-job-id-input"
    val referenceUrlsContentDescription: String get() = "generation-reference-url-input"
    val checkGenerationServiceContentDescription: String get() = "generation-check-service-button"
    val pollJobContentDescription: String get() = "generation-poll-job-button"
    val reviewNotesContentDescription: String get() = "generation-review-notes-input"
    val reviewAcceptContentDescription: String get() = "generation-review-accept-button"
    val reviewReviseContentDescription: String get() = "generation-review-revise-button"
    val reviewRejectContentDescription: String get() = "generation-review-reject-button"
    val packageDownloadContentDescription: String get() = "generation-package-download-button"
    val submitCommunityReviewContentDescription: String get() = "generation-submit-community-review-button"
    val refreshCommunitySubmissionContentDescription: String get() = "generation-refresh-community-submission-button"
    val contractDemoNoticeContentDescription: String get() = "generation-contract-demo-notice"
    val contractDemoNoLiveWorkerContentDescription: String get() = "generation-contract-demo-no-live-worker"
    val generationPublicApiBoundaryContentDescription: String get() = "generation-public-api-boundary-notice"
    val serverWorkerWaitNoticeContentDescription: String get() = "generation-server-worker-wait-notice"

    fun candidateSelectContentDescription(targetDownloadId: String): String =
        "generation-candidate-select-${targetDownloadId.safeGenerationMessageDetail("candidate")}"
    val previewUnavailable: String get() = text("预览不可用", "Preview unavailable")
    val loadingPreview: String get() = text("正在加载预览...", "Loading preview...")

    fun bodyShapeLabel(bodyShape: String): String =
        if (language == PetShellLanguage.English) {
            bodyShape
        } else {
            when (bodyShape) {
                "balanced" -> "均衡"
                "wide" -> "宽体"
                "wide-tail" -> "宽体长尾"
                "tall" -> "高挑"
                else -> bodyShape
            }
        }

    fun speechBubble(rawMessage: String): String {
        if (language == PetShellLanguage.English) return rawMessage

        return when (rawMessage) {
            "Loading community..." -> "正在加载社区..."
            "Desktop pet ready." -> "桌宠已待机。"
            "Welcome back, Demo Keeper." -> "欢迎回来，Demo Keeper。"
            "Community ready." -> "社区已就绪。"
            "Community home ready." -> "社区主页已就绪。"
            "Local fallback active." -> "本地兜底数据已启用。"
            "Daily reward already claimed." -> "今日奖励已经领取。"
            "No approved pets ready yet." -> "暂时还没有已通过的桌宠。"
            else -> localizedSpeechBubble(rawMessage)
        }
    }

    fun petActionLabel(action: PetAction): String =
        if (language == PetShellLanguage.English) {
            action.name
        } else {
            when (action) {
                PetAction.Idle -> "待机"
                PetAction.AppLoading -> "正在加载应用"
                PetAction.BubbleOpen -> "打开气泡"
                PetAction.FeedNext -> "下一页"
                PetAction.FeedPrevious -> "上一页"
                PetAction.FeedSkip -> "下下页"
                PetAction.ShowcaseNext -> "展示下一个"
                PetAction.ShowcasePrevious -> "展示上一个"
                PetAction.Reward -> "领取奖励"
                PetAction.Review -> "审核"
            }
        }

    fun generationMessage(rawMessage: String): String {
        if (language == PetShellLanguage.English) return englishGenerationMessage(rawMessage)

        return when (rawMessage) {
            DEFAULT_GENERATION_MESSAGE -> "描述一个桌宠，开始孵化。"
            "Creating mystery hatch job..." -> "正在创建神秘蛋孵化任务..."
            "Creating generation job..." -> "正在创建孵化任务..."
            "Resuming generation job..." -> "正在恢复孵化任务..."
            "Resuming recent generation job..." -> "正在继续最近的孵化任务..."
            "Recent generation job removed." -> "最近孵化任务已移除。"
            "Recent generation job is unavailable." -> "最近孵化任务不可用。"
            "Checking generation service..." -> "正在检查孵化服务..."
            "Submitting human review..." -> "正在提交人工审核..."
            "Polling generation job..." -> "正在刷新孵化任务..."
            "Contract demo task: this candidate is pre-seeded for public API validation; it is not a live pet generation run." ->
                "这是公共 API 契约演示任务：候选图是服务端预置的验证资源，不代表真实桌宠生成链路已运行。"
            "Contract demo fixture loaded; no live generation worker has run." ->
                "契约演示资源已载入；真实生成 worker 尚未运行。"
            "Waiting for a trusted server worker; this app only created and polls the job." ->
                "正在等待可信服务端 worker；app 只负责创建和轮询任务。"
            "Feedback recorded; a trusted server worker must publish the next candidate." ->
                "反馈已记录；需要可信服务端 worker 发布新的候选图。"
            "Create a generation job before review." -> "请先开始孵化，再提交审核。"
            "Create a generation job before download." -> "请先开始孵化，再接收资源包。"
            "Downloading pet.zip..." -> "正在下载 pet.zip..."
            "Description is required." -> "请填写桌宠描述。"
            "Description must be 4000 characters or fewer." -> "描述不能超过 4000 个字符。"
            "Choose a supported body shape." -> "请选择支持的体型。"
            "Use at most 8 reference URLs." -> "最多填写 8 个参考图 URL。"
            "Reference URLs must use HTTP or HTTPS." -> "参考图 URL 必须使用 HTTP 或 HTTPS。"
            "Task name can use letters, numbers, dot, underscore, or dash." ->
                "任务名称只能使用字母、数字、点、下划线或短横线。"
            "Enter a task name to poll." -> "请输入要刷新的任务名称。"
            "Revise and reject need specific visual notes." -> "修订或拒绝时需要填写具体视觉备注。"
            "Review notes cannot include internal paths or worker details." ->
                "审核备注不能包含内部路径或 worker 细节。"
            "pet.zip is ready to download." -> "pet.zip 已可下载。"
            "Waiting for generation worker." -> "正在等待孵化服务。"
            "Generating candidate assets." -> "正在生成候选资源。"
            "Waiting for worker output." -> "正在等待生成结果。"
            "Packaging pet.zip." -> "正在打包 pet.zip。"
            "Revision requested; waiting for a revised candidate." -> "已请求修订，正在等待新候选图。"
            "Candidate rejected; waiting for a new candidate." -> "候选图已拒绝，正在等待新候选图。"
            "Generation failed." -> "孵化失败。"
            "Queued" -> "已排队"
            "Generating" -> "生成中"
            "Ready for human review" -> "等待人工审核"
            "Packaging pet.zip" -> "正在打包 pet.zip"
            "Ready for download" -> "可以下载"
            "Revision requested" -> "已请求修订"
            "Candidate rejected" -> "候选图已拒绝"
            "Failed" -> "失败"
            "Waiting" -> "等待中"
            "Generation service ready." -> "孵化服务已就绪。"
            "Generation service blocked: unsafe readiness report." -> "孵化服务状态被阻止：报告不安全。"
            "Generation API contract blocked: unsafe public boundary." ->
                "生成 API 合约被阻止：公共边界不安全。"
            "Community import blocked." -> "社区导入被阻止。"
            "Creating community import draft..." -> "正在创建社区导入草稿..."
            "Submitting community import draft..." -> "正在提交社区导入草稿..."
            "Community submission blocked." -> "社区提交被阻止。"
            "Community submission status unavailable." -> "社区提交状态不可用。"
            else -> localizedGenerationMessage(rawMessage)
        }
    }

    fun progressLabel(rawLabel: String): String =
        if (language == PetShellLanguage.English) {
            rawLabel
        } else {
            when (rawLabel) {
                "Candidate generation" -> "候选图生成"
                "Generation orchestration" -> "孵化调度"
                "Planning" -> "规划"
                else -> rawLabel
            }
        }

    fun progressStatus(rawStatus: String): String =
        generationMessage(rawStatus).ifBlank { rawStatus }

    fun jobLabel(appJobId: String): String =
        text("最近任务", "Saved task")

    fun candidateTitle(rawTitle: String, index: Int): String =
        if (language == PetShellLanguage.English) rawTitle else "候选图 ${index + 1}"

    fun reviewNoteSuggestion(rawSuggestion: String): String =
        if (language == PetShellLanguage.English) {
            rawSuggestion
        } else {
            when (rawSuggestion) {
                "idle action jumps vertically" -> "idle 动作上下跳动明显"
                "running-right is nearly static" -> "running-right 几乎不动"
                "first and last frame mismatch" -> "首尾帧不一致"
                "main identity drifts" -> "主体身份漂移"
                "detached effect particles" -> "有漂浮特效粒子"
                "wrong facing direction" -> "朝向错误"
                "loop boundary is abrupt" -> "循环边界突兀"
                else -> rawSuggestion
            }
        }

    fun walletBalance(balance: Int): String =
        text("$balance 宠物币", "$balance petcoin")

    fun feedMetadataLabel(label: String): String {
        if (language == PetShellLanguage.English) {
            return when {
                label.startsWith("Draft ") -> "Draft ready"
                label.startsWith("Submission ") -> "Review submitted"
                label.startsWith("Score ") -> "Score ready"
                label.startsWith("Source ") -> "Reviewed source"
                label.startsWith("Preview ") -> "Preview ready"
                label.startsWith("Package ") -> "Package ready"
                else -> label
            }
        }

        return when {
            label == "Approved import" -> "已通过导入"
            label == "Draft ready" -> "草稿已就绪"
            label == "Review submitted" -> "已提交审核"
            label == "Score ready" -> "评分已就绪"
            label == "Reviewed source" -> "来源已审核"
            label == "Preview ready" -> "预览可用"
            label == "Package ready" -> "资源包可用"
            label.endsWith(" petcoin") -> label.replace(" petcoin", " 宠物币")
            label.startsWith("Draft ") -> "草稿已就绪"
            label.startsWith("Submission ") -> "已提交审核"
            label.startsWith("Score ") -> "评分已就绪"
            label.startsWith("Source ") -> "来源已审核"
            label.startsWith("Preview ") -> "预览可用"
            label.startsWith("Package ") -> "资源包可用"
            label.endsWith(" motion sheets") -> label.replace(" motion sheets", " 张动作表")
            else -> label
        }
    }

    fun postFooter(authorName: String, petId: String, reactionCount: Int): String =
        text(
            "$authorName / $petId / $reactionCount 次互动",
            "$authorName / $petId / $reactionCount reactions"
        )

    fun approvedPetRegistrySummary(pets: List<ApprovedPet>): String {
        if (language == PetShellLanguage.English) {
            return if (pets.isEmpty()) {
                "No approved pets yet"
            } else {
                "${pets.size} approved pet${if (pets.size == 1) "" else "s"}"
            }
        }
        return if (pets.isEmpty()) {
            "暂时还没有已通过的桌宠"
        } else {
            "${pets.size} 个已通过桌宠"
        }
    }

    fun approvedPetShowcaseTitle(
        pets: List<ApprovedPet>,
        selectedIndex: Int
    ): String {
        if (language == PetShellLanguage.English) {
            return if (selectedApprovedPet(pets, selectedIndex) == null) {
                "Awaiting approved pet"
            } else {
                "Approved desktop pet"
            }
        }
        return if (selectedApprovedPet(pets, selectedIndex) == null) {
            "等待已通过桌宠"
        } else {
            "已通过桌宠"
        }
    }

    fun approvedPetShowcaseDetail(
        pets: List<ApprovedPet>,
        selectedIndex: Int
    ): String {
        if (language == PetShellLanguage.English) {
            val pet = selectedApprovedPet(pets, selectedIndex) ?: return "Approved imports will appear here."
            return "Reviewed / score ${pet.totalScore} / ${pet.motionSheetCount} motion sheets"
        }
        val pet = selectedApprovedPet(pets, selectedIndex) ?: return "通过审核的导入会显示在这里。"
        return "已通过 / 评分 ${pet.totalScore} / ${pet.motionSheetCount} 张动作表"
    }

    fun approvedPetShowcasePosition(
        pets: List<ApprovedPet>,
        selectedIndex: Int
    ): String {
        if (language == PetShellLanguage.English) {
            if (pets.isEmpty()) return "No showcase selection"
            val displayIndex = if (selectedIndex in pets.indices) selectedIndex else 0
            return "Pet ${displayIndex + 1} of ${pets.size}"
        }
        if (pets.isEmpty()) return "暂无展示选择"
        val displayIndex = if (selectedIndex in pets.indices) selectedIndex else 0
        return "桌宠 ${displayIndex + 1} / ${pets.size}"
    }

    fun approvedPetShowcaseAsset(
        pets: List<ApprovedPet>,
        selectedIndex: Int
    ): String {
        if (language == PetShellLanguage.English) {
            val pet = selectedApprovedPet(pets, selectedIndex) ?: return "Remote preview pending"
            return if (pet.previewUrl.isNotBlank() ||
                pet.targetDownloadId.isNotBlank() ||
                pet.previewPath.isNotBlank()
            ) {
                "Remote preview ready"
            } else {
                "Remote preview pending"
            }
        }
        val pet = selectedApprovedPet(pets, selectedIndex) ?: return "远端预览待同步"
        return if (pet.previewUrl.isNotBlank() ||
            pet.targetDownloadId.isNotBlank() ||
            pet.previewPath.isNotBlank()
        ) {
            "远端预览可用"
        } else {
            "远端预览待同步"
        }
    }

    fun approvedPetShowcasePackage(
        pets: List<ApprovedPet>,
        selectedIndex: Int
    ): String {
        if (language == PetShellLanguage.English) {
            val pet = selectedApprovedPet(pets, selectedIndex) ?: return "Package pending"
            return if (pet.exportArtifactPath.isBlank()) {
                "Package pending"
            } else {
                "Package ready"
            }
        }
        val pet = selectedApprovedPet(pets, selectedIndex) ?: return "资源包待同步"
        return if (pet.exportArtifactPath.isBlank()) {
            "资源包待同步"
        } else {
            "资源包可用"
        }
    }

    fun approvedPetSourceLine(pet: ApprovedPet): String {
        val previewReady = pet.previewUrl.trim().isNotBlank() ||
            pet.targetDownloadId.trim().isNotBlank() ||
            pet.previewPath.trim().isNotBlank()
        if (language == PetShellLanguage.English) {
            return "Human reviewed / remote preview ${if (previewReady) "ready" else "pending"}"
        }

        return "人审已通过 / 远端预览${if (previewReady) "可用" else "待同步"}"
    }

    private fun text(chinese: String, english: String): String =
        if (language == PetShellLanguage.Chinese) chinese else english

    private fun englishGenerationMessage(rawMessage: String): String {
        englishDynamicGenerationMessage(rawMessage, "Generation poll failed: ")?.let { return it }
        englishDynamicGenerationMessage(rawMessage, "Saved generation job unavailable: ")?.let { return it }
        englishDynamicGenerationMessage(rawMessage, "Generation request failed: ")?.let { return it }
        englishReviewFailureMessage(rawMessage)?.let { return it }
        englishDynamicGenerationMessage(rawMessage, "Review failed: ")?.let { return it }
        englishDynamicGenerationMessage(rawMessage, "Generation API contract check failed: ")?.let { return it }
        englishDynamicGenerationMessage(rawMessage, "Generation service check failed: ")?.let { return it }
        englishDynamicGenerationMessage(rawMessage, "Package download blocked: ")?.let { return it }
        englishDynamicGenerationMessage(rawMessage, "Community import pending: ")?.let { return it }
        englishDynamicGenerationMessage(rawMessage, "Community import blocked because ")?.let { return it }
        englishDynamicGenerationMessage(rawMessage, "Community submission blocked because ")?.let { return it }
        return rawMessage.takeIf { it.isSafeGenerationMessageText() } ?: "Message unavailable."
    }

    private fun englishDynamicGenerationMessage(
        rawMessage: String,
        prefix: String
    ): String? {
        if (!rawMessage.startsWith(prefix)) return null
        return prefix + rawMessage.removePrefix(prefix).safeGenerationMessageDetail("unavailable")
    }

    private fun localizedSpeechBubble(rawMessage: String): String {
        if (rawMessage.startsWith("Daily reward claimed: +") && rawMessage.endsWith(" petcoin.")) {
            val amount = rawMessage
                .removePrefix("Daily reward claimed: +")
                .removeSuffix(" petcoin.")
            return "每日奖励已领取：+$amount 宠物币。"
        }
        if (rawMessage.startsWith("Showing approved pet ") && rawMessage.endsWith(".")) {
            val petName = rawMessage
                .removePrefix("Showing approved pet ")
                .removeSuffix(".")
            return "正在展示已通过桌宠 $petName。"
        }
        if (rawMessage.startsWith("Showing ") && rawMessage.endsWith(".")) {
            val title = rawMessage.removePrefix("Showing ").removeSuffix(".")
            return "正在展示 $title。"
        }
        return rawMessage
    }

    private fun localizedGenerationMessage(rawMessage: String): String {
        val oneCandidateSuffix = " candidate ready for human review."
        val manyCandidatesSuffix = " candidates ready for human review."
        return when {
            rawMessage.startsWith("Generation poll failed: ") ->
                localizedDynamicGenerationMessage(rawMessage, "Generation poll failed: ", "孵化状态刷新失败: ")
            rawMessage.startsWith("Saved generation job unavailable: ") ->
                localizedDynamicGenerationMessage(
                    rawMessage,
                    "Saved generation job unavailable: ",
                    "已保存孵化任务不可用: "
                )
            rawMessage.startsWith("Generation request failed: ") ->
                localizedDynamicGenerationMessage(rawMessage, "Generation request failed: ", "孵化请求失败: ")
            rawMessage == "Review failed: contract_demo_job_review_disabled" ->
                "审核失败: 这是公共 API 契约演示任务，不能作为真实孵化任务提交人审。"
            rawMessage == "Review failed: review_target_already_decided" ->
                "人审提交失败: 这个候选图已经审核过，请等待新的候选图。"
            rawMessage == "Review failed: review_target_must_be_candidate" ->
                "人审提交失败: 请选择未审核的候选图。"
            rawMessage == "Review failed: target_download_id_required" ->
                "人审提交失败: 请先选择候选图。"
            rawMessage == "Review failed: invalid_review_decision" ->
                "人审提交失败: 请选择接受、修订或拒绝。"
            rawMessage == "Review failed: review_notes_required" ->
                "人审提交失败: 修订或拒绝时需要填写具体视觉备注。"
            rawMessage == "Review failed: review_notes_must_not_include_internal_paths" ->
                "人审提交失败: 审核备注不能包含内部路径或 worker 细节。"
            rawMessage.startsWith("Review failed: ") ->
                localizedDynamicGenerationMessage(rawMessage, "Review failed: ", "人审提交失败: ")
            rawMessage.startsWith("Generation API contract check failed: ") ->
                localizedDynamicGenerationMessage(
                    rawMessage,
                    "Generation API contract check failed: ",
                    "孵化 API 合约检查失败: "
                )
            rawMessage.startsWith("Generation service check failed: ") ->
                localizedDynamicGenerationMessage(
                    rawMessage,
                    "Generation service check failed: ",
                    "孵化服务检查失败: "
                )
            rawMessage.startsWith("Downloaded ") && rawMessage.endsWith(" to app downloads.") -> {
                val fileName = rawMessage
                    .removePrefix("Downloaded ")
                    .removeSuffix(" to app downloads.")
                    .safeGenerationMessageDetail("pet.zip")
                "已接收 $fileName 到 app 下载目录。"
            }
            rawMessage.startsWith("Package download blocked: ") ->
                localizedDynamicGenerationMessage(rawMessage, "Package download blocked: ", "资源包接收被阻止: ")
            rawMessage == "Package download blocked." -> "资源包接收被阻止。"
            rawMessage.startsWith("Community import pending: ") ->
                localizedDynamicGenerationMessage(rawMessage, "Community import pending: ", "社区导入待处理: ")
            rawMessage.startsWith("Community import draft ") ->
                if (rawMessage.isSafeGenerationMessageText()) {
                    rawMessage
                        .replacePrefix("Community import draft ", "社区导入草稿 ")
                        .replace(" ready for ", " 已为 ")
                        .replaceSuffix(".", " 准备就绪。")
                } else {
                    "社区导入草稿状态不可用。"
                }
            rawMessage.startsWith("Community import blocked because ") ->
                localizedDynamicGenerationMessage(
                    rawMessage,
                    "Community import blocked because ",
                    "社区导入被阻止，原因："
                )
            rawMessage.startsWith("Community submission blocked because ") ->
                localizedDynamicGenerationMessage(
                    rawMessage,
                    "Community submission blocked because ",
                    "社区提交被阻止，原因："
                )
            rawMessage.startsWith("Community submission ") ->
                localizedDynamicGenerationMessage(rawMessage, "Community submission ", "社区提交 ")
            rawMessage.endsWith(oneCandidateSuffix) -> {
                val count = rawMessage
                    .removeSuffix(oneCandidateSuffix)
                    .safeGenerationMessageDetail("1")
                "$count 个候选图等待人工审核。"
            }
            rawMessage.endsWith(manyCandidatesSuffix) -> {
                val count = rawMessage
                    .removeSuffix(manyCandidatesSuffix)
                    .safeGenerationMessageDetail("多个")
                "$count 个候选图等待人工审核。"
            }
            else -> rawMessage.takeIf { it.isSafeGenerationMessageText() } ?: "消息不可显示。"
        }
    }

    private fun localizedDynamicGenerationMessage(
        rawMessage: String,
        prefix: String,
        replacement: String
    ): String =
        replacement + rawMessage
            .removePrefix(prefix)
            .removeSuffix(".")
            .safeGenerationMessageDetail("不可显示")

    private fun englishReviewFailureMessage(rawMessage: String): String? =
        when (rawMessage) {
            "Review failed: review_target_already_decided" ->
                "Review failed: this candidate has already been reviewed; wait for a new candidate."
            "Review failed: review_target_must_be_candidate" ->
                "Review failed: select an unreviewed candidate image."
            "Review failed: target_download_id_required" ->
                "Review failed: select a candidate image first."
            "Review failed: invalid_review_decision" ->
                "Review failed: choose accept, revise, or reject."
            "Review failed: review_notes_required" ->
                "Review failed: revise and reject need specific visual notes."
            "Review failed: review_notes_must_not_include_internal_paths" ->
                "Review failed: review notes cannot include internal paths or worker details."
            else -> null
        }
}

private fun selectedApprovedPet(pets: List<ApprovedPet>, selectedIndex: Int): ApprovedPet? {
    if (pets.isEmpty()) return null
    return pets[if (selectedIndex in pets.indices) selectedIndex else 0]
}

private fun String.replacePrefix(prefix: String, replacement: String): String =
    replacement + removePrefix(prefix)

private fun String.replaceSuffix(suffix: String, replacement: String): String =
    removeSuffix(suffix) + replacement

private fun String.isSafeAssetDisplayTextForStrings(): Boolean {
    val trimmed = trim()
    if (trimmed.isBlank()) return true
    val lower = trimmed.lowercase()
    return !Regex("(^|\\s)[A-Za-z]:[\\\\/]").containsMatchIn(trimmed) &&
        !lower.startsWith("file:") &&
        !lower.startsWith("/") &&
        !lower.contains("\\") &&
        INTERNAL_ASSET_MARKERS_FOR_STRINGS.none { marker -> lower.contains(marker) }
}

private fun String.safeGenerationMessageDetail(fallback: String): String =
    trim().takeIf { it.isNotBlank() && it.isSafeGenerationMessageText() } ?: fallback

private fun String.isSafeGenerationMessageText(): Boolean =
    isSafeAssetDisplayTextForStrings()

private val INTERNAL_ASSET_MARKERS_FOR_STRINGS = listOf(
    "server_run.json",
    "artifact-index.json",
    "resolution-map",
    "desktop-pet-casebook-audit.json",
    "desktop-pet-stage-gate-report.json",
    "desktop-pet-learning-memory.json",
    "human-feedback-context.json",
    "genericagent-orchestrator-task.json",
    "codex-worker-task.json",
    "codex-worker-task.output.json",
    ".invocation.json",
    ".execution.json",
    ".output.json.adapterprovenance",
    "adapterprovenance",
    "directcodexcli",
    "strategy-plan.json",
    "codex-generation-directives.json",
    "server-proof-summary.json",
    "server-proof-summary",
    "realadapterlaunch",
    "humanacceptance",
    "server-generation-learning-drill.json",
    "server-generation-regression-report.json",
    "learning-ledger.jsonl",
    "route-policy-decision.json",
    "genericagent-ledger-suggestions.json",
    "genericagent-ledger-import.json",
    "stage-gate-ledger-import.json",
    "learning-drill",
    "learningprogress",
    "learningmemoryresponse",
    "codexgenerationdirectiveresponse",
    "codexgenerationdirectiveresponsesummary",
    "codexqaevidence",
    "directivehistoryresponse",
    "narrowedrepairfocus",
    "gadirectivehistoryresponse",
    "casebookreferencesused",
    "repairstrategiesused",
    "desktoppetlearningmemorysummary",
    "servergenerationlearningprogresssummary",
    "qualitygatestatus",
    "qualitygatetrend",
    "learningassessment",
    "nextrepairfocus",
    "memorycarryforward",
    "learningmemoryinput",
    "learningmemoryoutput",
    "repeatedneedsrevisionstages",
    "repeatedhardfailuresobserved",
    "missingneedsrevisioncoverage",
    "missinghardfailurecoverage",
    "repaircoverage",
    "repairstrategyusecounts",
    "codex-action-attempt-n-server-imagegen-001",
    "stagegatereport",
    "stagegaterepair",
    "stagegaterepairrequests",
    "stagegatestatus",
    "learningledgersuggestions",
    "agent-review.json",
    "orchestration-review.json",
    "runs/",
    "runs\\",
    "secret/"
)
