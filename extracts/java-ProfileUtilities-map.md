# ProfileUtilities.java — structure map (orientation pass)
Source: org.hl7.fhir.core @ commit b06c7ee (sparse clone 2026-08-21)
File: org.hl7.fhir.r5/src/main/java/org/hl7/fhir/r5/conformance/profile/ProfileUtilities.java
Generated: 2026-08-26 (Phase 3 packet 1)

File is 5054 lines. Class `ProfileUtilities` (line 128) is annotated `@MarkedToMoveToAdjunctPackage @Slf4j`. Header doc (109-125) lists key operations and carries the maintainer warning: *"Do not make modifications to the snapshot generation without first changing the snapshot generation test cases to demonstrate the grounds for your change"* (line 121).

Out of scope (rendering/HTML/CSV/schematron/example-generation, listed once, not inventoried in detail below): `generateSchematrons` 4074-4089, `generateCsv` 4148-4158, `addToCSV` 4161-4168, `Slicer` class 4171-4179, `generateSlicer` 4181-4196, `generateForChildren` 4198-4240, `getByPath` 4245-4253 (schematron helper), `ExampleValueAccessor` 4422-4425, `BaseExampleValueAccessor` 4427-4442, `ExtendedExampleValueAccessor` 4444-4466, `generateExamples` 4468-4479, `generateExample` 4481-4497, `createExampleElement` 4499-4521, `hasAnyExampleValues` 4523-4535 / 4538-4543, `getRowColor` 4920-4931 (+ ROW_COLOR_* constants 4914-4918), `describeExtensionContext` 3544-3567 (HTML string builder), plus large blocks of commented-out table-generation code (~3572-3576, 4380-4420).

---

## 1. Method inventory (snapshot-relevant)

### Nested types (state carriers, not methods)

| Type | Lines | Purpose |
|---|---|---|
| `ElementDefinitionCounter` (inner class) | 157-220 | Accumulates slice min/max sums per sliced path for post-generation cardinality checking (checkMin/checkMax/count/checkMinMax) |
| `AllowUnknownProfile` (enum) | 222-226 | NONE / NON_EXTNEIONS [sic] / ALL_TYPES — governs behavior on unresolvable type profiles. NOTE: comment says NONE is "the default" (223) but the field default is ALL_TYPES (448) |
| `SourcedChildDefinitions` (static class) | 308-333 | (source SD, element list, path) tuple returned by getChildMap |
| `ElementDefinitionResolution` (inner class) | 335-353 | (source SD, element) pair returned by getElementById for contentReference resolution |
| `ElementChoiceGroup` (static class) | 355-385 | Choice group parsed from an "a|b|c <= 1" constraint (name, mandatory, element names) |
| `ExtensionContext` (static class) | 389-426 | (SD, element) of an extension definition; getExtensionValueDefinition scans for `.value` child |
| `ElementDefinitionHolder` (static class) | 3672-3718 | Tree node for differential sorting (self, name, baseIndex, children, placeHolder) |
| `ElementDefinitionComparer` (static class, Comparator) | 3720-3812 | Orders differential siblings by base-snapshot index; `find()` 3755-3797 follows contentReferences with MAX_RECURSION_LIMIT guard; `checkForErrors` 3799-3811 |
| `ElementNameCompare` (static class, Comparator) | 4044-4070 | Path-then-sliceName comparator; normalizes `[x]` suffix (unused by generation main path) |
| `SliceList` (inner class) | 4278-4308 | Tracks active slice names per path prefix during id generation (`seeElement` 4282-4292, `analyse` 4294-4306) |

### Constructors & context access

| Method | Lines | Purpose |
|---|---|---|
| `ProfileUtilities(IWorkerContext, List<ValidationMessage>, ProfileKnowledgeProvider, FHIRPathEngine)` | 456-473 | Main ctor; null messages ⇒ `wantThrowExceptions=true`; creates FHIRPathEngine if null; pulls `parameters` from `context.getExpansionParameters()` |
| `ProfileUtilities(IWorkerContext, List<ValidationMessage>, ProfileKnowledgeProvider)` | 475-491 | Same without fpe parameter |
| `getContext()` | 304-306 | Returns IWorkerContext |
| `getFpe()` | 5050-5052 | Returns the FHIRPathEngine |
| `getPkp()` | 4903-4905 | Returns the ProfileKnowledgeProvider |

### Navigation / child-map utilities

| Method | Lines | Purpose |
|---|---|---|
| `getChildMap(profile, element, chaseTypes)` | 510-512 | Delegates to 4-arg overload with type=null |
| `getChildMap(profile, element, chaseTypes, type)` | 513-601 | Direct children of element in snapshot; resolves internal/external contentReference; optionally chases into type definition; cached in `childMapCache`; multi-type fallback to `Element` per 2025-08-27 change (569-580) |
| `getSliceList(profile, element)` | 604-622 | All slices (same path) following a slicing entry |
| `findElementIndex(elements, element)` | 625-636 | indexOf with fallback match by id |
| `getChildList(profile, path, id)` | 645-647 | 3-arg overload (snapshot, no refs) |
| `getChildList(profile, path, id, diff)` | 649-651 | 4-arg overload |
| `getChildList(profile, path, id, diff, refs)` | 653-704 | Core: direct children by path/id from diff or snapshot; optional contentReference following |
| `getChildList(structure, element, diff, refs)` | 706-708 | ED-based overload |
| `getChildList(structure, element, diff)` | 710-712 | ED-based overload |
| `getChildList(structure, element)` | 714-726 | ED-based; resolves internal contentReference target first |
| `getSiblings(list, current)` | 2339-2349 | Elements with matching path after current (`[x]`-aware via pathMatches) |
| `getById(list, baseId)` | 1586-1593 | Find ED by id |
| `getElementInCurrentContext(path, list)` | 1189-1199 | Backwards scan for path within current parent context |
| `head(path)` | 1201-1203 | Path up to and including last '.' |
| `tail(path)` | 3581-3588 | Last path segment |
| `getUrlFor(sd, c)` | 3512-3520 | Finds `.url` child element of an extension element (schematron use, but generic) |

### Main generation entry point & direct support

