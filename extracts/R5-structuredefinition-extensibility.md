# R5 spec baseline: structuredefinition.html + extensibility.html

Extracted 2026-08-21 from local copies of the FHIR R5 (v5.0.0) specification:

- `spec-html/R5/structuredefinition.html` — "5.4 Resource StructureDefinition - Content" (Maturity N, **Normative** from v4.0.0)
- `spec-html/R5/extensibility.html` — "2.1.5.0 Extensibility" (**Normative**)

Citations: `[structuredefinition §5.4.x]` and `[extensibility §2.1.5.0.x]` use the pages' own heading
numbers. Verbatim quotes are CC0 (FHIR spec license). Blocks are tagged with the target chapters of the
snapshot-generation write-up: `ch1` overview, `ch2` differential preprocessing, `ch3` base
resolution/rebasing, `ch6` slicing, `ch7` type-profile & extension expansion, `ch9` logical models &
interfaces, `ch10` element ids & Base, `ch11` recursion, `ch12` errors.

---

## 1. What snapshot and differential ARE `[ch1]`

The element-definition texts (UML pane, §5.4.5) give the two one-line definitions everything else builds on:

> "A snapshot view is expressed in a standalone form that can be used and interpreted without considering
> the base StructureDefinition" [structuredefinition §5.4.5, definition of `snapshot`]

> "A differential view is expressed relative to the base StructureDefinition - a statement of differences
> that it applies" [structuredefinition §5.4.5, definition of `differential`]

Cardinalities: `snapshot 0..1`, `differential 0..1`, each with `element : ElementDefinition [1..*]`.
Both are optional individually, but sdf-6 (below) requires at least one of the two.

> "A structure is represented as a flat list of elements. The element.path provides the overall
> structure." [structuredefinition §5.4.6]

> "Differentials in constraints need only specify elements that they are making rules about. Other
> elements can be inferred as defined in the base resource" [structuredefinition §5.4.6]

That last sentence is the entire mandate for a snapshot generator: everything not mentioned in the
differential is inherited from the base. The page never says *how* the inference works — see Gaps.

There is **no mention of the `$snapshot` operation anywhere on this page** and no statement about tools
generating snapshots; the page's "Operations" tab links to a separate page (`structuredefinition-operations.html`,
not in the extraction scope). See Gaps.

## 2. Derivation semantics: kind / derivation / type / baseDefinition / abstract / url `[ch1, ch3]`

Element facts (§5.4.5 structure table):

| element | card | meaning |
|---|---|---|
| `kind` | 1..1 | `primitive-type \| complex-type \| resource \| logical` (required binding StructureDefinitionKind) |
| `abstract` | 1..1 | whether the structure can be instantiated |
| `type` | 1..1 | "Type defined or constrained by this structure" (uri; extensible binding All FHIR Types) |
| `baseDefinition` | 0..1 | `canonical(StructureDefinition)` — "Definition that this type is constrained/specialized from" |
| `derivation` | 0..1 | `specialization \| constraint` (required binding TypeDerivationRule) |
| `url` | 1..1 | canonical identifier of this SD itself |

The load-bearing normative text on `type` (§5.4.5 UML pane):

> "The type this structure describes. If the derivation kind is 'specialization' then this is the master
> definition for a type, and there is always one of these (a data type, an extension, a resource,
> including abstract ones). Otherwise the structure definition is a constraint on the stated type (and in
> this case, the type cannot be an abstract type). References are URLs that are relative to
> http://hl7.org/fhir/StructureDefinition e.g. \"string\" is a reference to
> http://hl7.org/fhir/StructureDefinition/string. Absolute URLs are only allowed in logical models, where
> they are required" [structuredefinition §5.4.5, definition of `type`]

On `abstract`:

> "Whether structure this definition describes is abstract or not - that is, whether the structure is not
> intended to be instantiated. For Resources and Data types, abstract types will never be exchanged
> between systems" [structuredefinition §5.4.5, definition of `abstract`]

Consequences for a generator:

- **specialization** (a new type): `type` names the *new* type; element paths in the differential are
  rooted at the new type name, while the base's paths are rooted at the base type name — so
  specialization implies **rebasing paths** from base-type-rooted to new-type-rooted `[ch3]`. (The page
  states the identity `type = snapshot.element.first().path` via sdf-11 but never describes the renaming
  step — see Gaps.)
