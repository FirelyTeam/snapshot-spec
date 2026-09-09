# min / mustSupport diff mining — full-sweep classification

.NET = Hl7.Fhir.R5 6.2.1 | Java engine = 6.10.2 (d06577dbc5c6) | golden = fhir-test-cases 1.7.67

Source: `harness/out/<id>/{java,dotnet}.norm.json`, diffed with report.py's own alignment logic
(elem_key by id, keyed-list alignment). Scope: every NEW-bucket diff whose leaf property is `min`
or `mustSupport` (both elements present on both sides). Machine-readable per-hit dump (with
co-occurrence scores and mechanism tags) in scratchpad `mm_classified.json` (session-local).

**Count correction vs the brief:** min = **52** (matches), mustSupport = **79**, not 61 — the 61
came from grepping `report.md`, which caps each test's NEW list at 40 entries (sd-comp-hist alone
has 36 mustSupport hits). Also excluded by construction: `mustSupport` pairs where one side is
explicit `false` and the other absent land in report.py's FALSE-VS-ABSENT bucket, not here.

## Headline: the exemplar hypothesis is REFUTED — inverted causality

The ILCorePractitioner co-occurrence signature is real, but it is **not** a .NET element-matching
failure. The .NET side of `Practitioner.identifier:prac-lic.value` **did** match and merge its own
differential (authored `short`, `example` value `1-12345`, `constraint` key `identifier-dash` are
all present in the .NET snapshot). The "authored" values on the Java side (`min=1`,
`mustSupport=true`) were never written on that differential element at all — they were authored on
the **unsliced slicing-entry's children** (`Practitioner.identifier.system` / `.value`, min=1
MS=true), and **Java propagates merged entry-child constraints into every named slice's children**;
.NET builds slice children from the raw base/datatype and has no such propagation. So the
extra-values direction is Java-side extra machinery, not .NET-side lost matches (ch04's matching is
not implicated; contrast .NET's pristine-clone slice base, `docs/snapshot-spec/06-slicing.md`
~130-138).

Java mechanism located: **`SnapshotGenerationPreProcessor.java`** (org.hl7.fhir.r5
`conformance/profile/`, invoked from `ProfileUtilities.java:825`). A differential pre-processing
pass collects "sliceStuff" — every differential element between a slicing entry and its first named
slice — and, working backward, **merges it into each named slice** (`processSlices` →
`mergeElements` → `merge`, lines ~688-810, 993-1075): fill-if-absent for min, max, mustSupport,
fixed, pattern, type, binding, constraint (append), example (append); elements missing from a slice
are **injected** (id rewritten `slicer.id → slice.id`, marked `SNAPSHOT_PREPROCESS_INJECTED` — this
also explains a chunk of the java-only ELEMENT-SET bucket). Extension (url-discriminator open)
slicing is excluded as a *slicer* (`isExtensionSlicing`), but entry-children that are themselves
extension slices ride along as sliceStuff (see t22 validDate below). This fills the pending stub at
`docs/snapshot-spec/02-differential-preprocessing.md:122`.

## Classification totals

In the task's A/B/C vocabulary:

| bucket | min | mustSupport | total |
|---|---|---|---|
| **A — .NET lost base-profile properties** (needs .NET repro) | 0 | 1 | 1 |
| **B — genuine per-property merge-semantics difference** | 0 | 0 | **0** |
| **C — known entry-min mechanics** (C1 DEV-020 + C2/C3 same family) | 26 | 0 | 26 |
| **M — Java-side mechanisms .NET does not implement** (new classes) | 26 | 78 | 104 |
| total | 52 | 79 | 131 |

Bucket B is empty *by evidence, not omission*: in every M1 group the .NET element carries its own
slice-child differential merges correctly (spot-checked ILCorePractitioner, t22 MRN validDate min,
sd-comp-hist section pattern merges) — min/mustSupport never diverge through the merge routine
itself in this sweep.

Breakdown of M (Java-side mechanisms):

| tag | mechanism | Java code | min | MS |
|---|---|---|---|---|
| M1 | slice-content propagation of entry-children | `SnapshotGenerationPreProcessor.processSlices/merge` | 26 | 71 |
| M1x | same, entry-level *extension slice* propagated into named slices | same (sliceStuff includes sub-slices) | 0 | 2 |
| M1q | same, suspected leaf-name cross-match quirk (OQ candidate) | `SnapshotGenerationPreProcessor.elementsMatch:812-822` | 0 | 2 |
| M2 | `structuredefinition-additionalBase` merge | `SnapshotGenerationPreProcessor.process:137-152` | 0 | 1 |
| M3 | `inherit-obligations` mustSupport stamp | `ProfileUtilities.java:2544-2552` | 0 | 2 |

