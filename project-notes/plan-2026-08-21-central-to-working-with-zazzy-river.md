# Reverse-Engineering the Snapshot Generation Algorithm

## Context

The SDK's snapshot generator (`src/Hl7.Fhir.Conformance/Specification/Snapshot/`, ~4,800 lines + a drifted 3,800-line STU3 fork) has been patched for a decade and become a liability: dense special-casing, ~200 dated inline design notes (2016–2025), nine self-flagged `WRONG` comments, compiled-in feature toggles, and an acknowledged unresolved diamond-problem for cardinality merging. The HL7 spec (profiling.html, conformance-rules.html, elementdefinition.html) documents only half the actual algorithm; the rest lives in this code, in the Java reference implementation (`hapifhir/org.hl7.fhir.core`), in shared test cases, and in years of Zulip conversations.

**Goal:** reverse-engineer a complete specification document of snapshot generation — including a register of deviations between the .NET and Java implementations — as the foundation for a fresh implementation later. Timeline: months; this plan covers the document phase only, not the rewrite.

**Deadline (added 2026-08-21):** Ewout attends an HL7 WGM around 2026-09-21 (one month out) with direct access to the relevant people (Grahame Grieve et al.). **The open-questions list must be ready by then** — a self-contained brief usable in a live evening session (Claude possibly invoked during it). This re-prioritizes the phases: deviation discovery and question harvesting come first; chapter prose can mature after the WGM. Target: questions brief frozen ~1 week before travel (~2026-09-14).

**Settled decisions (user-confirmed 2026-08-21):**
- Reference semantics: **R5**, with explicit R4/R4B delta notes. STU3 fork out of scope.
- Document lives in **this repo** under `docs/snapshot-spec/` (multi-chapter markdown). Internal tone is fine; can be shared later.
- Zulip history access: **personal API key** (verified working against `chat.fhir.org/api/v1/messages`, conformance stream id 179177).
- Empirical Java oracle (validator_cli snapshot mode diffed against our generator): **build early**, use throughout.

## Materials — verified access status

