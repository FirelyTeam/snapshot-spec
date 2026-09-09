# Java ch05 deep-read: updateFromDefinition + MappingAssistant + preprocessor additional-base merge (Phase 3 packet J-b, 2026-09-01)

Source: `C:\Git\snapshot-spec-materials\org.hl7.fhir.core`, clone commit **b06c7ee** (master 2026-08-21).
Files: `ProfileUtilities.java` (PU), `ProfilePathProcessor.java` (PPP), `SnapshotGenerationPreProcessor.java` (PRE),
`MappingAssistant.java` (MA), `org.hl7.fhir.utilities/Utilities.java` (UTIL, via `git grep HEAD`, blob fetch OK),
`org.hl7.fhir.r5/model/ElementDefinition.java` (MODEL, via `git grep HEAD`).

All line numbers verified against b06c7ee this session.

## 1. Entry point and signature

`updateFromDefinition(ElementDefinition dest, ElementDefinition source, String pn, boolean trimDifferential,
String purl, StructureDefinition srcSD, StructureDefinition derivedSrc, String path, MappingAssistant mappings,
boolean fromSlicer)` — PU:2585-3128 (protected).

Naming trap: internally `base = dest` (the working snapshot element, a clone of the base snapshot element
already processed by `updateFromBase`) and `derived = source` (the differential element) — PU:2589-2590. The
**differential element is mutated too** (trim branches null out properties; `SNAPSHOT_DERIVATION_EQUALS` user
data stamped on diff props that equal the base). PU:824 `cloneDiff` comment: the differential is a clone, user
data is migrated back afterwards — so the marks are downstream-visible, the nulled properties are clone-only.

Call sites (all in-package; method is protected, package swept):

| Site | Context | trimDifferential | fromSlicer | srcSD | derivedSrc |
|---|---|---|---|---|---|
| PPP:373 | simple path, 1 diff match | `isTrimDifferential()` (always false, PPP:139/176) | false | base SD | derived SD |
| PPP:813 | slice content merge | false | `slicerElement != null` (PPP:675) | base SD | derived SD |
| PPP:1246 | sliced-base: merging the diff's **slicing entry** | **`closed`** (base slicing rules == CLOSED, PPP:1227) | false | base SD | derived SD |
| PPP:1396 | sliced-base: new slice from reslice template | false | false | base SD | derived SD |
| PU:849 | SPECIALIZATION leftovers (diff elements never consumed by processPaths, merged into existing snapshot elements) | false | false | base SD | derived SD |

### trimDifferential semantics (carried question from J-a — resolved)

The parameter's *intent* (per the per-property pattern) is: when a diff property deep-equals the base, **delete
it from the differential** instead of marking it `SNAPSHOT_DERIVATION_EQUALS`. It is **hard-false everywhere**
except PPP:1246, where the *closed-slicing flag* is passed into it (comment: "if there's no slice, we don't want
to update the unsliced description" — reads like parameter abuse; J-a memory cite "PPP:1335" was wrong, correct
is PPP:1246). Net effect on the **snapshot**: none (trim branches only touch the diff clone, and only when values
are equal). Net effect on the differential clone: equal-valued props of a slicing-entry diff are stripped when
the base slicing is closed. Effectively a **near-dead parameter** within snapshot generation.

Trim-branch copy-paste asymmetry: for short/definition/min/max/etc. the trim branch does `derived.setX(null)`;
for **comment (PU:2716), label (PU:2725), requirements (PU:2734)** it instead does `base.setX(derived.getX().copy())`
— wrong object (harmless: values are equal in that branch, and trim is near-dead anyway).

## 2. Prologue (before per-property merging), PU:2586-2688

1. `source.setUserData(SNAPSHOT_GENERATED_IN_SNAPSHOT, dest)`; `derived.setUserData(SNAPSHOT_DERIVATION_POINTER, base)` (PU:2586, 2591).
2. `checkExtensionDoco(base)` (PU:2592 → PU:1948-1961): if dest path is `Extension` root / `*.extension` /
   `*.modifierExtension` (and Base.path ≠ `II.extension`), **wipe definitional text on dest before merging**:
   definition="An Extension", short="Extension", comment/requirements null, alias+mapping cleared (task 3970).
   Returns `isExtension`, which later **gates isModifier merging** (see §3).