.NET-side signature everywhere in M: the .NET value is simply the base source's value — explicit
`false` where the base chain carries explicit `mustSupport: false` (R5 core resource elements,
profile bases), `null` where slice children were unfolded from a datatype (Identifier, Coding) whose
elements carry no mustSupport. That split (n=False vs n=None) tracks the base source exactly,
confirming ".NET kept base".

## Bucket M1 groups — entry-authored constraints missing from .NET slice children

For each group: what the author wrote on the *unsliced entry children*, and where it fails to reach
the .NET slice children. In all groups the Java value equals the entry-child authored value
(verified programmatically for all 97 M1 hits).

### sd-comp-hist — 45 hits (9 min + 36 MS) — dominant test
Base `clinicaldocument`. Differential authors on the entry children: `Composition.section.code`
min=1 MS=true, `.section.title/.text/.entry` MS=true — then slices `section` into 9 named slices
(history, findings, impression, procedure, imagingstudy, order, recommendation, communication,
comparison). Java preprocessor copies code/title/text/entry constraints into all 9 slices
(9×min on `.code`, 9×4 MS). .NET: each slice's children come from the base BackboneElement
children (explicit MS=false in core Composition snapshot) → j=1/n=0, j=true/n=false.

### t22 — 27 hits (13 min + 14 MS, incl. M1x ×2)
Base Patient. Entry children authored: `Patient.identifier.type(.coding(.system/.code))`,
`.system`, `.value` all min=1 MS=true; plus an *extension slice on the entry*
`Patient.identifier.extension:validDate` (max=1, type profile identifier-validDate, MS=true). Two
named slices JHN/MRN. Java propagates all of it into both slices — including into the **reslice
children** `type.coding:preferredCoding.system/.code` (leaf-path matching applies entry
`type.coding.system` constraints inside the preferredCoding slice) and into the per-slice
`extension:validDate` elements (M1x: MS=true, type profile, and max=1 — note java MRN validDate
max="1" vs .NET "*", same mechanism, shows up in other buckets). .NET slice children unfold from
the Identifier datatype → min=0, MS absent.

### on-questionnaire — 21 M1 hits (3 min + 18 MS) + 2 M1q
Base Questionnaire. Entry children authored: `Questionnaire.item.text` min=1 MS=true,
`.item.linkId` MS=true, `.item.enableWhen(.question/.operator/.answer[x])` MS=true. Three named
item slices (display, group, question) each show the full set. .NET keeps core Questionnaire's
explicit MS=false.

**M1q (OQ candidate, code-inferred — not empirically isolated):**
`Questionnaire.item:group|question.extension:itemControl.value[x]` MS j=true / n=null. MS=true on
that value[x] is authored *nowhere* (not in the diff, not in the itemControl extension SD 5.2.0
snapshot). Reading of `elementsMatch` (`SnapshotGenerationPreProcessor.java:812-822`): it compares
only leaf `path` + leaf `sliceName`, ignoring ancestor slice names — so sliceStuff elements
`Questionnaire.item.extension:enableWhenExpression|hidden|renderStyle.value[x]` (all MS=true,
same path `Questionnaire.item.extension.value[x]`, null leaf sliceName) merge their MS into
*itemControl's* value[x]. Cross-slice contamination; if confirmed by a minimized repro this is a
Java defect worth WGM/GG attention.

### ILCorePractitioner — 4 hits (2 min + 2 MS)
Entry children `Practitioner.identifier.system/.value` min=1 MS=true → slice `prac-lic` children.
The same propagation also explains this element's example/binding co-diffs (entry-child example
`123456` appended ahead of the slice's authored `1-12345` on the Java side — one passing note:
example merges append in this pathway rather than replace).

### eob-nested — 2 MS hits
Entry child `ExplanationOfBenefit.adjudication.category` MS=true (plus binding description/extension
riding the same propagation, visible as co-diffs) → slices `adjudicationamounttype`, `denialreason`.

### t32 — 1 MS hit
Entry child `Questionnaire.item.linkId` MS=true → slice `item:record-type.linkId`. j=true/n=false.

## Bucket A — .NET lost base-profile properties (1 hit, needs .NET repro)

**mr-type-support**, `MedicationRequest.reported` MS j=true / n=false. Test registers a local
us-core-medicationrequest base whose differential authors `reported[x]`: MS=true + type boolean
carrying `elementdefinition-type-must-support`. The derived diff re-states type boolean only.
Co-diffs on the same element: java-side type extension (`type[boolean].extension` url+valueBoolean)
and the base's `argonaut-dq-dstu2` mapping present only in java; .NET shows explicit MS=false —
the value core R5 MedicationRequest carries. The .NET element looks rebuilt from core rather than
from the profile base. **Two-hop ambiguity:** registered bases aren't dumped by the harness, so
whether .NET dropped MS + type extension while generating the *base* snapshot or while merging the
*derived* re-typed choice cannot be told from the sweep output alone. Single element; tag: needs
.NET-side repro (ch04/ch05, renamed-choice-over-profile-base scenario; test description is
literally "Duplicating must-support extensions on type").

