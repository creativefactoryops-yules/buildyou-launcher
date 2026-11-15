<!-- digby:scaffold v1 -->

# Workspace

> **Agent guide — read first, kept lean on purpose.** Digby Code injects this file
> into every session, so it holds only what is SPECIFIC to this project. How Digby
> works (tools, permission modes, safety, subagents, memory) is built in — it is not
> repeated here. When you learn a durable project fact, update this file (or
> `.digby/notes/learnings.md`) instead of letting it die with the session.

## Overview

A new project scaffolded in Digby Code. **Stack: to be decided** — fill in the one-line "what/why", the Stack, and the Commands below once the shape is chosen.
Stack: **to be decided**  <!-- language + framework with versions -->

## Environment

- `run_command` runs in an **isolated per-session container**, not the user's machine. The worktree
  is ephemeral: commit real progress to your branch before the session ends.
- You start on a **fresh branch off `main`** — no need to create one for normal work.
- There is no `git_branch`/`git_fetch`/`git_pull` tool: make a second branch with `git checkout -b`
  via `run_command` if you must, and don't try to sync a diverged `main` mid-session.

## Commands

No manifest yet — fill these in (and keep them current) as soon as the stack exists.

```
Install:   <e.g. npm install / uv sync / cargo fetch>
Dev/run:   <e.g. npm run dev>
Typecheck: <e.g. npx tsc --noEmit>
Lint:      <e.g. npm run lint>
Test:      <e.g. npm test>
Build:     <e.g. npm run build>
```

Never assume a library is installed — `grep` the manifest or a neighbouring import first.

## Project Structure

- _(add the 3–5 directories that matter, one line each — not a full tree)_

Discovery/secret exclusions live in `.digbyignore`; the machine-readable command list is `.digby/config.json`.

`.digby/config.json` may also carry an optional **`hooks`** section — the repo's own
deterministic rules around tool calls, run by the harness with no model judgment:
`postEdit` hooks run AFTER an edit to a matching path (`{file}` = the path, shell-quoted)
and their outcome is appended to the tool result as a `[post-edit hook] … → ok | FAILED`
line — they never block and the file is already on disk, so a formatter can reshape it;
`preCommand` hooks match `run_command` BEFORE it runs and refuse it with this repo's own
message. A starter `postEdit` section is scaffolded when this repo declares a formatter or
linter it can run locally — tune it, or delete it, in the config. Rules live with the repo,
never in the platform: with no `hooks` section nothing changes. Full shape:
`docs/DIGBY_CODE_HARNESS.md`; implementation `coding/repo-hooks.ts`.

## Conventions

- Match the surrounding code style; read a neighbouring file before adding a new one.
- Make focused changes that match surrounding code; prefer editing over rewriting.

## Docs are part of the change

When a change alters behaviour, an API/CLI surface, configuration, setup steps, or an operational
procedure, update the documentation that describes it — README, docs/, this file, `.env.example` —
in the SAME commit/PR. Before finishing, grep the repo's markdown for the symbols/commands/paths you
changed and fix every claim your change invalidated; a doc you cannot confidently fix gets a dated
`> ⚠ STALE (YYYY-MM-DD): …` banner instead of silence. Pure-internal refactors with no observable
change need no doc edits — say so explicitly.

A results/tracker/journal APPEND is a RECORD, not a change — do not open a PR for it alone: keep
appending on the session branch and open ONE PR per day (or when a code/script change lands).
Results logs and trackers are APPEND-ONLY on main: archive (`results/archive/<name>-<YYYY-MM-DD>.md`),
never truncate; roll a log over into a new dated file when it passes ~200 KB. The CURRENT queue /
next step / of-record numbers live in `.digby/rules/00-current-state.md` (injected every run); the
durable record lives in `results/`. The supervisors read that state file from the DEFAULT branch (a
mirror of main, refreshed within 30 min), not from a session branch — a state-only PR (only
`.digby/**` files) is exempt from the one-PR-per-day rule: land it whenever the queue / next step
changes. Of-record NUMBERS also go to the goal's premise register (`goal_premise_set`) — that, not a
PR, is how movement is seen upstream.

## Done means verified

"Looks done" is not done. Before you call a task complete, run the project's typecheck/lint/test commands (see above) and show the command +
its output as evidence; fix failures and re-run until green. For a behaviour change, also drive the
changed flow and observe it. If there is no runtime surface to drive (pure refactor), say so rather
than fabricating an observation.

### Machine-run finals — claims block

A machine-triggered run (schedule / hook / auto-resume) that pushed, opened a PR, deployed,
updated tickets, or sent messages ENDS its final answer with a typed claims block — the
harness verifies each entry against its own tool receipts (receipt lookup, not prose parsing):

```claims
[{"type":"push","ref":"1539fa86","note":"branch digby/fix-x"},{"type":"pr","ref":"241"}]
```

Types: `push` `pr` `merge` `deploy` `commit` `ticket_update` `message_sent` `dispatch` `file_change` `verify` `schedule_armed` `email_sent` `state_update` `probe` `benchmark` — the same type set the finish_run tool documents. A `commit` entry is a LOCAL commit (sha ref, vouched by a git_commit receipt, never a push); a `schedule_armed` entry cites a schedule id; an `email_sent`/`state_update` entry cites the send / state action.
`ref` is the receipt-visible id of what you actually did: the pushed sha or branch, the PR
number/URL from the open_pull_request result, the ticket id, the changed path, or a receipt id
an envelope returned. A `verify` entry is vouched ONLY by a this-run `verify`-tool receipt (a
passed chain) — checks executed via `run_command` cannot vouch it, so report those as prose with
the quoted output line, never as a `verify` entry. An entry may add optional
`"scope":"session"` (work an earlier run of THIS chat did) or
`"scope":"relayed"` (a receipt you read with get_run_receipts this run) — a
scoped entry that matched no this-run receipt is re-checked against the chat's
recent receipt history instead of being struck. Claim ONLY what a tool call in THIS run
performed — an entry with no matching receipt is marked UNVERIFIED, while a
fully-verified block vouches for the matching prose. No consequential actions this run → no block.

## Project boundaries

_(Repo-specific off-limits only — generic safety is built in.)_
- Ask before touching: _(e.g. `auth/`, `payments/`, `prisma/migrations/`, any `.env`)_

## More context

Loads on demand, not injected here: `.digby/notes/learnings.md` (durable learned conventions) ·
`.digby/rules/*.md` (path-scoped) · `.digby/docs/*.md` (deep reference — `read_file` when relevant).
