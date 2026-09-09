# R4 → R5 spec deltas relevant to snapshot generation

Sources: local HTML only — `C:\Git\snapshot-spec-materials\spec-html\R4` (verified v4.0.1) and `...\R5`
(verified 5.0.0). No R4B pages locally, so every delta is "R4 4.0.1 → R5 5.0.0"; R4B attribution is
unverifiable here. Citations: `ed` = elementdefinition.html, `ed-defs` = elementdefinition-definitions.html,
`prof` = profiling.html, `sd` = structuredefinition.html (section numbers as printed on each page).

---

## 1. ElementDefinition property list changes

### Added in R5 (absent from R4 4.0.1)

| Property | Card/Type | Generator consequence |
|---|---|---|
| `mustHaveValue` | 0..1 boolean, Trial Use | Constraining flag on primitives ("value cannot be replaced by an extension"; shortcut for min=1 on the primitive's `value`). Merge like other constraint flags (differential overrides base; don't relax an inherited true). eld-28: mutually exclusive with `valueAlternatives`. (R5 ed-defs; semantics in R5 ed §4.4 "Primitive Values".) |
| `valueAlternatives` | 0..* canonical(SD), Trial Use | Extensions allowed to replace a primitive value. Meaning-if-missing: "any extension can be used" — absent is weaker than constrained; differential replaces base, don't blindly union. |
| `constraint.suppress` | 0..1 boolean | Suppresses an inherited warning/hint constraint in a derived profile. When merging `constraint` (additive, ∆-rule), a differential constraint repeating an inherited key with `suppress=true` must reach the snapshot; eld-26: errors cannot be suppressed. |
| `binding.additional` (+ `purpose` 1..1 code, `valueSet` 1..1 canonical, `documentation`, `shortDoco`, `usage` 0..* UsageContext, `any` boolean) | 0..* | Additional bindings "do not replace the main binding"; spec gives no dedup rule for merging with base repetitions (additive context). R4 has no core equivalent — equivalent semantics required extensions (exact extension not verifiable from these local pages). |

### Removed in R5

- `constraint.xpath` (R4 ed-defs, 0..1 string) — gone in R5, along with the R4 sd Interpretation-Notes
  paragraph on XPath prefixes. R4-model snapshots may carry/merge xpath alongside expression; R5 must not
  emit it; cross-version conversion drops it.

### Changed type/binding on shared properties (from a scripted per-anchor diff of both definitions pages)

- string → **markdown**: `binding.description`, `constraint.requirements`, `mapping.comment`.
- ElementDefinition base type: BackboneElement → **BackboneType** (R5 datatype reorganization).
- Binding renames only: `type.code` FHIRDefinedType → "Element Definition Types" (extensible); `code` LOINC-example → ElementDefinitionCode.
- Open-type choice for `defaultValue[x]`/`fixed[x]`/`pattern[x]`/`example.value[x]`: **50 → 54 types** — added integer64, CodeableReference, RatioRange, Availability, ExtendedContactDetail; removed **Contributor**. `minValue[x]`/`maxValue[x]`: 9 → 10 (+integer64). (ttl views of both ed pages.) Fixed/pattern/default handling must accept the new types in R5, reject Contributor.
- No core `obligation` element in R5 — obligations are an **extension** (R5 ed §4.8, prof §5.1.0.22). R5's `mustSupport` definition says it "is being phased out and replaced by obligations", but the machinery is unchanged.
- Verified unchanged: `sliceName` (incl. eld-16 regex), `contentReference` ("can only be defined in specializations"), `base` (typo fix only), `slicing`/`slicing.*`, `representation`, `condition`, `maxLength`, `orderMeaning`, `isModifier(Reason)`, `isSummary`.

---

## 2. Invariant changes (eld-*, full-row diff incl. expressions)

