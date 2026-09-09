# Upstream issue drafts — hapifhir/org.hl7.fhir.core (snapshot generation)

Java bugs found during the snapshot-spec reverse-engineering project (Phase 3 packets J-a 2026-08-31,
J-b/J-c/J-d 2026-09-01 deep-reads, plus packet-1 orientation candidates). **Filed: JI-1..13 + JI-17 (13
issues, #2584-#2597, 2026-09-01) + JI-14/18/19/21 (#2603/#2602/#2604/#2605, 2026-09-03). Held: JI-15, 16
(design questions → WGM brief Tier 3), JI-20 (needs custom-resource repro).**
All code citations are at commit `b06c7ee7c57367305ff72b6bb09ce779b5dbd6ce` (master, 2026-08-21) unless
marked; master `4f52ba6` (fetched 2026-09-01) has PU shifted +1 line from PU:222 onward, PPP/PRE unchanged;
master `f725fb7` (fetched 2026-09-03) has PU shifted +2 lines from PU:126 onward (e.g. PU:2611→2613,
PU:3082→3084, PU:4810→4836 after further insertions at PU:3522/4144), MA/PPP/PRE unchanged in the cited regions.
MA = `MappingAssistant.java` (same package as the others).

## Filing protocol

- **GATE LIFTED 2026-09-01: Ewout gave standing authorization to file Java snapshot-generator bugs
  ("you can keep posting them")** — bugs found in future packets can be filed as found, no re-ask
  needed. Only file **verified** items; needs-verification/needs-repro items stay held until resolved.
- **2026-09-03: the same standing authorization now covers .NET issues on `FirelyTeam/firely-net-sdk`**
  ("feel free to file .net issues too"). First use: DEV-031 → **firely-net-sdk#3597** (draft in
  `harness/repros/issue-dotnet-dev031-base-typed-children.md`). Same rules: verified items only, dup-check
  with `gh issue list --search` first.
- **FILED 2026-09-01** (12 drafts → 11 issues; all citations re-verified unchanged on master `4f52ba6`
  first — PRE byte-identical to b06c7ee, PU/PPP changes outside all cited regions):
  JI-1 → **#2584**, JI-2 → **#2585**, JI-4 → **#2586**, JI-6 → **#2587**, JI-7 → **#2588**,
  JI-8 → **#2589**, JI-9 → **#2590**, JI-10 → **#2591** (incl. the PRE:670 dead clause + isLower
  inverted unit guard), JI-12 → **#2592**, JI-13 → **#2593**, JI-3+JI-5 bundled → **#2594**.
  Issue bodies archived in nothing but the issues themselves (generated from the drafts below verbatim).
- **FILED 2026-09-01 (J-d):** JI-17 → **#2596** (all four `_HAS_CHILDREN__AND_MULTIPLE_TYPES` sites, master
  line numbers), JI-11 → **#2597** (AllowUnknownProfile comment vs default; verified in J-d: enforcement
  PU:2676-2687 never throws under the `ALL_TYPES` default, test driver defaults the same).
- **FILED 2026-09-03** (pre-WGM carry-in session; repros built with the harness `JavaRunner`, validator 6.10.2;
  dup-checked with `gh issue list --search` first; issue texts archived in `harness/repros/issue-*.md`):
  JI-18 → **#2602** (turned out to be a **crash**: `ext.getValue().copy()` on the complex `obligation`
  extension, PU:2611 → NPE for any diff row on an obligated element; repro `repros/ji18b-op3-plus-row-input.xml`
  = op3 + one `Patient.birthDate` short-only row), JI-14 → **#2603** (two-SD repro `repros/ji14-*.xml`:
  rename lands in `name` not `identity` (MA:76) AND is applied to the diff's mappings (PU:3082 reversal) —
  derived SD gets two `identity=m` declarations, diff mapping rewritten to an undeclared `m1`),
  JI-19 → **#2604** (verified: `ValidationEngine` never calls `setXver` at its four `new ProfileUtilities`
  sites, lines 931/1010/1088/1178; IG Publisher unchecked), **JI-21** (new) → **#2605**. DEV-033 minimized
  repro posted as a comment on #2584 (`repros/comment-2584-minimized-repro.md`).