- **constraint** (a profile): `type` names the *constrained* type and equals the base's type; paths in
  differential and snapshot stay rooted at that type. The constrained type cannot be abstract.
- `abstract = true` permits `baseDefinition` to be absent (sdf-4); only spec-level roots (`Base`,
  `Element`) have no base. If `baseDefinition` exists, `derivation` is mandatory (sdf-27).

§5.4.6.2 "Different Uses for StructureDefinition" enumerates the controlling pattern
(`url` / `kind` / `type` / `abstract` / `baseDefinition` / `derivation`) with 9 worked examples `[ch1]`:

1. Base data type: Quantity — `kind: complex-type`, base `Element` (no `type` shown in the example, i.e. spec-defined master definitions of that era; in the real artifacts `type` is always present since it is 1..1).
2. Constrained data type: Money — `kind: complex-type`, `type: Quantity`, base `.../Quantity`.
3. Base resource: Patient — `kind: resource`, base `DomainResource`.
4. Constraint on a resource (profile): clinicaldocument — `kind: resource`, `type: Composition`, base `.../Composition`.
5. Base Extension type: Extension — `kind: complex-type`, base `Element`.
6. A defined extension: data-absent-reason — `kind: complex-type`, `type: Extension`, base `http://hl7.org/fhir/StructureDefinition/Extension`.
7. A constraint on a defined extension (profile-of-extension) — `kind: complex-type`, `type: Extension`, base = the *extension's* url (example: base `http://hl7.org/fhir/us/core/StructureDefinition/us-core-race`). "no examples currently defined" in the spec. (The printed example carries a doubled `"abstract" : false, "abstract" : false` [sic] — a published typo.)
8. Base abstract resource: Resource — `kind: resource`, `abstract: true`, base `http://hl7.org/fhir/us/core/StructureDefinition/Base` [sic].
9. Interface: CanonicalResource — `kind: resource`, `abstract: true`, `type: CanonicalResource`, base `http://hl7.org/fhir/us/core/StructureDefinition/DomainResource` [sic]. Plus a logical model example: Definition — `kind: logical`, base `http://hl7.org/fhir/us/core/StructureDefinition/Base` [sic].

> **WARNING [ch12]:** the three `[sic]` baseDefinitions above are *published spec typos* (verified present
> in the source HTML, lines ~986/999/1012): `us/core` does not belong in those canonicals — the real
> artifacts use `http://hl7.org/fhir/StructureDefinition/Base` / `.../DomainResource`. A generator or test
> harness must not treat these narrative examples as ground truth.

> "On this list, structure definitions of type 1, 3, 5 and 8 - 9 can only be defined by the FHIR
> specification itself. The other kinds of structure definitions are (or may be) created by the
> specification but can also be defined by other implementers." [structuredefinition §5.4.6.2]

§5.4.6.3 Rules for Constrained Types is a single sentence deferring everything:

> "When the structure is a constraint (derivation = constraint), see Extending and Restricting Resources
> for the rules that apply." [structuredefinition §5.4.6.3]

(i.e. `profiling.html` — the actual constraint-merge semantics are NOT on this page.)

## 3. The sdf-* invariants (§5.4.5.2 Constraints) — complete table

Quoted verbatim (description + FHIRPath expression). This is the *only* machine-checkable definition on
the page of what a valid snapshot/differential looks like. Note the numbering gaps: **sdf-7, sdf-12 and
sdf-13 do not appear in the R5 table** (retired keys; recorded here as absent, nothing more).

### Snapshot well-formedness — what a generator must produce `[ch3, ch10, ch12]`

