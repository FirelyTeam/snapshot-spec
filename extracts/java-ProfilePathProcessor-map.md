# ProfilePathProcessor.java + state objects — structure map (orientation pass)
Source: org.hl7.fhir.core @ commit b06c7ee (sparse clone 2026-08-21)
Files: org.hl7.fhir.r5/.../conformance/profile/ProfilePathProcessor.java (+ 7 small helpers)
Generated: 2026-08-26 (Phase 3 packet 1)

File length: 1739 lines. Class annotations: `@AllArgsConstructor(access=PRIVATE)`, `@MarkedToMoveToAdjunctPackage`, `@Slf4j` (L37-40).

---

## 1. Method inventory (ProfilePathProcessor)

| Method (brief signature) | Lines | Purpose |
|---|---|---|
| `private ProfilePathProcessor(ProfileUtilities)` | 125-144 | Root constructor; nulls/zeros every field except `profileUtilities`. |
| `static ProfilePathProcessor getInstance(ProfileUtilities)` | 146-148 | Factory for the root processor instance. |
| `ProfilePathProcessor incrementDebugIndent()` | 150-152 | Returns copy with debug indent +2 spaces (used at every recursion site). |
| `static void processPaths(ProfileUtilities, base, derived, url, webUrl, differential, baseSnapshot, mapHelper)` | 155-183 | **Public entry point** (called from `ProfileUtilities.generateSnapshot`); builds initial state (cursors 0/0, limits = last index) and the fully-configured processor via withers, then starts recursion. |
| `private ElementDefinition processPaths(ProfilePathProcessorState cursors, MappingAssistant, ElementDefinition slicerElement)` | 191-235 | **Core recursive loop**: walks baseCursor to baseLimit; per base element computes `diffMatches` and dispatches to `processSimplePath` or `processPathWithSlicedBase`; contains GDG diffCursor-advance patch (L216-229); returns first produced element. |
| `private void checkAllElementsOK()` | 237-244 | Post-loop sanity check: throws on any result element with a null min value. |
| `private void checkDiffAssignedAndCursor(cursors)` | 246-263 | Empty — entire body is commented-out assertion scaffolding (diff elements before cursor must have UD_DERIVATION_POINTER). |
| `private void debugProcessPathsIteration(cursors, currentBasePath)` | 265-274 | Debug logging per loop iteration. |
| `private void debugProcessPathsEntry(cursors)` | 276-280 | Debug logging on entry to `processPaths`. |
| `public ElementDefinition processSimplePath(currentBase, currentBasePath, diffMatches, typeList, cursors, mapHelper, slicerElement)` | 283-305 | Dispatcher for non-sliced base element: empty diff / one match / diffs-constrain-types / default (diff slices the element). |
| `private void processSimplePathDefault(currentBase, currentBasePath, diffMatches, cursors, mapHelper)` | 307-448 | Differential introduces slicing on an unsliced base: validates preconditions, writes the slicer row (or recurses for a pre-slice default), then recurses once per slice in `diffMatches`. |
| `private ElementDefinition setPath(ed, path)` | 450-453 | Clears element id (despite name, does not set path). |
| `private String fixForRedirect(path, rootPath, redirect)` | 455-457 | String-replace of contentReference target path by root path. |
| `private int resolveContentReference(base, currentBase)` | 459-473 | Finds index of contentReference target element in base snapshot, walking backwards, skipping slices. |
| `private String diffPath(ed)` | 475-477 | Renders `StructureDefinition.differential.element[n]` locator string for messages. |
| `private String slicingSummary(ElementDefinitionSlicingComponent)` | 479-481 | `toString()` of a slicing component (message text). |
| `private boolean slicingMatches(s1, s2)` | 483-491 | Deep-compare of two slicing components (ordered, rules, discriminator). |
| `private void processSimplePathWhereDiffsConstrainTypes(currentBasePath, diffMatches, typeList, cursors, mapHelper)` | 493-672 | Type slicing on an unsliced base: shortcut synthetic-slicer insertion (R3 vs R4+ branch), slicing-shape validation, slice-name check/auto-fix, recursion per type slice, fixedType narrowing, open/closed adjustment. |
| `private ElementDefinition processSimplePathWithOneMatchingElementInDifferential(currentBase, currentBasePath, diffMatches, cursors, mapHelper, slicerElement)` | 674-938 | The single-match workhorse: template selection (reslice base / type-profile root / currentBase), merge with diff, slice-name & min handling, then descent into children (contentReference target or datatype snapshot). |
| `private ElementDefinition merge(src, slicer)` | 940-953 | Copies src; only merges min/max from slicer when `APPLY_PROPERTIES_FROM_SLICER` (i.e. currently a plain copy). |
| `private void checkToSeeIfSlicingExists(ed, template)` | 955-987 | When a named slice appears without a slicer in the result, injects a synthetic slicer row (extension url-discriminator or type-slicing $this). |
| `private boolean isJumpingIntoTypeSlicing(ed, template)` | 989-991 | Detects a named+typed slice landing on an un-sliced `[x]` template. |
| `private boolean pathsMatch(path1, path2)` | 993-1022 | Segment-wise path equality with `[x]`-prefix tolerance in either direction. |
| `private int indexOfFirstNonChild(base, currentBase, i, baseLimit)` | 1024-1041 | Index just past currentBase's children within limit (comments note "should be baseLimit?" doubts). |
| `private boolean baseHasChildren(base, ed)` | 1043-1049 | Whether the next base element is a child of `ed`. |
| `private boolean isChildOf(sub, focus)` | 1052-1058 | Child-path test with `[x]` handling. |
| `private void processSimplePathWithEmptyDiffMatches(currentBase, currentBasePath, diffMatches, cursors, mapHelper)` | 1061-1194 | No diff statement on this element: copy base row through, then if inner diff matches exist, recurse into base children, contentReference target, or datatype snapshot. |
| `private void processPathWithSlicedBase(currentBase, currentBasePath, diffMatches, typeList, cursors, mapHelper)` | 1196-1223 | Dispatcher when the **base** is already sliced: empty diff / diffs-constrain-types / default (rules-of-reslicing comment L1202-1207). |
| `private void processPathWithSlicedBaseDefault(currentBase, currentBasePath, diffMatches, cursors, path, mapHelper)` | 1225-1482 | Sliced base + diff slices: validates slicing agreement, writes slicer, copies BackboneElement children, walks base slices matching diff slices by sliceName, then appends new slices (closed check, type-profile min/max pickup). |
| `private boolean addToResult(outcome)` | 1484-1486 | Appends element to the result snapshot (single funnel for output). |
| `private void debugCheck(outcome)` | 1488-1492 | Leftover targeted debug: logs when path starts `List.` and url is a hardcoded Nictiz Bundle-MedicationOverview profile. |
| `private void processPathWithSlicedBaseWhereDiffsConstrainTypes(currentBasePath, diffMatches, typeList, cursors, mapHelper)` | 1494-1655 | Type slicing where base is already type-sliced: same shortcut insertion + checks as the simple variant, matches diff type slices to `BaseTypeSlice` ranges, runs unhandled base slices against a fake empty differential. |
| `private void processPathWithSlicedBaseAndEmptyDiffMatches(currentBase, currentBasePath, diffMatches, cursors, path, mapHelper)` | 1657-1721 | Sliced base, no direct diff match: either walk in (inner diff matches; children or datatype recursion) or bulk-copy base element + children + sibling slices. |
| `private boolean oneMatchingElementInDifferential(slicingDone, path, diffMatches)` | 1723-1736 | Predicate for the single-match dispatch arm (excludes implicit slicing, slicing setups, named extension slices — unless slicing already done). |

