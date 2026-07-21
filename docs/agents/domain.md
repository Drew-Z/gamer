# Domain Docs

How engineering skills should consume this repo's domain documentation when exploring the codebase.

## Layout

This repo uses a single-context documentation layout:

- `CONTEXT.md` at the repo root, when present
- `docs/adr/` at the repo root, when present

If these files do not exist, proceed silently. Do not flag their absence or suggest creating them upfront. Producer workflows can create them later when domain terms or architectural decisions need to be resolved.

## Repository Role

`gamer` is the app/community workspace. It owns the Android app, app-facing community services, admin review prototype, and shared packages used by the app/community surface.

`fantasy-pet-rule` is a separate sibling repository under `D:\workspace4Cursor\pet\fantasy-pet-rule`. It owns server-side generation rules, worker orchestration, pipeline logic, QA gates, and the public app API contract consumed by this repo.

Do not move worker, Codex, GenericAgent, private generation, or server-admin concerns into `gamer` unless the user explicitly changes the repository boundary.

## Folder Responsibilities

- `apps/android-community/`: Android app prototype and app-side integration with the community API.
- `apps/admin-review/`: Admin review prototype for moderation and review workflows.
- `services/community-api/`: Main app/community backend entry point and app gateway.
- `services/pet-generator/`: Pet generation adapter/service shell used by this workspace.
- `packages/community-contracts/`: Shared API contracts and drift guards for app/community integration.
- `packages/pet-package-spec/`: Shared package manifest and `pet.zip` format rules.
- `packages/pet-runtime/`: Shared runtime helpers for pet packages.
- `docs/`: Human-readable project docs, API notes, specs, and agent workflow config.
- `tools/`: Local verification, smoke, and integration helper scripts.

## Before Exploring

Read `CONTEXT.md` before making domain-sensitive changes, if it exists. Read ADRs in `docs/adr/` when they touch the area you are about to work in.

Use the repository boundary above when planning changes: app-facing UX, Android behavior, community API, and shared app contracts belong here; server worker and private generation pipeline changes belong in `fantasy-pet-rule`.

## Use the Project Vocabulary

When your output names a domain concept in an issue title, refactor proposal, hypothesis, or test name, use the term as defined in `CONTEXT.md`.

If the concept you need is not in the glossary yet, either reconsider whether the project uses that language or note the gap for a documentation workflow.

## Flag ADR Conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding it.