- Still held: **JI-15** (design question — intended additional-base bounds semantics; WGM brief Tier 3),
  **JI-16** (J-c 2026-09-01: sort comparer errors debug-only — design question; WGM brief Tier 3), **JI-20**
  (needs a custom-resource repro).
- J-c (ch04 matching + ch02 preprocessing, 2026-09-01): one minor ready item (JI-17, held), one design
  question (JI-16); the suspicious spots examined and cleared are listed in the extract
  `java-ch04-matching-and-ch02-preprocessing-2026-09-01.md` §M.
- How to file (verified 2026-09-01): no issue template; one command per issue:
  `gh issue create -R hapifhir/org.hl7.fhir.core --title "<title>" --body-file <file>`.
- Before filing anything new, re-verify each citation against current master (drafts are at b06c7ee;
  the code moves fast). Note in each filed issue that line refs are to b06c7ee and include permalinks.
- Abbreviations used in drafts: PPP = `org.hl7.fhir.r5/.../conformance/profile/ProfilePathProcessor.java`,
  PU = `ProfileUtilities.java`, PRE = `SnapshotGenerationPreProcessor.java` (same package).
  Permalink base:
  `https://github.com/hapifhir/org.hl7.fhir.core/blob/b06c7ee7c57367305ff72b6bb09ce779b5dbd6ce/org.hl7.fhir.r5/src/main/java/org/hl7/fhir/r5/conformance/profile/`

## Status legend

- **ready** — analysis complete, draft body below can be filed as-is (after re-verification vs master).
- **needs-repro** — mechanism confirmed in code + existing test exhibits, but a minimized standalone
  repro would make the issue much stronger; decide whether to build one first.
- **needs-verification** — packet-1 orientation *candidate*, not yet deep-read; verify before filing.
- **minor/cosmetic** — real but low-impact; consider bundling several into one "small fixes" issue.

---

## JI-1 — Preprocessor sliceStuff propagation contaminates sibling extension slices and silently drops authored constraints (= our DEV-033) — **ready** (needs-repro to be stronger)

**Severity: high** — wrong conformance data in generated snapshots; the fhir-test-cases golden file
`on-questionnaire-expected.xml` blesses the buggy output.

**Draft title:** Snapshot preprocessor merges slicing-entry trailing content into the wrong extension
slices (and silently drops it where intended)

**Draft body:**

`SnapshotGenerationPreProcessor.processSlices` propagates "sliceStuff" (differential content between a
slicing entry and its first named slice) into each named slice. The matching in `mergeElements` uses
`elementsMatch` (PRE:812-822), whose match key is only **(path modulo `[x]`, sliceName-or-null)** over
the slice's *entire descendant range* — element ids and inner-slice ancestry are never consulted.

Extension-slice `value[x]` children (same path `….extension.value[x]`, no sliceName) are therefore
indistinguishable across sibling extension slices, with a dual effect:

1. **Contamination:** sliceStuff authored for extension slice E1's `value[x]` fill-if-absent-merges
   into a *different* extension slice E2's `value[x]` inside the named slice (min, max, type, fixed,
   binding, mustSupport, … — the whole `merge` (PRE:993-1075) property set).
2. **Silent loss:** having "matched", the sliceStuff element is marked handled and is **not injected**,
   so the constraints the author wrote for E1's value never reach that named slice at all.

Observable in the shared test suite (`fhir-test-cases r5/snapshot-generation/on-questionnaire`):
- `item:group.extension:itemControl.value[x]` in the expected snapshot has `mustSupport=true` although
  the input authors only a binding there — the mustSupport comes from the *enableWhenExpression*
  extension's `value[x]` sliceStuff row. The fingerprint of the merge path: it arrives *without* the
  ontario mappings on that sliceStuff row, because `merge` does not propagate `mapping`.
