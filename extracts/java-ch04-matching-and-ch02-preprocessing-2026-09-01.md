# Java deep-read — ch04 element matching + ch02 differential preprocessing (Phase 3 packet J-c)

Date: 2026-09-01. Source commit: `b06c7ee` (org.hl7.fhir.core master 2026-08-21; sparse clone at
`C:\Git\snapshot-spec-materials\org.hl7.fhir.core`). Abbreviations: PU = `ProfileUtilities.java`,
PPP = `ProfilePathProcessor.java`, PRE = `SnapshotGenerationPreProcessor.java` (all in
`org.hl7.fhir.r5/src/main/java/org/hl7/fhir/r5/conformance/profile/`); driver = `SnapShotGenerationTests.java`
(`org.hl7.fhir.r5/src/test/java/org/hl7/fhir/r5/test/`). Every line number below was read from source in
this session. Out of scope here (already deep-read): preprocessor slice-stuff propagation
(`java-preprocessor-slicestuff-2026-08-31.md`), additional-base merge (`java-ch05-...`), the slicing
branches' internals (`java-ch06-*`).

---

## A. Walk architecture: Java walks the BASE and looks up the differential

Entry: static `ProfilePathProcessor.processPaths(pu, base, derived, url, webUrl, differential, baseSnapshot,
mapHelper)` PPP:155-181 sets `baseLimit = baseSnapshot.size()-1` (PPP:169) and
`diffLimit = differential.hasElement() ? size-1 : -1` (PPP:170), then calls the instance loop.

Instance loop PPP:191-235:
```
while (baseCursor <= baseLimit && baseCursor < base.size()) {          // 198
  currentBase = base[baseCursor]; currentBasePath = fixedPathSource(...)  // 200-201 (redirector-aware)
  diffMatches = pu.getDiffMatches(differential, currentBasePath, diffCursor, diffLimit, profileName) // 204
  dc = diffCursor                                                       // 206
  if (!currentBase.hasSlicing() || currentBasePath.equals(slicing.path)) processSimplePath(...)      // 208-209
  else processPathWithSlicedBase(...)                                   // 214
  // GDG 28-July-2025 patch, PPP:216-229: if a branch consumed matches without moving the cursor,
  if (diffMatches.size() > 0 && dc == diffCursor) diffCursor += diffMatches.size();               // 225-228
}
checkAllElementsOK();  // 233: throws NULL_MIN if any result element has a null min value (PPP:237-244)
```
- The differential is never walked on its own; it is *queried* per base element. Every base element in
  scope is visited exactly once; diff rows are consumed only when a base element pulls them in.
