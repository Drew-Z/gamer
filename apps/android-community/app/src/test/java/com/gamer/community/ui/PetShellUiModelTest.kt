package com.gamer.community.ui

import com.gamer.community.generation.DEFAULT_GENERATION_MESSAGE
import com.gamer.community.petshell.PetAction
import com.gamer.community.petshell.FeedPost
import com.gamer.community.petshell.ApprovedPet
import com.gamer.community.petshell.SubmissionSummary
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetShellUiModelTest {
    @Test
    fun petShellLanguageDefaultsToChineseAndParsesEnglish() {
        assertEquals(PetShellLanguage.Chinese, parsePetShellLanguage(null))
        assertEquals(PetShellLanguage.Chinese, parsePetShellLanguage(""))
        assertEquals(PetShellLanguage.Chinese, parsePetShellLanguage("zh"))
        assertEquals(PetShellLanguage.English, parsePetShellLanguage("en"))
        assertEquals(PetShellLanguage.Chinese, parsePetShellLanguage("ja"))
        assertEquals("zh", PetShellLanguage.Chinese.preferenceValue)
        assertEquals("en", PetShellLanguage.English.preferenceValue)
    }

    @Test
    fun petShellStringsExposeStableAppShellTabs() {
        val zh = defaultPetShellStrings()
        val en = petShellStrings(PetShellLanguage.English)

        assertEquals("\u793E\u533A", zh.communityTabLabel)
        assertEquals("孵化", zh.generateTabLabel)
        assertEquals("\u6211\u7684", zh.profileTabLabel)
        assertEquals("Community", en.communityTabLabel)
        assertEquals("Hatch", en.generateTabLabel)
        assertEquals("Mine", en.profileTabLabel)
        assertEquals("gamer-tab-community", zh.communityTabContentDescription)
        assertEquals("gamer-tab-generate", zh.generateTabContentDescription)
        assertEquals("gamer-tab-profile", zh.profileTabContentDescription)
        assertEquals("gamer-community-home", zh.communityHomeContentDescription)
        assertEquals("gamer-generation-workspace", zh.generationWorkspaceContentDescription)
        assertEquals("gamer-profile-workspace", zh.profileWorkspaceContentDescription)
        assertEquals("header-utility-dock", zh.headerUtilityDockContentDescription)
        assertEquals("gamer-pet-avatar", zh.petAvatarContentDescription)
        assertEquals("community-channel-rail", zh.communityChannelRailContentDescription)
        assertEquals("community-quick-actions", zh.communityQuickActionsContentDescription)
        assertEquals("latest-submission-action", zh.latestSubmissionActionContentDescription)
        assertEquals(
            "community-pet-companion-strip",
            zh.communityPetCompanionStripContentDescription
        )
        assertEquals("community-status-summary", zh.communityStatusSummaryContentDescription)
        assertEquals("community-showcase-panel", zh.communityShowcasePanelContentDescription)
        assertEquals("community-post-card", zh.communityPostCardContentDescription)
        assertEquals("community-feed-controls", zh.communityFeedControlsContentDescription)
        assertEquals("generation-studio-hero", zh.generationStudioHeroContentDescription)
        assertEquals("generation-studio-status-dock", zh.generationStudioStatusDockContentDescription)
        assertEquals("generation-prompt-canvas", zh.generationPromptCanvasContentDescription)
        assertEquals("generation-runtime-console", zh.generationRuntimeConsoleContentDescription)
        assertEquals("generation-review-action-dock", zh.generationReviewActionDockContentDescription)
        assertEquals("gamer-desktop-pet-mode", zh.desktopPetModeContentDescription)
        assertEquals("direct-pet-launch-toggle", zh.directPetLaunchToggleContentDescription)
        assertEquals("desktop-pet-overlay-toggle", zh.desktopPetOverlayToggleContentDescription)
        assertEquals(
            "desktop-pet-overlay-permission-button",
            zh.desktopPetOverlayPermissionContentDescription
        )
        assertEquals(
            "desktop-pet-notification-permission-button",
            zh.desktopPetNotificationPermissionContentDescription
        )
        assertEquals(
            "desktop-pet-overlay-reset-position-button",
            zh.desktopPetOverlayResetPositionContentDescription
        )
        assertEquals(
            "desktop-pet-overlay-active-preview",
            zh.desktopPetOverlayActivePreviewContentDescription
        )
        assertEquals("desktop-pet-overlay-start-button", zh.desktopPetOverlayStartContentDescription)
        assertEquals("desktop-pet-overlay-stop-button", zh.desktopPetOverlayStopContentDescription)
        assertEquals("desktop-pet-open-community", zh.desktopPetOpenCommunityContentDescription)
        assertEquals("desktop-pet-open-generate", zh.desktopPetOpenGenerateContentDescription)
        assertEquals("desktop-pet-open-profile", zh.desktopPetOpenProfileContentDescription)
        assertEquals("profile-keeper-hero", zh.profileKeeperHeroContentDescription)
        assertEquals("profile-wallet-summary", zh.profileWalletSummaryContentDescription)
        assertEquals("profile-pet-shelf", zh.profilePetShelfContentDescription)
        assertEquals("profile-action-dock", zh.profileActionDockContentDescription)
        assertEquals("generation-flow-rail", zh.generationFlowRailContentDescription)
        assertEquals("generation-brief-panel", zh.generationBriefPanelContentDescription)
        assertEquals("generation-review-desk", zh.generationReviewDeskContentDescription)
        assertEquals(
            "generation-review-waiting-candidate",
            zh.generationReviewWaitingContentDescription
        )
        assertEquals("community", zh.communityTabIconLabel)
        assertEquals("generate", zh.generateTabIconLabel)
        assertEquals("profile", zh.profileTabIconLabel)
    }

    @Test
    fun petShellTabHeaderCopyUsesSelectedProductSurface() {
        val zh = defaultPetShellStrings()
        val en = petShellStrings(PetShellLanguage.English)

        assertEquals(zh.communityTitle, petShellTabHeaderTitle(PetShellTab.Community, zh))
        assertEquals(zh.communitySubtitle, petShellTabHeaderSubtitle(PetShellTab.Community, zh))
        assertEquals(zh.generationWorkspaceTitle, petShellTabHeaderTitle(PetShellTab.Generate, zh))
        assertEquals(zh.generationWorkspaceSubtitle, petShellTabHeaderSubtitle(PetShellTab.Generate, zh))
        assertEquals(zh.profileWorkspaceTitle, petShellTabHeaderTitle(PetShellTab.Profile, zh))
        assertEquals(zh.profileWorkspaceSubtitle, petShellTabHeaderSubtitle(PetShellTab.Profile, zh))

        assertEquals("Pet Hatchery", petShellTabHeaderTitle(PetShellTab.Generate, en))
        assertEquals(
            "Public API loop from egg and prompt to review and shelf.",
            petShellTabHeaderSubtitle(PetShellTab.Generate, en)
        )
        assertEquals("My Pets", petShellTabHeaderTitle(PetShellTab.Profile, en))
        assertEquals(
            "Wallet, check-in, and approved desktop pets.",
            petShellTabHeaderSubtitle(PetShellTab.Profile, en)
        )
    }

    @Test
    fun petShellHeaderBackgroundUsesImmersiveTabSpecificPalette() {
        val community = petShellHeaderBackgroundSpec(PetShellTab.Community)
        val generate = petShellHeaderBackgroundSpec(PetShellTab.Generate)
        val profile = petShellHeaderBackgroundSpec(PetShellTab.Profile)

        assertEquals(Color.White, community.titleColor)
        assertEquals(Color(0xFFD7F3EE), community.subtitleColor)
        assertEquals(Color.White, generate.titleColor)
        assertEquals(Color.White, profile.titleColor)
        assertFalse(community.startColor == generate.startColor)
        assertFalse(generate.startColor == profile.startColor)
        assertFalse(profile.startColor == community.startColor)
        assertEquals(Color(0xFFF97316), community.accentColor)
        assertEquals(Color(0xFF60A5FA), generate.accentColor)
        assertEquals(Color(0xFFFFB86B), profile.accentColor)
    }

    @Test
    fun generationStudioHeroUsesSpecificWorkbenchCopy() {
        val zh = defaultPetShellStrings()
        val en = petShellStrings(PetShellLanguage.English)

        assertEquals("桌宠孵化室", zh.generationStudioHeroTitle)
        assertEquals(
            "从获得蛋、提示词孵化到人审入架，都在这里完成。",
            zh.generationStudioHeroSubtitle
        )
        assertEquals("Pet Hatchery", en.generationStudioHeroTitle)
        assertEquals(
            "Hatch an egg from prompt to human review and shelf delivery.",
            en.generationStudioHeroSubtitle
        )
        assertFalse(zh.generationStudioHeroTitle == zh.generationWorkspaceTitle)
        assertFalse(en.generationStudioHeroTitle == en.generationWorkspaceTitle)
    }

    @Test
    fun petShellStringsUseChineseByDefaultAndExposeEnglishAlternative() {
        val zh = defaultPetShellStrings()
        val en = petShellStrings(PetShellLanguage.English)

        assertEquals("玩家社区", zh.communityTitle)
        assertEquals("桌宠带路的每日社区", zh.communitySubtitle)
        assertEquals("孵化桌宠", zh.generatePanelTitle)
        assertEquals(
            "公共 API 只创建和轮询孵化任务；真实生成 worker 需要在服务端单独启动。",
            zh.generationPublicApiBoundaryNotice
        )
        assertEquals(
            "generation-public-api-boundary-notice",
            zh.generationPublicApiBoundaryContentDescription
        )
        assertEquals("开始自主孵化", zh.createGenerationJob)
        assertEquals("\uFF08\u5FC5\u586B\uFF09", zh.requiredFieldSuffix)
        assertEquals("孵化流程", zh.generationFlowRailTitle)
        assertEquals("自主孵化", zh.generationBriefPanelTitle)
        assertEquals("桌宠提示", zh.generationPromptStageTitle)
        assertEquals("随机设定", zh.generationPromptIdeaAction)
        assertEquals("预计耗时", zh.hatcherySlaTitle)
        assertEquals("备用 ≤ 2 分钟", zh.hatcheryReserveSla(120_000))
        assertEquals("神秘 ≤ 10 分钟", zh.hatcheryMysterySla(600_000))
        assertEquals("自主 ≤ 15 分钟", zh.hatcheryCustomSla(900_000))
        assertEquals(
            "建议每 3 秒轮询，连续 3 次失败后提示慢响应",
            zh.hatcheryPollingSla(3_000, 3)
        )
        assertEquals(
            "generation-prompt-idea-button",
            zh.generationPromptIdeaContentDescription
        )
        assertEquals(
            "\u8584\u8377\u8272\u5B88\u62A4\u8005\u684C\u5BA0\uFF0C\u5F85\u673A\u8F7B\u8F7B\u6F02\u6D6E\uFF0C\u8DD1\u52A8\u65F6\u5C3E\u5DF4\u5F39\u8DF3\u3002",
            zh.generationPromptIdeaText
        )
        assertEquals("孵化设置", zh.generationTaskStageTitle)
        assertEquals("体型预设", zh.generationBodyStageTitle)
        assertEquals("开始孵化", zh.generationRunStageTitle)
        assertEquals("人审与入架", zh.generationReviewDeskTitle)
        assertEquals(
            "动作候选会在孵化完成后出现在这里，选择 candidate 后才能提交人审。",
            zh.generationReviewWaitingForCandidate
        )
        assertEquals("动作检查", zh.candidateInspectionTitle)
        assertEquals("动作人审备注", zh.reviewNotesStageTitle)
        assertEquals("交付动作", zh.deliveryActionsTitle)
        assertEquals("已选中", zh.candidateSelectedStatus)
        assertEquals("待选择", zh.candidateAvailableStatus)
        assertEquals("动作候选", zh.candidateGalleryTitle)
        assertEquals("动作候选 2", zh.candidateTitle("Candidate 2", 1))
        assertEquals(
            "动作 idle-breathe / 检查身份、流畅度和触发语义",
            zh.candidateActionFocus("idle-breathe")
        )
        assertEquals(
            "审核完整动作：身份一致、帧间流畅、透明边缘干净",
            zh.candidateActionFocus("C:/secret/runs/job/idle.png")
        )
        assertEquals("接受", zh.reviewAccept)
        assertEquals("提交到社区审核", zh.submitToCommunityReview)
        assertEquals(
            "generation-submit-community-review-button",
            zh.submitCommunityReviewContentDescription
        )
        assertEquals(
            "generation-refresh-community-submission-button",
            zh.refreshCommunitySubmissionContentDescription
        )
        assertEquals(
            "generation-contract-demo-no-live-worker",
            zh.contractDemoNoLiveWorkerContentDescription
        )
        assertEquals(
            "generation-server-worker-wait-notice",
            zh.serverWorkerWaitNoticeContentDescription
        )

        assertEquals("Gamer Community", en.communityTitle)
        assertEquals("Daily community led by your desktop pet", en.communitySubtitle)
        assertEquals("Hatch Desktop Pet", en.generatePanelTitle)
        assertEquals(
            "Public API only creates and polls hatch jobs; live generation workers must be started on the server side.",
            en.generationPublicApiBoundaryNotice
        )
        assertEquals("Start custom hatch", en.createGenerationJob)
        assertEquals(" (required)", en.requiredFieldSuffix)
        assertEquals("Hatch flow", en.generationFlowRailTitle)
        assertEquals("Custom hatch", en.generationBriefPanelTitle)
        assertEquals("Pet prompt", en.generationPromptStageTitle)
        assertEquals("Random prompt", en.generationPromptIdeaAction)
        assertEquals("ETA", en.hatcherySlaTitle)
        assertEquals("Reserve <= 2 min", en.hatcheryReserveSla(120_000))
        assertEquals("Mystery <= 10 min", en.hatcheryMysterySla(600_000))
        assertEquals("Custom <= 15 min", en.hatcheryCustomSla(900_000))
        assertEquals(
            "Poll every 3s; show slow notice after 3 failures",
            en.hatcheryPollingSla(3_000, 3)
        )
        assertEquals(
            "generation-prompt-idea-button",
            en.generationPromptIdeaContentDescription
        )
        assertEquals(
            "Mint guardian pet, gentle idle bob, springy tail run.",
            en.generationPromptIdeaText
        )
        assertEquals("Hatch setup", en.generationTaskStageTitle)
        assertEquals("Body preset", en.generationBodyStageTitle)
        assertEquals("Start hatch", en.generationRunStageTitle)
        assertEquals("Review and shelf", en.generationReviewDeskTitle)
        assertEquals("Review target", en.deliveryReviewTargetStatus)
        assertEquals("Package", en.deliveryPackageStatus)
        assertEquals("Community", en.deliveryCommunityStatus)
        assertEquals(
            "Motion candidates appear here when hatching reaches review; select a candidate before review.",
            en.generationReviewWaitingForCandidate
        )
        assertEquals("Motion inspection", en.candidateInspectionTitle)
        assertEquals("Motion review notes", en.reviewNotesStageTitle)
        assertEquals("Delivery actions", en.deliveryActionsTitle)
        assertEquals("Selected", en.candidateSelectedStatus)
        assertEquals("Available", en.candidateAvailableStatus)
        assertEquals("Motion candidates", en.candidateGalleryTitle)
        assertEquals("Motion candidate 2", en.candidateTitle("Candidate 2", 1))
        assertEquals(
            "Action idle-breathe / check identity, flow, and trigger semantics",
            en.candidateActionFocus("idle-breathe")
        )
        assertEquals(
            "Review full motion: identity, frame flow, clean alpha edges",
            en.candidateActionFocus("C:/secret/runs/job/idle.png")
        )
        assertEquals("Accept", en.reviewAccept)
        assertEquals("Submit to community review", en.submitToCommunityReview)
    }

    @Test
    fun petShellStringsExposeCommunityFeedPerspectiveCopy() {
        val zh = petShellStrings(PetShellLanguage.Chinese)
        val en = petShellStrings(PetShellLanguage.English)
        val latestPending = SubmissionSummary(
            id = "submission-local-001",
            petId = "public-lifecycle-smoke",
            status = "pending"
        )
        val latestApproved = latestPending.copy(status = "approved")
        val latestHeld = latestPending.copy(status = "held")
        val latestRejected = latestPending.copy(status = "rejected")

        assertEquals("\u684C\u5BA0\u5BFC\u822A\u53F0", zh.communityPetCommandTitle)
        assertEquals(
            "今日社区由桌宠带路：签到、孵化、审核和展示都从这里出发。",
            zh.communityPetCommandDetail
        )
        assertEquals("\u770B\u4E0B\u4E00\u6761", zh.communityPetCommandNextPost)
        assertEquals("\u53BB\u5C55\u793A", zh.communityPetCommandShowcase)
        assertEquals(
            "1 个已通过 / 2 个待审 / 可签到",
            zh.communityCommandStatus(
                approvedPetCount = 1,
                pendingSubmissionCount = 2,
                latestSubmission = null,
                checkInClaimed = false
            )
        )
        assertEquals(
            "1 个已通过 / 2 个待审 / 最新待审 public-lifecycle-smoke / 可签到",
            zh.communityCommandStatus(
                approvedPetCount = 1,
                pendingSubmissionCount = 2,
                latestSubmission = latestPending,
                checkInClaimed = false
            )
        )
        assertEquals("Pet navigator", en.communityPetCommandTitle)
        assertEquals(
            "Your desktop pet leads today's check-in, hatching, review, and showcase loop.",
            en.communityPetCommandDetail
        )
        assertEquals("Next post", en.communityPetCommandNextPost)
        assertEquals("Showcase", en.communityPetCommandShowcase)
        assertEquals(
            "1 approved / 2 pending / checked in",
            en.communityCommandStatus(
                approvedPetCount = 1,
                pendingSubmissionCount = 2,
                latestSubmission = null,
                checkInClaimed = true
            )
        )
        assertEquals(
            "1 approved / 0 pending / latest approved public-lifecycle-smoke / checked in",
            en.communityCommandStatus(
                approvedPetCount = 1,
                pendingSubmissionCount = 0,
                latestSubmission = latestApproved,
                checkInClaimed = true
            )
        )
        assertEquals("最新待审 public-lifecycle-smoke", zh.latestSubmissionStatus(latestPending))
        assertEquals("latest approved public-lifecycle-smoke", en.latestSubmissionStatus(latestApproved))
        assertEquals("已通过，去我的宠物架查看 public-lifecycle-smoke", zh.latestSubmissionAction(latestApproved))
        assertEquals("Approved. Open My Pets for public-lifecycle-smoke.", en.latestSubmissionAction(latestApproved))
        assertEquals("审核暂缓，补充说明后重新提交 public-lifecycle-smoke", zh.latestSubmissionAction(latestHeld))
        assertEquals("Held. Add notes and resubmit public-lifecycle-smoke.", en.latestSubmissionAction(latestHeld))
        assertEquals("未通过，回到孵化室修订 public-lifecycle-smoke", zh.latestSubmissionAction(latestRejected))
        assertEquals("Rejected. Revise in Hatchery for public-lifecycle-smoke.", en.latestSubmissionAction(latestRejected))
        assertEquals("", zh.latestSubmissionAction(null))
        assertEquals("\u684C\u5BA0\u89C6\u89D2", zh.communityFeedSignalTitle)
        assertEquals("\u4E92\u52A8", zh.feedReactionLabel)
        assertEquals("孵化", zh.showcasePathGenerate)
        assertEquals("\u4EBA\u5BA1", zh.showcasePathReview)
        assertEquals("\u5C55\u793A", zh.showcasePathPublish)
        assertTrue(zh.communityFeedSignalDetail.contains("\u4E0A\u4E00\u9875"))
        assertTrue(zh.communityFeedSignalDetail.contains("\u4E0B\u4E0B\u9875"))
        assertEquals("Pet view", en.communityFeedSignalTitle)
        assertEquals("Reactions", en.feedReactionLabel)
        assertEquals("Hatch", en.showcasePathGenerate)
        assertEquals("Review", en.showcasePathReview)
        assertEquals("Showcase", en.showcasePathPublish)
        assertTrue(en.communityFeedSignalDetail.contains("previous"))
        assertTrue(en.communityFeedSignalDetail.contains("skip ahead"))
    }

    @Test
    fun petShellStringsLocalizeBodyShapesAndReviewSuggestions() {
        val zh = petShellStrings(PetShellLanguage.Chinese)
        val en = petShellStrings(PetShellLanguage.English)

        assertEquals("均衡", zh.bodyShapeLabel("balanced"))
        assertEquals("宽体长尾", zh.bodyShapeLabel("wide-tail"))
        assertEquals("wide-tail", en.bodyShapeLabel("wide-tail"))
        assertEquals("idle 动作上下跳动明显", zh.reviewNoteSuggestion("idle action jumps vertically"))
        assertEquals("idle action jumps vertically", en.reviewNoteSuggestion("idle action jumps vertically"))
    }

    @Test
    fun petShellStringsLocalizeCommonStateMessagesButKeepUnknownSafeText() {
        val zh = petShellStrings(PetShellLanguage.Chinese)
        val en = petShellStrings(PetShellLanguage.English)

        assertEquals("正在加载社区...", zh.speechBubble("Loading community..."))
        assertEquals("欢迎回来，Demo Keeper。", zh.speechBubble("Welcome back, Demo Keeper."))
        assertEquals("描述一个桌宠，开始孵化。", zh.generationMessage(DEFAULT_GENERATION_MESSAGE))
        assertEquals("正在创建孵化任务...", zh.generationMessage("Creating generation job..."))
        assertEquals(
            "这是公共 API 契约演示任务：动作候选资源是服务端预置的验证资源，不代表真实桌宠生成链路已运行。",
            zh.generationMessage(
                "Contract demo task: this candidate is pre-seeded for public API validation; it is not a live pet generation run."
            )
        )
        assertEquals(
            "契约演示资源已载入；真实生成 worker 尚未运行。",
            zh.generationMessage("Contract demo fixture loaded; no live generation worker has run.")
        )
        assertEquals(
            "正在等待可信服务端 worker；app 只负责创建和轮询任务。",
            zh.generationMessage(
                "Waiting for a trusted server worker; this app only created and polls the job."
            )
        )
        assertEquals(
            "反馈已记录；需要可信服务端 worker 发布新的动作候选。",
            zh.generationMessage(
                "Feedback recorded; a trusted server worker must publish the next candidate."
            )
        )
        assertEquals(
            "审核失败: 这是公共 API 契约演示任务，不能作为真实孵化任务提交人审。",
            zh.generationMessage("Review failed: contract_demo_job_review_disabled")
        )
        assertEquals("生成轮询失败: offline", zh.generationMessage("Generation poll failed: offline"))
        assertEquals(
            "2 个动作候选等待人工审核。",
            zh.generationMessage("2 candidates ready for human review.")
        )
        assertEquals("安全的自定义消息", zh.generationMessage("安全的自定义消息"))

        assertEquals("Loading community...", en.speechBubble("Loading community..."))
        assertEquals(DEFAULT_GENERATION_MESSAGE, en.generationMessage(DEFAULT_GENERATION_MESSAGE))
        assertEquals(
            "Contract demo fixture loaded; no live generation worker has run.",
            en.generationMessage("Contract demo fixture loaded; no live generation worker has run.")
        )
        assertEquals(
            "Waiting for a trusted server worker; this app only created and polls the job.",
            en.generationMessage(
                "Waiting for a trusted server worker; this app only created and polls the job."
            )
        )
        assertEquals("Generation poll failed: offline", en.generationMessage("Generation poll failed: offline"))
    }

    @Test
    fun petShellStringsLocalizeReviewFailureReasonsWithoutRawKeys() {
        val zh = petShellStrings(PetShellLanguage.Chinese)
        val en = petShellStrings(PetShellLanguage.English)

        assertEquals(
            "人审提交失败: 这个动作候选已经审核过，请等待新的动作候选。",
            zh.generationMessage("Review failed: review_target_already_decided")
        )
        assertEquals(
            "人审提交失败: 请选择未审核的动作候选。",
            zh.generationMessage("Review failed: review_target_must_be_candidate")
        )
        assertEquals(
            "人审提交失败: 请先选择动作候选。",
            zh.generationMessage("Review failed: target_download_id_required")
        )

        assertEquals(
            "Review failed: this motion candidate has already been reviewed; wait for a new one.",
            en.generationMessage("Review failed: review_target_already_decided")
        )
        assertEquals(
            "Review failed: select an unreviewed motion candidate.",
            en.generationMessage("Review failed: review_target_must_be_candidate")
        )
        assertEquals(
            "Review failed: select a motion candidate first.",
            en.generationMessage("Review failed: target_download_id_required")
        )
    }

    @Test
    fun petShellStringsHideUnsafeDynamicGenerationMessageDetails() {
        val zh = petShellStrings(PetShellLanguage.Chinese)
        val en = petShellStrings(PetShellLanguage.English)
        val unsafeFailure =
            "Generation poll failed: D:/workspace4Codex/fantasy-pet-rule/runs/job/codex-worker-task.json"
        val proofSummaryFailure = "Generation poll failed: server-proof-summary.json"
        val adapterProvenanceFailure = "Review failed: adapterProvenance"

        assertEquals("Generation poll failed: unavailable", en.generationMessage(unsafeFailure))
        assertEquals("Generation poll failed: unavailable", en.generationMessage(proofSummaryFailure))
        assertEquals("Review failed: unavailable", en.generationMessage(adapterProvenanceFailure))
        assertEquals("Message unavailable.", en.generationMessage("D:\\secret\\runs\\output.json"))
        assertEquals("Message unavailable.", en.generationMessage("genericagent-ledger-import.json"))

        val zhMessage = zh.generationMessage(unsafeFailure)
        assertTrue(zhMessage.isNotBlank())
        assertFalse(zhMessage.contains("D:/"))
        assertFalse(zhMessage.contains("runs/"))
        assertFalse(zhMessage.contains("codex-worker-task"))

        val zhProofMessage = zh.generationMessage(proofSummaryFailure)
        assertFalse(zhProofMessage.contains("server-proof-summary"))
        assertFalse(zh.generationMessage(adapterProvenanceFailure).contains("adapterProvenance"))
    }

    @Test
    fun petShellStringsLocalizeApprovedPetShowcaseSummaries() {
        val zh = petShellStrings(PetShellLanguage.Chinese)
        val en = petShellStrings(PetShellLanguage.English)
        val pet = approvedPet(
            petId = "pet-moonfox-001",
            displayName = "Moon Fox",
            previewPath = "previews/moonfox.png",
            exportArtifactPath = "exports/moonfox.zip",
            totalScore = 91,
            motionSheetCount = 3
        )

        assertEquals("暂时还没有已通过的桌宠", zh.approvedPetRegistrySummary(emptyList()))
        assertEquals("1 个已通过桌宠", zh.approvedPetRegistrySummary(listOf(pet)))
        assertEquals("Moon Fox", zh.approvedPetShowcaseTitle(listOf(pet), selectedIndex = 0))
        assertEquals(
            "fantasy-pet-rule / 评分 91 / 3 张动作表",
            zh.approvedPetShowcaseDetail(listOf(pet), selectedIndex = 0)
        )
        assertEquals("桌宠 1 / 1", zh.approvedPetShowcasePosition(listOf(pet), selectedIndex = 0))
        assertEquals("预览 previews/moonfox.png", zh.approvedPetShowcaseAsset(listOf(pet), selectedIndex = 0))
        assertEquals("资源包 exports/moonfox.zip", zh.approvedPetShowcasePackage(listOf(pet), selectedIndex = 0))
        assertEquals("评分", zh.approvedPetScoreMetric)
        assertEquals("动作表", zh.approvedPetMotionMetric)
        assertEquals("远端可看", zh.approvedPetPreviewReady)

        assertEquals("1 approved pet", en.approvedPetRegistrySummary(listOf(pet)))
        assertEquals("Pet 1 of 1", en.approvedPetShowcasePosition(listOf(pet), selectedIndex = 0))
        assertEquals("Score", en.approvedPetScoreMetric)
        assertEquals("Remote ready", en.approvedPetPreviewReady)
        val tracedPet = approvedPet(
            petId = "issue-1-fresh-timeout3600-20260610-1",
            displayName = "Generated pet",
            sourceAppJobId = "issue-1-fresh-timeout3600-20260610-1",
            targetDownloadId = "artifact-34"
        )
        assertEquals(
            "来源 fantasy-pet-rule / 任务 issue-1-fresh-timeout3600-20260610-1 / 预览 artifact-34",
            zh.approvedPetSourceLine(tracedPet)
        )
        assertEquals(
            "Source fantasy-pet-rule / job issue-1-fresh-timeout3600-20260610-1 / preview artifact-34",
            en.approvedPetSourceLine(tracedPet)
        )
        assertEquals(
            "已入架 Moon Fox / 远端预览可用",
            zh.latestApprovedShelfLine(
                latestSubmission = SubmissionSummary(
                    id = "submission-local-001",
                    petId = "pet-moonfox-001",
                    status = "approved"
                ),
                approvedPets = listOf(pet)
            )
        )
        assertEquals(
            "Shelved Moon Fox / remote preview ready",
            en.latestApprovedShelfLine(
                latestSubmission = SubmissionSummary(
                    id = "submission-local-001",
                    petId = "pet-moonfox-001",
                    status = "approved"
                ),
                approvedPets = listOf(pet)
            )
        )
        assertEquals(
            "",
            zh.latestApprovedShelfLine(
                latestSubmission = SubmissionSummary(
                    id = "submission-local-001",
                    petId = "pet-moonfox-001",
                    status = "pending"
                ),
                approvedPets = listOf(pet)
            )
        )
        assertEquals(
            "",
            zh.latestApprovedShelfLine(
                latestSubmission = SubmissionSummary(
                    id = "submission-local-001",
                    petId = "pet-missing-001",
                    status = "approved"
                ),
                approvedPets = listOf(pet)
            )
        )
    }

    @Test
    fun feedPostMetadataLabelsReturnsImportAndRewardLabels() {
        val post = FeedPost(
            id = "post-import-001",
            petId = "pet-import-001",
            title = "Approved pet import: pet-import-001",
            body = "preview accepted by user",
            authorName = "Demo Keeper",
            reactionCount = 0,
            sourceLabel = "Approved import",
            rewardLabel = "+80 petcoin"
        )

        assertEquals(listOf("Approved import", "+80 petcoin"), feedPostMetadataLabels(post))
    }

    @Test
    fun feedPostMetadataLabelsOmitsMissingLabels() {
        val post = FeedPost(
            id = "post-demo-001",
            petId = "pet-stardust-001",
            title = "Stardust dragon launch pose",
            body = "Fixture post.",
            authorName = "Demo Keeper",
            reactionCount = 12
        )

        assertEquals(emptyList<String>(), feedPostMetadataLabels(post))
    }

    @Test
    fun feedPostAuditLabelsReturnsImportReferences() {
        val post = FeedPost(
            id = "post-import-001",
            petId = "pet-import-001",
            title = "Approved pet import: pet-import-001",
            body = "preview accepted by user",
            authorName = "Demo Keeper",
            reactionCount = 0,
            importDraftLabel = "Draft import-draft-local-001",
            submissionLabel = "Submission submission-local-002",
            scoreReportLabel = "Score score-import-draft-local-001",
            importPreviewLabel = "Preview previews/overall-showcase.png",
            exportArtifactLabel = "Package exports/stardust-package.zip"
        )

        assertEquals(
            listOf(
                "Draft import-draft-local-001",
                "Submission submission-local-002",
                "Score score-import-draft-local-001",
                "Preview previews/overall-showcase.png",
                "Package exports/stardust-package.zip"
            ),
            feedPostAuditLabels(post)
        )
    }

    @Test
    fun feedPostAuditLabelsHideInternalAssetPathReferences() {
        val post = FeedPost(
            id = "post-import-001",
            petId = "pet-import-001",
            title = "Approved pet import: pet-import-001",
            body = "preview accepted by user",
            authorName = "Demo Keeper",
            reactionCount = 0,
            importDraftLabel = "Draft import-draft-local-001",
            importPreviewLabel = "Preview D:/workspace4Codex/fantasy-pet-rule/runs/feed/preview.html",
            exportArtifactLabel = "Package C:\\secret\\runs\\feed\\export.zip"
        )

        assertEquals(
            listOf("Draft import-draft-local-001"),
            feedPostAuditLabels(post)
        )
    }

    @Test
    fun feedPostAuditLabelsHideInternalLearningDrillReferences() {
        val post = FeedPost(
            id = "post-import-001",
            petId = "pet-import-001",
            title = "Approved pet import: pet-import-001",
            body = "preview accepted by user",
            authorName = "Demo Keeper",
            reactionCount = 0,
            importDraftLabel = "Draft import-draft-local-001",
            importPreviewLabel = "Preview server-generation-learning-drill.json",
            exportArtifactLabel = "Package server-generation-regression-report.json"
        )

        assertEquals(
            listOf("Draft import-draft-local-001"),
            feedPostAuditLabels(post)
        )
    }

    @Test
    fun feedPostAuditLabelsHideInternalLedgerReferences() {
        val post = FeedPost(
            id = "post-import-001",
            petId = "pet-import-001",
            title = "Approved pet import: pet-import-001",
            body = "preview accepted by user",
            authorName = "Demo Keeper",
            reactionCount = 0,
            importDraftLabel = "Draft import-draft-local-001",
            scoreReportLabel = "Score learning-ledger.jsonl",
            importSourceLabel = "Source route-policy-decision.json",
            importPreviewLabel = "Preview ledger-suggestions/genericagent-ledger-suggestions.json",
            exportArtifactLabel = "Package review/stage-gate-ledger-import.json"
        )

        assertEquals(
            listOf("Draft import-draft-local-001"),
            feedPostAuditLabels(post)
        )
    }

    @Test
    fun feedPostAuditLabelsOmitsMissingReferences() {
        val post = FeedPost(
            id = "post-demo-001",
            petId = "pet-stardust-001",
            title = "Stardust dragon launch pose",
            body = "Fixture post.",
            authorName = "Demo Keeper",
            reactionCount = 12
        )

        assertEquals(emptyList<String>(), feedPostAuditLabels(post))
    }

    @Test
    fun approvedPetRegistrySummaryShowsEmptyState() {
        assertEquals("No approved pets yet", approvedPetRegistrySummary(emptyList()))
    }

    @Test
    fun approvedPetRegistrySummaryShowsCount() {
        val pet = ApprovedPet(
            petId = "pet-stardust-001",
            displayName = "Stardust Dragon",
            sourceKind = "fantasy-pet-rule",
            previewPath = "previews/overall-showcase.png",
            exportArtifactPath = "",
            motionSheetCount = 2,
            totalScore = 86
        )

        assertEquals("1 approved pet", approvedPetRegistrySummary(listOf(pet)))
        assertEquals("2 approved pets", approvedPetRegistrySummary(listOf(pet, pet)))
    }

    @Test
    fun approvedPetShowcaseTitleUsesSelectedApprovedPet() {
        val pets = listOf(
            approvedPet("pet-stardust-001", "Stardust Dragon"),
            approvedPet("pet-moonfox-001", "Moon Fox")
        )

        assertEquals("Moon Fox", approvedPetShowcaseTitle(pets, selectedIndex = 1))
        assertEquals("Stardust Dragon", approvedPetShowcaseTitle(pets, selectedIndex = 99))
        assertEquals("Awaiting approved pet", approvedPetShowcaseTitle(emptyList(), selectedIndex = 1))
    }

    @Test
    fun approvedPetShowcaseDetailSummarizesSelectedPetScoreAndMotionSheets() {
        val pets = listOf(
            approvedPet(
                petId = "pet-stardust-001",
                displayName = "Stardust Dragon",
                totalScore = 86,
                motionSheetCount = 2
            ),
            approvedPet(
                petId = "pet-moonfox-001",
                displayName = "Moon Fox",
                totalScore = 91,
                motionSheetCount = 3
            )
        )

        assertEquals(
            "fantasy-pet-rule / score 91 / 3 motion sheets",
            approvedPetShowcaseDetail(pets, selectedIndex = 1)
        )
        assertEquals(
            "Approved imports will appear here.",
            approvedPetShowcaseDetail(emptyList(), selectedIndex = 1)
        )
    }

    @Test
    fun approvedPetShowcasePositionShowsSelectedPetPosition() {
        val pets = listOf(
            approvedPet("pet-stardust-001", "Stardust Dragon"),
            approvedPet("pet-moonfox-001", "Moon Fox"),
            approvedPet("pet-sunbird-001", "Sun Bird")
        )

        assertEquals("Pet 2 of 3", approvedPetShowcasePosition(pets, selectedIndex = 1))
        assertEquals("Pet 1 of 3", approvedPetShowcasePosition(pets, selectedIndex = 99))
        assertEquals("No showcase selection", approvedPetShowcasePosition(emptyList(), selectedIndex = 1))
    }

    @Test
    fun approvedPetShowcaseAssetShowsSelectedPreviewPath() {
        val pets = listOf(
            approvedPet("pet-stardust-001", "Stardust Dragon"),
            approvedPet(
                petId = "pet-moonfox-001",
                displayName = "Moon Fox",
                previewPath = "previews/moonfox.png"
            )
        )

        assertEquals("Preview previews/moonfox.png", approvedPetShowcaseAsset(pets, selectedIndex = 1))
        assertEquals("Preview asset pending", approvedPetShowcaseAsset(emptyList(), selectedIndex = 1))
    }

    @Test
    fun approvedPetShowcaseAssetHidesInternalPreviewPaths() {
        val pets = listOf(
            approvedPet(
                petId = "pet-moonfox-001",
                displayName = "Moon Fox",
                previewPath = "D:/workspace4Codex/fantasy-pet-rule/runs/export-registry/preview.html"
            )
        )

        assertEquals("Preview asset pending", approvedPetShowcaseAsset(pets, selectedIndex = 0))
    }

    @Test
    fun approvedPetShowcasePackageShowsSelectedExportArtifactPath() {
        val pets = listOf(
            approvedPet("pet-stardust-001", "Stardust Dragon"),
            approvedPet(
                petId = "pet-moonfox-001",
                displayName = "Moon Fox",
                exportArtifactPath = "exports/moonfox.zip"
            )
        )

        assertEquals("Package exports/moonfox.zip", approvedPetShowcasePackage(pets, selectedIndex = 1))
        assertEquals("Package artifact pending", approvedPetShowcasePackage(emptyList(), selectedIndex = 1))
    }

    @Test
    fun approvedPetShowcasePackageHidesInternalExportPaths() {
        val pets = listOf(
            approvedPet(
                petId = "pet-moonfox-001",
                displayName = "Moon Fox",
                exportArtifactPath = "D:\\workspace4Codex\\fantasy-pet-rule\\runs\\export-registry\\export.zip"
            )
        )

        assertEquals("Package artifact pending", approvedPetShowcasePackage(pets, selectedIndex = 0))
    }

    @Test
    fun approvedPetPreviewUrlUsesPublicArtifactRoute() {
        val pet = approvedPet(
            petId = "issue-1-fresh-timeout3600-20260610-1",
            displayName = "Generated pet",
            sourceAppJobId = "issue-1-fresh-timeout3600-20260610-1",
            targetDownloadId = "artifact-34"
        )

        assertEquals(
            "http://olivia.hidencloud.com:24674/pet-generation-jobs/issue-1-fresh-timeout3600-20260610-1/artifacts/artifact-34",
            approvedPetPreviewUrl(pet, baseUrl = "http://olivia.hidencloud.com:24674")
        )
    }

    @Test
    fun approvedPetPreviewUrlPrefersExplicitApiPreviewUrl() {
        val pet = approvedPet(
            petId = "legacy-pet-id",
            displayName = "Generated pet",
            sourceAppJobId = "legacy-app-job",
            targetDownloadId = "legacy-artifact",
            previewPath = "legacy-artifact",
            previewUrl = "/pet-generation-jobs/explicit-job/artifacts/artifact-34"
        )

        assertEquals(
            "http://olivia.hidencloud.com:24674/pet-generation-jobs/explicit-job/artifacts/artifact-34",
            approvedPetPreviewUrl(pet, baseUrl = "http://olivia.hidencloud.com:24674")
        )
    }

    @Test
    fun approvedPetPreviewUrlRejectsUnsafeExplicitPreviewUrl() {
        val pet = approvedPet(
            petId = "issue-1-fresh-timeout3600-20260610-1",
            displayName = "Generated pet",
            previewPath = "D:/workspace4Codex/fantasy-pet-rule/runs/job/output.png",
            previewUrl = "file:///D:/workspace4Codex/fantasy-pet-rule/runs/job/output.png"
        )

        assertEquals("", approvedPetPreviewUrl(pet, baseUrl = "http://olivia.hidencloud.com:24674"))
    }

    @Test
    fun approvedPetPreviewUrlRejectsNonPublicPreviewPath() {
        val pet = approvedPet(
            petId = "issue-1-fresh-timeout3600-20260610-1",
            displayName = "Generated pet",
            previewPath = "D:/workspace4Codex/fantasy-pet-rule/runs/job/output.png"
        )

        assertEquals("", approvedPetPreviewUrl(pet, baseUrl = "http://olivia.hidencloud.com:24674"))
    }

    @Test
    fun desktopPetOverlayDisplayNameNormalizesSafeNamesForNotification() {
        val pet = approvedPet(
            petId = "pet-moonfox-001",
            displayName = "  Moon   Fox\nGuardian  "
        )

        assertEquals("Moon Fox Guardian", desktopPetOverlayDisplayName(pet))
    }

    @Test
    fun desktopPetOverlayDisplayNameRejectsUnsafeNotificationNames() {
        val unsafeNames = listOf(
            "http://olivia.hidencloud.com:24674/pet-generation-jobs/job/artifacts/artifact-34",
            "/pet-generation-jobs/job/artifacts/artifact-34",
            "artifact-34",
            "D:/workspace4Codex/fantasy-pet-rule/runs/job/output.png",
            "server-proof-summary.json"
        )

        unsafeNames.forEach { unsafeName ->
            assertEquals(
                "",
                desktopPetOverlayDisplayName(
                    approvedPet(
                        petId = "pet-unsafe-name",
                        displayName = unsafeName
                    )
                )
            )
        }
    }

    private fun approvedPet(
        petId: String,
        displayName: String,
        totalScore: Int = 86,
        motionSheetCount: Int = 2,
        sourceAppJobId: String = "",
        previewPath: String = "previews/overall-showcase.png",
        targetDownloadId: String = "",
        previewUrl: String = "",
        exportArtifactPath: String = ""
    ): ApprovedPet =
        ApprovedPet(
            petId = petId,
            displayName = displayName,
            sourceKind = "fantasy-pet-rule",
            sourceAppJobId = sourceAppJobId,
            previewPath = previewPath,
            targetDownloadId = targetDownloadId,
            previewUrl = previewUrl,
            exportArtifactPath = exportArtifactPath,
            motionSheetCount = motionSheetCount,
            totalScore = totalScore
        )

}
