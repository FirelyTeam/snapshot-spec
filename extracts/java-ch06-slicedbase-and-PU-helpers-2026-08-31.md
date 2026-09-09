# Java deep-read: ProfilePathProcessor "sliced base" machinery + ProfileUtilities slicing helpers

**Extract date:** 2026-08-31
**Code read:** `org.hl7.fhir.core` sparse clone at commit `b06c7ee7c57367305ff72b6bb09ce779b5dbd6ce`.
**Files:**
- `org.hl7.fhir.r5/src/main/java/org/hl7/fhir/r5/conformance/profile/ProfilePathProcessor.java` (PPP, 1739 lines)
- `org.hl7.fhir.r5/src/main/java/org/hl7/fhir/r5/conformance/profile/ProfileUtilities.java` (PU)
- `org.hl7.fhir.r5/src/main/java/org/hl7/fhir/r5/conformance/profile/BaseTypeSlice.java`
- i18n message texts resolved via `org.hl7.fhir.utilities/src/main/resources/Messages.properties` and `.../i18n/I18nConstants.java` (read from git object store; not checked out in the sparse worktree).

**Caveat:** the project's empirical sweep oracle was validator_cli 6.10.2 @ `d06577dbc5c6` — a *different commit* than this code read (`b06c7ee`). Any claim that ties this code to sweep observations carries the commit-pair caveat.

Conventions: "diff" = differential of the derived profile; "base" = the (already generated) snapshot of the base profile. Line refs are `PPP:<n>` / `PU:<n>`. Quotes are verbatim from the source.

---

## PART A — ProfilePathProcessor sliced-base machinery

