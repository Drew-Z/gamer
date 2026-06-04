# Android Live API Integration Design

Date: 2026-06-05
Status: draft for user review

## Goal

Phase 5b connects `apps/android-community` to the local `services/community-api` for the first live Android MVP path.

The scope is intentionally small:

- Load feed posts from `GET /v1/feed`.
- Load wallet balance from `GET /v1/wallet/me`.
- Claim daily check-in through `POST /v1/check-in`.
- Keep the current local pet shell behavior working when the API is unavailable.

This phase should make Android feel connected to the ecosystem without coupling it to the still-changing `fantasy-pet-rule` workflow or to admin review features.

## Context

Current Android state is fully local:

- `PetShellController.initialState()` provides fixture feed posts, wallet balance, check-in state, and launch bubble text.
- `PetShellController.claimDailyReward()` mutates local wallet state by adding `10`.
- Feed navigation is local pagination over `PetShellState.posts`.
- Compose UI owns state directly with `remember { mutableStateOf(...) }`.

Current backend support is enough for this phase:

- `GET /v1/feed` returns feed post DTOs.
- `GET /v1/wallet/me` returns wallet state with balance and ledger entries.
- `POST /v1/check-in` records the daily claim and returns claim state.

The feed DTO currently includes `authorId`, not a display name. Android should map this to a stable fallback display label for now, such as `Demo Keeper`, and avoid introducing a user/profile dependency in this phase.

## Considered Approaches

### Recommended: Small Repository Layer With Local Fallback

Add a narrow Android data layer around the existing pure pet shell controller.

Shape:

- `CommunityApiClient` defines the remote calls Android needs.
- `HttpCommunityApiClient` implements those calls against the local API.
- `CommunityRepository` combines remote data, DTO mapping, and fallback fixtures.
- `PetShellController` remains focused on state transitions.
- `PetShellApp` triggers loading and check-in orchestration.

Why this is recommended:

- It keeps the existing Android prototype intact.
- It gives tests a clean fake-client boundary.
- It avoids committing to a larger app architecture before the API surface stabilizes.
- It supports real data while preserving the "pet appears immediately" launch experience.

### Alternative: Direct Network Calls From Compose UI

The UI could call the API directly from `PetShellApp`.

This is faster in the very short term, but it mixes rendering, network errors, mapping rules, fallback behavior, and pet state transitions in one file. It would make the next phases harder to test and harder to split.

### Alternative: Full ViewModel And Architecture Stack

The app could immediately adopt ViewModels, dependency injection, a persistence layer, and a full repository stack.

This is likely too much for the first live connection. It is a good future direction once the community app has multiple screens, auth, cache policy, and real user identity.

## Chosen Architecture

```text
PetShellApp
  -> CommunityRepository
       -> CommunityApiClient
            -> HttpCommunityApiClient
       -> DTO mappers
       -> local fallback fixtures
  -> PetShellController
       -> pure shell state transitions
```

The controller should stay pure. It may receive helper methods for applying remote results, but it should not know about HTTP, URLs, JSON parsing, or retry behavior.

The repository is the integration boundary. It owns:

- API base URL selection.
- Remote request success and failure mapping.
- Conversion from API DTOs into Android `FeedPost` and wallet/check-in state patches.
- Returning local fallback data when the local API is not reachable.

## API Base URL

Use a single local prototype base URL first:

```text
http://10.0.2.2:4000
```

This targets the host machine from an Android emulator.

For later real-device testing, the value can move to a Gradle build config field or settings screen. Phase 5b should not add environment switching UI.

## Launch Flow

The pet must still appear immediately.

```text
app starts
-> PetShellController.initialState() renders the pet and "Loading community..."
-> Android starts loading feed and wallet from CommunityRepository
-> if remote succeeds, state updates with live posts and live wallet balance
-> if remote fails, state keeps local fixtures and bubble explains local fallback
-> tapping the bubble enters the community page as before
```

Remote loading should not block first paint. The launch bubble remains the primary loading surface.

## Feed Flow

Remote feed loading should replace the initial local feed list when successful.

Mapping rule:

- API `id` -> Android post id.
- API `title` -> Android title.
- API `body` -> Android body/description.
- API `reactionCount` -> Android reaction count.
- API `authorId` -> Android author label fallback.

The current next, previous, and skip navigation behavior remains local over the loaded post list. If the remote list is empty or unavailable, fixture posts remain active.

## Wallet Flow

Remote wallet loading should update:

- `walletBalance`
- currency label if Android later exposes it
- check-in availability only when check-in state is returned or inferred safely

Phase 5b should not display the full ledger yet. The ledger remains a backend/admin concern until a wallet screen needs history.

## Check-In Flow

When the user claims the daily reward:

```text
tap claim
-> call POST /v1/check-in
-> on success, update claimed state and wallet balance from API-derived result
-> pet performs reward reaction
-> on failure, use existing local claim behavior and show fallback bubble text
```

The fallback should be explicit in the speech bubble so local testing does not look like a confirmed server-side reward.

## Error Handling

Errors should degrade to the existing local prototype instead of blocking the app.

Required behavior:

- API unavailable on launch: keep local feed and local wallet.
- Feed request fails: keep current posts.
- Wallet request fails: keep current balance.
- Check-in request fails: allow local claim once and mark speech as local fallback.
- JSON parse failure: treat as remote failure.
- Empty feed response: keep current posts and show a short fallback message.

No modal error screen is needed in this phase. The pet speech bubble is enough.

## Testing Strategy

Phase 5b should be test-first at the data and controller boundary.

Required tests:

- DTO mapper converts API feed posts into Android feed posts.
- Repository returns remote feed and wallet when the fake client succeeds.
- Repository returns fallback feed and wallet when the fake client fails.
- Check-in success updates wallet and claimed state.
- Check-in failure uses local fallback behavior.
- Existing `PetShellControllerTest` still passes.

Network code can remain thin and lightly tested through mapper/repository tests. Live server integration can be verified manually after unit tests pass.

## Dependencies

Prefer the smallest dependency change that gives reliable local HTTP and JSON parsing.

If Android's existing dependency set is enough, keep it. If adding a dependency is necessary, add only one narrow option for HTTP/JSON and document it in the implementation plan.

Do not change the verified Gradle plugin baseline as part of Phase 5b:

- Keep `com.android.application`.
- Keep `org.jetbrains.kotlin.plugin.compose`.
- Do not reintroduce `org.jetbrains.kotlin.android` unless a later verified Android baseline requires it.

## Out Of Scope

Phase 5b does not include:

- Real authentication.
- User profile lookup for feed author names.
- Admin review UI integration.
- Pet package import from Android.
- `fantasy-pet-rule` state binding.
- Wallet ledger history UI.
- Real-device API discovery.
- Offline persistence beyond in-memory fallback fixtures.

## Success Criteria

The phase is complete when:

- Android launches with the pet bubble immediately.
- With `community-api` running, Android displays live feed and wallet data from the API.
- Daily check-in calls the API and updates the pet shell state.
- With `community-api` stopped, Android still works using local fixtures.
- Unit tests cover repository fallback, mapping, check-in success, and check-in failure.
- Existing Android unit tests and backend tests still pass.