| Key | Level | Location | Description | Expression |
|---|---|---|---|---|
| sdf-1 | Rule | (base) | "Element paths must be unique unless the structure is a constraint" | `derivation = 'constraint' or snapshot.element.select(path).isDistinct()` |
| sdf-3 | Rule | snapshot | "Each element definition in a snapshot must have a formal definition and cardinalities, unless model is a logical model" | `%resource.kind = 'logical' or element.all(definition.exists() and min.exists() and max.exists())` |
| sdf-8 | Rule | snapshot | "All snapshot elements must start with the StructureDefinition's specified type for non-logical models, or with the same type name for logical models" | `(%resource.kind = 'logical' or element.first().path = %resource.type) and element.tail().all(path.startsWith(%resource.snapshot.element.first().path&'.'))` |
| sdf-8b | Rule | snapshot | "All snapshot elements must have a base definition" | `element.all(base.exists())` |
| sdf-11 | Rule | (base) | "If there's a type, its content must match the path name in the first element of a snapshot" | `kind != 'logical' implies snapshot.empty() or snapshot.element.first().path = type` |
| sdf-15 | Rule | (base) | "The first element in a snapshot has no type unless model is a logical model." | `kind!='logical' implies snapshot.element.first().type.empty()` |
| sdf-24 | Rule | snapshot | "For CodeableReference elements, target profiles must be listed on the CodeableReference, not the CodeableReference.reference" | `element.where(type.where(code='Reference').exists() and path.endsWith('.reference') and type.targetProfile.exists() and (path.substring(0,$this.path.length()-10) in %context.element.where(type.where(code='CodeableReference').exists()).path)).exists().not()` |
| sdf-25 | Rule | snapshot | "For CodeableReference elements, bindings must be listed on the CodeableReference, not the CodeableReference.concept" | `element.where(type.where(code='CodeableConcept').exists() and path.endsWith('.concept') and binding.exists() and (path.substring(0,$this.path.length()-8) in %context.element.where(type.where(code='CodeableReference').exists()).path)).exists().not()` |
| sdf-26 | Guideline | snapshot | "The root element of a profile should not have mustSupport = true" | `$this.where(element[0].mustSupport='true').exists().not()` — rationale given: "mustSupport should always be determined by the element referencing a type. The designer of a StructureDefinition cannot know all circumstances in which a type or profile might be used" |
| sdf-28 | Rule | snapshot.element | "If there are no discriminators, there must be a definition" | `slicing.exists().not() or (slicing.discriminator.exists() or slicing.description.exists())` `[ch6]` |
| sdf-10 | Rule | snapshot.element | "provide either a binding reference or a description (or both)" | `binding.empty() or binding.valueSet.exists() or binding.description.exists()` |

Reading of sdf-3 + sdf-8b for a generator: every produced snapshot element must carry `definition`,
`min`, `max` and `base` (path/min/max) — i.e. merge must fill these even when the differential is silent,
except in logical models where sdf-3 is relaxed. Reading of sdf-15 + sdf-11 + sdf-8: for non-logical
models the snapshot root element has path == `type` and **no `type` component**; every other element path
starts with `root.path + '.'`.

### Differential well-formedness — what a generator may assume of its input `[ch2, ch12]`

| Key | Level | Location | Description | Expression |
|---|---|---|---|---|
| sdf-6 | Rule | (base) | "A structure must have either a differential, or a snapshot (or both)" | `snapshot.exists() or differential.exists()` |
| sdf-8a | Rule | differential | "In any differential, all the elements must start with the StructureDefinition's specified type for non-logical models, or with the same type name for logical models" | `(%resource.kind = 'logical' or element.first().path.startsWith(%resource.type)) and (element.tail().empty() or element.tail().all(path.startsWith(%resource.differential.element.first().path.replaceMatches('\\..*','')&'.')))` — note: the differential's first element need only *start with* the type (a sparse differential may begin at a child path, e.g. `Patient.identifier`) |
| sdf-15a | Rule | (base) | "If the first element in a differential has no \".\" in the path and it's not a logical model, it has no type" | `(kind!='logical' and differential.element.first().path.contains('.').not()) implies differential.element.first().type.empty()` |
| sdf-20 | Rule | differential | "No slicing on the root element" | `element.where(path.contains('.').not()).slicing.empty()` `[ch6]` |
| sdf-23 | Rule | (base) | "No slice name on root" | `(snapshot \| differential).element.all(path.contains('.').not() implies sliceName.empty())` `[ch6]` |
| sdf-9 | Rule | (base) | "In any snapshot or differential, no label, code or requirements on an element without a \".\" in the path (e.g. the first element)" | `children().element.where(path.contains('.').not()).label.empty() and ...code.empty() and ...requirements.empty()` |

### Element ids `[ch10]`

| Key | Level | Location | Description | Expression |
|---|---|---|---|---|
| sdf-14 | Rule | (base) | "All element definitions must have an id" | `snapshot.element.all(id.exists()) and differential.element.all(id.exists())` |
| sdf-16 | Rule | (base) | "All element definitions must have unique ids (snapshot)" | `snapshot.element.all(id.exists()) and snapshot.element.id.trace('ids').isDistinct()` |
| sdf-17 | Rule | (base) | "All element definitions must have unique ids (diff)" | `differential.element.all(id.exists()) and differential.element.id.trace('ids').isDistinct()` |