- **eld-1 removed; replaced by sdf-28 with narrower scope.** R4 eld-1 ("If there are no discriminators, there must be a definition", `discriminator.exists() or description.exists()`) applied to every ElementDefinition, differentials included. R5 moves the same test to **sdf-28 on StructureDefinition.snapshot.element only**: an R5 differential slicing-entry without discriminator/description is no longer invariant-invalid, but the generated snapshot must still satisfy sdf-28.
- eld-4 / eld-17 expressions also allow `code = 'CodeableReference'` (aggregation / targetProfile on CodeableReference).
- eld-11 adds escape hatch `type.code.contains(":")` — bindings allowed on non-FHIR (absolute-URL) types in **logical models** — and adds `Duration` to the allowed codes.
- eld-12: valueSet may also start with `#` (contained ValueSet).
- eld-19/eld-20 rewritten (path syntax): anchored regexes, 64-char '.'-separated components, Upper/lowerCamelCase warning. Affects validating logical models/specializations, not constraint merging.
- New in R5: eld-23 (binding SHALL have description or valueSet), **eld-24 Guideline: prefer pattern[x] over fixed[x]**, eld-25 (Warning: ordered/openAtEnd slicing improper without orderMeaning), eld-26 (errors cannot be suppressed), eld-27 (Warning: mappings SHOULD be unique by key), eld-28 (mustHaveValue excludes valueAlternatives).
- Consequence: emitted snapshots are validated against these; eld-25 and eld-27 can fire on *merged* content (base+differential mappings; inherited ordered slicing without orderMeaning).

---

## 3. "Interpretation of ElementDefinition in different contexts" (R4 ed §2.30.0.4 / R5 ed §2.1.28.7.0.4)

The prohibited/optional/required table is **unchanged cell-for-cell** (both even retain the stale
"nameReference" row for contentReference). Deltas around it:

- min/max for "Type definition, first element": R4 "optional (irrelevant)" → R5 "optional§", new footnote: **the root cardinality of a type constrains elements that reference the type** (an extension defined 0..1 cannot be used 0..* in a profile). Paired with new R5 sd §5.4.6.1 "StructureDefinition Root Elements". Consequence: when expanding an element typed by an extension/profile, intersect the element's cardinality with that SD's root cardinality (R4 text treated root min/max as irrelevant).
- New R5 paragraphs on **FHIRPath System types** (`http://hl7.org/fhirpath/System.String`) and the `structuredefinition-fhir-type` extension on primitive `.value` elements — absent from R4.0.1's page; a dual-version generator handles both encodings of primitive value types.
- "Rules about min and max": R5 adds a 4th rule — **"If min = 0 and there is a fixed or pattern value present, the fixed or pattern value only applies if the element is actually present"** — and flags the section plus mustHaveValue/valueAlternatives as Trial Use. Validator semantics; no merge change.
- New R5 subsections: **"Primitive Values"** (§4.4, mustHaveValue/valueAlternatives as shortcuts), "Bindings / Additional Bindings" (§4.7, pointer), "Must-support" (§4.8, points at the obligation extension).
- **"Rules about Slicing" subsection: verbatim identical** (slicing only when constraining; on first repeat; later repeats need sliceName; @default; entry constraints apply to all slices; entry min governs total occurrences). "Rules about Aggregation" and the path/type-per-context table: identical.
- "Missing Elements": R5 adds meaning-if-missing rows for new elements; mustSupport meaning-if-missing text unchanged (snapshot SHOULD always populate mustSupport). No rule change.

---

## 4. Choice type (value[x]) renaming — headline delta

R4 4.0.1 **allowed (prescribed) renaming** the path of a single-type choice; R5 **removed renaming** — the
type-specific constraint is a type slice keeping the `[x]` path. Three independent confirmations:

1. R4 sd Interpretation Notes (§5.3.5): "When profiling a resource and only one type is specified, **the name of the element is changed to include the type instead of '[x]'**" — sentence **deleted** in R5 sd §5.4.6.
2. id algorithm example: R4 ed §2.30.0.3 "For **path = Patient.deceasedBoolean**, the id is Patient.deceased[x]:deceasedBoolean" → R5 ed §2.1.28.7.0.3 "For **path = Patient.deceased[x]**, the id of the boolean slice type element is Patient.deceased[x]:deceasedBoolean".
3. Choice-constraint rules: R4 "the inclusion of a **type specific path** (such as "Patient.deceasedBoolean")" → R5 "type specific **element** (such as "Patient.deceased[x]:deceasedBoolean")". Both keep: type-specific entries do not restrict allowed types, and "the original element SHALL always be represented in a snapshot; the type specific variants are only represented when needed". R5 ed path rules add: polymorphic paths SHALL end in "[x]".

Generator consequence: accept renamed paths (Patient.deceasedBoolean) in R4 differentials, matching them
to the base [x] element; for R5, expect/emit [x] path + sliceName; in both versions keep the original [x]
element in the snapshot.

---