The sliced-base path is entered from the main `processPaths` loop when the current base element `hasSlicing()` (dispatch not in this packet's scope). All four methods below assume the base snapshot already contains a slicing entry (the "slicer") and zero or more named slices at `currentBasePath`.

### A.1 `processPathWithSlicedBase` (PPP:1196-1223) — dispatcher

Signature (PPP:1196-1201):

```java
private void processPathWithSlicedBase(
    ElementDefinition currentBase,
    String currentBasePath,
    List<ElementDefinition> diffMatches, List<TypeSlice> typeList,
    final ProfilePathProcessorState cursors, MappingAssistant mapHelper)
```

The "rules of reslicing" comment block, verbatim (PPP:1202-1207):

```java
    // the item is already sliced in the base profile.
    // here's the rules
    //  1. irrespective of whether the slicing is ordered or not, the definition order must be maintained
    //  2. slice element names have to match.
    //  3. new slices must be introduced at the end
    // corallory: you can't re-slice existing slices. is that ok?
```

(Note: "corallory" [sic] — the comment CLAIMS reslicing of existing slices is unsupported; the code in A.2 nevertheless contains a reslice-template lookup at PPP:1377-1384, so the claim is partially stale — see below.)

Dispatch (PPP:1212-1222), in order:
1. `diffMatches.isEmpty()` → `processPathWithSlicedBaseAndEmptyDiffMatches` (A.4).
2. `profileUtilities.diffsConstrainTypes(diffMatches, currentBasePath, typeList)` → `processPathWithSlicedBaseWhereDiffsConstrainTypes` (A.3). (`diffsConstrainTypes` semantics: Part B.1; it also fills `typeList` as a side effect.)
3. otherwise → `processPathWithSlicedBaseDefault` (A.2).

### A.2 `processPathWithSlicedBaseDefault` (PPP:1225-1482) — core sliced-base merge

Execution order:

#### A.2.1 Slicing-agreement validation (PPP:1226-1240)

`closed` is computed first (PPP:1227): `boolean closed = currentBase.getSlicing().getRules() == ElementDefinition.SlicingRules.CLOSED;` — note this is the **base** slicer's rules, before any diff merge.

The checks run **only if `diffMatches.get(0).hasSlicing()`** (PPP:1229, comment: `// it might be null if the differential doesn't want to say anything about slicing`). A commented-out fragment immediately follows (PPP:1230-1231), verbatim:

```java
//            if (!isExtension)
//              diffpos++; // if there's a slice on the first, we'll ignore any content it has
```

Three checks, in order, each throwing `DefinitionException`:

1. **Order** (PPP:1234-1235) — only when **both** dSlice and bSlice `hasOrderedElement()`; then `!orderMatches(dSlice.getOrderedElement(), bSlice.getOrderedElement())` throws. Message (`Slicing_rules_on_differential__do_not_match_those_on_base___order___`):
   > `Slicing rules on differential ({0}) do not match those on base ({1}) - order @ {2} ({3})`
   with `{0}`=summarizeSlicing(dSlice), `{1}`=summarizeSlicing(bSlice), `{2}`=path, `{3}`=cursors.contextName.
2. **Discriminator** (PPP:1236-1237) — unconditional: `!discriminatorMatches(dSlice.getDiscriminator(), bSlice.getDiscriminator())` throws. Message (`..._disciminator___` [sic]):
   > `Slicing rules on differential ({0}) do not match those on base ({1}) - discriminator @ {2} ({3})`
   (`{3}`=url here, not contextName.)
3. **Rules** (PPP:1238-1239) — **skipped for choice elements**: `if (!currentBase.isChoice() && !ruleMatches(dSlice.getRules(), bSlice.getRules()))` throws. Message:
   > `Slicing rules on differential ({0}) do not match those on base ({1}) - rule @ {2} ({3})`
   (`{3}`=cursors.contextName.)

Exact tolerances of `orderMatches`/`discriminatorMatches`/`ruleMatches`: Part B.3.

#### A.2.2 Writing the slicer row (PPP:1241-1259)

```java
ElementDefinition outcome = profileUtilities.updateURLs(getUrl(), getWebUrl(), currentBase.copy(), true);
outcome.setPath(profileUtilities.fixedPathDest(...));
profileUtilities.updateFromBase(outcome, currentBase, getSourceStructureDefinition().getUrl());
```

So the slicer row starts as a **copy of the base slicer** (its slicing component included), with `ElementDefinition.base` set per `updateFromBase` (Part B.6). Then (PPP:1244-1251), verbatim:

```java
    if (diffMatches.get(0).hasSlicing() || !diffMatches.get(0).hasSliceName()) {
      profileUtilities.updateFromSlicing(outcome.getSlicing(), diffMatches.get(0).getSlicing());
      profileUtilities.updateFromDefinition(outcome, diffMatches.get(0), getProfileName(), closed, getUrl(), getSourceStructureDefinition(), getDerived(), diffPath(diffMatches.get(0)), mapHelper, false); // if there's no slice, we don't want to update the unsliced description
    } else if (!diffMatches.get(0).hasSliceName()) {
      diffMatches.get(0).setUserData(UserDataNames.SNAPSHOT_GENERATED_IN_SNAPSHOT, outcome); // because of updateFromDefinition isn't called
    } else {
      outcome.setUserData(UserDataNames.SNAPSHOT_auto_added_slicing, true);
    }
```

- Branch 1: if the first diff match carries a slicing component OR is an unnamed element (no sliceName), the diff's slicing is merged onto the copied base slicing via `updateFromSlicing` (per-field merge, Part B.4), **then** `updateFromDefinition` merges the rest of diff[0]'s content onto the slicer row (with `trimDifferential`=`closed`). Note: if diff[0] has no slicing component at all, `getSlicing()` auto-instantiates an empty component and `updateFromSlicing` is a no-op.
- Branch 2 (PPP:1247-1248) is **verified-unreachable dead code**: any element with `!hasSliceName()` already satisfies branch 1's condition, so reaching the else-if implies `hasSliceName()` = true, making its condition false. The `SNAPSHOT_GENERATED_IN_SNAPSHOT` bookkeeping it claims to do never happens here.
- Branch 3 (PPP:1250): reached exactly when diff[0] **has a sliceName and no slicing** — i.e. the differential dives straight into named slices without restating the slicing entry. The copied base slicer row is then flagged `SNAPSHOT_auto_added_slicing`. **Consequence (headline):** the min-sum gate in `generateSnapshot` (PU:998-999, section B.5) *silently overwrites* such a slicer's `min` with the sum of the slice mins instead of raising a message. So restating vs. not restating the slicing entry in the differential changes whether a min-sum discrepancy is an error or a silent fix-up.

Then `markExtensions`, `debugCheck`, `addToResult(outcome)` (PPP:1253-1255) — the slicer row is emitted. `diffpos` starts at 0; if diff[0] is the unnamed slicing entry, it is consumed: PPP:1257-1259:

```java
    if (!diffMatches.get(0).hasSliceName()) { // it's not real content, just the slice
      diffpos++;
    }
```

#### A.2.3 Inner-diff walk-in under the slicer (PPP:1260-1295)

Condition: `profileUtilities.hasInnerDiffMatches(getDifferential(), currentBasePath, cursors.diffCursor, getDiffLimit(), cursors.base.getElement(), false)` — note **`allowSlices=false`** here (contrast A.4 which passes `true`); with `false`, a diff element at the same path *with a sliceName* at the scan start makes it return false (PU:2427-2428), so named slices do NOT count as "inner" content in this check.

If there are inner diff matches (children of the slicer path in the diff before any slice):
- `newBaseLimit = findEndOfElement(base, baseCursor)`; `newDiffCursor = index of diff[0] (+1 if diff[0] hasSlicing)`; `newDiffLimit = findEndOfElement(differential, ndx)`.
- **If `newBaseLimit == cursors.baseCursor`** (base has no children under the slicer — implicit type walk):
  - Base element must have exactly one type or (PPP:1266-1267) `throw new Error(...)`:
    > `Differential walks into ''{0} (@ {1})'', but the base does not, and there is not a single fixed type. The type is {2}. This is not handled yet`
  - `getProfileForDataType(type[0], ...)`; null → `DefinitionException`: > `Unknown type {0} at {1}`
  - diffCursor advanced past all `currentBasePath.`-children; recursion into the datatype snapshot (PPP:1276-1284): base = `dt.getSnapshot()` starting at index 1, baseLimit = last element, diffLimit = newDiffLimit, contextPathSource = currentBasePath, contextPathTarget = outcome.getPath(), `new PathSlicingParams()` (i.e. slicing reset). (Indentation bug in source: PPP:1276-1284 are indented as if inside the while at 1274, but there are no braces — the while at 1274-1275 only advances diffCursor; the recursion runs once. Code DOES one recursion; layout SUGGESTS otherwise.)
- **Else** (base walks into children): recursion over the base children scope (PPP:1286-1294): same base, `baseCursor + 1` .. `newBaseLimit`, diff scope `newDiffCursor` .. `newDiffLimit`, profileName + pathTail(diff[0]), `withRedirector(null)`, `new PathSlicingParams()`.
- After PPP:1295 a commented-out remnant (PPP:1296-1297), verbatim:

```java
//            throw new Error("Not done yet");
//          } else if (currentBase.getType().get(0).getCode().equals("BackboneElement") && diffMatches.size() > 0 && diffMatches.get(0).hasSliceName()) {
```

#### A.2.4 BackboneElement children copy (PPP:1298-1308)

Only when there were NO inner diff matches and `!currentBase.getType().isEmpty() && currentBase.getType().get(0).getCode().equals("BackboneElement")`: every base element from `baseCursor+1` to `findEndOfElement(base, baseCursor)` is copied through (updateURLs + fixedPathDest + markExtensions + addToResult). Comment (PPP:1299): `// We need to copy children of the backbone element before we start messing around with slices`. Note: plain copy, **no `updateFromBase`** — these child rows keep whatever `.base` they had in the base snapshot; no diff merging.

#### A.2.5 Walk over base slices, matching diff slices by sliceName (PPP:1310-1362)

```java
List<ElementDefinition> baseMatches = profileUtilities.getSiblings(cursors.base.getElement(), currentBase);
```

`getSiblings` (Part B.7) returns the named base slices (same path, or [x]-compatible path). For each `baseItem`, in base order:

- `cursors.baseCursor` is repositioned to the baseItem; a candidate `outcome` is built: `baseItem.copy()` + `updateFromBase(outcome, currentBase, ...)` (**base component taken from the slicer `currentBase`, not from the baseItem itself** — PPP:1315) + `outcome.setSlicing(null)` (PPP:1317 — any slicing component ON the base slice row is stripped, i.e. reslicing structure present on a base slice does not survive the copy path) + path-sanity check (`Adding wrong path` DefinitionException).
- **Match test** (PPP:1321): `diffpos < diffMatches.size() && diffMatches.get(diffpos).hasSliceName() && diffMatches.get(diffpos).getSliceName().equals(outcome.getSliceName())`. This is a strictly-in-order, name-equality match — a diff slice can only match while it is the *current* diffpos entry, which enforces "rules" 1-2 of the comment block.
- **Match branch** (PPP:1322-1341): the candidate `outcome` is discarded (never added); instead the base-slice scope (`baseCursor` .. `findEndOfElement(base, baseCursor)`) is recursively processed against the diff-slice scope (`indexOf(diffMatches[diffpos])` .. `findEndOfElement`). Commented-out line kept verbatim (PPP:1323): `// no? updateFromDefinition(outcome, diffMatches.get(diffpos), profileName, closed, url);`. The recursion parameters (PPP:1330-1337) include:

```java
            .withTrimDifferential(closed)
            .withSlicing(new PathSlicingParams(true, null, null))
```

  — **`trimDifferential` is set to `closed`**: when the base slicing is closed, the recursive merge of a matched slice runs in "trim differential" mode (PPP:1335). After the recursion: `baseCursor = newBaseLimit; diffCursor = newDiffLimit + 1; diffpos++`.
- **No-match branch** (PPP:1342-1361): the candidate outcome IS emitted (copy-through of the base slice, with slicing stripped and `.base` from the slicer), then all its children are copied raw (loop PPP:1347-1358; each child gets `SNAPSHOT_BASE_PATH` = its own `outcome.getPath()` and `SNAPSHOT_BASE_MODEL` = source SD url, PPP:1352-1353, but **no `updateFromBase`**, so `.base` components are whatever the base snapshot had). Then verbatim (PPP:1359-1360):

```java
        //Lloyd - add this for test T15
        cursors.baseCursor--;
```

  (The decrement compensates for the child-copy loop overshooting by one; the outer `for` will re-index from the next baseItem anyway — `baseCursor` is re-assigned at PPP:1313 each iteration, so this only matters for the final iteration's exit state, combined with `cursors.baseCursor++` at PPP:1481.)

Because matching is in-order at a single `diffpos`, a diff that lists existing slice names in a different order than the base falls through to the "new slices" stage and is caught there (next section) as "out of order".

#### A.2.6 Closed-slicing enforcement + [x] exemption (PPP:1363-1369)

Verbatim:

```java
    // finally, we process any remaining entries in diff, which are new (and which are only allowed if the base wasn't closed
    if (closed && diffpos < diffMatches.size()) {
      // this is a problem, unless we're on a polymorhpic type and we're going to constrain a slice that actually implicitly exists
      if (!currentBase.getPath().endsWith("[x]")) {
        throw new DefinitionException(profileUtilities.getContext().formatMessage(I18nConstants.THE_BASE_SNAPSHOT_MARKS_A_SLICING_AS_CLOSED_BUT_THE_DIFFERENTIAL_TRIES_TO_EXTEND_IT_IN__AT__, getProfileName(), path, currentBasePath));
      }
    }
```

Message text: > `The base snapshot marks a slicing as closed, but the differential tries to extend it in {0} at {1} ({2})` — `{0}`=profileName, `{1}`=path, `{2}`=currentBasePath. The `[x]` exemption: if the sliced element is a choice (`path.endsWith("[x]")`), new "slices" are allowed even against a closed base slicing (they may be implicit type slices). Note `closed` is the base's rules, unaffected by any diff `updateFromSlicing` (which ran on the outcome copy, and after `closed` was computed anyway).

#### A.2.7 Appending new slices (PPP:1370-1480)

For each remaining diff match (each MUST have a sliceName by now — unnamed diff[0] was consumed at diffpos 0):

1. **Out-of-order guard** (PPP:1373-1375): if any baseMatch has the same sliceName → `DefinitionException`: > `Named items are out of order in the slice`
2. **Template selection / reslice lookup** (PPP:1377-1384), verbatim:

```java
        String id = diffItem.getId();
        String lid = profileUtilities.tail(id);
        ElementDefinition template = currentBase;
        if (lid.contains("/")) {
          profileUtilities.generateIds(getResult().getElement(), getUrl(), getSourceStructureDefinition().getType(), getSourceStructureDefinition());
          String baseId = id.substring(0, id.length() - lid.length()) + lid.substring(0, lid.indexOf("/")); // this is wrong if there's more than one reslice (todo: one thing at a time)
          template = profileUtilities.getById(getResult().getElement(), baseId);
        }
```

   Default template is the **slicer** `currentBase`. But if the diff element's id tail contains `/` (reslice notation `slice/reslice`), ids are (re)generated over the result-so-far and the parent slice row already emitted into the result is looked up by id and used as template — i.e. a reslice's new row is cloned from the *derived* parent slice, not from the base slicer. The quoted comment flags the multi-level-reslice limitation. (This machinery contradicts the A.1 comment's "you can't re-slice existing slices".)
3. **Row construction** (PPP:1385-1396): `template.copy()` → updateURLs → fixedPathDest → `updateFromBase(outcome, currentBase, ...)` → `outcome.setSlicing(null)` → **`outcome.setMin(0)`** with verbatim comment (PPP:1390):

```java
        outcome.setMin(0); // we're in a slice, so it's only a mandatory if it's explicitly marked so
```

   → path check (`Adding wrong path`) → markExtensions → **addToResult** → `updateFromDefinition(outcome, diffItem, getProfileName(), isTrimDifferential(), ...)` — note trim flag here is the processor's own `isTrimDifferential()`, NOT the local `closed`.
4. **Type-profile min/max pickup** (PPP:1398-1420), verbatim guards:

```java
        // do we need to pick up constraints from the type?
        List<String> profiles = new ArrayList<>();
        for (TypeRefComponent tr : outcome.getType()) {
          for (CanonicalType ct : tr.getProfile()) {
            profiles.add(ct.getValueAsString());
          }
        }
        if (profiles.size() == 1) {
          StructureDefinition sdt = profileUtilities.getContext().fetchResource(StructureDefinition.class, profiles.get(0), IWorkerContext.VersionResolutionRules.defaultRule());
          if (sdt != null) {
            ElementDefinition edt = sdt.getSnapshot().getElementFirstRep();
            if (edt.isMandatory() && !outcome.isMandatory()) {
              outcome.setMin(edt.getMin());
            }
            if (!edt.repeats() && outcome.repeats()) {
              outcome.setMax(edt.getMax());
            }
            // todo: should we consider other constraints?
            // throw new Error("Not handled yet: "+sdt.getVersionedUrl()+" / "+outcome.getPath()+":"+outcome.getSliceName());
          }
        } else if (profiles.size() > 1) {
          throw new Error("Not handled: multiple profiles at "+outcome.getPath()+":"+outcome.getSliceName()+": "+CommaSeparatedStringBuilder.join(",", profiles));          
        }
```

   So: exactly one type profile → the referenced profile's *root element* min/max can tighten the new slice (min only if the profile root is mandatory and the slice isn't; max only if the profile root doesn't repeat and the slice does). More than one profile across all types → hard `Error` (verbatim above, PPP:1419). Note the commented-out `throw new Error("Not handled yet: ...")` and `// todo: should we consider other constraints?` — comments claim broader pickup was contemplated; code only does min/max.
5. **Children generation for the new slice** (PPP:1421-1473): `diffCursor = indexOf(diffItem) + 1`. Guard chain: outcome has types, more diff elements exist, path contains "."; then `!profileUtilities.baseWalksInto(cursors.base.getElement(), cursors.baseCursor)` (Part B.8) and the next diff element's path starts with **`diffMatches.get(0).getPath() + "."`** (note: diff[0]'s path, not diffItem's — same string modulo [x] since all diffMatches share the path). Then:
   - multiple types allowed only if every type is `Reference`, else `DefinitionException` (PPP:1425-1429): > `{0} has children ({1}) and multiple types ({2}) in profile {3}`
   - **`Base`/`Element`/`BackboneElement`** first type (PPP:1431-1447): children are generated by recursing over the *base profile's own child scope of the slicer* (`baseStart = indexOf(currentBase)+1` up to the last `currentBase.getPath()+"."` child) against the diff child scope; contextPathSource = contextPathTarget = root path of the base list; `new PathSlicingParams()`.
   - **datatype** (PPP:1448-1470): `getProfileForDataType(outcome.getType().get(0), ...)`; commented-out fragment verbatim (PPP:1450-1452):

```java
                //                if (t.getCode().equals("Extension") && t.hasProfile() && !t.getProfile().contains(":")) {
                // lloydfix                  dt =
                //                }
```

     null dt → `DefinitionException`: > `{0} has children ({1}) for type {2} in profile {3}, but can''t find type`. Otherwise recursion into `dt.getSnapshot()` starting at index 1 (`/* starting again on the data type, but skip the root */`), diff scope = the diffItem's children, contextPathSource = diff[0]'s path, contextPathTarget = outcome path.
6. `diffpos++`; after the loop (PPP:1477-1479): `if (outcome.hasContentReference() && outcome.hasType()) { outcome.getType().clear(); }` — applies to the **last** constructed outcome only (variable scoping).

Finally `cursors.baseCursor++` (PPP:1481).

### A.3 `processPathWithSlicedBaseWhereDiffsConstrainTypes` (PPP:1494-1655) — type slicing over an already-type-sliced base

Entered when `diffsConstrainTypes` recognized the diff matches as type slices (Part B.1) over an `[x]` base that already has slicing.

#### A.3.1 Shortcut detection and synthetic slicer insertion (PPP:1495-1531)

```java
boolean shortCut = (!typeList.isEmpty() && typeList.get(0).type != null) || (diffMatches.get(0).hasSliceName() && !diffMatches.get(0).hasSlicing());
```

comment (PPP:1500): `// we come here whether they are sliced in the diff, or whether the short cut is used.` If shortCut (the diff dives straight into type slices without a slicing entry), a synthetic slicing-entry ElementDefinition is **inserted at the front of diffMatches AND into the live differential list** (`getDifferential().getElement().add(newDiffCursor, ed)`), remembered as `elementToRemove`. The version branch, verbatim (PPP:1503-1504):

```java
      // this is the short cut method, we've just dived in and specified a type slice.
      // in R3 (and unpatched R4, as a workaround right now...
      if (!VersionUtilities.isR4Plus(profileUtilities.getContext().getVersion()) || !profileUtilities.isNewSlicingProcessing()) { // newSlicingProcessing is a work around for editorial loop dependency
```

- **R3 / unpatched-R4 branch** (PPP:1505-1516): the synthetic slicer gets `ed.addType().setCode(ts.type)` for every TypeSlice in typeList (i.e. the slicer's types are constrained to the listed types), plus slicing discriminator `TYPE`/`$this`, `rules = CLOSED`, `ordered = false`, path from `determineTypeSlicePath`.
- **R4+ with newSlicingProcessing** (PPP:1517-1529), rationale verbatim (PPP:1518-1520):

```java
        // as of R4, this changed; if there's no slice, there's no constraint on the slice types, only one the type.
        // so the element we insert specifies no types (= all types) allowed in the base, not just the listed type.
        // see also discussion here: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Slicing.20a.20non-repeating.20element
```

  Same synthetic slicer but with **no types**. (`newSlicingProcessing` is a plain settable flag on ProfileUtilities, PU:440/4745-4751.)

#### A.3.2 Slicing-shape checks on the (possibly synthetic) entry (PPP:1535-1550)

All `FHIRException`, messages verbatim:
- `slicing.ordered == true` → > `Error at path {0} in {1}: Type slicing with slicing.ordered = true`
- discriminator count != 1 → > `Error at path {0} in {1}: Type slicing with slicing.discriminator.count() > 1`
- discriminator type != TYPE → > `Error at path {0} in {1}: Type slicing with slicing.discriminator.type != ''type''`
- discriminator path != `$this` → > `Error at path {0} in {1}: Type slicing with slicing.discriminator.path != ''$this''`
(Checks apply only to properties the entry actually has: `hasOrdered()` / `hasDiscriminator()` guards.)

#### A.3.3 Slice-name enforcement (PPP:1551-1568)

For each TypeSlice with a concrete type: expected name `tn = rootName(currentBasePath) + Utilities.capitalize(ts.type)` (e.g. `value` + `Quantity` → `valueQuantity`). Missing sliceName → auto-set to `tn`. Present-but-different → `FHIRException`: > `Error at path {0}: Slice name must be ''{1}'' but is ''{2}''`. Missing type → auto-added. More than one type → > `Error at path {0}: Slice for type ''{1}'' has more than one type ''{2}''`. Single wrong type → > `Error at path {0}: Slice for type ''{1}'' has wrong type ''{2}''`. ({0} is contextPathSource if set, else currentBasePath.)

#### A.3.4 Root/slicer processing and re-imposed slicing (PPP:1570-1589)

The slicing entry (diff scope from the entry to its `findEndOfElement`) is recursively processed against the base scope `baseCursor..newBaseLimit` with `PathSlicingParams(true, null, currentBasePath)`; result `e` is the emitted slicer row. `e == null` → `FHIRException`: > `Did not find type root: {0}`. Then verbatim (PPP:1584-1588):

```java
    // now set up slicing on the e (cause it was wiped by what we called.
    e.setSlicing(new ElementDefinition.ElementDefinitionSlicingComponent());
    e.getSlicing().addDiscriminator().setType(ElementDefinition.DiscriminatorType.TYPE).setPath("$this");
    e.getSlicing().setRules(ElementDefinition.SlicingRules.CLOSED); // type slicing is always closed; the differential might call it open, but that just means it's not constraining the slices it doesn't mention
    e.getSlicing().setOrdered(false);
```

**ANCHOR:** the comment CLAIMS and the code ENFORCES: the emitted type-slicer's slicing is unconditionally rebuilt as `TYPE`/`$this`, `rules=CLOSED`, `ordered=false` — whatever the differential (or the base) said. Any diff-authored `rules=open` on a type slicing is discarded here.

#### A.3.5 Matching diff type slices into base slice ranges (PPP:1591-1622)

Verbatim (PPP:1592): `List<BaseTypeSlice> baseSlices = profileUtilities.findBaseSlices(cursors.base, newBaseLimit);` — note the `start` argument is `newBaseLimit`, computed at PPP:1496 as `findEndOfElement(cursors.base, cursors.baseCursor)`: the last element of the *slicer's own* scope (named slices share the slicer's path, not `path+"."`, so `findEndOfElement` stops before them). In the normal case the slicer has no children in the base snapshot, so `newBaseLimit == cursors.baseCursor` and `findBaseSlices` anchors on the slicer itself, collecting the named-slice ranges as described in B.2. Edge: if the slicer DOES have children, `findBaseSlices` anchors on the slicer's **last child**, its same-path check then keys on the child's path, no named slice matches, and `baseSlices` comes back empty — feeding the PPP:1652 IndexOutOfBounds noted in A.3.6.

Loop over diff type slices `i = start(=1)..diffMatches.size()-1`:
- `type = determineFixedType(diffMatches, fixedType, i)` (B.1).
- **min>0 rule** (PPP:1598-1603): if a diff type slice has `min > 0` and it is **not the last** diff match → `FHIRException`: > `Invalid slicing: there is more than one type slice at {0}, but one of them ({1}) has min = 1, so the other slices cannot exist` ; if it IS the last, `fixedType = type` is recorded.
- `bs = chooseMatchingBaseSlice(baseSlices, type)` (B.2, exact type-code equality on the base slice's first type). If found: recursion scope is the base slice's own range `bs.getStart()..bs.getEnd()` and `bs.setHandled(true)`; if not found: scope is the whole `cursors.baseCursor..newBaseLimit` (the slicer scope — the new type slice is generated from the slicer). Recursion uses `PathSlicingParams(true, e, currentBasePath)` (slicer row `e` passed down).

#### A.3.6 Cleanup: synthetic removal, fixedType filter, replay of unhandled base slices (PPP:1623-1654)

- Synthetic slicer removed from the differential; `newDiffLimit--` (PPP:1623-1626).
- If a `fixedType` was recorded (a min>0 last slice): all other type codes are **removed from the slicer row `e`'s types** (PPP:1627-1634).
- **Fake-empty-differential replay** (PPP:1635-1650), verbatim core:

```java
    for (BaseTypeSlice bs : baseSlices) {
      if (!bs.isHandled()) {
        // ok we gimme up a fake differential that says nothing, and run that against the slice.
        StructureDefinition.StructureDefinitionDifferentialComponent fakeDiff = new StructureDefinition.StructureDefinitionDifferentialComponent();
        fakeDiff.getElementFirstRep().setPath(bs.getDefn().getPath());
        ProfilePathProcessorState nc3 = new ProfilePathProcessorState(cursors.baseSource, cursors.base, bs.getStart(), 0, cursors.contextName, cursors.resultPathBase);
          this
            .incrementDebugIndent()
            .withDifferential(fakeDiff)
            .withBaseLimit(bs.getEnd())
            .withDiffLimit(0)
            .withProfileName(getProfileName() + profileUtilities.tail(bs.getDefn().getPath())).withSlicing(new PathSlicingParams(true, e, currentBasePath))
            .processPaths(nc3, mapHelper, null);
      }
    }
```

  Exactly what a replayed base slice goes through: the processor is re-entered over the base slice's full element range (`bs.start..bs.end` — the named slice row plus its children) against a differential containing a **single element whose only property is the slice's path** (no sliceName, no constraints), with the slicer `e` supplied via `PathSlicingParams(true, e, currentBasePath)`. Observable effect: every unmentioned base type-slice is re-emitted through the normal merge machinery (so it gets the standard updateFromBase/path-fixing bookkeeping) with an effectively empty diff, rather than being raw-copied — and rather than being dropped, despite the slicing being forced CLOSED. So "closed" type slicing does not prune base slices the diff omits.
- Exit (PPP:1651-1654): `cursors.baseCursor = baseSlices.get(baseSlices.size() - 1).getEnd() + 1;` — **would throw IndexOutOfBoundsException if `baseSlices` is empty** (base sliced but with no named slice rows); then `cursors.diffCursor = newDiffLimit + 1;` and the commented-out throw, verbatim (PPP:1654):

```java
    //throw new Error("not done yet - slicing / types @ "+cpath);
```

### A.4 `processPathWithSlicedBaseAndEmptyDiffMatches` (PPP:1657-1721) — diff says nothing at this path

Branch on `profileUtilities.hasInnerDiffMatches(getDifferential(), path, cursors.diffCursor, getDiffLimit(), cursors.base.getElement(), true)` — note **`allowSlices=true`** here (contrast A.2.3's `false`): with `true`, a same-path named-slice diff element does not veto the scan and scanning continues across slices (PU:2435).

**Walk-in branch** (diff constrains children somewhere below, PPP:1658-1704):
- The slicer row is copied through: `currentBase.copy()` + updateURLs + fixedPathDest + `updateFromBase` + `markDerived(outcome)` (constraints flagged `SNAPSHOT_IS_DERIVED`) + path check (`Adding wrong path`) + addToResult. Slicing component survives (not stripped).
- Comment verbatim (PPP:1670-1671): `// the profile walks into this, so we need to as well` / `// did we implicitly step into a new type?`
- If `baseHasChildren(cursors.base, currentBase)` (`// not a new type here`): recursion continues at `baseCursor + 1` with the same diff cursor, `new PathSlicingParams()`; afterwards `cursors.baseCursor = indexOfFirstNonChild(...)`. (The base's named slices are *siblings*, not children — they re-enter the outer loop as their own currentBase rows on subsequent iterations.)
- Else (base has no children — implicit datatype step): `dt = getTypeForElement(...)` (throws `Unknown type {0} at {1}` / `{0} has no children ({1}) and no types in profile {2}` / multiple-non-Reference-types error via PU:1612-1631); diff children of `currentBasePath` are collected and replayed against `dt.getSnapshot()` from index 1 (`/* starting again on the data type, but skip the root */`), contextPathSource=currentBasePath, contextPathTarget=outcome path.
- `cursors.baseCursor++`.

**Bulk-copy branch** (diff totally silent, PPP:1705-1720), comment verbatim: `// the differential doesn't say anything about this item` / `// copy across the currentbase, and all of its children and siblings`:

```java
      while (cursors.baseCursor < cursors.base.getElement().size() && cursors.base.getElement().get(cursors.baseCursor).getPath().startsWith(path)) {
```

Because the test is `startsWith(path)` (not `path + "."`), this sweeps the slicer, all its children, AND all sibling named slices and their children in one raw copy loop. Each copied row: updateURLs + fixedPathDest + path check (`Adding wrong path in profile {0}: {1} vs {2}`) + markExtensions + addToResult + user data `SNAPSHOT_BASE_MODEL` = source SD url and `SNAPSHOT_BASE_PATH` = **`cursors.resultPathBase`** (PPP:1717) — note the mismatch with the analogous copy loop at PPP:1352 which sets `SNAPSHOT_BASE_PATH` to `outcome.getPath()`; setting it to the profile-wide `resultPathBase` here looks unintended (code-vs-intent flag). No `updateFromBase`, no `markDerived` in this branch.

Caveat on the `[x]` overlap: `startsWith(path)` on a path like `Observation.value[x]`… `path` here is `currentBase.getPath()` verbatim, so `value[x]` children (`Observation.value[x].id`) and same-path slices are caught; renamed type-slice paths (`Observation.valueQuantity`) would NOT be caught by `startsWith("Observation.value[x]")` — but renamed rows don't occur inside a single base snapshot's slice group in R4+ practice (slices keep the `[x]` path with sliceNames).

---

## PART B — ProfileUtilities slicing helpers

### B.1 Type-slice recognition in the differential

#### `diffsConstrainTypes` (PU:1806-1849)

```java
protected boolean diffsConstrainTypes(List<ElementDefinition> diffMatches, String cPath, List<TypeSlice> typeList)
```

- Commented-out size guard kept verbatim (PU:1807-1808): `//    if (diffMatches.size() < 2)` / `//      return false;` — a single diff match CAN qualify.
- Gate (PU:1809-1811): returns false unless the first diff match's path OR `cPath` ends with `[x]`.
- `typeList.clear()`; `rn = tail(cPath)` minus the trailing `[x]`.
- For every diff match: `n = tail(ed.getPath())`; if `n` doesn't start with `rn` → **return false** (any non-conforming sibling disqualifies the whole set). `s = n.substring(rn.length())` — the suffix after the choice stem (`""`, `"[x]"`, or a type name like `Quantity`). Only suffixes without `.` are classified:
  - `hasSliceName() && type.size()==1` → TypeSlice(ed, working code of the single type).
  - `hasSliceName() && type.size()==0` → type inferred from the **path suffix** `s` if `isDataType(s)` or `isPrimitive(uncapitalize(s))`; else from the **sliceName suffix** `ed.getSliceName().substring(n.length())` by the same two tests; if none match, the element is silently skipped (no typeList entry, but no `return false` either).
  - `!hasSliceName() && !s.equals("[x]")` (renamed path, e.g. `valueQuantity`, no sliceName) → TypeSlice by `isDataType(s)` / `isConstrainedDataType(s)` (mapped through `baseType(s)`) / `isPrimitive(uncapitalize(s))`; silently skipped if none.
  - `!hasSliceName() && s.equals("[x]")` → `typeList.add(new TypeSlice(ed, null))` — the unnamed `[x]` row rides along as a TypeSlice with null type (this is the slicing entry / "default" row).
- Always returns true if the path-prefix scan survives (even if typeList ends up with fewer entries than diffMatches). Note brace/indent oddity at PU:1844-1848 — the final `else` clause is inside the `!s.contains(".")` block; elements whose suffix contains `.` (children of a renamed path, e.g. `valueQuantity.system`, when they land in diffMatches via [x] matching) are classified as nothing but do not disqualify.

#### `determineTypeSlicePath` (PU:1788-1793)

```java
  protected String determineTypeSlicePath(String path, String cpath) {
    String headP = path.substring(0, path.lastIndexOf("."));
//    String tailP = path.substring(path.lastIndexOf(".")+1);
    String tailC = cpath.substring(cpath.lastIndexOf(".")+1);
    return headP+"."+tailC;
  }
```

Rebuilds the synthetic slicer's path as: the diff element's parent path + the base element's tail (so a renamed diff path `X.valueQuantity` yields slicer path `X.value[x]`). Throws `StringIndexOutOfBoundsException` on root paths without `.` (not guarded).

#### `determineFixedType` (PU:1712-1729)

For diff type slice `i`:
- 0 types + sliceName: strip the path tail's `[x]`, take `sliceName.substring(n.length())` (the part of the sliceName after the element name); `isDataType(t)` → t; `isPrimitive(uncapitalize(t))` → uncapitalized t; else `FHIRException`: > `Unexpected condition in differential: implicit slice name does not contain a valid type (''{0}''?) at {1}/{2}`
- exactly 1 type → its `getCode()`.
- otherwise → `FHIRException`: > `Unexpected condition in differential: invalid type at {0}/{1}`
Returns the (possibly updated) running `fixedType` — despite the name, the return value is just "the type of slice i"; the caller decides whether to latch it as fixed (only when min>0, PPP:1598-1603).

#### `getListOfTypes` (PU:1604-1610)

Returns the `Set<String>` of `t.getCode()` over the element's types. (Used elsewhere in dispatch; trivial.)

#### `rootName` (PU:1782-1785)

```java
  protected String rootName(String cpath) {
    String t = tail(cpath);
    return t.replace("[x]", "");
  }
```

### B.2 Base type-slice ranging and matching

#### `findBaseSlices` (PU:1742-1758)

```java
protected List<BaseTypeSlice> findBaseSlices(StructureDefinitionSnapshotComponent list, int start)
```

`base = list.getElement().get(start)`; first skips all `base.getPath()+"."` children from `start+1`; then repeatedly: while the next element has **exactly the same path** as `base` and `hasSliceName()`, open a range at it, absorb its `path+"."` children, and record `new BaseTypeSlice(sliceElement, sliceElement.getTypeFirstRep().getCode(), s, i-1)` — i.e. each base slice is (defn, type = code of its FIRST type, inclusive start index, inclusive end index of its child scope). Scanning stops at the first element that is neither a child nor a same-path named slice. Note the type key is `getTypeFirstRep().getCode()` — a base slice with multiple types is keyed by its first type only; a base slice with no types yields `""`/null code.

`BaseTypeSlice` (BaseTypeSlice.java:7-45): plain holder `{ElementDefinition defn; String type; int start; int end; boolean handled;}`.

#### `chooseMatchingBaseSlice` (PU:1732-1739)

Linear scan; returns the first BaseTypeSlice with `bs.getType().equals(type)` — exact string equality of type codes, no profile/inheritance awareness; null if none. (NPE risk if a base slice had no type → `getType()` null receiver is on `bs.getType()`, fine, but `null.equals` — actually `bs.getType()` would be null and `.equals` throws; unguarded.)

### B.3 Derived-vs-base slicing compatibility predicates

#### `orderMatches` (PU:2372-2374)

```java
  protected boolean orderMatches(BooleanType diff, BooleanType base) {
    return (diff == null) || (base == null) || (diff.getValue() == base.getValue());
  }
```

Tolerance: absent on either side → compatible; otherwise the two Boolean values must be identical — **`ordered` may not change in either direction** (false→true and true→false both rejected). Observation, not a bug claim: `getValue()` returns boxed `Boolean` and `==` is reference equality, saved in practice by `Boolean.valueOf` caching. Also note the call site (PPP:1234) already requires both sides to `hasOrderedElement()`, so the null tolerances are dead at that call site.

#### `discriminatorMatches` (PU:2376-2389)

```java
    if (diff.isEmpty() || base.isEmpty())
    	return true;
    if (diff.size() < base.size())
    	return false;
    for (int i = 0; i < base.size(); i++)
    	if (!matches(diff.get(i), base.get(i)))
    		return false;
    return true;
```

with `matches(c1,c2)` = `c1.getType().equals(c2.getType()) && c1.getPath().equals(c2.getPath())` (PU:2387-2389). Tolerances: either side empty → compatible (diff may omit discriminators entirely; base may have none). Otherwise the base's discriminator list must be a **pairwise, order-sensitive prefix** of the diff's (same type+path at each index); the diff may append extra discriminators (superset allowed), may not drop or reorder any (subset/permutation rejected). No comparison of discriminator type semantics beyond enum equality.

#### `ruleMatches` (PU:2392-2395)

```java
  protected boolean ruleMatches(SlicingRules diff, SlicingRules base) {
    return (diff == null) || (base == null) || (diff == base) || (base == SlicingRules.OPEN) ||
        ((diff == SlicingRules.OPENATEND && base == SlicingRules.CLOSED));
  }
```

Tolerance table (diff → allowed?): base OPEN → **anything** (open→closed and open→openAtEnd both allowed); base OPENATEND → only OPENATEND (or absent); base CLOSED → CLOSED **or OPENATEND** (the last disjunct explicitly tolerates a diff stating `openAtEnd` over a closed base — a nominal loosening that is accepted, not corrected); diff absent → always ok. Reminder: the whole check is skipped for choice elements (`!currentBase.isChoice()` guard, PPP:1238).

#### `summarizeSlicing` (PU:1978-2001)

Builds the `{0}`/`{1}` strings for the mismatch messages: comma-separated `type:path` discriminators, then ` (` + `ordered`/`unordered` (only if hasOrdered) + `/` + rules code (only if hasRules) + `)`, then optional ` "description"`.

### B.4 `updateFromSlicing` (PU:2351-2370) — merging a diff slicing component onto the base's

```java
  protected void updateFromSlicing(ElementDefinitionSlicingComponent dst, ElementDefinitionSlicingComponent src) {
    if (src.hasOrderedElement())
      dst.setOrderedElement(src.getOrderedElement().copy());
    if (src.hasDiscriminator()) {
      //    dst.getDiscriminator().addAll(src.getDiscriminator());  Can't use addAll because it uses object equality, not string equality
      for (ElementDefinitionSlicingDiscriminatorComponent s : src.getDiscriminator()) {
        boolean found = false;
        for (ElementDefinitionSlicingDiscriminatorComponent d : dst.getDiscriminator()) {
          if (matches(d, s)) {
            found = true;
            break;
          }
        }
        if (!found)
          dst.getDiscriminator().add(s);
      }
    }
    if (src.hasRulesElement())
      dst.setRulesElement(src.getRulesElement().copy());
  }
```

Per-field rules: `ordered` — overwritten by the diff when the diff states it (copy of the element, so extensions ride along); absent in diff → base value kept. `discriminator` — set-union keyed by (type, path): diff discriminators not already present are **appended** to the base's list; base discriminators are never removed or reordered. `rules` — overwritten when the diff states it (this is what lets open→closed happen, as permitted by `ruleMatches`). **`openAtEnd`: there is no separate `openAtEnd` field in R5 slicing — it exists only as the `SlicingRules.OPENATEND` code value, so it is covered by the rules copy; nothing else handles it. `description` is NOT merged by this method** (the diff's slicing.description does not reach the snapshot via updateFromSlicing; whether it arrives some other way is outside this packet). No validation happens here — compatibility was checked (or skipped) beforehand at PPP:1234-1239.

### B.5 The auto-added-entry min-sum gate — `generateSnapshot`, PU:976-1036 (ANCHOR PU:983-1005)

Location: near the end of `public void generateSnapshot(StructureDefinition base, StructureDefinition derived, String url, String webUrl, String profileName)` (PU:740), in the post-generation validation sweep. Directly above it (PU:969-975), the SPECIALIZATION-only Base backfill for contrast:

```java
        if (derived.getDerivation() == TypeDerivationRule.SPECIALIZATION) {
          for (ElementDefinition ed : derived.getSnapshot().getElement()) {
            if (!ed.hasBase()) {
              ed.getBase().setPath(ed.getPath()).setMin(ed.getMin()).setMax(ed.getMax());
            }
          }
        }
```

The gate itself, verbatim (PU:976-1018, abridged only by the max/minmax blocks quoted in full below):

```java
        // check slicing is ok while we're at it. and while we're doing this. update the minimum count if we need to
        String tn = derived.getType();
        if (tn.contains("/")) {
          tn = tn.substring(tn.lastIndexOf("/")+1);
        }
        Map<String, ElementDefinitionCounter> slices = new HashMap<>();
        i = 0;
        for (ElementDefinition ed : derived.getSnapshot().getElement()) {
          if (ed.hasSlicing()) {
            slices.put(ed.getPath(), new ElementDefinitionCounter(ed, i));            
          } else {
            Set<String> toRemove = new HashSet<>();
            for (String s : slices.keySet()) {
              if (Utilities.charCount(s, '.') >= Utilities.charCount(ed.getPath(), '.') && !s.equals(ed.getPath())) {
                toRemove.add(s);
              }
            }
            for (String s : toRemove) {
              ElementDefinitionCounter slice = slices.get(s);
              int count = slice.checkMin();
              boolean repeats = !"1".equals(slice.getFocus().getBase().getMax()); // type slicing if repeats = 1
              if (count > -1 && repeats) {
                if (slice.getFocus().hasUserData(UserDataNames.SNAPSHOT_auto_added_slicing)) {
                  slice.getFocus().setMin(count);
                } else {
                  String msg = "The slice definition for "+slice.getFocus().getId()+" has a minimum of "+slice.getFocus().getMin()+" but the slices add up to a minimum of "+count; 
                  addMessage(new ValidationMessage(Source.ProfileValidator, ValidationMessage.IssueType.VALUE, 
                      "StructureDefinition.snapshot.element["+slice.getIndex()+"]", msg, forPublication ? ValidationMessage.IssueSeverity.ERROR : ValidationMessage.IssueSeverity.INFORMATION).setIgnorableError(true));
                }
              }
              count = slice.checkMax();
              ...
```

Mechanics:
- Walking the **finished snapshot** in order, every `hasSlicing()` row opens an `ElementDefinitionCounter` keyed by path. Every later element **with a sliceName at that same path** feeds it (PU:1028-1029 → `counter.count(ed, sliceName)`): `countMin += ed.getMin()`, `countMax += max(ed.getMax())` saturating at `*`=`Integer.MAX_VALUE` (PU:193-206); duplicate sliceNames at a path → ERROR (ignorable) > `Duplicate slice name {name} on {id} ([{i}])` (PU:1028-1033). A sliceName with no open counter → ERROR (ignorable) > `The element {id} launches straight into slicing without the slicing being set up properly first` (PU:1023-1027).
- A counter is closed ("flushed") when a NON-slicing element appears whose path depth is <= the slicer's depth and whose path differs (PU:987-992). Children of slices (deeper) and the slice rows themselves (same path — excluded by `!s.equals(ed.getPath())`) don't close it.
- On flush, three checks — all gated by `repeats = !"1".equals(slice.getFocus().getBase().getMax())` for min and max (comment CLAIMS `// type slicing if repeats = 1`, i.e. Base.max=="1" is used as a proxy for "this is a type slicing where slice cardinalities don't sum" — code tests Base.max, not actual type-slicing shape):
  1. **min-sum**: `checkMin()` returns countMin only if `countMin > focus.getMin()` (PU:169-175). If so and repeats: **if the slicer carries `SNAPSHOT_auto_added_slicing` → `slice.getFocus().setMin(count)` — the slicer's min is silently raised to the sum of slice mins** (PU:998-999). Otherwise a message: > `The slice definition for {id} has a minimum of {min} but the slices add up to a minimum of {count}` — severity ERROR when `forPublication`, else INFORMATION, and `.setIgnorableError(true)` (PU:1001-1003).
  2. **max-sum** (PU:1006-1011): `checkMax()` fires when `countMax > max(focus.getMax())`; always just an INFORMATION message: > `The slice definition for {id} has a maximum of {max} but the slices add up to a maximum of {count}. Check that this is what is intended`
  3. **min<=max sanity** (PU:1012-1016): `!checkMinMax()` (`countMin <= countMax` violated) → WARNING: > `The slice definition for {id} has a maximum of {max} which is less than the minimum of {min}` (message text prints the FOCUS min/max, though the check compared the SUMS — code-vs-message mismatch).
- **Never-flushed tail:** counters still open when the element loop ends are simply abandoned — a sliced element whose slice group extends to the very last snapshot element is never checked/adjusted.
- Where `SNAPSHOT_auto_added_slicing` comes from: PPP:345 (unsliced-base path: diff introduces slices with no slicing entry → synthesized `makeExtensionSlicing()` slicer) and PPP:1250 (sliced-base path: diff dives into named slices without restating the slicing entry → the *copied base slicer* gets the flag). So in BOTH paths, "differential silent about the slicing entry" ⇒ min is auto-adjusted; "differential restates the slicing entry" ⇒ discrepancy is reported instead. The PPP:1250 case means even a slicer fully authored in the *base* profile is treated as "auto added" for this purpose — arguably surprising; flagged.

### B.6 KEY QUESTION — `updateFromBase` (PU:2004-2020) and Base.min for new slices

Verbatim, complete:

```java
  protected void updateFromBase(ElementDefinition derived, ElementDefinition base, String baseProfileUrl) {
    derived.setUserData(UserDataNames.SNAPSHOT_BASE_MODEL, baseProfileUrl);
    derived.setUserData(UserDataNames.SNAPSHOT_BASE_PATH, base.getPath());
    if (base.hasBase()) {
      if (!derived.hasBase())
        derived.setBase(new ElementDefinitionBaseComponent());
      derived.getBase().setPath(base.getBase().getPath());
      derived.getBase().setMin(base.getBase().getMin());
      derived.getBase().setMax(base.getBase().getMax());
    } else {
      if (!derived.hasBase())
        derived.setBase(new ElementDefinitionBaseComponent());
      derived.getBase().setPath(base.getPath());
      derived.getBase().setMin(base.getMin());
      derived.getBase().setMax(base.getMax());
    }
  }
```

Answer (scoped to the sliced-base path of this packet):
- For the **slicing entry** (PPP:1243) and for **every slice row** — matched base slices' candidate copies (PPP:1315), copied-through base slices (same), and **newly appended slices** (PPP:1388) — `updateFromBase(outcome, currentBase, ...)` is called with `currentBase` = the base slicer element. Since a generated base snapshot's elements always `hasBase()`, the first branch runs: **`ElementDefinition.base.path/min/max` are copied verbatim from the sliced element's own `base` component. Base.min is NOT reset to 0 for a named slice** — it stays whatever the base element's Base.min was (e.g. a slice of a min=1 element inherits Base.min=1). Both branches overwrite unconditionally; the `!derived.hasBase()` guard only controls component creation, never preservation of existing values.
- What IS set to 0 for a newly appended slice is the element's **own** `min` (PPP:1390, `outcome.setMin(0)` — quoted in A.2.7), possibly re-raised by the type-profile pickup (PPP:1409-1411) or by `updateFromDefinition` applying the diff's stated min.
- The `hasBase()==false` fallback (Base := element's own path/min/max) plus the SPECIALIZATION-only backfill at PU:969-975 (quoted in B.5) are the only places Base is synthesized; neither zeroes min either.
- Conclusion for the WGM question: **Java's Base component for a named slice equals the sliced base element's Base component unchanged (min included)** — matching the .NET behavior of deep-copying the sliced element's Base without resetting min. (Commit-pair caveat: verified in code at b06c7ee, not re-verified against the 6.10.2 sweep oracle.)

### B.7 Small predicates

#### `isSlicedToOneOnly` (PU:2397-2399)

```java
    return (e.hasSlicing() && e.hasMaxElement() && e.getMax().equals("1"));
```

#### `isTypeSlicing` (PU:2401-2405)

```java
    return (e.hasSlicing() && e.getSlicing().getDiscriminator().size() == 1 && 
        e.getSlicing().getDiscriminatorFirstRep().getType() == DiscriminatorType.TYPE &&
        "$this".equals(e.getSlicing().getDiscriminatorFirstRep().getPath()));
```

Exactly one discriminator, TYPE/$this.

#### `isImplicitSlicing` (PU:1796-1803)

```java
    if (ed == null || ed.getPath() == null || path == null)
      return false;
    if (path.equals(ed.getPath()))
      return false;
    boolean ok = path.endsWith("[x]") && ed.getPath().startsWith(path.substring(0, path.length()-3));
    return ok;
```

True when the diff element's path is a renamed variant of a `[x]` base path (differs from it but shares the stem) — recognizes shortcut type slicing without slicing/sliceName.

#### `unbounded` (PU:2517-2526)

```java
    StringType max = definition.getMaxElement();
    if (max == null)
      return false; // this is not valid
    if (max.getValue().equals("1"))
      return false;
    if (max.getValue().equals("0"))
      return false;
    return true;
```

Any max other than "0"/"1" (i.e. "*" or an integer >= 2) counts as unbounded; missing max → false with the comment CLAIMING invalidity.

#### `getSiblings` (PU:2339-2349)

```java
    String path = current.getPath();
    int cursor = list.indexOf(current)+1;
    while (cursor < list.size() && list.get(cursor).getPath().length() >= path.length()) {
      if (pathMatches(list.get(cursor).getPath(), path))
        result.add(list.get(cursor));
      cursor++;
    }
```

Collects all subsequent elements whose path `pathMatches` the current path — `pathMatches` (PU:2027-2029) is equality OR `[x]`-stem match (`p2` ends with `[x]`, `p1` starts with the stem, and the remainder after the stem position contains no `.`). Scan stops at the first element with a *shorter* path (children have longer paths and are skipped by the pathMatches test, so the scan runs across the whole slice group including children until an ancestor/uncle path appears). Used at PPP:1311 to enumerate the named base slices (and, for `[x]` bases, renamed variants).

#### `baseWalksInto` (PU:1877-1883)

```java
    if (cursor >= elements.size())
      return false;
    String path = elements.get(cursor).getPath();
    String prevPath = elements.get(cursor - 1).getPath();
    return path.startsWith(prevPath + ".");
```

True when the element at `cursor` is a child of the element at `cursor-1`. (Unguarded `cursor - 1` → would throw at cursor 0.) Used at PPP:1423 to suppress datatype-child generation for a new slice when the base itself continues into children.

### B.8 `makeExtensionSlicing` (PU:2408-2414)

Verbatim, complete:

```java
  protected ElementDefinitionSlicingComponent makeExtensionSlicing() {
  	ElementDefinitionSlicingComponent slice = new ElementDefinitionSlicingComponent();
    slice.addDiscriminator().setPath("url").setType(DiscriminatorType.VALUE);
    slice.setOrdered(false);
    slice.setRules(SlicingRules.OPEN);
    return slice;
  }
```

Synthesized slicing for auto-added extension slicers: single discriminator VALUE/`url`, `ordered=false`, `rules=open`, no description. (Used in the unsliced-base path, PPP:344, alongside the `SNAPSHOT_auto_added_slicing` flag — see B.5.)

---

## Cross-cutting code-vs-comment mismatches (collected)

1. PPP:1207 comment claims "you can't re-slice existing slices", but PPP:1377-1384 implements a (single-level) reslice template lookup; its own comment admits `// this is wrong if there's more than one reslice (todo: one thing at a time)`.
2. PPP:1247-1249 dead branch — the `SNAPSHOT_GENERATED_IN_SNAPSHOT` bookkeeping commented as "because of updateFromDefinition isn't called" is unreachable.
3. PPP:1276-1284 indentation implies the recursion is inside the diffCursor-advance `while`; it is not (no braces).
4. PU:996 comment `// type slicing if repeats = 1` — code actually tests `Base.max == "1"`, a proxy that also matches any non-repeating element, not just type slicing.
5. PU:1013-1015 min<=max message prints the slicer's own min/max while the failed check compared the slice SUMS.
6. PPP:1717 sets `SNAPSHOT_BASE_PATH` to `cursors.resultPathBase` where the analogous PPP:1352 uses the element's own path.
7. PPP:1587 comment and code agree (type slicing forced CLOSED) — listed here as the one place a diff's stated rules are silently *overridden* rather than validated.

## Edge/robustness observations (code as written)

- PPP:1652 `baseSlices.get(baseSlices.size()-1)` throws on an empty list. Two triggers: (a) a sliced `[x]` base with a slicing entry but zero named slices entering the type-slice path; (b) a slicer that has children in the base snapshot — `findBaseSlices` then anchors on the last child and finds no same-path slices (see A.3.5).
- PU:2373 `orderMatches` compares boxed Booleans with `==` (works via `Boolean.valueOf` caching; observation only).
- PU:1732-1739 `chooseMatchingBaseSlice` NPEs if a base slice row has no type (its `type` field is null).
- PU:983-1036 min-sum gate: counters open at the end of the snapshot are never flushed (no tail check).
- PPP:1390 new-slice `min=0` happens BEFORE `updateFromDefinition`, so a diff-stated min always wins; the type-profile pickup (PPP:1405-1417) runs AFTER updateFromDefinition and only raises min when the outcome is still non-mandatory.

---

## Parked findings (other packets' scope — recorded, not expanded)

- **`getDiffMatches`** (PU:2444-2484): matches diff elements to a base path segment-by-segment via `split("\\.")` with `isSameBase` `[x]`-stem tolerance per segment (PU:2487-2489); contains a large commented-out warning block about "unknown element ... or it is out of order" removed because it misfired on inherited elements. Internals belong to the diff-matching packet.
- **Unsliced-base slicing establishment** (PPP:320-362): where the diff *introduces* slicing over an unsliced base — includes the "default set before the slices" branch, `slicingMatches` re-statement checking with `ATTEMPT_TO_CHANGE_SLICING` (> `The element at {0} defines the slicing {1} but then an element in the slicing {2} tries to redefine the slicing to {3}`; second occurrence logged at INFORMATION with mismatched format args), `makeExtensionSlicing()` + `SNAPSHOT_auto_added_slicing` at PPP:344-345, and `DID_NOT_FIND_SINGLE_SLICE_` (> `Did not find single slice: {0}`). Only the auto-added flag interaction was pulled into B.5; the rest is the non-sliced-base packet's scope.
- **`updateFromDefinition`** merge semantics (min/max/fixed/binding/constraints merging, `trimDifferential` effect) — referenced many times above; not read in this packet.
- **`hasInnerDiffMatches`** (PU:2420-2442) quoted only for the `allowSlices` asymmetry (false at PPP:1260 vs true at PPP:1658); its fall-through comment `// not sure why we get here, but returning false at this point makes a bunch of tests fail` (PU:2438) parked with it.
- **`oneMatchingElementInDifferential`** (PPP:1723-1736) — dispatcher-side; uses `isImplicitSlicing`; belongs to the main-loop packet.
- **`fillOutFromBase`** (PU:1886+) — slice back-fill used elsewhere; not in this packet's call paths.
- `ElementDefinitionCounter.count()` max-saturation subtlety: once countMax reaches MAX_VALUE it stays (PU:195-202) — relevant to any future max-sum spec text.
