# Full-suite harness sweep — Phase 4 packet 2 (2026-08-26)

Versions: .NET = NuGet Hl7.Fhir.R5 **6.2.1** (+ FhirPackageSource package parity) | Java engine =
validator_cli **6.10.2 (d06577dbc5c6)** in JUnit-driver config via `harness/BatchRunner.java` |
golden = fhir-test-cases **1.7.67** | core = hl7.fhir.r5.core 5.0.0 | THO 7.3.0 |
uv.extensions 5.2.0 | sdc 4.0.0 | xver 0.1.0. Comparison = `equalsDeep`, narrative stripped.
All artifacts in `harness/out/` (summary.tsv, report.md, per-test norm.json triples).

## Oracle health: PERFECT

**Java-vs-golden EQUAL on all 143 gen/sort tests (143/143); all 21 fail tests JUnit-pass
(15 THREW + 6 ERRORS).** Verified fully clean against both secondary JUnit gates too: zero
gen/sort tests carry ERROR-level messages (`jerr=0` everywhere — the :677 message gate that fails
a JUnit test even when the golden matches), and zero fail-test `regex` mismatches. The oracle (one shared-context JVM, manifest order, faithful
testGen/testSort/register/getSD replication, cache-backed packages) reproduces the golden corpus
bit-for-bit (modulo narrative). Every `.NET vs golden` difference below is therefore a real
behavioral difference against current Java, not harness noise.

Two harness lessons that cost a sweep each (details in harness/README):
1. **ANSI logger noise on stdout** breaks RESULT-line parsing → results now go to a dedicated file.
2. **`NpmPackage.fromPackage(stream)` ⇒ eager load branch ⇒ `TestPackageLoader.loadBundle()` stub
   silently drops every resource** while reporting hundreds "loaded" (packages appeared loaded, all
   their SDs unresolvable). Cache-backed (`~/.fhir`) packages take the lazy `.index.json` branch CI
   uses. Symptom to remember: "Loaded hl7.fhir.uv.extensions#5.2.0" + "Can't find Extension
   definition for ...patient-birthTime".

## Verdicts

- gen/sort tests (143): java EQUAL **143**; net DIFF **141**, net MISSING **2** (.NET threw):
  - `ext-recursion-2`, `logical-goo`: `NotSupportedException: Recursive profile dependency
    detected` — .NET's one-URI recursion guard (ch11) refuses recursive extension / mutually
    recursive logical-model structures that Java generates (and golden blesses). Register
    candidates (ch11).
- net-vs-golden EQUAL: **0** — no .NET output matches golden anywhere (pervasive noise classes
  below guarantee equalsDeep failure even where semantics agree).
- fail tests (21): java 15 THREW + 6 ERRORS (all JUnit-pass; t29b regex-ok). .NET side:

  | .NET outcome | count | ids |
  |---|---|---|
  | THREW | 4 | t37, obs-1-2, obs-2-3, obs-3 |
  | ERRORS (generated, error msgs) | 4 | t15a, t29b, ihe1, sushi3 |
  | **NO-THROW (silently accepts)** | **13** | t23a, t29a, t43a, obs-5, obs-badfixed, obs-badpattern, ihe2, obs-unit, ext-recursion-1, simplifier-1, obs-ms-bad, ext-ccuk, mi-use-distinct |

  .NET silently accepts 13 of the 21 profiles Java rejects — aggregate exhibit for OQ-014
  (error taxonomy). Per-test causes to be mined in the fail-test classification pass.

## Property-level classification (.NET vs Java, report.py, keyed-list alignment)

| class | count | status |
|---|---|---|
| ELEM-MAPPING | 29023 | known (DEV-017 + comma-join, harness-first-runs #4) |
| NEW (unclassified) | 7974 | review queue, aggregation below |
| FALSE-VS-ABSENT | 5632 | known (#7) |
| EXT-SLICING-STAMP | 3151 | known (#2) |
| TYPE-CONSTRAINT (.NET adds datatype constraints, source=datatype url) | 3099 | packet-2 find, ch05 register candidate |
| SD-MAPPING | 1728 | known (#3, OQ-007) |
| REL-LINKS | 1233 | known (#5) |
| ELEMENT-SET (element on one side only) | 556 | 450 java-only / 106 net-only, mine next packet |
| CONSTRAINT-ORDER | 160 | known (#6) |
| MAPPING-ORDER | 11 | order-only |

## NEW-bucket aggregation (top patterns; ~all mechanically recognizable)

| count | property (side) | suspected mechanism |
|---|---|---|
| 2410 | comment (.NET-only) | .NET enriches elements with the **datatype root's** comment; Java/golden don't (same family: TYPE-CONSTRAINT) |
| 740 | alias (.NET-only) | same datatype-root enrichment |
| 271+265+146+91 | binding.extension.*, extension.* (.NET-only) | .NET copies bindingName/other extensions from datatype defs |
| 435 | type entries (.NET-only) | .NET keeps/restores types Java prunes (contentReference #3177 restoration + DEV-020 collapse territory) |
| 191×5 | constraint.* (Java-only) | ~191 constraints present only in Java — ele-1 re-stamped on re-expanded contentReference children (cf. obs-2-1) |
| 323/268/44 | definition/short/comment (both) | wording differs (enrichment source differs per side) |
| 273 | type.extension.valueUrl (both) | fhir-type compiler-magic extension differences |
| 78 | contentReference (both) | Java absolute `url#path` vs .NET local `#path` (ch08, ensureAbsoluteContentReferences scope) |
| 61 | mustSupport (both) | genuine merge difference, mine next packet |
| 52 | min (both) | genuine cardinality differences beyond obs-2b (includes DEV-020 entry-min mechanic) |

## Headline finding classified this packet: DEV-020 / OQ-020 (obs-2b + family)

Type-slicing entry normalization. Java conditionally rewrites the author's slicing entry;
.NET merges it as written. The obs-2 family maps Java's gradient (all inputs have entry
`rules=open`):

| test | differential delta | golden/Java entry outcome | .NET entry outcome |
|---|---|---|---|
| obs-2 | slice typed CC | 13 types, `open`, `type:$this` injected | 13 types, `open`, no discriminator |
| obs-2a | entry also types CC | [CC] (authored), **closed** | [CC] (authored), `open` |
| obs-2b | slice CC + **min=1** | **[CC] collapsed, closed, min 0→1** | 13 types, `open`, min 0 |

Full analysis with spec basis in `docs/snapshot-spec/13-deviation-register.md` DEV-020 and
`14-open-questions.md` OQ-020 (prime WGM material). OQ-018 got its renamed-form data point:
ts-case2 structurally identical both sides.

## Next-packet queue

1. Classify the fail-test divergences (13 silent .NET accepts) — per-test root causes.
2. Mine ELEMENT-SET (450 java-only / 106 net-only elements) — where do element sets diverge?
3. Register entries for: datatype-root enrichment family (comment/alias/binding-ext/TYPE-CONSTRAINT
   — likely ONE .NET mechanism), Java-only ele-1 on contentReference re-expansion, contentReference
   absolute-vs-local, mustSupport diffs, the 52 min diffs, ext-recursion-2/logical-goo recursion
   refusals (ch11).
4. Phase 3 packet J-a (ch06 Java deep-read) now has concrete anchors: DEV-020 trigger conditions
   (PPP L597/L1587, L802-810, checkToSeeIfSlicingExists :955-987).