## 5. Slicing (profiling.html)

- **sliceIsConstraining: VERIFIED PRESENT in the local R4 v4.0.1 page** (R4 ed-defs anchor, 0..1 boolean, definition identical to R5's). No R4→R5 delta. (4.0.0 vs 4.0.1-correction origin not determinable from the page.)
- **Discriminator `value` redefined / `pattern` deprecated** (R4 prof §5.1.0.11 → R5 prof §5.1.0.13): R4 had `value` = "different values in the nominated element" with `pattern` separate (pattern[x]-based). R5: `value` = "different values ... as determined by the applicable **fixed value, pattern, or required ValueSet binding**"; `pattern` "has the same meaning as 'value' and is deprecated" (R5 Short Display: "value | exists | type | profile | position"; the code stays valid). Consequence: R5 `value` matching evaluates fixed, pattern *and* required binding; treat pattern ≡ value both directions when merging inherited slicings.
- **New discriminator type `position`** (R5 only): match by index; only if all but the last slice have min=max (min>0); last slice may differ.
- **`exists` discriminator tightened** (R5): SHALL be at most two slices — one max=0, the other min≥1; order irrelevant. R4 had no such rules.
- Unchanged in both: restricted-FHIRPath discriminator grammar (extension(url), resolve(), ofType()); value/pattern discriminators backed by fixed[x], pattern[x] or required extensional binding; composite discriminator values; slice-group adjacency; extensions sliced by url. R5 adds a clarification that elements after `resolve()` are constrained via the **targetProfile** on the element preceding resolve().
- **Slice cardinality** (R4 §5.1.0.12 → R5 §5.1.0.14): first rule fixed from R4's garbled "Each slice cannot have a greater cardinality than the maximum number of slices allowed" to "The maximum cardinality on each slice cannot exceed n"; R5 adds "sum of the minimum cardinalities of the slices SHOULD be ≤ m" (advisory).
- **Default slice** (@default) rules identical. **Re-profiling and Re-slicing** (incl. `/` reslice names, @default/@default): **verbatim identical** — important non-delta.
- New R5 section "Profiling portions of a resource" (§5.1.0.16): documents the `elementdefinition-profile-element` extension on `type.profile` (start expansion at a nominated element id in the target profile). The extension is already referenced in the R4 ed-defs comment on type.profile — support in both versions; R5 only adds normative-page documentation and an example.
- New R5 section "Recursive Elements" (§5.1.0.10): constraints on a recursive element (Questionnaire.item) apply to its recursive (contentReference) descendants too. Not stated in R4; affects expansion of contentReference children under a constrained recursive element.

---

## 6. structuredefinition.html: invariants, derivation, logical models

- **sdf-3 relaxed for logical models**: R4 "Each element definition in a snapshot must have a formal definition and cardinalities" → R5 "...unless model is a logical model" (`%resource.kind = 'logical' or element.all(definition.exists() and min.exists() and max.exists())`). Behavior fork: R4 logical-model snapshots need definition/min/max synthesized on every element; R5 permits their absence.
- New R5 invariants: **sdf-24** (Rule, snapshot): CodeableReference target profiles go on the CodeableReference element, not its `.reference` child; **sdf-25** (Rule, snapshot): bindings on the CodeableReference, not `.concept`; **sdf-26** (Guideline): profile root should not have mustSupport=true; **sdf-27** (Rule): baseDefinition implies derivation; **sdf-28**: see eld-1 above; **sdf-29** (Warning): resource elements min 0|1, max 1|*. sdf-24/25 constrain what the generator may emit when a profile walks into a CodeableReference.
- sdf-0 (name-is-identifier warning) dropped; equivalent now cnl-0 (CanonicalResource common invariant). sdf-23 (no slice name on root) unchanged.
- Element-ordering rules **unchanged**: order follows baseDefinition; base elements before new elements in specializations; children depth-first; **unsliced descendants of sliced elements appear before slices**.
- "Different Uses for StructureDefinition": R5 adds `abstract` to the pattern and three new categories — abstract base Resource, **interfaces** (e.g. CanonicalResource; abstract, spec-only), and logical models with baseDefinition **Base**. An R5 generator meets abstract interfaces as bases and logical models rooted on Base; R4 had neither.
- StructureDefinition element diff: only `versionAlgorithm[x]` and `copyrightLabel` added (metadata, no snapshot impact); `kind`/`derivation`/`context` machinery unchanged. Logical Models narrative: **verbatim identical**.
- New R5 rule in prof "Using StructureDefinitions" (§5.1.0.9): a profile may restrict/clarify a mapping "by providing a new mapping with the **same identity**, which means that the new mapping **replaces** a mapping with the same identity in the element being profiled". R4 mappings were additive-only (∆ footnote; R5 keeps it for *additional* mappings). Merge change: R5 mapping merge = replace-by-identity, else append (eld-27 warns on duplicate keys).

---

## 7. fixed[x] / pattern[x] semantics

- R4 4.0.1 already contains the full pattern-matching prose including **arrays** ("each element provided in the pattern[x] array must (recursively) match at least one element from the instance array") — NOT new in R5.
- R5 changes: fixed[x] adds "**, if present**"; pattern[x] becomes "each occurrence of the element ... SHALL follow ... **if the element has a value**"; array sentence reworded to "When **an element within a** pattern[x] is used to constrain an array..."; new sentence: "**If a pattern[x] is declared on a repeating element, the pattern applies to all repetitions**. If the desire is for a pattern to apply to only one element or a subset of elements, slicing must be used." With the new min/max rule (§3), fixed/pattern do not force presence and outer repetition is not the "array match".
- New eld-24 Guideline: prefer pattern[x] over fixed[x]. eld-5/6/7/8 unchanged.

## 8. type.profile / targetProfile

Unchanged R4→R5: both 0..* `canonical(StructureDefinition | ImplementationGuide)`, identical definitions
("one must apply" disjunctive semantics); eld-17 gains only the CodeableReference alternative. Neither
version's local pages mention the STU3 history (STU3 had 0..1 profile/targetProfile) — source that elsewhere.

## 9. Descriptive-element merging (new R5 guidance)

New R5 prof §5.1.0.8 "Profiling descriptive elements" table: label revise; code.coding add/remove;
short revise; definition revise; comment revise/add/remove; requirements revise/add; alias revise/add/remove;
example revise/add/remove; mapping revise/add/remove. R4 had no such table (only ‡/∆ footnotes). Explicit
license for differential-overrides-base on descriptive properties, including *removal* (which the
∆-additive model did not contemplate for mapping/example/alias).

## 10. Must Support / obligations

R4 prof §5.1.0.19 and R5 prof §5.1.0.22 share the core rules (meaning must be defined; false→true only,
never true→false). R5 adds: obligations as structured alternative (addable, never loosened); MS semantics
across IG dependency chains; child-MS-without-parent-MS semantics; complex-element MS default expectation.
mustSupport meaning-if-missing (snapshot SHOULD always populate it) unchanged in both.

---

## Verified unchanged (important non-deltas)

- Re-profiling/re-slicing rules incl. `/` reslice-name syntax — verbatim identical.
- "Differential vs Snapshot" — verbatim identical (sparse differentials, root optional in differential, snapshot fully calculated).
- Cardinality-restriction table (0..1/0..*/1..1/1..* × allowed restrictions) — identical.
- "Rules about Slicing" (ed page); slicing/discriminator ElementDefinition definitions; default slice (@default) rules — identical.
- Element ordering rules for differential and snapshot — identical.
- sliceName + eld-16 regex; contentReference; ElementDefinition.base — identical.
- Extension Definitions & Using Extensions in Profiles; Binding Definitions; Changing Binding Strength table — identical.
- Limitations of Use — identical except "cannot give more specific names to elements" → "cannot change the name of elements" (reinforces §4).

## Could NOT be verified from these local pages

1. **R4B (4.3.0)**: no pages downloaded — cannot tell which R5-vs-R4.0.1 deltas (CodeableReference in eld-4/17, RatioRange, sdf-24/25, discriminator changes, ...) already appeared in R4B.
2. **conformance-rules.html / extensibility.html**: only R5 copies exist locally — no R4 counterparts to diff; obligation-extension detail and extension-context rules live there.
3. Whether `sliceIsConstraining` was in R4 4.0.0 or only the 4.0.1 technical correction (page states v4.0.1 only).
4. STU3→R4 history of type.profile/targetProfile cardinality (0..1 → 0..*): not mentioned on either page.
5. Exact membership of the DiscriminatorType / Element Definition Types value sets (pages give names/short displays, not expansions).
6. Normative vs Trial-Use status per element beyond inline statements (ballot-status markers were stripped with the HTML).