Constant: `APPLY_PROPERTIES_FROM_SLICER = false` (L58; explanatory comment L42-57).

---

## 2. Builder/state pattern

Three channels of state flow through the recursion:

### (a) Immutable processor fields (lombok `@Getter @With`, L61-123)
Each recursion site builds a *copy* of the processor with selected fields replaced (`this.incrementDebugIndent().withBaseLimit(...)...processPaths(...)`). Changes never propagate back to the caller's processor.

| Field | Type | Role (what changes per recursion level) |
|---|---|---|
| `profileUtilities` | ProfileUtilities | Fixed service handle (no `@With`). |
| `debugIndent` | String | +2 spaces per level (`incrementDebugIndent`, L150-152). |
| `result` | ...SnapshotComponent | Output sink; never replaced. |
| `differential` | ...DifferentialComponent | The diff being applied; replaced only for unhandled base slices (fake empty diff replay, L1638-1643). |
| `baseLimit` / `diffLimit` | int | Scope window into base/diff element lists; re-narrowed at nearly every recursion site. |
| `url` | String | Profile canonical (constant). |
| `webUrl` | String | Base web URL; replaced when descending into a datatype snapshot (`profileUtilities.getWebUrl(dt, ...)`). |
| `profileName` | String | Grows with `pathTail(...)` per slice/child descent (message context). |
| `contextPathSource` / `contextPathTarget` | String | Path remapping pair when descending into a datatype or contentReference target (source path in the type SD vs target path in the profile). |
| `trimDifferential` | boolean | Set true (`closed`) when processing diff slices against a closed base slicing (L1335). |
| `redirector` | List\<ElementRedirection\> | Stack of path redirections for contentReference descents (`redirectorStack`); reset to empty list for datatype descents (L930), nulled at L1293. |
| `sourceStructureDefinition` | StructureDefinition | The SD whose snapshot is the current base; replaced when a contentReference target lives in a different SD. |
| `derived` | StructureDefinition | The profile being generated (constant). |
| `slicing` | PathSlicingParams | Slicing context for the callee: fresh `new PathSlicingParams()` (not in a slice), or `(true, slicerElement/null, path/null)` when processing a slice. |