3. Obligation profiles (Java-only surface, DEV-032): collect same-id elements from each obligation profile
   snapshot (PU:2593-2599); copy their obligation extensions onto dest (PU:2608-2614); later fold their
   mustSupport (PU:2860-2870) and additional bindings (PU:2936-2954) in.
4. R5-snapshot hack: if dest carries the `translatable` extension **twice**, remove the second (PU:2601-2605).
5. `updateExtensionsFromDefinition(dest, source, ...)` (PU:2606 → PU:3199-3217) — the ED-level extension merge, see §4.
6. **Profile-root override** (PU:2619-2688): resolve a profile — for a named slice, the base's single
   type.profile (PU:2621-2623); else the source's first type.profile (PU:2624-2626), with xver fallback for
   cross-version extension urls (PU:2628-2640: BadVersion/Invalid/Unknown → throw; Valid → xver definition,
   snapshot generated on the fly). Non-Extension/non-Resource/non-Logical kinds → profile discarded with
   `msg=false` (PU:2643-2648, in-code comment admits "we're kind of hacking things here").
   If the profile is a RESOURCE kind or Extension type (PU:2650-2671): the **profile root's**
   definition (relative-url-processed), binding.description, short (unconditionally, even to empty!),
   comment, requirements replace dest's; dest alias and mapping lists are **cleared and replaced** by the
   profile root's. I.e. for extension slices / resource-profile references, the referenced profile's descriptive
   text beats the base *before* the diff applies on top.
   Unresolvable profile (PU:2672-2688): warning log; throws DefinitionException unless
   `allowUnknownProfile` permits (Extension: only ALL_TYPES suffices; other types: anything but NONE).

## 3. Per-property table (PU:2689-3127)

Standard pattern per property: `if (derived.hasX) { if (!deepEqual(derived.X, base.X)) apply; else if (trim)
null-out-diff; else mark diff X SNAPSHOT_DERIVATION_EQUALS }`. "apply" is per the table. deepEqual =
`Base.compareDeep(..., false)` (fixed uses `true` = exact).

| Property | Java behavior | Cite |
|---|---|---|
| sliceName | replace (unconditional when present) | PU:2690-2692 |
| short | replace | PU:2694-2701 |
| definition | replace via `mergeMarkdown` — "..." append + extension merge | PU:2703-2710, 3134-3150 |
| comment | replace via `mergeMarkdown` | PU:2712-2719 |
| label | replace via `mergeStrings` — "..." append **with swapped operands = broken** (see §5) | PU:2721-2728, 3152-3168 |
| requirements | replace via `mergeMarkdown`; then **sdf-9 enforcement**: requirements stripped from BOTH diff and dest on root elements (path without ".") | PU:2730-2742 |
| alias | add-if-absent union (by string value) | PU:2744-2755 |
| min | **diff wins**; ERROR message when `derived.min < base.min` unless diff has sliceName ("in a slice, minimum cardinality rules do not apply"); illegal value still taken | PU:2757-2766 |
| max | **diff wins**; ERROR message when larger than base (`isLargerMax` PU:3380-3388, `*`-aware); illegal value still taken | PU:2768-2777 |
| fixed[x] | **wholesale replace** (copy; exact compare `compareDeep(...,true)`); post-merge type check, see below | PU:2779-2786 |
| pattern[x] | **wholesale replace** | PU:2788-2796 |
| example | union keyed on (label AND value) both-deepEqual; `EXT_ED_SUPPRESS` extension honored **always** (no setting), incl. `"$all"` label wildcard = remove all inherited examples | PU:2798-2827 |
| maxLength | replace, no narrowing check | PU:2829-2836 |
| maxValue[x] | replace, no narrowing check | PU:2838-2845 |
| minValue[x] | replace, no narrowing check | PU:2847-2854 |
| condition | union by id value (at the end of the routine) | PU:3099-3103 |
| mustSupport | obligation-profile-aggregated (any true → true, extensions union PU:3258-3260 = blind addAll); ERROR on true→false **unless fromSlicer**; diff still wins | PU:2859-2880 |
| mustHaveValue | replace; ERROR on true→false unless fromSlicer | PU:2882-2892 |
| valueAlternatives | add-if-absent union (canonical value) | PU:2893-2904 |
| isModifier / isModifierReason | **merged only when `isExtension`** (checkExtensionDoco); otherwise a diff isModifier is **silently ignored** (frozen enforced by omission). For modifier extensions without a reason, boilerplate reason auto-filled | PU:2906-2927 |
| binding | rebuild-with-sub-property-overlay + required-lattice/subset enforcement — see §6 | PU:2929-3037 |
| isSummary | change with base present → **`throw new Error(...)` — generation aborts** (except context version 1.4.0); else replace | PU:3039-3048 |
| type | **wholesale replace** after per-type `checkTypeDerivation`; whitelisted extension copy-down from matched base type — see §7 | PU:3053-3080 |
| mapping | `MappingAssistant.merge()` — see §8 | PU:3082 |
| constraint | inherited constraints: mark `SNAPSHOT_IS_DERIVED`, stamp `source` = **base SD url** when absent (BEFORE diff appended); diff constraints appended **only if key not already present — restated key silently dropped entirely** ("// todo: constraints are cumulative. there is no replacing") | PU:3084-3098 |
| *(epilogue)* binding deleted if no remaining bindable type (task 8477) | `hasBindableType` PU:3362-3377: hardcoded list Coding/CodeableConcept/Quantity/uri/string/code/CodeableReference + `EXT_BINDING_STYLE` + type-characteristics `can-bind` | PU:3105-3108 |
| *(epilogue)* fixed/pattern type check | `checkTypeOk` PU:3342-3360: fixed/pattern fhirType must be among element type codes (root: SD type) else ERROR message | PU:3121-3126 |

