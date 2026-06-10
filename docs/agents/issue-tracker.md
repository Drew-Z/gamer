# Issue Tracker: GitHub

Issues and PRDs for this repo live in GitHub Issues for `Drew-Z/gamer`.

## Conventions

- Create feature work as GitHub issues unless the user explicitly asks for a local draft first.
- Keep PRDs and implementation tickets in GitHub issues, linked together by issue references.
- Use labels from `triage-labels.md` for workflow state.
- Prefer `gh` CLI commands when `gh` is installed and authenticated.
- In this current Windows workspace, `gh` is not on `PATH`; until it is installed, inspect public issue metadata through GitHub web/API and ask before attempting remote mutations.

## Common Operations

- Create an issue: `gh issue create --repo Drew-Z/gamer --title "..." --body "..."`
- Read an issue: `gh issue view <number> --repo Drew-Z/gamer --comments`
- List issues: `gh issue list --repo Drew-Z/gamer --state open`
- Comment on an issue: `gh issue comment <number> --repo Drew-Z/gamer --body "..."`
- Apply a label: `gh issue edit <number> --repo Drew-Z/gamer --add-label "..."`
- Close an issue: `gh issue close <number> --repo Drew-Z/gamer --comment "..."`

## When a Skill Says "Publish to the Issue Tracker"

Create or update a GitHub issue in `Drew-Z/gamer`.

## When a Skill Says "Fetch the Relevant Ticket"

Read the GitHub issue by number, including comments and labels.