### (b) Mutable cursor state — `ProfilePathProcessorState` (see §5)
Passed as a parameter, fields mutated freely (`cursors.baseCursor++`, `cursors.diffCursor = ...`, `cursors.contextName = dt.getUrl()`, `cursors.resultPathBase = outcome.getPath()`, even `cursors.base = tgt.getSource().getSnapshot()` at L1124). Recursion sites almost always construct a **new** state object (`ncursors`) so callee cursor movement does not leak back; the parent then advances its own cursors from `newBaseLimit`/`newDiffLimit` (e.g. L446-447, L670-671, L1652-1653). One deliberate exception: `processSimplePathWithEmptyDiffMatches` L1086-1092 reads `ncursors.diffCursor` back into `cursors.diffCursor` after the child recursion.

### (c) `slicerElement` parameter
Third argument of `processPaths`/`processSimplePath...`: the slicer row governing the slices currently being generated (used for max-capping at L816-818 and the disabled `APPLY_PROPERTIES_FROM_SLICER` template logic). Passed as non-null only for the first iteration of a slice-scope recursion (`first ? slicerElement : null`, L209).

### PathSlicingParams / TypeSlice / BaseTypeSlice
- **PathSlicingParams** (37 lines): `done` (boolean — slicing already handled for this scope), `elementDefinition` (the slicer ED), `path` (base path for type slicing over a sliced base), plus mutable `slices` list filled by `withDiffs(diffMatches)` (adds diffMatches[1..n]; note this is a mutate-and-return method, *not* a lombok wither). Default ctor = not-in-slice. Consumed at L208 (`currentBasePath.equals(getSlicing().getPath())`), L296 (`isDone()`), and the slice min=0 logic at L801-810.
- **TypeSlice** (25 lines): pair of `defn` (diff ED) + `type` (constrained type code, null for the slicing root). Produced by `profileUtilities.diffsConstrainTypes(...)` into the `typeList` accumulated in `processPaths` L194 and consumed by both `...ConstrainTypes` methods.
- **BaseTypeSlice** (45 lines): a pre-existing type slice in the **base** snapshot: `defn`, `type`, `start`/`end` (base element index range), `handled` flag. Produced by `profileUtilities.findBaseSlices(...)` (L1592), matched via `chooseMatchingBaseSlice` (L1608), unhandled ones replayed with a fake diff (L1635-1650).

