package com.gamer.community.petshell

object PetShellController {
    fun initialState(): PetShellState =
        PetShellState(
            phase = ShellPhase.LaunchBubble,
            petAction = PetAction.AppLoading,
            speechBubble = "Loading community...",
            feedIndex = 0,
            walletBalance = 90,
            checkInClaimed = false,
            approvedPets = emptyList(),
            approvedPetIndex = 0,
            posts = fixtureFeedPosts
        )

    fun onBubbleTapped(state: PetShellState): PetShellState =
        state.copy(
            phase = ShellPhase.Community,
            petAction = PetAction.BubbleOpen,
            speechBubble = "Welcome back, Demo Keeper."
        )

    fun navigateFeed(state: PetShellState, direction: FeedDirection): PetShellState {
        val skipStep = if (state.posts.size > 2) 2 else 1
        val nextIndex = when (direction) {
            FeedDirection.Next -> state.feedIndex + 1
            FeedDirection.Previous -> state.feedIndex - 1
            FeedDirection.Skip -> state.feedIndex + skipStep
        }.floorMod(state.posts.size)

        val nextAction = when (direction) {
            FeedDirection.Next -> PetAction.FeedNext
            FeedDirection.Previous -> PetAction.FeedPrevious
            FeedDirection.Skip -> PetAction.FeedSkip
        }

        val nextPost = state.posts[nextIndex]
        return state.copy(
            feedIndex = nextIndex,
            petAction = nextAction,
            speechBubble = "Showing ${nextPost.title}."
        )
    }

    fun claimDailyReward(state: PetShellState): PetShellState {
        if (state.checkInClaimed) {
            return state.copy(
                petAction = PetAction.Idle,
                speechBubble = "Daily reward already claimed."
            )
        }

        return state.copy(
            walletBalance = state.walletBalance + 10,
            checkInClaimed = true,
            petAction = PetAction.Reward,
            speechBubble = "Daily reward claimed: +10 petcoin."
        )
    }

    fun navigateApprovedPet(state: PetShellState, direction: FeedDirection): PetShellState {
        if (state.approvedPets.isEmpty()) {
            return state.copy(
                approvedPetIndex = 0,
                petAction = PetAction.Idle,
                speechBubble = "No approved pets ready yet."
            )
        }

        val nextIndex = when (direction) {
            FeedDirection.Next,
            FeedDirection.Skip -> state.approvedPetIndex + 1
            FeedDirection.Previous -> state.approvedPetIndex - 1
        }.floorMod(state.approvedPets.size)
        val nextPet = state.approvedPets[nextIndex]

        return state.copy(
            approvedPetIndex = nextIndex,
            petAction = when (direction) {
                FeedDirection.Previous -> PetAction.ShowcasePrevious
                FeedDirection.Next,
                FeedDirection.Skip -> PetAction.ShowcaseNext
            },
            speechBubble = "Showing approved pet ${nextPet.displayName}."
        )
    }

    fun applyCommunityLoad(
        state: PetShellState,
        posts: List<FeedPost>,
        approvedPets: List<ApprovedPet>,
        walletBalance: Int,
        usedFallback: Boolean,
        message: String
    ): PetShellState {
        val nextPosts = posts.ifEmpty { state.posts }
        return state.copy(
            petAction = if (usedFallback) state.petAction else PetAction.Idle,
            speechBubble = message,
            feedIndex = 0,
            walletBalance = walletBalance,
            approvedPets = approvedPets,
            approvedPetIndex = 0,
            posts = nextPosts
        )
    }

    fun applyCheckInResult(
        state: PetShellState,
        walletBalance: Int?,
        claimed: Boolean,
        rewardAmount: Int,
        usedFallback: Boolean,
        message: String
    ): PetShellState {
        if (state.checkInClaimed) {
            return state.copy(
                petAction = PetAction.Idle,
                speechBubble = "Daily reward already claimed."
            )
        }

        val nextWalletBalance = walletBalance ?: state.walletBalance + rewardAmount
        return state.copy(
            walletBalance = nextWalletBalance,
            checkInClaimed = claimed,
            petAction = PetAction.Reward,
            speechBubble = message
        )
    }
}

private fun Int.floorMod(divisor: Int): Int {
    if (divisor <= 0) return 0
    val remainder = this % divisor
    return if (remainder >= 0) remainder else remainder + divisor
}