- The group/question slices contain **no** `extension:renderStyle/enableWhenExpression/hidden.value[x]`
  rows at all — spuriously "handled", never injected.
- Control: the `display` slice (whose range has no element at that path) got all three rows *injected*
  correctly, mappings included.

Minimal trigger shape: an outer non-extension slicing whose sliceStuff contains an extension slice E1
plus a sliceName-less `…extension.value[x]` row; a named slice containing a different extension slice
E2 with its own sliceName-less `value[x]` row.

Suggested direction: match sliceStuff elements by their id path relative to the slicer (the id rewrite
at PRE:799 already assumes ids align), or at minimum require the full ancestor sliceName chain to match.

Also note: because `on-questionnaire-expected.xml` was generated by this code, fixing the bug will
require regenerating that golden file.

---

## JI-2 — IndexOutOfBoundsException in type slicing over a sliced base with no named slice rows (PPP:1652) — **ready**

**Severity: medium** (hard crash on plausible input).

**Draft title:** IndexOutOfBoundsException in processPathWithSlicedBaseWhereDiffsConstrainTypes when
findBaseSlices returns no slices

**Draft body:**

`processPathWithSlicedBaseWhereDiffsConstrainTypes` ends with
`cursors.baseCursor = baseSlices.get(baseSlices.size() - 1).getEnd() + 1;` (PPP:1652), which throws
`IndexOutOfBoundsException` when `baseSlices` is empty. Two ways to get there:

1. the base snapshot has a slicing entry on the `[x]` element but zero named slice rows;
2. the slicing entry has children in the base snapshot: `findBaseSlices(cursors.base, newBaseLimit)`
   (PU:1742-1758) is called with `newBaseLimit` = the end of the slicer's own scope, so it anchors on
   the slicer's **last child**; its same-path scan then keys on the child's path, matches no named
   slice, and returns an empty list.

Guard the empty case (fall back to `newBaseLimit + 1`).

---

## JI-3 — ATTEMPT_TO_CHANGE_SLICING info message passes 2 args to a 4-placeholder message (PPP:355-357) — **minor/cosmetic**

The INFORMATION-severity variant (slicing restated *identically* on a later slice element) calls
`formatMessage(I18nConstants.ATTEMPT_TO_CHANGE_SLICING, id0, idI)` but the message text is
`The element at {0} defines the slicing {1} but then an element in the slicing {2} tries to redefine
the slicing to {3}` — `{1}` renders the element id where a slicing summary belongs and `{2}`/`{3}`
render unfilled. The ERROR variant (PPP:351-353) passes all four. Either pass the same four args or
use a dedicated "slicing restated redundantly" message.

---

## JI-4 — Off-by-one in the type-slice min reset: `typeList.size() > start + 1` (PPP:630-632) — **ready** (behavior question)

**Severity: low-medium** (wrong `min` on type slices in the two-slice case).

After each type-slice recursion, `if (typeList.size() > start + 1) { typeSliceElement.setMin(0); }`
resets the slice's min. `start` is already 1 at this point (incremented at PPP:604), so the reset fires
only with **three or more** typeList rows; with exactly two type slices, each keeps a min inherited
from the entry rather than being reset — inconsistent with the presumed intent "multiple slices share
the min" (cf. the analogous ≥2 logic at PPP:806-809). It also overwrites whatever min the recursion
just computed, *after* the min>0 legality check at PPP:611-616. If the intent is "more than one slice ⇒
slice min 0", the guard should be `typeList.size() > start` (or count actual named slices).

---

## JI-5 — NPE risk in chooseMatchingBaseSlice for a base slice with no type (PU:1732-1739) — **minor**

`chooseMatchingBaseSlice` compares `bs.getType().equals(type)`; `findBaseSlices` (PU:1742-1758) builds
`BaseTypeSlice` with `type = sliceElement.getTypeFirstRep().getCode()`, which is null for a typeless
base slice row → `null.equals(...)` NPE on the next type-slicing pass over that base. Guard the null.