**This page contains NO rule for how element ids are constructed** (no `path:sliceName` convention, no
dotted-segments grammar) — only that ids exist and are unique. The generation algorithm for ids lives on
`elementdefinition.html` ("Element IDs" section, in the local corpus, separate extraction).

### Derivation bookkeeping / metadata rules `[ch1, ch3]`

| Key | Level | Location | Description | Expression |
|---|---|---|---|---|
| sdf-4 | Rule | (base) | "If the structure is not abstract, then there SHALL be a baseDefinition" | `abstract = true or baseDefinition.exists()` |
| sdf-27 | Rule | (base) | "If there's a base definition, there must be a derivation" | `baseDefinition.exists() implies derivation.exists()` |
| sdf-5 | Rule | (base) | "If the structure defines an extension then the structure must have context information" | `type != 'Extension' or derivation = 'specialization' or (context.exists())` `[ch7]` |
| sdf-18 | Rule | (base) | "Context Invariants can only be used for extensions" | `contextInvariant.exists() implies type = 'Extension'` `[ch7]` |
| sdf-21 | Rule | (base) | "Default values can only be specified on specializations" | `differential.element.defaultValue.exists() implies (derivation = 'specialization')` |
| sdf-22 | Rule | (base) | "FHIR Specification models never have default values" | `url.startsWith('http://hl7.org/fhir/StructureDefinition') implies (snapshot.element.defaultValue.empty() and differential.element.defaultValue.empty())` |
| sdf-19 | Rule | (base) | "FHIR Specification models only use FHIR defined types" | `url.startsWith('http://hl7.org/fhir/StructureDefinition') implies (differential \| snapshot).element.type.code.all(matches('^[a-zA-Z0-9]+$') or matches('^http:\\/\\/hl7\\.org\\/fhirpath\\/System\\.[A-Z][A-Za-z]+$'))` `[ch9]` — note it *permits* FHIRPath System.* type urls |
| sdf-29 | Warning | (base) | "Elements in Resources must have a min cardinality or 0 or 1 and a max cardinality of 1 or *" | `((kind in 'resource' \| 'complex-type') and (derivation = 'specialization')) implies differential.element.where((min != 0 and min != 1) or (max != '1' and max != '*')).empty()` |
| sdf-2 | Rule | mapping | "Must have at least a name or a uri (or both)" | `name.exists() or uri.exists()` |
| cnl-0 | Warning | (base) | name should match `^[A-Z]([A-Za-z0-9_]){1,254}$` | — |
| cnl-1 | Warning | url | "URL should not contain \| or # ..." | `exists() implies matches('^[^|# ]+$')` |

## 4. Ordering rules (§5.4.6 Interpretation Notes) `[ch2]`

The only normative statement of element ordering in the whole corpus of these two pages — quoted in full:

> "Elements specified in the differential (and all elements in the snapshot) must be ordered as such:
>
> - Elements from the baseDefinition appear before new elements in a StructureDefinition with derivation
>   'specialization'
> - Elements must be in the same order as the baseDefinition, and child elements appear in depth-first
>   order.
> - Unsliced descendants of sliced elements appear before slices" [structuredefinition §5.4.6]

Consequences: (a) a differential is required to already be in base order — a generator may treat
out-of-order differentials as input errors `[ch12]` or normalize them in preprocessing `[ch2]`; (b) the
merge can be a single ordered walk; (c) within a sliced element, the slicing-entry's (unsliced) children
come before the slice groups `[ch6]`. The page does not say where *new* elements go in a `constraint`
derivation (only for `specialization`) — constraints cannot introduce new elements at all per profiling
rules, but that statement is on profiling.html, not here.

Other interpretation notes worth carrying:

> "element.type is used to specify which types may be used for this element. If there is more than one
> type, the element offers a choice of types and must have a name that terminates in \"[x]\". (Note: when
> substituting [x] with a specific data type, always capitalize the first letter. Choice types are always
> camel-case. Ex: \"effectiveDateTime\" is correct, \"effectivedateTime\" is NOT correct.)"
> [structuredefinition §5.4.6] `[ch2]` — the renaming rule for choice-type differential paths.