### Frozen-by-omission (verified absent from PU:2585-3128 by regex sweep)

`code` (Coding list), `representation`, `orderMeaning`, `meaningWhenMissing`, `defaultValue[x]`,
`sliceIsConstraining`, `contentReference` (only a commented-out fragment PU:3050-3051), `slicing` (handled by
`updateFromSlicing` in PPP, ch6), element `id` (regenerated, ch10). A differential supplying any of these gets
them **silently dropped** — Java's implementation of the †/frozen rows (deliberate for
isModifier/defaultValue/meaningWhenMissing per comment PU:2906; collateral for `code`, which the spec's
§5.1.0.8 table says is add/removable). Contrast .NET: replace/union for every one of these.
(Code-derived; no shared test exercises diff-supplied `code` — carried-forward empirical check.)

## 4. ED-level extension merge — `updateExtensionsFromDefinition` (PU:3199-3217)

Applied to the element pair AND separately to binding (PU:2934). Four static policy lists (PU:232-302):

1. First, dest is purged of `NON_INHERITED_ED_URLS` (15 urls: binding-definition, no-binding, isCommonBinding,
   standards-status(+reason), category, fmm, implements, explicit-type-name, security-category, wg,
   normative-version, obligation-profile ×2, EXT_SUMMARY) and of `DEFAULT_INHERITED_ED_URLS` members
   (questionnaire-optionRestriction/referenceProfile/referenceResource/unitOption, mimeType) **when the diff
   redeclares that url** (PU:3200).
2. Then per diff extension (PU:3202-3216): url absent in dest → append copy. Present and in
   `NON_OVERRIDING_ED_URLS` (translatable, json/xml serialization tooling urls, type-specifier,
   defaulttype...) → diff ignored. Present and in `OVERRIDING_ED_URLS` (date-format, date-rules, designNote,
   allowedUnits, question, entryFormat, maxDecimalPlaces, maxSize, minLength, questionnaire-* UI set,
   display-hint, explicit-type-name) → **value overwritten in place**. Present and in **neither list** →
   **a second copy is appended — duplicate url in the snapshot** (PU:3213-3214).
3. `markExtensionSource` (PU:3186-3197) stamps SNAPSHOT_EXTENSION_SOURCE user data + obligation-source
   canonical sub-extension on obligation extensions.