## Bucket B — per-property merge-semantics differences

None found (see totals). Every min/mustSupport divergence traced to a mechanism, not to the
property-merge rules of ch05.

## Bucket C — entry-min mechanics (26 min hits)

**C1 — DEV-020 type-slicing entry normalization** (5): `obs-2b` (the documented exemplar),
`obs-4`, `zib-BodyHeight` (`Observation.value[x]`), `t29` (`Parameters.parameter.part:foo.value[x]`),
`t34a` (`Extension.value[x]`). A type slice with min≥1 makes Java raise the entry min
(`ProfilePathProcessor.java:609-617: if (diffMatches.get(i).getMin() > 0) ... elementDefinition.setMin(1)`).
Tagged, not re-analyzed — register entry `13-deviation-register.md` DEV-020 / OQ-020. t29/t34a/zib
are fresh instances confirming the mechanism on sparse/shortcut forms (t34a: diff writes
`Extension.valueCode min=1` only; both engines build the `value[x]:valueCode` slice identically,
only the entry min diverges).

**C2 — auto-added slicing entry min := sum of slice mins** (20): all the `.extension` /
`.modifierExtension` entry hits (complex-extension2, pat-xver-extension, t12, t12a, t14, t15, t16,
t18, t19, t34, t35, t22 `identifier:MRN.extension`, telus-oo nested `.extension`, on-questionnaire
`…extension:translation.extension` j=2 ×3 and `…extension:targetConstraint.extension` j=4 ×2) plus
**au-med-k** (`MedicationStatement.medication.concept.coding` j=1/n=0). Java's post-pass
`ProfileUtilities.java:983-1005`: for a sliced element that repeats, if the slice mins sum above the
entry min, Java **sets entry min to the sum — but only when the entry carries
`SNAPSHOT_auto_added_slicing`** (otherwise it just emits a warning). Extension slicing entries are
always auto-added; au-med-k gets there because its diff re-slices `coding` *without restating the
slicing intro*, which makes Java stamp the entry auto-added (`ProfilePathProcessor.java:343-345`)
even though the base au-med authored an explicit intro. The j=2/j=4 values are the sums of the
mandatory sub-extension mins unfolded from the complex-extension definitions. Same "entry min
raise" family as DEV-020 (candidate scope-extension of that register entry rather than a new one).

**C3 — extension value[x] slice keeps base min** (1): telus-oo
`OperationOutcome.issue.extension:userText.value[x]:valueString` j=1/n=0. Java zeroes an unstated
slice min *except* when the sliced path ends in `xtension.value[x]` — an explicit hack
(`ProfilePathProcessor.java:801-805`, comment "hack work around for problems with snapshots in
official releases"), so the valueString type slice inherits the extension's mandatory value min=1.
.NET resets slice min to 0 per its pristine-clone rule (`06-slicing.md`, DEV-008 territory).

## Other Java-side mechanisms (M2/M3, 3 MS hits)

- **M2 additionalBase** (1): multi-profile `Patient.gender` MS j=true/n=false. SD carries
  `structuredefinition-additionalBase` → Java preprocessor merges the additional base profile's
  differential (`SnapshotGenerationPreProcessor.process:137-152`, `mergeElementsFromAdditionalBase`);
  gender MS=true comes from `multi-additional-base`. .NET does not implement additionalBase.
- **M3 obligation inheritance** (2): profile-patient-op3 `Patient.birthDate` / `Patient.deceased[x]`
  MS j=true/n=false. SD carries four `inherit-obligations` extensions; Java merges obligation-profile
  elements and sets MS=true when any obligation element has it (`ProfileUtilities.java:2544-2552`);
  the java-side obligation extension on birthDate shows as co-diffs. .NET does not implement
  obligation inheritance.

## Which tests dominate

sd-comp-hist (45), t22 (27), on-questionnaire (29 incl. C2/M1q), ILCorePractitioner (4) — i.e.
77% of all min/mustSupport NEW diffs are the single M1 propagation mechanism. Fixing/deciding M1
(spec question: *do slices inherit the slicing entry's children constraints?* — R5 profiling only
says slices "must be consistent with" the entry; Java implements copy-down as differential
preprocessing, .NET implements nothing) would clear 101 of 131 hits across 6 tests. The rest:
26 entry-min mechanics (DEV-020 family), 3 unimplemented-feature hits (additionalBase/obligations),
1 .NET repro case (mr-type-support).