> "The condition element is used to assert that a constraint defined on another element affects the
> allowed cardinality of this element." [structuredefinition §5.4.6]

## 5. Root element semantics (§5.4.6.1) `[ch3, ch7, ch10]`

> "The very first element in a snapshot (the one with an element id of just the type name) defines
> characteristics that apply to the type as a whole. Some of these characteristics affect the constraints
> that can hold on an element that references the type. For example, if the root element cardinality is
> 0..1, then an element declared to be of that type cannot have a maximum cardinality greater than 1.
> (This is often used when defining extensions to indicate that multiple extension repetitions of that
> type are not allowed within a single element.)" [structuredefinition §5.4.6.1]

This is directly load-bearing for extension expansion `[ch7]`: when a profiled extension's own root says
`0..1`, the referencing element's max must be capped at 1. Note in passing it asserts the root element id
is "just the type name" — the closest thing on this page to an id-generation rule.

§5.4.6.1 also notes deliberate redundancy: root `element.definition` vs `StructureDefinition.description`
"may have the same content put both places" (definition = what the type means; description = broader).

## 6. Extension context and contextInvariant `[ch7]`

Facts from the structure table (§5.4.5) and metadata (§5.4.3.1):

- `context : BackboneElement 0..*` — "If an extension, where it can be used in instances", with
  `context.type : code 1..1` = `fhirpath | element | extension` (required binding ExtensionContextType,
  "How an extension context is interpreted") and `context.expression : string 1..1` — "Where the
  extension can be used in instances".
- `contextInvariant : string 0..*` — "A set of rules as FHIRPath Invariants about when the extension can
  be used (e.g. co-occurrence variants for the extension). All the rules must be true" [structuredefinition §5.4.5].
- sdf-5: a `type='Extension'` + `derivation='constraint'` SD must have `context`; sdf-18:
  `contextInvariant` only on `type = 'Extension'`.
- The UML pane defers the semantics: "For more guidance on using the 'context' element, see the
  [defining extensions page](defining-extensions.html#context)" — i.e. what the three context types
  actually mean (FHIRPath expression / element path / extension url) is NOT on either extracted page.

**Context inheritance under constraint is unspecified here**: nothing on this page says whether a profile
of an extension repeats, narrows, or inherits `context` — see Gaps.

## 7. Logical models `[ch9]`

§5.4.6.5:

> "StructureDefinitions are used to define the basic structures of FHIR: data types, resources,
> extensions, and profiles. The same definition structure can also be used to define any arbitrary
> structures that are a directed acyclic graph with typed nodes, where the primitive types are those
> defined by the FHIR specification." [structuredefinition §5.4.6.5]

Uses listed: "Describing any arbitrary content model", "Describing existing HL7 content models (e.g. v2,
CDA) using FHIR", "Describing common design patterns used in FHIR", "Defining a content model to support
the mapping language".

Logical-model deltas relevant to a generator, all encoded in the invariants rather than prose:

