# Java deep-read — ch07 type-profile & extension expansion + ch03 base resolution (packet J-d, 2026-09-01)

Source: `org.hl7.fhir.core` @ `b06c7ee` (clone), citations re-checked against master `4f52ba6` where noted
(PU shifted +1 line from PU:222 onwards on master: 223→224, 448→449, 1624→1625; PPP unchanged at every
cited line). PU = ProfileUtilities.java, PPP = ProfilePathProcessor.java, PRE = SnapshotGenerationPreProcessor.java.
Message texts from `org.hl7.fhir.utilities/.../Messages.properties` (fetched from the blob store, same commit).
.NET contrast = ch07/ch03 ".NET behavior" sections (already in the doc).

## A. Where a type profile enters the Java walk

Java has no `mergeTypeProfiles`. A `type.profile` on a differential row influences three separate places:

### A1. Template selection in the one-match path (PPP:674-787)

`processSimplePathWithOneMatchingElementInDifferential` builds the *template* the diff is merged onto:

| Case | Lines | Template | Template SD |
|---|---|---|---|
| reslice (`lid` contains `/`) | 684-691 | the already-generated parent slice from the **result** (ids regenerated first) | derived (source SD) |
| diff has exactly 1 type, with a profile, code ≠ Reference, and the profile differs from the base's first profile | 692-773 | see below | the type profile SD |
| otherwise | 775-783 | `currentBase.copy()` (or the slicer copy when `APPLY_PROPERTIES_FROM_SLICER`, which is `false`) | base source |

Inside the type-profile case:
- resolve via `context.fetchResource(...)` with the canonical's version-resolution rules (698-699) — NOT via
  `findProfile`, so the `default/force/check-profile-version` parameters (PU:4109-4139) do **not** apply here;
- xver fallback (700-712): only if `profileUtilities.getXver() != null` — `getXver()` (PU:4810) returns the raw
  field, unlike the lazy `makeXVer()` (PU:1182) used at PU:1045/2078/2628. Only the test driver calls
  `setXver` (SnapShotGenerationTests:606); library callers that don't get the xver branch **skipped** here until
  some later `makeXVer()` call has initialised the field → order-dependent (first xver-typed extension in a
  ProfileUtilities instance falls to the base-copy template; `updateFromDefinition` PU:2626-2638 then still
  resolves the xver SD for the doc override). JI-19 candidate (needs-verification: does ValidationEngine call
  `setXver`?). BadVersion/Invalid/Unknown xver status → `FHIRException` (702-707); Valid → synthesized SD +
  `generateSnapshot(Extension, sd, ...)` re-entrantly (709-710);