| Source | Status | How |
|---|---|---|
| .NET generator + tests | ✅ explored in depth | this repo (inventory below) |
| HL7 spec pages | ✅ fetchable, but large pages truncate via web summarizer | **must download raw HTML locally** in Phase 0 and read directly |
| Java implementation | ✅ 10 files, ~380 KB in `org.hl7.fhir.r5/.../conformance/profile/` | sparse-clone locally in Phase 0 |
| fhir-test-cases (r4b/r5 snapshot-generation) | ✅ | clone locally in Phase 0 |
| Zulip #conformance | ✅ verified via API key | key currently only in chat transcript — store in local file outside repo (e.g. `%USERPROFILE%\.zulip-fhir-key`); recommend user regenerates key after project or once stored. **Read-only use only; never post.** |
| GitHub issues (#827, #1052, #1252–#1256, #2466, #3177…) | ✅ | `gh issue view` |

## Key facts from exploration (do not re-derive)

**.NET architecture:** `SnapshotGenerator.cs` (2,193 ln, main loop: `generate()` → `merge()` → `mergeElement()` recursion), `ElementMatcher.cs` (1,043 ln, diff↔base child matching → `Merge/Add/Slice/New/Remove/Invalid`), `ElementDefnMerger.cs` (828 ln, per-property override/append/cumulative semantics), `DifferentialTreeConstructor.cs` (sparse-diff → full tree), `SnapshotBaseComponentGenerator.cs`, plus navigation layer (`ElementDefinitionNavigator` + extensions) and infra in `Hl7.Fhir.Shims.Base` (recursion stack, element-id generator, annotations, events, 6 settings flags).

**Notable .NET quirks to document:** type-profiles-win-over-base ordering (SnapshotGenerator.cs:999), the canonical 3-step merge order (1383), `CACHE_ROOT_ELEMDEF` legitimate-recursion cache, type-list replace-then-item-merge (#827), min=max(both)/max=most-constrained cardinality, suppress-extension deletion, unresolved cardinality diamond problem (ElementDefnMerger.cs:689), `mergeTypeProfiles` (~310 ln, densest region).

**Java architecture:** `ProfileUtilities.java` (223 KB), `ProfilePathProcessor.java` (105 KB, the refactored path-walking core), `SnapshotGenerationPreProcessor.java` (51 KB), 7 small support classes. Java is one R5-based codebase serving all FHIR versions via conversion — some apparent deviations will be version-normalization artifacts, not algorithm differences.

**Test landscape:** shared test source compiled into R4/R4B/R5 test projects (~155 tests in `SnapshotGeneratorTest.cs`, ~26 matcher tests). The HL7/Java shared suite lives (misleadingly) in `src/Hl7.Fhir.Specification.Shared.Tests/TestData/snapshot-test/Type Slicing/` — 64 manifest entries, of which **8 commented out, ~10 `[Ignore]`d against issues #1252–#1256, and 5 fixtures + 5 rules patched at runtime** by `FixInput()`/`FixManifest()` in `SnapshotGeneratorManifestTests.cs`. Verification is FHIRPath-rule-based; the checked-in `-expected.xml` files are **never structurally diffed** — no golden-file comparison exists anywhere. Upstream fhir-test-cases has grown past our vendored 64 entries.

## Phases

### Phase 0 — Materials acquisition (≈1 session)
Working area: a new top-level `docs/snapshot-spec/` for the document; local clones/downloads under a scratch/tools dir (not committed).
1. Sparse-clone `hapifhir/org.hl7.fhir.core`: `org.hl7.fhir.r5/src/main/java/org/hl7/fhir/r5/conformance/profile/` **plus its test driver** `SnapshotGenerationTests.java` (defines semantics of manifest `gen`/`sort`/`register`/`include` attributes) and the Java-side test resources.
2. Clone `FHIR/fhir-test-cases`; diff upstream r5+r4b `snapshot-generation/manifest.xml` against our vendored copy → early cheap deliverable: "what we never integrated".
3. Download raw HTML of R5 profiling.html, conformance-rules.html, elementdefinition.html (esp. `#interpretation` — "Interpretation of ElementDefinition in Different Contexts", the most load-bearing spec text), structuredefinition.html, extensibility.html; same pages for R4 where deltas matter. Never rely on web-fetch summaries.
4. Store Zulip API key in `%USERPROFILE%\.zulip-fhir-key`; write a small query script (stream=conformance + search narrows). Search confluence.hl7.org and community blogs (Health Intersections, fhirblog.com) for snapshot-generation write-ups — treat as a search task, not assumed sources.
5. Verify validator_cli snapshot-generation invocation against current docs (JRE prerequisite check) — groundwork for the oracle harness.
6. Write project memory files so future sessions don't re-derive today's inventory.

### Phase 1 — Document skeleton + spec baseline (≈1–2 sessions)
Create `docs/snapshot-spec/` chapters mapped to algorithm areas:
1. Overview & terminology; inputs/outputs; constraint vs specialization
2. Differential preprocessing (sparse-path tree construction; Java pre-processor comparison)
3. Base resolution, rebasing, root handling (incl. logical models)
4. Element matching (paths, choice-type renames, order rules)
5. Per-property merge semantics — **a table per ElementDefinition property**: override / append (`...`) / cumulative / never-merged / special (types, cardinality, bindings, constraints, mappings, suppression)
6. Slicing (named, type, extension, reslicing, slicing-entry synthesis, ordering rules, closed/openAtEnd)
7. Type-profile & extension expansion (merge order, complex-extension `url#element` refs, url fixups)
8. contentReference handling
9. Logical models & interfaces (`structuredefinition-interface`)
10. Element IDs & Base component generation
11. Recursion & circularity guards
12. Error handling philosophy (report vs throw) and settings/configuration surface
13. **Deviation register** (structured: id, area, .NET behavior, Java behavior, spec basis, *version-normalization artifact?*, reproducing input, status/resolution)
14. Open questions log (feeds Zulip/Grahame consultation)
15. **Spec RFCs** (added 2026-08-21): proposed FHIR-spec changes — errata/clarifications/additions to the *shape* rules — written for the standardization audience, to be posted on HL7 Confluence/JIRA for the ANSI process; timely for the R6 normative wrap-up. Settled open questions graduate into RFCs. Re-verify every entry against R6 ballot text before posting.

Fill each chapter with what the official spec pages actually say (the baseline), so implementation-derived rules are clearly marked as such.

### Phase 2 — .NET deep-read (≈several sessions)
Fill chapters from the .NET code with `file:line` citations. The merger property table, matcher decision table, and slicing state machine come first — they're the heart. Mine the ~200 dated inline comments and `#define` toggles for design rationale; record each "WRONG"/TODO/issue-reference as either a rule, a known bug, or an open question.

### Phase 3 — Java deep-read + deviation register (≈several sessions)
Same chapters, from `ProfileUtilities`/`ProfilePathProcessor`/`SnapshotGenerationPreProcessor`. **Seed the register before reading**: known divergences from issues #1252–#1256 (ignored obs-* tests), #3177, #2466, #827, #1052, plus every `FixInput`/`FixManifest` runtime patch (each one documents "Java fixture we considered wrong"). Flag version-normalization artifacts separately.

### Phase 4 — Test-case cross-check + oracle harness (≈2–3 sessions, interleaved with 2–3)
1. Build the empirical harness: run validator_cli snapshot mode and our `SnapshotGenerator` on the same inputs; canonical-form diff of outputs (this also prototypes the golden-file comparator the current tests lack).
2. Sweep upstream fhir-test-cases through both; every mismatch → register entry with reproducing input.
3. Re-examine the ~18 disabled/patched manifest tests with the harness.

### Phase 5 — Adjudication (ongoing, gated on register substance)
Per deviation: spec basis → proposed resolution → where unresolvable, a drafted question. Search Zulip history first (API); remaining questions go into the **WGM question brief** (below) and/or are drafted for Ewout to post on #conformance (I draft, user posts — I never post). Output: settled semantics per deviation, the direct input to the new-implementation design.

## WGM question brief (hard deadline ~2026-09-14)

The first concrete deliverable: a self-contained brief of open questions for the HL7 WGM (~2026-09-21), to be used in a live evening session with the Java/spec people — possibly with Claude invoked during the session.

**Consequences for pacing (supersedes the phase-order estimates above):**
- Phases 2–4 run question-first: each session's output is measured in register entries and open questions, not finished prose. Chapter polish is explicitly allowed to slip past the WGM.
- Pull Phase 4's oracle harness forward — it is the best question generator (mismatches on the 102 never-integrated upstream tests are exactly the concrete, demo-able cases to put in front of Grahame).
- Before freezing the brief: sweep Zulip/JIRA per question so nothing already-answered is asked.

**Brief format:** per question — one-paragraph context, the concrete reproducing input (or test id from fhir-test-cases), what .NET does, what Java does, what the spec says (or doesn't), and the specific decision needed. Ordered by importance to the new implementation. Portable (single markdown file, printable), self-contained so it works without this repo or conversation at hand.

## Verification
- Every documented rule cites at least one of: code (ideally both implementations), a test case, or spec text. Rules with only single-implementation evidence are marked as such.
- Every register entry should carry a concrete reproducing input where feasible; harness output (both snapshots) archived next to it.
- Document builds nothing, so "tests" = the harness runs + internal consistency: chapter cross-references resolve, no orphan open-questions.

## Out of scope (explicit)
- The STU3 fork (frozen; note its existence in ch. 1, nothing more).
- Writing the new generator (later project; this document is its input).
- Fixing bugs found along the way — file issues / register entries instead (avoid shotgun surgery in the old code).
