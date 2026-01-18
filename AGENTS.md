# AGENTS.md — Converge Android Guide

> Converge is a vision for **semantic governance**. We move from fragmented intent to unified, converged states through a deterministic alignment engine. Our mission is to provide a stable foundation for complex decision-making where human authority and AI agency coexist in a transparent, explainable ecosystem.

**For AI coding assistants (Claude, Gemini, Codex, Cursor, etc.)**

This repository contains the **Android mobile application** for Converge.

## Project Specifics

- **Kotlin & Compose**: Modern Android development with Jetpack Compose.
- **On-Device ML**: Uses TFLite for local action prediction.
- **gRPC Kotlin**: Bidirectional streaming for runtime interaction.

## Development Patterns

- **MVVM Architecture**: Strict separation of concerns between UI, ViewModel, and Domain.
- **State Management**: Use `StateFlow` and `collectAsStateWithLifecycle`.
- **UI Consistency**: Follow the Converge Design System (colors, typography).
- **Cross-Platform**: Adhere to the [android-CROSS_PLATFORM_CONTRACT.md](../converge-business/knowledgebase/archive/android-CROSS_PLATFORM_CONTRACT.md).

---

## Consolidated Documentation (converge-business)

- **Knowledgebase**: [converge-business/knowledgebase/](../converge-business/knowledgebase/consolidated/00_index.md)
- **Cross-Platform Contract**: [converge-business/knowledgebase/android-CROSS_PLATFORM_CONTRACT.md](../converge-business/knowledgebase/archive/android-CROSS_PLATFORM_CONTRACT.md)

## Version Control

Use **Jujutsu (jj)** instead of git for version control operations:

- `jj status` instead of `git status`
- `jj log` instead of `git log`  
- `jj diff` instead of `git diff`
- `jj commit -m "message"` instead of `git add . && git commit -m "message"`
- `jj git push` instead of `git push`
- `jj git fetch` instead of `git fetch`
- `jj undo` to undo the last operation

All repos have jj initialized in colocated mode (works alongside git).



## Issue Tracking

This project uses **bd (beads)** for issue tracking.
Run `bd prime` for workflow context, or install hooks (`bd hooks install`) for auto-injection.

**Quick reference:**
- `bd ready` - Find unblocked work
- `bd create "Title" --type task --priority 2` - Create issue
- `bd close <id>` - Complete work
- `bd sync` - Sync with git (run at session end)

For full workflow details: `bd prime`

## Landing the Plane (Session Completion)

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `jj git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
   ```bash
   jj git fetch && jj rebase -d main@origin
   bd sync
   jj git push
   jj status  # Working copy should be clean
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `jj git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds

## Beads Tips (Local Repo Workflow)

When using `bd` in this repo, note these gotchas:

### Creating Issues Locally

Always use `--repo .` to target this repo (otherwise issues may route to `~/.beads-planning`):

```bash
bd create "My task" --type task --repo .
bd create "My feature" --type feature --priority 2 --repo .
```

### After Fresh Clone

If `bd create` fails with "issue_prefix config is missing":

```bash
bd config set issue-prefix <repo-name>
# e.g., bd config set issue-prefix converge-analytics
```

### First Push with jj

Before first push, track the remote bookmark:

```bash
jj bookmark track main --remote=origin
jj git push --bookmark main
```

### Quick Reference

```bash
bd ready                    # See unblocked work
bd create "..." --repo .    # Create issue locally
bd close <id>              # Close completed work
bd sync                    # Sync to JSONL
jj commit -m "..."         # Commit changes
jj bookmark set main -r @- # Move main to parent
jj git push --bookmark main # Push
```