---

## JI-6 — Slice-cardinality sum check never runs for slice groups at the end of the snapshot (PU:976-1036) — **ready**

**Severity: low-medium** (missed validation + missed auto-min-fix, position-dependent).

The post-generation sweep that checks slice min/max sums (and silently raises auto-added entries' min)
flushes an `ElementDefinitionCounter` only when a later element with shallower-or-equal path depth
appears. Counters still open when the element loop ends are **abandoned** — a sliced element whose
slice group extends to the last snapshot element is never checked or adjusted. Whether the check runs
thus depends on where the sliced element happens to sit in the resource. Flush remaining counters after
the loop.

Related cosmetic issue in the same block: the min>max warning (PU:1012-1016) prints the entry's own
min/max although the failed comparison was between the *sums* (`checkMinMax` compares countMin vs
countMax) — the message text can't match the numbers the check used.

---

## JI-7 — `"modiferExtension"` typo in isExtensionSlicing (PRE:1078) — **ready**

**Severity: low** (behavioral, not just cosmetic).

`isExtensionSlicing` checks `Utilities.existsInList(ed.getName(), "extension", "modiferExtension")`
[sic]. A slicing on `modifierExtension` therefore never qualifies as standard extension slicing, so
the preprocessor treats it as a real SliceInfo: its trailing content gets propagation treatment, and
its presence inside another slicing's trailing region can trigger the UNSUPPORTED_SLICING_COMPLEXITY
total bail-out (PRE:713-723). Second, separable question in the same predicate (design, maybe
intentional): the clause `(!ed.getSlicing().hasOrdered() || ed.getSlicing().getOrdered())` (PRE:1081)
means a slicing that *omits* `ordered` — the common case in the wild — is also not exempted; only an
explicit `ordered=false` passes.

---

## JI-8 — UNSUPPORTED_SLICING_COMPLEXITY bail-out abandons ALL slicings, not just the offending one (PRE:713-723) — **ready** (design question)

**Severity: medium** (silent under-processing).

When one slicing's trailing region contains one non-extension nested slicing, `processSlices` logs a
warning and `return`s — abandoning trailing-content propagation for **every** slicing in the
differential and skipping the final `markExtensions` pass, not just skipping the offending SliceInfo.
A log-warn is also easy to miss for something that changes the generated snapshot substantially.
Suggest: skip only the offending SliceInfo, and/or surface a ValidationMessage instead of a log line.

---

## JI-9 — `mergeAdditionalBinding` `any`-flag merge is a self-assignment no-op (PU:3225-3227) — **ready** (verified J-b 2026-09-01)

**Severity: low** (lost data on additional-binding merge).

```java
if (source.getAny()) {
  source.setAny(true);   // sets SOURCE, not dest — no-op
}
```
When a diff additional binding matching an inherited one (same valueSet+purpose) carries `any=true`, the
merged binding never receives it. All other props in the method write to `dest`. One-line fix.

## JI-10 — Additional-base pattern×pattern branch passes the `getFixed()` values (PRE:449-450) — **ready** (verified J-b 2026-09-01)

**Severity: medium** (additional-base pattern merging broken).

```java
} else if (source.hasPattern() && base.hasPattern()) {
  merged.setPattern(checkPatternValues(..., source.getFixed(), base.getFixed(), true));
```
In this branch neither side has `fixed` (the enclosing `if` handled that), so both operands are null —
`checkPatternValues` NPEs or misbehaves; clearly meant `getPattern()`. Related in the same method family:
dead disjunct `p1.getValues().size() > 1 || p1.getValues().size() > 2` at PRE:670 (second clause
unreachable; presumably meant `p2`), and `isLower`'s Quantity guard is inverted (PRE:638-642: **throws when
units are EQUAL** and happily compares values when units differ).

