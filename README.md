# snapshot-spec — reverse-engineering FHIR snapshot generation

## What we are doing

FHIR profiles are written as **differentials**: a list of the constraints an author adds on top of a base
definition. Tools need the **snapshot**: the fully computed element list with every inherited rule applied.
The FHIR specification defines the shape of both, but not the algorithm that turns one into the other. HL7 has
acknowledged this twice ([FHIR-9079](https://jira.hl7.org/browse/FHIR-9079) in 2016,
[FHIR-13402](https://jira.hl7.org/browse/FHIR-13402) in 2018), agreed both times that the rules should be written
down, and both times nobody did. In the meantime the two mainstream generators — the Java reference implementation
in `hapifhir/org.hl7.fhir.core` (used by the validator, the IG Publisher and SUSHI) and the Firely .NET SDK's
`SnapshotGenerator` (used by Simplifier and Forge) — each invented their own semantics, and in a long list of
places they invented different ones. The same profile yields different snapshots depending on the tool, and the
shared test suite's golden files encode whatever Java happened to do, bugs included.

This project writes the missing specification by reverse-engineering both implementations, side by side,
against the published spec text and the shared test cases. Three goals:

1. **A specification of snapshot generation as it is actually performed** — precise enough to reimplement from,
   with every rule cited to spec text, to code in both engines, or to a shared test case. This is the foundation
   for a fresh reimplementation of the SDK's `SnapshotGenerator`.
2. **A deviation register** — one entry per place where .NET and Java disagree, with the mechanism on both
   sides, a reproducing input, and what (if anything) the spec or HL7 has said about it.
3. **Decisions and spec fixes** — the questions the spec leaves open, taken to the people who can settle them
   (an HL7 WGM evening session in September 2026, Zulip, JIRA), and the errata and clarifications the spec text
   itself needs, written up as proposals for the FHIR R6 normative wrap-up.

Bugs found along the way in either engine are filed upstream as they are verified, never fixed in place: the old
code is the object of study, not the patient.

## How

- **Reference semantics: FHIR R5**, with R4/R4B deltas noted, and the R6 CI build tracked as the forward-looking
  source (several of our gaps are already being changed there).
- **Both implementations are read at pinned revisions** and every claim carries a `file:line` citation:
  .NET `Hl7.Fhir.R5` 6.2.1, Java `org.hl7.fhir.core` @ b06c7ee / validator_cli 6.10.2.
- **An empirical harness** runs both engines and the golden files over the 164 shared `fhir-test-cases`
  snapshot-generation tests, in the exact configuration the golden files were produced with. The Java oracle
  reproduces its golden files on 143/143 generation tests and 21/21 fail tests, so every .NET difference is
  real signal; the differences are then classified and mined into the register.
- **Prior discussion is swept before anything is asked:** chat.fhir.org (all public streams) and jira.hl7.org
  were searched per open question, every quote is tiered by evidence (resolution text vs comment vs chat
  agreement), and nothing already decided is asked again.
- **Work is done in packets** across many sessions with Claude Code; the session protocol and the running
  state live in the project memory, salvaged here under `project-notes/`.

## What comes out of it

The deliverable is the document set in **`docs/` of this repository** (it began life on the Firely .NET SDK's
`docs/snapshot-spec` branch and was moved here on 2026-09-09 with its history; the SDK will receive the
*results* — a reimplementation — later, not the study):

- **Chapters 1–12** — the algorithm: preprocessing, base resolution and rebasing, element matching, per-property
  merge, slicing, type-profile and extension expansion, contentReference, logical models, ids, recursion, errors
  and settings. Each chapter is layered *spec baseline → .NET behavior → Java behavior → deviations → open
  questions*, so implementation-derived rules are never presented as spec rules.
- **Chapter 13, the deviation register** (DEV-001…DEV-038), **chapter 14, open questions** (OQ-001…OQ-021, each
  with its JIRA/Zulip verdict), **chapter 15, spec RFCs** (RFC-001…RFC-019 for HL7).
- **The WGM brief** (`wgm-brief-2026-09.md`, frozen 2026-09-03): the ten live decisions, the confirmations of
  rulings that exist but that one engine ignores, and the evidence, for the September 2026 WGM.
- **Upstream issues:** hapifhir/org.hl7.fhir.core #2584–#2597 and #2602–#2605; FirelyTeam/firely-net-sdk #3583,
  #3587–#3591, #3597.

Where it stands (2026-09-09): phases 1–3 (spec baseline, .NET, Java) and the harness sweep are complete, the
brief is frozen, all pre-WGM carry-ins are closed. Next is the WGM itself and then folding its outcomes back into
chapters 13–15 and the JIRA/Zulip venues (FHIR-31405 is the live ticket for the merge rules).

## This repository

This repository holds the document set and the **irreplaceable** part of the study's working materials (about
12 MB). Everything regenerable is ignored (see `.gitignore`) and re-creatable from the pins below.

### Tracked

| path | what |
|---|---|
| `docs/` | **The document set:** `README.md` (status table, chapter list, methodology), chapters `01`–`12`, `13-deviation-register.md`, `14-open-questions.md`, `15-spec-rfcs.md`, `wgm-brief-2026-09.md`. Start with `docs/README.md`. |
| `extracts/` | Durable outputs of every deep-read, orientation map, harness mining pass and the Zulip/JIRA prior-discussion sweeps (2026-08-21 → 2026-09-03). The sweeps are the record; the raw API output is mostly gone. |
| `harness/` | The three-way test harness (Java oracle in JUnit-driver configuration, .NET runner, golden-file compare): `BatchRunner.java`, `JavaRunner.java`, `Compare.java`, `ProbeRunner.java`, `run_tests.py`, `report.py`, `runner-dotnet/` sources, `README.md` (configuration, pins, noise ledger). |
| `harness/repros/` | Standalone repro inputs and their outputs for the bugs filed upstream, plus the issue texts as filed. |
| `harness/out-backup-harness-settings-2026-09-03/` | The harness-settings .NET outputs of the four settings-sensitive tests, kept for the DEV-029 comparison. |
| `spec-html/` | Local copies of the spec pages the study cites. `R4/` and `R5/` are re-downloadable; **`R6-build/` is not** — it is the build.fhir.org snapshot of v6.0.0-ballot4 (generated 2026-08-18, fetched 2026-08-21) that every R6 claim and every RFC was checked against, and the CI build changes daily. |
| `upstream-java-issues.md` | Log of every Java finding: drafts, verification records, filing protocol, issue numbers. |
| `project-notes/` | Copies of the Claude Code plan and project memory that drive the multi-session protocol, with a README on resuming the project from them. |
| `tools/zulip-search.sh` | Read-only Zulip search helper (credentials come from `~/.zulip-fhir-key`, never from this repo). |
| `vendored-ids.txt`, `upstream-r5-ids.txt` | Manifest id lists behind the vendored-vs-upstream test comparison. |

### Ignored — how to regenerate

| path | regenerate |
|---|---|
| `org.hl7.fhir.core/` | `git clone --depth 1 --filter=blob:none --sparse https://github.com/hapifhir/org.hl7.fhir.core`; sparse cone: `org.hl7.fhir.r5/src/main/java/org/hl7/fhir/r5/conformance/profile`, `org.hl7.fhir.r5/src/test/java/org/hl7/fhir/r5/test`, `org.hl7.fhir.r5/src/test/resources/snapshot-generation`, `org.hl7.fhir.validation/src/main/java/org/hl7/fhir/validation/cli`. Code citations are at commit **b06c7ee** (master 2026-08-21); later masters checked: 4f52ba6 (2026-09-01), f725fb7 (2026-09-03). |
| `fhir-test-cases/` | sparse clone of https://github.com/FHIR/fhir-test-cases at **1.7.67 / 9f495e8**: `r5/snapshot-generation`, `r4b/snapshot-generation`, `r5/packages`. |
| `FHIR-Primer.wiki/` | `git clone https://github.com/chrisgrenz/FHIR-Primer.wiki.git` (Chris Grenz's 2017 snapshot write-ups). |
| `tools/validator_cli.jar` | validator_cli **6.10.2** (Git# d06577dbc5c6, built 2026-08-13) from the org.hl7.fhir.core GitHub releases. |
| `tools/pkgs/*.tgz` | hl7.terminology.r5 7.3.0, hl7.fhir.uv.extensions 5.2.0, hl7.fhir.uv.sdc 4.0.0, hl7.fhir.xver-extensions 0.1.0 from packages.fhir.org; import into `~/.fhir` as described in `harness/README.md`. |
| `harness/out/` | `python harness/run_tests.py --all --pkgs` (about 20 minutes). |
| `harness/runner-dotnet/bin,obj` | `dotnet build -c Release` in `harness/runner-dotnet/`. |

### Version stamps used throughout

`.NET = Hl7.Fhir.R5 6.2.1 | Java engine = validator_cli 6.10.2 (d06577dbc5c6), source citations @ b06c7ee | golden = fhir-test-cases 1.7.67 (9f495e8) | core = hl7.fhir.r5.core 5.0.0 | THO 7.3.0 | uv.extensions 5.2.0 | sdc 4.0.0 | xver 0.1.0 | R6 build = v6.0.0-ballot4 (2026-08-18)`.