Contrast .NET: union(url) with value overlay for ALL urls — no policy lists, no duplicate-append. .NET's
17-url non-inheritable blocklist (ch12/OQ-019) ≠ Java's 15-url NON_INHERITED list (overlapping but different).

## 5. The "..." append convention — Java side (OQ-010 answered)

`UTIL appendDerivedTextToBase(baseText, derivedText)` = `baseText + "\r\n" + derivedText.substring(3)`
(Utilities.java:1769-1775; null base → just strips marker). Byte-identical convention to .NET (CRLF, marker
`"..."` = 3 chars) — including the same separator.

- `mergeMarkdown(derived.X, base.X)` (definition/comment/requirements, PU:3134-3150): merged = diff copy;
  diff value empty → base value used (extension-only diff doesn't wipe text — same wrinkle as .NET
  ElementDefnMerger.cs:849, which credits "Java validator logic"); diff starts with "..." →
  `appendDerivedTextToBase(base, diff)` — **correct order**.
- `mergeStrings(derived.label, base.label)` (label only, PU:3152-3168): same shape BUT the append call is
  `appendDerivedTextToBase(res.getValue()=diffLabel, source.getValue()=baseLabel)` — **operands swapped**:
  result = `"...diffLabel" + CRLF + baseLabel.substring(3)`. The marker survives into the snapshot and the
  *base* text loses its first 3 characters. Upstream-issue candidate (parked, JI list).
- Property-level extension merge inside both helpers (PU:3141-3148/3159-3166 + findMatchingExtension
  PU:3170-3184): base-side extensions matched by url (translation matched by `lang` sub-extension — same as
  .NET) — but on match the **base's value overwrites the diff's** (`matchingExtension.setValue(sourceExtension
  .getValue())`), opposite direction to .NET's diff-wins. Behavior note.