## JI-12 — `"..."` label append has swapped operands in `mergeStrings` (PU:3156-3157) — **ready** (verified J-b 2026-09-01)

**Severity: low-medium** (corrupted label text).

`mergeMarkdown` (definition/comment/requirements) calls
`appendDerivedTextToBase(source.getValue()=base, merged.getValue()=diff)` — correct. `mergeStrings` (label
only) calls `appendDerivedTextToBase(res.getValue()=DIFF, source.getValue()=BASE)` — operands swapped: for a
diff label starting with `"..."` the result is `"...difflabel" + CRLF + baselabel.substring(3)` — the marker
survives into the snapshot and the *base* label loses its first three characters.

## JI-13 — Obligation-profile additional-binding fold has an inverted guard (PU:2947-2952) — **ready** (verified J-b 2026-09-01)

**Severity: low** (obligation-profile additional bindings never propagate).

```java
if (binding.hasAdditional(ab)) {
  binding.getAdditional().add(ab.copy());   // adds only when ALREADY present → duplicate
}
```
The parallel extension-form block a few lines earlier (PU:2573-2578) correctly uses `!binding.hasAdditional(ab)`.
As written, a *new* additional binding from an obligation profile is never added, and an already-present one
is duplicated.

## JI-11 — `AllowUnknownProfile` comment says NONE is the default, field is ALL_TYPES (PU:223/448) — **FILED #2597** (verified J-d 2026-09-01)

Enforcement PU:2676-2687: Extension → throw unless `ALL_TYPES`; other types → throw only if `NONE`; so under
the actual default nothing throws (log.warn + final-sweep WARNING only). Test driver defaults to `ALL_TYPES`;
only `t15a` sets `allow="none"`. Also `NON_EXTNEIONS` typo (API name, not proposed for rename).

## JI-17 — dead `nonExtension` guards (PPP:846-851, 1097-1116) — **FILED #2596** (J-c found, J-d completed)

Live sites PPP:1425-1428 and PU:1621-1625 (via PPP:385, PPP:1680); dead sites fall through to the `Element`
expansion (PPP:908/1158) and orphan errors. Body suggests iterating the diff child window instead of
`diffMatches`.

## Remaining needs-verification candidates

- **JI-20** (J-e, 2026-09-01) — `generateIdForElement` absolutizes local contentReferences with a
  **hard-coded** `http://hl7.org/fhir/StructureDefinition/<type>` prefix for every non-LOGICAL kind
  (`getUrlForSource`, PU:4367-4373). For a constraint profile on a *custom* (non-core) resource or a
  specialization living elsewhere, the resulting reference points at a non-existent core SD. Needs a repro
  with a custom resource (R6 §5.4.6.6 pattern) before filing; check whether the publisher's custom-resource
  path already works around it. Also of note (design, not filed): `setIds(derived, false)` at PU:886 rewrites
  the **caller's** differential ids + contentReference form.
- J-e minor notes (not filed): PPP:351/355 add messages bypassing the `wantThrowExceptions` gate; PU:4322-4323
  empty "second pass" + unused `replacedIds`; `fixChars` PU:4375 (`_`→`-` in id segments only, no exhibit in
  the suite); `debugCheck` PPP:1488 hard-coded Nictiz url; `throw new Error("huh?")` PU:1353; `java.lang.Error`
  paths skip the snapshot-nulling `catch (Exception)` at PU:1078.
