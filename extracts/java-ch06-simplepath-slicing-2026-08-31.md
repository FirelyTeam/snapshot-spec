# Java deep-read extract — ProfilePathProcessor "simple path" slicing machinery

**Source:** `org.hl7.fhir.r5/src/main/java/org/hl7/fhir/r5/conformance/profile/ProfilePathProcessor.java` in the sparse clone at
`C:\Git\snapshot-spec-materials\org.hl7.fhir.core`.
**Code read = clone commit `b06c7ee7c57367305ff72b6bb09ce779b5dbd6ce`** (verified via `git log -1`).
**Caveat:** the project's empirical sweep oracle was validator_cli **6.10.2 @ commit d06577dbc5c6**. Any claim in this extract that connects this code to sweep behavior carries that commit-pair caveat — b06c7ee and d06577dbc5c6 are different commits and the two must be re-diffed before treating a line-level claim as an explanation of an observed oracle output.

All line numbers below refer to `ProfilePathProcessor.java` @ b06c7ee unless another file is named. Helper semantics quoted from `ProfileUtilities.java` and `PathSlicingParams.java` (same package, same commit); message texts quoted from `org.hl7.fhir.utilities/src/main/resources/Messages.properties` @ b06c7ee (retrieved from the git object DB; the utilities module is not checked out in the sparse worktree).

---

## 0. Dispatch context (brief)

