# snapshot-spec-materials

Working materials for the FHIR **snapshot-generation reverse-engineering study** whose deliverable lives in the
Firely .NET SDK repository under `docs/snapshot-spec/` (branch `docs/snapshot-spec`): a chapter-by-chapter
specification of snapshot generation as the .NET SDK and the Java reference implementation actually perform it,
a deviation register, open questions, spec-change proposals and the September 2026 WGM brief.

This repository holds the **irreplaceable** part of the study's materials (about 10 MB). Everything regenerable is
ignored (see `.gitignore`) and re-creatable from the pins below.

## Tracked

| path | what |
|---|---|
| `extracts/` | Durable outputs of every deep-read, orientation map, harness mining pass and the Zulip/JIRA prior-discussion sweeps (2026-08-21 → 2026-09-03). The sweeps are the record; the raw API output is mostly gone. |
| `harness/` | The three-way test harness (Java oracle in JUnit-driver configuration, .NET runner, golden-file compare): `BatchRunner.java`, `JavaRunner.java`, `Compare.java`, `ProbeRunner.java`, `run_tests.py`, `report.py`, `runner-dotnet/` sources, `README.md` (configuration, pins, noise ledger). |
| `harness/repros/` | Standalone repro inputs and their outputs for the bugs filed upstream, plus the issue texts as filed. |
| `harness/out-backup-harness-settings-2026-09-03/` | The harness-settings .NET outputs of the four settings-sensitive tests, kept for the DEV-029 comparison. |
| `spec-html/` | Local copies of the spec pages the study cites. `R4/` and `R5/` are re-downloadable; **`R6-build/` is not** — it is the build.fhir.org snapshot of v6.0.0-ballot4 (generated 2026-08-18, fetched 2026-08-21) that every R6 claim and every RFC was checked against, and the CI build changes daily. |
| `upstream-java-issues.md` | Log of every Java finding: drafts, verification records, filing protocol, issue numbers (hapifhir/org.hl7.fhir.core #2584–#2597, #2602–#2605). |
| `project-notes/` | Copies of the Claude Code plan and project memory that drive the multi-session protocol, with a README on resuming the project from them. |
| `tools/zulip-search.sh` | Read-only Zulip search helper (credentials come from `~/.zulip-fhir-key`, never from this repo). |
| `vendored-ids.txt`, `upstream-r5-ids.txt` | Manifest id lists behind the vendored-vs-upstream test comparison. |

## Ignored — how to regenerate

| path | regenerate |
|---|---|
| `org.hl7.fhir.core/` | `git clone --depth 1 --filter=blob:none --sparse https://github.com/hapifhir/org.hl7.fhir.core`; sparse cone: `org.hl7.fhir.r5/src/main/java/org/hl7/fhir/r5/conformance/profile`, `org.hl7.fhir.r5/src/test/java/org/hl7/fhir/r5/test`, `org.hl7.fhir.r5/src/test/resources/snapshot-generation`, `org.hl7.fhir.validation/src/main/java/org/hl7/fhir/validation/cli`. Code citations are at commit **b06c7ee** (master 2026-08-21); later masters checked: 4f52ba6 (2026-09-01), f725fb7 (2026-09-03). |
| `fhir-test-cases/` | sparse clone of https://github.com/FHIR/fhir-test-cases at **1.7.67 / 9f495e8**: `r5/snapshot-generation`, `r4b/snapshot-generation`, `r5/packages`. |
| `FHIR-Primer.wiki/` | `git clone https://github.com/chrisgrenz/FHIR-Primer.wiki.git` (Chris Grenz's 2017 snapshot write-ups). |
| `tools/validator_cli.jar` | validator_cli **6.10.2** (Git# d06577dbc5c6, built 2026-08-13) from the org.hl7.fhir.core GitHub releases. |
| `tools/pkgs/*.tgz` | hl7.terminology.r5 7.3.0, hl7.fhir.uv.extensions 5.2.0, hl7.fhir.uv.sdc 4.0.0, hl7.fhir.xver-extensions 0.1.0 from packages.fhir.org; import into `~/.fhir` as described in `harness/README.md`. |
| `harness/out/` | `python harness/run_tests.py --all --pkgs` (about 20 minutes). |
| `harness/runner-dotnet/bin,obj` | `dotnet build -c Release` in `harness/runner-dotnet/`. |

## Version stamps used throughout

`.NET = Hl7.Fhir.R5 6.2.1 | Java engine = validator_cli 6.10.2 (d06577dbc5c6), source citations @ b06c7ee | golden = fhir-test-cases 1.7.67 (9f495e8) | core = hl7.fhir.r5.core 5.0.0 | THO 7.3.0 | uv.extensions 5.2.0 | sdc 4.0.0 | xver 0.1.0 | R6 build = v6.0.0-ballot4 (2026-08-18)`.