- .NET applies append to definition/comment/requirements only, guarded by AppendedTextAnnotation against
  double-append; Java has no double-append guard (but merges each element once per generation, and diff
  DERIVATION_EQUALS marks don't interact).

## 6. Binding (PU:2929-3037)

1. Binding-presence is obligation-profile-aggregated (PU:2929-2932). Obligation-profile additional bindings
   with purpose ∉ {maximum, required, extensible} folded in — but the fold at PU:2947-2952 has an
   **inverted guard**: `if (binding.hasAdditional(ab)) add` (copies only when already present = duplicate or
   no-op; the parallel block at PU:2573-2578 correctly uses `!hasAdditional`). JI candidate.
2. Enforcement (only when diff binding ≠ base binding): base strength REQUIRED and diff strength ≠ REQUIRED →
   ERROR message (PU:2957-2958; the once-throw is commented out at 2959). Both REQUIRED with valueSets →
   full terminology **subset check**: resolve both VSs (WARNING if unresolvable), expand (WARNING if
   unexpandable), too-costly/1000-cap → WARNING "unable to check", else every derived code validated against
   base VS → ERROR "not a subset" listing offending codes (PU:2960-2999, checkSubset PU:3391-3399). No other
   lattice rows enforced (extensible/preferred/example changes pass silently).
3. Merge = **rebuild**: `nb = base.binding.copy()` (PU:3002); extensions **cleared**
   (`COPY_BINDING_EXTENSIONS = false`, PU:428/3003-3005) then diff's binding extensions added;
   **description reset to null** (PU:3006) then diff's taken if present — an inherited binding.description is
   **dropped** whenever the diff constrains the binding without restating it; strength/valueSet: diff wins if
   present, else inherited (PU:3010-3018). `additional`: matched by (valueSet, purpose, and diff item has no
   usage) → `mergeAdditionalBinding` (PU:3219-3235: usage union; `if (source.getAny()) source.setAny(true)` —
   **no-op self-assignment**, dest.any never updated — JI candidate; shortDoco/documentation replace);
   unmatched → append (not copied — same list instance).
4. Diff has NO binding at all: base binding kept, but its extensions purged of NON_INHERITED_ED_URLS (PU:3032-3036).
5. Epilogue: binding deleted when no type is bindable (task 8477; PU:3105-3108, hasBindableType PU:3362-3377).
   Contrast .NET: `ModelInspector.IsBindable` (ICoded interface) vs Java's hardcoded 7-type list + 2 extensions
   — the "lists don't agree" theme (DEV-022/OQ-019 adjacent).

Contrast .NET mergeBinding: overlay keeps base description when diff silent, keeps base extensions, unions
additional by exact value; no strength-lattice or subset enforcement at all.

## 7. Type (PU:3053-3080)

When diff type list ≠ base type list:
1. Per diff type, `checkTypeDerivation` (PU:3262-3315): the diff code must match a base type code directly, or
   the base type must be abstract/logical and the diff type's base-definition chain must reach the base type's
   type. No match → **DefinitionException throw** (unless suppressIgnorableExceptions). Matched with diff
   targetProfiles: each must conform to a base targetProfile via `sdConformsToTargets` (PU:3318-3340 — direct
   hit, version-stripped hit, base-definition chain, or `EXT_SD_IMPOSE_PROFILE` chain; unresolvable target →
   WARNING + pass); failure → ERROR message (not throw); **specialization exempt**.
2. Matched base type's extensions copied onto the diff type: `elementdefinition-type-must-support` only-if-new,
   `elementdefinition-pattern` + obligation urls (both) always (PU:3292-3293).
3. Then base type list **cleared and replaced by diff list verbatim** (copy; PU:3060-3068). So type.profile /
   targetProfile / aggregation / versioning always come from the diff item alone — inherited per-code
   profiles are NOT preserved (agrees with .NET's wholesale profile/targetProfile replace within matched
   codes; differs in that .NET item-merges and preserves base type extensions generally, Java copies only the
   whitelist in step 2).

## 8. Mapping — `MappingAssistant` (MA:1-279; DEV-017 enrichment)

Constructed once per generation (PU:837) with `mappingMergeMode` (PU:449, default **APPEND**; setter-configurable)
and `suppressedMappings` (PU:453/5046, list of mapping *URIs* to suppress; IG-tooling surface).

**SD-level reconciliation (constructor, MA:45-96):** derived SD's mapping declarations are canonical and
untouched. Each base SD declaration: suppressed-by-uri → excluded; matched in derived (by URI first — "if the
URIs match, they match, irregardless of anything else" — else by identity + compatible names) → derived's
declaration wins, base's name/uri/comment fill absent fields, identity mismatch recorded in `renames`
(base-identity → derived-identity); unmatched but identity collides with a derived declaration → base
declaration re-added under suffixed identity (`rim` → `rim1`) and rename recorded; else appended.

**Element-level merge (MA:173-188, called PU:3082 as `mappings.merge(derived, base)` — "note reversal of names
to be correct"):** result list = **diff's mappings first** (renames applied), then dest's inherited mappings
appended unless matched (`addMappings`, MA:190-213; `EXT_SUPPRESSED` boolean extension on the mapping item or
its SD-level declaration → dropped). Match = `compareMaps` (MA:233-262): identity equal AND map-text equal →
duplicate, skip. **R5+ only** (VersionUtilities.isR5Plus): identity equal, map different → merge-mode kicks in:
APPEND (default) = **comma-append** the inherited map text into the diff mapping's `map` (dedup by comma-split;
mergeMaps MA:264-277); IGNORE = inherited text replaces (sic — mode names read inverted: IGNORE overwrites the
diff's map with the base's, OVERWRITE keeps the diff's); DUPLICATE = both kept as separate items. **R4 and
earlier: identity+map both must match, else both kept** — exactly .NET's identity+map union (DEV-017 is
R5+-only divergence).

**Renames direction — likely inverted (needs verification, JI candidate):** `renames` maps *base-SD*
identities, and the element mappings carrying base identities are the *inherited* ones (in dest). But merge()
applies renames to the **diff's** list and passes `null` for the inherited list (MA:175-177) — in a collision
the diff's identity would be rewritten to the suffixed name while the inherited mapping keeps the stale
identity. No shared test exercises SD-level identity collisions (masked).

**Post-generation `update()` (MA:150-170, called at end of generateSnapshot):** SD-level mapping declarations
pruned to those referenced by at least one snapshot element mapping or declared in the derived SD itself.
Element map texts trimmed (MA:182-186). Contrast .NET: never touches SD-level `StructureDefinition.mapping`.

## 9. Preprocessor SECOND merge table — additional bases (PRE:399-531)

Context (PRE:134-176): when the derived SD carries `EXT_ADDITIONAL_BASE` extensions, each additional base SD's
**differential** is recursively preprocessed, sparse-filled, and merged into the profile's differential
(`mergeElementsFromAdditionalBase(srcWrapper=profile, ab=additional base)`); the merged list **replaces the
profile's differential** before path processing. This is a *differential × differential* merge (both inputs are
constraint sets on the same type) — a third merge table besides ch5's base×diff and the preprocessor's
slice-propagation fill-if-absent table (DEV-025). Structure walk (PRE:178-232): both sides walked in the
*type's* child order; both-present → merge; slices matched via `getMatchingSlice` else copied; one-side-only →
copied. Slicing must be discriminator-consistent else throw (PRE:189-192).

`mergeElementDefinitions(base, source, baseSD)` (PRE:399-531) — `source` = the profile's element, `base` = the
additional base's element; **source wins** all fill-if-absent choices:

| Property group | Semantics | Cite |
|---|---|---|
| path, slicing | from source only | PRE:401-404 |
| label, short, definition, comment, requirements, meaningWhenMissing, orderMeaning, mustHaveValue, mustSupport, isModifier(+Reason), isSummary | `chooseProp`: source if non-empty else base (fill-if-absent, no conflict detection — e.g. isModifier true/false conflicts silently resolve to source) | PRE:406-418, 533-541 |
| min | both present → **the LOWER wins** (`source.min < base.min ? source : base`) — looser, arguably inverted (intersection semantics require the higher) | PRE:420-424 |
| max | both present → the lower wins (`getMaxAsInt`, `*` = MAX_VALUE, MODEL:13160-13162) — stricter, correct | PRE:425-429 |
| fixed × fixed | must be deepEqual else **throw** | PRE:431-438 |
| fixed × pattern (either direction) | `checkPatternValues` — fixed must satisfy pattern (recursive; extra fixed props allowed via `extras=false`... see note), result = fixed | PRE:439-445 |
| pattern × pattern | `checkPatternValues(..., extras=true)` — **operand bug: passes `source.getFixed(), base.getFixed()` (both null in this branch) instead of the patterns** → NPE or silent misbehavior; known candidate from packet 1 | PRE:449-450 |
| one-side fixed/pattern | copied | PRE:442,447,452 |
| minValue | both → **higher** wins (stricter); `isLower` compares Quantity (unit-equal check is **inverted**: throws when units are EQUAL, PRE:638-642 — but see note), dateTime, decimal; else throw | PRE:455-459, 634-655 |
| maxValue | both → lower wins (stricter) | PRE:460-464 |
| maxLength | both → **the LARGER wins** (looser — inconsistent with maxValue's stricter-wins) | PRE:465-469 |
| alias, code, example, constraint, mapping | union (source items all; base items appended unless `equals` — Java object equality = deep for model types) | PRE:470-475, 619-632 |
| valueAlternatives | **intersection** when both present (else whichever exists) | PRE:477-496 |
| type | **intersection by working code**, matched pairs via `mergeTypes`; empty intersection → throw | PRE:498-518 |
| binding | both present → **`throw new Error("not done yet")`**; else whichever exists | PRE:520-527 |

`mergeTypes` (PRE:543-593): profile×profile → single-profile only (multi → throw "Not handled yet");
specialisation-aware: if one profile specialises the other, the more specialised survives; else search the
context for a **joint profile** whose baseDefinitions contain both (PRE:595-604) else throw.
targetProfile×targetProfile → **silently unhandled** (comment only, keeps t1's list as-is, PRE:579-580);
aggregation/versioning conflicts → throw.

`checkPatternValues` (PRE:657-687): fhirType must match; primitives must be value-equal; complex: recursive
per-child; `extras=false` → property present only in v2 throws; note dead clause at PRE:670
(`p1.getValues().size() > 1 || p1.getValues().size() > 2` — second disjunct unreachable; likely meant p2) —
multi-valued children under-supported. JI-candidate cluster.

Note PRE:426/466: `getMaxAsInt` handles `*` (MAX_VALUE) so no parse hazard.

## 10. Divergence summary vs .NET ElementDefnMerger (ch5 table deltas)

Where not listed, the two agree in outcome (short/definition/comment/requirements replace + "..." append;
alias/condition/valueAlternatives union; maxLength/min-max-Value replace unchecked; sliceName replace;
fixed/pattern effectively diff-wins in the common whole-value case; type-list wholesale replace = DEV-001
agreement Java-side).

1. **min/max illegal diffs**: .NET most-restrictive (silently ignores loosening); Java diff-wins + ERROR
   message → different snapshot outputs on illegal input. Empirically invisible on the 164-test corpus.
2. **isSummary**: .NET silent replace; Java hard `throw new Error` on change = generation ABORT (strongest
   generator-enforcement data point for OQ-011).
3. **isModifier**: .NET replace; Java frozen (silently ignored) except on extension elements.
4. **Frozen-by-omission set** (§3): code/representation/orderMeaning/meaningWhenMissing/defaultValue/
   sliceIsConstraining silently dropped by Java, merged by .NET.
5. **fixed/pattern partial overlay**: .NET overlays same/derived-type values (OQ-012); Java always wholesale
   replace + post-merge checkTypeOk type check.
6. **constraint**: .NET restated key → overlay-merge; Java restated key → **dropped**. Source stamping: .NET
   stamps every unstamped constraint AFTER merge with the *derived/type-profile* url (only when diff has ≥1
   constraint); Java stamps inherited constraints BEFORE merge with the *base SD's* url, always; diff-added
   constraints never stamped by Java (left as authored). Three-way DEV-002 contrast + RFC-009 Java answer.
7. **mapping**: R4- identical (identity+map union); R5+ Java comma-appends into one item per identity
   (APPEND default), configurable; MappingAssistant also rewrites SD-level mapping declarations + prunes
   unused ones (.NET never touches SD.mapping); suppression = SD-level EXT_SUPPRESSED (uri-list +
   per-declaration) vs .NET's setting-gated per-item elementdefinition-suppress.
8. **example**: key = label+value (Java) vs label (NET); suppression always-on (Java, incl. $all wildcard) vs
   RespectSuppressExtension setting (NET).
9. **binding**: Java enforces required-strength row + expansion-based subset check; drops inherited
   description on constrained bindings; clears base binding extensions (COPY_BINDING_EXTENSIONS=false); .NET
   overlay keeps both, enforces nothing. Bindable-type deletion: both do it, different type lists.
10. **extensions**: .NET union-by-url overlay; Java 4-list policy + duplicate-append default + inheritance
    blocklists differ.
11. **label "..."**: Java append broken (swapped operands); .NET has no label append at all.
12. **mustSupport true→false**: .NET silent; Java ERROR message (skipped for slices via fromSlicer).
13. **Profile-root override** (§2.6): Java pulls descriptive text from the referenced type profile/extension
    root; .NET analog is the ch7 root-merge (`:1464-1476`) — mechanisms differ (Java replaces text before diff;
    .NET merges the profile root as a full element).

## 11. Upstream-issue candidates from this packet (for upstream-java-issues.md)

- mergeStrings label append: swapped operands (PU:3156-3157) — marker survives, base text truncated.
- Obligation-profile additional-binding fold inverted guard (PU:2949 `hasAdditional` vs `!hasAdditional` at PU:2575).
- mergeAdditionalBinding any-flag self-assignment no-op (PU:3225-3227).
- MappingAssistant renames applied to the wrong mapping list (MA:175-177) — needs a repro to confirm.
- PRE additional-base: min picks lower (PRE:421), maxLength picks larger (PRE:466) — intersection semantics
  violated; pattern×pattern operand bug (PRE:450, known); dead clause PRE:670; isLower unit-check inverted
  (PRE:638-642: throws when units are equal, proceeds comparing values when units differ).
- Trim-branch copy-paste asymmetry (PU:2716/2725/2734) — cosmetic given trim is near-dead.