- type compatibility (714-718): `isMatchingType(sd, diffTypes, profileElementId)` (PU:1643-1668) walks the
  profile's base chain; a match is (a) core url prefix `http://hl7.org/fhir/StructureDefinition` **and**
  `sd.type == type.code`, or (b) `sd.url == type.code` (logical model absolute type codes), or (c) with a
  profile-element id: the named element's own types intersect the diff types (PU:1650-1662). Skipped while the
  target SD `isGeneratingSnapshot()` (714). Failure → **throw** `Validation_VAL_Profile_WrongType2`
  ("Type mismatch processing profile {0} at path {1}: The element type is {4}, but the profile {3} is for a
  different type {2}");
- snapshot-less profile → on-demand `generateSnapshot` of the profile (725-731; base resolved with raw
  `fetchResource`, `checkNotGenerating(sdb, "an extension base")`) — the re-entrant channel (ch11);
  in-progress profile (`isGeneratingSnapshot`) → only its already-populated **first element** may be used
  (719-724; empty → throw `Attempt_to_use_a_snapshot_on_profile__as__before_it_is_generated`);
- the template source element `src` (732-762):
  - **profile-element extension present** (734-749): the element with that **id** in the profile snapshot;
    not found → `log.info("At this time the reference to … cannot be handled - consult Grahame Grieve")` when
    the profile is still generating, `throw Unable_to_find_element__in_` when the base type is
    Extension/Resource, else silently `src = null`;
  - else the profile's **root** element copy (754); if the profile is a **RESOURCE** kind its root
    `constraint`s are **cleared** (756-760, "the sense of %resource changes when the root resource is treated
    as an element. The validator will enforce the constraint");
- **the template is used only when `currentBase.typeSummary()` is `Extension` or `Resource`** (763-772):
  `template = merge(src, slicerElement).setPath(currentBase.getPath())`, sliceName cleared; for non-Extension
  (i.e. Resource) the template's min/max are **reset to the base's** ("temporary work around", 768-771).
  For every other type (Address, Quantity, Identifier … profiles) `template` stays `null` and the base row is
  used → **datatype profile roots are never merged in Java**.
- `merge(src, slicer)` (940-953) = `src.copy()` (slicer path dead: `APPLY_PROPERTIES_FROM_SLICER=false`).
- `fillOutFromBase(template, currentBase)` (786; PU:1886-1945): **fill-if-absent** of sliceName, label, code
  (add missing), definition, short, comment, requirements, alias (add), min, max, fixed, pattern, example,
  minValue, maxValue, maxLength, mustSupport, isSummary, isModifier, isModifierReason, mustHaveValue, binding
  (copy), constraint (add by key), extension (add by url). So an extension definition's root wins over the
  base `extension` row for every property it states.

Then (789-826): `updateURLs` (relative `#` refs → absolute, markdown link rewriting), `fixedPathDest`,
`updateFromBase` (Base component from `currentBase`, ch10), sliceName handling incl. min→0 for open slicing
(801-810, ch6), `markExtensions`, `updateFromDefinition(outcome, diff)` (ch5), slicer-max cap (816-818),
`outcome.setSlicing(null)` (819).

**Extension cardinality outcome** (the OQ-001 diamond, Java side): with the template = extension definition
root, `min`/`max` come from that root unless the diff states them (`fillOutFromBase` fills only absent
props; `updateFromDefinition` then applies the diff). But PPP:801-805 sets `min = 0` for any named slice
without an explicit diff min under a non-closed slicing (hack exemption: paths ending `xtension.value[x]`);
so the extension root's `min=1` does **not** survive into an open extension slice — only its `max` does, then
capped to the slicer's max (816-818). Sliced-base new slices (PPP:1398-1420) use a separate mechanism: after
`updateFromDefinition`, if exactly one profile is declared, `min` is raised to the profile root's min when the
root is mandatory and the slice is not, and `max` is lowered to the root's max when the root does not repeat
and the slice does; **two or more profiles → `throw new Error("Not handled: multiple profiles at …")`**
(1418-1420, a `java.lang.Error`).

### A2. The profile-doc override block in `updateFromDefinition` (PU:2619-2688)

Runs for every merged element (any path):
1. `profile` = for a base row with a sliceName: its own single `type.profile[0]` via `findProfile` (2621-2623);
   else the diff's `type[0].profile[0]` via `findProfile(…, derivedSrc)` (2624-2626); xver fallback with the
   same 4-status switch, throwing on Bad/Invalid/Unknown and re-entrantly generating on Valid (2628-2639).
2. A resolved profile that is **neither type `Extension` nor kind RESOURCE/LOGICAL is discarded** (`profile =
   null; msg = false`, 2643-2648 — in-code: "we sometimes want the details from the profile to override the
   inherited attributes, and sometimes not"). Datatype profiles therefore never reach step 3, and their
   unresolvability is never reported here (`msg=false`).
3. For Extension / RESOURCE profiles (2650-2671): empty snapshot → throw `Snapshot_is_empty`; the profile
   **root**'s `definition` (with relative-url processing against the profile's `render_webroot`),
   `binding.description`, `short`, `comment`, `requirements` are copied onto the element, `alias` and
   `mapping` are **replaced** by the root's. (`checkExtensionDoco` PU:1948-1961 had already blanked
   definition/short/comment/requirements/alias/mapping for extension paths — this block re-fills them from the
   extension definition; LOGICAL kinds pass step 2 but skip step 3.)
4. Unresolved profile (`else if` 2672-2688): only when the diff has one type with a profile **without** the
   profile-element extension and `msg` is still true: `log.warn("Can't find …")`, then
   `allowUnknownProfile` gate: Extension type → throw unless `ALL_TYPES`; other type → throw only if `NONE`.
   In-code todo at 2673: "should we change down the profile_element if there's one?" — profile-element
   references are exempt from the unknown-profile check.
   **JI-11 verified:** enum comment PU:223 says `NONE` is "the default"; field default PU:448 is `ALL_TYPES`;
   the test driver also defaults to `ALL_TYPES` (driver:143-150, manifest `allow=` attr used once: t15a
   `allow="none"`). Effective default = nothing throws for unknown profiles.

### A3. Walking into children (where the profile's *snapshot* is used)

All step-ins skip the type SD's root (`ProfilePathProcessorState(dt, dt.getSnapshot(), 1, …)`), remap paths via
`contextPathSource/Target`, and take the diff window `start..diffCursor-1`.

- **One-match path** (PPP:828-935), gate: diff limit not exhausted, element not root, and type is a datatype /
  non-Resource / has contentReference (828); the next diff row is a child of this element **and the base
  snapshot has no children here** (`!baseWalksInto`, 829). **If the base already carries children (e.g. a
  parent profile constrained `Patient.address.city`), Java never opens the type profile's snapshot** — the
  main loop simply continues over the base children (the diff children match them). Type-profile constraints
  on children are thus ignored whenever the base already expanded the element → "base beats type profile"
  for children; the profile url stays on `type.profile` for the validator.
  - renamed-choice collapse (830-842): base `value[x]`, diff `valueString` → type list narrowed to the suffix
    type (existing TypeRef kept "to keep any additional information", else a bare code) — ch6/OQ-018 material;
  - multi-type guard (843-853): the `nonExtension` loop iterates `diffMatches` (exactly one element here) → can
    never be true → **dead** (JI-17 site 1);
  - contentReference (858-896): `getElementById` resolves `url#id` or `#id` against the source SD or another SD
    (PU:3524); target in another SD → walk that SD's snapshot with a redirector; else same base (ch8);
  - datatype step-in (897-933): >1 distinct type codes → `getProfileForDataType("Element")` (908, i.e. only
    `id`/`extension` addressable — same fallback as .NET); all codes equal → first TypeRef; **>1 profiles on the
    TypeRef → core type** (914-915, = .NET's `SafeSingleOrDefault`); one profile → `getProfileForDataType(tr,
    webUrl, derived)` (917; PU:2073-2093: `findProfile(profile[0])` → xver (Valid only, with re-entrant
    `generateSnapshot`) → `fetchTypeDefinition(workingCode)`; failure only `log.debug`/`log.warn("XX: failed to
    find profle …")`). Null → throw `_has_children__for_type__in_profile__but_cant_find_type`.
    `profileName` is extended with the path tail (929) — error-message context only.
- **Empty-match path** (PPP:1061-1194): base row copied through with `updateFromBase`,
  `updateConstraintSources`, `checkExtensions` (strips `NON_INHERITED_ED_URLS`), `markExtensions`,
  **`updateFromObligationProfiles`** (1068 — the ONLY caller), `markDerived`. If `hasInnerDiffMatches`
  (1077): base has children → recurse over them (1080-1092); else no type & no contentReference → throw
  `_has_no_children__and_no_types_in_profile_` (1094-1096); multi-type `nonExtension` guard over an **empty**
  `diffMatches` → **dead** (1097-1116, JI-17 site 2) → `fetchTypeDefinition("Element")` for multi-type
  (1158); contentReference (1118-1156); else `getProfileForDataType(type[0])` (1158) → step-in (1163-1189,
  redirector variant when inside a contentReference).
- **Slicer inline step-in** (PPP:385-400, in `processSimplePathDefault`): when the slicing entry itself has
  child rows and the base has none: `getTypeForElement` (PU:1612-1631) — 0 types → throw
  (`Unable_to_resolve_content_reference_in_this_context` if contentReference, else
  `_has_no_children__and_no_types_in_profile_`); >1 types with any non-Reference → **throw**
  `_has_children__and_multiple_types__in_profile_` (**live** JI-17 site, PU:1621-1625); resolves via
  `getProfileForDataType(type[0], …)`; null → throw `Unknown_type__at_`.
- **Sliced-base, empty diff** (PPP:1657-1704): same `getTypeForElement` (1680) → live guard.
- **Sliced-base, new slice** (PPP:1421-1473): multi-type non-Reference → **throw** (1425-1428, live JI-17
  site, no `nonExtension` guard); `Base`/`Element`/`BackboneElement` typed → recurse over the base's own
  children (1431-1447); else `getProfileForDataType(type[0])` step-in (1449-1469).

**JI-17 complete site list**: dead guards PPP:846-851 (one-match) and PPP:1097-1116 (empty-match) — both
iterate a `diffMatches` list that has ≤1 element; live throws PPP:1425-1428 (sliced-base new slice),
PU:1621-1625 (`getTypeForElement`, reached from PPP:385 and PPP:1680). Consequence: the same author error
(walking into an unsliced multi-type element) throws in three code paths and silently expands against
`Element` in the two most common ones (unsliced base, one diff row or none) — child rows then orphan with
"No match found …" ERRORs (PU:921-927).

### A4. The final consistency sweep (PU:1038-1077)

After `setIds`, for every `type.profile` in the generated snapshot: `findProfile(u, derived)` → xver
(`makeXVer`, Valid only) → unknown: **WARNING** "The type of profile {u} cannot be checked as the profile is
not known" (1049-1051; this is ext-recursion-2's one message — its own snapshot is in progress and not yet
registered, ch11). Known: compare the profile's `type` (or, with profile-element, the named element's
`typeSummary()`; missing element → ERROR) against the element's working code (`Bundle.entry.response.outcome`
hard-wired to `OperationOutcome`, 1054-1056); mismatch → `isCompatibleType(wt, sd)` (PU:1464-1480: walks the
profile type's base chain by `type`/`url` equality; **NPE if `fetchTypeDefinition(wt)` is null** — reachable
for a working code that is not a loaded type, e.g. logical-model absolute type urls) unless profile-element
(then no compat walk) → ERROR "The profile … has type … which is not consistent with the stated type …"
(`handleError` = ERROR message, throw only with `wantThrowExceptions`). This sweep is the **only** place Java
checks profile/type consistency for elements whose profile did not drive a template or step-in.

