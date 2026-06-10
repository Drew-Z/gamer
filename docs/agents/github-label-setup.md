# GitHub Label Setup

This repo's agent workflow expects four workflow labels in addition to GitHub's default `wontfix` label.

## Required Labels

| Label | Description |
| --- | --- |
| `needs-triage` | Maintainer needs to evaluate this issue |
| `needs-info` | Waiting on reporter for more information |
| `ready-for-agent` | Fully specified, ready for an AFK agent |
| `ready-for-human` | Requires human implementation |

## Create With GitHub CLI

Install and authenticate `gh`, then run these commands from this repository:

```powershell
gh label create "needs-triage" --description "Maintainer needs to evaluate this issue" --color "ededed"
gh label create "needs-info" --description "Waiting on reporter for more information" --color "d876e3"
gh label create "ready-for-agent" --description "Fully specified, ready for an AFK agent" --color "0e8a16"
gh label create "ready-for-human" --description "Requires human implementation" --color "fbca04"
```

If a label already exists, update it instead:

```powershell
gh label edit "needs-triage" --description "Maintainer needs to evaluate this issue" --color "ededed"
gh label edit "needs-info" --description "Waiting on reporter for more information" --color "d876e3"
gh label edit "ready-for-agent" --description "Fully specified, ready for an AFK agent" --color "0e8a16"
gh label edit "ready-for-human" --description "Requires human implementation" --color "fbca04"
```

## Notes

- The current Windows workspace did not have `gh` on `PATH` when this file was written.
- Unauthenticated GitHub API reads can hit rate limits, so prefer authenticated `gh` for label setup.
- Keep `docs/agents/triage-labels.md` in sync if the remote label names change.
