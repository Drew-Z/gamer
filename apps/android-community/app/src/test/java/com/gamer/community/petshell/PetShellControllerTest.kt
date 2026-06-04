package com.gamer.community.petshell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetShellControllerTest {
    @Test
    fun initialStateStartsOnLaunchBubble() {
        val state = PetShellController.initialState()

        assertEquals(ShellPhase.LaunchBubble, state.phase)
        assertEquals(PetAction.AppLoading, state.petAction)
        assertEquals("Loading community...", state.speechBubble)
        assertEquals(0, state.feedIndex)
    }

    @Test
    fun tappingBubbleOpensCommunity() {
        val state = PetShellController.onBubbleTapped(PetShellController.initialState())

        assertEquals(ShellPhase.Community, state.phase)
        assertEquals(PetAction.BubbleOpen, state.petAction)
        assertEquals("Welcome back, Demo Keeper.", state.speechBubble)
    }

    @Test
    fun feedNavigationUpdatesIndexAndPetAction() {
        val open = PetShellController.onBubbleTapped(PetShellController.initialState())
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
    fun checkInClaimsRewardAndUpdatesWallet() {
        val open = PetShellController.onBubbleTapped(PetShellController.initialState())
        assertFalse(open.checkInClaimed)

        val claimed = PetShellController.claimDailyReward(open)

        assertTrue(claimed.checkInClaimed)
        assertEquals(100, claimed.walletBalance)
        assertEquals(PetAction.Reward, claimed.petAction)
        assertEquals("Daily reward claimed: +10 petcoin.", claimed.speechBubble)
    }
}