## B. `Extension.url` fixing — Java has none

`grep setFixed(` over PU/PPP/PRE: PU:1913 (`fillOutFromBase` fill-if-absent), PU:2781/2783 (diff fixed →
snapshot, ch5), PU:4715 (xver **template** synthesis, `makeExtensionForVersionedURL`), PRE:437-447/1034
(additional-base merge / slice-stuff). There is **no generator-side backfill of `fixedUri` on
`Extension.url`** anywhere in the cone (no `".url"` path test except `getUrlFor` PU:3512-3520, used only by
the CSV/rendering code at PU:4184). Test evidence: `ext-recursion-2` input has no `fixedUri`, expected has
**zero** `fixedUri`; `ext-sort-issue` input has none, expected has one — inherited from its *base* extension
`http://hl7.org/fhir/StructureDefinition/extension-Communication.topic` (whose snapshot fixes url to the
**base's** url, which the derived profile keeps — .NET would keep it too, since `fixExtensionUrl` only fills
an absent `fixed[x]`). `complex-extension` (in=5, exp=10): the doubled count is the same 5 values appearing
in the expected file's differential+snapshot — all authored. → The `fixedUri` convention is produced by
authoring tools (IG publisher/FSH) and inherited, never synthesized by the Java generator. .NET's
`fixExtensionUrl` (`SnapshotGenerator.cs:1743-1785`) is .NET-only → new DEV-037; DEV-019's Java side is
"n/a — no backfill for any extension element".