| Method | Lines | Purpose |
|---|---|---|
| `generateSnapshot(base, derived, url, webUrl, profileName)` | 740-1097 | THE entry point: validation preamble (741-776), re-entrancy/circularity guard, recursive base-snapshot generation (767), preprocessing, delegation to ProfilePathProcessor (839), specialization second pass (842-867), prohibited-type-slice type removal (869-881), diff-matched-to-snapshot verification (908-948), R4 mapping/constraint-source hack (949-968), slice min/max accounting (976-1036), profile/type consistency sweep (1038-1077), cleanup/finally (1078-1096) |
| `findLastChildForParent(path, element)` | 1099-1109 | Insertion point after last child of parent path |
| `getPathForElement(parentPath, element)` | 1111-1118 | Index of element with exact path |
| `destInfo(ed, snapshot)` | 1121-1129 | Debug: where a diff element landed in snapshot |
| `findTypeSlice(list, i, path, typeCode)` | 1131-1139 | Find later type-slice element matching path+type |
| `pathMatches(path, ed)` | 1141-1153 | Path equality with `[x]`-renamed match |
| `typeMatches(ed, typeCode)` | 1155-1157 | Single-type code equality |
| `checkTypeParameters(base, derived)` | 1159-1180 | Validates EXT_TYPE_PARAMETER type-parameter compatibility (walks derived's type up base chain) |
| `makeXVer()` | 1182-1187 | Lazy XVerExtensionManager creation |
| `findInheritedObligationProfiles(derived)` | 1205-1215 | Populates `obligationProfiles` from EXT_OBLIGATION_INHERITS extensions |
| `handleError(url, msg)` | 1217-1219 | addMessage with ERROR severity |
| `addMessage(msg)` | 1221-1226 | Appends to messages; throws DefinitionException if ERROR && wantThrowExceptions |
| `copyInheritedExtensions(base, derived, webUrl)` | 1228-1253 | Copies SD-level extensions from base unless NON_INHERITED; per-extension action (add/overwrite/ignore/defer) via EXT_SNAPSHOT_BEHAVIOR |
| `getExtensionAction(url)` | 1255-1261 | Reads EXT_SNAPSHOT_BEHAVIOR off the extension's own SD; default "defer" |
| `addInheritedElementsForSpecialization(snapshot, focus, type, path, url, weburl)` | 1263-1283 | For specialization: appends type's snapshot children under new element; merges root constraints/extensions into focus |
| `walksInto(list, ed)` | 1285-1288 | Does next diff element descend into ed |
| `fixTypeOfResourceId(base)` | 1290-1295 | R4+: normalize Resource.id type in snapshot+differential |
| `fixTypeOfResourceId(list)` | 1297-1307 | Sets Resource.id type to `http://hl7.org/fhirpath/System.String` + EXT_FHIR_TYPE "id" |
| `checkDifferentialBaseType(derived)` | 1317-1325 | Errors if first diff element (root) has a type; optionally auto-clears it (wantFixDifferentialFirstElementType) unless LOGICAL |
| `typeMatchesAncestor(type, baseDefinition, src)` | 1327-1330 | Root diff type equals base SD's type? |
| `checkGroupConstraints(derived)` | 1333-1342 | Removes children of choice-group elements zeroed out by a mandated sibling |
| `checkForChildrenInGroup(derived, toRemove, element)` | 1344-1377 | Applies max=0 to non-mandated members of a choice group; `throw new Error("huh?")` at 1353 |
| `getChildren(derived, element)` | 1379-1397 | Direct non-slice children in snapshot |
| `addAllChildren(derived, element, toRemove)` | 1399-1405 | Recursive collection for removal |
| `checkDifferential(elements, type, url)` | 1413-1461 | Validates diff paths: presence, root prefix, name charset/length rules, `[x]` placement |
| `isCompatibleType(base, sdt)` | 1464-1480 | Profile type compatible with stated type via base-chain walk |
| `cloneDiff(source)` | 1483-1491 | Deep-copies differential; links copies to originals via SNAPSHOT_diff_source user data |
| `cloneSnapshot(source, baseType, derivedType)` | 1493-1508 | Copies base snapshot renaming root type in id/path (specialization rebasing) |
| `constraintSummary(ed)` | 1510-1519 | Debug string |
| `sliceSummary(ed)` | 1522-1538 | Debug string |
| `typeSummaryWithProfile(ed)` | 1554-1571 | Debug string |
| `updateConstraintSources(ed, url)` | 1595-1602 | Fills missing constraint.source |
| `getListOfTypes(e)` | 1604-1610 | Set of type codes |
| `getTypeForElement(differential, diffCursor, profileName, diffMatches, outcome, webUrl, srcSD)` (package-private) | 1612-1631 | Resolves the single type SD to walk into when diff descends below an element; errors on 0 or >1 non-Reference types |
| `sliceNames(diffMatches)` | 1633-1641 | Comma list of slice names |
| `isMatchingType(sd, types, inner)` | 1643-1668 | SD (or its base chain / inner element) matches one of the type refs |
| `isMatchingType(test, desired)` | 1670-1679 | Any type-code intersection |
| `isValidType(t, base)` | 1681-1692 | Diff type exists in base types (logs on workingCode-only match) |
| `checkNotGenerating(sd, role)` | 1694-1698 | Throws if sd.isGeneratingSnapshot() — re-entrancy guard |
| `isBaseResource(types)` | 1700-1710 | True unless any type is "Resource" (note inverted-looking logic) |
| `determineFixedType(diffMatches, fixedType, i)` (package-private) | 1712-1729 | Infers type-slice type from slice name or single type; throws on ambiguity |
| `chooseMatchingBaseSlice(baseSlices, type)` | 1732-1739 | Base type-slice for a type |
| `findBaseSlices(list, start)` | 1742-1758 | Collects BaseTypeSlice entries following a slicing element in base snapshot |
| `getWebUrl(dt, webUrl)` | 1761-1774 | Web root for a datatype's page ("this is a hack" comment 1763) |
| `descED(list, index)` | 1776-1778 | Debug presenter |
| `rootName(cpath)` | 1782-1785 | Tail without `[x]` |
| `determineTypeSlicePath(path, cpath)` | 1788-1793 | Rewrites type-slice path onto current context head |
| `isImplicitSlicing(ed, path)` | 1796-1803 | Diff renames `foo[x]` to `fooType` without explicit slicing |
| `diffsConstrainTypes(diffMatches, cPath, typeList)` | 1806-1849 | Detects `[x]` type-constraining diff sets; fills TypeSlice list (by explicit type, slice name, or path rename) |
| `redirectorStack(redirector, outcome, path)` | 1852-1857 | Pushes an ElementRedirection (contentReference walk state) |
| `getByTypeName(type, t)` | 1860-1867 | Filter TypeRefs by working code |
| `replaceFromContentReference(outcome, tgt)` | 1870-1874 | Swap contentReference for target's types |
| `baseWalksInto(elements, cursor)` | 1877-1883 | Base cursor descends below previous element |
| `fillOutFromBase(profile, usage)` | 1886-1945 | Copy profile ED, backfilling ~25 properties from usage where absent (extension-slice reconstitution) |
| `checkExtensionDoco(base)` | 1948-1961 | For `.extension`/`.modifierExtension` elements: resets doc text to generic "An Extension" (task 3970 comment at 1949); returns isExtension |
| `pathTail(diffMatches, i)` | 1964-1969 | ".name[profile]" debug/key string |
| `markDerived(outcome)` | 1972-1975 | Flags all constraints SNAPSHOT_IS_DERIVED |
| `summarizeSlicing(slice)` (static, package-private) | 1978-2001 | Human-readable slicing summary |
| `updateFromBase(derived, base, baseProfileUrl)` | 2004-2020 | Sets derived.base (path/min/max) from base's `.base` or base itself; records SNAPSHOT_BASE_MODEL/PATH user data |
| `pathStartsWith(p1, p2)` | 2023-2025 | startsWith with `[x].` tolerance |
| `pathMatches(p1, p2)` | 2027-2029 | Equality with `[x]` rename tolerance |
| `fixedPathSource(contextPath, pathSimple, redirector)` | 2032-2049 | Rewrites datatype-internal path into source coordinates (redirector-aware) |
| `fixedPathDest(contextPath, pathSimple, redirector, redirectSource)` | 2051-2071 | Rewrites into destination coordinates |
| `getProfileForDataType(type, webUrl, src)` | 2073-2093 | Resolve type.profile[0] (xver fallback incl. on-the-fly generateSnapshot at 2080) else fetchTypeDefinition |
| `getProfileForDataType(type)` | 2095-2100 | fetchTypeDefinition with warn |
| `typeCode(types)` (static) | 2102-2114 | Debug type list string |
| `isDataType(types)` | 2117-2126 | All types are datatypes/primitives |
| `updateURLs(url, webUrl, element, processRelatives)` | 2135-2176 | Absolutizes `#`-relative valueSet/profile/targetProfile refs; rewrites relative markdown links in definition/comment/requirements/meaningWhenMissing/binding.description/extensions |
| `processRelativeUrls(markdown, webUrl, basePath, resourceNames, baseFilenames, localFilenames, processRelatives)` (static) | 2179-2287 | Markdown link scanner; guesses which relative links target the core spec (GDG history comments 2241-2263) |
| `issLocalFileName(url, localFilenames)` (static) | 2289-2298 | Link prefix in local filenames |
| `isLikelySourceURLReference(url, resourceNames, baseFilenames, localFilenames, baseUrl)` (static) | 2301-2337 | Heuristic: relative link points at core spec (uses BASE_FILENAMES) |
| `updateFromSlicing(dst, src)` | 2351-2370 | Merge slicing: ordered, union of discriminators (string equality), rules |
| `orderMatches(diff, base)` | 2372-2374 | ordered compatibility |
| `discriminatorMatches(diff, base)` | 2376-2385 | diff discriminators extend base's (prefix match) |
| `matches(c1, c2)` | 2387-2389 | Discriminator type+path equality |
| `ruleMatches(diff, base)` | 2392-2395 | Slicing rules compatibility (open ⊇ anything; openAtEnd vs closed allowed) |
| `isSlicedToOneOnly(e)` | 2397-2399 | Sliced with max=1 |
| `isTypeSlicing(e)` | 2401-2405 | Single discriminator type:$this |
| `makeExtensionSlicing()` | 2408-2414 | Standard url/VALUE, unordered, open slicing |
| `isExtension(currentBase)` | 2416-2418 | Path ends `.extension`/`.modifierExtension` |
| `hasInnerDiffMatches(context, path, start, end, base, allowSlices)` (package-private) | 2420-2442 | Diff contains children below path within window (comment 2438: "not sure why we get here...") |
| `getDiffMatches(context, path, start, end, profileName)` | 2444-2484 | THE diff matcher: segment-wise path equality with `[x]` base-name tolerance within [start,end] |
| `isSameBase(p, sp)` | 2487-2489 | `[x]` vs renamed-type segment equivalence |
| `findEndOfElement(differential, cursor)` | 2491-2499 | Last index of descendants in diff |
| `findEndOfElement(snapshot, cursor)` | 2501-2507 | Last index of descendants in snapshot |
| `findEndOfElementNoSlices(snapshot, cursor)` | 2509-2515 | As above, stopping at slices |
| `unbounded(definition)` | 2517-2526 | max not 0/1 |
| `updateFromObligationProfiles(base)` | 2529-2582 | Injects obligation extensions, mustSupport, additional bindings from matched obligation-profile elements |
| `updateFromDefinition(dest, source, pn, trimDifferential, purl, srcSD, derivedSrc, path, mappings, fromSlicer)` | 2585-3128 | THE per-property merge: extension policy (2606), profile doc override for Resource/Extension profiles (2619-2688), then property-by-property merge with DERIVATION_EQUALS tracking — sliceName 2690, short 2694, definition 2703, comment 2712, label 2721, requirements 2730 (sdf-9 2738-2742), alias 2744, min 2757 (slice exempt from min rule 2759), max 2768, fixed 2779, pattern 2788, example incl. EXT_ED_SUPPRESS deletion 2798-2827, maxLength 2829, maxValue 2838, minValue 2847, mustSupport+obligations 2859-2880, mustHaveValue 2882, valueAlternatives 2893, isModifier/isModifierReason (extensions only) 2908-2927, binding incl. required-strength guard + valueset subset expansion check + additional bindings 2929-3037, isSummary 3039 (1.4.0 workaround 3041), type incl. checkTypeDerivation 3053-3080, mappings.merge 3082, constraints (cumulative) 3084-3098, condition 3099-3103, unbindable-binding removal (task 8477) 3105-3108, fixed/pattern type check 3121-3126 |
| `getVersionResolutionRules(element)` | 3130-3132 | Delegates to ExtensionUtilities |
| `mergeMarkdown(dest, source)` | 3134-3150 | "..." prefix = append-to-base semantics; merges translation extensions |
| `mergeStrings(dest, source)` | 3152-3168 | Same for StringType |
| `findMatchingExtension(res, extensionToMatch)` | 3170-3184 | By url; translation extensions matched by lang |
| `markExtensionSource(extension, overrideSource, srcSD)` (static) | 3186-3197 | Stamps SNAPSHOT_EXTENSION_SOURCE user data; sets obligation-source sub-extension |
| `updateExtensionsFromDefinition(dest, source, destSD, srcSD)` | 3199-3217 | Removes NON_INHERITED + overridden DEFAULT_INHERITED; adds/overrides per NON_OVERRIDING/OVERRIDING lists (else duplicates are appended, 3214) |
| `mergeAdditionalBinding(dest, source)` | 3219-3235 | Union usages; copy any/shortDoco/documentation (note 3225-3227: `source.setAny(true)` looks like a bug — sets source not dest) |
| `hasUsage(dest, tgt)` | 3237-3247 | UsageContext membership (uses `matches()` regex on code — flagged by checkstyle suppression 3239-3241) |
| `getMatchingAdditionalBinding(nb, ab)` | 3249-3256 | Match by valueSet+purpose when no usage |
| `mergeExtensions(tgt, src)` | 3258-3260 | addAll |
| `checkTypeDerivation(purl, srcSD, base, derived, ts, path, specialising)` | 3262-3315 | Derived type must match a base type (or descend from abstract/logical base type); targetProfile chain check; copies type-level must-support/pattern/obligation extensions (3292-3293); throws DefinitionException unless suppressIgnorableExceptions |
| `sdConformsToTargets(path, dPath, url, rules, td)` | 3318-3340 | Target profile conforms via baseDefinition chain or EXT_SD_IMPOSE_PROFILE |
| `checkTypeOk(dest, ft, sd, fieldName)` | 3342-3360 | fixed/pattern value type is among element types |
| `hasBindableType(ed)` | 3362-3377 | Coding/CodeableConcept/Quantity/uri/string/code/CodeableReference or EXT_BINDING_STYLE / EXT_TYPE_CHARACTERISTICS "can-bind" |
| `isLargerMax(derived, base)` | 3380-3388 | Cardinality comparison with `*` |
| `checkSubset(expBase, expDerived)` | 3391-3399 | Derived expansion ⊆ base expansion |
| `checkCodesInExpansion(codes, contains, expansion)` | 3402-3409 | Recursive membership collection |
| `inExpansion(cc, contains)` | 3412-3422 | Recursive system+code membership |
| `closeDifferential(base, derived)` | 3424-3438 | Adds max=0 diff entries for base children not mentioned; recurses into sliced elements; re-sorts |
| `closeChildren(base, edb, derived, edm)` | 3440-3460 | Recursive closing within matched windows |
| `findEnd(list, ed, cursor)` | 3462-3468 | End of descendants |
| `getMatchInDerived(ed, list)` | 3471-3478 | Path match in diff |
| `getMatchInDerived(ed, list, start, end)` | 3480-3488 | Windowed path match |
| `isImmediateChild(ed)` | 3491-3498 | Depth-1 path |
| `isImmediateChild(candidate, base)` | 3500-3508 | Direct child of base path |
| `getElementById(source, elements, contentRefElement)` | 3524-3541 | Resolve contentReference (`#id` local or `url#id` cross-SD) to (SD, ED) |
| `isDataType(value)` | 3590-3597 | COMPLEXTYPE+SPECIALIZATION (hardcoded fallback list when SDs unavailable) |
| `isConstrainedDataType(value)` | 3599-3605 | COMPLEXTYPE+CONSTRAINT (fallback: SimpleQuantity/MoneyQuantity) |
| `baseType(value)` | 3607-3614 | Type name of constrained datatype |
| `isPrimitive(value)` | 3617-3623 | PRIMITIVETYPE (hardcoded fallback list) |
| `getProfile(source, ref)` | 3642-3668 | Resolve `#contained` / `url#contained` / plain url to SD |
| `sortDifferential(base, diff, name, errors, errorIfChanges)` | 3815-3869 | Builds ElementDefinitionHolder tree, sorts siblings by base-snapshot order, serializes back; detects count changes |
| `compareDiffs(diffList, newDiff, errors)` | 3871-3885 | Reports first out-of-order element |
| `processElementsIntoTree(edh, i, list)` | 3888-3908 | Diff list → tree, inserting placeholder parents for skipped levels |
| `sortElements(edh, cmp, errors)` | 3910-3928 | Recursive sibling sort; single-child special case 3911-3913 |
| `getComparer(cmp, child)` | 3930-4002 | Chooses the comparer/snapshot for a child's subtree: same SD, Resource-profile, Extension-profile, single-type datatype, `[x]`-renamed type, Reference, or Element fallback |
| `resolveType(code, src)` | 4004-4012 | Absolute-url type code → type name |
| `sdNs(type)` (static) | 4014-4016 | Delegates |
| `sdNs(type, overrideVersionNs)` (static) | 4018-4025 | Type name → canonical SD url |
| `isAbstract(code)` | 4028-4030 | Element/BackboneElement/Resource/DomainResource |
| `writeElements(edh, list)` | 4033-4039 | Tree → list, skipping placeholders |
| `findProfileStr(ref, source)` | 4091-4093 | String overload |
| `findProfile(ref, source)` | 4095-4145 | Central SD resolver: strips `#fragment`, splits `url|version`, applies `default-profile-version` / `force-profile-version` / `check-profile-version` parameters (4109-4139), drops source-package pinning for core (4141-4143), then context.fetchResource |
| `setIds(sd, checkFirst)` | 4256-4267 | Generates element ids for differential and snapshot (only if missing when checkFirst) |
| `hasMissingIds(list)` | 4270-4276 | Any ED without id |
| `generateIds(list, name, type, srcSD)` | 4310-4324 | Per-element id generation pass (comment 4322 notes an unimplemented second fix-up pass) |
| `generateIdForElement(list, name, type, srcSD, ed, sliceInfo, replacedIds, idList)` | 4326-4364 | Builds `path:slice.path:slice` id, `_`→`-` fixChars, duplicate-id error, absolutizes local contentReference (4359-4363) |
| `getUrlForSource(type, srcSD)` | 4367-4373 | Logical models keep own url; else core SD url |
| `fixChars(s)` | 4375-4377 | `_` → `-` |
| `urlTail(profile)` (static) | 4387-4389 | Last url segment |
| `populateLogicalSnapshot(sd)` | 4546-4556 | Naive logical-model snapshot: root + base snapshot children + own diff children, re-prefixed |
| `copyElements(sd, list)` | 4559-4567 | Re-prefix child paths onto logical root |
| `cleanUpDifferential(sd)` | 4570-4573 | Public wrapper |
| `cleanUpDifferential(sd, start)` | 4575-4619 | Injects missing slicing entries for repeated diff paths; recursion per level |
| `determineSlicing(slicer, slices)` | 4622-4645 | Auto-names slices (`slice-N`), hardcodes discriminators for .extension / DiagnosticReport.result / Observation.related / Bundle.entry, else `throw new Error` (4644) |
| `interpretR2Discriminator(discriminator, isExists)` (static) | 4648-4660 | R2 `@pattern/@profile/@type/@exists` string → discriminator component |
| `makeDiscriminator(dType, str)` (static) | 4663-4665 | Empty path → `$this` |
| `buildR2Discriminator(t)` (static) | 4668-4677 | Reverse mapping; throws FHIRException on unrepresentable |
| `makeExtensionForVersionedURL(context, url)` (static) | 4680-4721 | Synthesizes an extension SD for an `http://hl7.org/fhir/<ver>/StructureDefinition/extension-Type.path` url from a SMART template (hardcoded snapshot indices 4715-4717) |
| `makeBaseDefinition(FHIRVersion)` / `makeBaseDefinition(String)` (static) | 4777-4779 / 4780-4808 | Synthesizes the abstract `Base` SD (root of the R5 type hierarchy) |
| `readChoices(ed, children)` | 4820-4829 | ElementChoiceGroups from constraints |
| `processConstraint(children, c)` | 4831-4884 | FHIRPath-parses `(a | b | c) = 1` / `<= 1` constraints into a choice group |
| `checkExtensions(outcome)` | 5003-5009 | Strip NON_INHERITED_ED_URLS extensions from element + binding |
| `markExtensions(ed, overrideSource, src)` (static) | 5011-5023 | markExtensionSource across element/binding/type extensions |
| `hasObligations(sd)` (static) | 5025-5040 | Any EXT_OBLIGATION_CORE on SD/elements/types |
| `isExtensionDefinition(sd)` (static) | 4933-4935 | CONSTRAINT on type Extension |
| `isSimpleExtension(sd)` / `isComplexExtension(sd)` (static) | 4945-4951 / 4953-4959 | Extension.value prohibited or not |
| `isModifierExtension(sd)` (static) | 4961-4964 | Root isModifier |
| `isResourceBoundary(ed)` (static) | 4978-4980 | Single type "Resource" |
| Getters/setters (config surface, see §4) | 493-508, 4724-4774, 4810-4817, 4886-4901, 4937-4943, 4966-4972, 4974-4976, 4982-4995, 4999-5001, 5042-5048 | See §4 |

---

## 2. Call graph from generateSnapshot

Legend: `→ [PPP]` / `→ [PRE]` = control transfers to ProfilePathProcessor / SnapshotGenerationPreProcessor (mapped by other agents). Trivial debug-string helpers (typeSummaryWithProfile, sliceSummary, constraintSummary, destInfo) omitted below the first mention.

```
generateSnapshot (740)
├─ checkNotGenerating (747, 748, 766)
├─ findProfile (763)                                — resolve base's base when base has no snapshot
├─ generateSnapshot (767)                           — RECURSIVE: generate base's snapshot first
├─ fixTypeOfResourceId (769) → fixTypeOfResourceId(list) (1297)
├─ checkTypeParameters (771)
│   └─ findProfile (1175)
├─ [snapshotStack circularity check 774-776; setGeneratingSnapshot 777]
├─ checkDifferential (791)
├─ checkDifferentialBaseType (792)
│   └─ typeMatchesAncestor (1319) → findProfile
├─ copyInheritedExtensions (808)
│   ├─ getExtensionAction (1231)
│   └─ processRelativeUrls (1238, 1246)
├─ findInheritedObligationProfiles (810) → findProfile
├─ cloneDiff (824)
├─ new SnapshotGenerationPreProcessor(this).process(diff, derived) (825)   → [PRE] HANDOFF
├─ cloneSnapshot (831)                              — SPECIALIZATION only
├─ new MappingAssistant(...) (837)                  — external class (mapping merge)
├─ ProfilePathProcessor.processPaths(this, base, derived, url, webUrl, diff, baseSnapshot, mappingDetails) (839)   → [PPP] HANDOFF (the whole element-matching walk)
├─ checkGroupConstraints (841)
│   └─ checkForChildrenInGroup (1338)
│       ├─ getChildren (1345)
│       ├─ readChoices (1346) → processConstraint (4823)
│       ├─ tail (1351, 1369)
│       └─ addAllChildren (1372) → getChildList (recursive)
├─ [SPECIALIZATION second pass 842-867]
│   ├─ getElementInCurrentContext (846) → head
│   ├─ updateFromDefinition (849)                   — see subtree below
│   ├─ findLastChildForParent (851) → getPathForElement
│   ├─ updateURLs (852) → processRelativeUrls → isLikelySourceURLReference / issLocalFileName
│   ├─ markExtensions (854) → markExtensionSource
│   ├─ walksInto (856)
│   └─ addInheritedElementsForSpecialization (860)
│       ├─ updateURLs, markExtensions / markExtensionSource
├─ [prohibited type-slice type removal 869-881]
│   └─ findTypeSlice (874) → pathMatches / typeMatches
├─ mappingDetails.update() (884)                    — MappingAssistant
├─ setIds (886)
│   ├─ hasMissingIds (4257, 4262)
│   └─ generateIds (4260, 4265)
│       └─ generateIdForElement (4320)
│           ├─ SliceList.seeElement / analyse
│           ├─ fixChars (4344)
│           └─ getUrlForSource (4361)
├─ [diff→snapshot verification loop 908-948]
│   └─ addMessage (926) / handleError (947)
├─ [R4 mapping-trim + constraint-source absolutization hack 949-968]
├─ [ensure .base on all elements for SPECIALIZATION 969-975]
├─ [slice min/max accounting 976-1036]              — ElementDefinitionCounter; addMessage
├─ [profile/type consistency sweep 1038-1077]
│   ├─ findProfile (1043)
│   ├─ makeXVer / xver.getDefinition (1045-1047)
│   ├─ isCompatibleType (1068) → findProfile
│   └─ addMessage / handleError
├─ [catch: wipe snapshot, rethrow 1078-1085; finally: pop stack 1086-1090]
└─ [EXT_VERSION_BASE stamp 1091-1093; setGeneratedSnapshot 1094; SNAPSHOT_GENERATED_MESSAGES 1096]
```

### updateFromDefinition subtree (called from generateSnapshot:849 AND from ProfilePathProcessor)

```
updateFromDefinition (2585)
├─ checkExtensionDoco (2592)
├─ updateExtensionsFromDefinition (2606, 2934) → markExtensionSource
├─ [obligation element injection 2608-2614]
├─ findProfile (2622, 2626)                          — slice/type profile doc override
├─ makeXVer / xver (2628-2639)
│   └─ generateSnapshot (2638)                       — RECURSIVE: xver extension snapshot
├─ processRelativeUrls (2658, 2661)
├─ mergeMarkdown (2705, 2714, 2732) / mergeStrings (2723) → findMatchingExtension
├─ addMessage (min/max/mustSupport/mustHaveValue/binding/subset errors)
├─ isLargerMax (2770)
├─ mergeExtensions (2866)
├─ getVersionResolutionRules (2961, 2962)
├─ context.expandVS / validateCode (2968-2980)       — binding subset check
├─ checkSubset (2994) → checkCodesInExpansion → inExpansion
├─ markExtensionSource (3008, 3035, 3066, 3076)
├─ getMatchingAdditionalBinding (3020) → mergeAdditionalBinding → hasUsage
├─ checkTypeDerivation (3057)
│   ├─ findProfile (3280)
│   └─ sdConformsToTargets (3298) → recursive; findProfile-like fetchResourceRaw
├─ mappings.merge (3082)                             — MappingAssistant
├─ hasBindableType (3106)
└─ checkTypeOk (3122, 3125)
```

### Callback surface used by ProfilePathProcessor (not reachable from generateSnapshot inside this file)

NOTE: this list is inferred from protected/package-private visibility and the design of the split (ProfilePathProcessor/SnapshotGenerationPreProcessor were not read in this pass) — exact call sites to be confirmed against the ProfilePathProcessor map. ProfilePathProcessor drives the base/diff cursor walk and calls back into these ProfileUtilities members (protected/package-private/public): `getDiffMatches` (2444), `hasInnerDiffMatches` (2420), `findEndOfElement` ×2 (2491, 2501), `findEndOfElementNoSlices` (2509), `updateFromBase` (2004), `updateFromDefinition` (2585), `updateFromSlicing` (2351), `updateFromObligationProfiles` (2529), `makeExtensionSlicing` (2408), `isExtension` (2416), `isSlicedToOneOnly` (2397), `isTypeSlicing` (2401), `orderMatches` (2372), `discriminatorMatches` (2376), `ruleMatches` (2392), `unbounded` (2517), `fillOutFromBase` (1886), `markDerived` (1972), `updateURLs` (2135), `updateConstraintSources` (1595), `getProfileForDataType` (2073/2095), `getTypeForElement` (1612), `determineFixedType` (1712), `diffsConstrainTypes` (1806), `determineTypeSlicePath` (1788), `isImplicitSlicing` (1796), `rootName` (1782), `pathTail` (1964), `pathStartsWith` (2023), `fixedPathSource` (2032), `fixedPathDest` (2051), `redirectorStack` (1852), `getByTypeName` (1860), `replaceFromContentReference` (1870), `getElementById` (3524), `baseWalksInto` (1877), `findBaseSlices` (1742), `chooseMatchingBaseSlice` (1732), `isBaseResource` (1700), `isValidType` (1681), `isMatchingType` (1643), `sliceNames` (1633), `getListOfTypes` (1604), `getById` (1586), `descED` (1776), `getWebUrl` (1761), `checkExtensions` (5003), `getSiblings` (2339), `tail` (3581), `isPrimitive` (3617), `findProfile` (4095), `addMessage` via handleError, `getContext`, `isNewSlicingProcessing`/`isAutoFixSliceNames`/`isDebug` getters. SnapshotGenerationPreProcessor similarly uses `getDiffMatches`, `findEndOfElement`, `sortDifferential`-adjacent helpers, and `getContext`.

### Secondary public entry points (independent of generateSnapshot)

- `sortDifferential` (3815) → processElementsIntoTree (3888) → sortElements (3910) → getComparer (3930) → resolveType/sdNs/isAbstract/findProfile → writeElements (4033) → compareDiffs (3871). Also invoked by closeDifferential (3437).
- `closeDifferential` (3424) → getMatchInDerived / closeChildren / findEnd / isImmediateChild → sortDifferential.
- `setIds` (4256) — also called inside generateSnapshot (886).
- `populateLogicalSnapshot` (4546) → findProfile, copyElements.
- `cleanUpDifferential` (4570) → determineSlicing (4622).
- `generateExamples` (4468), `generateSchematrons` (4074), `generateCsv` (4148) — out of scope.
- Static factories: `makeBaseDefinition` (4777/4780), `makeExtensionForVersionedURL` (4680), R2 discriminator converters (4648-4677).

---

## 3. Method → chapter mapping

| Chapter | Methods (lines) |
|---|---|
| **ch02 differential preprocessing** | checkDifferential 1413; checkDifferentialBaseType 1317 (+typeMatchesAncestor 1327); cloneDiff 1483; sortDifferential 3815 (+compareDiffs 3871, processElementsIntoTree 3888, sortElements 3910, getComparer 3930, writeElements 4033, ElementDefinitionHolder 3672, ElementDefinitionComparer 3720, resolveType 4004, sdNs 4014/4018, isAbstract 4028); cleanUpDifferential 4570/4575 (+determineSlicing 4622); closeDifferential 3424 (+closeChildren 3440, findEnd 3462, getMatchInDerived 3471/3480, isImmediateChild 3491/3500); handoff to SnapshotGenerationPreProcessor at 825; interpretR2Discriminator 4648 / makeDiscriminator 4663 / buildR2Discriminator 4668 (legacy input normalization) |
| **ch03 base resolution / rebasing / root element** | generateSnapshot preamble 740-839 (base checks, recursive base generation 762-768, snapshotStack); findProfile 4095 / findProfileStr 4091 / getProfile 3642 (canonical resolution incl. version parameters); cloneSnapshot 1493 (specialization root rename); fixTypeOfResourceId 1290/1297; copyInheritedExtensions 1228 (+getExtensionAction 1255); updateURLs 2135 (+processRelativeUrls 2179, issLocalFileName 2289, isLikelySourceURLReference 2301, BASE_FILENAMES 130); getWebUrl 1761; makeBaseDefinition 4777/4780; checkNotGenerating 1694; root type-on-first-element error 882-883 |
| **ch04 element matching** | getDiffMatches 2444 (+isSameBase 2487); hasInnerDiffMatches 2420; findEndOfElement 2491/2501; findEndOfElementNoSlices 2509; pathStartsWith 2023; pathMatches 2027 / 1141; baseWalksInto 1877; walksInto 1285; getSiblings 2339; getElementInCurrentContext 1189 (+head 1201); findLastChildForParent 1099 (+getPathForElement 1111); getChildMap 510/513; getChildList 645-726; getSliceList 604; findElementIndex 625; diff↔snapshot verification loop 908-948 |
| **ch05 per-property merge semantics** | updateFromDefinition 2585-3128 (the whole merger); updateFromBase 2004; fillOutFromBase 1886; mergeMarkdown 3134; mergeStrings 3152; findMatchingExtension 3170; mergeExtensions 3258; mergeAdditionalBinding 3219 (+hasUsage 3237, getMatchingAdditionalBinding 3249); isLargerMax 3380; checkSubset 3391 (+checkCodesInExpansion 3402, inExpansion 3412); hasBindableType 3362; checkTypeOk 3342; checkTypeDerivation 3262 (+sdConformsToTargets 3318); markDerived 1972; updateConstraintSources 1595; checkExtensionDoco 1948; getVersionResolutionRules 3130; MappingAssistant handoffs 837/884/3082 |
| **ch06 slicing** | updateFromSlicing 2351; orderMatches 2372; discriminatorMatches 2376 (+matches 2387); ruleMatches 2392; isSlicedToOneOnly 2397; isTypeSlicing 2401; makeExtensionSlicing 2408; isImplicitSlicing 1796; diffsConstrainTypes 1806; determineFixedType 1712; determineTypeSlicePath 1788; rootName 1782; findBaseSlices 1742 (+BaseTypeSlice external); chooseMatchingBaseSlice 1732; findTypeSlice 1131; sliceNames 1633; slice min/max accounting 976-1036 (+ElementDefinitionCounter 157); prohibited type-slice removal 869-881; slice-without-slicing / duplicate-slice-name checks 1023-1034; SliceList 4278; summarizeSlicing 1978; getSliceList 604 |
| **ch07 type-profile & extension expansion** | getProfileForDataType 2073/2095; getTypeForElement 1612; isValidType 1681; isMatchingType 1643/1670; isCompatibleType 1464; getByTypeName 1860; isDataType 2117/3590; isConstrainedDataType 3599; baseType 3607; isPrimitive 3617; isExtension 2416; isBaseResource 1700; profile-doc override block in updateFromDefinition 2619-2688; final profile/type consistency sweep 1038-1077; makeXVer 1182; makeExtensionForVersionedURL 4680; xver snapshot generation 2080/2638; AllowUnknownProfile enforcement 2676-2687; isExtensionDefinition 4933; isSimpleExtension 4945; isComplexExtension 4953; isModifierExtension 4961; ExtensionContext 389; updateFromObligationProfiles 2529; findInheritedObligationProfiles 1205; hasObligations 5025 |
| **ch08 contentReference handling** | getElementById 3524 (+ElementDefinitionResolution 335); replaceFromContentReference 1870; getChildMap contentReference branch 523-549; getChildList refs branch 678-693; getChildList(structure, element) target resolution 714-726; redirectorStack 1852 (+ElementRedirection external); fixedPathSource 2032; fixedPathDest 2051; contentReference absolutization in generateIdForElement 4359-4363; ElementDefinitionComparer.find contentReference following 3770-3788 |
| **ch09 logical models & interfaces** | populateLogicalSnapshot 4546 (+copyElements 4559); checkTypeParameters 1159 (EXT_TYPE_PARAMETER); logical-model exemptions in checkDifferentialBaseType 1321 and root-type check 882; getUrlForSource 4367; addInheritedElementsForSpecialization 1263; specialization second pass 842-867; SPECIALIZATION .base backfill 969-975; abstract/logical type matching in checkTypeDerivation 3276-3282 |
| **ch10 element ids & Base component** | setIds 4256; hasMissingIds 4270; generateIds 4310; generateIdForElement 4326 (+SliceList 4278, fixChars 4375); updateFromBase 2004 (.base path/min/max); SPECIALIZATION .base backfill 969-975; makeBaseDefinition 4777/4780 (Base type root); urlTail 4387 |
| **ch11 recursion & circularity** | snapshotStack 436, push/pop/check 774-778, 1089; checkNotGenerating 1694 + call sites 747/748/766; setGeneratingSnapshot lifecycle 777, 1083, 1088; recursive generateSnapshot 767/2080/2638; MAX_RECURSION_LIMIT 387 used in ElementDefinitionComparer.find 3786-3787; childMapCache 447 (re-entrancy note 432: "ProfileUtilities are used re-entrantly internally, so nothing with process state can be here") |
| **ch12 error handling & configuration/settings** | addMessage 1221 (throw-on-error switch); handleError 1217; wantThrowExceptions logic 459-463/4990-4995/4724-4731; suppressIgnorableExceptions 154/4982-4988 + use 3312; messages list 435/4974/4990; getMessages 4974; all setters in §4; exception cleanup in generateSnapshot catch/finally 1078-1090; checkGroupConstraints 1333 (Error throws); ValidationMessage severity policy (forPublication toggling ERROR vs INFORMATION at 1003); setIgnorableError usages 1003/1026/1032 |

---

## 4. Configuration surface

### Instance flags/properties affecting snapshot behavior

| Field | Decl line | Default | Setter (line) | Getter (line) | Effect |
|---|---|---|---|---|---|
| `debug` | 431 | false | setDebug 4760 | isDebug 4755 | Extra diff-match debugging (2477-2482), sort error reporting (3916-3918) |
| `messages` | 435 | new ArrayList | setMessages 4990 (also forces wantThrowExceptions=false) | getMessages 4974 | Sink for ValidationMessages; passing null in ctor ⇒ throw mode |
| `wantThrowExceptions` | 452 | false (true if ctor messages==null, 459-463) | setThrowException 4729 | isThrowException 4724 | addMessage(ERROR) throws DefinitionException (1223-1225) |
| `terminologyServiceOptions` | 439 | new ValidationOptions(R5) | setTerminologyServiceOptions 4739 | get 4734 | Terminology ops options (declared; binding checks build fresh options at 2980) |
| `newSlicingProcessing` | 440 | false | setNewSlicingProcessing 4749 (fluent) | isNewSlicingProcessing 4744 | Consumed by ProfilePathProcessor (slicing algorithm variant); not read inside this file |
| `defWebRoot` | 441 | null (defaults to webUrl at 786-787) | setDefWebRoot 4770 (appends '/') | getDefWebRoot 4765 | Fallback web root for datatype pages (1767) |
| `autoFixSliceNames` | 442 | false | setAutoFixSliceNames 505 (fluent) | isAutoFixSliceNames 501 | Consumed by ProfilePathProcessor (slice-name repair); not read inside this file |
| `xver` | 443 | null (lazy 1182-1187) | setXver 4814 (fluent) | getXver 4810 | Cross-version extension manager |
| `wantFixDifferentialFirstElementType` | 444 | false | set 497 | is 493 | Auto-clear type on root diff element when it matches ancestor (1319-1320) |
| `masterSourceFileNames` | 445 | null | set 4890 | get 4886 | Relative-URL heuristics in processRelativeUrls |
| `localFileNames` | 446 | null | set 4899 | get 4895 | Relative-URL heuristics (local links NOT rebased) |
| `childMapCache` | 447 | empty HashMap | — | — | getChildMap memoization |
| `allowUnknownProfile` | 448 | **ALL_TYPES** (enum comment 223 claims NONE is default — discrepancy) | setAllowUnknownProfile 4941 | get 4937 | Throw vs warn on unresolvable type/extension profiles (2678-2685) |
| `mappingMergeMode` | 449 | MappingMergeModeOption.APPEND | — (no setter in this file) | — | Passed to MappingAssistant (837) |
| `forPublication` | 450 | false | setForPublication 4970 | isForPublication 4966 | Slice-min overflow reported as ERROR vs INFORMATION (1003) |
| `obligationProfiles` | 451 | empty list | — (filled by findInheritedObligationProfiles 1205) | — | Obligation merging in updateFromDefinition / updateFromObligationProfiles |
| `suppressedMappings` | 453 | empty list | setSuppressedMappings 5046 | get 5042 | Passed to MappingAssistant (837) |
| `parameters` | 454 (Lombok @Getter @Setter) | context.getExpansionParameters() (471, 489) | Lombok | Lombok | `default-profile-version` / `force-profile-version` / `check-profile-version` handling in findProfile (4109-4139) |
| `snapshotStack` | 436 | empty list | — | — | Circularity detection (774-776) |
| `fpe` | 434 | ctor arg or new FHIRPathEngine | — | getFpe 5050 | Constraint parsing in processConstraint |
| `pkp` | 437 | ctor arg | — | getPkp 4903 | ProfileKnowledgeProvider (mostly rendering; passed through) |
| `propertyCache` | 4997 | empty | — | getCachedPropertyList 4999 | Element-model property cache (external users) |

### Static

| Field | Line | Default | Access |
|---|---|---|---|
| `suppressIgnorableExceptions` | 154 | false | isSuppressIgnorableExceptions 4982 / setSuppressIgnorableExceptions 4986 — suppresses the illegal-constrained-type DefinitionException (3312-3314) |
| `MAX_RECURSION_LIMIT` | 387 | 10 | contentReference loop guard in sorter (3786) |
| `COPY_BINDING_EXTENSIONS` | 428 | false | binding merge keeps/clears base binding extensions (3003-3005) |
| `DONT_DO_THIS` | 429 | false | dead switch for legacy type-match workarounds (commented use ~3285-3290) |
| `BASE_FILENAMES` | 130-153 | ~200 core-spec page names | isLikelySourceURLReference (2333) |
| `NON_INHERITED_ED_URLS` | 232-249 | list | Extensions stripped in inherited profiles (1230, 1276, 3200, 3033, 5004-5006) |
| `DEFAULT_INHERITED_ED_URLS` | 251-257 | list | Inherited but replaced when the source redefines them (3200) |
| `NON_OVERRIDING_ED_URLS` | 262-275 | list | Diff occurrences ignored when dest already has them (3205) |
| `OVERRIDING_ED_URLS` | 280-302 | list | Diff occurrences overwrite ancestor value (3210) |
| `UD_ERROR_STATUS` / STATUS_* / ROW_COLOR_* | 4908-4918 | — | Rendering only |

### Exception types thrown

- `org.hl7.fhir.exceptions.DefinitionException` — dominant type for definitional errors (no base profile 742, no type 751/754, no derivation 757, type mismatch 760, base not found 765, circular 775, contentReference failures 535/541/548, getChildMap type errors 568-592, snapshot empty 2652, unknown extension/profile 2679/2684, illegal constrained type 3313, getTypeForElement 1618-1629, missing path in generateIds 4329, schematron preconditions…). Also thrown by addMessage (1224) for any ERROR-severity message when wantThrowExceptions.
- `org.hl7.fhir.exceptions.FHIRException` — path validity errors in checkDifferential (1418-1455), checkNotGenerating (1696), type-slice conditions (1721/1726), sorter resolution failures (3937/3960/3965/3997), profile version conflict (4134), findLastChildForParent internal error (1103), xver bad references (2631-2635), buildR2Discriminator (4675), logical base not found (4552).
- `org.hl7.fhir.exceptions.FHIRFormatError` — declared by fillOutFromBase (1886).
- `java.lang.Error` — used for internal/should-not-happen states: getSliceList misuse (606), null element (663), type on first snapshot element (883), wrong path root (1021), first-diff-element type (1322), "huh?" (1353), two mandatory in group (1362), contentReference in wrong context (1616), recursion guard (3787), sorter unhandled cases (3975/3980/3988), unknown base type (3613), no slicing rule in determineSlicing (4644).

### Error-message patterns

- Most messages go through `context.formatMessage(I18nConstants.*)` — i18n constants are the stable identifiers (e.g. `CIRCULAR_SNAPSHOT_REFERENCES_DETECTED_...` 775, `TYPE_ON_FIRST_DIFFERENTIAL_ELEMENT` 1322, `ILLEGAL_PATH__IN_DIFFERENTIAL_*` family 1425-1455, `STRUCTUREDEFINITION__AT__ILLEGAL_CONSTRAINED_TYPE__FROM__IN_` 3313, `SD_TYPE_PARAMETER_*` 1162-1178, `UNABLE_TO_RESOLVE_NAME_REFERENCE__AT_PATH_` 548, `SAME_ID_ON_MULTIPLE_ELEMENTS__IN_` 4356).
- Hardcoded English (not i18n) for several validator messages: "No match found for X in the generated snapshot..." (925), "...don't have a matching element in the snapshot..." (935), slice min/max messages (1001/1008/1013), "launches straight into slicing" (1024), "Duplicate slice name" (1030), "derived min ... cannot be less than" (2760), "derived max ... cannot be greater than" (2771), "Illegal constraint [must-support = false]..." (2873), "illegal attempt to change the binding" (2958), binding located/expanded/subset messages (2964-2996), "The type of profile X cannot be checked..." (1051), "The profile X has type Y which is not consistent..." (1062/1070).
- `ValidationMessage.setIgnorableError(true)` marks the soft slice-accounting errors (1003, 1026, 1032).
- Message locations use `"StructureDefinition.differential.element[i]"` / `"StructureDefinition.snapshot.element[i]"` index expressions (926, 1003, 1010, 1015, 1026, 1032, 1051).

---

## 5. Notable landmarks

### Extension URLs used by generation logic
- The four policy lists: NON_INHERITED_ED_URLS (232), DEFAULT_INHERITED_ED_URLS (251), NON_OVERRIDING_ED_URLS (262), OVERRIDING_ED_URLS (280) — tools/hl7 binding, json/xml serialization hints, questionnaire extensions etc.
- `ExtensionDefinitions.EXT_TYPE_PARAMETER` (770, 1160-1164) — logical-model type parameters.
- `EXT_SNAPSHOT_BEHAVIOR` (1257) — per-extension inheritance action: add/overwrite/ignore, default "defer".
- `EXT_OBLIGATION_INHERITS_NEW/OLD`, `EXT_OBLIGATION_PROFILE_FLAG_NEW/OLD` (1206-1209), `EXT_OBLIGATION_CORE/TOOLS` (2539, 2610, 3190, 5026-5034), `EXT_OBLIGATION_SOURCE(_SHORT)` (3191-3193).
- `EXT_VERSION_BASE` (1092) — snapshot stamped with base SD version.
- `EXT_FHIR_TYPE` (1302-1303) — Resource.id System.String/"id" fix.
- `EXT_PROFILE_ELEMENT` (1058-1060, 2672) — profiled-element indirection.
- `EXT_ED_SUPPRESS` (2801) — example suppression, incl. `$all` label (2802).
- `EXT_TRANSLATABLE` (2602), `EXT_TRANSLATION` (3172) — translation merge.
- `EXT_BINDING_ADDITIONAL` (2564, 2938), `EXT_BINDING_STYLE` (3368), `EXT_TYPE_CHARACTERISTICS` "can-bind" (3371-3372), `EXT_EXP_TOOCOSTLY` (2974-2975).
- `EXT_SD_IMPOSE_PROFILE` (3333) — target-profile conformance via imposeProfile.
- Literal URLs in checkTypeDerivation: `elementdefinition-type-must-support`, `elementdefinition-pattern`, `obligation` (3292-3293).
- `http://hl7.org/fhirpath/System.String` (1301).
- xver template: `http://fhir-registry.smarthealthit.org/StructureDefinition/capabilities` (4700).

### Known-bug workarounds / hacks (with comments in source)
- 569-580 (getChildMap): multi-type walk-in returns `Element` instead of throwing — behavior change dated 2025-08-27, prior code threw; comment describes both unresolved design problems.
- 664-665: element id nullability check disabled — "in some corner cases it's not, and in those cases, we don't care".
- 949-968: "hack around a problem in R4 definitions (somewhere?)" — trims mapping.map whitespace and absolutizes constraint.source to `http://hl7.org/fhir/StructureDefinition/...`.
- 1054-1056: `Bundle.entry.response.outcome` hard-coded to expect OperationOutcome.
- 1763: getWebUrl "this is a hack, but it works for now, since we don't have deep folders".
- 2241-2263 (processRelativeUrls): long comment on guessing relative refs to core spec; GDG disabled 7-Dec-2021, re-enabled 11-Feb-2022 with `processRelatives` parameter (davinci-dtr/SDC $assemble case).
- 2438: hasInnerDiffMatches — "not sure why we get here, but returning false at this point makes a bunch of tests fail".
- 2466-2473 (getDiffMatches): commented-out out-of-order warning — "raises warnings when profiling inherited elements... Might be better done when we're sorting the profile?".
- 2601-2605: "hack workaround for problem in R5 snapshots" — removes duplicate EXT_TRANSLATABLE extension.
- 2643-2648: profile override "we're kind of hacking things here... sometimes want the details from the profile to override... sometimes not".
- 2673: "todo: should we change down the profile_element if there's one?".
- 2856-2857: "todo: what to do about conditions?".
- 2906-2907: "profiles cannot change: isModifier, defaultValue, meaningWhenMissing / but extensions can change isModifier".
- 2923-2926: modifier extensions get a default isModifierReason string.
- 3041: isSummary conflict tolerated for version "1.4.0" — "work around a known issue with some 1.4.0 cosntraints [sic]".
- 3050-3051: contentReference/type coexistence "would make sense but blows up the process later... sort out the business rule elsewhere".
- 3084: "todo: constraints are cumulative. there is no replacing".
- 3105 (task 8477): binding deleted when no bindable type remains.
- 3110-3119: commented-out extension copy — "no, we already did.".
- 3225-3227 (mergeAdditionalBinding): `if (source.getAny()) { source.setAny(true); }` — no-op on source; looks like it should be `dest.setAny(true)` (candidate finding).
- 1948-1949 (task 3970): extension elements get generic doco instead of inherited definitional text; exempts `II.extension` base (CDA).
- 3779-3781: legacy contentReference style `#parameter` (2016May) handled in sorter.
- 4322-4323 (generateIds): "second path - fix up any broken path based id references" — comment with no code.
- 4634-4644 (determineSlicing): "right now, we hard code this..." — fixed discriminators for 4 paths, Error otherwise.
- 4715-4717 (makeExtensionForVersionedURL): fixed snapshot element indices 3 and 4 mutated on a copied template.
- 1095: `derived.setUserData(SNAPSHOT_GENERATED...)` commented out — replaced by setGeneratedSnapshot(true) (1094); messages stashed in user data SNAPSHOT_GENERATED_MESSAGES (1096) "used by the publisher".
- 930: diff/snapshot elements deliberately cross-linked via user data ("note: this means diff/snapshot are cross-linked").
- 223 vs 448: AllowUnknownProfile doc/default discrepancy (NONE documented as default; ALL_TYPES actual).

### User-data keys (UserDataNames.*) driving cross-phase state
SNAPSHOT_GENERATED_IN_SNAPSHOT (821, 848, 853, 921, 2586), SNAPSHOT_diff_source (913, 1488), SNAPSHOT_DERIVATION_EQUALS (916, pervasive in updateFromDefinition), SNAPSHOT_DERIVATION_POINTER (918, 2591), SNAPSHOT_DERIVATION_DIFF (930), SNAPSHOT_IS_DERIVED (1974, 3086), SNAPSHOT_BASE_MODEL / SNAPSHOT_BASE_PATH (2005-2006), SNAPSHOT_EXTENSION_SOURCE (3187), SNAPSHOT_auto_added_slicing (998), SNAPSHOT_SORT_ed_index (3818, 3880), SNAPSHOT_slice_name (4626), render_webroot (2655), UD_ERROR_STATUS (4908).

### Other structural landmarks
- Base.setCopyUserData(true) wraps the whole generation (779-780, 1087) — user-data propagation through element copies is load-bearing.
- Generation is guarded by both `snapshotStack` (URL list, 436) and per-SD `isGeneratingSnapshot()` flag; failed generation nulls the snapshot (1082) so no half-generated snapshot leaks.
- `checkGroupConstraints`/`processConstraint` (1333/4831) implement FHIRPath-based choice-group pruning — a rarely documented behavior where `(a | b | c) = 1` constraints cause sibling max=0 zeroing.
- The class is stated to be re-entrant (432) — all per-run state lives in generateSnapshot locals, user data, or the derived SD, except `snapshotStack`, `messages`, `obligationProfiles` (shared across nested generations).
