# Project notes — salvaged copies of the `.claude`-resident project state

The snapshot-spec project (see `C:\Git\firely-net-sdk\docs\snapshot-spec\`, branch `docs/snapshot-spec`) keeps
its session protocol, phase status, NEXT UP pointer, settled decisions and materials map in Claude Code's
per-project memory, and its original plan in Claude Code's plans folder. Both live under `%USERPROFILE%\.claude\`,
which is swept automatically when a project goes idle. This folder holds copies so the project can be resumed
without them.

| file | source (live copy) | what it is |
|---|---|---|
| `plan-2026-08-21-central-to-working-with-zazzy-river.md` | `~/.claude/plans/central-to-working-with-zazzy-river.md` | the original five-phase plan (2026-08-21) |
| `memory-snapshot-spec-project.md` | `~/.claude/projects/C--Git-firely-net-sdk/memory/snapshot-spec-project.md` | the project memory: session protocol, phase status, **NEXT UP**, decisions, materials map, carry-ins, authorizations |
| `memory-index-line.md` | one line of `~/.claude/projects/C--Git-firely-net-sdk/memory/MEMORY.md` | the index entry that makes Claude recall the memory file |

Also salvaged into `../extracts/jira-raw-2026-09-03/`: the raw JIRA REST JSON of the 2026-09-03 re-reads (16
tickets, with the `customfield_10618` resolution field). The 2026-09-02 sweep's per-ticket dumps had already been
cleared from that session's scratchpad (only empty folders remained), so `extracts/jira-sweep-2026-09-02.md` is
the sole record of that sweep — it was written to be exactly that.

## How to resume after a sweep

1. Copy `memory-snapshot-spec-project.md` back to `~/.claude/projects/C--Git-firely-net-sdk/memory/snapshot-spec-project.md`
   and append the line in `memory-index-line.md` to that folder's `MEMORY.md` (create the folder/file if missing).
2. Say "continue the snapshot spec project". The memory's session protocol takes it from there:
   memory → `docs/snapshot-spec/README.md` status table → NEXT UP.
3. If even that is gone: the repo `README.md` status table plus `extracts/` and `upstream-java-issues.md`
   contain everything factual; only the working conventions (one packet per session, restart advice, "never post
   to Zulip", filing authorizations) live in the memory file.

## Keeping this current

The memory protocol's session-end invariant (from 2026-09-09) includes re-copying the memory file here. If the
copy here is older than the live one, the live one wins.

Not salvaged, deliberately: the per-session `tool-results/` dumps under `~/.claude/projects/...` (raw tool
output; everything used from them is in `extracts/`), and the session scratchpads' copies of SDK source files
(regenerable from git). Credentials are not in `.claude` at all: the Zulip key is `~/.zulip-fhir-key`
(referenced by `tools/zulip-search.sh`), never committed; regenerate it when the project ends.

State at the time of the first copy (2026-09-09): WGM brief frozen (v3, 2026-09-03), all pre-WGM carry-ins
closed, Java issues #2584–#2597 and #2602–#2605 filed on hapifhir/org.hl7.fhir.core, .NET issues #3583,
#3587–#3591, #3597 filed on FirelyTeam/firely-net-sdk; next work is post-WGM adjudication.