## C. Extension-definition classification helpers (PU:4933-4964)

`isExtensionDefinition` = derivation CONSTRAINT ∧ type `Extension`; `isSimpleExtension` = extension def whose
snapshot `Extension.value` element exists and is not prohibited (`max=0`); `isComplexExtension` = value
absent or prohibited; `isModifierExtension` = snapshot (else differential) root `Extension.isModifier`. None
are used by the generation walk itself (rendering/validation helpers) — the spec's ext-1 simple/complex
dichotomy has no generator effect in Java either.

## D. Cross-version (xver) extensions

URL shape (XVerExtensionManager, `org.hl7.fhir.r5/utils/xver/`, fetched blob): `http://hl7.org/fhir/<ver>/
StructureDefinition/extension-<Type.path>`; `matchingUrl` = length ≥ 56, prefix `http://hl7.org/fhir/` (20
chars), version at [20,23), `/StructureDefinition/extension-` at [23,54), element path from 54. Status ∈
{BadVersion, Unknown, Invalid, NotAllowed, Valid}; `getDefinition` synthesizes an extension SD (base
`Extension`, PU-side snapshot generated on the fly at PPP:710 / PU:2080 / PU:2638). Factory picks Old vs New
loader (`XVerExtensionManagerFactory.newLoader`, static, default false). `PU.makeExtensionForVersionedURL`
(4680-4721) is an older static variant: copies the SMART `capabilities` extension SD as a **template**, sets
url/id/name/title/description/context from the source element, then mutates snapshot indices **3** (`url`
→ `fixedUri` = the xver url) and **4** (replaced by the source element copy, path
`Extension.value<Type>`) — hard-coded positions, no differential (differential cleared 4714). Not called from
the generator cone (candidate for the J-e "dead/legacy surface" list; verify callers outside the cone before
claiming dead). Shared tests: `pat-xver-extension` (`extension-Patient.animal`), `es-xver` (5
ExampleScenario xver urls), `sd-nested-ext`. .NET has no xver concept (resolves the url like any canonical;
the harness pre-loads the xver 0.1.0 package for parity — harness README).