---

## 3. Call graph

`PU.` marks handoffs to ProfileUtilities (mapped separately). All `processPaths` arrows are recursive calls on a re-configured processor copy.

```
ProfileUtilities.generateSnapshot
└─ static processPaths (155)                        [entry; builds root processor + state]
   └─ processPaths (191)                            [loop over base scope]
      ├─ debugProcessPathsEntry (276) / debugProcessPathsIteration (265)
      ├─ PU.fixedPathSource, PU.getDiffMatches, PU.diffsConstrainTypes
      ├─ processSimplePath (283)                    [base not sliced]
      │  ├─ oneMatchingElementInDifferential (1723) → PU.isImplicitSlicing, PU.isExtension
      │  ├─ processSimplePathWithEmptyDiffMatches (1061)
      │  │  ├─ PU.updateURLs, PU.fixedPathDest, PU.updateFromBase, PU.updateConstraintSources,
      │  │  │  PU.checkExtensions, PU.markExtensions, PU.updateFromObligationProfiles, PU.markDerived
      │  │  ├─ PU.hasInnerDiffMatches, baseHasChildren (1043)
      │  │  ├─ → processPaths                        [base children walk-in, L1087-1090]
      │  │  ├─ PU.getElementById / PU.replaceFromContentReference
      │  │  ├─ → processPaths                        [contentReference target, other SD, L1131-1140]
      │  │  ├─ → processPaths                        [contentReference target, same SD, L1148-1155]
      │  │  ├─ PU.getProfileForDataType / fetchTypeDefinition("Element")
      │  │  ├─ → processPaths                        [datatype snapshot, no redirector, L1167-1175]
      │  │  └─ → processPaths                        [datatype snapshot, redirector stack, L1180-1188]
      │  ├─ processSimplePathWithOneMatchingElementInDifferential (674)
      │  │  ├─ PU.isValidType, PU.generateIds, PU.getById, PU.getXver (xver extension resolution)
      │  │  ├─ PU.generateSnapshot                   [RE-ENTRANT: xver extension def, L710]
      │  │  ├─ PU.checkNotGenerating, PU.generateSnapshot [RE-ENTRANT: type-profile base, L730]
      │  │  ├─ PU.isMatchingType, PU.fillOutFromBase, merge (940)
      │  │  ├─ PU.updateURLs, PU.fixedPathDest, PU.updateFromBase
      │  │  ├─ checkToSeeIfSlicingExists (955) → isJumpingIntoTypeSlicing (989), pathsMatch (993)
      │  │  ├─ PU.markExtensions, PU.updateFromDefinition   [per-property merge]
      │  │  ├─ PU.getElementById / PU.replaceFromContentReference / PU.redirectorStack
      │  │  ├─ → processPaths                        [contentReference target, other SD, L871-879]
      │  │  ├─ → processPaths                        [contentReference target, same SD, L887-895]
      │  │  └─ → processPaths                        [datatype snapshot, L924-933]
      │  ├─ processSimplePathWhereDiffsConstrainTypes (493)
      │  │  ├─ PU.determineTypeSlicePath, PU.rootName, PU.determineFixedType, PU.getListOfTypes
      │  │  ├─ → processPaths                        [type-slicing root, L584-591]
      │  │  └─ → processPaths                        [each type slice, L622-629]
      │  └─ processSimplePathDefault (307)
      │     ├─ PU.unbounded, PU.isSlicedToOneOnly, PU.isTypeSlicing, PU.isExtension
      │     ├─ → processPaths                        [pre-slice default content, L326-331]
      │     ├─ PU.updateURLs, PU.updateFromBase, PU.makeExtensionSlicing, slicingMatches (483)
      │     ├─ PU.updateFromDefinition, PU.hasInnerDiffMatches, PU.getTypeForElement
      │     ├─ → processPaths                        [inline datatype for slicer, L393-400]
      │     ├─ resolveContentReference (459), fixForRedirect (455)   [inline contentReference dump]
      │     ├─ PU.checkExtensionDoco
      │     └─ → processPaths                        [each named slice, L438-443]
      └─ processPathWithSlicedBase (1196)            [base already sliced]
         ├─ processPathWithSlicedBaseAndEmptyDiffMatches (1657)
         │  ├─ PU.hasInnerDiffMatches, PU.getTypeForElement
         │  ├─ → processPaths                        [base children walk-in, L1673-1677]
         │  └─ → processPaths                        [datatype snapshot, L1691-1700]
         ├─ processPathWithSlicedBaseWhereDiffsConstrainTypes (1494)
         │  ├─ PU.determineTypeSlicePath, PU.findBaseSlices, PU.chooseMatchingBaseSlice, PU.determineFixedType
         │  ├─ → processPaths                        [type-slicing root, L1574-1581]
         │  ├─ → processPaths                        [each diff type slice into matched base range, L1614-1621]
         │  └─ → processPaths                        [unhandled base slices vs fake empty diff, L1640-1647]
         └─ processPathWithSlicedBaseDefault (1225)
            ├─ PU.orderMatches, PU.discriminatorMatches, PU.ruleMatches, PU.summarizeSlicing
            ├─ PU.updateFromSlicing, PU.updateFromDefinition, PU.getSiblings
            ├─ → processPaths                        [inner diff walk-in: datatype, L1276-1284]
            ├─ → processPaths                        [inner diff walk-in: children, L1286-1294]
            ├─ → processPaths                        [matched diff slice per base slice, L1329-1337]
            ├─ PU.generateIds, PU.getById            [reslice template lookup]
            ├─ PU.baseWalksInto
            ├─ → processPaths                        [new slice children: Base/Element/BackboneElement, L1439-1447]
            └─ → processPaths                        [new slice children: datatype snapshot, L1460-1469]
```