- `type` for logical models is an **absolute URL** and this is *required* ("Absolute URLs are only allowed
  in logical models, where they are required" [§5.4.5, `type`]).
- Snapshot root path only needs to equal the *type name* (not the full url): sdf-8/sdf-8a/sdf-11 all carve
  out `kind = 'logical'` ("or with the same type name for logical models").
- The root element MAY have a `type` (sdf-15/sdf-15a exempt logical models).
- Snapshot elements need not all carry definition/min/max (sdf-3 exempts logical models).
- Nothing on this page describes rebasing a logical model onto `Base`/`Element` or how
  `derivation=specialization` merging differs for logical models beyond these exemptions. See Gaps.

## 8. Interfaces, obligations, imposeProfile / compliesWithProfile `[ch9]`

The whole R5 surface on this page:

- The structure table's root row lists "Interfaces Implemented: CanonicalResource" for StructureDefinition
  itself, and §5.4.6.2 example 9 shows what an interface SD looks like (`abstract: true`, `kind: resource`,
  `type: CanonicalResource`). No mechanism (no mention of the `structuredefinition-interface` extension or
  of how implementing an interface affects derivation/snapshots) is given.
- §5.4.4 "Extension References" names two relevant extensions by link only:
  `structuredefinition-compliesWithProfile` (rendered text "StructureDefinition Compiles With Profile"
  [sic — display typo]) and `structuredefinition-imposeProfile` (rendered "StructureDefinition Dependent
  Profiles"). **No semantics on this page.**
- The mapping-targets table (§5.4.6.4) lists an "Interface Pattern" mapping url
  `http://hl7.org/fhir/interface` — mapping metadata only.
- **Obligations are not mentioned anywhere on either page.**

## 9. Extensibility page — the Extension type `[ch7]`

### 9.1 Structure of Extension [extensibility §2.1.5.0.1]

> "Every resource or datatype element may include one or more \"extension\" child elements. The extension
> is either simple or complex. Simple extensions have only a value and no nested extensions. Complex
> extensions contain one or more nested extensions and no value. An extension can not have both a value
> and nested extensions." [extensibility §2.1.5.0.1]

Structure table: `Extension : Element` (Normative) with rule "Must have either extensions or value[x],
not both" (this is invariant ext-1, though the key isn't printed on this page); children
`url : uri 1..1` — "identifies the meaning of the extension"; `value[x] : * 0..1` — one of **54** types
(R5 list, verbatim from the JSON template): base64Binary, boolean, canonical, code, date, dateTime,
decimal, id, instant, integer, integer64, markdown, oid, positiveInt, string, time, unsignedInt, uri,
url, uuid, Address, Age, Annotation, Attachment, CodeableConcept, CodeableReference, Coding,
ContactPoint, Count, Distance, Duration, HumanName, Identifier, Money, Period, Quantity, Range, Ratio,
RatioRange, Reference, SampledData, Signature, Timing, ContactDetail, DataRequirement, Expression,
ParameterDefinition, RelatedArtifact, TriggerDefinition, UsageContext, Availability,
ExtendedContactDetail, Dosage, Meta. (R5 changes: `Extension.url` type changed string→uri; value[x] adds
integer64, CodeableReference, RatioRange, Availability, ExtendedContactDetail, Meta; removes Contributor.)

Notes (all normative, §2.1.5.0.1 "Notes"):

> "The url is a mandatory attribute / property and identifies a retrievable extension definition that
> defines the content and meaning of the extension" [extensibility §2.1.5.0.1]

> "Extensions are not allowed on Extension.url or on Element.id; see ElementDefinition.representation for
> background" [extensibility §2.1.5.0.1]

> "The url SHALL be a URL, not a URN (e.g. not an OID or a UUID), and it SHALL be a reference to the
> canonical URL of a StructureDefinition that defines the extension. Except for child extensions defined
> within complex extensions, the URL SHALL be an absolute URL." [extensibility §2.1.5.0.1]

> "In the case where an extension defines complex content, the identity of the parts of the extension are
> local/relative to the reference to the extension definition." [extensibility §2.1.5.0.1]

The last two together are the page's entire statement on what nested urls look like: in the citizenship
example the child extensions use `url="code"` and `url="period"` — bare relative names. **This is
instance syntax; the page never states that a snapshot must carry these as `fixedUri` on `extension.url`
elements — that convention is tooling practice, not spec text on these pages.** See Gaps.

> "If the meaning of an extension is clearly defined by a code, that code SHOULD be declared on the
> extension's root code (StructureDefinition.differential.element[0].code)" [extensibility §2.1.5.0.1]
> — note the spec here addresses the *differential's* root element directly.

> "An extension SHALL have either a value (i.e. a value[x] element) or sub-extensions, but not both. If
> present, the value[x] element SHALL have content (value attribute or other elements)"
> [extensibility §2.1.5.0.1]

`valueCode` carries a parenthetical constraint: "code (only if the extension definition provides a fixed
binding to a suitable set of codes)".

Extensions can nest for two reasons: "either because the extension definition itself defines complex
content ... or because the extension is extended with an additional extension defined separately" (the
latter uses an absolute url inside the complex extension, per the passport-number example) `[ch11]`.

### 9.2 Extensions on primitives [extensibility §2.1.5.0.1.1]

Primitives can carry extensions in place of a value; "a mandatory primitive datatype (i.e. mininum
cardinality=1) may be present with extensions but no value" (DataAbsentReason pattern), but such an
extension "cannot be used to bypass a required or extensible binding requirements". §2.1.5.0.2.2 adds:
a data-absent-reason on a valueless primitive "is not a modifier extension, because the primitive
datatype has no value."

### 9.3 Modifier extensions [extensibility §2.1.5.0.2] `[ch7]`

The generator-relevant sentences:

