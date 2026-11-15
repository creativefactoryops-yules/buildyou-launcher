# .digby/

Digby Code's per-project framework directory. Digby scaffolds it on project create.

- `config.json` — machine-readable commands + verify chain (source of truth).
- `notes/learnings.md` — durable, git-committed learned conventions (self-improving).
- `rules/*.md` — path/topic-scoped rules, loaded on demand (add as needed).
- `docs/*.md` — deep reference (architecture, testing, db), read on demand.

The lean always-on guide is the root `AGENTS.md`. Invariant Digby doctrine (tools,
permission modes, safety) is built into the agent, not stored here.
