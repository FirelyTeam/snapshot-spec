# SnapshotGenerationPreProcessor.java — structure map (orientation pass)
Source: org.hl7.fhir.core @ commit b06c7ee (sparse clone 2026-08-21)
File: org.hl7.fhir.r5/src/main/java/org/hl7/fhir/r5/conformance/profile/SnapshotGenerationPreProcessor.java
Generated: 2026-08-26 (Phase 3 packet 1)

File length: 1227 lines. Class declared at line 53, `@Slf4j`, package `org.hl7.fhir.r5.conformance.profile`.

## 1. Purpose statement

The class-level javadoc (lines 41–51) describes only one job: when a differential defines a sliced element and puts *trailing properties* after the slicing entry ("stuff for all slices"), those properties must be merged into each named slice that follows — "kind of multiple inheritance". The javadoc itself flags this as risky territory: extension-slicing concerns and merging logic that "might need to be sophisticated" (lines 48–49).

Structurally, however, the class does **three distinct jobs**:

1. **Slice trailing-property propagation** (the javadoc's job): `process` → `processSlices` (688) scans the differential, builds `SliceInfo` records, and for each slicing whose `sliceStuff` (content defined at the slicer level after slicing starts) is non-empty, merges that content into every named slice via `mergeElements(List, List, ED, ED)` (743) and `merge` (993), inserting missing elements at computed positions (847–980 machinery).
2. **Additional-base merging** (lines 137–152 in `process`): if the source SD carries the `EXT_ADDITIONAL_BASE` extension, each additional base SD is fetched, *recursively preprocessed by a fresh `SnapshotGenerationPreProcessor`* (line 147), sparse-filled, and then merged element-by-element into the differential via `mergeElementsFromAdditionalBase` (167) → `mergeElements(List, DefinitionNavigator, …)` (178) → `mergeElementDefinitions` (399) with a family of per-property merge helpers.
3. **Two orphan public utilities**: `supplementMissingDiffElements` (1102) — returns a diff element list with synthesized root and intermediate sparse elements — and `trimSnapshot` (1183) — produces a reduced SD containing only snapshot elements that trace back to the differential. **No callers found in the sparse clone** (the clone excludes other modules such as validator/rendering that may call them); not invoked from `process`.

**Call sites (verified):**
- `ProfileUtilities.generateSnapshot`, line 825: `new SnapshotGenerationPreProcessor(this).process(diff, derived);` — invoked **unconditionally** (no guard flag) on every snapshot generation, immediately after `SNAPSHOT_GENERATED_IN_SNAPSHOT` user data is cleared (820–821). It operates on a **clone** of the differential (`cloneDiff`, line 824, with comment "we make a copy here because we're sometimes going to hack the differential while processing it. Have to migrate user data back afterwards").
- `SnapShotGenerationTests.java:615`: `new SnapshotGenerationPreProcessor(pu).process(sdc.getDifferential(), sdc);` (test harness pre-process mode).
- Self-recursive construction at line 147 (additional-base processing).

## 2. Method inventory

Top-level methods of `SnapshotGenerationPreProcessor` (inner-class methods listed in §3).

| Method (brief signature) | Lines | Purpose |
|---|---|---|
| `SnapshotGenerationPreProcessor(ProfileUtilities utils)` (ctor) | 128–132 | Store `utils`, take `context` from it. |
| `process(StructureDefinitionDifferentialComponent diff, StructureDefinition srcOriginal)` **public** | 134–153 | Entry point: wraps src in shallow clone, runs `processSlices`; if `EXT_ADDITIONAL_BASE` present, sparse-fills diff and recursively preprocesses + merges each additional base. |
| `shallowClone(StructureDefinition, StructureDefinitionDifferentialComponent)` | 155–165 | New SD carrying url/version/type/derivation/baseDefinition/extension + the given diff. |
| `mergeElementsFromAdditionalBase(StructureDefinition sourceSD, StructureDefinition baseSD)` | 167–176 | Sets up DefinitionNavigators over base+source diffs and rewrites sourceSD's diff element list to the merged output. |
| `mergeElements(List<ED> output, DefinitionNavigator base, DefinitionNavigator source, SourcedChildDefinitions children, StructureDefinition baseSD)` | 178–244 | Recursive walk over type-child order merging base/source elements, incl. slice pairing; "Not done yet" throws when only one side is sliced (215, 217). |
| `getMatchingSlice(DefinitionNavigator base, DefinitionNavigator slice, ElementDefinitionSlicingComponent slicing)` | 246–266 | Find the base slice whose discriminator values deep-equal the source slice's; throws `Error("Duplicate slice")` on double match (261). |
| `getDiscriminatorValue(DefinitionNavigator slice, …DiscriminatorComponent d)` | 268–304 | Compute a slice's discriminator value; only TYPE-on-`$this` and VALUE(fixed/pattern) implemented, all other discriminator kinds throw "Not supported yet". |
| `valuesMatch(List<DataType>, List<DataType>)` | 306–315 | Pairwise value-list comparison. |
| `valuesMatch(DataType, DataType)` | 317–325 | Null-safe `equalsDeep`. |
| `slicingIsConsistent(ElementDefinitionSlicingComponent src, … base)` | 327–344 | Same rules + same discriminator set (type+path) between two slicings. |
| `describeDiscriminators(ElementDefinitionSlicingComponent)` | 346–352 | Human-readable slicing summary for error messages. |
| `getChildren(SourcedChildDefinitions children, ED child, DefinitionNavigator source, DefinitionNavigator base, StructureDefinition baseSD)` | 354–387 | Child map for recursion; for choice types resolves stated/implied type on both sides, errors on mismatch or indeterminate type. |
| `statedOrImpliedType(DefinitionNavigator)` | 389–397 | Manual type on the navigator, else single-type element's code, else null. |
| `mergeElementDefinitions(ED base, ED source, StructureDefinition baseSD)` | 399–531 | Property-by-property merge of one source/base ED pair for additional-base merging: chooseProp for texts/flags, tighter min/max, fixed/pattern reconciliation, min/max value+length, union lists, valueAlternatives intersection, type intersection via `mergeTypes`, binding (both-sides case unimplemented, 522). |
| `chooseProp(T source, T base)` | 533–541 | Source-wins-if-nonempty, else base, else null (copies). |
| `mergeTypes(String vurl, String path, TypeRefComponent t1, TypeRefComponent t2)` | 543–593 | Merge one matching type pair: profile reconciliation via `specialises`/`findJointProfile`, targetProfile both-sides case empty (580), aggregation/versioning conflict errors. |
| `findJointProfile(StructureDefinition sd1, StructureDefinition sd2)` | 595–604 | Scan all known SDs for one whose baseDefinitions contain both. |
| `specialises(StructureDefinition focus, StructureDefinition other)` | 606–617 | Recursive base-definition ancestry test (ignores impose/compliesWith — comment at 607). |
| `union(List<T> merged, List<T> source, List<T> base)` | 619–632 | Copy source items, add base items not already present. |
| `isLower(String vurl, String path, String property, DataType v1, DataType v2)` | 634–655 | Compare Quantity/dateTime/decimal values for min/maxValue merging; errors on incomparable types. |
| `checkPatternValues(String vurl, String path, DataType v1, DataType v2, boolean extras)` | 657–685 | Recursive compatibility check + merge of fixed/pattern values; conflicts and multi-valued children throw. |
| `processSlices(StructureDefinitionDifferentialComponent diff, StructureDefinition src)` | 688–741 | Job-1 driver: pass 1 partitions diff into `SliceInfo`s; pass 2 bails on nested-slicing-in-sliceStuff; pass 3 (backwards) merges sliceStuff into each slice; final pass `ProfileUtilities.markExtensions` on every element. |
| `mergeElements(List<ED> elements, List<ED> allSlices, ED slice, ED slicer)` | 743–810 | Job-1 per-slice merge: match sliceStuff into the slice's element range; unmatched entries are copied, re-id'd (slicer id → slice id), marked `SNAPSHOT_PREPROCESS_INJECTED`, and inserted at a computed point. |
| `elementsMatch(ED ed1, ED ed2)` | 812–822 | Path match (choice-aware) + sliceName equality/absence. |
| `pathsMatch(String path1, String path2)` | 824–845 | Equal, or one side `[x]`-truncated prefix of a same-level concrete path. |
| `determineInsertionPoint(List<ED> elements, int startOfSlice, int endOfSlice, String id, String path, List<ElementAnalysis> edDef)` | 847–869 | Work backwards through id segments finding peers; insert before the first peer that comes after, else after last peer, else at end of slice. |
| `findPeers(List<ED> elements, int startOfSlice, int endOfSlice, String subId)` | 871–880 | Elements in the slice range whose id starts with subId. |
| `summary(List<ElementAnalysis> edDef)` | 882–889 | Comma-joined ElementAnalysis summaries (debug helper). |
| `comesAfterThis(String id, String path, List<ElementAnalysis> edDef, ED ed)` | 891–916 | Order two element ids by position of their diverging name in the definition's child order; special case for slice-setup-after-slice (904–909). |
| `indexOfName(ElementAnalysis sed, String name)` | 918–928 | Index of a (slice-name-stripped) name in the analysed children list. |
| `analysePath(ED ed)` | 930–939 | Build an ElementAnalysis chain for each path segment. |
| `analysePathSegment(ED ed, List<ElementAnalysis> res, String pn)` | 941–980 | First segment: fetch type SD; later segments: resolve child (choice-aware, primitive-uncapitalize) via `utils.getChildMap`; DefinitionException on unknown type/property. |
| `findEndOfSlice(List<ED> elements, ED slice)` | 982–991 | Last index still belonging to the slice (descendant path, or same path + same sliceName). |
| `merge(ED focus, ED base)` | 993–1075 | Job-1 fill-if-absent merge: ~26 properties copied from sliceStuff element into slice element only when the slice doesn't already state them. |
| `isExtensionSlicing(ED ed)` | 1077–1086 | True for standard extension slicing: name extension/"modiferExtension" (sic), rules OPEN, `ordered` explicitly present and false (line 1081: absent or true `ordered` fails the test), single VALUE discriminator on "url". |
| `getSlicing(ED ed)` | 1088–1100 | Find the innermost open `SliceInfo` whose path prefixes ed's path; closes slicings whose path is longer than ed's. |
| `supplementMissingDiffElements(StructureDefinition profile)` **public** | 1102–1118 | Copy of diff list with synthesized root + sparse intermediate elements. No callers in the sparse clone. |
| `insertMissingSparseElements(List<ED> list, String typeName)` | 1120–1156 | Insert a root element if absent, then synthesize intermediate parent elements (path+id) wherever consecutive entries are neither siblings nor parent/child. |
| `findParent(List<ED> list, int i, String path)` | 1159–1164 | Walk backwards to the nearest element whose path prefixes `path`. |
| `isSibling(String[] pathCurrent, String[] pathLast, int firstDiff)` | 1166–1168 | Same depth, diverging only at the last segment. |
| `isChild(String[] pathCurrent, String[] pathLast, int firstDiff)` | 1171–1173 | Exactly one level deeper, sharing the full parent path. |
| `makeTail(String[] pathCurrent, int start, int index)` | 1175–1181 | Dot-joined path segment range for synthesized paths. |
| `trimSnapshot(StructureDefinition profile)` **public** | 1183–1225 | Mark snapshot elements reachable from a diff-derived element (stack walk over `SNAPSHOT_DERIVATION_DIFF` / `SNAPSHOT_FROM_DIFF` user data), then build a skeleton SD holding only the marked elements. No callers in the sparse clone. |

## 3. Internal data structures

**Fields** (123–126):
- `context : IWorkerContext` (123) — resolution context, taken from `utils`.
- `utils : ProfileUtilities` (124) — owner; used for `getChildMap` and constants.
- `typeNames : Set<String>` (125) — **declared but never used** in this file.
- `slicings : List<SliceInfo>` (126) — accumulator for job 1; instance state, one preprocessor per `process` call.

**Inner class `ElementAnalysis`** (55–84) — one resolved step of an element path: `structure` (defining SD), `element` (the ED), `type` (chosen type for choice elements), `children` (SourcedChildDefinitions, lazily set). Methods: ctor 60–65, `getStructure` 66–68, `getElement` 69–71, `getChildren` 72–74, `setChildren` 75–77, `getType` 78–80, `summary` 81–83.

**Inner class `SliceInfo`** (86–121) — one slicing encountered in the diff: `parent` (enclosing SliceInfo for nested slicing), `path` (slicer path), `closed` (no longer accepting elements), `slicer` (the slicing ED), `sliceStuff` (elements defined after the slicer but before the first named slice — the "for all slices" content), `slices` (the named slice EDs, null until first slice). Methods: ctor 94–102, `newSlice` 104–112, `add` 113–120. Note `add` routes content to `sliceStuff` only while `slices == null`, and always forwards to `parent.add`.

## 4. Call graph

```
process (134)                                        [public entry #1]
├── shallowClone (155)
├── processSlices (688)
│   ├── getSlicing (1088)
│   ├── isExtensionSlicing (1077)
│   ├── SliceInfo ctor / newSlice / add (94/104/113)
│   ├── mergeElements(List, List<ED>, ED, ED) (743)      [job-1 overload]
│   │   ├── findEndOfSlice (982)
│   │   ├── elementsMatch (812)
│   │   │   └── pathsMatch (824)
│   │   ├── merge(ED, ED) (993)                          [fill-if-absent]
│   │   ├── analysePath (930)
│   │   │   └── analysePathSegment (941)  [→ utils.getChildMap, context.fetchTypeDefinition]
│   │   └── determineInsertionPoint (847)
│   │       ├── findPeers (871)
│   │       └── comesAfterThis (891)
│   │           └── indexOfName (918)
│   └── ProfileUtilities.markExtensions (739)
└── [if EXT_ADDITIONAL_BASE] (137)
    ├── insertMissingSparseElements (1120)
    │   ├── findParent (1159)
    │   ├── isSibling (1166) / isChild (1171)
    │   └── makeTail (1175)
    ├── new SnapshotGenerationPreProcessor(utils).process(...) (147–149)   [recursion per additional base]
    └── mergeElementsFromAdditionalBase (167)
        ├── mergeElementDefinitions (399)                 [root pair]
        └── mergeElements(List, DefNav, DefNav, SourcedChildDefinitions, SD) (178)  [job-2 overload, recursive]
            ├── mergeElementDefinitions (399)
            │   ├── chooseProp (533)
            │   ├── checkPatternValues (657)  [recursive]
            │   ├── isLower (634)             [recursive for Quantity.value]
            │   ├── union (619)
            │   └── mergeTypes (543)
            │       ├── specialises (606)     [recursive]
            │       └── findJointProfile (595)
            ├── slicingIsConsistent (327)
            ├── describeDiscriminators (346)
            ├── getMatchingSlice (246)
            │   ├── getDiscriminatorValue (268)
            │   └── valuesMatch (306 → 317)
            └── getChildren (354)
                └── statedOrImpliedType (389)

supplementMissingDiffElements (1102)                 [public entry #2 — no in-clone callers]
└── insertMissingSparseElements (1120)

trimSnapshot (1183)                                  [public entry #3 — no in-clone callers]
    (self-contained; reads SNAPSHOT_DERIVATION_DIFF / SNAPSHOT_FROM_DIFF user data)
```

`summary(List<ElementAnalysis>)` (882) has no callers in this file (debug leftover).

## 5. Method → chapter mapping

| Method(s) | Chapter(s) | Rationale |
|---|---|---|
| `process` (134), `shallowClone` (155) | ch02 | Differential preprocessing entry point; also anchor for ch03 note that this runs before base-snapshot walking, on a cloned diff. |
| `processSlices` (688), `SliceInfo` (86), `getSlicing` (1088), `isExtensionSlicing` (1077) | ch06 (+ch02) | Partitioning the diff into slicings; extension-slicing exemption. |
| `mergeElements(List,List,ED,ED)` (743), `merge` (993), `findEndOfSlice` (982) | ch06, ch05 | Trailing-content-into-slice propagation; `merge` is a fill-if-absent property list relevant to ch05's merge-semantics register. |
| `elementsMatch` (812), `pathsMatch` (824) | ch04 (+ch06) | Element matching incl. `[x]` renaming tolerance. |
| `determineInsertionPoint` (847), `findPeers` (871), `comesAfterThis` (891), `indexOfName` (918), `analysePath` (930), `analysePathSegment` (941), `summary` (882) | ch06 (+ch04) | Ordering injected elements by definition child order. |
| Additional-base gate in `process` (137–152), `mergeElementsFromAdditionalBase` (167), `mergeElements(DefNav overload)` (178), `getChildren` (354), `statedOrImpliedType` (389) | ch05, ch03 | Merging a second base into the differential — base resolution + structured walk; **not** ch06 despite the slice-pairing code. |
| `mergeElementDefinitions` (399), `chooseProp` (533), `union` (619), `isLower` (634), `checkPatternValues` (657) | ch05 | Per-property merge semantics (a second, distinct merge table from ElementDefnMerger's). |
| `mergeTypes` (543), `specialises` (606), `findJointProfile` (595) | ch07 (+ch05) | Type/profile reconciliation between two bases. |
| `getMatchingSlice` (246), `getDiscriminatorValue` (268), `valuesMatch` (306/317), `slicingIsConsistent` (327), `describeDiscriminators` (346) | ch06 (+ch12) | Slice identity across two bases via discriminator values; large unimplemented surface → ch12. |
| `supplementMissingDiffElements` (1102), `insertMissingSparseElements` (1120), `findParent` (1159), `isSibling` (1166), `isChild` (1171), `makeTail` (1175) | ch02 (+ch10) | Sparse-diff completion incl. root synthesis; synthesized ids at 1148–1149 → ch10. |
| `trimSnapshot` (1183) | ch12 / n-a | No in-clone caller; user-data-driven snapshot reduction — record as configuration/tooling surface. |
| All "Not done yet"/"Not supported yet"/FHIRException paths (see §6) | ch12 | Error handling + unimplemented-feature boundary. |

Net: the file feeds ch06 and ch02 as predicted for job 1, but job 2 (additional bases) is a substantial ch05/ch03/ch07 contributor that the prediction missed.

## 6. Notable landmarks

- **Class javadoc caveats** (41–51): explicitly flags extension-slicing problems and merge sophistication as open risks.
- **Unused field** `typeNames` (125).
- **Unconditional call site**, no flag: ProfileUtilities.java:825; diff is cloned at 824 ("we're sometimes going to hack the differential"); user data `SNAPSHOT_GENERATED_IN_SNAPSHOT` cleared just before (820–821).
- **Bail-out on nested slicing** (713–723): if any `sliceStuff` element itself has non-extension slicing, logs `UNSUPPORTED_SLICING_COMPLEXITY` and **`return`s from `processSlices` entirely** — skipping merge for ALL slicings in the profile *and* the final `markExtensions` pass, not just the offending one.
- **Backwards iteration** over slicings when merging (726) — innermost/last slicings handled first.
- **Injection marker**: `UserDataNames.SNAPSHOT_PREPROCESS_INJECTED` set on injected elements (802); id synthesized by string-replacing slicer id with slice id (799).
- **"modiferExtension" typo** (1078) in `isExtensionSlicing`'s name list — modifierExtension slicing can never be recognized as standard extension slicing.
- **`isExtensionSlicing` ordered clause** (1081): `(!ed.getSlicing().hasOrdered() || ed.getSlicing().getOrdered())` disqualifies — so slicings that *omit* `ordered` (the common case) are NOT exempted as extension slicing; only an explicit `ordered=false` passes.
- **Suspicious operands** at 450: pattern+pattern branch passes `source.getFixed(), base.getFixed()` to `checkPatternValues` (both getters are for fixed, in the branch where neither side has fixed — presumably meant `getPattern()`).
- **Redundant condition** at 670: `p1.getValues().size() > 1 || p1.getValues().size() > 2`.
- **Fall-through hazard** in `getDiscriminatorValue` (289–302): case VALUE with a child lacking fixed and pattern falls through to `default: throw new Error("Not supported yet")`; child == null returns null.
- **Copy-paste error label** at 590: versioning mismatch error message reuses the `.aggregation` path suffix.
- **Unimplemented-territory throws** (ch12 register): "Not done yet" one-side-sliced merge (215, 217); discriminator types EXISTS/NULL/PATTERN/POSITION/PROFILE and TYPE-not-`$this` (274–288); both-sides binding merge (522); multiple type profiles (548); multi-valued pattern children (671, 679); `Error("Duplicate slice")` (261); both-sides targetProfile merge silently does nothing (579–580 empty branch).
- **`specialises` ignores impose/compliesWith** — "for now?" comment (607).
- **Additional-base machinery** keyed on `ExtensionDefinitions.EXT_ADDITIONAL_BASE` (137, 139); type mismatch between source and additional base is a hard `FHIRException("Type mismatch")` (145); i18n error family `SD_ADDITIONAL_BASE_INCOMPATIBLE_VALUES` / `SD_ADDITIONAL_BASE_INDETERMINATE_TYPE` / `SD_ADDITIONAL_BASE_NO_TYPE` (190, 364, 379, 435, 507, 569, 587, 590, 640, 653, 659, 664, 676).
- **Orphan publics**: `supplementMissingDiffElements` (1102) and `trimSnapshot` (1183) have no callers anywhere in the sparse clone (verified by grep); the clone excludes modules (validator, rendering) that may call them.
- **Checkstyle suppressions** for single-char `split("\\.")` at 849, 892, 895, 932, 1128, 1131.
- **Debug leftover**: `summary(List<ElementAnalysis>)` (882) uncalled.
