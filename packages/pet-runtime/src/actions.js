export const petActions = {
  idle: {
    id: "idle",
    displayName: "Idle",
    loop: true
  },
  appLoading: {
    id: "app-loading",
    displayName: "App Loading",
    loop: true
  },
  bubbleOpen: {
    id: "bubble-open",
    displayName: "Bubble Open",
    loop: false
  },
  feedNext: {
    id: "feed-next",
    displayName: "Feed Next",
    loop: false
  },
  feedPrevious: {
    id: "feed-previous",
    displayName: "Feed Previous",
    loop: false
  },
  feedSkip: {
    id: "feed-skip",
    displayName: "Feed Skip",
    loop: false
  },
  reward: {
    id: "reward",
    displayName: "Reward",
    loop: false
  },
  review: {
    id: "review",
    displayName: "Review",
    loop: true
  }
};

export function getActionForLaunchStage(stage) {
  if (stage === "loading") {
    return petActions.appLoading;
  }

  if (stage === "ready") {
    return petActions.bubbleOpen;
  }

  return petActions.idle;
}

export function getActionForFeedNavigation(direction) {
  if (direction === "next") {
    return petActions.feedNext;
  }

  if (direction === "previous") {
    return petActions.feedPrevious;
  }

  if (direction === "skip") {
    return petActions.feedSkip;
  }

  return petActions.idle;
}