- **JI-18 → verified and FILED 2026-09-03 as #2602 (a crash, see §Verification record below).** Original note:
  (J-d, 2026-09-01) — obligation-profile **mustSupport / additional-binding fold runs only for
  elements the differential does not touch**: `updateFromObligationProfiles` (PU:2529-2582) has a single
  caller, the empty-diff copy-through path PPP:1068; `updateFromDefinition` (diff-touched elements) only
  re-adds the obligation *extensions* (PU:2608-2614) and runs the additional-binding fold with the inverted
  guard (JI-13/#2593). A differential row that merely changes `short` on an obligated element therefore
  drops the inherited `mustSupport=true`. `profile-patient-op3` cannot show it (its diff touches
  `active`/`maritalStatus`; MS is on `birthDate`/`deceased[x]` in op1). Build a two-line repro (op3 variant
  with a `Patient.birthDate` short-only row) before filing; could be intentional ("author took over the
  element") — ask if unsure. Related instance-state note: `obligationProfiles` (PU:451) is never cleared
  between `generateSnapshot` calls on one `ProfileUtilities`.
- **JI-19 → verified and FILED 2026-09-03 as #2604 (see §Verification record below).** Original note:
  (J-d, 2026-09-01) — xver resolution in template selection is **order-dependent**: PPP:700 tests
  `profileUtilities.getXver() != null` (raw field, PU:4810) whereas PU:1045/2078/2628 use the lazy
  `makeXVer()`. A caller that never calls `setXver` (only `SnapShotGenerationTests:606` does in the
  checked-out cone) gets the xver branch skipped for the *first* xver-typed extension slice in a
  `ProfileUtilities` instance (template = base row; `updateFromDefinition` PU:2628-2639 then initialises xver
  and applies the doc override), while later encounters use the xver template. Verify whether
  ValidationEngine / IG publisher call `setXver` (outside the cone) before filing; fix = use `makeXVer()` at
  PPP:700 (or a `getXver()` that lazily creates).
- **JI-14 → verified and FILED 2026-09-03 as #2603 (plus the `setName` defect, see §Verification record below).**
  Original note: `MappingAssistant.merge()` applies the identity-`renames` table to the **diff's** mappings and
  passes `null` for the inherited list (MA:173-177) — but `renames` maps *base-SD* identities, which are the
  ones inherited element mappings carry. Looks inverted (in an SD-level identity collision the diff mapping
  gets rewritten to the suffixed name while the inherited mapping keeps the stale identity, dangling against
  the reconciled declaration list). Needs a two-SD repro with colliding mapping identities before filing.
- **JI-15** additional-base bounds merge picks the *looser* value for `min` (PRE:421: lower of the two mins)
  and `maxLength` (PRE:466: larger wins), while `max`/`minValue`/`maxValue` correctly pick the stricter —
  under both-bases-must-hold semantics min should take the higher and maxLength the lower. Design question:
  confirm intended semantics before filing (maybe intersection was never the contract).
- **JI-16** (J-c, 2026-09-01) `sortDifferential` swallows "path not found in base" in production: the
  comparer records `"Differential contains path X which is not found in the base …"` (PU:3790-3795) but
  `sortElements` copies those strings into the caller's `errors` list **only when `debug`** (PU:3916-3918;
  `debug` defaults false), and an unknown path gets base index 0 → silently sorted to the *front* of its
  sibling group. Callers passing an `errors` list (the test driver, tooling) therefore never see the
  message. Possibly deliberate — the same "misfires on inherited/contentReference'd elements" rationale that
  disabled the out-of-order warning in `getDiffMatches` (PU:2465-2473) may apply, and a bad path is caught
  anyway by `checkDifferential` if generation follows (though NOT for a path that is merely absent from the
  base yet well-formed). Design question rather than a clear bug: ask before filing; re-check vs master.
- ~~JI-17~~ — filed as #2596 (see its section above).

## Deliberately NOT queued (design decisions / stale comments, not bugs)

- `APPLY_PROPERTIES_FROM_SLICER=false` — community decision (Zulip #IG-creation), spec-side question.
- Type-slicing "always closed" comment vs reopen logic (PPP:597 vs :646-667) and the sliced-base
  no-reopen asymmetry (PPP:1584-1588) — WGM/spec question (our OQ-020), not an upstream bug report.
- `setPath` misnomer (PPP:450), dead branch PPP:1247-1249, dead `missing` set (PRE:753/765),
  `checkToSeeIfSlicingExists` silent third outcome — code-quality notes; mention opportunistically
  if JI-2/JI-4 get traction.
- J-d notes, not queued: `isCompatibleType` PU:1465 unchecked `fetchTypeDefinition(base)` (NPE only for an
  unresolvable working code carrying a profile — needs a reaching input); `updateURLs` PU:2143/2147 concatenate
  the profile *list*'s `toString()` for `#`-relative profiles (latent, unused syntax); `PPP:1418-1420` `throw
  new Error("Not handled: multiple profiles …")` for the spec-legal profile disjunction on a new slice under a
  sliced base (design question — OQ-014 row, ask at WGM); `makeExtensionForVersionedURL` PU:4715-4717
  hard-coded snapshot indices (legacy; check callers in J-e); `PPP:768-771` "temporary work around" resetting
  Resource-template min/max; `PPP:803` `xtension.value[x]` min hack. Suspicious-spots table in the J-d extract §G.

## JI-21 (new, 2026-09-03, filed as #2605) — `isExtensionSlicing` requires an explicit `ordered=false`

`SnapshotGenerationPreProcessor.isExtensionSlicing` (PRE:1077-1086 @ b06c7ee = master f725fb7) returns false when
`slicing.ordered` is **absent** (`(!hasOrdered() || getOrdered())`), so a hand-written extension slicing entry
(`rules=open`, one `value:url` discriminator, no `ordered`) placed inside a slicer's sliceStuff is treated as
unsupported nested slicing; `processSlices` (PRE:695-720) then `return`s — abandoning slice pre-processing for
the whole profile (the #2589 bail-out) — with a `log.warn` only. Spec: `ordered` is 0..1, "Order is not required
unless specified". Empirical: `harness/repros/dev033-input.xml` (entry without `ordered`) → warning, nothing
propagates into the named slice; `dev033b-input.xml` (no explicit entry) and `dev033c-input.xml` (entry with
`ordered=false`) → pre-processing runs (and reproduces #2584). Suggested fix: treat absent as false; raise the
bail-out to an ERROR-level message. Related: #2588 (`"modiferExtension"` typo in the same method).

## Verification record 2026-09-03 (JI-14 / JI-18 / JI-19)

- **JI-18 → crash.** Repro `repros/ji18b-op3-plus-row-input.xml` = literal `profile-patient-op3-input.xml` +
  `Patient.birthDate` row with only `short`; deps op-base-input/op1/op2/op2a (op2, op2a carry a complex
  `tools/obligation` extension on birthDate). Result: `NullPointerException` at
  `ProfileUtilities.updateFromDefinition(ProfileUtilities.java:2611)` ← `PPP.processSimplePathWithOneMatchingElementInDifferential(:813)`.
  Control (unmodified op3) generates cleanly. The original "mustSupport fold only on copy-through" asymmetry
  remains as a design question once the NPE is fixed (stated in #2602).
- **JI-14 → confirmed, plus a second defect.** Repro `repros/ji14-base.xml` + `repros/ji14-derived-input.xml`
  (identity `m` = uri A in base, uri B in derived). Output `repros/out/ji14.java.xml`: SD.mapping has TWO
  entries with `identity=m` (the base's copy got `name="m1"` — MA:76 `setName(n)` instead of `setIdentity`);
  inherited `Patient.active` mapping keeps `m` (now resolving to B); the derived `Patient.gender` mapping is
  rewritten to `m1`, declared nowhere. Both effects follow from `mappings.merge(derived, base)` (PU:3082,
  "note reversal of names") + `addMappings(list, base.getMapping(), renames)` (MA:175).
- **JI-19 → confirmed for the validator.** `org.hl7.fhir.validation/.../ValidationEngine.java` lines 931, 1010,
  1088, 1178: `new ProfileUtilities(context, null, null).setAutoFixSliceNames(true)` — no `setXver`. The only
  `setXver` caller in the checked-out cone (r5 profile package, r5 test driver, validation src/main) is
  `SnapShotGenerationTests:606`. IG Publisher not checked.