Shared per-row plumbing everywhere: `PU.updateURLs`, `PU.fixedPathDest`, `PU.updateFromBase`, `PU.markExtensions`, `debugCheck` (1488), `addToResult` (1484).

Differential mutation sites (synthetic slicer inserted into the live differential, later removed): L514-516 / L634-636 (simple) and L1514-1529 / L1623-1626 (sliced base).

---

## 4. Method → chapter mapping

| Method | Chapters |
|---|---|
| static `processPaths` (155) | ch03 (initial scope/root), ch11 (recursion setup), ch12 (limits/config) |
| `processPaths` (191) | ch04 (matching loop, getDiffMatches dispatch), ch11 (recursion driver), ch12 (GDG cursor patch) |
| `checkAllElementsOK` (237) | ch12 |
| `checkDiffAssignedAndCursor` (246) | ch12 (dead assertion) |
| `debugProcessPaths*` (265, 276) | ch12 |
| `processSimplePath` (283) | ch04 (dispatch taxonomy) |
| `oneMatchingElementInDifferential` (1723) | ch04, ch06 (implicit-slicing exclusion) |
| `processSimplePathWithEmptyDiffMatches` (1061) | ch04 (copy-through), ch07 (datatype descent), ch08 (contentReference), ch11 |
| `processSimplePathWithOneMatchingElementInDifferential` (674) | ch05 (merge orchestration), ch06 (slice min/max, checkToSeeIfSlicingExists), ch07 (type-profile template + extension xver), ch08 (contentReference), ch10 (generateIds for reslices), ch11 (re-entrant generateSnapshot) |
| `merge` (940) | ch06 (disabled slicer-inheritance) |
| `checkToSeeIfSlicingExists` / `isJumpingIntoTypeSlicing` / `pathsMatch` (955/989/993) | ch06, ch04 (path matching w/ [x]) |
| `processSimplePathWhereDiffsConstrainTypes` (493) | ch06 (type slicing), ch12 (R3/R4+ + newSlicingProcessing config) |
| `processSimplePathDefault` (307) | ch06 (slicing introduction), ch02 (auto-added slicing), ch08 (inline contentReference dump), ch12 (errors) |
| `setPath` (450) | ch10 (id clearing) |
| `fixForRedirect` / `resolveContentReference` (455/459) | ch08 |
| `diffPath` (475) | ch12 (message locators) |
| `slicingSummary` / `slicingMatches` (479/483) | ch06 |
| `indexOfFirstNonChild` / `baseHasChildren` / `isChildOf` (1024/1043/1052) | ch04 |
| `processPathWithSlicedBase` (1196) | ch06 (sliced-base taxonomy) |
| `processPathWithSlicedBaseDefault` (1225) | ch06 (slice matching/extension, closed rules), ch05 (updateFromSlicing/Definition), ch07 (type-profile min/max pickup L1398-1420), ch10 (reslice ids) |
| `processPathWithSlicedBaseWhereDiffsConstrainTypes` (1494) | ch06, ch12 (version branch) |
| `processPathWithSlicedBaseAndEmptyDiffMatches` (1657) | ch06 (copy sliced content), ch07, ch11 |
| `addToResult` (1484) | ch10 (single output funnel) |
| `debugCheck` (1488) | ch12 (leftover debug) |

