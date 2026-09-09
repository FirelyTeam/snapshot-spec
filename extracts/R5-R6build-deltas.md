# R5 → R6 CI-build deltas — snapshot-generation focus

> **R6 source: FHIR CI-Build `v6.0.0-ballot4`** (`hl7.fhir.r6.core#6.0.0-ballot4`, page footer "generated on Tue, Aug 18, 2026 17:39+0000"), **fetched 2026-08-21**. The CI build changes daily — re-verify before relying on any citation below.
> R5 source: published FHIR R5 `v5.0.0` (permanent URLs). Pages compared: profiling, conformance-rules, elementdefinition (+definitions), structuredefinition, extensibility, defining-extensions.
> Method: tag-stripped HTML, per-section/per-anchor word-level diffs; table cells and invariant expressions re-verified against raw HTML where load-bearing.

---

## Job 1 — Deltas by topic

### 1. ElementDefinition datatype (elementdefinition-definitions)

Exactly **one new element**: `ElementDefinition.binding.additional.key` (0..1, `id`, Σ) — "unique identifier for the additional binding within the element that allows additional bindings to be matched across profiles." This is the hook for constraining additional bindings in derived profiles (see §7 below).

Other schema-level changes (R6's own "Changes from R5 to R6" list confirms; no elements removed):

- `binding.strength` adds code **`descriptive`** (now required | extensible | preferred | example | descriptive), policed by new **eld-32**: `strength = 'descriptive' implies valueSet.empty()`.
- `binding.additional.purpose` adds codes **`current-extensible`**, **`best-practice`** (rendered enum: maximum | minimum | required | extensible | candidate | current | current-extensible | best-practice | preferred | ui | starter | component). Caveat: the page's changes-list also claims code `open`, which the rendered enumeration does *not* contain — ballot inconsistency.
- Open-type choices (`defaultValue[x]`, `fixed[x]`, `pattern[x]`, `example.value[x]`, and `Extension.value[x]`) add **VirtualServiceDetail** (choice count 54 → 55).
- **`valueAlternatives` semantics extended**: R5 = extensions that can replace a *primitive* value. R6 = also applies to **complex types and backbone elements** ("if the element is present, it SHALL have at least one of the listed extensions, a value (if primitive), or a non-extension child element (if a complex type or backbone element)"), plus explicit: "valueAlternatives applies only to the elements it is declared on. **It does not propagate to child elements.**" The R5 "Trial Use" disclaimer on mustHaveValue/valueAlternatives is removed.
- **Obligations are still NOT in core ElementDefinition** — mustSupport + the Obligation *extension* remain the mechanism. The "Obligations" tab/checkbox on R6 pages is CI-build page rendering, not schema.
- `binding.description` redefined ("Free-text guidance on the codes that are appropriate for this element"), with new comment: may be used "where terminology constraints are expressed only as additional bindings" — pairs with strength `descriptive`.
- `isSummary` comment tightened: descendant "**within the same StructureDefinition**" marked isSummary=true.
- Presentation only: invariant tables relabeled "Invariants" → "Constraints"; various comment expansions (`code` mapping guidance, `mapping`, `constraint.source`, `maxLength` — the latter with fresh R6 typos "Tupe Characteristics"/"Characterstics").

**Generator consequence**: model gets one new mergeable node (`additional.key`), one new binding-strength code that must be excluded from vocabulary validation lattices, and valueAlternatives merge logic must not restrict itself to primitives.

### 2. "Interpretation of ElementDefinition in different contexts" table (elementdefinition §2.1.30.6.0.4)

Structurally the same table; substantive cell changes (columns: Type-def root | Type-def following | Constraint-def root | Constraint-def following):

| Row | R5 | R6 | Note |
|---|---|---|---|
| *(headers)* | "first element" | "**root element**" | new footnote ♉: "the element without . separators in the path. It is not always present in differentials" |
| slicing | proh / **proh** / proh / opt | proh / **opt** / proh / opt | **slicing now allowed in specializations** (type definitions) |
| requirements/comments/alias | one combined row: proh / opt / proh‡ / opt‡ | **split into 3 rows**: `requirements` keeps proh/opt/proh‡/opt‡; **`comment` = optional ×4; `alias` = optional ×4** | comment/alias now legal on roots |
| base | snapshot: required / diff: optional (both type cols); **required / required** (constraint cols) | snapshot: **expected** / diff: optional (both); **expected / expected** | see erratum (c); "Expected/Not expected" is a new soft category: "SHOULD or SHOULD NOT be present, but real world StructureDefinitions … are not invalid" |
| type | **required** / required / opt / opt | **Not expected** / required / opt / opt | root of a type definition should carry no `type` |
| mustSupport | **proh / proh** / opt / opt | **optional♉︎ / optional♉︎** / opt / opt | footnote ♉︎: "In general base definitions should not set mustSupport to true, and no HL7 resources do" |
| isModifier | **proh** / opt / **proh** / opt† | **opt / opt / opt** / opt† | |
| isSummary | **proh** / opt / **proh** / opt† | **Not expected** / opt / **Not expected** / opt† | |
| binding | proh / opt / **proh** / opt | proh / opt / **opt** / opt | binding now allowed on constraint root |
| condition | **proh** / opt / **proh** / opt∆ | **Not expected** / opt / **Not expected** / opt∆ | |

Unchanged rows: sliceName, label, code, short/definition, nameReference (sic), min/max, defaultValue[x], meaningWhenMissing, fixed[x], pattern[x], example[x], minValue[x], maxValue[x], maxLength, constraint, mapping. Footnotes †/‡/∆/§ textually unchanged; new footnotes ♉ (root/Expected definitions) and ♉︎ (mustSupport) added — the build literally renders both as the Taurus glyph, and "Notes:♉" is glued.

**Generator consequence**: several "prohibited" cells a strict generator/validator might enforce are now optional or merely "Not expected" — notably slicing in specializations, isModifier/binding on constraint roots, and mustSupport in type definitions. The `base` requirement on snapshot roots of type definitions is softened from required to expected.

### 3. Invariants

**eld-\***: R5 set = eld-2..8, 11..28 (eld-1, 9, 10 absent — unchanged in R6, nothing removed). All existing expressions byte-identical except eld-11's quote style (`":"` → `':'`). **Six new in R6**:

- **eld-29** (Warning, on `type`): profiles SHOULD be unique — `profile.isDistinct()`
- **eld-30** (Warning, on `type`): targetProfiles SHOULD be unique — `targetProfile.isDistinct()`
- **eld-31** (Guideline, on `binding.additional`): additionalBindings should have a key — `key.exists()`
- **eld-32** (Rule, on `binding`): "If the strength is 'descriptive' no value set can ve provided" *(typo in source)* — `strength = 'descriptive' implies valueSet.empty()`
- **eld-33** (Warning, on ElementDefinition): >1 bindable type ⇒ usually an error to have a binding — `type.where(code in 'code' | 'Coding' | 'CodeableConcept' | 'CodeableReference' | 'uri' | 'string' | 'Quantity').count() > 1 implies binding.empty()` (note: type list differs from eld-11's — has CodeableReference, lacks Duration)
- **eld-34** (Guideline, on `binding`): required binding with no usage should be the base binding — `strength != 'required' implies additional.where(purpose = 'required' and usage.empty()).empty()`

**sdf-\***: R5 set = sdf-1..6, 8, 8a, 8b, 9-11, 14, 15, 15a, 16-29 (7, 12, 13 absent — unchanged). One expression change, one addition:

- **sdf-9 relaxed**: R5 "no **label, code or** requirements on an element without a '.' in the path" → R6 "no **requirements** on…"; expression drops the label/code conjuncts: `children().element.where(path.contains('.').not()).requirements.empty()`. **Label and code are now legal on root elements** (consistent with the interpretation table, which always allowed them).
- **sdf-30 new** (Rule, base): "ContentReferences can only be defined in specializations, not constrained types" — `differential.element.where(contentReference.empty().not()).exists() implies derivation = 'specialization'`.

### 4. Slicing (profiling §5.1.0.13-17 + elementdefinition §…4.1)

- **Discriminator types**: table unchanged — value / exists / pattern (still deprecated, "same meaning as 'value'") / type / profile / position. Both R5 `position` definitions survive verbatim, contradiction included (erratum b).
- **New paragraph** (profiling, after the restricted-FHIRPath rules): the element(s) asserting the discriminating constraints **may live in different profiles** than the one defining the slicing — always the case with `resolve()`, but also possible in-resource (example: slice Patient.address by 'use', with the fixed values in separate Address profiles).
- **NEW normative-style list in elementdefinition "Rules about Slicing"** — the constraints on the slicing entry that apply **across all slices and entries**: *max, type (code, profile, targetProfile, aggregation and versioning), fixed[x], pattern[x], minValue[x], maxValue[x], maxLength, constraints, required and extensible bindings (including additional bindings), mustHaveValue, valueAlternatives.* Obligations apply across all slices unless the obligation specifies an 'applicable-number'. **mustSupport assertions that do not specify otherwise cannot be assumed to apply to all slices** (legacy reasons; consult documentation/community); a slice labelled must-support isn't nullified by an unlabelled slicer. — *This is effectively the spec for what a snapshot generator propagates from a slicing entry onto slices; R5 had no such list.*
- **Unchanged (verified)**: slicing lattice open/closed/openAtEnd and `ordered` semantics, reslicing rules and syntax (`slice/reslice` names), slice-cardinality rules (including the erratum-a contradiction), the "extensions are always sliced by url" rule, the restricted-discriminator-FHIRPath subset, and the slicing-rules code list.

### 5. Recursion / contentReference — **semantics flipped** (profiling §5.1.0.11 area)

- R5: constraints on recursing backbone elements (e.g. Questionnaire.item) **apply to the recursive references as well** (item.item etc. inherit them); FHIRPath constraints suggested to scope by level.
- R6: constraints "**only apply to the literally stated path**"; also generalized to same-content elements (ValueSet.compose.exclude vs include). To constrain contentReference targets, constrain them directly or use the new **`contentReferenceProfile` extension** to pull constraints from an applicable element in the same or another profile.
- Reinforced by **sdf-30** (no contentReference introduction in constraints).
- **Generator consequence**: this is the highest-impact behavioral delta found. An R5-conformant generator/validator that pushes slicer constraints down recursive contentReference chains is wrong under R6; under R6 the chain is inert unless contentReferenceProfile (or direct constraints) say otherwise.

### 6. Descriptive properties & constraint removal (profiling §5.1.0.8)

Revise/Add/Remove table changed: `label` gains **Remove=Yes**; `code.coding` row renamed **`code`** and gains **Revise=Yes**; `requirements` gains **Remove=Yes** (short/definition, comment, alias, example, mapping unchanged). New mechanism sentence: "To remove any of the listed properties, repeat the property in the differential with identical values and use the **`elementdefinition-suppress`** extension."

Constraint removal reworded: R5 "cannot 'remove' **mappings and** constraints … can refrain from repeating them" → R6 "cannot 'remove' constraints" + new guidance that a child profile **may rewrite `constraint.human`** (e.g. "No longer relevant based on profile constraints") when inherited invariants become vacuous. (Mappings dropped from the sentence — removal via suppress-table above.)

### 7. Additional bindings (profiling new §5.1.0.20 + eld-31/34)

New section "Additional Bindings": when deriving, an additionalBinding **can be constrained iff it has the same `key`; a keyless base additionalBinding cannot be constrained**. Constraint rules for additionalBinding uses/value sets = same as for regular bindings. Subsequent §§ renumbered (+1 through §5.1.0.27).
**Gap to note**: the §5.1.0.22→23 binding-strength lattice table (required/extensible/preferred/example) was **not** extended for the new `descriptive` strength.

### 8. Must Support (profiling §5.1.0.22→23)

Expectation documentation pointer changed **ElementDefinition.definition → ElementDefinition.comment**. New (duplicative) early statement of the child-MS/parent-not-MS rule. mustSupport newly optional in type definitions per the interpretation table (with the ♉︎ "don't actually do it" footnote). Obligation extension remains the fine-grained mechanism (now titled "Obligation Extension").

### 9. StructureDefinition page

- **Schema unchanged**: no element added/removed (R6's own change list: only `jurisdiction` value-set swap and new `fhirVersion` codes 6.0 … 6.0.0-ballot4).
- **Verified unchanged**: §5.4.6 Interpretation Notes incl. **element ordering rules**, §5.4.6.1 root-element rules, §5.4.6.3 rules for constrained types, §5.4.6.5 logical models — all byte-identical after normalization.
- **New §5.4.6.6 "Using StructureDefinitions to Define Additional Resource"** — rules for custom resources: canonical outside `http://hl7.org/fhir`; fhirVersion ≥ 6.0.0; kind=resource; abstract=false; **type = canonical URL exactly**; baseDefinition = Resource or DomainResource directly (CanonicalResource/MetadataResource adherence via the `implements` extension); derivation=specialization; root path = tail of canonical URL; compartments via `additional-resource-compartment` extension. (Its own ViewDefinition example violates the baseDefinition rule — see errata candidates.)
- Worked-examples list (§5.4.6.2): the "Constraint on a resource (Clinical Document Profile / clinicaldocument)" example was **removed**; doubled `"abstract": false` fixed; us/core baseDefinition URLs remain (erratum e).
- New profiling-machinery extensions referenced from this page: Inherit Obligations, Obligations Profile, Preferred Value Alternatives, Profile Mapping, StructureDefinition Implements, Imposes Profile, Complies With Profile.

### 10. Extension pages

- extensibility: `Extension.value[x]` adds **VirtualServiceDetail**; `url` and `value[x]` now flagged Σ. Modifier-extension rules text unchanged apart from editorial contractions.
- defining-extensions: **new Notes on context matching** — context type `element` includes **specialization logic** (context Element ⇒ all specializations; DomainResource.meta ⇒ meta on all DomainResources; **interfaces like CanonicalResource are valid contexts**); Resource is a valid root context; by convention type=element + expression "Element" = any element and any DomainResource; "all elements except the root" needs a fhirpath context. `fhirpath` context row moved after `element`/`extension`; §2.1.5.1.2 retitled "Extension Context".

### 11. Choice types [x] / renamed paths / path & id rules — verified unchanged

"Constraining elements with a choice of Type" (elementdefinition §…4.2), the path-based [x] renaming guidance, "Use of ElementDefinition.path" rules, ElementDefinition.id / type-slice id rules, "Rules about min and max", Primitive Values, Aggregation, and the path/type context table in the interpretation notes: **only punctuation/renumbering changes** (e.g. → e.g., etc.). Discriminator worked-examples table unchanged. conformance-rules.html: only editorial changes (RFC 8174 reference added), refreshed isModifier/meaningWhenMissing element lists, "Obligation Extension" naming — Guideline/bestpractice invariant machinery unchanged.

### 12. New R6-build errata candidates (found in passing, worth reporting upstream)

1. §5.4.6.6 ViewDefinition example sets `"baseDefinition": …/CanonicalResource`, violating that same section's "SHALL be a direct reference to … Resource or DomainResource".
2. eld-32 human text "no value set can **ve** provided"; eld-34 "encouraged to **but** the max value set".
3. maxLength comment: "**Tupe** Characteristics and Structure Type **Characterstics**".
4. ♉/♉︎ (Taurus emoji) used as footnote markers in the interpretation table; "Notes:♉" glued without space.
5. §5.4.6.2 closing sentence "structure definitions of type 1, 3, 5 and 8 - 9 can only be defined by the FHIR specification" not renumbered after the clinicaldocument example was deleted.
6. ElementDefinition changes-list claims `binding.additional.purpose` adds code `open`; the rendered enumeration lacks it.
7. eld-33's bindable-type list (has CodeableReference, lacks Duration) is inconsistent with eld-11's (has Duration, lacks CodeableReference).
8. Binding-strength derivation lattice (§5.1.0.23) not updated for strength `descriptive`.

---

## Job 2 — R5 errata verified against v6.0.0-ballot4

| # | Erratum (found in R5) | R6-build verdict | Evidence |
|---|---|---|---|
| a | Slice minimum sums: "must be ≤ n" vs "SHOULD be ≤ m" in adjacent bullets of the slice-cardinality rules | **STILL PRESENT** | R6 profiling txt lines 959/963 — both bullets verbatim, contradictory strength intact |
| b | Discriminator `position` defined twice with different conditions ("min=max … optional last slice undifferentiated" vs "min > 0 and min = max … last slice MAY have min != max") | **STILL PRESENT** | Both tables survive verbatim in R6 profiling (first ~line 746, second ~line 814) |
| c | Interpretation table `base` row: "required" in both Constraint-Definition columns | **FIXED (changed)** | Both cells now read **"expected"** (and the type-definition cells "snapshot: expected / differential: optional"), under the new softened Expected/Not-expected category defined in footnote ♉ |
| d | Interpretation table row uses DSTU2 name `nameReference` instead of `contentReference` | **STILL PRESENT** | R6 row: `nameReference | prohibited | optional | prohibited | optional` — verbatim from R5 |
| e | Derivation worked-examples: us/core baseDefinition URLs on Resource/CanonicalResource/Definition; doubled `"abstract": false` | **PARTIALLY FIXED** | Doubled `"abstract": false` (race example) fixed; all three `http://hl7.org/fhir/us/core/StructureDefinition/{Base,DomainResource,Base}` URLs remain (R6 txt 2248/2260/2272) |
| f1 | Broken link text `"&gt;Obligations` (§5.1.0.22→23) | **STILL PRESENT** | R6 profiling.html line 1576: `<a href="obligations.html">"&gt;Obligations</a>` |
| f2 | Typo "discrinator" (§5.1.0.13 resolve() note) | **STILL PRESENT** | R6 profiling.html line 1169 |
| f3 | Typo "mimimum" (conformance-rules §2.1.1.0.3) | **STILL PRESENT** | R6 conformance-rules txt line 196 |
| f4 | Unbalanced parenthesis, §5.1.0.7: "…(note that datatypes and resources do not define default values at all, but default values may be defined for logical models" — never closed | **STILL PRESENT** | Verbatim in R6 profiling (txt line 390). (Separate cosmetic defect `("formally called an 'Extensional' value set")` — stray quote — also unchanged) |
| g | Interpretation table missing rows for mustHaveValue, valueAlternatives, sliceIsConstraining, binding.additional, representation, orderMeaning, isModifierReason | **STILL PRESENT** | R6 row set identical to R5 except the requirements/comment/alias split; none of the seven rows added |

---

## Verified non-deltas (safe to keep citing R5 text for R6)

Slicing lattice (open/closed/openAtEnd, ordered), reslicing rules, slice-cardinality arithmetic, discriminator type table and restricted-FHIRPath subset, discriminator worked-examples, "extensions sliced by url", choice-type [x] constraint rules and renamed-path guidance, ElementDefinition.path/.id rules, min/max rules, primitive-value rules, aggregation rules, cardinality-derivation lattice, binding-strength derivation lattice (unextended), profile-element extension mechanism, §5.4.6 ordering rules ("Elements must be in the same order as the baseDefinition, and child elements appear in depth-first order."), root-element rules, rules for constrained types, logical-model rules, StructureDefinition element set, eld-2..28 and sdf-1..29 expressions (except sdf-9), absent ids eld-1/9/10 and sdf-7/12/13, snapshot/differential authoring obligations (both SHOULD be published; no new generator obligations added).
