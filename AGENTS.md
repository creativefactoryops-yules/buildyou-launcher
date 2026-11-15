<!-- digby:scaffold v1 -->

# Workspace

> **Agent guide — read first, kept lean on purpose.** Digby Code injects this file
> into every session, so it holds only what is SPECIFIC to this project. How Digby
> works (tools, permission modes, safety, subagents, memory) is built in — it is not
> repeated here. When you learn a durable project fact, update this file (or
> `.digby/notes/learnings.md`) instead of letting it die with the session.

## Overview

**--force ./BUILD YOU** — a real Android home-screen launcher (Kotlin + Jetpack Compose,
Material 3, dark-only). Declares `CATEGORY_HOME`, so it appears in the system
"Use as Home app" chooser; lists real installed apps, launches them, and runs a
terminal pane bound to real actions.
Stack: **Kotlin 2.0.21 · Compose BOM 2024.10.01 (Material 3) · AGP 8.13.2 · Gradle 8.14.3 · compileSdk/targetSdk 36 · minSdk 31 · Java 17**

## Environment

- `run_command` runs in an **isolated per-session container**, not the user's machine. The worktree
  is ephemeral: commit real progress to your branch before the session ends.
- You start on a **fresh branch off `main`** — no need to create one for normal work.
- There is no `git_branch`/`git_fetch`/`git_pull` tool: make a second branch with `git checkout -b`
  via `run_command` if you must, and don't try to sync a diverged `main` mid-session.
- Toolchain (installed under `$HOME/.local`, PATH via `$DIGBY_ENV`): JDK 17 (Temurin) at
  `~/.local/jdk17`, Android SDK at `~/.local/android-sdk` (platform-36, build-tools 36.0.0),
  Gradle 8.14.3 at `~/.local/gradle-dist`. `local.properties` points `sdk.dir` at the SDK.

## Commands

```
Install:   (none — wrapper + cached deps; first build downloads ~500 MB from Google Maven)
Dev/run:   ./gradlew --no-daemon assembleDebug   (no dev server; it's an Android app)
Typecheck: ./gradlew --no-daemon compileDebugKotlin
Lint:      ./gradlew --no-daemon lintDebug
Test:      ./gradlew --no-daemon testDebugUnitTest   (no tests yet)
Build:     ./gradlew --no-daemon assembleDebug  →  app/build/outputs/apk/debug/app-debug.apk
Verify APK: $ANDROID_HOME/build-tools/36.0.0/aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk
```

Use `--no-daemon`: daemons eat RAM and outlive the session in this container.

## Project Structure

- `app/src/main/java/force/buildyou/launcher/` — all Kotlin:
  `MainActivity` (single-activity, edge-to-edge, owns intents/state), `AppInventory`
  (PackageManager query + wallpaper), `AppDeck` (grid + long-press sheet),
  `BrandHeader` (date row, stamp ladder, TextMeasurer auto-shrink wordmark),
  `TerminalEngine`/`TerminalPane` (command line), `LauncherPrefs` (hidden + order).
- `app/src/main/AndroidManifest.xml` — HOME/DEFAULT intent filter, `singleTask`,
  `<queries>` for package visibility (NO `QUERY_ALL_PACKAGES`).
- `gradle/libs.versions.toml` — every version pin lives here.
- `.github/workflows/build.yml` — once activated (see README's **CI** section) this
  is the CI that builds and uploads the debug APK. It is **not currently on
  `main`**: the automation that first published this repo lacks the GitHub
  `workflow` scope, so the validated, secret-free workflow is parked, byte-identical,
  at `docs/ci-workflow.yml`.

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
