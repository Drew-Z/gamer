# Gamer Mobile Community UI Direction

This note captures the UI skills and online references currently used for the Android community shell. Treat it as a project-local design skill, not as a copy of any one product.

## Skill Stack

- `gamer-mobile-community-ui`: project-local skill for this Android pet-community app. Source copy: `D:\workspace4Codex\pet\gamer\docs\skills\gamer-mobile-community-ui\SKILL.md`.
- `frontend-design`: choose a clear aesthetic point of view and avoid prototype-looking generic screens.
- `web-design-guidelines`: use as a review pass for hierarchy, accessibility, readable density, and layout polish.
- `theme-factory`: translate the visual direction into stable color roles instead of one-off colors.
- `local-test-android-apps-android-emulator-qa`: verify key screens on the Android emulator with screenshots.
- Jetpack Compose design systems: keep custom surfaces wrapped around Material behavior rather than replacing interaction, semantics, or testing hooks.

## Online References

- Vercel Web Interface Guidelines: `https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md`
- Android Compose custom design systems: `https://developer.android.com/develop/ui/compose/designsystems/custom`
- Material Design 3: `https://m3.material.io/`
- Material Design accessibility: `https://m3.material.io/foundations/accessible-design/overview`
- Android Compose accessibility: `https://developer.android.com/develop/ui/compose/accessibility`
- Frontend skill discovery example: `https://github.com/finfin/awesome-frontend-skills`
- Addy Osmani agent skills, `frontend-ui-engineering`: `https://github.com/addyosmani/agent-skills`
- Android official skills: `https://github.com/android/skills`
- HoYoLAB Google Play listing: `https://play.google.com/store/apps/details?id=com.mihoyo.hoyolab`
- Profile pattern notes: `https://gummble.com/patterns/profile`
- Samsung One UI design guide: `https://design.samsung.com/global/contents/one-ui/download/oneui_design_guide_eng.pdf`

## Online Skill Scan

Do not install external skills globally by default. Treat them as reference material until their source, scope, and safety are reviewed.

- `openai/skills` `frontend-skill`: useful for composition-first product UI, utility copy, image/visual anchoring, and avoiding card-heavy generic app shells. It is web-oriented, so only carry over the hierarchy and restraint rules.
- `ibelick/ui-skills`: useful as a checklist for accessibility, reduced motion, short interaction animations, clear empty states, and avoiding heavy blur/glow/gradient effects. Most rules are Tailwind/React-specific, so translate them into Compose equivalents instead of installing verbatim.
- `pbakaus/impeccable`: useful as a two-mode design workflow, especially the distinction between brand surfaces and product surfaces. For this app, use the product mode idea: UI serves daily community operation, generation review, and pet ownership.
- `nextlevelbuilder/ui-ux-pro-max-skill`: useful as a broad design-intelligence catalog, but too large and generic for this repo. Pull from it only when we need a deliberate style exploration pass.
- `frontend-ui-engineering` from `addyosmani/agent-skills`: useful for component composition, design-system adherence, simple state ownership, accessibility, and avoiding generic AI UI.
- `android/skills`: useful for Android-native guidance such as Jetpack Compose, edge-to-edge, app testing, and performance workflows. Prefer it over web-first skills when a rule affects Compose structure or Android behavior.
- `mobile-design` / `mobile-ui-design-specialist`: useful for touch target discipline, thumb-zone actions, safe areas, mobile forms, bottom navigation, and native-feeling motion.
- `ui-from-image`: useful later when we intentionally compare against HoYoLAB or other community-app screenshots. It should be used as a fidelity audit tool, not as permission to clone brand assets.
- `premium-frontend-ui` / `design-taste-frontend`: useful as inspiration for stronger art direction, but should be filtered heavily for Android performance and the app's pet-first community purpose.

2026-06-09 second online UI-skill scan:

- Keep `android/skills`, Android Developers Compose docs, and Material 3 as the source of truth for Android structure, accessibility, navigation, and performance decisions.
- Use OpenAI `frontend-skill`, `ibelick/ui-skills`, `impeccable`, and `frontend-ui-engineering` as taste and review prompts only. The useful shared pattern is: one clear product mode, fewer nested panels, compact repeated modules, concrete empty-state actions, reduced motion, and visible state feedback.
- Do not install remote UI skills directly into the working agent profile. Copy durable project-specific ideas into `gamer-mobile-community-ui` after reviewing source, scope, and Android fit.
- HoYoLAB/MiHoYo-style community references should inform density and game-community utility hierarchy, not brand assets or a visual clone.
- Every online UI reference must be translated into a small Android Compose rule, an implemented component change, or an emulator screenshot target.

Decision after the web scan:

- Do not install remote UI skills directly. They vary in scope and are usually web-first or aesthetic-first.
- Keep a fixed project-local skill instead: `gamer-mobile-community-ui`.
- Promote that skill into `C:\Users\zhang\.codex\skills\gamer-mobile-community-ui\SKILL.md` when we want Codex to discover it automatically after restart.
- Treat external UI skills as research inputs only; any instruction that affects this app should be copied into project docs in our own words.