> "Whether extensions are considered modifiers or not is based on the isModifier flag on the root element
> in the extension definition (the element definition with path \"Extension\" in the generated snapshot).
> If this is true, then the extension SHALL only be contained in the modifierExtension element. If this
> is false, then the extension SHALL only be contained in the extension element."
> [extensibility §2.1.5.0.2]

Note the phrase **"in the generated snapshot"** — one of the very few places anywhere in the R5 spec that
presumes a snapshot has been *generated* for an extension definition and hangs normative behavior off it.

> "When defining a new modifier extension, it is designated as being a 'modifier' rather than a 'regular'
> extension by setting the isModifier element on the root 'element' for the StructureDefinition to
> 'true'. Only extensions that are defined as being modifiers can appear in modifierExtension. A regular
> extension cannot be 'constrained' to be a modifier." [extensibility §2.1.5.0.2]

The last sentence is a derivation rule `[ch3, ch12]`: a constraint on an extension must not flip
`isModifier` false→true (root Extension element).

Other modifier facts: complex extensions may contain internal elements marked Is-Modifier ("these
elements modify the extension value itself") which still serialize under `extension`, not
`modifierExtension`; "extensions SHALL NOT have modifier extensions internally"; "Modifier extensions
SHALL NOT change the meaning of any elements on Resource or DomainResource"; datatypes other than a few
selected ones SHALL NOT have modifier extensions. Processing obligations for unknown modifier extensions
(§2.1.5.0.2, five options; conformance summary §2.1.5.0.2.1) are instance-processing rules, not
generator rules — summarized only.

### 9.4 Exchanging extensions [extensibility §2.1.5.0.3]

Instance-handling guidance (retain unknown extensions; remove not-understood extensions from modified
elements; never modify an element governed by an un-understood modifier extension; don't reject unknown
non-modifier extensions). Not generator-relevant; recorded for completeness.

### 9.5 What extensibility.html does NOT contain `[ch6, ch7]`

Contrary to what its title suggests, this page has **nothing** about profiling extensions:

- No description of how a profile slices `extension` arrays by url (discriminator `value:url` etc.) —
  that lives on `profiling.html` ("Using Extensions in Profiles") and `defining-extensions.html`.
- No statement about expanding an extension definition into a referencing profile's snapshot.
- No rules on whether a profile may further constrain an extension's value type or cardinality (beyond
  the §5.4.6.1 root-cardinality interaction and the "cannot be constrained to be a modifier" rule above).
- Extension *context* rules (what `element` vs `fhirpath` vs `extension` context types match; context of
  nested extensions) are explicitly deferred to `defining-extensions.html`.

## 10. Recursion `[ch11]`

Neither page contains material on recursion in snapshot generation. The only self-referential structure
mentioned is Extension itself (Extension.extension : Extension, implied by the complex-extension examples
and "Extensions can also contain extensions"), plus the logical-model requirement that structures be "a
directed acyclic graph with typed nodes" [structuredefinition §5.4.6.5] — the sole acyclicity statement,
and it applies to logical models' node graphs, not to profile reference cycles. No guidance on
terminating expansion of recursive types (Reference, Identifier→Reference, Extension) exists here.

## 11. Error conditions catalog `[ch12]`

Everything on these pages a generator can raise as an input/output error, consolidated:

- Input lacks both differential and snapshot (sdf-6).
- Non-abstract SD without baseDefinition (sdf-4); baseDefinition without derivation (sdf-27).
- Constraint SD whose `type` is an abstract type (§5.4.5 `type` definition prose).
- Extension constraint without context (sdf-5); contextInvariant on non-extension (sdf-18).
- Differential path errors: first element not starting with `type` / elements not under first element's
  root segment (sdf-8a); root element with type (sdf-15a); slicing on root (sdf-20); sliceName on root
  (sdf-23); label/code/requirements on root (sdf-9); missing or duplicate element ids (sdf-14, sdf-17).
- defaultValue in a constraint differential (sdf-21).
- Differential elements out of base order (§5.4.6 ordering rules — a Rule-level ordering statement
  without an sdf key).
- Output must satisfy: sdf-1, sdf-3, sdf-8, sdf-8b, sdf-11, sdf-14, sdf-15, sdf-16, sdf-24, sdf-25,
  sdf-28, sdf-10; plus guideline sdf-26 (root mustSupport) and warning sdf-29.
- Constraint flipping a regular extension into a modifier [extensibility §2.1.5.0.2].

---

## Spec gaps noticed

Silence is graded into three kinds.

### A. Covered elsewhere in the LOCAL corpus (cross-reference, extracted separately)

1. **Constraint-merge semantics** ("what may a profile change, and how do differential and base element
   merge"): §5.4.6.3 defers wholesale to *Extending and Restricting Resources* = `profiling.html` (local).
2. **Element id generation grammar** (`path:sliceName.path...`): absent here beyond sdf-14/16/17
   (exist + unique) and the §5.4.6.1 aside that the root id is "just the type name". Lives on
   `elementdefinition.html` (local).
3. **Slicing extension arrays by url**, discriminators, re-slicing: nothing on either page; lives on
   `profiling.html` (local).
4. **ElementDefinition.base semantics** (what base.path/min/max mean, when they're set): sdf-8b requires
   `base` to exist on every snapshot element but neither page defines its content. Lives on
   `elementdefinition.html` (local).

### B. Deferred to pages NOT in the local corpus (fetch candidates)

5. **`defining-extensions.html`** — both pages defer extension-context semantics there (structuredefinition's
   UML note: "For more guidance on using the 'context' element, see the defining extensions page"). The
   meaning of context.type `fhirpath|element|extension`, context matching, and nested-extension context is
   unavailable locally.
6. **`structuredefinition-operations.html`** — the `$snapshot` operation is never mentioned in
   structuredefinition.html's content; its definition (in/out parameters, server behavior) is on the
   Operations tab page, not in the corpus.
7. **`structuredefinition-imposeProfile` / `structuredefinition-compliesWithProfile`** extension
   definitions — referenced by link only (§5.4.4); their semantics (additional profiles a conforming
   instance must satisfy / claims of compliance without re-profiling) are in the R5 extensions pack, not
   here. Obligations and the `structuredefinition-interface` mechanism appear nowhere on either page.

### C. Genuinely unspecified on these pages (a generator must invent or borrow conventions)

8. **How the Extension type's snapshot is merged under a profiled `extension` element.** Nothing anywhere
   in these two pages describes expanding `Patient.extension:foo` against the foo extension's own
   snapshot (unfolding url/value[x]/nested extension children into the profile's snapshot, merging the
   extension SD's root constraints with the referencing element). The §5.4.6.1 root-cardinality
   interaction is the *only* stated cross-SD effect.
9. **What `extension.url` looks like in a generated snapshot.** The instance rule (absolute url = the
   defining SD's canonical; relative url = local part name inside a complex extension) implies the
   conventional `fixedUri` on `Extension.url`, but no sentence on either page requires a fixed value,
   names the element property to use (`fixedUri` vs `pattern`), or says what the fixed value is for
   nested parts. Pure tooling convention.
10. **Path rebasing for `derivation=specialization`.** sdf-11/sdf-8 pin the snapshot root to the new
    `type`, and §5.4.6 orders base elements first — but the actual renaming of inherited base paths
    (`Resource.id` → `Patient.id`), and what happens to base elements' ids and `base` components during
    specialization, is never described.
11. **Where new elements go and how ordering is enforced for `constraint`.** The ordering rule's first
    bullet covers specialization only; constraints presumably cannot add elements, but that is not stated
    here, and no error behavior is prescribed for a differential that violates base order.
12. **Logical model derivation mechanics**: which invariant exemptions imply (root may keep `type`,
    definition/min/max optional) is clear, but how a logical model's elements merge with `Base`/`Element`
    /another logical model when `baseDefinition` is present is unstated.
13. **Extension `context` inheritance under constraint**: whether a profile of an extension must repeat,
    may narrow, or implicitly inherits `context`/`contextInvariant` is unstated (sdf-5's escape clause
    `or (context.exists())` merely requires *some* context on extension constraints — it does not relate
    it to the base's context). Note sdf-5 as written is satisfied by `derivation = 'specialization'`
    alone, so the base Extension type itself needs no context.
14. **Published typos a test harness must tolerate**: §5.4.6.2's `us/core` baseDefinition urls for
    Resource/CanonicalResource/Definition examples and the doubled `"abstract": false` (verified in
    source HTML); the extension-reference display text "Compiles With Profile" for
    `compliesWithProfile`. Narrative examples on this page are not reliable ground truth.
