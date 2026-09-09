# R5 ElementDefinition — spec baseline for snapshot generation

**Sources (local):** `spec-html/R5/elementdefinition.html` (FHIR v5.0.0, Normative) and
`spec-html/R5/elementdefinition-definitions.html` (detailed descriptions, Normative).
FHIR spec text is CC0; normative sentences are quoted verbatim.

**Citation convention:** the local page numbers its sections `2.1.28.7.0.x`; anchors are the
stable identifiers, so citations use `[elementdefinition §2.1.28.7.0.4 / #interpretation]` form.
Detailed-descriptions citations use `[elementdefinition-definitions / #def]` (that page is one
table anchored at `#def`, per-element ids like `ElementDefinition.mustSupport`).

**Chapter tags:** `ch4` element matching · `ch5` per-property merge semantics · `ch6` slicing ·
`ch7` type-profile expansion · `ch8` contentReference · `ch10` element ids & Base component ·
`ch12` error handling.

---

## 1. Use of ElementDefinition.path `[ch4]`

[elementdefinition §2.1.28.7.0.2 / #path]

> "The path element is the most important property of the element definition. It both names the
> element, and locates the element within a hierarchy defined within a particular context. Within
> the FHIR specification, there is only one original definition for each path."

Naming rules (verbatim, condensed list):

- "Element names ... SHALL NOT contain whitespace"; SHALL NOT contain `,:;'"/|?!@#$%^&*()[]{}`;
  SHOULD not contain non-ASCII; SHALL NOT exceed 64 characters.
- "Element paths cannot imply elements that are not explicitly defined i.e. a.b.c.d cannot be
  defined unless a.b.c is explicitly defined"
- "If the element is polymorphic (has more than one datatype), then the end of the path for the
  element SHALL be \"[x]\" ... The \"[x]\" is not considered to be part of the official element
  name"

Load-bearing for profiles:

> "StructureDefinitions with derivation = constraint (i.e. Resource and Datatype profiles) are not
> allowed to define or include ElementDefinitions with a path not defined within the base type
> definition from which they derive" [#path]

`ch12` implication: a differential element whose path does not exist in the base (after type
expansion) is an error, not a new element.

## 2. ElementDefinition.id `[ch10]`

[elementdefinition §2.1.28.7.0.3 / #id] — verbatim, in full:

> "In addition to the path, every ElementDefinition SHALL have a populated id, and the id SHALL
> have a unique value populated by following this algorithm:
> - The id will be constructed as a dot separated string, each part corresponding to a token in the path
> - For each token in the path, use the syntax pathpart:slicename/reslicename
> - For type choice elements, the id reflects the type slice. e.g. For path = Patient.deceased[x],
>   the id of the boolean slice type element is Patient.deceased[x]:deceasedBoolean
>
> Note that in a profile with no slices, this id will match the path exactly and entirely.
> id values constructed in this fashion are unique, and persistent, and may be used as the target
> of external references into the definition, where necessary."

Notes for the generator (`ch10`, `ch4`): reslices use `slicename/reslicename` in a single path
token; type slices of a choice element keep the `[x]` path and carry the type-slice name after
`:`. Since ids are deterministic from path+sliceNames, a snapshot generator can (and must)
recompute/normalize them.

## 3. Interpretation of ElementDefinition in different contexts — IN FULL `[ch5][ch12]`

[elementdefinition §2.1.28.7.0.4 / #interpretation] — this whole section is quoted essentially
verbatim; it is the most load-bearing spec text for snapshot generation.

> "The datatype ElementDefinition is used in StructureDefinition. The way its elements are to be
> used and interpreted depends on the context:"

*(Reading note, not spec text: "first element" means the root ElementDefinition of the structure
— the one whose path is the type/resource name — not the first repetition of a sliced element.
This reading is supported by footnote §, which concerns the type root, and by the base row.)*

| ElementDefinition field | Type definition, first element | Type definition, following elements | Constraint Definition, first element | Constraint Definition, following elements |
|---|---|---|---|---|
| sliceName | prohibited | prohibited | prohibited | required for slices, else prohibited |
| label | optional | optional | recommended | recommended |
| code | optional | optional | optional | optional |
| slicing | prohibited | prohibited | prohibited | optional |
| short/definition | required | required | required‡ | required‡ |
| requirements / comments/alias | prohibited | optional | prohibited‡ | optional‡ |
| base | snapshot: required / differential: optional | snapshot: required / differential: optional | required | required |
| type | required | required | optional | optional |
| nameReference | prohibited | optional | prohibited | optional |
| min/max | optional§ | required | optional | optional† |
| defaultValue[x] | prohibited | optional | prohibited | optional† |
| meaningWhenMissing | prohibited | optional | prohibited | optional† |
| fixed[x] | prohibited | prohibited | prohibited | optional |
| pattern[x] | prohibited | prohibited | prohibited | optional |
| example[x] | prohibited | optional | prohibited | optional |
| minValue[x] | prohibited | prohibited | prohibited | optional |
| maxValue[x] | prohibited | prohibited | prohibited | optional |
| maxLength | prohibited | prohibited | prohibited | optional |
| mustSupport | prohibited | prohibited | optional | optional |
| isModifier | prohibited | optional | prohibited | optional† |
| isSummary | prohibited | optional | prohibited | optional† |
| binding | prohibited | optional | prohibited | optional |
| constraint | optional | optional | optional∆ | optional∆ |
| condition | prohibited | optional | prohibited | optional∆ |
| mapping | optional | optional | optional∆ | optional∆ |

Notes (verbatim — **the four footnotes are the core merge semantics of the whole page**):

> - "Type definition: A StructureDefinition without a baseDefinition element, or where the
>   derivation type is 'specialization'"
> - "Constraint definition: A StructureDefinition with a baseDefinition element and a derivation
>   of 'constraint' - e.g. a definition of a structure that constrains another base structure,
>   referring to the differential portion"
> - "†: The element's presence, and value, must match the definition in the base definition"
> - "‡: The element content must be consistent with that matching element in the base definition"
> - "∆: Additional constraints and mappings can be defined, but they do not replace the ones in
>   the base definition"
> - "§: The cardinality on a type places constraints on references to that type. I.e. profiles
>   referencing the type must fall within the cardinality bounds of the type itself. This most
>   commonly occurs with Extension profiles, where the profile may prohibit the extension from
>   repeating (max=1), or less commonly, make the extension mandatory (min=1) - i.e. if the
>   extension is referenced in a profile, it must be marked as mandatory."

FHIRPath primitive types (verbatim; `ch7` — hit on every primitive's `.value` element):

> "For some simple types, rather than pointing to a FHIR datatype, the element will be defined as
> having a datatype defined by [FHIRPath]. E.g. http://hl7.org/fhirpath/System.String . This
> occurs when the generic behavior of the FHIR primitive type is not desired (e.g. inheriting
> extension, id, etc.). Implementations should map these FHIRPath datatypes to the appropriate
> simple datatype defined in their implementation language."
>
> "In addition, when an element has a datatype of http://hl7.org/fhirpath/System.String then it
> will typically also include a structuredefinition-fhir-type extension. This extension might
> further constrain the allowed value of an element that might be treated as a string. ... If the
> extension is not present, no additional constraints apply."

Path/type usage by context (verbatim table; `ch4` `ch7`):

> "The use of Path and type depends more deeply on the context where the ElementDefinition is used:"

| Context | path (1st element) | path (following elements) | type (1st element) |
|---|---|---|---|
| Base definition of a datatype (example: Quantity) | Name of the type | Path inside the datatype | Element |
| A constrained datatype (example: Money) | Name of the base type | Path inside the datatype | Name of the base type |
| Base definition of a resource (example: Patient) | The name of the resource | Path inside the resource | DomainResource or sometimes Resource |
| Constraint on a resource (example: DAF Patient) | The name of the resource | Path inside the resource (including into the datatypes) | The name of the resource |
| Base Extension (which is a standard datatype) | Extension | Extension.value[x] or Extension.extension | Extension |
| A defined Extension | Extension | Extension.value[x] or Extension.extension (for complex extensions) | Extension |

### 3.1 Rules about Slicing `[ch6]`

[elementdefinition §2.1.28.7.0.4.1 / #slicing] — verbatim, in full:

> - "Slicing is only allowed when constraining an existing structure"
> - "slicing can only be used on the first repetition of an element. This first element that
>   declares slicing is considered to be the slicing entry"
> - "All elements following the first repeat that containing a slicing SHALL have a sliceName"
> - "The special slice name @default applies to all entries that are not in any other slice"
> - "The first entry (the one having the slicing information) is understood to be the set of
>   constraints that apply to all slices and entries, whether they have a defined slice or not.
>   It's use follows the \"normal case\", except:
>   - slicing must be present
>   - min governs the number of total occurrences of the sliced element including the number of
>     occurrences in the open portion of the slice (individual slices may have a different min value)."

### 3.2 Constraining elements with a choice of Type `[ch4][ch7][ch10]`

[elementdefinition §2.1.28.7.0.4.2 / #typesx] — verbatim, in full:

> "Elements that allow a choice of multiple types can be constrained. In principle, there are two
> different types of constraints to apply:
> - A constraint that applies to the element as a whole - e.g. as restricting the cardinality, or
>   limiting the choice of types
> - A constraint that applies to the use of a particular type - e.g. value set binding
>
> When constraining elements with multiple types, the following rules apply:
> - Constraints limiting the acceptable list of types must be applied to the original \"[x]\"
>   element as this is where the list of acceptable types is defined
> - The inclusion of a type specific element (such as \"Patient.deceased[x]:deceasedBoolean\")
>   SHALL NOT be interpreted as constraining allowed types, but instead, it constrains the use of
>   a particular type
> - the original element SHALL always be represented in a snapshot; the type specific variants are
>   only represented when needed"

`ch4/ch10` implication: type-specific constraints are expressed as *type slices* on the `[x]`
path with id suffix `:<typeName>` (see §2). This section does NOT sanction renamed paths
(`Patient.deceasedBoolean`) in R5 differentials — see Spec gaps.

### 3.3 Rules about min and max `[ch5]`

[elementdefinition §2.1.28.7.0.4.3 / #min-max] — verbatim, in full:

> - "If there is no StructureDefinition.baseDefinition: min and max are always required"
> - "Otherwise, in StructureDefinition.differential: min and max are always optional; if they are
>   not present, they default to the min and max from the base definition"
> - "In StructureDefinition.snapshot: min and max are always required"
> - "If min = 0, and there is a fixed or pattern value present, the fixed or pattern value only
>   applies if the element is actually present. If the element is omitted, it is NOT treated as
>   though the fixed value or pattern had been specified"
>
> "This section and the elements mustHaveValue and valueAlternatives are considered Trial Use for
> FHIR Release 5."

This is the only place on the page that states an explicit differential-absence = inherit rule.
Note: the rule that a profile may only *narrow* cardinality (min ≥ base.min, max ≤ base.max) is
NOT on this page — it lives on profiling.html.

### 3.4 Primitive Values `[ch5]`

[elementdefinition §2.1.28.7.0.4.4 / #primitives] — summarized: even a present primitive element
may lack a value and carry only extensions (data-absent-reason, nullflavor, originalText,
expression). Two Trial-Use shortcuts:

> "The flag mustHaveValue can be set to true to indicate that the primitive data type must have
> value if present (so it has impact whatever the value of min). Note that this flag is a short
> cut, equivalent to the profile walking into the primitive data type and setting min = 1 for the
> value."
>
> "profiles can list the allowable extensions using the valueAlternatives element, which contains
> a list of the extensions that can appear if the primitive value is not present. Note that this
> list is a short cut for actually profiling the extensions on the primitive data type and making
> a co-occurence constraint, but has no effect when the value is present."

### 3.5 Rules about Aggregation `[ch5]`

[elementdefinition §2.1.28.7.0.4.5 / #aggregation] — verbatim, in full:

> - "If an aggregationMode is present in the definition, the 'reference' element SHALL be present
>   and have a value and the target of the reference SHALL be aggregated as defined"
> - "If type.versioning is present in the definition, the 'reference' element SHALL be present and
>   have a value and the reference SHALL be populated as the versioning constraint dictates."

### 3.6 Missing Elements `[ch5]`

[elementdefinition §2.1.28.7.0.4.6 / #missing] — key sentence:

> "for some elements, this specification makes specific rules about what it means if the element
> is missing. Constraints on other structures cannot change the missing meaning of an element."

The section lists ~90 per-element defaults across all resources; only the ElementDefinition.*
entries matter here (verbatim):

> - "ElementDefinition.slicing.ordered: Order is not required unless specified"
> - "ElementDefinition.valueAlternatives: If this element is missing, then any extension can be
>   used if its context is appropriate"
> - "ElementDefinition.mustSupport: In a base type, where the element is being defined for the
>   first time, the element is assumed to be mustSupport = false. In a profile (a constraint on an
>   existing type), if mustSupport is not specific in either the differential or the snapshot, the
>   mustSupport value is not changed from the base definition. Note, though, that the snapshot
>   SHOULD always populate the mustSupport value"
> - "ElementDefinition.isModifier: An element is not a modifier element unless it is explicitly
>   specified to be one"
> - "ElementDefinition.isSummary: An element is not included in the summary unless it is
>   explicitly specified to be so"

The mustSupport entry is the ONE property for which the spec states the differential-absent →
inherit-from-base + snapshot-should-populate rule explicitly (besides min/max in §3.3).

Why FHIR has no default values (verbatim, rationale for defaultValue handling):

> "This specification does not define any default values for resources or datatypes because:
> - The value must be known by all implementations
> - When an element has a default value, it can never be unknown - e.g. it is implicitly mandatory
> - The default value can never be changed
> - The presence of a default value interacts with minimum cardinality and the :missing search
>   token in ways that create confusion for implementations
>
> Note that default values can be defined in Logical Models."

### 3.7 Bindings / Additional Bindings `[ch5]`

[elementdefinition §2.1.28.7.0.4.7 / #bindings] — the section is a single pointer: "For further
information about bindings, see Terminology bindings." (No merge rules here; see Spec gaps.)

### 3.8 Must-support `[ch5]`

[elementdefinition §2.1.28.7.0.4.8 / #must-support] — summarized: mustSupport indicates rules
apply to how data is handled; the Obligation extension makes more detailed statements; see
Implementation Obligations.

---

## 4. Per-property semantics as they bear on merging `[ch5]`

Sources: structure table + UML text [elementdefinition / #definition] and the detailed
descriptions [elementdefinition-definitions / #def]. Interpretation-table status from §3 is
repeated per property as *diff:* (Constraint Definition columns). Where the spec says nothing
about how a differential value combines with the base, this is marked **merge: unstated**.

### Documentation properties

- **short / definition** — diff: required‡ (content must be consistent with base).
  > "For the case of elements derived from existing elements (e.g. constraints), the definition
  > SHALL be consistent with the base definition, but convey the meaning of the element in the
  > particular context of use of the resource." [defs: ElementDefinition.definition]
  Comments warn: "It is easy for a different short definition to change the meaning of an element."
  **merge: unstated** (universally implemented as: differential value replaces base value in the
  snapshot; absence inherits).
- **comment / requirements** — diff: optional‡. Same consistency requirement; merge: unstated.
- **alias** — diff: optional‡ (grouped with requirements/comments in the table). Repeating list;
  **merge: replace-vs-append unstated.**
- **label** — diff: "recommended". No constraint against base. merge: unstated.
- **code** (Coding 0..*) — optional in all four contexts. "A code that has the same meaning as the
  element in a particular terminology." **merge: replace-vs-append unstated.**

### Structural properties

- **min / max** — see §3.3 verbatim rules: differential absent → default to base; snapshot always
  required. eld-2 (min<=max), eld-3 (max is number or `*`). Narrowing-only rule not on this page.
- **base** (path/min/max, each 1..1 when present) — `[ch10]`
  > "Information about the base definition of the element, provided to make it unnecessary for
  > tools to trace the deviation of the element through the derived and related profiles. When the
  > element definition is not the original definition of an element - e.g. either in a constraint
  > on another type, or for elements from a super type in a snap shot - then the information in
  > provided in the element definition may be different to the base definition. On the original
  > definition of the element, it will be same." [defs: ElementDefinition.base]
  > "For tooling simplicity, the base information must always be populated in element definitions
  > in snap shots, even if it is the same." [defs: ElementDefinition.base, Comments]
  > "The Path that identifies the base element - this matches the ElementDefinition.path for that
  > element. Across FHIR, there is only one base definition of any element - that is, an element
  > definition on a StructureDefinition without a StructureDefinition.base."
  > [defs: ElementDefinition.base.path]
  So base.path/min/max record the *original* definition's identity and cardinality; the generator
  must populate base on every snapshot element (propagating from the base snapshot, or seeding
  path/min/max from the element itself when the element is first defined).
- **contentReference** — `[ch8]`
  > "Identifies an element defined elsewhere in the definition whose content rules should be
  > applied to the current element. ContentReferences bring across all the rules that are in the
  > ElementDefinition for the element, including definitions, cardinality constraints, bindings,
  > invariants etc." [defs: ElementDefinition.contentReference]
  > "ContentReferences can only be defined in specializations, not constrained types, and they
  > cannot be changed and always reference the non-constrained definition."
  > [defs: ElementDefinition.contentReference, Comments]
  eld-5: an element with contentReference cannot have type, defaultValue, fixed, pattern, example,
  minValue, maxValue, maxLength, or binding. (Note the interpretation table still names this row
  "nameReference" — a DSTU2-era leftover.) The page is silent on how a profile constrains
  *children* of a contentReference'd element (whether/when the generator may expand the referenced
  content in the snapshot) — see Spec gaps.
- **type (code / profile / targetProfile / aggregation / versioning)** — `[ch7]`
  > "The Type of the element can be left blank in a differential constraint, in which case the
  > type is inherited from the resource. Abstract types are not permitted to appear as a type when
  > multiple types are listed." [defs: ElementDefinition.type, Comments]
  - `code` 1..1 uri: "References are URLs that are relative to http://hl7.org/fhir/StructureDefinition
    e.g. \"string\" is a reference to http://hl7.org/fhir/StructureDefinition/string. Absolute URLs
    are only allowed in logical models." eld-13: types unique by code.
  - `profile` 0..*: "If any profiles are specified, then the content must conform to at least one
    of them." (i.e. the list is a disjunction). Backbone elements can be profiled via the
    elementdefinition-profile-element extension.
  - `targetProfile` 0..*: only for Reference/canonical/CodeableReference (eld-17); "If any profiles
    are specified, then the content must conform to at least one of them."
  - `aggregation`: only when a type is Reference/canonical/CodeableReference (eld-4); semantics in §3.5.
  - `versioning`: "The base specification never makes a rule as to which form is allowed."
  **merge: unstated** — nothing on this page says a differential type list must be a subset of the
  base's, nor whether profile/targetProfile lists on a matching type code replace or append.
- **representation** — diff: not listed in the interpretation table.
  > "In resources, this is rarely used except for special cases where the representation deviates
  > from the normal, and can only be done in the base standard (and profiles must reproduce what
  > the base standard does)." [defs: ElementDefinition.representation, Comments]
  So for a snapshot generator: copy from base, never changed by a differential.

### Value constraints

- **defaultValue[x]** — diff: optional† (presence and value must match base).
  > "the default value can never be changed, or changed in constraints on content models. ... No
  > default values are ever defined in the FHIR specification, nor can they be defined in
  > constraints (\"profiles\") on data types or resources. This element only exists so that
  > default values may be defined in logical models." [defs: ElementDefinition.defaultValue[x], Comments]
  eld-15: mutually exclusive with meaningWhenMissing.
- **meaningWhenMissing** — diff: optional†.
  > "Implicit meanings for missing values can only be specified on a resource, data type, or
  > extension definition, and never in a profile that applies to one of these. An implicit meaning
  > for a missing value can never be changed" [defs: ElementDefinition.meaningWhenMissing, Comments]
- **orderMeaning** — not in the interpretation table.
  > "This element can only be asserted on repeating elements and can only be introduced when
  > defining resources or data types. It can be further refined profiled elements but if absent in
  > the base type, a profile cannot assert meaning." [defs: ElementDefinition.orderMeaning, Comments]
  eld-25 (warning): no ordered slicing / openAtEnd unless orderMeaning present.
- **fixed[x]** — diff: optional (prohibited in type definitions).
  > "Specifies a value that SHALL be exactly the value for this element in the instance, if
  > present. For purposes of comparison, non-significant whitespace is ignored, and all values
  > must be an exact match (case and accent sensitive). Missing elements/attributes must also be
  > missing." [defs: ElementDefinition.fixed[x]]
  eld-6 (one type only), eld-8 (fixed xor pattern), eld-24 (guideline: prefer pattern). "Not
  recommended for Coding and CodeableConcept."
- **pattern[x]** — diff: optional. Exact matching semantics, verbatim:
  > "Specifies a value that each occurrence of the element in the instance SHALL follow - that is,
  > any value in the pattern must be found in the instance, if the element has a value. Other
  > additional values may be found too. This is effectively constraint by example.
  > When pattern[x] is used to constrain a primitive, it means that the value provided in the
  > pattern[x] must match the instance value exactly. When an element within a pattern[x] is used
  > to constrain an array, it means that each element provided in the pattern[x] must (recursively)
  > match at least one element from the instance array. When pattern[x] is used to constrain a
  > complex object, it means that each property in the pattern must be present in the complex
  > object, and its value must recursively match -- i.e.,
  > - If primitive: it must match exactly the pattern value
  > - If a complex object: it must match (recursively) the pattern value
  > - If an array: it must match (recursively) the pattern value
  > If a pattern[x] is declared on a repeating element, the pattern applies to all repetitions. If
  > the desire is for a pattern to apply to only one element or a subset of elements, slicing must
  > be used." [defs: ElementDefinition.pattern[x]]
  eld-7 (one type only), eld-8. "In general, pattern[x] is not intended for use with primitive
  types, where is has the same meaning as fixed[x]."
  **merge: unstated** — whether a differential pattern must be compatible with (or may replace) an
  inherited base pattern/fixed is not addressed.
- **example (label 1..1 + value[x] 1..1, 0..*)** — diff: optional; prohibited on first element.
  Informative content. **merge: replace-vs-append unstated.**
- **minValue[x] / maxValue[x]** — diff: optional (prohibited in type definitions). Inclusive
  bounds; allowed types date/dateTime/instant/time/decimal/integer/integer64/positiveInt/
  unsignedInt/Quantity.
  > "Except for date/date/instant, the type of the minValue[x] SHALL be the same as the specified
  > type of the element. For the date/dateTime/instant values, the type of minValue[x] SHALL be
  > either the same, or a Duration which specifies a relative time limit to the current time. ...
  > A minimum value for a Quantity is interpreted as a canonical minimum - e.g. you cannot provide
  > 100mg if the minimum value is 10g." [defs: ElementDefinition.minValue[x]] (maxValue mirror-image)
  merge: unstated (no narrowing rule stated).
- **maxLength** — diff: optional (prohibited in type definitions). "If not specified, there is no
  conformance expectation for length support." SHOULD only be used on primitives with a string
  representation. merge: unstated.
- **mustHaveValue / valueAlternatives** — Trial Use, not in the interpretation table (see §3.4).
  eld-28: mutually exclusive (valueAlternatives prohibited when mustHaveValue=true).
  valueAlternatives missing = "any extension can be used if its context is appropriate".

### Constraints & conditions

- **constraint** — diff: optional∆ — "Additional constraints ... can be defined, but they do not
  replace the ones in the base definition." I.e. in a snapshot, inherited constraints persist and
  differential constraints are appended. eld-14: "Constraints must be unique by key" (within one
  ElementDefinition). eld-21 (warning): should have an expression. eld-26: "Errors cannot be
  suppressed" (`severity='error' implies suppress.empty()`).
  - `key` 1..1 id, `severity` 1..1 (error|warning), `human` 1..1, `expression` 0..1 FHIRPath,
    `requirements` 0..1 markdown, `suppress` 0..1 boolean (Trial Use).
  - `source` 0..1 canonical(StructureDefinition):
    > "A reference to the original source of the constraint, for traceability purposes." ...
    > "This is used when, e.g. rendering, where it is not useful to present inherited constraints
    > when rendering the snapshot." [defs: ElementDefinition.constraint.source]
    So generators are expected to stamp/preserve `source` on inherited constraints in snapshots.
  - `suppress`: "should only be present in a derived profile where a warning or hint has been
    determined to be spurious/incorrect."
- **condition** — diff: optional∆ (additional, do not replace). "A reference to an invariant that
  may make additional statements about the cardinality or value in the instance." Merge = union.

### Flags

- **mustSupport** — diff: optional (both first and following elements). Inheritance rule quoted in
  §3.6. Direction is one-way:
  > "A profile on a type can always make mustSupport = true if it is false in the base type but
  > cannot make mustSupport = false if it is true in the base type."
  > [defs: ElementDefinition.mustSupport, Comments]
- **isModifier (+ isModifierReason)** — diff: optional† (presence and value must match base).
  > "Only the definition of an element can set IsModifier true - either the specification itself
  > or where an extension is originally defined. Once set, it cannot be changed in derived
  > profiles (except in the special case of the defining a new extension)."
  > [defs: ElementDefinition.isModifier, Comments]
  eld-18: isModifier=true requires isModifierReason.
- **isSummary** — diff: optional† (must match base). Missing = not in summary.
- **sliceName** — diff: "required for slices, else prohibited". `[ch4][ch6]`
  > "The name SHALL be unique within the structure within the context of the constrained resource
  > element. (Though to avoid confusion, uniqueness across all elements is recommended.)"
  > [defs: ElementDefinition.sliceName, Comments]
  eld-16: `sliceName.matches('^[a-zA-Z0-9\\/\\-_\\[\\]\\@]+$')` ("proper tokens separated by /").
- **sliceIsConstraining** — Trial Use, not in the interpretation table. `[ch6]`
  > "If true, indicates that this slice definition is constraining a slice definition with the
  > same name in an inherited profile. If false, the slice is not overriding any slice in an
  > inherited profile. If missing, the slice might or might not be overriding a slice in an
  > inherited profile, depending on the sliceName." [defs: ElementDefinition.sliceIsConstraining]
  > "If set to true, an ancestor profile SHALL have a slicing definition with this name. If set to
  > false, no ancestor profile is permitted to have a slicing definition with this name." [Comments]
  eld-22: only when sliceName is present. This implies the default slice-matching rule across
  profiles: slices in a derived profile match ancestor slices *by sliceName*.
- **binding (strength / description / valueSet / additional)** — diff: optional.
  - eld-23: "binding SHALL have either description or valueSet"; eld-12: valueSet SHALL start with
    http/https/urn:/#; eld-11: binding only on code/Coding/CodeableConcept/Quantity/string/uri/
    Duration (or non-FHIR types); valueSet reference "may be version-specific or not".
  - `additional` (Trial Use; purpose 1..1 of maximum|minimum|required|extensible|candidate|
    current|preferred|ui|starter|component, valueSet 1..1, documentation, shortDoco, usage, any):
    > "Additional bindings do not replace the main binding but provide more information and/or
    > context." [defs: ElementDefinition.binding.additional]
    > "Conformance bindings are in addition to the base binding, not instead of it."
    > [defs: ElementDefinition.binding.additional.purpose, Comments]
    `any`: "Whether the binding applies to all repeats, or just to any one of them."
  - **merge: unstated** — whether a differential binding replaces the base binding wholesale or
    per-sub-element, and whether strength may only be tightened, is not on this page (strength
    rules live on terminologies.html).
- **mapping (identity / language / map / comment)** — diff: optional∆ (additional, do not replace
  base mappings). eld-27 (warning): unique by identity. Merge = union/append.
- **slicing** — see §5 below.

## 5. Slicing component semantics `[ch6]`

[defs: ElementDefinition.slicing and children; elementdefinition / #definition]

Definition (verbatim — includes the slice-set boundary rule, load-bearing for `ch4` matching):

> "Indicates that the element is sliced into a set of alternative definitions (i.e. in a structure
> definition, there are multiple different constraints on a single element in the base resource).
> Slicing can be used in any resource that has cardinality ..* on the base resource, or any
> resource with a choice of types. The set of slices is any elements that come after this in the
> element sequence that have the same path, until a shorter path occurs (the shorter path
> terminates the set)." [defs: ElementDefinition.slicing]

> "The first element in the sequence, the one that carries the slicing, is the definition that
> applies to all the slices. This is based on the unconstrained element, but can apply any
> constraints as appropriate. This may include the common constraints on the children of the
> element." [defs: ElementDefinition.slicing, Comments]

- **discriminator** (0..*):
  > "Designates which child elements are used to discriminate between the slices when processing
  > an instance. If one or more discriminators are provided, the value of the child elements in
  > the instance data SHALL completely distinguish which slice the element in the resource matches
  > based on the allowed values for those elements in each of the slices."
  > [defs: ElementDefinition.slicing.discriminator]
  "If there is no discriminator, the content is hard to process, so this should be avoided."
  - `type` 1..1: `value | exists | type | profile | position` (position is new in R5).
    "'pattern' is deprecated - it works exactly the same as 'value'." [defs: ...discriminator.type]
  - `path` 1..1: "A FHIRPath expression, using the simple subset of FHIRPath, that is used to
    identify the element on which discrimination is based."
- **description** 0..1: "If there is no discriminator, this is required to be present to provide
  whatever information is possible about how the slices can be differentiated."
- **ordered** 0..1 boolean; missing = "Order is not required unless specified". "requiring ordered
  data makes the profile much less re-usable."
- **rules** 1..1: `closed | open | openAtEnd` — "Whether additional slices are allowed or not.
  When the slices are ordered, profile authors can also say that additional slices are only
  allowed at the end."

Interpretation-table status: slicing is "optional" only for "Constraint Definition, following
elements" — consistent with §3.1, since the slicing entry (first repetition of the sliced
element) is always a *following* element relative to the structure root. Merge across profiles
(reslicing an already-sliced base, compatibility of redefined discriminators): **unstated on
this page**.

## 6. eld-* invariants `[ch12]`

[elementdefinition / "Constraints" table] — verbatim (key, level, location, text; FHIRPath
condensed where obvious). These define what a *valid* generated snapshot/differential element
looks like; a generator should enforce/preserve them.

| Key | Level | Location | Text |
|---|---|---|---|
| eld-2 | Rule | (base) | "Min <= Max" — `min.empty() or max.empty() or (max = '*') or iif(max != '*', min <= max.toInteger())` |
| eld-3 | Rule | max | "Max SHALL be a number or \"*\"" |
| eld-4 | Rule | type | "Aggregation may only be specified if one of the allowed types for the element is a reference" (Reference, canonical, CodeableReference) |
| eld-5 | Rule | (base) | "if the element definition has a contentReference, it cannot have type, defaultValue, fixed, pattern, example, minValue, maxValue, maxLength, or binding" |
| eld-6 | Rule | (base) | "Fixed value may only be specified if there is one type" — `fixed.empty() or (type.count() <= 1)` |
| eld-7 | Rule | (base) | "Pattern may only be specified if there is one type" — `pattern.empty() or (type.count() <= 1)` |
| eld-8 | Rule | (base) | "Pattern and fixed are mutually exclusive" |
| eld-11 | Rule | (base) | "Binding can only be present for coded elements, string, and uri if using FHIR-defined types" (code, Coding, CodeableConcept, Quantity, string, uri, Duration; or type.code with ':') |
| eld-12 | Rule | binding | "ValueSet SHALL start with http:// or https:// or urn: or #" |
| eld-13 | Rule | (base) | "Types must be unique by code" — `type.select(code).isDistinct()` |
| eld-14 | Rule | (base) | "Constraints must be unique by key" — `constraint.select(key).isDistinct()` |
| eld-15 | Rule | (base) | "default value and meaningWhenMissing are mutually exclusive" |
| eld-16 | Rule | (base) | "sliceName must be composed of proper tokens separated by \"/\"" — `sliceName.matches('^[a-zA-Z0-9\\/\\-_\\[\\]\\@]+$')` |
| eld-17 | Rule | type | "targetProfile is only allowed if the type is Reference or canonical" (also CodeableReference per expression) |
| eld-18 | Rule | (base) | "Must have a modifier reason if isModifier = true" |
| eld-19 | Rule | (base) | "Element path SHALL be expressed as a set of '.'-separated components with each component restricted to a maximum of 64 characters and with some limits on the allowed choice of characters" |
| eld-20 | Warning | (base) | "The first component of the path should be UpperCamelCase. Additional components (following a '.') should be lowerCamelCase." |
| eld-21 | Warning | constraint | "Constraints should have an expression or else validators will not be able to enforce them" |
| eld-22 | Rule | (base) | "sliceIsConstraining can only appear if slicename is present" |
| eld-23 | Rule | binding | "binding SHALL have either description or valueSet" |
| eld-24 | Guideline | (base) | "pattern[x] should be used rather than fixed[x]" — "pattern[x] is generally preferred over fixed[x] because it doesn't preclude the use of id and additional extensions" |
| eld-25 | Warning | (base) | "Order has no meaning (and cannot be asserted to have meaning), so enforcing rules on order is improper" — `orderMeaning.empty() implies slicing.where(rules='openAtEnd' or ordered).exists().not()` |
| eld-26 | Rule | constraint | "Errors cannot be suppressed" — `(severity = 'error') implies suppress.empty()` |
| eld-27 | Warning | (base) | "Mappings SHOULD be unique by key" — `mapping.select(identity).isDistinct()` |
| eld-28 | Rule | (base) | "Can't have valueAlternatives if mustHaveValue is true" |

(eld-1, eld-9, eld-10 do not exist in R5 — keys retired in earlier releases.)

Generator-relevant consequences (`ch12`): after merging, a snapshot element with >1 type must not
carry fixed/pattern (eld-6/7); merged constraint lists must stay key-unique (eld-14) — which is in
tension with the ∆ "do not replace" rule when a differential restates an inherited key; a merged
type list must stay code-unique (eld-13); an element that ends up with a contentReference must
have none of the eld-5-listed properties.

## 7. R5 deltas vs R4/R4B (from the page's diff table)

- discriminator.type: added code `position`.
- Added elements: `constraint.suppress`, `mustHaveValue`, `valueAlternatives`,
  `binding.additional.*` (all flagged Trial Use).
- `constraint.xpath` deleted.
- `constraint.requirements`, `binding.description`, `mapping.comment`: string → markdown.
- fixed/pattern/defaultValue/example.value: types integer64, CodeableReference, RatioRange,
  Availability, ExtendedContactDetail, Meta added.
- type.code binding changed to the "Element Definition Types" value set.

---

## 8. Spec gaps noticed

Places where an implementer of snapshot generation needs an answer and this page (and its
definitions page) is silent or self-contradictory. These gaps are as load-bearing as the rules.

1. **No merge algorithm anywhere.** The page defines what a valid differential element and a
   valid snapshot element each look like (the #interpretation table), but never states HOW to
   compute snapshot = base + differential. Even "differential value replaces base value in the
   snapshot" is only stated explicitly for min/max (§3.3) and mustSupport (§3.6). Everything else
   is inference.
2. **Replace vs append unstated for repeating properties**: `code`, `alias`, `example`,
   `type.profile`, `type.targetProfile`. Only `constraint`, `condition`, `mapping` get the ∆
   "additional, do not replace" rule, and `binding.additional`/representation are addressed
   indirectly. Whether a differential's `code` list replaces or extends the base's is undefined.
3. **Binding merge granularity unstated.** Does a differential `binding` replace the base binding
   wholesale (dropping base description/extensions if omitted) or merge per-sub-element? Is a
   derived profile's `additional` list appended to the base's? May strength only be tightened?
   None of that is here (strength progression rules live on terminologies.html, not extracted on
   this page).
4. **eld-14 vs ∆-additivity tension.** Inherited constraints "do not replace" + differential
   constraints are added, but constraints "must be unique by key" within an element. What a
   generator must do when a differential restates an inherited key (treat as identical
   restatement? error? replace?) is unstated.
5. **Type list merging unstated.** "Type ... can be left blank in a differential ... inherited
   from the resource" is the only merge rule for `type`. Subset requirements, and how
   profile/targetProfile/aggregation on a matching type code combine with the base's, are not here
   (subset rule is on profiling.html).
6. **Renamed choice-type paths.** §3.2 mandates constraining via the `[x]` element and type slices
   (`Patient.deceased[x]:deceasedBoolean` ids), and requires the original `[x]` element in the
   snapshot; but whether a differential may use a *renamed* path (`Patient.deceasedBoolean`) —
   common in R4-era profiles — is neither sanctioned nor prohibited on this page.
7. **Slice matching across profiles is only implied.** sliceIsConstraining's definition implies
   ancestor slices are matched by sliceName, but there is no explicit rule for merging a
   differential slice onto a base slice, for reslicing an inherited slice
   (`slicename/reslicename` id syntax exists but no semantics here), or whether a differential may
   redefine `slicing.discriminator`/`rules`/`ordered` on an already-sliced base element (replace?
   must-be-compatible?).
8. **Interpretation-table quirks (treat as errata):**
   (a) the `base` row says "required" for *both* Constraint Definition columns while the Type
   Definition columns distinguish "snapshot: required / differential: optional" — read literally a
   constraint differential must populate base, contradicting universal practice; the defs page
   ("the base information must always be populated in element definitions in snap shots") resolves
   the intent: base is a snapshot obligation, differentials omit it and the generator fills it.
   (b) the row named `nameReference` is the DSTU2-era name of `contentReference` — the table was
   never updated.
9. **Interpretation table has no rows for R5-new elements** — `mustHaveValue`,
   `valueAlternatives`, `sliceIsConstraining`, `binding.additional` — nor for `representation`,
   `orderMeaning`, `isModifierReason`, `mustSupport`-on-first-element nuances beyond the existing
   row. Which contexts those are legal in must be reconstructed from their Comments
   (representation: base-standard-only; orderMeaning: resources/datatypes only; the Trial-Use
   trio: constraint-only by their nature — but formally unstated).
10. **contentReference + profiling.** eld-5 bars type/value-constraints on a contentReference'd
    element, and contentReferences "cannot be changed and always reference the non-constrained
    definition" — but nothing says whether/how a generator may expand the referenced content into
    the snapshot when a profile constrains children of such an element (e.g.
    Questionnaire.item.item). `[ch8]`
11. **min/max narrowing not on this page.** §3.3 gives defaulting rules only; the requirement that
    a profile's cardinality stays within the base's bounds is on profiling.html — this page's only
    hint is note § (about type-root cardinality constraining *references* to the type).
12. **Cross-property consistency after merge.** No rule states which invariants (eld-5/6/7/11/13/14)
    must be *re-verified after merging* differential onto base — e.g. base has pattern, differential
    narrows type list to 2 types → eld-7 violation that neither input had. Error handling for such
    merge-created invalidity is entirely unspecified. `[ch12]`
