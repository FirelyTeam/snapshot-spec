# ELEMENT-SET mining: one-side-only snapshot elements (.NET vs Java)

.NET = Hl7.Fhir.R5 6.2.1 | Java engine = 6.10.2 (d06577dbc5c6) | golden = fhir-test-cases 1.7.67

Source: per-test `harness/out/<id>/{java,dotnet}.norm.json`, aligned with the exact
`elem_key()` from `harness/report.py` (`id`, falling back to `path[:sliceName]`), same
test selection as the classifier (skip `mode=fail*`, skip tests missing a norm file).
Recount script: session scratchpad `elemset.py` / `groups.py` (throwaway).

On **all 23 contributing tests java == golden** (`summary.tsv` java column = EQUAL), so
every one-side-only element below is a **.NET-side deviation from expected output** —
java-only = element .NET should have emitted; net-only = element .NET emitted that the
golden snapshot does not contain.

## 1. Recount reconciliation

| | claimed (report.py) | recount | match |
|---|---|---|---|
| total ELEMENT-SET | 556 | **556** | exact |
| java-only | ≈450 | **450** | exact |
| net-only | ≈106 | **106** | exact |

Exclusions (identical to report.py's): 21 tests with `mode=fail*` skipped; **ext-recursion-2**
and **logical-goo** skipped because `dotnet.norm.json` is missing (.NET refused those runs
outright — they contribute 0 to the 556 but are deviations in their own right, tracked
elsewhere). 23 tests contribute elements.

**Adjusted engine-level count.** Two whole-test buckets are harness/production artifacts,
not element-level merge divergence:

- **xt-logical** (57 java-only): .NET snapshot generation *failed* (`dotnet.log`: "Unable to
  resolve reference to profile 'http://www.xt-ehr.eu/fhir/models/StructureDefinition/EHDSDataSet'
  … Snapshot generation failed for 'EHDSDocument'") and the harness wrote a norm.json with an
  **empty snapshot** (0 elements vs java/golden 57).
- **t26** (52 net-only): a `sort`-mode test — golden and java outputs have **no snapshot at
  all** (0 elements, differential only); the .NET harness ran full generation and emitted a
  52-element Patient snapshot.

Removing both: **engine-level element-set deviation ≈ 393 java-only / 54 net-only (447 total)**.

## 2. Group table

| # | group | side | count | tests | exemplar | mechanism (chapter) |
|---|---|---|---|---|---|---|
| G1 | cref slicing-entry re-expansion | java | 144 | t21 (70), comp-deep (24), comp-nested (24), reslicing-profile-parent (12), t29 (7), params-nested-slices (7) | `PlanDefinition.action.action.title` | Java expands the contentReference target's children under the **unsliced slicing entry**; .NET expands only under named slices (08, 11) |
| G2 | reslice subtree dropped by .NET | java | 138 | slice23 (86), reslicing-profile (26), slicing-profile-child (26) | `AuditEvent.agent.extension:altid/npi.value[x].system` | .NET **drops reslice elements entirely**, including diff-constrained ones — silent constraint loss (06) |
| G3a | slicing-entry children propagated into slices | java | 52 | org2a (16), org2b (16), on-questionnaire (20) | `Organization.identifier:NPI.system` | Java materializes entry-child constraints inside each named slice; .NET keeps slices collapsed (06) |
| G3b | complex-extension nested-slice inlining | java | 14 | pat-xver-extension | `Patient.extension:animal.extension:breed` | Java inlines the extension SD's full nested slice structure; .NET expands only diff-mentioned slices, without children (07) |
| G3c | additionalBase merge | java | 1 | multi-profile | `Patient.extension:pronouns` | Java honors `structuredefinition-additionalBase`; .NET ignores it (03) |
| G4 | renamed-choice anchoring | both | 89 (47 net + 42 java) | t16 (20n+18j), t31 (25n+24j), sushi1/2 (1n each) | `…latitude.value[x]:valueDecimal.*` (net) vs `…latitude.value[x].*` (java) | Same constraints, different element identity: .NET materializes a `value[x]:valueTYPE` slice and nests the subtree there; Java/golden nests directly under bare `value[x]` (07) |
| G5 | xt-logical .NET generation failure | java | 57 | xt-logical | `EHDSImagingReport.header.language` | .NET could not resolve the logical-model base chain → empty snapshot; harness artifact (09, 03) |
| G6 | t26 sort-mode snapshot | net | 52 | t26 | `Patient.contact.period` | golden/java sort output has no snapshot; .NET generated one anyway; harness artifact |
| G7 | R5 core elements injected into R4-based profile | net | 4 | sd-nested-ext | `Questionnaire.versionAlgorithm[x]` | base chain is R4 (fhirVersion 4.0.1, SDC); .NET rebuilds against R5 core Questionnaire adding R5-only elements; Java/golden keeps the R4 shape (03) |
| G8 | cdshooks key/value path rebasing | both | 4 (2 each) | cdshooks-services | java `…services.prefetch.key` vs net `…services.key` | .NET drops the `prefetch` path segment when placing key/value in a logical model — UNEXPLAINED, likely path-rebasing bug (nearest: 09) |
| G9 | cross-version `reported[x]` residue | net | 1 | mr-type-support | `MedicationRequest.reported[x]` | base is R4 us-core-medicationrequest; .NET keeps the R4 `reported[x]` element alongside R5 `reported`; Java emits only `reported` (03) |

Total 556; zero ungrouped. Chapter files: `docs/snapshot-spec/03-base-resolution.md`,
`06-slicing.md`, `07-type-and-extension-expansion.md`, `08-contentreference.md`,
`09-logical-models-interfaces.md`, `11-recursion.md`.

## 3. Per-group mechanism notes

### G1 — contentReference slicing-entry re-expansion (144 java-only)

Exemplar **t21**: diff slices `PlanDefinition.action.action` (a contentReference to
`#PlanDefinition.action`) into `subtask1`/`subtask2`, constraining `prefix` inside each
slice. Both engines emit the slicing entry `PlanDefinition.action.action` plus both slice
subtrees fully expanded (java 213 / dotnet 143 elements under the prefix). Java
**additionally** expands the cref target's complete child set under the unsliced slicing
entry itself — all 70 java-only t21 elements are `PlanDefinition.action.action.<child…>`
(deep: `…condition.*`, `…input.*`, `…participant.*` etc.).

Same shape everywhere the sliced element is a contentReference:

- **t29 / params-nested-slices**: `Parameters.parameter.part` (cref → `Parameters.parameter`).
  .NET has only `part` + `part:foo.*`; Java also has `part.{id,extension,modifierExtension,name,value[x],resource,part}`.
- **comp-deep / comp-nested / reslicing-profile-parent**: `Composition.section:<slice>.section`
  (cref → `Composition.section`). Java expands the 12 section children under the nested
  `.section` slicing entry at every recursion level the diff reaches (`section:flt.section.*`,
  `section:flt.section:relapseAfterEOT.section.*`); .NET emits the entry element only.

Matches the ch08 hypothesis (.NET expands under a cref only where the diff constrains
children — the slices — while Java also normalizes the entry), with comp-deep adding the
ch11 recursion-depth angle: the divergence repeats per nesting level.

### G2 — .NET drops reslices, Java expands them (138 java-only) — strongest register item

Exemplar **reslicing-profile**: diff (base AuditEvent) slices `AuditEvent.agent.extension`
by `url` + `value.system`, declares slice `altid` (ext profile), then reslices
`altid/npi` and `altid/ssn` with explicit constraints `altid/npi.value[x].use min=1`,
`altid/npi.value[x].system min=1 patternUri=…us-npi`, `altid/ssn.value[x].system min=1
patternUri=…us-ssn`. Java/golden emits both reslices with the extension fully expanded and
`value[x]` expanded as Identifier (13 elements each). .NET output contains **only**
`AuditEvent.agent.extension` and `…extension:altid` — the reslice entries **and the
differential's own constraints on them (min, patternUri) are silently lost**.

**slice23** is the same plus slice propagation: reslices `extOtherId/npi`,
`extOtherId/provider-id` declared on the unsliced `AuditEvent.agent.extension` also
materialize (in java/golden) under the agent slices `agent:user` / `agent:userorg`
(`AuditEvent.agent:user.extension:extOtherId/npi.*` etc., 14 elements per reslice, plus
the propagated `extension:extOtherId` entries); .NET has none of them.
slicing-profile-child = reslicing-profile's pattern via a child profile.

Ties to ch06 (reslicing) — .NET reslicing limitation confirmed empirically as *data loss*,
not merely reduced expansion.

### G3a — slicing-entry child constraints propagated into named slices (52 java-only)

Exemplar **org2a**: base is a profile whose snapshot already carries `identifier:NPI` /
`identifier:CLIA`. The diff constrains children of the **unsliced** slicing entry only
(`Organization.identifier.{id,use,type,period,assigner}` mustSupport). Java/golden
propagates the (modified) entry children into each named slice, materializing
`identifier:NPI.{id,extension,use,type,system,value,period,assigner}` (8×2 per test);
.NET leaves both slices as bare entry elements.

**on-questionnaire** is the same one level deeper: diff constrains extension slices and
their `value[x]` on unsliced `Questionnaire.item`; under the item slices
(`item:display`, `item:group`, `item:question`) java/golden carries each propagated
extension slice **with** its `{id,extension,url,value[x]}` children (20 java-only), .NET
carries the propagated slice entries only. Ch06 mechanism: what gets copied when
entry-content propagates into slices — Java copies the materialized subtree, .NET only the
top element.

### G3b — complex-extension nested-slice inlining (14 java-only)

Exemplar **pat-xver-extension**: diff mentions only `Patient.extension:animal` (typed by
the xver complex extension) and `…animal.extension:species`. Java/golden inlines the
extension definition's structure: nested slices `species`, `breed`, `genderStatus`
each with `{id,extension,url,value[x]}`. .NET emits only the diff-mentioned `species`
entry — and even that without children. Ch07 hypothesis (c) confirmed: extension
`url`/`value[x]` child sets differ because .NET expands only diff-constrained nodes.

### G3c — additionalBase (1 java-only)

**multi-profile** declares `structuredefinition-additionalBase =
…/multi-additional-base`. Java merges that additional base, which contributes
`Patient.extension:pronouns`; .NET ignores the extension. One element here, but a distinct
mechanism (ch03 base resolution).

### G4 — renamed-choice anchoring (89, both sides: 47 net-only + 42 java-only)

Exemplar **t16**: diff constrains children under a **renamed choice** —
`…extension:latitude.valueDecimal.extension:Geolocation-latitude-rendered(.valueString)`.
Both engines express all constraints; they disagree on element identity:

- .NET materializes an explicit choice slice `…latitude.value[x]:valueDecimal` and hangs
  the whole subtree under it (10 net-only elements per lat/long), keeping bare `value[x]` too.
- Java/golden keeps bare `value[x]` and hangs the subtree directly under it
  (`value[x].id`, `value[x].extension:…rendered.*`, `value[x].value` — 8-9 java-only per
  lat/long). At the innermost level Java too uses renamed-slice ids
  (`…rendered.value[x]:valueString`), which appear java-only merely because the parent id
  differs (3 records — parent-id fallout, same mechanism).

**t31** identical (`Parameters.parameter:string.value[x]` with a 21-element `translation`
extension subtree); **sushi1/sushi2** contribute one net-only
`value[x]:valueCodeableConcept` entry each (.NET adds the renamed slice element, Java puts
the constraints on `value[x]` itself). Golden resolves the representation choice in Java's
favor. Ch07 hypothesis (b) — refined: it is not that Java "prunes" renamed forms, but that
Java anchors renamed-choice diffs on the unrenamed `value[x]` while .NET normalizes to an
explicit type slice. This is the DEV-020/OQ-020 normalization territory.

### G5 — xt-logical: .NET generation failure (57 java-only) — harness artifact

`dotnet.log` shows resolution failure of the base logical-model chain
(`EHDSDataSet` unresolvable → snapshot generation failed for `EHDSDocument`), and the
emitted `dotnet.norm.json` has an empty snapshot. Same family as the excluded
ext-recursion-2 / logical-goo refusals (ch09/ch03), but here .NET wrote an output file so
the 57 elements leak into ELEMENT-SET. Should be reclassified harness-health, not merge
divergence.

### G6 — t26: snapshot on a sort test (52 net-only) — harness artifact

t26 is `mode=sort`; golden and java outputs contain **no snapshot** (differential-only,
4 elements). The .NET harness driver generated a full 52-element Patient snapshot anyway.
Classifier artifact of comparing a generated snapshot against an intentionally absent one.

### G7 — R5 elements injected into R4-based profile (4 net-only)

**sd-nested-ext**: base chain is `sdc-questionnaire` with `fhirVersion 4.0.1`. .NET's
output contains R5-only Questionnaire elements (`versionAlgorithm[x]`, `copyrightLabel`,
`item.answerConstraint`, `item.disabledDisplay`) — it rebuilt the base expansion against
the R5 core `Questionnaire`. Java/golden preserves the R4 shape. Cross-version base
handling (ch03).

### G8 — cdshooks-services key/value placement (2 java-only + 2 net-only)

Logical model; diff has `CDSHooksServices.services.prefetch.key` / `.value`. Java/golden
keeps them at `services.prefetch.key`/`.value`; .NET emits them as `services.key` /
`services.value` — the `prefetch` segment is dropped from both id and path.
**UNEXPLAINED** by current chapters (nearest ch09); looks like a .NET path-rebasing bug
for children of a logical-model element.

### G9 — cross-version `reported[x]` residue (1 net-only)

**mr-type-support**: base is R4 `us-core-medicationrequest` whose snapshot has
`MedicationRequest.reported[x]`; in R5 the element is `MedicationRequest.reported`.
Java/golden emits only `reported`; .NET keeps the stale R4 `reported[x]` alongside it.
Cross-version base handling (ch03), cousin of G7.

## 4. The ele-1 / contentReference question, answered

- **Cref re-expansion children: 144 java-only elements**, not ~191
  (t21 70, comp-deep 24, comp-nested 24, reslicing-profile-parent 12, t29 7,
  params-nested-slices 7). The earlier ~191 estimate (from ~191×5 ele-1 constraint diffs)
  is **not reproduced** by cref children alone; the next-largest java-only pools are the
  G2 reslice subtrees (138) and G3 slice propagation (67), so the 191 figure likely mixed
  classes. Discrepancy noted; not reconstructed further here.
- **ele-1 on the java-only elements**: 313/450 java-only elements carry an `ele-1`
  constraint; within G1 it is **128/144**, and the 16 without are exactly the `.id` and
  `.resource` children (element types that have no ele-1 in the base). So yes — the
  java-only cref children each carry the usual ~5 base constraints incl. ele-1.
- **"Do .NET-side siblings lack ele-1?"** — not applicable in the sibling sense: the .NET
  outputs have **no counterpart elements at all** for these keys (whole-element absence,
  not constraint-stripped twins). The ele-1 property diffs previously observed are the
  property-level shadow of these same missing/extra whole elements plus the G4 identity
  split, not an independent constraint-pruning behavior.

Full breakdown ele-1 vs side: java-only 313 with / 137 without; net-only 81 with / 25
without (the without-side dominated by `.id`/`.resource`-type children and t26/G6 rows).
