# Harness first runs — Phase 4 packet 1 (2026-08-26)

Versions: .NET = NuGet Hl7.Fhir.R5 **6.2.1** | Java engine = validator_cli **6.10.2 (d06577dbc5c6)**,
JUnit-driver config via `harness/JavaRunner.java` | golden = fhir-test-cases **1.7.67** |
core = hl7.fhir.r5.core 5.0.0 (same tgz as Java CI). Comparison = `equalsDeep`, narrative stripped.
Harness: `C:\Git\snapshot-spec-materials\harness\` (see its README for config provenance).

## Verdict table (milestone set)

| test | subject | java vs golden | net vs golden | note |
|---|---|---|---|---|
| t1 | no diff change | **EQUAL** | DIFF | property-level only |
| t2 | no change + valueset | **EQUAL** | DIFF | |
| t3 | cardinality change | **EQUAL** | DIFF | |
| ts-case1 | type slicing: do nothing | **EQUAL** | DIFF | |
| ts-case2 | renamed type slice (OQ-018 R4 form) | **EQUAL** | DIFF | **structurally identical** (ids/min/max/types/sliceNames all agree); only property noise |
| obs-2b | open type slicing + min on slice | **EQUAL** | DIFF | `value[x]` **min 0 (.NET) vs 1 (Java)** — see below |
| obs-2-1 | reslicing choice to closed | FAIL (harness gap) | DIFF | base = `urn:uuid:…` canonical of obs-2's *output* — cross-linked test, driver must stage obs-2-expected; next packet |
| logical1 | logical model define | **EQUAL** | DIFF | |

Java oracle reproducing golden on 7/7 runnable tests **validates the oracle end-to-end**
(config parity with the JUnit driver confirmed empirically).

## Findings (deviation-register candidates, to be classified into ch13)

1. **obs-2b `Observation.value[x]` min: .NET 0 vs Java 1.** Diff puts min on a slice of the open
   type slicing; Java raises/keeps the sliced root's min, .NET leaves 0. Directly the slice-min /
   §5.1.0.14 WGM question (connects to ch06 packet-5 finding: .NET resets slice-entry min via
   initSliceBase; Java PPP:802-810 slice min=0 reset has different conditions). Classify against
   input diff next packet; likely a headline WGM exhibit.
2. **Extension-slicing stamp:** .NET emits `slicing` (url discriminator, "Extensions are always
   sliced by (at least) url") on **every** `.extension`/`.modifierExtension` snapshot element;
   Java/golden only where extension slices actually exist. Everywhere in every test (5 elements
   in obs-2b/ts-case2). ch06 register entry.
3. **SD-level `mapping` declarations:** Java copies the base SD's mapping list onto the derived SD
   (MappingAssistant masterList/update); .NET does not copy SD-level mappings. (t1, obs-2b.)
4. **Element `mapping.map` merge:** Java concatenates same-identity maps into one comma-joined map
   (`"Entity, Role, or Act,Patient[classCode=PAT]"`); .NET keeps separate repeats — and t1 shows
   .NET emitting near-duplicates (`"Entity. Role, or Act"` + `"Entity, Role, or Act"`), the
   period/comma pair Java's R4-hack normalizes (PU:949-968). ch05 mapping semantics.
5. **Relative markdown links:** Java rebases spec-relative links in definition/comment/requirements
   markdown to absolute (`extensibility.html` → `http://hl7.org/fhir/R5/extensibility.html`;
   updateURLs/processRelativeUrls PU:2135/2179). .NET leaves them relative. ch03/updateURLs;
   probably "noise" class but a real behavioral gap in .NET.
6. **Constraint order:** same constraint sets, different order (dom-3/dom-4/dom-5 sequences differ).
   ch05 constraint-append order.
7. **Explicit `isSummary: false`:** golden carries explicit `false` where .NET omits the property.
   Property-presence semantics (primitive false vs absent); breaks equalsDeep.
8. **`snapshot-base-version` extension:** Java stamps `http://hl7.org/fhir/tools/StructureDefinition/`
   `snapshot-base-version` = 5.0.0 on the derived SD (PU:1091-1093). .NET has no equivalent. Noise class.

Positive agreements worth recording: element **sets, ids, and Base components identical** in every
compared test (obs-2b, ts-case2 checked element-by-element) — .NET/Java agree on structure, slice
ids and Base for these cases; the divergence lives in per-property merge behavior.

## Harness gaps (next packet)

- Cross-linked tests (obs-2-1 et al.): driver must detect a base URL matching another test's
  expected output and stage that `-expected` file as dep (Java driver caches outputs; getByUrl
  searches all tests' expected/included).
- Package fetches for `hl7.terminology.r5` / `hl7.fhir.uv.extensions` / xver fail on this network:
  engine HTTP client refuses NAT64-mapped addresses (`64:ff9b::…` "non-public"). Try
  `-Djava.net.preferIPv4Stack=true`, or pre-populate `~/.fhir/packages` via another tool. THO was
  NOT needed for the milestone tests (oracle still EQUAL), but extension-heavy tests will need
  uv.extensions.
- Performance: each JavaRunner/Compare invocation compiles the .java and loads the 37MB core
  (~2 min/test). For the full 165-test sweep: batch mode (one JVM, many tests) or precompiled
  class + shared context.
- Diff report: build a noise-filtered structural report (per-element property diff classified
  against the noise ledger) instead of raw line diffs.