---

## 5. Small helper files

All in the same directory; all `@MarkedToMoveToAdjunctPackage`.

- **ProfilePathProcessorState.java** (21 lines) — the mutable cursor bundle threaded through recursion. Fields (all protected, `@AllArgsConstructor(PROTECTED)`): `baseSource` (SD owning the base snapshot), `base` (snapshot component being walked), `baseCursor` / `diffCursor` (int indexes), `contextName` (URL of current base SD/datatype, used in error messages), `resultPathBase` (first result path; prefix guard for every added element). Used only by ProfilePathProcessor. → ch11, ch12 (guards).
- **PathSlicingParams.java** (37 lines) — slicing context passed down one recursion level: `done` (slicing handled), `elementDefinition` (slicer ED), `path` (sliced path for type slicing over sliced base), mutable `slices` list via `withDiffs()` (diffMatches minus the first). → ch06.
- **TypeSlice.java** (25 lines) — (diff ED, type-code) pair; type null for the slicing root entry. Built by `ProfileUtilities.diffsConstrainTypes`, consumed by the two `...ConstrainTypes` methods. → ch06.
- **BaseTypeSlice.java** (45 lines) — a type slice already present in the base snapshot: `defn`, `type`, `start`, `end` (base index range), mutable `handled`. Built by `ProfileUtilities.findBaseSlices`; unhandled ones get the fake-diff replay (L1635-1650). → ch06.
- **MappingAssistant.java** (279 lines) — mapping-declaration reconciliation between base and derived SD. Fields: `mappingMergeMode` (enum DUPLICATE/IGNORE/OVERWRITE/APPEND, default APPEND), `base`, `derived`, `masterList`, `renames` map (identity collisions get numeric suffixes), `version`, `suppressedMappings`. Constructor (L45-96) builds master list + renames; `merge(base, derived)` (L173-188) merges per-element mappings during `updateFromDefinition` (R5+ merge modes in `compareMaps` L233-262, version branch `isR5Plus` L238); `update()` (L150-170) prunes unused SD-level mappings at the end. Created in ProfileUtilities.generateSnapshot, threaded as `mapHelper` through every processPaths call. → ch05 (mapping property merge), ch12 (merge-mode config).
- **BindingResolution.java** (18 lines) — dumb DTO: `display`, `url`, `uri`, `external`. Returned by `ProfileKnowledgeProvider.resolveBinding`; rendering-oriented, not used inside ProfilePathProcessor. → ch12 (peripheral).
- **ProfileKnowledgeProvider.java** (25 lines) — interface for host-environment knowledge: `isDatatype`, `isPrimitiveType`, `isResource`, `hasLinkFor`, `getLinkFor`, `resolveBinding` (x2 overloads), `getLinkForProfile`, `prependLinks`, `getLinkForUrl`, `getCanonicalForDefaultContext`, `getDefinitionsName`. Mostly rendering/link generation; injected into ProfileUtilities (`pkp`). Not referenced by ProfilePathProcessor directly. → ch12 (configuration surface).

