# Gamer Pet Community Ecosystem Design

Date: 2026-06-04
Status: draft for user review

## Goal

Create `D:\workspace4Codex\gamer` as the main workspace for a pet-first community ecosystem.
The product combines four ideas:

- A community where users browse, publish, review, and collect desktop pet creations.
- An Android-first app shell where the pet is the main interaction surface.
- A submission and reward system based on pet package ownership, completion, visual quality, and daily activity.
- An integration layer that connects to the existing `fantasy-pet-rule` generation flow without weakening its visual review gates.

The work should be split into independent subprojects so Android UI, backend services, generation tooling, contracts, and review tools can be developed separately.

## Local Context

Existing local projects should be treated as upstream or adjacent systems, not copied into `gamer`.

- `D:\workspace4Codex\fantasy-pet-rule` is the current source of truth for the Codex-only pet generation state machine.
- `D:\workspace4Codex\fantasy-pet-kmp` contains KMP, preview, package, and runtime evidence that can inform Android integration.
- `D:\workspace4Codex\floating-pet-android` contains Android floating pet experiments that can inform overlay/runtime behavior.

`gamer` should consume outputs from these projects through stable contracts: pet packages, state summaries, score reports, preview evidence, ownership claims, and import manifests.

## Product Principles

1. Pet-first, community-second.
   The app should feel like browsing through a companion's eyes, not like a normal forum with a mascot pasted on top.

2. Generation flow remains gated.
   Any generated image or action is a candidate until accepted by the proper visual and technical gates in the generation flow.

3. Ownership is declared, reviewed, and auditable.
   Uploading a package should require a license and ownership claim. Rewards should be reversible or holdable if moderation finds risk.

4. Currency starts as a reward ledger, not a full economy.
   Daily check-in and approved submissions can grant currency first. Redemption, marketplace, trading, and advanced sinks can be added later.

5. Every major subsystem can run with fake data.
   Android UI, backend API, generator adapter, and review console should each be testable before the full chain exists.

## Workspace Layout

```text
gamer/
  apps/
    android-community/
    admin-review/
  services/
    community-api/
    pet-generator/
  packages/
    pet-package-spec/
    pet-runtime/
    community-contracts/
  docs/
    architecture/
    product/
    api/
    superpowers/
      specs/
```

## Subprojects

### `apps/android-community`

Android-first community client.

Responsibilities:

- Show the app loading process as a pet speech bubble by default.
- Let the user tap the speech bubble to enter the full app.
- Browse community content with pet-led actions for next page, previous page, and skip-forward navigation.
- Display pet profiles, user profiles, posts, submission status, currency balance, and daily check-in.
- Import and preview approved pet packages.

Early implementation can use local JSON fixtures and local pet packages. It should not depend on a live backend for the first UI/runtime milestone.

### `apps/admin-review`

Moderation and review console.

Responsibilities:

- Review submitted pet packages, license claims, ownership claims, and scoring evidence.
- Display score breakdowns: completeness, visual consistency, action quality, package validity, preview evidence, and IP risk.
- Approve, reject, hold, or revoke reward grants.
- Provide audit history for reward decisions.

This can start as a web app with mocked API data, then connect to `community-api`.

### `services/community-api`

Main backend for community and reward state.

Responsibilities:

- Users, profiles, posts, comments, reactions, and feeds.
- Pet package registry and submission records.
- Ownership claim records and moderation state.
- Currency ledger for daily check-in and submission rewards.
- Scoring job orchestration and score report storage.
- Public app API and private admin API.

The service should expose stable API contracts before the Android app depends on it.

### `services/pet-generator`

Adapter around the existing generation workflow.

Responsibilities:

- Start or import `fantasy-pet-rule` runs.
- Read generation state without bypassing gates.
- Convert accepted generation outputs into community import records.
- Produce package summaries and scoring inputs.
- Return evidence paths, preview paths, and status summaries to `community-api`.

This service should not generate assets by ad hoc scripts. It should call or wrap the existing `fantasy-pet-rule` tools and respect their accepted/blocked/current-stage semantics.

### `packages/pet-package-spec`

Shared schema package for pet assets.

Responsibilities:

- Define pet package manifest fields.
- Define `license.json`, ownership claim, score report, preview evidence, and package validation result contracts.
- Provide validators used by `community-api`, `pet-generator`, `admin-review`, and Android import flows.

This package is the first recommended build target because it protects all later subprojects from drifting.

### `packages/pet-runtime`

Shared runtime model for pet behavior.

Responsibilities:

- Define pet actions, action triggers, speech bubbles, app-loading presentation, feed navigation gestures, and transition semantics.
- Map community interactions to pet actions.
- Provide runtime fixtures that Android can use before real package import exists.

This package should describe behavior in platform-neutral terms, while Android decides the rendering implementation.

### `packages/community-contracts`

Shared API and fixture contracts.

Responsibilities:

- Define user, post, feed, profile, wallet, check-in, submission, moderation, and reward ledger DTOs.
- Provide mock data for Android and admin prototypes.
- Keep frontend/backend naming consistent.

## Core Flows

### App Launch Bubble Flow

```text
user opens app
-> pet appears immediately
-> app loading status is shown inside pet speech bubble
-> user taps bubble
-> full community page opens
-> pet remains available as navigation companion
```

Default behavior uses this flow. Settings may later allow a traditional launch flow.

### Pet-First Browsing Flow

```text
feed page visible
-> user taps pet action zone or navigation control
-> pet performs next/previous/skip-forward action
-> feed request runs
-> new content page appears
-> pet returns to idle or context-specific reaction
```