## E. Obligation profiles

- `findInheritedObligationProfiles` (PU:1205-1215): for each `inherit-obligations` extension (new url
  `http://hl7.org/fhir/StructureDefinition/inherit-obligations`, old `…/tools/…`) resolve via `findProfile`;
  accept only if the target carries `obligation-profile = true` **and has the same `baseDefinition` (version
  stripped) as the derived SD** (1210) — silently ignored otherwise. Accumulates into the instance-level
  `obligationProfiles` list (PU:451) — **never cleared** between `generateSnapshot` calls on the same
  `ProfileUtilities` (instance-state leak candidate; the test driver creates a fresh PU per test, driver:~560-606).
- `updateFromObligationProfiles(base)` (PU:2529-2582), called **only** from the empty-diff copy-through path
  (PPP:1068): for obligation-profile elements with the same **id**: copy `obligation` extensions (core + tools
  urls); if any has `mustSupport`, merge the MS element's extensions and set `mustSupport=true` if any is
  true; if any has a binding, fold `additional` bindings (extension form and R5 form) whose purpose is not
  maximum/required/extensible, add-if-absent (2575 correct `!hasAdditional`).
- `updateFromDefinition` (PU:2593-2599, 2608-2614): for elements the **diff touches**, only the obligation
  **extensions** are re-added (as core-url extensions), plus the later additional-binding fold at PU:2947-2952
  with the **inverted guard** (JI-13, filed #2593). **No mustSupport fold on diff-touched elements** →
  asymmetry: a diff row that merely edits `short` on `Patient.birthDate` loses the obligation profile's
  `mustSupport=true`. `profile-patient-op3` cannot exhibit it (diff touches `active`/`maritalStatus`; MS
  lives on `birthDate`/`deceased[x]` in op1). JI-18 needs-verification/design question.
- `hasObligations(sd)` (PU:5025-5040): SD-level or any element/type `obligation` extension (rendering use).

## F. ch03 — base resolution in `generateSnapshot` (PU:740-1097)

Preamble (740-776), all `DefinitionException` unless noted:
1. null base/derived → `no_base_profile_provided` / `NO_DERIVED_STRUCTURE_PROVIDED`; `checkNotGenerating`
   both (`FHIRException` "Attempt to use a snapshot on profile … as … before it is generated", PU:1694-1698).
2. base without `type` → `Base_profile__has_no_type`; derived without `type` → `Derived_profile__has_no_type`
   (no logical-model carve-out: **Java requires `type` on every SD**, vs .NET's parse-from-diff repair for
   logical models, ch9); no `derivation` → `Derived_profile__has_no_derivation_value…`.
3. **constraint with `base.type ≠ derived.type` → throw** `Base & Derived profiles have different types
   ({0} = {1} vs {2} = {3})` (759-760) — .NET never checks (t29b, DEV-028 (i)). Specializations exempt.
4. base without snapshot → resolve **its** base via `findProfile(base.baseDefinitionElement, base)`, null →
   throw `Unable_to_find_base__for_`, `checkNotGenerating(sdb, "an extension base")`, recursive
   `generateSnapshot(sdb, base, base.url, webUrl-of-sdb, base.name)` (762-768) — unconditional on-demand
   generation (no `GenerateSnapshotForExternalProfiles`-style gate; .NET ch3).
5. `fixTypeOfResourceId(base)` (769; 1290-1307): for RESOURCE kinds at R4+, every element whose
   `base.path == "Resource.id"` gets type code `http://hl7.org/fhirpath/System.String` + `fhir-type` extension
   `id` — in **both** the base's snapshot and differential (mutates the *base* SD in the context).
6. `EXT_TYPE_PARAMETER` (`http://hl7.org/fhir/tools/StructureDefinition/type-parameter`) on the base →
   `checkTypeParameters` (770-771; 1159-1180): derived must declare one; both `type` sub-values must resolve;
   the derived's parameter type must descend from the base's (walk via `findProfile`); else throw
   `SD_TYPE_PARAMETER_MISSING/UNKNOWN/INVALID`. Java-only surface (R6 generic-type tooling).
7. `snapshotStack.contains(derived.url)` → throw `Circular snapshot references detected; cannot generate
   snapshot (stack = …)` (774-776); `setGeneratingSnapshot(true)`, push url, `Base.setCopyUserData(true)`
   (777-780). Pop/reset in `finally` (1086-1089); on any exception the half-built snapshot is **nulled**
   (1078-1084). → ch11 (J-e).

Body (781-1097):
- `webUrl` normalised with trailing `/`; `defWebRoot` defaults to it (783-787); fresh empty snapshot (788).
- `checkDifferential` + `checkDifferentialBaseType` (791-792, ch2); `copyInheritedExtensions(base, derived,
  webUrl)` (808; 1228-1253): every SD-level extension of the base not in `NON_INHERITED_ED_URLS` is copied to
  the derived SD per its `snapshot-behavior` policy (`http://hl7.org/fhir/tools/StructureDefinition/
  snapshot-behavior` on the extension's own SD, read via `fetchResourceRaw`, default `"defer"`): `ignore` →
  skip; `add` → always add; `overwrite` → replace existing value; default → add-if-absent; markdown values get
  relative-url processing. (SD-level counterpart of the element-level extension policy machine, ch5 (h).)
- `findInheritedObligationProfiles` (810, §E); clear `SNAPSHOT_GENERATED_IN_SNAPSHOT` on the caller's diff
  rows (820-821); `cloneDiff` + preprocessor (824-825, ch2).
- **Specialization rebase** (828-832, `cloneSnapshot` 1493-1508) — keyed on `derivation == SPECIALIZATION`,
  NOT on kind, so **logical models (all specializations in the shared suite: xt-logical, logical1, logical-goo,
  cdshooks-*) are rebased too**: every base snapshot element is copied with `id` **and** `path` rewritten by
  `replaceFirst(base.getTypeName(), derived.getTypeName())` — a plain first-occurrence string replace of the
  type *name* (safe: the name is always the first segment). `StructureDefinition.getTypeName()`
  (model/StructureDefinition.java:5347-5350, blob fetched) returns the url tail for LOGICAL kinds whose `type`
  contains `/`, so absolute-url logical types rebase by last segment, like .NET. Constraint SDs: no rebase (the
  walk's `fixedPathDest` remaps only during step-ins). `checkDifferential` (791) receives the same
  `derived.getTypeName()`. Contrast .NET: `Rebase` rewrites paths only; ids are regenerated (ch3/ch10).
  (Correction 2026-09-01 after advisor review: an earlier draft said "logical models: no rebase" — wrong.)
- `MappingAssistant` (837, ch5); `ProfilePathProcessor.processPaths` (839; PPP:155-183: base cursor 0, diff
  cursor 0, base limit = last base row, diff limit = last diff row or **-1 for an empty differential**, url,
  webUrl, `profileName = derived.present()`, no redirector, slicing = none).
- `checkGroupConstraints` (841); **specialization append** (842-867): diff rows not matched by the walk are
  merged onto a same-path snapshot element if one exists in the current context, else `updateURLs`-copied and
  inserted after the last child of their parent; a row that "walks into" children with >1 type → throw
  "Unsupported scenario: specialization walks into multiple types"; single type →
  `addInheritedElementsForSpecialization` (1263-1283): the type's snapshot children are copied under the new
  element (path `replace(typeName, path)`), the type root's constraints appended and its non-inherited
  extensions added-if-absent. (ch9 logical-model material; .NET = anonymous inline elements, ch9.)
- prohibited type-slice pruning (869-881): a multi-type element loses every `type` entry whose type slice
  (`findTypeSlice`, same path modulo `[x]`, single matching type) is `prohibited()` (`max=0`).
- **root type check** (882-883): non-logical derived with a `type` on the first snapshot element → `throw new
  Error` "Type on first snapshot element for {0} in {1} from {2}" (a `java.lang.Error`; the differential-side
  twin `type_on_first_differential_element` is at PU:1322).
- `mappingDetails.update()` (884, ch5); `setIds(derived, false)` (886, ch10).
- **orphan check** (908-948): every diff row (clone) without `SNAPSHOT_GENERATED_IN_SNAPSHOT` → ERROR "No match
  found for {id} in the generated snapshot: check that the path and definitions are legal in the
  differential (including order)" per row + one summary ERROR (ch4/DEV-035); derivation user data copied back
  to the caller's rows (913-920, OQ-015).
- R4 hacks (949-968): `mapping.map` trimmed; **relative `constraint.source` made absolute** against
  `http://hl7.org/fhir/StructureDefinition/` (`Type.path` → `…/Type#Type.path`, bare `Type` → `…/Type`)
  (ch5/DEV-002 data point: Java also normalises pre-existing relative sources).
- **specialization Base fill** (969-975): any element without `base` gets `base = {path, min, max}` of itself
  (sdf-8b obligation; ch10).
- slice-count sweep (976-1036, ch6/JI-6), path-prefix check (1020-1022: element path must start with the
  derived type name (url tail) → `throw new Error`), slice-without-slicing ERROR (1023-1027), duplicate slice
  name ERROR (1028-1033).
- final profile/type sweep (1038-1077, §A4).
- epilogue (1091-1096): `snapshot-base-version` extension (`http://hl7.org/fhir/tools/StructureDefinition/
  snapshot-base-version` = base.version) added to the snapshot component; `setGeneratedSnapshot(true)`;
  messages attached as user data.

### `findProfile` (PU:4095-4145) — the central resolver
Strips `#fragment` **silently** (4100-4102 — so a legacy `url#element` type profile resolves to the whole SD
and the fragment is lost; no Java code reads it); splits `url|version`; applies expansion `Parameters`:
`default-profile-version` (only when no explicit version), `force-profile-version` (always), `check-profile-
version` (throw `FHIRException` "Profile resolves to … which does not match required profile version …" on
mismatch) (4109-4139); drops the source-package pin when the source is a **core** package ("switch the
extension pack in", 4141-4143); `context.fetchResource(SD, url, versionRules(ref), version, source)`.
`ExtensionUtilities.getVersionResolutionRules(ref)` reads per-reference version-resolution hints. Used for
bases (763, 1175, 1328), obligations (1208), type-profile lookups in PU (1043, 2076, 2622, 2626, 3280) — but
**not** by PPP's template selection (raw `fetchResource`, 698/726) nor by the sliced-base min/max pickup
(PPP:1406, `defaultRule()`), nor `getExtensionAction` (`fetchResourceRaw`, 1256). Version handling is thus
inconsistent within one generation. .NET: `ProfileReference` parses `url[#element]`; `|version` passes
through to the resolver unparsed (no version parameters concept).

### Other ch03 helpers
- `makeBaseDefinition(fhirVersion)` (4777-4808): synthesizes the abstract `Base` SD (kind COMPLEXTYPE,
  abstract, one root element `Base 0..*` in snapshot and differential) — Java's answer to the missing `Base`
  artifact pre-R5 (context-side synthesis) vs .NET's #3576 issue-and-proceed carve-out.
- `checkNotGenerating` (1694-1698): `FHIRException` on `isGeneratingSnapshot()`.
- `getWebUrl(dt, webUrl)` (1761-1774): folder of the type SD's `webPath` else the inherited webUrl — drives
  markdown relative-link rewriting during step-ins (rendering concern, not conformance).
- `updateURLs` (2135-2176): `#local` `binding.valueSet`, `type.profile`, `type.targetProfile` → prefixed with
  the profile url (NB 2143/2147 concatenate `url + t.getProfile()` — the **list**'s `toString()`, not
  `u.getValue()` — a latent bug when a `#`-relative profile is used; harmless today because `#` profile
  references are essentially unused); markdown fields + markdown extension values through
  `processRelativeUrls`. Applied to every emitted row.
- **Interface bases:** no `structuredefinition-interface` handling anywhere in PU/PPP/PRE (grep: only
  `getAbstract()`/LOGICAL in `checkTypeDerivation` PU:3276). Java's generator does not skip interface SDs in
  the base chain (.NET `resolveBaseDefinition` does, ch3/ch9) — whether the R5 context loader hides them is
  outside the cone.
- `getElementInCurrentContext` (1189-1199) / `findLastChildForParent` (1099-1109, "internal code error"
  throw) — specialization append helpers.

## G. Fixed-position and suspicious spots (not all bugs)

| Where | What | Disposition |
|---|---|---|
| PPP:846-851, 1097-1116 | dead `nonExtension` guards (diffMatches ≤ 1 element) | **JI-17 — file** (all 4 sites known) |
| PU:223 vs 448 | `AllowUnknownProfile` comment "NONE (the default)" vs field `ALL_TYPES` | **JI-11 — file (minor)** |
| PPP:700 `getXver() != null` vs lazy `makeXVer()` elsewhere | template-selection xver branch skipped for callers that never `setXver` until a later lazy init | JI-19 needs-verification (check ValidationEngine/IG publisher call `setXver`) |
| PU:2529 only called at PPP:1068 | obligation MS/binding fold skipped for diff-touched elements | JI-18 needs-verification / design question |
| PU:1465 | `fetchTypeDefinition(base)` unchecked → NPE for unresolvable working codes in the final sweep | note; reaching input = logical-model absolute type url on an element carrying a profile |
| PU:2143/2147 | `url + t.getProfile()` list toString | latent; `#`-relative profiles unused |
| PU:451 `obligationProfiles` | instance list never cleared across `generateSnapshot` calls | note (driver uses a fresh PU per test) |
| PPP:1418-1420 | `throw new Error("Not handled: multiple profiles …")` for a new slice in a sliced base with 2+ profiles | OQ-014 row (java.lang.Error for an author choice the spec allows — disjunction) |
| PPP:745 | `log.info("… consult Grahame Grieve")` for profile-element on an in-progress SD | OQ-014 row |
| PU:4715-4717 | hard-coded snapshot indices in `makeExtensionForVersionedURL` | legacy; check callers in J-e |
| PPP:768-771 | "temporary work around": Resource-typed template min/max reset to base | note |
| PPP:803 | `xtension.value[x]` hack exemption from min→0 | note ("problems with snapshots in official releases") |

## H. Test-suite coverage of this chapter's mechanisms (fhir-test-cases r5/snapshot-generation)

- profile-element extension: **no test file mentions it** (grep empty) — Java's support is code-only.
- xver: pat-xver-extension, es-xver, sd-nested-ext(expected only).
- obligations: profile-patient-op-base, profile-patient-op3 (+op1/op2/op2a registered), profile-mapping-1..4.
- additionalBase: multi-profile (+multi-dt-both).
- type-parameter: none.
- imposeProfile: mi-use-imposed (generator reads `structuredefinition-imposeProfile` only inside
  `sdConformsToTargets` PU:3333 — a *targetProfile* derivation check may pass through an imposed profile;
  PRE:607 comment "we ignore impose and compliesWith - for now?"). No snapshot-shaping effect.