`processPaths` (L191-235) iterates `cursors.baseCursor` from entry to `baseLimit`. Per base element it computes `diffMatches = profileUtilities.getDiffMatches(...)` (L204 — **out of scope, see Parked findings**) and, when the base element is *not* sliced (or we are re-entering the slicer's own path), dispatches to `processSimplePath` (L208-209):

```java
L208  if (!currentBase.hasSlicing() || currentBasePath.equals(getSlicing().getPath())) {
L209    ElementDefinition currentRes = processSimplePath(currentBase, currentBasePath, diffMatches, typeList, cursors, mapHelper, first ? slicerElement : null);
```

Note `first ? slicerElement : null` — the incoming `slicerElement` parameter is only forwarded for the **first** base element of a recursion scope; subsequent iterations get `null`.

`processSimplePath` (L283-305) dispatch order:

1. `diffMatches.isEmpty()` → `processSimplePathWithEmptyDiffMatches` (plain copy; out of scope).
2. `oneMatchingElementInDifferential(getSlicing().isDone(), currentBasePath, diffMatches)` (L296) → `processSimplePathWithOneMatchingElementInDifferential` (§3). Predicate in §6.
3. `profileUtilities.diffsConstrainTypes(diffMatches, currentBasePath, typeList)` (L298) → `processSimplePathWhereDiffsConstrainTypes` (§2).
4. otherwise → `processSimplePathDefault` (§1).

Only case 2 returns a non-null `res` (the element handed back to a parent recursion, e.g. as slicer row).

### PathSlicingParams (context object for "we are inside a slicing")

`PathSlicingParams.java` L15-36: fields `done` (boolean), `elementDefinition` (the slicer ED), `path`, and `slices` — a list filled by `withDiffs(diffMatches)` with **`diffMatches[1..]`** (index 0, the slicing intro / first match, is excluded):

```java
L31  public PathSlicingParams withDiffs(List<ElementDefinition> diffMatches) {
L32    for (int i = 1; i < diffMatches.size(); i++) {
L33      slices.add(diffMatches.get(i));
```

So `slicing.slices.size() > 1` (used at L806, §3) means **at least two named-slice diff entries** followed the intro.

### `setPath` helper — name lies (code vs claim mismatch)

```java
L450  private ElementDefinition setPath(ElementDefinition ed, String path) {
L451    ed.setId(null);
L452    return ed;
L453  }
```

Despite its name and its `path` parameter, `setPath` **does not set the path**; it only nulls the id. Both call sites (L335, L369) read as if the slicer row were being re-pathed to `currentBasePath`; in fact only the id is cleared (ids are regenerated later). Spec text must not claim a path assignment here.

---

## 1. `processSimplePathDefault` (L307-448) — diff introduces slicing on an unsliced base

Reached when there are multiple diffMatches (or one that carries slicing/implicit slicing) and they do not constrain types.

### 1.1 Entry preconditions (L308-314)

**Unbounded check** (L309-312): slicing a non-repeating element is an error unless the intro is "sliced to one only" or is type slicing:

```java
L309  if (!profileUtilities.unbounded(currentBase) && !(profileUtilities.isSlicedToOneOnly(diffMatches.get(0)) || profileUtilities.isTypeSlicing(diffMatches.get(0))))
L312    throw new DefinitionException(...ATTEMPT_TO_A_SLICE_AN_ELEMENT_THAT_DOES_NOT_REPEAT__FROM__IN_, currentBase.getPath(), currentBase.getPath(), cursors.contextName, diffMatches.get(0).getId(), profileUtilities.sliceNames(diffMatches));
```

with the in-code comment (L310-311): `// you can only slice an element that doesn't repeat if the sum total of your slices is limited to 1` / `// (but you might do that in order to split up constraints by type)`.

Helper semantics (ProfileUtilities.java):
- `unbounded` (L2517-2526): true iff `max` is present and is neither `"1"` nor `"0"` (so `max="*"` or `max>=2`); a missing max element returns false (comment: `// this is not valid`).
- `isSlicedToOneOnly` (L2397-2399): `e.hasSlicing() && e.hasMaxElement() && e.getMax().equals("1")` — note it tests the **diff intro's own max**, and only literal `"1"`.
- `isTypeSlicing` (L2401-2405): `e.hasSlicing()` with exactly one discriminator of type `TYPE` and path `"$this"`.

Message text: `Attempt to a slice an element that does not repeat: {0}/{1} from {2} in {3}, at element {4} (slice = {5})`.

**Missing-slicing-intro check** (L313-314): if the first diff match has **no** `slicing` component and the base is not an extension element, it's an error:

```java
L313  if (!diffMatches.get(0).hasSlicing() && !profileUtilities.isExtension(currentBase)) // well, the diff has set up a slice, but hasn't defined it. this is an error
L314    throw new DefinitionException(...DIFFERENTIAL_DOES_NOT_HAVE_A_SLICE__B_OF_____IN_PROFILE_, ...);
```

`isExtension` (ProfileUtilities L2416-2418): path ends with `.extension` or `.modifierExtension`. Message text: `Differential in profile {5} does not have a slice at {6} (on {0}, position {1} of {2} / {3} / {4})`.

### 1.2 The "pre-slice default content" branch (L317-336)

```java
L322  if (diffMatches.size() > 1 && diffMatches.get(0).hasSlicing() && (newBaseLimit > cursors.baseCursor || (diffMatches.size() > 1 ? getDifferential().getElement().indexOf(diffMatches.get(1)) : -1) > (diffMatches.size() > 0 ? getDifferential().getElement().indexOf(diffMatches.get(0)) : -1) + 1)) { // there's a default set before the slices
```

i.e. multiple diff matches, the first carries a slicing entry, AND (the base element has children — `newBaseLimit > baseCursor` — OR there is at least one differential row *between* diffMatches[0] and diffMatches[1], i.e. the intro has inner/default content). Then the intro (plus its inner content) is processed by a **recursion** (L326-331) scoped to `diffMatches[0]`'s diff range and the base element's children, `withSlicing(new PathSlicingParams(true, null, null))` — slicing marked done, no slicer element. The returned element `e`:

```java
L332  if (e == null)
L333    throw new FHIRException(...DID_NOT_FIND_SINGLE_SLICE_, diffMatches.get(0).getPath());
L334  e.setSlicing(diffMatches.get(0).getSlicing());
L335  slicerElement = setPath(e, currentBasePath);
L336  start++;
```

The diff intro's slicing component is stamped **by reference (no `.copy()`)** onto the merged result row; that row becomes `slicerElement`; `start++` marks diffMatches[0] consumed. (Recall §0: `setPath` only nulls the id.) Message: `Did not find single slice: {0}`.

### 1.3 The plain slicer-row branch (L337-425)

Comment (L338): `// we're just going to accept the differential slicing at face value`.

Construction (L339-341): copy `currentBase`, `updateURLs`, `fixedPathDest`, then `profileUtilities.updateFromBase(outcome, currentBase, sourceStructureDefinition.getUrl())` (records base provenance / base min/max component). The differential is **not yet merged** at this point.

#### ANCHOR L343-345 — auto-added slicing ("reslice-without-intro stamping")

```java
L343      if (!diffMatches.get(0).hasSlicing()) {
L344        outcome.setSlicing(profileUtilities.makeExtensionSlicing());
L345        outcome.setUserData(UserDataNames.SNAPSHOT_auto_added_slicing, true);
```

What it does: when the first diff match carries **no** slicing entry, the generator fabricates the standard extension slicing — `makeExtensionSlicing()` (ProfileUtilities L2408-2414) = one discriminator `value:url`, `ordered=false`, `rules=OPEN` — stamps it on the snapshot slicer row, and marks the row with userData key `SNAPSHOT_auto_added_slicing = true` so downstream consumers (e.g. rendering, round-trip trimming) can tell the slicing was synthesized rather than authored.

When it triggers: **only for extension elements.** By elimination: reaching this line with `!diffMatches.get(0).hasSlicing()` requires having survived the L313 guard, which throws for any non-extension base in exactly that condition. So the reachable case is a differential that puts named slices (or re-slices) directly on `.extension`/`.modifierExtension` without an intro row — the classic "extension slicing may be omitted" allowance. (The prior sweep's "reslice-without-intro" label: a diff that dives straight into `extension:foo` without restating the slicing intro lands here and gets the synthetic `url`-discriminator slicer.)

#### Slicing-conflict check for later intros (L347-361)

Else-branch: `outcome.setSlicing(diffMatches.get(0).getSlicing().copy())` (L347 — **copied** here, unlike L334). Then every *subsequent* diff match that also carries a slicing component is compared via `slicingMatches` (L483-491: `ordered`, `rules`, and deep-equal `discriminator` list; an s2 that adds `ordered`/`rules` where s1 had none is a mismatch, but s2 *omitting* what s1 has is tolerated):

- mismatch → ValidationMessage **ERROR** (L351-353), message `ATTEMPT_TO_CHANGE_SLICING` = `The element at {0} defines the slicing {1} but then an element in the slicing {2} tries to redefine the slicing to {3}` with args `(id0, summary0, idI, summaryI)`. Non-throwing: recorded in `profileUtilities.getMessages()`.
- match → ValidationMessage **INFORMATION** (L355-357), same message key but **only 2 args passed** (`id0, idI`) — placeholders {2}/{3} of the 4-placeholder text are left unfilled. *Arity bug; flag for error-taxonomy register.*

Then path sanity (L363-366, throws `ADDING_WRONG_PATH` = `Adding wrong path` if outcome path doesn't start with `resultPathBase`), `addToResult(outcome)` (append to snapshot, L1484-1486), and `slicerElement = setPath(outcome, currentBasePath)` (L369 — id nulled only).

#### Merging the intro's own diff content (L371-424)

Comment (L371): `// differential - if the first one in the list has a name, we'll process it. Else we'll treat it as the base definition of the slice.` — note the comment's wording is inverted relative to the code: the code processes diffMatches[0] into the slicer row when it has **no** sliceName (L372 `if (!diffMatches.get(0).hasSliceName())`), and in that case:

- `updateFromDefinition(outcome, diffMatches.get(0), ...)` (L373) merges the intro row's constraints into the slicer row (last arg `fromSlicer=false`).
- Sanity (L374-376): a non-root outcome with neither contentReference nor type throws `NOT_DONE_YET` (`Not done yet`).
- **Inner diff matches under the intro** (L377-401): if `hasInnerDiffMatches(differential, currentBasePath, diffCursor, diffLimit, base, false)`:
  - base **has** children (L378 `baseHasChildren`) → two hard failures:
    - `diffCursor == 0` → `throw new DefinitionException("Error: The profile has slicing at the root ('"+currentBase.getPath()+"'), which is illegal")` (L380 — literal string, not i18n).
    - otherwise → `throw new Error("This situation is not yet handled (constrain slicing to 1..1 and fix base slice for inline structure - please report issue to grahame@fhir.org along with a test case that reproduces this error (@ " + currentBasePath + " | " + currentBase.getPath() + ")")` (L382 — literal string).
  - base has **no** children → inline datatype recursion (L384-401): resolve the element's type SD via `getTypeForElement`, advance `diffCursor` across all rows starting with `currentBasePath + "."`, and recurse into the type's snapshot (`baseCursor=1`, comment `/* starting again on the data type, but skip the root */`), `withSlicing(new PathSlicingParams())` (fresh, not-done), passing `slicerElement` as the processPaths slicer argument (L400).
- **No inner matches** but base has a contentReference and no children (L403-419): "`we have to dump the content of the redirect type inline here at this point.`" — `resolveContentReference` (L459-473) walks **backwards** from the current element to find the nearest prior element whose path equals the reference target **and is not a slice** (comment L463: `// Important: we must find the base element, not a slice (slices have the same path as their base)`); returns -1 if not found (note: L405 does not check for -1; `findEndOfElementNoSlices(base, -1)` would follow). Each child of the target is copied, id nulled, path rewritten by `fixForRedirect` (L455-457: `path.replace(redirect, rootPath)` — a plain string replace), `updateFromBase`'d, `markExtensions`'d, and appended.
- `start++` (L421) — intro consumed. Commented-out line L422: `// result.getElement().remove(result.getElement().size()-1);`

If the intro **has** a sliceName (i.e. no separate intro row; first match is already a named slice, only possible via the extension allowance above): `profileUtilities.checkExtensionDoco(outcome)` only (L424); `start` stays 0 so diffMatches[0] is processed as a slice in the loop below.

### 1.4 Per-named-slice recursion (L426-444)

```java
L430  for (int i = start; i < diffMatches.size(); i++) {
L432    newDiffCursor = getDifferential().getElement().indexOf(diffMatches.get(i));
L433    newDiffLimit = profileUtilities.findEndOfElement(getDifferential(), newDiffCursor);
...
L438    this.incrementDebugIndent()
L439      .withBaseLimit(newBaseLimit)
L440      .withDiffLimit(newDiffLimit)
L441      .withProfileName(getProfileName() + profileUtilities.pathTail(diffMatches, i))
L442      .withSlicing(new PathSlicingParams(true, slicerElement, null).withDiffs(diffMatches))
L443      .processPaths(ncursors, mapHelper, slicerElement);
```

Each remaining diff match re-processes the **same base scope** (`cursors.baseCursor..newBaseLimit`) against its own diff range. The recursion carries the slicer row twice: as `PathSlicingParams.elementDefinition` (drives the min-reset rules in §3) and as the `processPaths` `slicerElement` argument (drives template selection / max-cap in §3, first element only per §0). `withDiffs(diffMatches)` gives the recursion visibility of the sibling-slice count (`slices` = diffMatches[1..]).

Exit (L446-447): `baseCursor = newBaseLimit + 1; diffCursor = newDiffLimit + 1`.

---

## 2. `processSimplePathWhereDiffsConstrainTypes` (L493-672) — type slicing on an unsliced base

Entry condition — `diffsConstrainTypes` (ProfileUtilities L1806-1849), full semantics: returns true iff diffMatches[0].path or currentBasePath ends with `[x]` and **every** diff match's tail starts with the base's root name (base tail minus `[x]`). Side effect: fills `typeList` with `TypeSlice(ed, type)` entries — type inferred from (a) single `type` entry on a named slice; (b) the path suffix (`valueString` → `string`, incl. constrained-datatype → `baseType`); (c) the sliceName suffix; or (d) `TypeSlice(ed, null)` for an unrenamed `[x]` row without sliceName (the explicit slicing intro case). Rows whose suffix contains `.` (children) are skipped. Note the commented-out size guard at L1807-1808 (`// if (diffMatches.size() < 2) return false;`) — a **single** type-constraining diff row can enter this path.

`shortCut = !typeList.isEmpty() && typeList.get(0).getType() != null` (L498) — true when the first entry is a typed row (the author "dived in" without an explicit `[x]` slicing intro).

### 2.1 Version branch and synthetic slicer injection — ANCHOR L504

```java
L504  if (!VersionUtilities.isR4Plus(profileUtilities.getContext().getVersion()) || !profileUtilities.isNewSlicingProcessing()) { // newSlicingProcessing is a work around for editorial loop dependency
```

- **R3 / unpatched-R4 branch** (L505-516; comment L503: `// in R3 (and unpatched R4, as a workaround right now...`): a synthetic `ElementDefinition` is built with path = `determineTypeSlicePath(path, currentBasePath)` (ProfileUtilities L1788-1793: head of the diff path + tail of the base path), **`ed.addType().setCode(ts.getType())` for every entry in typeList** (i.e. the synthetic slicer is *typed*, constraining the base's types to the listed ones), slicing = one discriminator `type:$this`, `rules=CLOSED`, `ordered=false`.
- **R4+ branch** (L517-529; comments L518-520: `// as of R4, this changed; if there's no slice, there's no constraint on the slice types, only one the type.` / `// so the element we insert specifies no types (= all types) allowed in the base, not just the listed type.` / `// see also discussion here: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Slicing.20a.20non-repeating.20element`): identical construction **without the types loop** — the synthetic slicer is *untyped*, so the slicing entry keeps all base types.

`isNewSlicingProcessing` is a plain caller-configured flag on ProfileUtilities (field L440 `private boolean newSlicingProcessing;`, default **false**; setter L4749). So even on R4+ the *old typed* behavior applies unless the caller opts in — the in-code comment calls the flag "a work around for editorial loop dependency".

**Post-extract correction (verified 2026-08-31, main-session follow-up):** the HL7 test driver DOES opt in — `SnapShotGenerationTests.java:130` parses the manifest attribute as `newSliceProcessing = !"false".equals(test.getAttribute("new-slice-processing"))` (default **true**) and applies it at `:597` (`pu.setNewSlicingProcessing(test.isNewSliceProcessing())`); `:566`/`:697` (register-gen and sort paths) hardcode `true`. Only one R5 manifest test opts out (`dk1`, `new-slice-processing="false"`). So the golden files and the project's sweep oracle reflect the **untyped R4+ branch**, while plain library consumers on the default get the typed branch. No ValidationEngine call site exists in the checked-out sparse cone (worktree grep; the validation module beyond `cli` is not checked out).

In both branches the synthetic element is prepended to `diffMatches` **and physically inserted into the live differential list** at `newDiffCursor` (L514-515 / L527-528), remembered as `elementToRemove`, and removed again at L634-636 (with `newDiffLimit--`). This mutation is visible to all index arithmetic in between.

If **not** shortCut (explicit intro exists), path-tail agreement is enforced (L531-538):

```java
L534  if (!t1.equals(t2)) {
L535    throw new FHIRException(...ED_PATH_WRONG_TYPE_MATCH, path.replace(t2, t1), path);
```

Message: `The path must be ''{0}'' not ''{1}'' when the type list is not constrained`.

### 2.2 Slicing-shape validation (L542-557)

Applied to `diffMatches.get(0).getSlicing()` (synthetic or authored):
- `ordered == true` → throw `ERROR_AT_PATH__IN__TYPE_SLICING_WITH_SLICINGORDERED__TRUE`: `Error at path {0} in {1}: Type slicing with slicing.ordered = true`.
- discriminator count != 1 → `...SLICINGDISCRIMINATORCOUNT__1`: `...Type slicing with slicing.discriminator.count() > 1`.
- discriminator type != TYPE → `...SLICINGDISCRIMINATORTYPE__TYPE`: `...slicing.discriminator.type != ''type''`.
- discriminator path != `$this` → `...SLICINGDISCRIMINATORPATH__THIS`: `...slicing.discriminator.path != ''$this''`.
All checks only run when the corresponding component is present (`hasOrdered`/`hasDiscriminator`).

### 2.3 Slice-name check / auto-fix and type stamping (L558-579)

For every typed `TypeSlice`: canonical name `tn = rootName(currentBasePath) + capitalize(type)` (rootName = tail minus `[x]`, ProfileUtilities L1782-1785; e.g. `valueString` for `value[x]`+`string`).
- no sliceName → set it to `tn` (L562-563).
- wrong sliceName → if `profileUtilities.isAutoFixSliceNames()` set it to `tn` (L565-566), else throw `ERROR_AT_PATH__SLICE_NAME_MUST_BE__BUT_IS_`: `Error at path {0}: Slice name must be ''{1}'' but is ''{2}''` (path arg prefers `contextPathSource` when non-empty).
- no `type` on the slice → add the inferred code (L571-572); more than one type → throw `ERROR_AT_PATH__SLICE_FOR_TYPE__HAS_MORE_THAN_ONE_TYPE_` (`...has more than one type ''{2}''`); single but different type → `ERROR_AT_PATH__SLICE_FOR_TYPE__HAS_WRONG_TYPE_` (`...has wrong type ''{2}''`).

### 2.4 Type-slicing-root recursion and forced slicing shape (L582-604)

The intro (diffMatches[0], synthetic or authored) is processed by recursion (L584-591) over the base element's scope with `PathSlicingParams(true, null, null)`; null result → throw `DID_NOT_FIND_TYPE_ROOT_` (`Did not find type root: {0}`). Then the returned root's slicing is **rebuilt unconditionally** (comment L594: `// now set up slicing on the e (cause it was wiped by what we called.`):

#### ANCHOR L595-598 — "type slicing is always closed"

```java
L595    elementDefinition.setSlicing(new ElementDefinition.ElementDefinitionSlicingComponent());
L596    elementDefinition.getSlicing().addDiscriminator().setType(ElementDefinition.DiscriminatorType.TYPE).setPath("$this");
L597    elementDefinition.getSlicing().setRules(ElementDefinition.SlicingRules.CLOSED); // type slicing is always closed; the differential might call it open, but that just means it's not constraining the slices it doesn't mention
L598    elementDefinition.getSlicing().setOrdered(false);
```

The enforcement is the unconditional `setSlicing(new ...)` + `setRules(CLOSED)`: **whatever the differential's slicing said (rules, ordered, even extra discriminators) is discarded** and replaced by `type:$this / CLOSED / unordered`. **Code-vs-comment mismatch:** the comment says "always closed", but L646-667 (§2.6) can flip the very same slicing back to `OPEN` when allowed types remain unsliced — so the net rule is: CLOSED iff every type still allowed on the entry element has a matching type slice; otherwise OPEN (or, in the `xtension.value` shortcut case, the unsliced types are deleted and it stays CLOSED).

Slicer copy for the per-slice recursions (L599-602):

```java
L599    ElementDefinition slicerElement = elementDefinition.copy();
L600    if (elementDefinition.getType().size() > 1) {
L601      slicerElement.setMin(0);
L602    }
```

(the *copy* handed to slices gets `min=0` when several types remain — so the slicer's min doesn't leak into each single-type slice; the entry element in the snapshot keeps its min).

### 2.5 Per-type-slice recursion, entry-min raise, fixedType (L606-633)

```java
L609    for (int i = start; i < diffMatches.size(); i++) {
```

#### ANCHOR L609-617 — entry-min raise / multiple-slice min conflict

```java
L611      if (diffMatches.get(i).getMin() > 0) {
L612        if (diffMatches.size() > i + 1) {
L613          throw new FHIRException(...INVALID_SLICING__THERE_IS_MORE_THAN_ONE_TYPE_SLICE_AT__BUT_ONE_OF_THEM__HAS_MIN__1_SO_THE_OTHER_SLICES_CANNOT_EXIST, diffMatches.get(i).getPath(), diffMatches.get(i).getSliceName());
L614        } else {
L615          elementDefinition.setMin(1);
L616        }
L617        fixedType = profileUtilities.determineFixedType(diffMatches, fixedType, i);
L618      }
```

Exact semantics: a type slice with `min > 0` is only legal when it is the **last** diff match (`diffMatches.size() > i + 1` throws — message: `Invalid slicing: there is more than one type slice at {0}, but one of them ({1}) has min = 1, so the other slices cannot exist`). When legal, the **slicing entry element's min is raised to literal 1** (not to the slice's min — a slice `min=2` still yields entry `min=1`), and `fixedType` is (re)computed from that slice (`determineFixedType`, ProfileUtilities L1712-1729: the slice's single type code, or the type parsed from sliceName suffix; 0-types-without-parseable-name and >1-types both throw `UNEXPECTED_CONDITION_IN_DIFFERENTIAL_TYPESLICETYPELISTSIZE...` messages). Note the arithmetic quirk: because the throw happens for *any non-last* `min>0` slice, a profile with two `min>0` type slices always errors, but a single `min>0` slice that is not last (i.e. followed by `min=0` slices) *also* errors — the rule is positional, not count-based.

Each slice then recurses (L622-629) over the same base scope with `PathSlicingParams(true, elementDefinition, null).withDiffs(diffMatches)` and `processPaths(..., slicerElement)` — note the slicing params carry the **entry element** while processPaths gets the **min-0 copy**. Post-recursion:

```java
L630      if (typeList.size() > start + 1) {
L631        typeSliceElement.setMin(0);
L632      }
```

`start` is already 1 here (incremented at L604), so this fires only when `typeList.size() > 2`, i.e. **three or more** type-slice rows in the diff (in shortCut mode typeList excludes the synthetic intro; in explicit-intro mode the intro *is* typeList[0] with type null, making the threshold effectively "two or more named type slices"). Looks like an off-by-one relative to the presumed intent "more than one slice ⇒ each slice min 0"; quote is verbatim — spec author should treat the raw guard as normative for Java behavior. It also overwrites whatever min the recursion computed for the slice, *after* the L611-616 legality check.

### 2.6 fixedType narrowing and open/closed adjustment (L638-668)

- **fixedType narrowing** (L638-645): if some slice had `min>0`, all type entries on the slicing entry element whose code != fixedType are removed (iterator remove).
- **Slice coverage check** (L646-667), guarded by `!"0".equals(elementDefinition.getMax())`: compute `allowedTypes = getListOfTypes(elementDefinition)` (set of type codes, ProfileUtilities L1604-1610), remove every type claimed by a `TypeSlice` (by `t.type`, or by the single type of a named slice). If types remain unsliced:

#### ANCHOR L657 — the `xtension.value` special case

```java
L656      if (!allowedTypes.isEmpty()) {
L657        if (currentBasePath.contains("xtension.value") && shortCut) {
L658          for (Iterator<ElementDefinition.TypeRefComponent> iter = elementDefinition.getType().iterator(); iter.hasNext(); ) {
L659            ElementDefinition.TypeRefComponent tr = iter.next();
L660            if (allowedTypes.contains(tr.getCode())) {
L661              iter.remove();
L662            }
L663          }
L664        } else {
L665          elementDefinition.getSlicing().setRules(ElementDefinition.SlicingRules.OPEN);
L666        }
L667      }
```

Alternative behavior explained: normally, unsliced-but-allowed types force the slicing OPEN (contradicting the L597 comment). But when the current base path contains the substring `xtension.value` (matches both `extension.value[x]` and `modifierExtension.value[x]` at any nesting depth — the leading letter is deliberately dropped) AND the shortcut form was used (typed slice rows, no authored intro), the generator instead **deletes the non-sliced types from the slicing entry element** and leaves the slicing CLOSED — i.e. on Extension.value[x], diving straight into `valueString` narrows the entry's type list to the sliced types rather than opening the slicing. No message is emitted in either branch.

Exit (L670-671): `baseCursor = newBaseLimit + 1; diffCursor = newDiffLimit + 1` (the latter using the decremented limit from the synthetic-row removal).

---

## 3. Slicing aspects of `processSimplePathWithOneMatchingElementInDifferential` (L674-938)

Orchestration summary (brief — template selection/merge is another chapter's topic): the method builds a `template` ED for the single diff match — from a reslice base id (L686-691), or from the snapshot root of a single declared type profile (L692-773; includes xver extension handling, snapshot generation of the type profile, `EXT_PROFILE_ELEMENT` element targeting, and root-resource constraint clearing L756-760 with comment about `%resource` sense change). Template errors seen here (taxonomy): `VALIDATION_VAL_ILLEGAL_TYPE_CONSTRAINT` = `Invalid constraint in profile {0} at path {1} - cannot constrain to type {2} from base types {3}` (L681, suppressible via `isSuppressIgnorableExceptions`), `ATTEMPT_TO_USE_A_SNAPSHOT_ON_PROFILE__AS__BEFORE_IT_IS_GENERATED` = `Attempt to use a snapshot on profile ''{0}'' as {1} before it is generated` (L723), `SNAPSHOT_IS_EMPTY` = `The snapshot for the profile ''{0}'' is empty. This is usually due to a processing error logged elsewhere` (L752), `UNABLE_TO_FIND_ELEMENT__IN_` (L747), `UNABLE_TO_FIND_BASE__FOR_` (L728), `VALIDATION_VAL_PROFILE_WRONGTYPE2` (L716), plus literal `"Reference to invalid version in extension url "`/`"Reference to invalid extension "`/`"Reference to unknown extension "` (L703/705/707). Fallback template selection at L775-787 is quoted in §4 (APPLY_PROPERTIES_FROM_SLICER). After template selection: `updateURLs`, `fixedPathDest`, `updateFromBase` (L789-793).

`fromSlicer = slicerElement != null` (L675) is later passed as the final boolean of `updateFromDefinition` (L813).

### 3.1 Slice-name handling and min reset — ANCHOR L794-811

```java
L794    if (diffMatches.get(0).hasSliceName()) {
L795      template = merge(currentBase, slicerElement);
L796      template = profileUtilities.updateURLs(getUrl(), getWebUrl(), template, true);
L797      template.setPath(profileUtilities.fixedPathDest(getContextPathTarget(), template.getPath(), getRedirector(), getContextPathSource()));
L798
L799      checkToSeeIfSlicingExists(diffMatches.get(0), template);
L800      outcome.setSliceName(diffMatches.get(0).getSliceName());
L801      if (!diffMatches.get(0).hasMin()) {
L802        if ((slicing.elementDefinition == null || slicing.elementDefinition.getSlicing().getRules() != ElementDefinition.SlicingRules.CLOSED) && !currentBase.hasSliceName()) {
L803          if (!currentBasePath.endsWith("xtension.value[x]")) { // hack work around for problems with snapshots in official releases
L804            outcome.setMin(0);
L805          }
L806        } else if (slicing.elementDefinition != null && slicing.elementDefinition.getSlicing().getRules() == ElementDefinition.SlicingRules.CLOSED && slicing.slices.size() > 1) {
L807          // we have multiple slices, they share the min cardinality between them
L808          outcome.setMin(0);
L809        }
L810      }
L811    }
```

Semantics, spelled out (applies only when the diff row is a named slice AND does not state its own `min`):

- **Branch 1 (L802-805), non-CLOSED slicing** (no known slicer, or slicer rules != CLOSED) **and the base row is not itself a slice** (`!currentBase.hasSliceName()` — protects reslicing, where the parent slice's min should be inherited): slice min is reset to 0 (the base's min belongs to the entry, not each slice) — **unless** the path ends with `xtension.value[x]` (again letterless, catching `extension.value[x]`/`modifierExtension.value[x]`), where the base min is kept. The in-code justification is verbatim: `// hack work around for problems with snapshots in official releases` — i.e. published core/extension snapshots contain value[x] slices whose min was not reset, and regenerating them with the reset would produce diffs.
- **Branch 2 (L806-809), CLOSED slicing with multiple slices**: when the governing slicer (`slicing.elementDefinition`, from `PathSlicingParams`) is CLOSED and `slicing.slices.size() > 1` (≥ 2 named-slice diff rows besides the intro, per §0), min is also reset to 0, with the verbatim comment `// we have multiple slices, they share the min cardinality between them`. **Consequences of the guard shape:** under CLOSED slicing with exactly **one** named slice, *neither* branch fires — the single slice **inherits the base/entry min** (rationale: closed + only slice ⇒ that slice must absorb the entry's minimum). Also note branch 2 lacks branch 1's `!currentBase.hasSliceName()` protection and its `xtension.value[x]` exclusion — a CLOSED multi-slice reslice would still get min=0.

`checkToSeeIfSlicingExists` (L799) may inject a synthetic slicer row *before* the slice — §5.

### 3.2 Merge, commented-out renaming, max-cap — ANCHOR L812-819

```java
L812    profileUtilities.markExtensions(outcome, false, templateSD);
L813    profileUtilities.updateFromDefinition(outcome, diffMatches.get(0), getProfileName(), isTrimDifferential(), getUrl(), getSourceStructureDefinition(), getDerived(), diffPath(diffMatches.get(0)), mapHelper, fromSlicer);
L814  //          if (outcome.getPath().endsWith("[x]") && outcome.getType().size() == 1 && !outcome.getType().get(0).getCode().equals("*") && !diffMatches.get(0).hasSlicing()) // if the base profile allows multiple types, but the profile only allows one, rename it
L815  //            outcome.setPath(outcome.getPath().substring(0, outcome.getPath().length()-3)+Utilities.capitalize(outcome.getType().get(0).getCode()));
L816    if (!APPLY_PROPERTIES_FROM_SLICER && slicerElement != null && outcome.getMaxAsInt() > slicerElement.getMaxAsInt()) {
L817      outcome.setMaxElement(slicerElement.getMaxElement());
L818    }
L819    outcome.setSlicing(null);
```

- L814-815 (commented out): the historical "single-type [x] rename" (`value[x]` → `valueString` when constrained to one type) is **disabled** in this code path — snapshot rows keep the `[x]` path.
- L816-818 **slice max-cap**: active because `APPLY_PROPERTIES_FROM_SLICER` is false (§4). When processing a slice under a known slicer, if the merged slice's max exceeds the slicer's max, it is **capped to the slicer's max element** (the actual StringType is copied, preserving `*`). `getMaxAsInt` (ElementDefinition.java L13160-13162) maps `"*"` to `Integer.MAX_VALUE`, so: slice `*` under slicer `3` → capped to `3`; slice `5` under slicer `*` → untouched; both `*` → untouched (equal). No message is emitted — silent narrowing.
- L819: a named-or-not single-match row never keeps a slicing component here (`setSlicing(null)`); slicing intros for this element are handled by other paths (§1) or injected by `checkToSeeIfSlicingExists`.

The rest of the method (L820-936) appends the row, advances cursors, and — when the diff walks into children of a datatype/resource/contentReference (L828+) — recurses into the type snapshot or contentReference target; multi-type + children throws `_HAS_CHILDREN__AND_MULTIPLE_TYPES__IN_PROFILE_` unless all extra diff rows are `.extension` (L843-853) or all types share one code (L900-909, falling back to the `Element` profile when codes differ); missing type SD throws `_HAS_CHILDREN__FOR_TYPE__IN_PROFILE__BUT_CANT_FIND_TYPE` (L921); unresolvable contentReference throws `UNABLE_TO_RESOLVE_REFERENCE_TO_` (L861). Path-ordering violation throws `ADDING_WRONG_PATH` (L823). (Orchestration detail beyond slicing left to the merge chapter.)

---

## 4. `APPLY_PROPERTIES_FROM_SLICER` (L42-58)

Full comment, verbatim:

```java
  /**
   * The code contains a fix for an issue that was reported where slices that are being defined don't inherit 
   * properties from the slice. This is a reference to this text in the profiling documentation:
   * 
   * > That is, the first element in a slice group must contain a slicing property that defines the discriminator 
   * > for all members of the group. It also contains the unconstrained definition of the element that is sliced, 
   * > potentially including children of the unconstrained element, if there are any
   * 
   * APPLY_PROPERTIES_FROM_SLICER = true will mean that this code works that way (though there is some interpretational questions 
   * around the meaning of this text)
   * 
   * But the community decided not to apply this fix in practice, and to change(/clarify) the text above, so the 
   * fix is not enabled. See discussion at 
   * https://chat.fhir.org/#narrow/channel/179252-IG-creation/topic/Slices.20not.20inheriting.20preferred.20bindings.20from.20root
   * 
   */
  private static final boolean APPLY_PROPERTIES_FROM_SLICER = false;
```

So: slices deliberately do **not** inherit properties (e.g. preferred bindings) from the slicing-intro element; the community decision (Zulip thread above) was to clarify the spec text instead. Every guard site, with the effective (false) behavior:

1. **L776-786, template fallback** (§3):
   ```java
   L776  if (!APPLY_PROPERTIES_FROM_SLICER || slicerElement == null || currentBase.hasContentReference()) {
   L777    template = currentBase.copy();
   ...
   L780    template = slicerElement.copy();   // dead branch
   ...
   L786    template = profileUtilities.fillOutFromBase(template, APPLY_PROPERTIES_FROM_SLICER && slicerElement != null ? slicerElement : currentBase);
   ```
   Effective: template is always `currentBase.copy()` (L777) when no type-profile/reslice template was found, and `fillOutFromBase` always fills from `currentBase`, never from the slicer. (L764 in the Extension/Resource type-profile branch also routes through `merge(src, slicerElement)` — see 4.)
2. **L816** (§3.2): the slice max-cap to the slicer's max runs **only because** the constant is false (`!APPLY_PROPERTIES_FROM_SLICER && ...`). Were the fix enabled, the cap would be redundant (the max would come from the slicer template).
3. **L941, `merge`** (§5): with the constant false, `merge(src, slicer)` ignores the slicer entirely.
4. **L764** (template from type profile when base is Extension/Resource): `template = merge(src, slicerElement).setPath(currentBase.getPath());` — again effectively `src.copy()`.

---

## 5. `merge`, `checkToSeeIfSlicingExists`, `isJumpingIntoTypeSlicing`, `pathsMatch`

### `merge(src, slicer)` (L940-953)

```java
L940  private ElementDefinition merge(ElementDefinition src, ElementDefinition slicer) {
L941    if (slicer != null && APPLY_PROPERTIES_FROM_SLICER) {
L942      ElementDefinition res = slicer.copy();
L943      if (src.getMin() > res.getMin()) {
L944        res.setMinElement(src.getMinElement().copy());
L945      }
L946      if (src.getMaxAsInt() < res.getMaxAsInt()) {
L947        res.setMaxElement(src.getMaxElement());
L948      }
L949      return res;
L950    } else {
L951      return src.copy();
L952    }
L953  }
```

Effective behavior (constant false): **always `src.copy()`**. The dead branch documents the intended "inherit from slicer" merge: start from the slicer, then take the stricter min (larger) and stricter max (smaller, `*`=MAX_VALUE) from src.

### `checkToSeeIfSlicingExists(ed, template)` (L955-987)

Called only from L799, when a **named slice** diff row is being placed. It scans the result snapshot **backwards from the end** (L957-972) looking for an element whose path `pathsMatch`es the slice's path and which is either a slicing intro (`hasSlicing()`), another slice (`hasSliceName()`), or a `[x]` row (`getPath().endsWith("[x]")`); the scan aborts early when it walks past an element with a **shorter** path (L968-970 — i.e. it only looks within the current parent's tail of the snapshot).

When **no** such anchor exists (`m == null`) — i.e. a named slice is arriving with no slicer row in the result — a synthetic slicer may be **appended** (via `addToResult(template)`, L1484-1486 — plain `add`, so it lands immediately before the slice row that will be added right after; "inserted" = end of current result):

- **Extension url-discriminator slicer** (L974-978), when the template's path ends with `.extension`:
  ```java
  L975    template.getSlicing().setRules(SlicingRules.OPEN);
  L976    template.getSlicing().setOrdered(false);
  L977    template.getSlicing().addDiscriminator().setType(DiscriminatorType.VALUE).setPath("url");
  L978    addToResult(template);
  ```
- **Type-slicing `$this` slicer** (L979-983), when `isJumpingIntoTypeSlicing(ed, template)`:
  ```java
  L980    template.getSlicing().setRules(SlicingRules.CLOSED);
  L981    template.getSlicing().setOrdered(false);
  L982    template.getSlicing().addDiscriminator().setType(DiscriminatorType.TYPE).setPath("$this");
  L983    addToResult(template);
  ```
  (followed by the commented-out `// log.error("checkToSeeIfSlicingExists: "+ed.getPath()+":"+ed.getSliceName()+" is not sliced");` at L984.)

**Silent third outcome:** if `m == null` but the path is neither `.extension` nor a `[x]` type-slice jump, **nothing is injected and nothing is reported** — the named slice is emitted without any slicer row in the snapshot. Note also: the injected `template` is the `merge(currentBase, slicerElement)` copy from L795 — it keeps the base's cardinality/content and (in the extension case) is OPEN; in the type case CLOSED. Contrast with §2's L597/L665: the *jumped-into* type slicing is stamped CLOSED here with no open/closed coverage adjustment.

### `isJumpingIntoTypeSlicing` (L989-991)

```java
L989  private boolean isJumpingIntoTypeSlicing(ElementDefinition ed, ElementDefinition template) {
L990    return template.getPath().endsWith("[x]") && !template.hasSliceName() && !template.hasSlicing() && ed.hasSliceName() && ed.getType().size() > 0;
L991  }
```

i.e. the base row is an unsliced, un-named `[x]` element and the diff row is a named slice that states at least one type.

### `pathsMatch(path1, path2)` (L993-1022) — the `[x]`-tolerance rules

Segment-wise comparison after splitting on `.`; **lengths must be equal** (L1000-1002). Per segment: exact equality, OR — if one side's segment ends with `[x]` — the other side's segment must **start with** the `[x]` side's stem (segment minus the trailing `[x]`, L1007-1014). So `value[x]` matches `valueString`, `valueQuantity`, but also (prefix-only, no type validation) any `value...` segment. Tolerance is symmetric (checked in both directions); two literal unequal segments without `[x]` fail.

---

## 6. `oneMatchingElementInDifferential` (L1723-1736) — dispatch predicate, verbatim

```java
L1723  private boolean oneMatchingElementInDifferential(boolean slicingDone, String path, List<ElementDefinition> diffMatches) {
L1724    if (diffMatches.size() != 1) {
L1725      return false;
L1726    }
L1727    if (slicingDone) {
L1728      return true;
L1729    }
L1730    if (profileUtilities.isImplicitSlicing(diffMatches.get(0), path)) {
L1731      return false;
L1732    }
L1733    return !(diffMatches.get(0).hasSlicing()
L1734      || (profileUtilities.isExtension(diffMatches.get(0))
L1735      && diffMatches.get(0).hasSliceName()));
L1736  }
```

Reading: exactly one diff match, AND (we are already inside slice processing — `getSlicing().isDone()`, in which case a slicing intro / named extension slice is still "one match"), OR (not implicitly slicing — `isImplicitSlicing` (ProfileUtilities L1796-1803): diff path differs from base path, base path ends `[x]`, diff path starts with the stem, i.e. a lone renamed `valueString` row counts as slicing, not a simple match — AND the row neither carries a slicing component nor is a named `.extension`/`.modifierExtension` slice). Rows failing this predicate fall through to the type-slicing test and then to `processSimplePathDefault`.

Note `isExtension` here is applied to the **diff** element (path-suffix test), unlike L313 where it is applied to the base.

---

## Error/exception register (this scope)

| Site | Kind | Key / literal | Text (verbatim from Messages.properties @ b06c7ee) |
|---|---|---|---|
| L312 | DefinitionException | ATTEMPT_TO_A_SLICE_AN_ELEMENT_THAT_DOES_NOT_REPEAT__FROM__IN_ | `Attempt to a slice an element that does not repeat: {0}/{1} from {2} in {3}, at element {4} (slice = {5})` |
| L314 | DefinitionException | DIFFERENTIAL_DOES_NOT_HAVE_A_SLICE__B_OF_____IN_PROFILE_ | `Differential in profile {5} does not have a slice at {6} (on {0}, position {1} of {2} / {3} / {4})` |
| L333 | FHIRException | DID_NOT_FIND_SINGLE_SLICE_ | `Did not find single slice: {0}` |
| L351-353 | ValidationMessage ERROR (non-throwing) | ATTEMPT_TO_CHANGE_SLICING | `The element at {0} defines the slicing {1} but then an element in the slicing {2} tries to redefine the slicing to {3}` |
| L355-357 | ValidationMessage INFORMATION (non-throwing) | ATTEMPT_TO_CHANGE_SLICING | same key, **only 2 args passed — {2}/{3} unfilled (arity bug)** |
| L365, L823 | DefinitionException | ADDING_WRONG_PATH | `Adding wrong path` |
| L375 | DefinitionException | NOT_DONE_YET | `Not done yet` |
| L380 | DefinitionException | (literal) | `Error: The profile has slicing at the root ('<path>'), which is illegal` |
| L382 | java.lang.Error | (literal) | `This situation is not yet handled (constrain slicing to 1..1 and fix base slice for inline structure - please report issue to grahame@fhir.org along with a test case that reproduces this error (@ <basePath> | <path>)` |
| L535 | FHIRException | ED_PATH_WRONG_TYPE_MATCH | `The path must be ''{0}'' not ''{1}'' when the type list is not constrained` |
| L544 | FHIRException | ERROR_AT_PATH__IN__TYPE_SLICING_WITH_SLICINGORDERED__TRUE | `Error at path {0} in {1}: Type slicing with slicing.ordered = true` |
| L549 | FHIRException | ...SLICINGDISCRIMINATORCOUNT__1 | `Error at path {0} in {1}: Type slicing with slicing.discriminator.count() > 1` |
| L552 | FHIRException | ...SLICINGDISCRIMINATORTYPE__TYPE | `Error at path {0} in {1}: Type slicing with slicing.discriminator.type != ''type''` |
| L555 | FHIRException | ...SLICINGDISCRIMINATORPATH__THIS | `Error at path {0} in {1}: Type slicing with slicing.discriminator.path != ''$this''` |
| L568 | FHIRException | ERROR_AT_PATH__SLICE_NAME_MUST_BE__BUT_IS_ | `Error at path {0}: Slice name must be ''{1}'' but is ''{2}''` |
| L574 | FHIRException | ERROR_AT_PATH__SLICE_FOR_TYPE__HAS_MORE_THAN_ONE_TYPE_ | `Error at path {0}: Slice for type ''{1}'' has more than one type ''{2}''` |
| L576 | FHIRException | ERROR_AT_PATH__SLICE_FOR_TYPE__HAS_WRONG_TYPE_ | `Error at path {0}: Slice for type ''{1}'' has wrong type ''{2}''` |
| L593 | FHIRException | DID_NOT_FIND_TYPE_ROOT_ | `Did not find type root: {0}` |
| L613 | FHIRException | INVALID_SLICING__THERE_IS_MORE_THAN_ONE_TYPE_SLICE_AT__... | `Invalid slicing: there is more than one type slice at {0}, but one of them ({1}) has min = 1, so the other slices cannot exist` |
| L681 | DefinitionException (suppressible) | VALIDATION_VAL_ILLEGAL_TYPE_CONSTRAINT | `Invalid constraint in profile {0} at path {1} - cannot constrain to type {2} from base types {3}` |
| L723 | FHIRException | ATTEMPT_TO_USE_A_SNAPSHOT_ON_PROFILE__AS__BEFORE_IT_IS_GENERATED | `Attempt to use a snapshot on profile ''{0}'' as {1} before it is generated` |
| L752 | FHIRException | SNAPSHOT_IS_EMPTY | `The snapshot for the profile ''{0}'' is empty. This is usually due to a processing error logged elsewhere` |

(Template-selection errors L703/705/707/716/728/747, and the children-recursion errors L851/861/921 noted in §3, belong to other chapters' registers but are listed there with locations.)

## Code-vs-comment mismatches (summary)

1. **L597 vs L665**: comment "type slicing is always closed" — code reopens the slicing (OPEN) when allowed types remain unsliced, except in the `xtension.value` shortcut case, which stays CLOSED by pruning types.
2. **`setPath` (L450-453)**: name/parameter claim a path assignment; the code only nulls the id.
3. **L371 comment** "if the first one in the list has a name, we'll process it. Else we'll treat it as the base definition of the slice" — inverted: the code merges diffMatches[0] into the slicer row precisely when it has **no** sliceName.
4. **L355-357**: INFORMATION message uses a 4-placeholder text with 2 args.
5. **L630-632**: comment-free `typeList.size() > start + 1` guard behaves as "≥3 typeList rows", likely intended as "≥2 slices"; verbatim guard is normative for Java behavior.

## Parked findings (getDiffMatches / diff-side matching — other packet's scope)

- `getDiffMatches` is invoked at L204 with `(differential, currentBasePath, diffCursor, diffLimit, profileName)`; its matching rules (incl. `[x]` rename tolerance feeding `diffsConstrainTypes` and `oneMatchingElementInDifferential`) were not read.
- `processPaths` L216-228 carries a dated comment (GDG 28-July-2025) about force-advancing `diffCursor` by `diffMatches.size()` when a code path failed to consume matches (`if (dc == cursors.diffCursor) { cursors.diffCursor = cursors.diffCursor + diffMatches.size(); }`) — added for the `sd-nested-ext` test case; the comment admits the per-path cursor discipline is unresolved. Interacts with any diff-matching spec.
- `hasInnerDiffMatches` (ProfileUtilities L2420-2441) has an unexplained fall-through arm: `// not sure why we get here, but returning false at this point makes a bunch of tests fail` (L2438) — relevant to diff-scope matching semantics.
- `checkDiffAssignedAndCursor` (L246-263) is fully commented out — a disabled invariant check that every diff element before the cursor has been "assigned" (`UD_DERIVATION_POINTER`).
- `diffsConstrainTypes` (ProfileUtilities L1806-1849) has its `diffMatches.size() < 2` guard commented out (L1807-1808), so a single renamed/typed row can route to the type-slicing path; the interplay with `getDiffMatches` grouping is diff-side scope.
- `resolveContentReference` (L459-473) returns -1 when the target path is not found; the caller (L405) does not check for -1 before `findEndOfElementNoSlices(cursors.base, bstart)`.