Navigation must remain accessible through normal UI controls too, so the pet interaction is delightful rather than mandatory.

### Submission Reward Flow

```text
user submits pet package
-> ownership claim and license metadata required
-> package validator runs
-> scoring job produces score report
-> admin or policy gate approves
-> currency ledger records grant
-> user wallet reflects reward
```

Rewards should be ledger entries, not direct balance mutation. This makes moderation, rollback, and audit possible.

### Daily Check-In Flow

```text
user opens check-in
-> app checks today's claim state
-> successful claim writes ledger grant
-> pet performs reward reaction
-> wallet view updates
```

The daily reward amount can be fixed in the first milestone.

### Generator Integration Flow

```text
natural pet idea or existing run
-> fantasy-pet-rule run progresses through its state gates
-> accepted preview/package evidence exists
-> pet-generator reads state and outputs import summary
-> community-api creates submission or registry record
```

`pet-generator` may report blocked states, but it must not mark blocked assets as community-ready.

## Initial Data Contracts

### Pet Package Manifest

```json
{
  "schema": "gamer.pet-package.v1",
  "petId": "string",
  "displayName": "string",
  "ownerUserId": "string",
  "source": {
    "kind": "fantasy-pet-rule",
    "runId": "string",
    "statePath": "string"
  },
  "assets": {
    "baseImage": "string",
    "previewImage": "string",
    "motionSheets": []
  },
  "license": "license.json",
  "scoreReport": "score-report.json"
}
```

### Ownership Claim

```json
{
  "schema": "gamer.ownership-claim.v1",
  "claimId": "string",
  "userId": "string",
  "petId": "string",
  "claimType": "original-created",
  "attestation": "string",
  "sourceReferences": [],
  "submittedAt": "string",
  "reviewStatus": "pending"
}
```

### Score Report

```json
{
  "schema": "gamer.pet-score-report.v1",
  "petId": "string",
  "totalScore": 0,
  "breakdown": {
    "packageCompleteness": 0,
    "visualQuality": 0,
    "actionCoverage": 0,
    "identityConsistency": 0,
    "previewEvidence": 0,
    "licenseReadiness": 0
  },
  "rewardRecommendation": {
    "grant": true,
    "amount": 0,
    "reason": "string"
  },
  "risks": []
}
```

### Currency Ledger Entry

```json
{
  "schema": "gamer.currency-ledger-entry.v1",
  "entryId": "string",
  "userId": "string",
  "amount": 0,
  "sourceType": "daily-checkin",
  "sourceId": "string",
  "status": "posted",
  "createdAt": "string"
}
```

## Development Phases

### Phase 0: Contracts and Workspace

Create the `gamer` workspace structure and define schemas, fixtures, and validation expectations.

Deliverables:

- `packages/pet-package-spec`
- `packages/pet-runtime`
- `packages/community-contracts`
- fixture pet packages and fixture feed data

### Phase 1: Android Pet Shell

Build the Android app with local fixtures.

Deliverables:

- Pet appears during app launch.
- Loading is shown inside pet speech bubble.
- Tap bubble opens the community page.
- Feed pagination can be triggered by pet actions.
- Profile, wallet, and check-in screens are present with mocked state.

### Phase 2: Community API

Build backend flows against the shared contracts.

Deliverables:

- User/profile/feed APIs.
- Submission APIs.
- Daily check-in ledger.
- Reward ledger.
- Admin review state.

### Phase 3: Generator Adapter

Connect accepted `fantasy-pet-rule` outputs to community submissions.

Deliverables:

- Read run state.
- Detect blocked, candidate, accepted, and preview-ready statuses.
- Produce package import summaries.
- Generate score report inputs.

### Phase 4: Review and Reward Console

Build admin review tools.

Deliverables:

- Submission queue.
- Score breakdown view.
- Ownership and license review.
- Reward approval and revoke actions.

### Phase 5: Integrated MVP

Connect Android, API, generator adapter, and admin console.

Deliverables:

- Submit or import a pet package.
- Review and approve the package.
- Grant currency.
- Show the approved pet in community and Android views.

## Testing Strategy

- Contract tests for schema validation and fixture compatibility.
- Android UI tests for launch bubble, feed navigation, check-in, and wallet update.
- API tests for submissions, ledger entries, moderation state, and feed reads.
- Generator adapter tests using known `fantasy-pet-rule` run states.
- Review console tests for approve, reject, hold, and revoke flows.

## Main Risks

### Reward Abuse

Risk: users submit low-effort or repeated packages for currency.

Mitigation: reward through auditable ledger entries, require package validation, apply review gates, and add rate limits before public launch.

### IP and Ownership Risk

Risk: users upload assets they do not own.

Mitigation: require license metadata and ownership claims, show risk flags in admin review, and allow reward hold/revoke.

### Generation Gate Drift

Risk: community integration accidentally treats candidates as finished packages.

Mitigation: `pet-generator` reads `fantasy-pet-rule` state and only imports accepted preview/package evidence.

### Android Complexity

Risk: app launch bubble, floating pet, and community UI become tangled.

Mitigation: keep `pet-runtime` behavior contracts separate from Android rendering and keep normal navigation controls available.

## Recommended First Implementation Plan

Start with Phase 0. The first concrete implementation plan should define:

- The exact schemas in `packages/pet-package-spec`.
- The runtime action model in `packages/pet-runtime`.
- Mock feed, wallet, check-in, submission, and review fixtures in `packages/community-contracts`.
- A small compatibility checklist for importing evidence from `fantasy-pet-rule`.

After Phase 0 is accepted, Android and backend can proceed in parallel.
