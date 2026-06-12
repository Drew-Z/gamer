package com.gamer.community.petshell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetShellControllerTest {
    @Test
    fun initialStateStartsOnDesktopPetMode() {
        val state = PetShellController.initialState()

        assertEquals(ShellPhase.DesktopPet, state.phase)
        assertEquals(PetAction.Idle, state.petAction)
        assertEquals("Desktop pet ready.", state.speechBubble)
        assertEquals(0, state.feedIndex)
        assertEquals(0, state.approvedPetIndex)
    }

    @Test
    fun desktopPetModeCanOpenCommunity() {
        val state = PetShellController.openCommunity(PetShellController.initialState())

        assertEquals(ShellPhase.Community, state.phase)
        assertEquals(PetAction.Idle, state.petAction)
        assertEquals("Welcome back, Demo Keeper.", state.speechBubble)
    }

    @Test
    fun desktopPetModeCanReturnToDesktopPet() {
        val desktopPet = PetShellController.initialState()
        val state = PetShellController.openCommunity(desktopPet)
        val returned = PetShellController.openDesktopPet(state)

        assertEquals(ShellPhase.DesktopPet, returned.phase)
        assertEquals(PetAction.Idle, returned.petAction)
        assertEquals("Desktop pet ready.", returned.speechBubble)
    }

    @Test
    fun feedNavigationUpdatesIndexAndPetAction() {
        val open = PetShellController.openCommunity(
            PetShellController.initialState().copy(posts = testFeedPosts)
        )
        val next = PetShellController.navigateFeed(open, FeedDirection.Next)
        val previous = PetShellController.navigateFeed(next, FeedDirection.Previous)
        val skipped = PetShellController.navigateFeed(previous, FeedDirection.Skip)

        assertEquals(1, next.feedIndex)
        assertEquals(PetAction.FeedNext, next.petAction)
        assertEquals(0, previous.feedIndex)
        assertEquals(PetAction.FeedPrevious, previous.petAction)
        assertEquals(1, skipped.feedIndex)
        assertEquals(PetAction.FeedSkip, skipped.petAction)
    }

    @Test
    fun feedNavigationDoesNotCreateFallbackPostsWhenFeedIsEmpty() {
        val open = PetShellController.openCommunity(PetShellController.initialState())

        val next = PetShellController.navigateFeed(open, FeedDirection.Next)

        assertEquals(0, next.feedIndex)
        assertTrue(next.posts.isEmpty())
        assertEquals(PetAction.Idle, next.petAction)
        assertEquals("Community feed is waiting for remote posts.", next.speechBubble)
    }

    @Test
    fun checkInClaimsRewardAndUpdatesWallet() {
        val open = PetShellController.openCommunity(PetShellController.initialState())
        assertFalse(open.checkInClaimed)

        val claimed = PetShellController.claimDailyReward(open)

        assertTrue(claimed.checkInClaimed)
        assertEquals(100, claimed.walletBalance)
        assertEquals(PetAction.Reward, claimed.petAction)
        assertEquals("Daily reward claimed: +10 petcoin.", claimed.speechBubble)
    }

    @Test
    fun applyingRemoteCommunityLoadReplacesPostsAndWallet() {
        val state = PetShellController.initialState()
        val remotePost = FeedPost(
            id = "post-live-001",
            petId = "pet-live-001",
            title = "Live feed",
            body = "Remote body",
            authorName = "Demo Keeper",
            reactionCount = 18
        )
        val approvedPet = ApprovedPet(
            petId = "pet-stardust-001",
            displayName = "Stardust Dragon",
            sourceKind = "fantasy-pet-rule",
            previewPath = "previews/overall-showcase.png",
            exportArtifactPath = "",
            motionSheetCount = 2,
            totalScore = 86
        )

        val loaded = PetShellController.applyCommunityLoad(
            state = state,
            posts = listOf(remotePost),
            approvedPets = listOf(approvedPet),
            walletBalance = 123,
            usedFallback = false,
            message = "Community ready."
        )

        assertEquals(0, loaded.feedIndex)
        assertEquals(123, loaded.walletBalance)
        assertTrue(loaded.remoteCommunitySynced)
        assertEquals(1, loaded.approvedPets.size)
        assertEquals("Stardust Dragon", loaded.approvedPets[0].displayName)
        assertEquals("Live feed", loaded.posts[loaded.feedIndex].title)
        assertEquals(PetAction.Idle, loaded.petAction)
        assertEquals("Community ready.", loaded.speechBubble)
    }

    @Test
    fun applyingUnavailableCommunityLoadDoesNotCreatePlaceholderPosts() {
        val state = PetShellController.initialState().copy(posts = testFeedPosts)

        val loaded = PetShellController.applyCommunityLoad(
            state = state,
            posts = emptyList(),
            approvedPets = emptyList(),
            walletBalance = 90,
            usedFallback = true,
            message = "Remote community unavailable."
        )

        assertTrue(loaded.posts.isEmpty())
        assertFalse(loaded.remoteCommunitySynced)
        assertEquals(PetAction.Idle, loaded.petAction)
        assertEquals("Remote community unavailable.", loaded.speechBubble)
    }

    @Test
    fun approvedPetNavigationUpdatesSelectedPetAndAction() {
        val state = PetShellController.initialState().copy(
            approvedPets = listOf(
                approvedPet("pet-stardust-001", "Stardust Dragon"),
                approvedPet("pet-moonfox-001", "Moon Fox")
            )
        )

        val next = PetShellController.navigateApprovedPet(state, FeedDirection.Next)
        val previous = PetShellController.navigateApprovedPet(state, FeedDirection.Previous)

        assertEquals(1, next.approvedPetIndex)
        assertEquals(PetAction.ShowcaseNext, next.petAction)
        assertEquals("Showing approved pet Moon Fox.", next.speechBubble)
        assertEquals(1, previous.approvedPetIndex)
        assertEquals(PetAction.ShowcasePrevious, previous.petAction)
        assertEquals("Showing approved pet Moon Fox.", previous.speechBubble)
    }

    @Test
    fun approvedPetNavigationHandlesEmptyRegistry() {
        val state = PetShellController.initialState()

        val next = PetShellController.navigateApprovedPet(state, FeedDirection.Next)

        assertEquals(0, next.approvedPetIndex)
        assertEquals(PetAction.Idle, next.petAction)
        assertEquals("No approved pets ready yet.", next.speechBubble)
    }

    @Test
    fun applyingRemoteCommunityLoadResetsApprovedPetSelection() {
        val state = PetShellController.initialState().copy(
            approvedPetIndex = 3,
            approvedPets = listOf(approvedPet("pet-old-001", "Old Pet"))
        )
        val remotePost = FeedPost(
            id = "post-live-001",
            petId = "pet-live-001",
            title = "Live feed",
            body = "Remote body",
            authorName = "Demo Keeper",
            reactionCount = 18
        )

        val loaded = PetShellController.applyCommunityLoad(
            state = state,
            posts = listOf(remotePost),
            approvedPets = listOf(approvedPet("pet-stardust-001", "Stardust Dragon")),
            walletBalance = 123,
            usedFallback = false,
            message = "Community ready."
        )

        assertEquals(0, loaded.approvedPetIndex)
    }

    @Test
    fun applyingRemoteCommunityLoadSetsHomeSummaryState() {
        val loaded = PetShellController.applyCommunityLoad(
            state = PetShellController.initialState(),
            posts = testFeedPosts,
            approvedPets = emptyList(),
            walletBalance = 144,
            checkInClaimed = true,
            pendingSubmissionCount = 2,
            usedFallback = false,
            message = "Community home ready."
        )

        assertEquals(144, loaded.walletBalance)
        assertTrue(loaded.checkInClaimed)
        assertEquals(2, loaded.pendingSubmissionCount)
        assertEquals("Community home ready.", loaded.speechBubble)
    }

    @Test
    fun applyingRemoteCheckInUsesRemoteWalletBalance() {
        val open = PetShellController.openCommunity(PetShellController.initialState())

        val checkedIn = PetShellController.applyCheckInResult(
            state = open,
            walletBalance = 133,
            claimed = true,
            rewardAmount = 10,
            usedFallback = false,
            message = "Daily reward claimed: +10 petcoin."
        )

        assertEquals(133, checkedIn.walletBalance)
        assertTrue(checkedIn.checkInClaimed)
        assertEquals(PetAction.Reward, checkedIn.petAction)
        assertEquals("Daily reward claimed: +10 petcoin.", checkedIn.speechBubble)
    }

    @Test
    fun applyingUnavailableCheckInDoesNotClaimLocalReward() {
        val open = PetShellController.openCommunity(PetShellController.initialState())

        val checkedIn = PetShellController.applyCheckInResult(
            state = open,
            walletBalance = null,
            claimed = false,
            rewardAmount = 0,
            usedFallback = true,
            message = "Remote check-in unavailable."
        )

        assertEquals(90, checkedIn.walletBalance)
        assertFalse(checkedIn.checkInClaimed)
        assertEquals(PetAction.Idle, checkedIn.petAction)
        assertEquals("Remote check-in unavailable.", checkedIn.speechBubble)
    }

    private fun approvedPet(
        petId: String,
        displayName: String
    ): ApprovedPet =
        ApprovedPet(
            petId = petId,
            displayName = displayName,
            sourceKind = "fantasy-pet-rule",
            previewPath = "previews/overall-showcase.png",
            exportArtifactPath = "",
            motionSheetCount = 2,
            totalScore = 86
        )

    private val testFeedPosts = listOf(
        FeedPost(
            id = "post-test-001",
            petId = "pet-test-001",
            title = "Test feed one",
            body = "Remote body one",
            authorName = "Demo Keeper",
            reactionCount = 3
        ),
        FeedPost(
            id = "post-test-002",
            petId = "pet-test-002",
            title = "Test feed two",
            body = "Remote body two",
            authorName = "Demo Keeper",
            reactionCount = 5
        )
    )
}
