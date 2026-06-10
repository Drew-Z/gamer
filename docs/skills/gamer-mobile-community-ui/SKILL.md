---
name: gamer-mobile-community-ui
description: Use when designing, reviewing, or implementing UI for the D:\workspace4Codex\pet\gamer Android community app. This skill keeps the app aligned with a pet-first game-community direction, Material 3 Compose behavior, HoYoLAB-like density, fantasy-pet public API safety boundaries, and emulator-verified mobile polish.
---

# Gamer Mobile Community UI

Use this skill whenever work touches the Android community shell, generation flow, profile, feed, navigation, or visual design tokens in `D:\workspace4Codex\pet\gamer`.

The goal is not to clone any existing community app. The goal is to make Gamer feel like a pet-first game companion community: compact, lively, trustworthy, and usable on Android.

## Starting Context

Read `D:\workspace4Codex\pet\gamer\docs\ui\mobile-community-ui-direction.md` when available. Treat it as the project design brief and update it when the UI direction changes.

Relevant reference styles:

- Game community apps such as HoYoLAB and MiHoYo community products: dense feed, event-like modules, compact profile economy, warm reward surfaces.
- Material 3 and Jetpack Compose custom design systems: keep standard interaction, state, semantics, accessibility, and testability.
- Android official skills: prefer Android-native guidance for Compose layout, edge-to-edge, testing, and performance details when it conflicts with web-first UI advice.
- Frontend/UI engineering skills: avoid generic prototype screens; use intentional hierarchy, real states, and production details.
- Online UI skills such as OpenAI `frontend-skill`, `ibelick/ui-skills`, `impeccable`, and UI/UX catalog skills may be used as research inputs only. Translate useful ideas into this Android Compose skill; do not install or follow remote web-first rules blindly.

Online scan rule of thumb:

- Treat remote UI skills as inspiration, not executable project policy. Prefer copying durable ideas here in our own words.
- Translate web-first advice into Android-native primitives: Material fields/buttons/navigation, Compose semantics, emulator screenshots, and public API state safety.
- Cross-check any remote UI rule that touches layout, motion, navigation, accessibility, or performance against Android/Material guidance before applying it to Compose.
- Convert online UI research into a concrete component rule, implemented change, or screenshot target; avoid adding broad aesthetic rules that cannot be verified in the emulator.
- For this app, use the "product surface" mode from design-system skills: compact modules for repeat use, one vivid pet-led identity surface per screen, and lightweight boxes/chips inside major sections.
- Keep generation and review controls dense but legible. Avoid form-in-card-in-card layouts; use short section headers, status tiles, segmented controls, and one visible next action.
- Treat bottom navigation and safe areas as part of the design surface. Primary actions must be fully visible and reachable above the nav bar on the default emulator viewport.

## Product Principles

- Put the desktop pet in the frame before generic community chrome. The pet should anchor launch, navigation, profile, and generation states.
- Make screens feel like an app people return to daily, not a landing page. Prefer dense mobile product surfaces over oversized hero blocks.
- Keep community browsing, pet generation, profile, wallet, and review flows visually related through shared tokens and repeated component language.
- Preserve the fantasy-pet public API trust boundary. UI polish must never hide state, imply automatic approval, expose internal paths, or allow package download before readiness.
- Keep Chinese as the primary default interface while preserving the existing bilingual string path.

## Visual Direction

Use a warm game-community palette with clear functional roles:

- Identity teal for pet/community presence.
- Reward orange or gold for coins, sign-in, score, and achievement moments.
- Review blue for human review and candidate inspection.
- Trust slate for infrastructure, API, waiting, and neutral surfaces.
- Warning red only for failed, blocked, reject, or destructive states.

Avoid one-note purple gradients, generic white test pages, decorative blobs, and oversized marketing composition. The app should feel designed, but still fast on an emulator.

## Compose Implementation

- Build on Material 3 components for buttons, fields, navigation, disabled states, ripples, and semantics.
- Use a small token layer for color, shape, spacing, and elevation instead of scattering one-off values.
- Keep cards at `8.dp` radius or less unless the existing component already requires a different shape.
- Avoid nested cards. Use section bands, rows, chips, and lightweight boxes inside major surfaces.
- Keep touch targets at least `48.dp` where practical.
- Keep text compact and container-aware. Do not let labels overflow buttons, cards, or chips on small devices.
- Keep test tags/content descriptions stable unless tests are updated in the same change.
- Avoid heavy first-frame rendering. Be cautious with repeated gradients, large Canvas work, high elevation shadows, and animated content on the initial route.
- Keep motion small and production-like: prefer transform/alpha feedback, pause looping work when off-screen, avoid animating layout, and avoid blur/glow/large-gradient effects on scrollable surfaces.
- Every loading, empty, failed, blocked, waiting-for-review, and ready-for-download state should show one nearby next action or clearly explain why no action is available.

## Screen Patterns

Shell:

- The shared app header must describe the selected product surface, not only the default community feed.
- Community, generation, and profile tabs should each expose their own title/subtitle while keeping the same wallet and language controls.
- Treat the header as orientation chrome: it should make the current mode obvious before the user reads the inner content.
- Inner hero strips should add context for the current workflow instead of repeating the shared header title verbatim.
- Keep the shared language and wallet controls compact and semantically grouped; they should feel like app chrome, not a debug panel.
- On light shell backgrounds, configure Android system bars with readable dark status/navigation icons before screenshot verification.

Launch:

- Show the pet immediately as the first-viewport signal.
- Keep the loading/app-entry bubble short, tappable, and semantically testable.
- Do not block the first navigation path behind decorative animation.

Community:

- Use a compact feed with channel filters, pet reaction affordances, and visible next actions.
- Quick actions should read as game-community shortcuts: use small glyphs plus short labels, not text-only utility boxes.
- Feed cards should make the pet-view premise visible through pet identity, reaction badges, and directional navigation affordances.
- Make previous/next/page actions feel pet-driven, but keep buttons discoverable and testable.
- Showcase empty states should present the generation -> human review -> showcase path and one clear create action, not disabled dead-end controls.
- Use screenshots or generated pet assets as real visual anchors where available.

Generation:

- Treat the generation area as a studio/workbench, not as a plain form.
- Keep description input, body shape, reference URL, job creation, polling, candidate gallery, review notes, and package download in one understandable vertical path.
- Show waiting, processing, waiting-for-review, packaging, ready-for-download, revision-requested, candidate-rejected, and failed as distinct states.
- Candidate artifacts must use `downloadUrl` or public artifact download IDs only.
- Accept/revise/reject must require human action. Revise/reject require specific notes.

Profile:

- Make profile feel like the keeper's home base: pet shelf, wallet, score, sign-in, ownership, and quick actions.
- Use a compact keeper hero on mobile so wallet, pet shelf, and quick actions can appear without feeling buried below the fold.
- Use compact metric rows and pet cards, not empty white panels.
- When the pet shelf is empty, mirror the ecosystem path from community showcase: generation -> human review -> showcase, plus one nearby create action.
- Surface currency and completion progress without making rewards feel like the whole product.

## Review Checklist

Before finishing UI work:

- Capture or inspect emulator screenshots for the touched screens.
- Verify the UI still works on a narrow Android viewport.
- Check that the first screen is not blank, stalled, or too expensive to render.
- Check disabled, loading, empty, error, and review states where the flow supports them.
- Confirm public API safety rules still hold: no admin calls, no internal paths, no automatic accept, no download before `downloadReady`.
- Run focused unit tests and relevant Android smoke tests when the change touches generation or navigation behavior.
