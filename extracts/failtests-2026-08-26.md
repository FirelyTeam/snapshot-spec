# Fail-test mining — the 21 `fail="true"` tests of the r5/snapshot-generation suite

> Versions: .NET = Hl7.Fhir.R5 6.2.1 | Java engine = 6.10.2 (d06577dbc5c6) | golden = fhir-test-cases 1.7.67
>
> Harness: three-way run 2026-08-26 (`C:\Git\snapshot-spec-materials\harness\out\<id>\`).
> .NET runner settings: **`ForceRegenerateSnapshots = true` + `GenerateSnapshotForExternalProfiles = true`**
> (`harness/runner-dotnet/Program.cs:88-89`) — several .NET outcomes below are conditional on these
> settings; flagged where relevant.
> Chapter citations refer to `C:\Git\firely-net-sdk\docs\snapshot-spec\` (branch `docs/snapshot-spec`).

**Headline:** Java behaves per the golden fail expectation on all 21. .NET satisfies the fail expectation
on only **8/21** (4 THREW + 4 finished with error issues) and **silently generates a snapshot on 13/21**.
Of the 8, only five fail for the *intended* reason (obs-1-2, obs-2-3, obs-3, t37, and — matching in
substance but not severity — t15a); ihe1, t29b, and sushi3 fail for side-effect or harness-induced reasons.
Two of the 13 silent tests produce **corrupt output**, not merely missing validation (t23a duplicate
element id + fabricated `base`; obs-unit phantom element + dropped constraint).

## 1. Summary table

| id | intent (one-liner) | Java | .NET | explaining .NET gap | DEV tag |
|---|---|---|---|---|---|
| t15a | unknown extension profile URL | THREW `DefinitionException` | ERRORS(6), generated | unresolvable profile = issue + continue, not fatal (ch07/ch12) | — |
| t23a | out-of-order differential | ERRORS(2), no match for diff element | NO-THROW, **corrupt**: duplicate `males.telecom` | ordering assumed, never verified (ch02) | — |
| t29a | wrong type-slice name (`Coding` ≠ `valueCoding`), closed slicing | THREW `FHIRException` | NO-THROW, slice kept as named | slice names never validated (ch04/ch06) | (rel. DEV-020) |
| t29b | SD.type (`OperationOutcome`) ≠ base type (`Parameters`) | THREW `DefinitionException` (matches manifest regex) | ERRORS(1) — unrelated contentReference symptom | SD.type/base coherence never checked (ch03) | — |
| t37 | typo in path (`MedicationRequiest`) | THREW `FHIRException` (sort failed) | THREW `InvalidOperationException` (multiple roots) — **same author error** | tree constructor throw, ch02 `:69-78` | DEV-014 |
| t43a | wrong type-slice name (`Quantity` ≠ `valueQuantity`) | THREW `FHIRException` | NO-THROW, slice kept | slice names never validated (ch04/ch06) | DEV-014 |
| obs-1-2 | profile a type already profiled out | THREW `DefinitionException` | THREW `InvalidOperationException` — **same reason** | type-widening throw in matcher (ch04) | DEV-003 → agreement |
| obs-2-3 | profile a type not in remaining slices | THREW `DefinitionException` | THREW `InvalidOperationException` — **same reason** | idem | DEV-005 → agreement |
| obs-3 | profile a type not in the element | THREW `DefinitionException` | THREW `InvalidOperationException` — **same reason** | idem | DEV-005 → agreement |
| obs-5 | two type slices, one min=1, element max=1 | THREW `FHIRException` | NO-THROW, both slices emitted | slice cardinality arithmetic unchecked (ch06 `:242`) | DEV-007 |
| obs-badfixed | `fixedUri` on value[x] with no uri type | ERRORS(1) | NO-THROW, `fixedUri` merged as written | fixed/pattern type-compat never validated (ch05) | — |
| obs-badpattern | `patternUri` on value[x] with no uri type | ERRORS(1) | NO-THROW, `patternUri` merged as written | idem | — |
| ihe1 | constrain `Bundle.entry.resource` (type Resource) to `Reference` | THREW `DefinitionException` | ERRORS(2) — but only because DocumentManifest is unresolvable in R5 | type replace is wholesale, no derivation check (ch05) | — |
| ihe2 | profile DocumentReference on element typed OperationOutcome | ERRORS(1) | NO-THROW, type+profile merged as written | idem | — |
| obs-unit | `..` in path (`Observation...unit`) | THREW `FHIRException` | NO-THROW, **corrupt**: phantom `Observation.` element, constraint dropped | path syntax unvalidated; stand-in parent synthesis (ch02) | — |
| sushi3 | duplicate diff element ids / missing sliceName | ERRORS(2) same-id | ERRORS(2) — **different reason** (harness-induced: unresolvable grandparent base) | duplicate ids: debug-only warning (ch02); failure here from `ForceRegenerateSnapshots` | — |
| ext-recursion-1 | `type.profile` = self **on the root element** | THREW (type on first diff element) | NO-THROW, self-type merged onto root | root-element invariants (sdf-15a) unvalidated; root type never expanded (ch02/ch11) | — |
| simplifier-1 | slice a non-repeating element (`Reference.identifier`, max=1) | THREW `DefinitionException` | NO-THROW, slice emitted and expanded | `REJECT_SLICE_NONREPEATING_ELEMENT` compiled out; issue 10003 unreachable (ch12) | — |
| obs-ms-bad | mustSupport true→false | ERRORS(1) | NO-THROW, `mustSupport=false` in snapshot | mustSupport direction not enforced (ch05 `:251`) | — |
| ext-ccuk | slicing entry on the root (`Extension`) | THREW `DefinitionException` | NO-THROW, root slicing kept in snapshot | sdf-20 never validated; only root *sliceName* repaired (ch02) | — |
| mi-use-distinct | targetProfile not derived from base's targetProfile | THREW `DefinitionException` | NO-THROW, targetProfile replaced as written | targetProfile replaces wholesale, no derivation check (ch05 `:279`, ch09) | — |

## 2. Cross-cutting grouping of the 13 NO-THROW tests

1. **Slice-name conventions unvalidated** — t29a, t43a. Java enforces that a renamed-choice type slice's
   `sliceName` equals the renamed element name (`valueCoding`/`valueQuantity`); .NET generates *missing*
   type-slice names (`GENERATE_MISSING_TYPE_SLICE_NAMES`, ch04) but never validates author-supplied ones
   (eld-16 grammar and naming conventions never checked, ch06 `:223`). t29a additionally shows the
   DEV-020/OQ-020 family behavior: .NET normalizes the diff's `valueCoding:Coding` entry to snapshot id
   `Parameters.parameter.part:foo.value[x]:Coding`.
2. **Ordering assumed → corrupt output** — t23a. "Ordering is assumed, never verified" (ch02 `:96-98`);
   misordered input silently degrades in the forward-only matcher (ch04 `:94-107`). Result here is not
   just missing validation but a **corrupt snapshot** (duplicate element id, fabricated `base` with the
   diff's own `min`).
3. **Slice cardinality arithmetic unchecked** — obs-5. §5.1.0.14 sums "not checked" (ch06 `:242`).
4. **fixed[x]/pattern[x] type compatibility unchecked** — obs-badfixed, obs-badpattern. Fixed/pattern
   merge is overlay-or-replace with no validation against the element's type list (ch05 `:242-243`;
   the spec itself leaves base-compatibility unstated, ch05 `:105-106`).
5. **type / targetProfile derivation unchecked (wholesale replace)** — ihe2, mi-use-distinct (and ihe1's
   near-miss). Diff type lists replace wholesale (ch05 `:279`, RFC-008); the only compatibility check .NET
   has is `isValidTypeProfile` on *expanded* `type.profile` targets (ch07 `:131-137`), which never runs
   for `targetProfile` and runs against the **replaced** type code — that is why ihe2 is silent:
   DocumentReference does derive from the diff's replaced `Resource`, while the OperationOutcome→Resource
   widening itself goes unexamined. No interface/imposeProfile semantics at all (ch09 `:102-109`).
6. **Path syntax unvalidated → corrupt output** — obs-unit. The tree constructor splits on `.` and
   synthesizes stand-in parents (ch02 `:80-98`); an empty segment becomes a phantom element and the
   author's constraint is silently lost.
7. **Root-element invariants (sdf-15a/20) unvalidated** — ext-recursion-1 (type on root), ext-ccuk
   (slicing on root). .NET's only root repair is `FIX_SLICENAMES_ON_ROOT_ELEMENTS` for a root
   `sliceName` (ch02); root `type` and root `slicing` pass through unexamined.
8. **Non-repeating-element slicing check compiled out** — simplifier-1. .NET *has* the check but behind
   the disabled `REJECT_SLICE_NONREPEATING_ELEMENT` conditional; issue 10003 INVALID_SLICE is
   "unreachable" (ch12 issue catalog).
9. **mustSupport direction unenforced** — obs-ms-bad. "replace — true→false possible; spec allows only
   false→true; not enforced" (ch05 `:251`).

Recurring theme for the WGM brief: .NET's stated policy is that the generator "should never throw" and
correctness belongs to the validator (ch04 `:158-164`), so 11 of 13 silent cases are validation gaps by
design — but groups 2 and 6 show the same policy producing **silently corrupt snapshots**, which no
downstream validator can be expected to repair.

## 3. Special exhibit — recursion crossover (ext-recursion-1 vs ext-recursion-2 / logical-goo)

The recursion behaviors of the two sides *cross over*: the one recursive input Java rejects is silently
accepted by .NET, and the two recursive inputs Java generates are hard-rejected by .NET (under the harness
settings — see caveat below).

| test | structure | Java | .NET (harness settings) |
|---|---|---|---|
| ext-recursion-1 (fail test) | Extension SD; differential **root** element `Extension` carries `type = Extension, profile = <its own url>` | THREW `Error: Type on first differential element!` — rejected structurally (type on root), before recursion is even a question | NO-THROW: 5-element ordinary Extension snapshot with the self-referencing `<type><profile>` merged onto the root. Root type is neither validated (sdf-15a, ch02/ch12) nor expanded — expansion only happens where the diff constrains children (ch11 §1), and there are none, so the cycle is never entered |
| ext-recursion-2 (gen test, golden expects success) | Extension SD; diff slices `Extension.extension:recurse` with `type = Extension, profile = <its own url>` | Generated; one WARNING "The type of profile ...ext-recursion-2 cannot be checked as the profile is not known" (its own snapshot is still in progress). Java records the type and moves on; the slice has no diff children, so it never expands into the profile | THREW `NotSupportedException: ... Recursive profile dependency detected for profile 'http://hl7.org/fhir/test/StructureDefinition/ext-recursion-2' on element ''` — type-profile merge resolves the profile and `ensureSnapshot` re-enters a url already on the `SnapshotRecursionStack` (ch11 §2; eager ensure via `GenerateSnapshotForExternalProfiles`, ch07 `:138-139`) |
| logical-goo (gen test, golden expects success) | Logical model; input SD has **url = baseDefinition = `http://example.org/StructureDefinition/Boo`** (root element `Goo`); the register also supplies `logical-boo.xml` — same canonical url, snapshot included, base `.../Foo` | Generated silently (no log output). Java resolves the base canonical to the registered Boo and uses its shipped snapshot; no re-entry | THREW `NotSupportedException: ... Recursive profile dependency detected for profile 'http://example.org/StructureDefinition/Boo' on element 'Goo'` — `generate(Boo)` → `ensureSnapshot(base = Boo)`: same URI already on the stack; the guard is keyed purely by canonical URI (ch11 §2), so a profile whose base shares its url is a hard failure regardless of which SD the resolver would return |

**Settings caveat:** both .NET throws are conditional on the harness config
(`ForceRegenerateSnapshots = true`, `GenerateSnapshotForExternalProfiles = true`). With shipped snapshots
honored, logical-goo's base (registered Boo, snapshot present) would need no `ensureSnapshot` re-entry, and
ext-recursion-2's eager external-profile ensure would not fire the same way. Default-settings .NET behavior
for these two is **unverified** — worth one targeted re-run before the WGM brief. The asymmetry of
ext-recursion-1 (Java structural rejection vs .NET silent acceptance) is *not* settings-dependent.

## 4. Per-test dossiers

### t15a — unknown extension
- **Intent:** manifest `fail="true" description="Unknown extension" allow="none"`. Diff slices
  `Patient.address.extension:Geolocation` with `type.profile = http://hl7.org/fhir/StructureDefinition/geolocationX`
  (nonexistent) and a child slice with the bare profile value `latitude`.
- **Java:** `EXCEPTION: DefinitionException: Unable to find Extension definition for http://hl7.org/fhir/StructureDefinition/geolocationX`.
- **.NET:** ERRORS(6), generated. Three error pairs: `Unable to resolve reference to profile
  'http://hl7.org/fhir/StructureDefinition/geolocationX'` (×3 variants) and `... to profile 'latitude'`
  (×3). Snapshot contains the Geolocation and latitude slices with the unresolvable
  `<type><profile>` values kept verbatim, children default-expanded from core Extension.
- **Explanation:** unresolvable external profile → `UNAVAILABLE_REFERENCED_PROFILE` issue + continue
  without merge (ch07 `:129-131`); fatal only for the SD's *own* base at top level (ch12 issue catalog).
  Same error *detected* as Java, different severity model (issue-and-continue vs hard throw).
- **Tag:** —

### t23a — unsorted differential
- **Intent:** `description="Test Slicing - unsorted differential"`. Slice `Patient.contact:males`; diff
  lists `males.gender` (base position 5) *before* `males.telecom` (base position 3) — children out of
  base order.
- **Java:** ERRORS(2): `No match found for Patient.contact:males.gender in the generated snapshot: check
  that the path and definitions are legal in the differential (including order)` + the profile-level
  summary error.
- **.NET:** NO-THROW, empty log. **Corrupt snapshot:** `males.gender` *was* matched in place (carries
  `fixedCode="male"`), but `males.telecom` — behind the base cursor by then — was appended as a **second
  `Patient.contact:males.telecom` element** after `males.period`, duplicating the id and carrying a
  fabricated `<base><path value="Patient.contact.telecom"/><min value="1"/></base>` (the diff's own min
  copied into `base`, wrong).
- **Explanation:** ch02 `:96-98` "Ordering is assumed, never verified"; ch04 `:94-107` forward-only walk
  ("the base cursor never moves backwards") — misordered input "silently degrades".
- **Tag:** — (candidate new register row: out-of-order diff → duplicate-id corrupt snapshot).

### t29a — closed type slicing, wrong slice name
- **Intent:** recursion-depth path (`Parameters.parameter.part`) + type slicing of
  `part:foo.value[x]` where the renamed-form slice is named `Coding` (diff id
  `...part:foo.valueCoding:Coding`), not the required `valueCoding`.
- **Java:** `EXCEPTION: FHIRException: Error at path Parameters.parameter: Slice name must be 'valueCoding' but is 'Coding'`.
- **.NET:** NO-THROW. Emitted the slice, normalized to id `Parameters.parameter.part:foo.value[x]:Coding`
  (the diff's `valueCoding` rename folded back to `[x]` form), fully expanded Coding children.
- **Explanation:** .NET never validates author-supplied slice names (ch04 choice-type matching accepts any
  sliceName; ch06 `:223` eld-16 grammar never validated). The `valueCoding:Coding` → `value[x]:Coding`
  entry rewrite is the DEV-020/OQ-020 normalization territory.
- **Tag:** related to DEV-020 (entry normalization); slice-name rule itself unregistered.

### t29b — SD.type mismatch
- **Intent:** manifest `fail="true" regex=".*OperationOutcome.*"`. SD declares `type = OperationOutcome`
  but `baseDefinition = .../Parameters` and all diff paths are `Parameters.*`.
- **Java:** `EXCEPTION: DefinitionException: Base & Derived profiles have different types
  (http://hl7.org/fhir/StructureDefinition/Parameters = Parameters vs urn:uuid:... = OperationOutcome)` —
  matches the manifest regex.
- **.NET:** ERRORS(1): `Parameters.parameter.part:foo: Element Parameters.parameter.part:foo has a
  nameReference to '#Parameters.parameter', which cannot be found in the StructureDefinition.` Produced a
  14-element snapshot ending at `Parameters.parameter.part:foo` — the slice's children never expanded
  (contentReference resolution failed). Note: the .NET message does **not** contain "OperationOutcome",
  so .NET's output would not satisfy the manifest regex; the regex-ok flag belongs to Java only.
- **Explanation:** .NET never checks SD.type coherence against the base's type (no such rule anywhere in
  ch03's base-resolution behavior); the type mismatch surfaces indirectly when the
  `#Parameters.parameter` contentReference cannot be resolved inside an SD whose type says
  OperationOutcome (issue 10002 INVALID_TYPEPROFILE_NAMEREF, ch12).
- **Tag:** —

### t37 — bad path (typo)
- **Intent:** `description="Test bad Path"` — fourth diff element's path is
  `MedicationRequiest.dosageInstruction.timing.event` (typo `Requiest`).
- **Java:** `EXCEPTION: FHIRException: Sort failed: [Sort failed: counts differ; at least one of the paths
  in the differential is illegal]` (detected by the sort preprocessor).
- **.NET:** `EXCEPTION: InvalidOperationException: Error in snapshot generator. Differential has multiple
  roots at 'MedicationRequiest'` (tree constructor synthesizes stand-in ancestors up to the single-segment
  root `MedicationRequiest`, which at index > 0 throws — ch02 `:83-90`, `DifferentialTreeConstructor.cs:69-78`).
- **Both threw for the same author error** (illegal path), via different detectors and different exception
  taxonomies (Java author-facing message vs .NET `InvalidOperationException`; OQ-014 error-taxonomy row).
- **Tag:** DEV-014 — for the *fail* direction this is now an agreement-on-outcome row.

### t43a — choice slicing, wrong slice name
- **Intent:** constrain `Observation.value[x]` to Quantity via type slicing; the slice is named `Quantity`
  instead of `valueQuantity`.
- **Java:** `EXCEPTION: FHIRException: Error at path Observation.value[x]: Slice name must be
  'valueQuantity' but is 'Quantity'`.
- **.NET:** NO-THROW. Snapshot has `Observation.value[x]:Quantity` (name kept as written), expanded
  Quantity children, `value[x]:Quantity.value` min=1 merged.
- **Explanation:** as t29a — slice names never validated (ch04/ch06).
- **Tag:** DEV-014.

### obs-1-2 — profile a type already profiled out
- **Intent:** manifest `reason="trying to profile a type already profiled out"`. Base (obs-1-1 chain,
  urn:uuid base) had closed-type-sliced `Observation.value[x]` down to CodeableConcept; this diff
  constrains a `Quantity` type slice.
- **Java:** `EXCEPTION: DefinitionException: ... at Observation.value[x]: invalid constrained type
  Quantity from CodeableConcept in urn:uuid:...`.
- **.NET:** `EXCEPTION: InvalidOperationException: Internal error in snapshot generator
  (ElementMatcher.constructChoiceTypeMatch): choice type of diff does not occur in snap,
  snap = 'Observation.value[x]', diff = 'Observation.value[x]'.`
- **Same reason on both sides** (type not among the base's remaining choice types); .NET's is the
  choice-type matcher's type-subset throw (ch04 `constructChoiceTypeMatch`, `typeIsSubSetOf` region —
  "the only author error in the matcher that escalates to an exception", OQ-014). Difference is purely
  taxonomy/message quality: Java names the offending and allowed types; .NET says "Internal error".
- **Tag:** DEV-003 — **flip to agreement row** (outcome agreement, message/taxonomy divergence only).

### obs-2-3 — profile a type not in remaining slices
- **Intent:** `reason="trying to profile an illegal type (not in slices remaining)"` — ContactPoint slice
  on a value[x] whose open slicing already consumed the choice.
- **Java:** `DefinitionException: ... invalid constrained type ContactPoint from Quantity,
  CodeableConcept, string, boolean, integer, Range, Ratio, SampledData, time, dateTime, Period,
  Attachment, Reference(...MolecularSequence) in urn:uuid:...`.
- **.NET:** identical `InvalidOperationException` as obs-1-2 (constructChoiceTypeMatch).
- **Same reason both sides.** **Tag:** DEV-005 — flip to agreement row.

### obs-3 — profile a type not in the element
- **Intent:** `reason="trying to profile an illegal type (not in element)"` — ContactPoint is not among
  `Observation.value[x]`'s types at all (base = core Observation).
- **Java:** `DefinitionException: ... invalid constrained type ContactPoint from ... in
  http://hl7.org/fhir/StructureDefinition/Observation`.
- **.NET:** same `InvalidOperationException` (constructChoiceTypeMatch).
- **Same reason both sides.** **Tag:** DEV-005 — flip to agreement row.

### obs-5 — conflicting type-slice minimums
- **Intent:** manifest `reason="More than one type slice has min=1"`. `Observation.value[x]` (0..1)
  sliced open with `valueCodeableConcept` min=1 and `valueQuantity` min=0..1 — the min=1 slice makes the
  other unsatisfiable.
- **Java:** `EXCEPTION: FHIRException: Invalid slicing: there is more than one type slice at
  Observation.value[x], but one of them (valueCodeableConcept) has min = 1, so the other slices cannot
  exist`.
- **.NET:** NO-THROW. Snapshot: `value[x]` 0..1 with both slices, `valueCodeableConcept` 1..1,
  `valueQuantity` 0..1 — the arithmetic contradiction emitted as-is.
- **Explanation:** slice cardinality arithmetic (§5.1.0.14 sums) "is not checked" (ch06 `:242`).
- **Tag:** DEV-007.

### obs-badfixed — fixed value of wrong type
- **Intent:** `reason="fixed value type mismatch"` — `fixedUri` on `Observation.value[x]`, whose 13-type
  choice does not include `uri`.
- **Java:** ERRORS(1): `The fixed value has type 'uri' which is not valid (valid types: [dateTime,
  string, Reference, Quantity, Period, Attachment, integer, Range, boolean, SampledData, Ratio, time,
  CodeableConcept])`.
- **.NET:** NO-THROW. Snapshot `value[x]` carries `fixedUri="http://example.org"` alongside the untouched
  full 13-type list.
- **Explanation:** fixed/pattern merged with no compatibility validation against the element's types
  (ch05 `:242`; base-compat also unstated by the spec, ch05 `:105-106`).
- **Tag:** —

### obs-badpattern — pattern value of wrong type
- **Intent / Java / .NET / explanation:** mirror of obs-badfixed with `patternUri`; Java ERRORS(1)
  `The pattern value has type 'uri' which is not valid (...)`; .NET NO-THROW, `patternUri` merged as
  written next to the full type list. Same gap (ch05 `:243`).
- **Tag:** —

### ihe1 — Reference on base of Resource
- **Intent:** `description="Use Reference on base of resource"` — diff sets `Bundle.entry.resource`
  (base type `Resource`) to `type = Reference, profile = .../DocumentManifest`.
- **Java:** `EXCEPTION: DefinitionException: Invalid constraint in profile ... at path
  Bundle.entry.resource - cannot constrain to type Reference{...DocumentManifest} from base types Resource`.
- **.NET:** ERRORS(2) (`Unable to resolve reference to profile
  'http://hl7.org/fhir/StructureDefinition/DocumentManifest'` ×2), generated. Snapshot
  `Bundle.entry.resource`: `<type><code value="Reference"/><profile value=".../DocumentManifest"/></type>`,
  min=1 — the illegal type constraint emitted as written.
- **Explanation:** the errors are **resolution artifacts** (DocumentManifest does not exist in R5), not a
  detection of the illegal Resource→Reference constraint. Counterfactual: had the profile been resolvable,
  .NET would still only have emitted an *issue* — `isValidTypeProfile` walks the resolved SD's base chain
  against the element's type code (ch07 `:131-137`), and DocumentManifest (→DomainResource→Resource) never
  derives from the **replaced** type `Reference`, so the result is issue 10009 INVALID_PROFILE_TYPE +
  continue, never Java's hard throw. The Resource→Reference type replacement itself is unchecked either way
  (ch05 `:279`). Evidence that the check runs against the *replaced* type, not the base type: ihe2's empty
  dotnet.log (DocumentReference derives from the replaced `Resource`; against the base type
  OperationOutcome it would have been flagged).
- **Tag:** —

### ihe2 — wrong profile for stated type
- **Intent:** `description="Use wrong profile"` — `type = Resource, profile = .../DocumentReference` on
  `Bundle.entry.resource` and on `Bundle.entry.response.outcome` (base type OperationOutcome).
- **Java:** ERRORS(1): `The profile http://hl7.org/fhir/StructureDefinition/DocumentReference has type
  DocumentReference which is not consistent with the stated type OperationOutcome`.
- **.NET:** NO-THROW, empty log. Both elements carry
  `<type><code value="Resource"/><profile value=".../DocumentReference"/></type>` min=1 — including the
  silent type *widening* OperationOutcome→Resource on `response.outcome`.
- **Explanation:** wholesale type replace with no derivation/consistency check outside choice elements
  (ch05 `:279`); DocumentReference *is* derived from Resource, so even the expansion-time
  `isValidTypeProfile` check passes against the replaced type.
- **Tag:** —

### obs-unit — `..` in path
- **Intent:** `description=".. in path"` — single diff element `path = "Observation...unit"` with
  `fixedString = "%"`.
- **Java:** `EXCEPTION: FHIRException: Invalid path 'Observation...unit' in differential in ...:
  name portion missing ('..')`.
- **.NET:** NO-THROW; one WARNING `Element Observation. has neither a type nor a nameReference`.
  **Corrupt snapshot:** contains a phantom element `<element id="Observation."><path value="Observation."/>
  <base><path value="Observation."/></base></element>`; the author's `fixedString "%"` appears **nowhere**
  in the output — the constraint was silently dropped.
- **Explanation:** the tree constructor splits on `.` and synthesizes a stand-in parent for the empty
  segment (ch02 `:80-90`); path syntax is never validated; the unexpandable phantom means the `...unit`
  leaf never merges.
- **Tag:** — (corrupt-output class, with t23a).

### sushi3 — duplicate diff element ids (NPE regression test)
- **Intent:** `description="Caused an NPE in the snapshot generator (but will fail because of the missing
  slice name)"`, `register="sushi3-dep"`. Diff contains **two elements with identical id/path
  `Observation.component` and no sliceName**. Base = registered `genotype` (sushi3-dep.xml, R4 artifact,
  ships **with** a snapshot), whose own base `.../genomics-reporting/StructureDefinition/finding` is not
  supplied.
- **Java:** ERRORS(2): `Same id 'Observation.component' on multiple elements
  Observation.component/Observation.component in http://hl7.org/fhir/sushi-test/StructureDefinition/mygenotype` (×2)
  — the intended error.
- **.NET:** ERRORS(2), no snapshot: `Unable to resolve reference to profile
  '.../StructureDefinition/finding'` + `Snapshot generation failed for profile with url '.../genotype'`.
- **Explanation:** **harness-induced agreement.** `ForceRegenerateSnapshots = true` discards genotype's
  shipped snapshot; regenerating it needs the absent `finding` → the run dies one level up and never
  reaches the duplicate-id input. .NET itself would not report the duplicate ids anyway — duplicate
  sibling constraints rate only a debug-build warning (ch02 `:97-98`). Default-settings .NET behavior
  (shipped dep snapshot honored) is **untested** — expected NO-THROW with a possibly NPE-free merge, worth
  one targeted run.
- **Tag:** —

### ext-recursion-1 — self-typed root
- See the recursion-crossover exhibit (§3). Java `EXCEPTION: Error: Type on first differential element!`;
  .NET NO-THROW, self-referencing `<type><profile>` merged onto the root of an otherwise ordinary
  5-element Extension snapshot. Gap: root-element invariants (sdf-15a) unvalidated — only root
  `sliceName` is repaired (ch02); root types are never expanded, so the cycle is never entered (ch11 §1).
- **Tag:** —

### simplifier-1 — slicing a non-repeating element
- **Intent:** `description="Complex nested slicing pattern from Simplifier definitions"` (4 registered
  dep SDs). The offending construct: slicing `Patient.identifier:versicherungsnummer_gkv.assigner.identifier`
  (Reference.identifier, max=1) into slice `iknr`.
- **Java:** `EXCEPTION: DefinitionException: Attempt to a slice an element that does not repeat:
  Reference.identifier/Reference.identifier from http://hl7.org/fhir/StructureDefinition/Reference in
  Patient.identifier:versicherungsnummer_gkv.assigner.identifier, at element iknr (slice = {5})`.
- **.NET:** NO-THROW. Snapshot keeps the slicing entry (`value:system`, open) on the max=1
  `...assigner.identifier` and emits the fully expanded `identifier:iknr` slice (incl. nested
  `type.coding:XX-Type` slicing).
- **Explanation:** .NET's check for exactly this exists but is compiled out —
  `REJECT_SLICE_NONREPEATING_ELEMENT` disabled, issue 10003 INVALID_SLICE "unreachable" (ch12 issue
  catalog).
- **Tag:** —

### obs-ms-bad — mustSupport true→false
- **Intent:** `register="obs-ms-base-input"`; base sets `Observation.value[x]` mustSupport=true, diff
  sets it to false — illegal loosening per profiling §5.1.0.22.
- **Java:** ERRORS(1): `Illegal constraint [must-support = false] when [must-support = true] in the base
  profile`.
- **.NET:** NO-THROW. Snapshot `value[x]` has `mustSupport = false` — the diff simply replaced the base's
  value.
- **Explanation:** ch05 `:251`: mustSupport "replace — `true`→`false` possible; spec allows only
  `false`→`true`; not enforced".
- **Tag:** —

### ext-ccuk — slicing on the root
- **Intent:** manifest `reason="trying to slice on the root"` — a UK CareConnect extension whose root
  `Extension` element carries a `slicing` entry (`value:url`, open); the sub-extensions are otherwise a
  normal complex extension.
- **Java:** `EXCEPTION: DefinitionException: Error: The profile has slicing at the root ('Extension'),
  which is illegal`.
- **.NET:** NO-THROW. Snapshot root `Extension` element retains
  `<slicing><discriminator><type value="value"/><path value="url"/></discriminator><rules value="open"/></slicing>`;
  the sub-extension slices generate normally.
- **Explanation:** sdf-20 (no slicing on root) is never validated; .NET's only root repair concerns
  `sliceName` (ch02 root-sliceName repair; ch12 input-error catalogue lists sdf-20 as detectable but ch06
  documents no .NET check).
- **Tag:** —

### mi-use-distinct — invalid targetProfile constraint
- **Intent:** manifest `reason="invalid constrained type Reference from Reference in .../mi-use-distinct"`,
  `register="mi-defn-base,mi-defn-distinct,mi-use-base"`. Base profile constrains `Observation.subject` to
  `Reference(mi-defn-base)`; this diff re-targets to `Reference(mi-defn-distinct)`, which is **not**
  derived from mi-defn-base.
- **Java:** `EXCEPTION: DefinitionException: ... at Observation.subject: invalid constrained type
  Reference(.../mi-defn-distinct) from Reference(.../mi-defn-base) in .../mi-use-base` (+ ERROR: `The
  target profile .../mi-defn-distinct is not a valid constraint on the base`).
- **.NET:** NO-THROW, empty log. Snapshot `Observation.subject`:
  `<type><code value="Reference"/><targetProfile value=".../mi-defn-distinct"/></type>` — the illegal
  target substituted as written.
- **Explanation:** `targetProfile` lists "replace wholesale" with no derivation walk (ch05 `:279`,
  `mergeCanonicals`); `isValidTypeProfile` applies only to expanded `type.profile` values, never
  `targetProfile` (ch07); and .NET has no interface/imposeProfile-adjacent conformance machinery at all
  (ch09 `:102-109` — "no support for imposeProfile/compliesWithProfile or any interface-conformance
  semantics").
- **Tag:** — (mi-* family currently only covered by DEV-015 coverage-gap row).

## 5. Register/action notes

- **DEV-003 (obs-1-2), DEV-005 (obs-2-3/obs-3):** convert from "shared-suite divergence" to
  **agreement-on-outcome** rows — both sides refuse for the same reason; residual divergence is error
  taxonomy (Java author-facing `DefinitionException` vs .NET "Internal error"
  `InvalidOperationException`, OQ-014).
- **DEV-014 (t37, t43a):** t37 = agreement-on-outcome (different detectors, same author error);
  t43a = genuine divergence (Java enforces the type-slice naming convention, .NET silent).
- **DEV-007 (obs-5):** confirmed as "slice cardinality arithmetic unchecked" (ch06 `:242`).
- **New-row candidates:** (a) out-of-order diff → duplicate-id corrupt snapshot (t23a);
  (b) `..` path → phantom element + dropped constraint (obs-unit); (c) slice-name convention validation
  absent (t29a/t43a); (d) fixed/pattern type-compat validation absent (obs-badfixed/badpattern);
  (e) type/targetProfile derivation validation absent (ihe1/ihe2/mi-use-distinct); (f) root
  type/slicing invariants unvalidated (ext-recursion-1/ext-ccuk); (g) non-repeating-element slicing
  check compiled out (simplifier-1); (h) mustSupport direction unenforced (obs-ms-bad); (i) SD.type/base
  coherence unchecked (t29b); (j) recursion-guard crossover incl. same-url base hard failure
  (logical-goo/ext-recursion-2 — re-run under default settings first).
