# R5 spec extract: Profiling (profiling.html) + Conformance (conformance-rules.html)

Baseline extract for reverse-engineering snapshot generation (differential + base → snapshot). Source: FHIR v5.0.0 (R5), local copies of `spec-html/R5/profiling.html` ("5.1.0 Profiling FHIR", **Normative**) and `spec-html/R5/conformance-rules.html` ("2.1.1.0 Conformance", **Normative**). Citations: `[profiling §5.1.0.x]`, `[conformance §2.1.1.0.x]`; quote blocks / quotation marks are verbatim, the rest is summary. Chapter tags: ch1 overview/terminology; ch2 differential preprocessing; ch3 base resolution/rebasing; ch4 element matching; ch5 per-property merge; ch6 slicing; ch7 type-profile & extension expansion; ch8 contentReference; ch9 logical models & interfaces; ch10 element ids & Base; ch11 recursion; ch12 error handling & settings.

Coverage note: these two pages say **nothing** about contentReference (ch8), element-id generation / the Base component (ch10), or error handling & settings (ch12) — that material lives in `elementdefinition.html` / `structuredefinition.html`, outside this extract's sources (see "Spec gaps noticed").

---

## 1. Differential vs snapshot — definitions and tool obligations `[ch1, ch2]`

[profiling §5.1.0.11] "StructureDefinitions may contain a differential statement, a snapshot statement or both."

> "Differential statements describe only the differences that they make relative to the structure definition they constrain (which is most often the base FHIR resource or datatype)." [profiling §5.1.0.11]

> "Nothing else is stated - all the rest of the structural information is implied (note that this means that a differential profile can be sparse and only mention the elements that are changed, without having to list the full structure. This rule includes the root element - it is not needed in a sparse differential)." [profiling §5.1.0.11]

**ch2 consequence**: a generator must tolerate differentials that omit the root element (and any unconstrained elements/ancestors), and must synthesize the implied structure from the base.

> "In order to properly understand a differential structure, it must be applied to the structure definition on which it is based. In order to save tools from needing to support this operation (which is computationally intensive - and impossible if the base structure is not available), a StructureDefinition can also carry a 'snapshot' - a fully calculated form of the structure that is not dependent on any other structure. The FHIR project provides tools for the common platforms that can populate a snapshot from a differential (note that the tools generate complete verbose snapshots; they do not support suppressing mappings or constraints)." [profiling §5.1.0.11]

[profiling §5.1.0.11] "StructureDefinition resources used in operational systems should always have the snapshot view populated." The differential "serves the authoring process, while the snapshot serves the implementation tooling." A differential may leave elements unconstrained, or prohibit all optional elements (max = 0, "closes the content").

**Terminology** [profiling §5.1.0.1, §5.1.0.9]: a *Profile* is "A set of constraints on a resource represented as a structure definition with derivation = constraint". A "constraint" StructureDefinition "specifies a set of restrictions on the content of a FHIR resource or datatype, or an additional set of constraints on an existing profile." Identified by canonical URL, "which SHOULD be the URL at which it is published."

**Element list encoding** [profiling §5.1.0.9]: "A structure definition contains a linear list of element definitions. The inherent nested structure of the elements is derived from the path value of each element." … "The structure is coherent - children are never implied, and the path statements are always in order."

---

## 2. What a constraining profile may / may not do `[ch1, ch5]`

### 2.1 General limits [profiling §5.1.0.7] `[ch1]`

- "Profiles cannot break the rules established in the base specification (e.g. cardinality as described above)"
- "Profiles cannot specify default values or meanings for elements defined in the base specification (note that datatypes and resources do not define default values at all, but default values may be defined for logical models" *(unbalanced parenthesis in source)* — the logical-model clause is the only ch9-relevant statement on these pages.
- "Profiles cannot change the name of elements defined in the base specification, or add new elements"
- "It must be safe to process a resource without knowing the profile" — profile-mandated behavior that cannot be ignored must be expressed via a modifier extension; "knowledge must be explicit in the instance, not implicit in the profile."

### 2.2 The master constraint rule [profiling §5.1.0.9] `[ch5]`

> "Any changed definitions SHALL be restrictions that are consistent with the rules defined in the resource in the FHIR Specification from which the profile is derived. Note that some of these restrictions can be enforced by tooling (and are by the FHIR tooling), but others (e.g. alignment of changes to descriptive text) cannot be automatically enforced." [profiling §5.1.0.9]

