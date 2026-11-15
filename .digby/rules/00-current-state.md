# Current state (always injected)

This file is injected into EVERY run's system prompt (all `.digby/rules/*.md`
files are — up to 6 files, 6,000 chars each). AGENTS.md is injected only up to
its first 8,000 characters, so anything the agent MUST see every run belongs
here, not deep in AGENTS.md.

Keep here (short, current, pruned — THIS FILE ONLY: replace superseded entries,
never append forever; results logs and trackers under results/ are the opposite —
append-only on main, archived under results/archive/, never truncated):
- The project's CURRENT phase/goals and active constraints.
- The CURRENT work queue, next step and of-record numbers for scheduled lanes —
  this file is the cross-run state channel (injected every run). A results log
  that must be re-landed on main every firing is not: a results/tracker append
  is a RECORD, not a change — batch appends on the session branch, one PR per
  day or with the next code change. The supervisors read THIS file from the
  default branch (a mirror of main, refreshed within 30 min) — a PR touching
  only `.digby/**` is state-only and exempt from that rule: land it whenever
  the queue / next step changes. Of-record numbers ALSO go to the goal's
  premise register (goal_premise_set) — the supervisors read movement from
  there, never from PR ids.
- Standing directives for scheduled/monitor lanes (reference this file from the
  schedule prompt instead of rewriting the prompt).
- "Do not touch" areas and pointers to deeper docs.

<!-- Nothing here yet — the agent or the owner fills this in as the project takes shape. -->