---

## 6. Notable landmarks

- **L42-58**: `APPLY_PROPERTIES_FROM_SLICER = false` — disabled fix for slices not inheriting slicer properties; comment cites profiling-doc text and the Zulip discussion where the community chose *not* to enable it. Guards L764, L776-786, L816, L941.
- **L216-229**: GDG comment dated 28-July-2025 — post-hoc diffCursor advance when a code path consumed diffMatches without moving the cursor (sd-nested-ext test case); author explicitly flags it as fragile ("might be something that needs revisiting").
- **L246-263**: `checkDiffAssignedAndCursor` body fully commented out (derivation-pointer assertions).
- **L319**: commented-out earlier condition for the pre-slice-default detection.
- **L380-382**: root-slicing error + "This situation is not yet handled ... please report issue to grahame@fhir.org" Error.
- **L504** and **L1504**: version branch — `!VersionUtilities.isR4Plus(version) || !profileUtilities.isNewSlicingProcessing()`: R3 (and unpatched R4) shortcut inserts a typed synthetic slicer; R4+ inserts an untyped one (no constraint on other slices' types). `newSlicingProcessing` flag is described as "a work around for editorial loop dependency". Duplicated in both ConstrainTypes methods.
- **L514-516 / L634-636** and **L1514-1529 / L1623-1626**: the synthetic shortcut slicer is inserted into the *live differential* and removed again afterwards (`elementToRemove`).
- **L597 / L1587**: comment — type slicing is always CLOSED regardless of what the differential says.
- **L657**: special case `currentBasePath.contains("xtension.value")` — shortcut type-slicing on extension values removes non-sliced types instead of opening the slicing.
- **L689** and **L1382**: reslice base-id computation — "this is wrong if there's more than one reslice (todo: one thing at a time)" (duplicated workaround).
- **L710, L730**: re-entrant `profileUtilities.generateSnapshot` calls (xver extension definitions; type-profile bases without snapshots) — circularity surface (ch11).
- **L745**: `log.info("At this time the reference to " + eid + " cannot be handled - consult Grahame Grieve")` — profile-element reference during snapshot generation.
- **L756-760**: constraints cleared when a resource root is used as an element template ("the sense of %resource changes").
- **L767**: "temporary work around" — non-Extension type templates get min/max reset from currentBase.
- **L802-810**: slice min=0 reset logic, incl. shared-min rule for multiple slices under CLOSED slicing.
- **L803**: `!currentBasePath.endsWith("xtension.value[x]")` — "hack work around for problems with snapshots in official releases".
- **L814-815**: commented-out [x]-renaming logic (path renaming for single-type constraints).
- **L816-818**: slice max capped to slicer max when `!APPLY_PROPERTIES_FROM_SLICER`.
- **L1028-1029, L1039**: `indexOfFirstNonChild` off-by-one doubts in comments ("should be baseLimit?").
- **L1090**: `//TODO - fix` on a recursion call missing withDiffLimit.
- **L1202-1207**: sliced-base rules comment, incl. "corallory: you can't re-slice existing slices. is that ok?".
- **L1296-1297**: commented-out `throw new Error("Not done yet")` + alternative BackboneElement condition.
- **L1359-1360**: "Lloyd - add this for test T15" — `cursors.baseCursor--` compensation.
- **L1364-1369**: closed-slicing extension error skipped for `[x]` paths ("polymorphic type ... slice that actually implicitly exists").
- **L1415-1416**: "todo: should we consider other constraints?" + commented-out Error in type-profile min/max pickup.
- **L1419**: `throw new Error("Not handled: multiple profiles at ...")` for new slices with >1 type profile.
- **L1450-1452**: commented-out "lloydfix" for Extension profile resolution.
- **L1488-1492**: `debugCheck` hardcodes `http://nictiz.nl/fhir/StructureDefinition/Bundle-MedicationOverview` — leftover targeted debugging.
- **L1654**: commented-out `throw new Error("not done yet - slicing / types @ ...")`.