> "Note that structure definitions cannot 'remove' mappings and constraints that are defined in the base structure, but for purposes of clarity, they can refrain from repeating them." [profiling §5.1.0.9]

**ch5 consequence (critical)**: omission in a differential is *never* removal. A generator merging a differential element onto its base must carry forward base mappings/constraints even when the differential is silent; when the differential repeats or extends them, the merge must be additive.

Statements a constraint profile can make [profiling §5.1.0.9] (paraphrased): restrict cardinality; rule out an element (max = 0); fix an element to a single value; add FHIRPath constraints; restrict the types of a choice element; require a typed element or reference target to conform to another profile; rebind to a different value set; refine definitions/comments/examples; add or refine mappings; declare mustSupport. On mappings, verbatim: "providing a new mapping with the same identity … means that the new mapping replaces a mapping with the same identity in the element being profiled" — **the one property with explicit replace-by-key merge semantics**.

### 2.3 Cardinality [profiling §5.1.0.6] `[ch5]`

"A profile can restrict the cardinality of an element within the limits of the base structure it is constraining." Allowed derived ranges per base:

| base \ derived | 0..0 | 0..1 | 0..n | 1..1 | 1..n |
|---|---|---|---|---|---|
| 0..1 | yes | yes | no | yes | no |
| 0..* | yes | yes | yes | yes | yes |
| 1..1 | no | no | no | yes | no |
| 1..* | no | no | no | yes | yes |

> "When a profile is constraining another profile where there are more cardinality options (e.g. low is not just 0 or 1, and high is not just 1 or *), the same principles still apply: the constraining profile can only allow what the base profile allows." [profiling §5.1.0.6]

I.e. general rule: derived [min,max] must be a sub-range of base [min,max]. Narrowing x..\* to x..1 does not change JSON representation (still an array) [profiling §5.1.0.6].

Cardinality semantics [conformance §2.1.1.0.3]: the base spec "only defines the following cardinalities: 0..1, 0..\*, 1..1, and 1..\*. Profiles that describe specific use cases may use other values for cardinality within the limits of the cardinality defined by the base resource." Elements can never be empty ("they SHALL have a value attribute, child elements, or extensions"), so min=1 alone does not guarantee data — FHIRPath constraints are needed for that. Order of repeats has no meaning unless the definition sets `ElementDefinition.orderMeaning`; "it is not possible to define a meaning for the order of the elements in a profile using a StructureDefinition."

### 2.4 Descriptive elements [profiling §5.1.0.8] `[ch5]`

"The meaning and guidance provided in the base resource or profile can't be invalidated, only constrained or contextualized." "Removing an element is only appropriate if the element no longer applies in the context of the constraints/domain space of the profile." Allowed changes per property (verbatim table):

| Element | Revise? | Add? | Remove? |
|---|---|---|---|
| label | Yes | | |
| code.coding | | Yes | Yes |
| short | Yes | | |
| definition | Yes | | |
| comment | Yes | Yes | Yes |
| requirements | Yes | Yes | |
| alias | Yes | Yes | Yes |
| example | Yes | Yes | Yes |
| mapping | Yes | Yes | Yes |

This is the closest the spec comes to per-property merge semantics — but it describes what a *profile author* may express, not how a *generator* merges (e.g. does a differential `alias` list replace or append to the base list? unstated; see gaps).

### 2.5 Bindings [profiling §5.1.0.19–21] `[ch5]`

Binding strength changes (base down, derived across) [profiling §5.1.0.21]:

| base \ derived | required | extensible | preferred | example |
|---|---|---|---|---|
| required | yes | no | no | no |
| extensible | yes | yes | no | no |
| preferred | yes | yes | yes | no |
| example | yes | yes | yes | yes |

> "Note that a constraining profile may leave the binding strength the same and change the value set instead. Whatever the constraining profile does, it cannot make codes valid that are invalid in the base profile." [profiling §5.1.0.21]

Value-set customization per base strength [profiling §5.1.0.20]: required → the new value set "can only contain codes contained in the value set specified by the FHIR specification"; extensible → may add codes that "SHOULD NOT have the same meaning as existing codes in the base value set"; preferred/example → "whatever is appropriate for local use".

### 2.6 mustSupport / obligations [profiling §5.1.0.22; conformance §2.1.1.0.5] `[ch5]`