Project-local takeaway:

- Build screens as compact mobile product surfaces, not web landing pages.
- Use stable component primitives and semantic test hooks before decorative detail.
- Keep touch targets, keyboard dismissal, disabled states, and scroll reachability in the design checklist.
- Prefer a narrow design token layer over scattering one-off color, shape, and spacing choices through Compose code.
- Use real emulator screenshots as the design review artifact for each major screen.
- Keep motion under user control: use short transform/alpha feedback for buttons, tabs, and pet reactions; avoid layout animation, large blur, glow, or repeated gradient work on scrolling surfaces.
- Make every loading, empty, review, failed, and blocked state show one concrete next action near the state itself.
- Treat online UI skills as input to this document and `gamer-mobile-community-ui`; do not let a remote skill override fantasy-pet public API safety rules.

2026-06-09 UI scan follow-up:

- Keep remote UI skills as research inputs. Do not install them globally without reviewing source and scope.
- The generation page now uses a more compact console layout: slimmer flow rail, shorter status tiles, lighter prompt canvas, and tighter stage headers.
- Screenshot artifact: `D:\workspace4Codex\pet\gamer\tmp-gamer-generation-compact-console-ui.png`.

2026-06-09 empty-generation refinement:

- Empty jobs now show the creation brief before progress/status modules, so the first generation screen starts with the user's prompt and supported controls instead of four waiting-state tiles.
- Screenshot artifact: `D:\workspace4Codex\pet\gamer\tmp-gamer-generation-empty-brief-first.png`.
- The description field now marks itself required and starts compact, keeping the disabled create button fully visible in the first screen.
- Screenshot artifact: `D:\workspace4Codex\pet\gamer\tmp-gamer-generation-compact-required-label.png`.

2026-06-09 online UI skill follow-up:

- Favor Android-native skills and docs when generic frontend advice conflicts with Compose, bottom navigation, safe areas, or emulator behavior.
- Keep the generation entry path thumb-reachable: after a user fills the prompt, the primary create action should be fully visible above the bottom navigation on the default emulator viewport.
- Use remote UI skill catalogs for discovery only. Before a rule becomes project policy, translate it into this document and `gamer-mobile-community-ui` in Android-specific language.
- The generation hero is now a compact product header instead of a tall mascot card, making the prompt-to-create path less crowded.
- Screenshot artifact: `D:\workspace4Codex\pet\gamer\tmp-gamer-generation-compact-hero-prompt-filled.png`.

2026-06-09 community quick-entry polish:

- Community quick actions now use compact glyphs for check-in, generation, review, and showcase. This makes the first community viewport feel more like a game companion app and less like a text-only prototype grid.
- Keep these entries short and thumb-friendly: the icon, action label, and one-line detail should fit without pushing the pet companion strip out of the first viewport.
- Screenshot artifact: `D:\workspace4Codex\pet\gamer\tmp-gamer-community-quick-icons.png`.

2026-06-09 profile compact home-base pass:

- Profile now uses a compact keeper hero with pet artwork and a short bubble instead of the taller action-labeled pet avatar. The wallet, shelf, and quick actions all fit more naturally in the first viewport.
- Keep profile surfaces as a home base: the hero should identify the keeper, the wallet should summarize reward state, and the action dock should remain visible above bottom navigation on the default emulator viewport.
- Screenshot artifact: `D:\workspace4Codex\pet\gamer\tmp-gamer-profile-compact-hero.png`.

2026-06-09 pet-view feed pass:

- Community posts now use a stronger pet-view header, a compact interaction badge, and a dark pet-perspective hint block. The feed should read as desktop-pet-guided browsing rather than a generic social card.
- Feed controls now use directional glyphs for previous, next, and skip-ahead actions while keeping the existing navigation callbacks and semantics.
- Screenshot artifacts: `D:\workspace4Codex\pet\gamer\tmp-gamer-community-feed-card-top.png`, `D:\workspace4Codex\pet\gamer\tmp-gamer-community-feed-card-controls.png`.

2026-06-09 showcase empty-state CTA:

- The community showcase empty state now shows the ecosystem path `generate -> human review -> showcase` and a primary `Create new pet` action instead of disabled previous/next pet controls.
- Empty states should explain the next useful action without implying automatic approval. The user still needs generation, human review, and community import before a pet appears in the showcase.
- Screenshot artifact: `D:\workspace4Codex\pet\gamer\tmp-gamer-community-showcase-empty-cta.png`.

2026-06-09 profile empty-shelf CTA:

- The profile pet shelf now mirrors the same `generate -> human review -> showcase` path when no approved pets exist, with a nearby `Create new pet` action.
- This keeps profile as the keeper home base while avoiding a passive "empty white panel" feeling.
- In the empty profile shelf, omit pending preview/package rows so the path, CTA, and common actions stay reachable above the bottom navigation.

2026-06-09 tab-aware shell header:

- The shared top header now follows the selected product surface instead of always saying `Gamer Community`.
- Generation shows the generation workspace title/subtitle; Profile shows the keeper home-base title/subtitle. The wallet and language controls remain shared chrome.
- This follows the online UI-skill scan takeaway: use product-mode orientation, compact repeated chrome, and Android-native verification rather than installing remote web-first skills.
- Screenshot artifacts: `D:\workspace4Codex\pet\gamer\tmp-gamer-header-generate.png`, `D:\workspace4Codex\pet\gamer\tmp-gamer-header-profile.png`.

2026-06-09 generation hero copy polish:

- The generation page keeps `Generation Workspace` as the shell orientation title, while the inner hero now uses a more specific safe-generation workbench title.
- This avoids the prototype-like feeling of repeating the same title twice in the first viewport.
- Screenshot artifact: `D:\workspace4Codex\pet\gamer\tmp-gamer-generation-hero-copy.png`.

2026-06-09 compact header utility dock:

- The shared language and wallet controls now live in a semantically grouped header utility dock.
- Wallet balance is the first compact status chip; language switching remains available but reads as app chrome rather than a large settings block.
- Keep `header-utility-dock` as the stable UI tree anchor for future header polish and smoke checks.
- Screenshot artifact: `D:\workspace4Codex\pet\gamer\tmp-gamer-header-utility-dock.png`.

2026-06-09 readable Android system bars:

- The app now applies a light system bar style with dark status and navigation icons before Compose content is rendered.
- This keeps the emulator status bar readable on the light shell background and avoids the unfinished feel of white icons on near-white UI.
- Screenshot artifact: `D:\workspace4Codex\pet\gamer\tmp-gamer-readable-system-bars.png`.

2026-06-09 immersive shell header:

- The shared top header now uses a tab-specific immersive background instead of a plain text row.
- Keep the header compact: 8dp radius, dark multi-hue gradient, subtle Canvas pattern, white title text, and a tab-specific accent.
- The wallet and language controls remain inside the header utility dock so app chrome stays grouped and testable.

2026-06-09 immersive header depth pass:

- The shared header backdrop now has a stable `gamer-immersive-header-backdrop` UI anchor for emulator regression checks.
- Keep the backdrop at a compact minimum height with layered depth: top light, dark floor, soft horizon platform, motion trail, and small accent panels.
- The header should feel like a pet-eye view into the selected surface while keeping the title, wallet, and language controls readable.

2026-06-09 market community UI scan:

- Sources checked: HoYoLAB Google Play listing (`https://play.google.com/store/apps/details?id=com.mihoyo.hoyolab`), TapTap Lite Google Play listing (`https://play.google.com/store/apps/details?id=com.taptap.global.lite`), Discord Google Play listing (`https://play.google.com/store/apps/details?id=com.discord`), Reddit Google Play listing (`https://play.google.com/store/apps/details?id=com.reddit.frontpage`).
- HoYoLAB points to the most relevant game-community stack for us: recommended posts, fan art sharing, official event information, and practical game tools should sit near the user's daily return path.
- TapTap reinforces a game-library/community mix: discovery, guides, reviews, creator content, and developer feedback work best when the app has a clear game/object context before the feed.
- Discord and Reddit are useful as structure references: spaces/channels/topics make dense communities navigable, while voting/reputation mechanics explain why a post matters.
- Product translation for Gamer: the first community viewport should not be only a post list. It needs a pet-led command center that summarizes wallet/check-in/showcase status and gives quick next actions, then channels, tools, showcase, and feed.
- The community home now starts with a pet navigator module before the channel rail. Keep it compact, status-rich, and action-oriented; avoid turning it into a marketing hero.
- Screenshot artifact: `D:\workspace4Codex\pet\gamer\tmp-gamer-community-market-ui-final.png`.

## Product Direction

The app should feel like a pet-first game community, closer to a lightweight game companion app than a generic admin dashboard.

Use these persistent traits:

- Pet as navigation anchor: hero strips, speech bubbles, and pet action states should explain where the user is.
- Dense but calm community layout: channel rail, quick actions, showcase, feed, and profile utilities should be scannable in one vertical flow.
- Game community energy: use warm accent color, teal identity color, compact badges, status rails, and small reward moments.
- Public API trust boundary: generation UI can look like a studio, but it must still visibly separate waiting, review, packaging, and download states.
- Human review gravity: candidate gallery and review controls should look deliberate, not like a one-click automation surface.

## Compose Implementation Rules

- Keep shape radius at `8.dp` or below unless the existing component requires another shape.
- Avoid nested card stacks. A major section can be a `Surface`; inner metrics should be lightweight boxes, chips, or rows.
- Keep automation semantics stable. Visual polish must not rename content descriptions used by tests.
- Preserve Material 3 interaction primitives for buttons, text fields, navigation, and disabled states.
- Use color roles consistently: identity teal, reward orange, trust slate, review blue, warning red.
- Keep text compact inside tool surfaces. Profile, generation, and feed cards should use smaller headings than launch or hero areas.
- Verify emulator screenshots after meaningful visual changes.