- `checkDiffAssignedAndCursor` PPP:246-263 is a fully commented-out invariant ("every diff row before the
  cursor has been assigned") — a disabled self-check of the cursor discipline.
- The July-2025 patch comment (PPP:216-224) admits the per-branch cursor discipline is unresolved
  ("some of the code paths above don't [advance]… pretty difficult… it *works*"); added for `sd-nested-ext`
  (manifest line 296).

Contrast .NET (`ElementMatcher.Match`, ch4 .NET section): .NET walks the DIFF children of a level and looks
up the base sibling with a forward-only cursor. Inverse driver, same one-pass/ordering-dependent character.

## B. `getDiffMatches` — the matcher (PU:2444-2484) and `isSameBase` (PU:2487-2489)

```java
protected List<ElementDefinition> getDiffMatches(diff context, String path, int start, int end, String profileName) {
  String[] p = path.split("\\.");
  for (int i = start; i <= end; i++) {                       // 2449 — whole remaining scope, inclusive
    String[] sp = context.getElement().get(i).getPath().split("\\.");
    boolean ok = sp.length == p.length;                       // 2454 — same depth
    for (j...) ok = ok && sp.length > j && (p[j].equals(sp[j]) || isSameBase(p[j], sp[j]));  // 2456 — per segment
    if (ok) result.add(...);                                  // 2474
  }
  ...
}
private boolean isSameBase(String p, String sp) {             // 2487
  return (p.endsWith("[x]") && sp.startsWith(p.substring(0, p.length()-3)))
      || (sp.endsWith("[x]") && p.startsWith(sp.substring(0, sp.length()-3)));
}
```
Semantics, with consequences:

| # | rule | consequence | .NET (`ElementMatcher`, ch4) |
|---|---|---|---|
| B1 | scan is over the **entire remaining scope** `[diffCursor, diffLimit]`, not just the cursor position | every same-path row in scope is collected in ONE list (`diffMatches`) — the slicing entry and all its slices arrive together, whether or not they are adjacent (intervening *deeper* rows are skipped by the depth test; intervening *foreign sibling* rows are not matched here and are skipped by the cursor jumps in §D → orphans, §F) | per-level walk of diff children in order; forward-only base cursor `MoveToNext(name)` (`:134`) |
| B2 | depth equality (`sp.length == p.length`) | children never match a parent path; a diff row can only match a base element at the same depth | same (per-level) |
| B3 | segment equality OR `isSameBase` **at every segment** | `[x]`-tolerance applies to ancestor segments too, not just the leaf | leaf-only test via `IsRenamedChoiceTypeElement` |
| B4 | `isSameBase` first disjunct: base seg `value[x]`, diff seg starts with `value` | renamed-form diff row (`valueQuantity`) matches base `value[x]` — **no validation that the suffix is a type**, and the **bare stem** `value` also matches (length not required to exceed the stem) | prefix test too, but requires diff name *longer* than stem (`ElementDefinitionNavigationFunctions.cs:94`); suffix likewise unvalidated |
| B5 | `isSameBase` second disjunct: diff seg `value[x]`, base seg starts with `value` (i.e. base element is a **renamed** choice, R4-style) | a diff constraining `value[x]` against a base that renamed it **matches** | **does not match** → New + warning (`constructNew` `:517-535`) — code-derived deviation, no shared test exhibits it |
| B6 | unbounded prefix: any base sibling whose name extends the stem would also be pulled in (`value[x]` vs a hypothetical `valueSet` sibling) | latent; we know of no core type with such a sibling collision | same latent property |
| B7 | duplicates: two diff rows with the same path and no sliceName are both returned | dispatch decides (§E) — "slicing without a slicing entry" → throw `DIFFERENTIAL_DOES_NOT_HAVE_A_SLICE` unless extension (PPP:313-314) | unnamed non-extension slice → `Invalid` (drop with issue) |

Dead/commented material in the method: a debug-only check (PU:2458-2463) and — important for ch2 — a
**commented-out out-of-order warning** PU:2465-2473: `"Error: unknown element '<diff[start].path>' (or it is
out of order) in profile … (looking for '<path>')"`, removed "because it raises warnings when profiling
inherited elements… Might be better done when we're sorting the profile?". So Java has *no* in-walk
ordering diagnostic; ordering violations surface only as orphans (§G). The debug block PU:2477-2482 builds
an unused id set (no effect).

## C. `hasInnerDiffMatches` (PU:2420-2442) — "does the diff say anything BELOW this base element?"

```java
boolean hasInnerDiffMatches(diff context, String path, int start, int end, List<ElementDefinition> base, boolean allowSlices) {
  end = Math.min(context.getElement().size(), end); start = Math.max(0, start);
  for (int i = start; i <= end; i++) {
    statedPath = context.getElement().get(i).getPath();
    if (!allowSlices && statedPath.equals(path) && ed.hasSliceName()) return false;        // 2427 — a named slice row of the same path ends the search
    else if (statedPath.startsWith(path+".")) return true;                                    // 2429 — a child
    else if (path.endsWith("[x]") && statedPath.startsWith(path.substring(0, len-3))) return true; // 2431 — [x]: any row extending the stem (incl. a renamed SIBLING row itself, not only children)
    else if (i != start && !allowSlices && !statedPath.startsWith(path+".")) return false;    // 2433
    else if (i != start && allowSlices && !statedPath.startsWith(path)) return false;         // 2435
    else { /* not sure why we get here, but returning false at this point makes a bunch of tests fail */ } // 2438
  }
  return false;
}
```
- The fall-through arm (PU:2437-2439) is reached when `i == start` and the first row in the window is
  neither a child nor a same-path named slice — i.e. **the normal case where the window begins at the
  element's own row** (or at an unrelated row). Skipping the first row and then asking whether the
  *following* rows are children is exactly the intended semantics; the comment reflects that the author
  did not recognize `i == start` as that case. Not a bug. Precision (advisor check, 2026-09-01): this holds
  for non-`[x]` paths. For a `[x]` path the element's OWN row already satisfies the stem test at PU:2431
  (`Observation.value[x]` starts with `Observation.value`) and returns **true** before the fall-through — a
  spurious "inner match" if such a window ever occurred. It does not in practice: every dispatch tests
  `diffsConstrainTypes` (which returns true for any `[x]` base path with matches, PU:1810-1848) before the
  branches that call `hasInnerDiffMatches` with the window at the own row (PPP:377, PPP:1260), and the
  empty-match callers (PPP:1077, PPP:1658) by definition have no own row in the window. Recorded, not a bug.
- `allowSlices` asymmetry: `false` at PPP:377 (introducing slicing on an unsliced base) and PPP:1260
  (sliced-base default) — a named slice row terminates the search, because rows after it belong to the
  slice, not to the entry; `true` at PPP:1077 (base element not mentioned) and PPP:1658 (sliced base,
  nothing mentioned) — named slice rows are looked past.
- Bound: `end = min(size, end)` then `get(i)` for `i <= end` would throw if `end == size`; every caller
  passes `getDiffLimit()`, whose top-level value is `size-1` (PPP:170) and whose sub-scope values come from
  `findEndOfElement` (≤ size-1). Safe as used — not recorded as a bug.

## D. Scope helpers

- `findEndOfElement(diff, cursor)` PU:2491-2499: index of the last row whose path starts with
  `diff[cursor].path + "."` (contiguous run only). Guards `cursor >= size` → returns cursor (PU:2493-2494).
- `findEndOfElement(snapshot, cursor)` PU:2501-2507: same on the base, no bound guard.
- `findEndOfElementNoSlices(snapshot, cursor)` PU:2509-2515: stops at the first child carrying a sliceName.
- `getSiblings(list, current)` PU:2339-2349: rows after `current` with `pathMatches` (`[x]`-tolerant,
  PU:2027-2029) while path length ≥ current's — used for base slice rows (PPP:1311).
- `pathStartsWith(p1, p2)` PU:2023-2025: prefix test tolerant of `p2` ending in `[x].`.
- `pathMatches(path, ed)` PU:1141-1153 (used by `findTypeSlice`): equality, or `[x]` stem + non-dotted
  suffix (leaf-only, requires a suffix).

## E. Dispatch — where `diffMatches` goes (the Java "action space")

### E.1 Unsliced base (`processSimplePath` PPP:283-305)

| condition (in order) | branch | .NET action analogue |
|---|---|---|
| `diffMatches.isEmpty()` PPP:293 | `processSimplePathWithEmptyDiffMatches` PPP:1061 — copy base; if `hasInnerDiffMatches(…, allowSlices=true)` PPP:1077 recurse into base children (PPP:1080-1092) or, when the base has no children, step into the type (PPP:1093-1130, ch7) | base child not in diff → copied; .NET's `MakeTree` stand-in parent produces the same outcome (see §H.4) |
| `oneMatchingElementInDifferential(slicingDone, path, diffMatches)` PPP:296 (see below) | `processSimplePathWithOneMatchingElementInDifferential` PPP:674-938 — the plain **merge** (`updateFromDefinition` PPP:813) incl. named-slice handling when called from a slice scope (PPP:794-811) | `Merge` |
| `pu.diffsConstrainTypes(diffMatches, path, typeList)` PPP:298 (PU:1806-1849; `size<2` guard commented out PU:1807-1808 → a single renamed/typed row suffices) | `processSimplePathWhereDiffsConstrainTypes` PPP:493 — type slicing (ch6, DEV-020) | `constructChoiceTypeMatch` (`Merge`/`Add` on type slices) |
| else PPP:301 | `processSimplePathDefault` PPP:307-448 — the diff **slices** an unsliced base: preconditions (non-repeating → throw `ATTEMPT_TO_A_SLICE_AN_ELEMENT_THAT_DOES_NOT_REPEAT` PPP:309-312 unless sliced-to-one/type-slicing; first row without `slicing` and not extension → throw `DIFFERENTIAL_DOES_NOT_HAVE_A_SLICE` PPP:313-314); extension without entry → `makeExtensionSlicing()` + `SNAPSHOT_auto_added_slicing` PPP:343-345; then each remaining row processed as a slice against the same base scope PPP:430-444 | `Slice` (incl. .NET's implicit extension `Slice` with empty diff bookmark) then `Add` per slice |

`oneMatchingElementInDifferential` PPP:1723-1736:
```
size != 1 → false; slicingDone → true; isImplicitSlicing(row, path) → false   // PU:1796-1803: path ≠ row.path AND path ends [x] AND row.path starts with stem
return !(row.hasSlicing() || (isExtension(row) && row.hasSliceName()))
```
so a single row is a plain merge unless it (a) is a renamed-form row of a `[x]` base (→ type slicing),
(b) carries a `slicing` component (→ slice the base), or (c) is a *named extension slice* (→ slicing path,
which auto-creates the extension entry). A single named **non-extension** slice under an unsliced base
outside a slice scope IS treated as a plain merge here → its handling then depends on
`checkToSeeIfSlicingExists` PPP:955-987 (ch6 Java section).

### E.2 Sliced base (`processPathWithSlicedBase` PPP:1196-1223)

| condition | branch |
|---|---|
| empty | `processPathWithSlicedBaseAndEmptyDiffMatches` PPP:1657 (copy entry + all base slices; inner matches with `allowSlices=true` PPP:1658) |
| `diffsConstrainTypes` | `processPathWithSlicedBaseWhereDiffsConstrainTypes` PPP:1494 (ch6) |
| else | `processPathWithSlicedBaseDefault` PPP:1225-1482: slicing-component compatibility throws PPP:1229-1240; entry merge PPP:1241-1255; then **slice-to-slice matching** PPP:1311-1362 — walk base slices in base order (`getSiblings`), and for each base slice test ONLY the diff row at `diffpos` (`diffMatches[diffpos].sliceName == base.sliceName`, PPP:1321): match → recurse into that slice's scope and `diffpos++`; no match → copy the base slice + its children unchanged. Leftovers PPP:1364-1396: `closed` base slicing + leftover rows → throw `THE_BASE_SNAPSHOT_MARKS_A_SLICING_AS_CLOSED…` unless `[x]` path (PPP:1364-1369); a leftover whose sliceName equals ANY base slice name → throw `NAMED_ITEMS_ARE_OUT_OF_ORDER_IN_THE_SLICE` (PPP:1373-1375); otherwise **new slice** appended (template = base entry, or the parent slice for reslices `a/b` via `getById` PPP:1380-1384), `min=0` PPP:1390, merged PPP:1396. |

Ordering consequence: slice-to-slice matching is a *lockstep* walk — diff slices must appear in base order.
A diff that lists existing slices out of order (or a new slice *before* an existing one) yields a leftover
carrying an existing name → hard throw. .NET (`matchSlice`, ch4 `:804-853`): forward-only base slice cursor;
an out-of-order existing name is a no-match → `Add` (a second slice with the same name — code-derived; no
shared test exhibits it). Note `sliceIsConstraining` plays NO role in Java: the string does not occur
anywhere in PU/PPP/PRE (grep, b06c7ee) — see §K.

## F. Cursor discipline → how orphans arise

- One-match branch: `cursors.diffCursor = differential.indexOf(diffMatches[0]) + 1` PPP:827 — the cursor
  **jumps** to just after the matched row. Any diff rows between the old cursor and the match are skipped
  for good (they can never be matched later because getDiffMatches only scans forward from the cursor and
  the base cursor never returns). Then PPP:828-857 consumes the contiguous children (`pathStartsWith(row,
  matchPath + ".")`) into the recursive scope.
- Slicing branch: `cursors.diffCursor = newDiffLimit + 1` PPP:447 (end of the LAST slice's children) — rows
  interleaved between slices that belong to other base elements are skipped.
- Sliced-base branch: `cursors.diffCursor = newDiffLimit + 1` per matched slice PPP:1340.
- Generic patch PPP:225-228 advances by `diffMatches.size()` only when a branch left the cursor untouched.

**t23a mechanism (DEV-027 Java side):** diff order `contact:males.gender` (base position 5) then
`contact:males.telecom` (base position 3). Base cursor reaches `telecom` first; `getDiffMatches` scans
`[cursor=gender, limit]`, finds `telecom` at cursor+1 → merge; cursor jumps to `indexOf(telecom)+1`, past
`gender`. When the base cursor later reaches `gender` the scan window starts after it → no match → base
`gender` copied unchanged → the `gender` row is an orphan → §G reports `No match found for
Patient.contact:males.gender in the generated snapshot: check that the path and definitions are legal in
the differential (including order)` (PU:925). Verified against the fail-test extract (t23a: ERRORS(2)).

## G. Unmatched differential rows: the constraint/specialization split (PU:842-867, PU:908-948)

1. **Specialization second pass** PU:842-867 (only `derivation == SPECIALIZATION`): every diff row (path
   containing `.`) NOT marked `SNAPSHOT_GENERATED_IN_SNAPSHOT` is looked up by exact path among the
   snapshot's trailing rows (`getElementInCurrentContext` PU:1189-1199: scans backwards from the end while
   the candidate shares the row's parent prefix); found → `updateFromDefinition` onto it (PU:849); not
   found → **appended as a new element** at `findLastChildForParent` (PU:1099-1109: after the last row
   starting with the parent path; throws `"Unable to find parent path … (internal code error)"` if the
   parent is absent) with `updateURLs` + `markExtensions` (PU:852-855), and if the diff walks into it
   (`walksInto` PU:1285-1288 = next row is a child) with a single type, the type's children are inherited
   (`addInheritedElementsForSpecialization` PU:860; multiple types → throw PU:858).
2. **Verification loop** PU:908-948 (all derivations): for every row of the *processed* diff clone, migrate
   `SNAPSHOT_DERIVATION_EQUALS`/`_POINTER` user data back to the caller's original row via
   `SNAPSHOT_diff_source` (PU:913-920; rows injected by the preprocessor have no source and are skipped);
   rows still lacking `SNAPSHOT_GENERATED_IN_SNAPSHOT` are **orphans**: per-row ERROR message if the row has
   an id (PU:924-927, text above), plus one profile-level ERROR via `handleError` PU:947 ("The profile … has
   N element(s) in the differential (…) that don't have a matching element in the snapshot: check that the
   path and definitions are legal in the differential (including order)"). Matched rows get
   `SNAPSHOT_DERIVATION_DIFF` cross-links (PU:929-930).
3. `handleError` PU:1217-1219 → `addMessage` PU:1221-1226: appended to `messages`; **throws
   `DefinitionException` only if `wantThrowExceptions`** (field PU:452, default `false`; setter PU:4729).
   The test driver sets it `false` and fails a test on any ERROR message (driver message gate,
   :627-632/:677-679 per the driver extract). So for a constraint profile an orphan row is **dropped from
   the snapshot and reported**, never materialized.

**Contrast .NET:** no derivation split — every unmatched diff child becomes a `New` element via
`createNewElement` (`SnapshotGenerator.cs:887`), **no issue** (ch4 .NET table). The spec's
"constraint SDs are not allowed to define … a path not defined within the base" [elementdefinition #path]
+ "elements from the baseDefinition appear before new elements in a StructureDefinition with derivation
'specialization'" [structuredefinition §5.4.6] is implemented literally by Java (orphan = author error for
constraints; append for specializations) and not at all by .NET.

## H. Preprocessing in `generateSnapshot` (PU:770-839) — order of operations

```
774-778  circular snapshotStack check (throw) → derived.generatingSnapshot=true
788      derived.snapshot = new (empty)
791      checkDifferential(derived.differential.element, derived.typeName, url)   // on the ORIGINAL rows
792      checkDifferentialBaseType(derived)                                         // may MUTATE the original root row
808      copyInheritedExtensions(base, derived, webUrl)
810      findInheritedObligationProfiles(derived)
820-821  clear SNAPSHOT_GENERATED_IN_SNAPSHOT on every original diff row
824      diff = cloneDiff(derived.differential)   // "we're sometimes going to hack the differential while processing it"
825      new SnapshotGenerationPreProcessor(this).process(diff, derived)            // on the CLONE
827-832  baseSnapshot = base.snapshot, or cloneSnapshot with ids re-rooted for SPECIALIZATION (PU:1493-1500+)
837      MappingAssistant
839      ProfilePathProcessor.processPaths(...)
```

### H.1 `checkDifferential` PU:1413-1461 — path validation (all throws are `FHIRException`)
For every row: no `path` element / null value → throw (`NO_PATH_ON_ELEMENT_IN_DIFFERENTIAL_IN_`,
`NO_PATH_VALUE_…` PU:1417-1423); path must equal `urlTail(type)` (first row only) or start with `type+"."`
→ else throw `ILLEGAL_PATH__IN_DIFFERENTIAL_IN__MUST_START_WITH_` PU:1424-1426 (this is the sdf-8a check
— note it is applied to EVERY row, and it is what catches t37's `MedicationRequiest…` when the driver does
not sort first); per segment (PU:1434-1458): empty segment → throw `…NAME_PORTION_MISING_` (obs-unit `..`),
>64 chars → throw, Unicode whitespace → throw, characters `, : ; ' " / | ? ! @ # $ % ^ & * ( ) { }` → throw,
any char `< ' '` or `> 'z'` → throw (note: `[` and `]` pass this filter — they are handled next), and `[`/`]`
allowed only as a trailing `[x]` with none elsewhere in the segment → else throw. `first` is never set to
false (PU:1414 initialised, never reassigned) — so the "may equal the type" allowance applies to every row,
not just the first; a duplicate bare root row later in the diff passes this check.

### H.2 `checkDifferentialBaseType` PU:1317-1325 — root type (sdf-15a)
If the first diff row is a root (no `.`) and has `type`: when `wantFixDifferentialFirstElementType` (PU:444,
**default false**; setter PU:497) AND the single type equals the base SD's `type` (`typeMatchesAncestor`
PU:1327-1330) → **clear the type on the ORIGINAL row** (PU:1320 — mutation of the caller's SD, before
cloning); else if not LOGICAL → `throw new Error(TYPE_ON_FIRST_DIFFERENTIAL_ELEMENT)` (`java.lang.Error` —
PU imports no custom `Error` type; verified against the import list — not FHIRException;
ext-recursion-1/ext-ccuk in DEV-028 group (f)). Logical models pass through.

### H.2a Differential-less SD (OQ-016 Java side, code-derived)
`checkDifferential` iterates zero rows; `checkDifferentialBaseType` is skipped (`hasDifferential()` false);
`cloneDiff` yields an empty component; PPP:170 sets `diffLimit = -1`, so `getDiffMatches`/`hasInnerDiffMatches`
loops (`for i = start; i <= end`) never execute → every base row takes the empty-match branch → snapshot =
base copy with `updateFromBase`/constraint-source/obligation fill. Same answer as .NET's synthesized empty
differential for the main path. Whether Java accepts a differential-less SD as a *type profile* root
(.NET's second, refusing path) is a ch7 question — J-d.

### H.3 `cloneDiff` PU:1483-1491 — caller isolation (OQ-015 Java side)
Every row is `copy()`d into a fresh differential component; each clone carries
`SNAPSHOT_diff_source` → original. All in-generation mutation (preprocessor injections/merges, generated
slice names, `setUserData` bookkeeping) happens on clones; the only things written back to the original
rows are `SNAPSHOT_DERIVATION_EQUALS`/`_POINTER` user data (PU:916-919), the `SNAPSHOT_GENERATED_IN_SNAPSHOT`
clear (PU:820-821), and the H.2 root-type clear (off by default). Note `Base.setCopyUserData(true)`
PU:779-780 wraps the whole run so `copy()` carries user data (restored afterwards).
The driver additionally runs `setIds(source, false)` on the ORIGINAL before generation (driver :548/:600)
— ids on Java differentials may originate outside `generateSnapshot` (ch10 note).

### H.4 No tree reconstruction — sparse parents handled in the walk
Java never builds a differential tree and never inserts stand-in rows (PRE injects rows only for slice
propagation, other extract). A base element not mentioned by the diff whose *children* are mentioned is
handled by `processSimplePathWithEmptyDiffMatches` PPP:1061-1130: the base row is copied (with
`updateFromBase`, `updateConstraintSources`, `updateFromObligationProfiles`, `markDerived`, PPP:1062-1070),
then `hasInnerDiffMatches(…, allowSlices=true)` PPP:1077 decides whether to recurse: base has children
(`baseHasChildren` PPP:1043-1049, `[x]`-aware `isChildOf` PPP:1052-1058) → recurse over
`[baseCursor+1, end-of-children]` with the same diffCursor (PPP:1080-1092, `withSlicing(new
PathSlicingParams())`); base has no children → "implicitly step into a new type": no type and no
contentReference → throw `_HAS_NO_CHILDREN__AND_NO_TYPES_IN_PROFILE_` PPP:1094-1096; then the
`_HAS_CHILDREN__AND_MULTIPLE_TYPES__IN_PROFILE_` throw PPP:1115-1117 — **DEAD CODE here**: its guard
`nonExtension` is set only inside `for (ed : diffMatches) if (ed != diffMatches.get(0) && …)` PPP:1101-1103,
and `diffMatches` is empty in this branch, so the loop never runs (the same guard at PPP:846-851 in the
one-match branch is dead too: `size()==1` so `ed != get(0)` is always false). What actually happens for a
multi-type sparse parent (PPP:1158): `dt = fetchTypeDefinition("Element")` when `type.size() > 1`, else the
element's datatype profile → the child rows are processed against **Element's** snapshot (so non-extension
children under a multi-type parent become orphans, §G, rather than the intended targeted error);
`dt == null` → throw `UNKNOWN_TYPE__AT_` PPP:1159-1160; contentReference → follow (PPP:1118-1156, ch8).
The same constant is used live-or-not at PPP:1428 (leftover new-slice branch) and PU:1624
(`getTypeForElement`, called with the real multi-row `diffMatches` from PPP:385) — not analysed here (J-d).
Outcome-equivalent to .NET's stand-in parent (an empty ElementDefinition merged over the base copy is a
no-op merge) — same "expand only where the diff constrains children" policy (ch7/ch11). Difference in
*kind*: .NET validates nothing here (a sparse chain to a bogus path becomes stand-ins + New), Java throws
for the no-type/no-contentReference case.

### H.5 The preprocessor invocation
`new SnapshotGenerationPreProcessor(this).process(diff, derived)` PU:825 — on the clone, after validation,
before the walk. Its three jobs are documented in the J-a/J-b extracts (slice-stuff propagation, additional
bases, orphan utilities). Its injected rows carry no `SNAPSHOT_diff_source` and are exempt from orphan
reporting (PU:913-914).

## I. `sortDifferential` — tooling-side ordering normalization (PU:3815-3869 + helpers)

**Not called from `generateSnapshot`** (or anywhere else in the checked-out cone except the test driver:
`:550` testSort, `:610` testGen when `sort="true"`, `:699` every base in a getSD chain). Public API used
by tooling (IG publisher etc. — outside the cone, not verified).

Algorithm:
1. Stamp `SNAPSHOT_SORT_ed_index` = original index on every row (PU:3816-3820; used only in the
   `compareDiffs` message). Snapshot `original` list; `lastCount = size` (PU:3821-3824). Empty diff → return.
2. Root holder (PU:3829-3840): if `diff[0].path` contains `.` → a **placeholder** root holder for
   `path.split(".")[0]`; else `diff[0]` is the root holder and processing starts at index 1.
3. `hasSlicing` scan PU:3842-3850 — computed, **never used** (dead).
4. `processElementsIntoTree(holder, i, list)` PU:3888-3908: consumes rows while `path.startsWith(prefix)`
   (`prefix = holder.path + "."`); a row more than one level below the holder gets a **placeholder
   intermediate holder** (PU:3892-3899; the `substring(prefix.length()+1)` skips one char of the child
   segment — harmless since a segment cannot start with `.` after H.1); otherwise a real child holder and
   recursion from `i+1`. **The top-level loop stops at the first row not under the root prefix** (a second
   root row, or a row under a different root such as t37's `MedicationRequiest.…`): that row and everything
   after it are never placed → dropped → step 8 error.
5. `sortElements(holder, cmp, errors)` PU:3910-3928: siblings sorted by `ElementDefinitionComparer`
   (PU:3720-3812) = **ascending index of the corresponding row in the base snapshot** (`find`, PU:3755-3797:
   exact path, `[x]`-tolerance in both directions PU:3764-3768, and `contentReference` following with three
   reference styles PU:3770-3782 + `MAX_RECURSION_LIMIT` guard PU:3786-3787). Not found → **index 0** and an
   error string recorded in the comparer (PU:3790-3795)… **which is copied to `errors` only when `debug`**
   (`if (debug) cmp.checkForErrors(errors)` PU:3916-3918; `debug` PU:431 default false). So in non-debug
   mode an unknown path silently sorts to the FRONT of its sibling group. Single child: `find(…, mandatory=
   false)` → no error even in debug (PU:3911-3913). `Collections.sort` is stable → rows with equal base
   index (a slicing entry and its slices; two slices) keep their authored relative order.
6. Child scopes get their own comparer via `getComparer` PU:3930-4002 ("running off the base profile into a
   data type profile"): the base row's type decides which snapshot orders the children — no type / abstract
   (`Element`, `BackboneElement`, `Resource`, `DomainResource` PU:4028-4030) / type == path → same snapshot
   (PU:3949), except `Resource` typed with a single profile → that profile's non-constraint ancestor
   (PU:3935-3947; >1 profile → throw `UNHANDLED_SITUATION_RESOURCE_IS_PROFILED_TO_MORE_THAN_ONE_OPTION`);
   `Extension` with a single profiled child type → the extension SD (PU:3951-3956, null → skip sorting that
   subtree); single non-`*` type → that type's SD (PU:3957-3961, unresolvable → throw); child single-typed
   → child's type (PU:3962-3966); `[x]` base with renamed child row → type parsed from the suffix
   (PU:3967-3976, unknown → `Error`); Reference-only multi-type → Reference (PU:3977-3992; mixed → throw
   `CANT_HAVE_CHILDREN_ON_AN_ELEMENT_WITH_A_POLYMORPHIC_TYPE…`/`NOT_HANDLED_YET_SORTELEMENTS_`); otherwise
   `Element` ("allowed if we only profile the extensions", PU:3993-3999).
7. `writeElements` PU:4033-4039 serialises depth-first, **omitting placeholders**.
8. If `errorIfChanges` → `compareDiffs(original, newDiff)` PU:3871-3885 (size change or first out-of-order
   row → error string; the driver always passes `false`; no caller passes `true` in the cone). Replace the
   list in place (PU:3864-3865). `lastCount != size` → error `"Sort failed: counts differ; at least one of
   the paths in the differential is illegal"` PU:3867-3868.

Consequences recorded:
- **Java's golden files are sorted-differential behavior**: the driver sorts every `sort="true"` test and
  every base in a chain before generation; the harness oracle (BatchRunner) replicates this. 28 R5 manifest
  tests carry `sort="true"` (grep of manifest.xml: t23, t26, t28, t29, t29a, t30a, t30b, t31–t38, t40–t45
  incl. t43a/t44a, ext-sort-issue, au-med-k, nl-med, telus-oo); t26 is sort-only.
- **t23 vs t23a are the same differential** (fixtures diff only in ids/metadata; both list `males.gender`
  before `males.telecom`): t23 `sort="true"` → sorted → passes; t23a no sort → orphan error (§F). The
  manifest attribute is the whole difference.
- **t37** (`MedicationRequiest…` typo) with `sort="true"` fails in the SORT step: the foreign-root row is
  dropped by step 4 → count error → driver throws "Sort failed: …" (fail-test extract confirms). Without the
  sort step, `checkDifferential` H.1 would throw the path-naming `ILLEGAL_PATH…MUST_START_WITH_` instead.
  Correction for OQ-014's "Java names the offending types" clause: true for obs-1-2/obs-2-3/obs-3, NOT for
  t37 (sort count error names nothing).
- **.NET side:** the vendored manifest driver deserializes the `sort` attribute
  (`SnapshotGeneratorManifestTests.cs:916-917`) but **never reads it**; instead `Fix_t23` (`:236-252`)
  rewrites the checked-in t23 input by **swapping elements 3 and 4** — a hand-sort standing in for the
  missing `sortDifferential`. t26 (sort-only) is `[Ignore]`d with "input==expected" (`:445`): correct, since
  t26's input is already in base order (verified: identical path sequences in `t26-input.xml` and
  `t26-expected.xml`) and a sort-only test on ordered input must leave it unchanged (settles DEV-012).
  t23a is commented out (`:435`).

## J. `closeDifferential(base, derived)` PU:3424-3438 — "prohibit everything not mentioned"

For every base-snapshot row that is an **immediate child of the root** (exactly two segments,
`isImmediateChild` PU:3491-3498) and does not end in `.id`: no diff row with the same path
(`getMatchInDerived` PU:3471-3478 — path only, sliceName ignored, first hit) → append a new diff row
`{path, max="0"}` (no id, no min); if the base row **has slicing** and IS mentioned → `closeChildren`
PU:3440-3460 recurses over the base row's children within the diff row's contiguous scope (`findEnd`
PU:3462-3468), closing unmentioned immediate children and recursing into mentioned ones. Then
`sortDifferential(…, errors=new list, false)` PU:3437 — errors discarded. Quirks: deeper unmentioned
elements under *unsliced* mentioned parents are NOT closed (only root children and sliced subtrees);
matching ignores slice names. **No caller in the checked-out cone** (public utility; IG-publisher-side
"closed profile" tooling presumably). .NET has no counterpart.

## K. `cleanUpDifferential(sd)` PU:4570-4619 + `determineSlicing` PU:4622-4645 — legacy slicing repair

`cleanUpDifferential(sd)` → `(sd, start=1)` when >1 rows (assumes row 0 is the root). Per level: for each
first occurrence of a path, scan later same-level rows with the same path; on the second occurrence insert a
synthetic **slicing entry** `{path, slicing.rules=OPEN}` before the first (PU:4593-4599), collect all rows as
slices; `determineSlicing` names them from `SNAPSHOT_slice_name` user data if present (no producer anywhere
in the checked-out cone) else `slice-N` (PU:4624-4632), and assigns a **hard-coded discriminator by path**
(PU:4635-4644): `.extension`/`.modifierExtension` → `value:url`; `DiagnosticReport.result` →
`value:reference.code`; `Observation.related` → `value:target.reference.code` (STU3-era element);
`Bundle.entry` → `value:resource.@profile` (DSTU2 discriminator syntax); anything else → `throw new
Error("No slicing for …")`. Recurses per child level (PU:4612-4617). **Not called by `generateSnapshot`;
no caller in the cone.** Java's in-generation counterpart to .NET's implicit extension slicing entry is
NOT this — it is `makeExtensionSlicing` at PPP:343-345 and `checkToSeeIfSlicingExists` PPP:955-987 (ch6).
Sibling legacy helpers: `interpretR2Discriminator`/`makeDiscriminator`/`buildR2Discriminator` PU:4648-4677
(R2 discriminator string ↔ R5 component; input normalization for converted DSTU2 content).

## L. Verified absences (this scope, b06c7ee)

- `sliceIsConstraining` is **never read** in PU/PPP/PRE (grep `-i` over the package: zero hits). Java
  matches slices by name only and ignores the property entirely; .NET enforces it (`matchSlice`
  `ElementMatcher.cs:816-838`). Instance-validator behaviour is out of scope.
- No in-walk ordering diagnostic (warning commented out PU:2465-2473); ordering violations are detected
  only post hoc as orphans (§G) — and only for rows that end up unmatched. A misordered row that still
  gets matched because the base element happens to come later is accepted silently.
- No differential tree / stand-in rows (§H.4). No path-level validation beyond `checkDifferential` (no
  sliceName grammar check here — eld-16 is ch6's territory; type-slice naming is checked in the type-slicing
  branch, ch6).
- `sortDifferential`/`closeDifferential`/`cleanUpDifferential` are not part of generation.

## M. Upstream candidates from this packet

- **JI-16 (needs-verification, design question):** `sortDifferential` records "Differential contains path X
  which is not found in the base" only under `debug` (PU:3916-3918); in production an unknown path silently
  sorts to the front (`find` returns 0). Possibly deliberate — the guard may exist to silence false
  positives on inherited/contentReference'd elements (cf. the identical rationale for the commented-out
  warning in `getDiffMatches`); an unknown path is anyway caught later by `checkDifferential` (H.1) when
  generation follows. Do not file blind; re-check vs master and consider asking rather than filing.
- **JI-17 (ready, minor; hold until J-d reads PPP:1428/PU:1624):** the `nonExtension` guard for
  `_HAS_CHILDREN__AND_MULTIPLE_TYPES__IN_PROFILE_` iterates `diffMatches` skipping `get(0)` — at PPP:846-851
  the list has exactly one element and at PPP:1097-1116 it is empty, so the intended "children under a
  multi-type element" error can never fire from those two sites; the code falls through to processing the
  children against `Element` (PPP:908 / PPP:1158). Presumably the loop was meant to run over the *child
  rows* in the diff window. Verified by reading both sites (§H.4); consequence = silent orphaning instead
  of a targeted error. Re-check vs master before filing.
- Not queued (not bugs): `hasInnerDiffMatches` fall-through (§C, intended semantics); its `end` bound
  (safe as called); `processElementsIntoTree` `+1` substring quirk (harmless); dead `hasSlicing` scan
  PU:3842-3850 (cosmetic); `checkDifferential` `first` never reset (lets a later bare-root row through the
  path check — but such a row then fails in the walk as an orphan/`ADDING_WRONG_PATH`; low value).

## N. .NET ↔ Java contrast summary (feeds ch2/ch4 tables)

| topic | .NET | Java |
|---|---|---|
| walk driver | diff children → base sibling (forward-only base cursor) | base rows → diff query over remaining scope (`getDiffMatches`), cursor jumps forward on match |
| sparse parents | `MakeTree` stand-in rows before matching | none; base copy + `hasInnerDiffMatches` recursion in the walk |
| path validation | none (`..` → phantom element, DEV-027) | `checkDifferential`: prefix, empty segment, charset, length, `[x]` placement — all throw |
| root `type` | never checked (DEV-028 f) | throw unless LOGICAL; optional auto-clear (default off) mutates the caller's SD |
| caller isolation | shared element instances (OQ-015) | `cloneDiff`; write-back limited to derivation user data (+ root-type clear when enabled) |
| unmatched diff row, constraint | silent `New` element | orphan: dropped + ERROR (throw if `wantThrowExceptions`) |
| unmatched diff row, specialization | silent `New` | second pass appends after the parent's last child, inherits type children |
| out-of-order siblings | silently degrade to `New` (t23a corrupt) | skipped by the cursor jump → orphan ERROR (t23a) |
| out-of-order existing slices | no-match → `Add` (duplicate name; code-derived) | throw `NAMED_ITEMS_ARE_OUT_OF_ORDER_IN_THE_SLICE` |
| slicing entry absent (non-extension, unsliced base) | `Invalid`, dropped with issue | throw `DIFFERENTIAL_DOES_NOT_HAVE_A_SLICE` |
| renamed diff row vs `[x]` base | match (prefix, longer than stem) | match (prefix, stem itself also accepted) |
| `[x]` diff row vs renamed base | New + warning | match (`isSameBase` symmetric) |
| `sliceIsConstraining` | enforced (`Invalid` on conflict) | ignored |
| type-slice removal direction | base type *slices* removed when the intro's type list shrinks (`Remove`) | *types* removed from a multi-type row when their type slice is prohibited (`max=0`) (PU:869-881) — inverse trigger |
| ordering normalization | none; `Fix_t23` hand-edits the fixture | `sortDifferential` (tooling; driver applies to 28 tests + all bases) |
| closed-profile / legacy slicing repair | none | `closeDifferential`, `cleanUpDifferential` (public utilities, unused in generation) |