mustSupport "can be declared on profiles but not on resource or datatype definitions"; it is "distinct from cardinality". "The meaning of 'support' is not defined by the base FHIR specification"; a profile setting it true "SHALL also make clear exactly what kind of 'support' is required" — via detailed Obligations (the spec HTML's own link text here is corrupted: `">Obligations`) or via definition/description text.

> "If creating a profile based on another profile, Must Support can be changed from false to true, but cannot be changed from true to false. Obligations can be added, particularly for different actors, but existing actor obligations cannot be undone or loosened." [profiling §5.1.0.22]

Interpretation notes [profiling §5.1.0.22]: elements newly marked mustSupport take that term's meaning from the new IG/profile; a mustSupport child of a non-mustSupport parent binds only systems that support the parent; a mustSupport complex element without mustSupport children implies support for "at least a subset of the child elements". IsModifier does not imply mustSupport. [conformance §2.1.1.0.5] Application behavior rules are made "by using the mustSupport property of ElementDefinition in profiles, and the Obligation extension"; the base specification itself sets no application obligations.

### 2.7 isModifier [conformance §2.1.1.0.4] `[ch5]`

> "Every element in the base resource has a value of 'true' or 'false' for the Is-Modifier flag. The value of the flag cannot be changed by profiles on the resource, in either direction. When a StructureDefinition defines an extension, it labels the extension with the Is-Modifier flag, and this cannot be changed in other profiles." [conformance §2.1.1.0.4]

Also stated as: "Whether an element is a modifier cannot be changed when element usage is described in a constraining Structure Definition." Definition: an element is a modifier "if and only if it cannot be safely ignored because its value, or its meaning if missing, may cause the interpretation of the containing element or one of its descendants to no longer conform to the stated definition". Modifier extensions serialize as `modifierExtension`. **ch5 consequence**: propagate base isModifier/isModifierReason unchanged; a differential flipping it is invalid.

### 2.8 Constraints / invariants [conformance §2.1.1.0.6] `[ch5]`

Constraint properties: Key ("Identifies the constraint uniquely amongst all the constraints in the context"), Requirements, Severity, Suppress ("Allows for suppress warnings or best practice guidelines in derived profiles"), Human Description, Expression (FHIRPath, "must evaluate to true when run on the element"), Source ("The URL of the profile that defined the constraint"). Severity levels: **Error (rule)** — "A rule that all resources must conform to"; **Warning** — resource "is considered valid and can be processed normally"; **Guideline** — a warning marked with the `elementdefinition-bestpractice` extension, treated as error when best-practice enforcement is requested.

> "Profiles may define additional constraints that apply to an element, but they cannot alter or remove constraints that are already applied." [conformance §2.1.1.0.6]

**ch5 consequence**: constraints merge additively (union of base + differential). Elements "can also be explicitly associated with constraints defined elsewhere" — informational only.

> "The constraint keys are required to be unique when they are declared. However constraints are repeated throughout profiles where ever the original element definitions, which means that there's no simple way to constrain the keys to be unique. FHIR validators will validate that constraint keys are meaningfully unique as they can." [conformance §2.1.1.0.6]

### 2.9 Other ElementDefinition metadata [conformance §2.1.1.0.7] `[ch5]`

Listed without profiling rules: `isSummary` (summary-search behavior), `meaningWhenMissing`, `maxLength` ("How long the representation of the primitive value is allowed to be (not including XML or JSON escaping)"). No statement on whether a profile may change these — see gaps.

---

## 3. Slicing `[ch6]`

### 3.1 Concept [profiling §5.1.0.12]

Slicing = taking "an element that may occur more than once (e.g. in a list)" and splitting "the list into a series of sub-lists, each with different restrictions". Slice names ("systolic" etc.) "are never exchanged" — serialization is unaltered by the profile.

### 3.2 Discriminators [profiling §5.1.0.13]

"a sliced element can designate a field or set of fields that act as a 'discriminator' used to tell the slices apart."

> "When a constraining structure designates one or more discriminators, it SHALL ensure that the possible values for each slice are different and non-overlapping, so that the slices can easily be distinguished." [profiling §5.1.0.13]

Each discriminator = (type, restricted-FHIRPath path). **Six** types in R5 (yes, `position` exists):

- **value** — "The slices have different values in the nominated element, as determined by the applicable fixed value, pattern, or required ValueSet binding."
- **exists** — "The slices are differentiated by the presence or absence of the nominated element. There SHALL be no more than two slices. The slices are differentiated by the fact that one must have a max of 0 and the other must have a min of 1 (or more). The order in which the slices are declared doesn't matter."
- **pattern** — "This has the same meaning as 'value' and is deprecated." (retained for backwards compatibility)
- **type** — "The slices are differentiated by type of the nominated element."
- **profile** — "The slices are differentiated by conformance of the nominated element to a specified profile. Note that if the path specifies .resolve() then the profile is the target profile on the reference." (">1000-fold" processing cost; "use this only where absolutely required")
- **position** — "The slices are differentiated by their index. This is only possible if all but the last slice have min=max cardinality, and the (optional) last slice contains other undifferentiated elements." The notes restate it as: "only possible if all but the last slice have a fixed cardinality where min > 0 and min = max. The last slice MAY have min != max." *(the two formulations differ — see gaps)*

Restricted discriminator FHIRPath may contain only: element selections (no "()"), `extension(url)`, `resolve()`, `ofType()` [profiling §5.1.0.13].

Slice element definitions must back the discriminator: "If the type is value, or pattern, then the element definition must use either: ElementDefinition.fixed[x], or ElementDefinition.pattern[x], or if the element has a terminology binding, a required binding with a Value Set that enumerates the list of possible codes" (extensional) [profiling §5.1.0.13].

"It is the composite (combined) values of the discriminators that are unique, not each discriminator alone." Discriminators are optional but their absence is "discouraged" (content "very difficult to process"). type/profile discriminators also work "where a repeating element contains a resource directly (e.g. DomainResource.contained, Bundle.entry, Parameters.parameter.resource)".

`resolve()` note: path elements past `resolve()` refer to the referenced resource "as constrained by the applicable targetProfile (regardless of the discrinator type)" *(sic)*; the fixed value/pattern lives in the profile the targetProfile points to; the targetProfile is declared on the element immediately preceding `resolve()` — "the final element that the slice declares."

### 3.3 Slice group structure [profiling §5.1.0.13] `[ch4, ch6]`

"a slice is defined using multiple element entries that share a path but have distinct names. These entries together form a 'slice group' that is:"

- **Initiated by a "slicing entry"**: "the first element in a slice group must contain a slicing property that defines the discriminator for all members of the group. It also contains the unconstrained definition of the element that is sliced, potentially including children of the unconstrained element, if there are any"
- **Mutually exclusive**: "each element in a slice group SHALL describe a distinct set of values for the group's discriminators … an element in a resource instance will never match more than one element in a given slice group. If no discriminators are named, it SHOULD still be possible to differentiate the slices based on their properties"
- **Serialized as a group**: "The entries in a slice group must be adjacent in a serialized structure definition, or, if there are any intervening elements, those elements must be 'compatible with' the group. Concretely, this means that any intervening elements must have a path that starts with the slice group's path." (e.g. `Observation.name.extension` does not break up a group at `Observation.name`)

**ch4 consequence**: matching relies on path + sliceName + document order within adjacency; the spec gives structural rules, no matching algorithm.

### 3.4 Slice cardinality [profiling §5.1.0.14] `[ch6]`

"When an element of a fixed cardinality m..n is sliced, the following rules apply:" (verbatim bullets)

- "The maximum cardinality on each slice cannot exceed n"
- "The sum of the maximum cardinalities can be larger than n"
- "The sum of the minimum cardinalities must be less or equal to n"
- "Each individual slice can have a minimum cardinality of 0 (less than m - the only situation where this is allowed), but the total number of elements in the instance must still be greater or equal to m"
- "The sum of the minimum cardinalities of the slices SHOULD be less than or equal to m. The cardinality of the sum of the slice minimums must be met as well as the minimum on the base element."

*(bullets 3 and 5 conflict — "must be ≤ n" vs "SHOULD be ≤ m" — see gaps)*. Key takeaway for validation of generated snapshots: slice min may drop below element min; slice max may not exceed element max.

### 3.5 Default slice [profiling §5.1.0.15] `[ch6]`

- "It is identified because the name of the slice is @default. The sliceName '@default' is reserved and cannot be used in any other context"
- "Default slices are only allowed when the slicing rule = closed"
- "Default slices must not fix the value of the discriminator elements"
- "Default slices can be re-sliced in dependent profiles" (reslice naming applies: "@default/@default" [profiling §5.1.0.17])

Purpose: rules for "all of the remaining content that is not in one of the defined slices".

### 3.6 Reslicing and constraining existing slicing [profiling §5.1.0.17] `[ch6, ch3]`

"When a profile constrains another profile, it can make additional constraints, including extending the discriminator, adding new slices (if the slices are not already closed), and slicing inside the existing slices."

Rules for constraining `ElementDefinition.slicing` (verbatim):

- "ElementDefinition.slicing.rule can be constrained from open to closed"
- "ElementDefinition.slicing.ordered can be constrained from false to true"
- "If a discriminator for an element is declared in a parent profile, child profiles referencing that element: SHALL include all the same discriminators; MAY add additional discriminators"

*(Note: `openAtEnd` is a legal value of slicing.rules in the ElementDefinition datatype, but this page never mentions it or its constraint lattice — see gaps.)*

Derived-profile options on a sliced element X (profile B derives from A; C from B) [profiling §5.1.0.17]:

- constraint on X with **no sliceName** → "the profile is adding constraints to all slices of X"
- constraint on X with **a sliceName** → constrains only that slice (new slice if name is new; further constraints if name matches parent's slice)
- both are allowed simultaneously; "Note that it is possible for Profile C to make rules that are incompatible with profile B, in which case there is no set of instances that can be valid against profile C"

Reslicing names: "This is done by separating the names using '/'. For example, if Profile B defines the slice 'example', and profile C defines the slice 'example/example1', then this is deemed to be 'example1' slice of the example slice. This process can continue indefinitely by separating each layer of slicing names with the '/' character."

Slice names must be "unique across the set of slices in the profile" [profiling §5.1.0.17].

### 3.7 Extension slicing [profiling §5.1.0.13 note, §5.1.0.18.1] `[ch6, ch7]`

> "Note that extensions are always sliced by the url element, though they may be resliced on additional elements where required." [profiling §5.1.0.13]

"To prescribe the use of an extension in an instance, the extension list on the resource needs to be sliced." [profiling §5.1.0.18.1] Discriminator example table: context `List.entry.extension`, type `value`, path `url` — "Extensions are differentiated by the value of their url property (usually how extensions are sliced)".

**ch7 consequence**: generators conventionally auto-materialize `type=value, path=url` slicing entries on extension elements; the spec states the convention but not the generator obligation.

---

## 4. Extension definition & profiling `[ch7]`

[profiling §5.1.0.18] An extension definition defines the identifying URL, the allowed contexts, "and then defines the extension element using the same details used to profile the structural elements that are part of resources" — one definition reusable across resources/datatypes.

> "Note that the minimum cardinality of an extension SHALL be a valid restriction on the minimum cardinality in the definition of the extension. If the minimum cardinality of the extension is 1 when it is defined, it can only be mandatory when it is added to a profile. This is not recommended - the minimum cardinality of an extension should usually be 0." [profiling §5.1.0.18.1]

**ch7 consequence**: the extension SD's own element constraints act as the effective base for the extension slice. Extensions may be used in instances without any profile mentioning them; profiles only "describe how an extension is used" [profiling §5.1.0.18.1]. Modifier extensions: flag is set in the defining SD and "cannot be changed in other profiles" [conformance §2.1.1.0.4].

---

## 5. Profiling portions of a resource (profile-element extension) `[ch7]`

[profiling §5.1.0.16] A profile can be applied starting at a nominated element rather than the root, via the `http://hl7.org/fhir/StructureDefinition/elementdefinition-profile-element` extension on `ElementDefinition.type.profile`, whose `valueString` is an element **id** in the target SD (e.g. `Composition.section:codeB`): "an instruction to a validator to apply the profile starting at the nominated element (by its ID)." Used for e.g. Composition section libraries (abstract SDs of reusable slices). **ch7 consequence**: when expanding `type.profile`, a generator must rebase the referenced SD's subtree at the nominated id instead of the root.

---

## 6. Recursive elements `[ch11]`

> "Some backbone elements recurse. E.g. Questionnaire.item. When a profile defines constraints on such elements, the constraints apply to the recursive references to those elements as well. I.e. If Questionnaire.item is constrained to have a type of 'group', that will cause Questionnaire.item.item, Questionnaire.item.item.item, etc. to all have the same constraint." [profiling §5.1.0.10]

Level-specific constraints on recursive items must be expressed as FHIRPath invariants limiting themselves "to only the root or other specific levels of nesting" [profiling §5.1.0.10]. *(Note: recursion here is realized via contentReference in the SD encoding, but this page never says so — ch8 material is absent from these pages.)*

---

## 7. Derivation chains `[ch3]`

[profiling §5.1.0.17] Profiles derive from profiles arbitrarily deep ("Re-profiling"; complexity warning only, no limit) — a generator must recursively ensure the base's snapshot exists (implied, not stated; see gaps).

---

## Spec gaps noticed

Gaps an implementer of snapshot generation must fill that these two (normative!) pages do not:

1. **No merge algorithm at all.** The spec's entire statement of snapshot generation is that tools "can populate a snapshot from a differential" and that generated snapshots are "complete verbose" [profiling §5.1.0.11]. There is no per-property specification of replace vs. append vs. keep-base for any ElementDefinition property (short, definition, alias, example, condition, code, constraint, mapping, minValue/maxValue, maxLength, mustHaveValue, valueAlternatives, …). Only two properties get explicit semantics: **mapping** (replace-by-identity [profiling §5.1.0.9]) and **constraint** (additive, cannot alter/remove [conformance §2.1.1.0.6]). The §5.1.0.8 Revise/Add/Remove table constrains what an author may express, not how a tool merges (e.g. differential `alias` list: replacement of, or union with, the base list? unstated).
2. **No element-matching algorithm** `[ch4]`. How a differential entry is paired with its base element — by path, by id, by sliceName; how choice-type renames (`value[x]` vs `valueQuantity`) match; how ambiguity is resolved — is not defined on these pages. Only the slice-group adjacency/"compatible with" rule [profiling §5.1.0.13] hints at ordering expectations.
3. **Internal contradiction on slice minimum sums** [profiling §5.1.0.14]: bullet 3 says the sum of slice minimum cardinalities "must be less or equal to **n**" (the max) while bullet 5 says it "SHOULD be less than or equal to **m**" (the min) — different bound, different conformance strength. An implementation must pick an interpretation.
4. **Position discriminator defined twice, inconsistently** [profiling §5.1.0.13]: the table says all-but-last slices need "min=max cardinality"; the notes add "min > 0". Also nothing about how position interacts with `slicing.ordered`.
5. **`openAtEnd` and `ordered` under-specified**: the constraint lattice in [profiling §5.1.0.17] covers only open→closed and ordered false→true. Whether open→openAtEnd, openAtEnd→closed etc. are legal is unstated on these pages.
6. **Slicing entry content in the snapshot**: §5.1.0.13 says the slicing entry "contains the unconstrained definition of the element that is sliced, potentially including children" — but not whether/how the generator must emit the slicing entry's children plus each slice's children in the snapshot, nor how a differential that constrains "all slices of X" (no sliceName [profiling §5.1.0.17]) is merged into each existing slice of the base.
7. **Base already sliced**: beyond §5.1.0.17's author-level rules, nothing on how a generator merges a differential onto a base whose snapshot already contains slice groups (matching resliced names, inheriting the parent's slicing entry, default-slice expansion).
8. **contentReference (ch8), element-id generation and the ElementDefinition.base component (ch10)**: entirely absent from these two pages. §5.1.0.16 assumes stable element ids and §5.1.0.10 assumes recursion semantics, but the rules live in `elementdefinition.html` / `structuredefinition.html` (a scoping boundary of this extract) — the *profiling* page itself never tells a generator to populate `base`, ids, or expand contentReference targets.
9. **Type-profile expansion** `[ch7]`: nothing on whether/when a generator expands children of an element from its type's SD or from `type.profile` (other than the profile-element start-point rule §5.1.0.16), nor how declared type profiles interact with inherited children.
10. **Logical models & interfaces (ch9)**: only one clause — logical models may define default values [profiling §5.1.0.7]. Nothing on `derivation = specialization` semantics, interface-based derivation, or type slicing in logical models.
11. **Error handling (ch12)**: no guidance on generator behavior for invalid differentials (illegal cardinality widening, unknown paths, discriminator violations): whether to fail, warn, or produce a best-effort snapshot. "Note that it is possible for Profile C to make rules that are incompatible with profile B" [profiling §5.1.0.17] acknowledges unsatisfiable profiles without prescribing tool behavior.
12. **Descriptive-text validity is explicitly unenforceable**: "others (e.g. alignment of changes to descriptive text) cannot be automatically enforced" [profiling §5.1.0.9] — the spec concedes tooling cannot validate parts of the constraint rule it imposes.
13. **isSummary / meaningWhenMissing / maxLength** [conformance §2.1.1.0.7]: listed as metadata with no statement about profile-changeability (contrast isModifier's explicit freeze), leaving merge/override rules to the ElementDefinition page.
14. **Source-text defects when citing**: the broken link text `">Obligations` in §5.1.0.22, the unbalanced parenthesis in §5.1.0.7, and typos "discrinator" (§5.1.0.13) / "mimimum" (§2.1.1.0.3) are all present in the published R5 HTML itself.
