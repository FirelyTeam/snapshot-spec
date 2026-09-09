# Zulip sweep 2026-09-02 — cluster C (long tail: OQ-003/005/006/007/008/009/010/012/013/015/016/017/019, DEV-037, contentReference)

Read-only sweep of chat.fhir.org via `tools/zulip-search.sh "<q>" all <n>` (GET only; nothing posted).
Zulip full-text search ANDs + stems terms, so short queries were used and long threads were re-read by
searching on their topic title. Permalinks: `https://chat.fhir.org/#narrow/stream/<id>-<slug>/topic/<topic>/near/<msg>`
(Zulip encodes non-alphanumerics in topics as `.XX` hex: space `.20`, `.`→`.2E`, `?`→`.3F`, `,`→`.2C`, `:`→`.3A`, `/`→`.2F`, `[`→`.5B`, `]`→`.5D`).

## Queries run (hit counts = messages returned, newest-first, before relevance filtering; a count equal to the `n` cap — 30/30, 40/40, 60/60 — means the search saturated and is a lower bound, not a total)

| # | query | n | hits | for |
|---|---|---|---|---|
| 1 | `slice non repeating` | 30 | 30 | OQ-003 |
| 2 | `FHIR-28619` | 30 | 4 | OQ-003 |
| 3 | `slicing non-repeating elements allowed` | 30 | 30 | OQ-003 |
| 4 | `Slicing a non-repeating element` (topic re-read) | 80 / 200 | 80 / 200 | OQ-003, OQ-005, OQ-009, OQ-015 |
| 5 | `Validation without snapshot` (topic re-read) | 30 | 30 | OQ-003, OQ-016 |
| 6 | `openAtEnd` | 30 | 20 | OQ-005 |
| 7 | `slicing closed derived profile` | 30 | 9 | OQ-005 |
| 8 | `slicing rules tighten` | 30 | 6 | OQ-005 |
| 9 | `slicing rules less strict` | 40 | 1 | OQ-005 |
| 10 | `Slicing rules question` (topic re-read) | 40 | 40 | OQ-005 |
| 11 | `type slices open closed` | 40 | 37 | OQ-005 |
| 12 | `sliceIsConstraining` | 60 | 2 | OQ-006 |
| 13 | `profiles can change sliceName` (Grahame's property list) | 30 | 25 | OQ-006, OQ-012 |
| 14 | `mapping inherited profile snapshot` | 30 | 13 | OQ-007 |
| 15 | `StructureDefinition.mapping derived` | 30 | 1 | OQ-007 |
| 16 | `mapping identity snapshot` | 30 | 28 | OQ-007 |
| 17 | `Inheritance StructureDefinition.mappings` (topic re-read) | 40 | 40 | OQ-007, OQ-008 |
| 18 | `duplicate mappings diff` | 40 | 6 | OQ-007, OQ-012 |
| 19 | `elementdefinition-suppress` | 30 | 5 | OQ-008 |
| 20 | `suppress mapping snapshot` | 30 | 4 | OQ-008 |
| 21 | `verbose snapshot` | 30 | 15 | OQ-008 |
| 22 | `constraining out element properties` (topic re-read) | 30 | 30 | OQ-008 |
| 23 | `mappings remove not remove` | 30 | 30 (all noise) | OQ-008 |
| 24 | `Snapshot reconciliation` | 30 | 30 (noise) | OQ-008 |
| 25 | `element id snapshot regenerate` | 30 | 30 | OQ-009 |
| 26 | `element id slice name generated` | 30 | 30 | OQ-009 |
| 27 | `snapshot element ids` | 30 | 30 | OQ-009 |
| 28 | `keeps same id parent regenerating` | 30 | 4 | OQ-009 |
| 29 | `Adding extensions Firely terminal` (topic re-read) | 40 | 40 | OQ-009 |
| 30 | `canonical element id format Forge` | 30 | 2 | OQ-009 |
| 31 | `append definition inherited text` | 30 | 0 | OQ-010 |
| 32 | `three dots definition` | 30 | 1 (noise) | OQ-010 |
| 33 | `ellipsis comment inherit` | 30 | 0 | OQ-010 |
| 34 | `... prefix definition append` | 30 | 0 | OQ-010 |
| 35 | `appendDerivedTextToBase` | 30 | 0 | OQ-010 |
| 36 | `definition prepend inherited` | 30 | 0 | OQ-010 |
| 37 | `dot dot dot definition` | 30 | 26 (noise) | OQ-010 |
| 38 | `inherit definition text` | 30 | 30 (noise, but surfaced "Array values in differentials") | OQ-010, OQ-012 |
| 39 | `pattern inherit merge` | 30 | 3 (noise) | OQ-012 |
| 40 | `patternCodeableConcept derived` | 30 | 4 | OQ-012 |
| 41 | `fixed value derived profile override` | 30 | 3 | OQ-012 |
| 42 | `pattern coding derived profile inherit` | 30 | 1 (noise) | OQ-012 |
| 43 | `array differential replace merged appended` | 30 | 1 | OQ-012 |
| 44 | `Array values in differentials` (topic re-read) | 40 | ~10 | OQ-012 |
| 45 | `ElementDefinition modifierExtension` | 30 | 9 | OQ-013 |
| 46 | `modifierExtension element definition profile` | 30 | 4 | OQ-013 |
| 47 | `Modifier extension ElementDefinition` / `Modifier extension on ElementDefinition` (topic re-read) | 30 / 40 | 5 / 9 | OQ-013 |
| 48 | `autoFixSliceNames` | 30 | 0 | OQ-015 |
| 49 | `slice name auto generated` | 30 | 6 | OQ-015 |
| 50 | `differential modified snapshot generation` | 30 | 12 | OQ-015 |
| 51 | `snapshot generator changes differential` | 30 | 16 | OQ-015 |
| 52 | `differential changed by publisher` | 30 | 30 (noise) | OQ-015 |
| 53 | `no differential snapshot` | 30 | 30 | OQ-016 |
| 54 | `StructureDefinition without differential` | 30 | 26 | OQ-016 |
| 55 | `empty differential` | 30 | 30 | OQ-016 |
| 56 | `Differential in Logical Models` | 30 | 30 | OQ-016 |
| 57 | `specialization copying differential snapshot` / `additional resources snapshot DomainResource` | 30 / 20 | trunc. / 3 | OQ-016 |
| 58 | `profile-element extension` | 30 | 12 | OQ-017 |
| 59 | `profile element type.profile` | 30 | 30 | OQ-017 |
| 60 | `canonical fragment profile element` | 30 | 3 (noise) | OQ-017 |
| 61 | `profile fragment type` | 30 | 30 (noise) | OQ-017 |
| 62 | `define slices recursively backbone` | 30 | 7 | OQ-017 |
| 63 | `fmm inherited snapshot` | 30 | 1 | OQ-019 |
| 64 | `standards-status derived profile` | 30 | 0 | OQ-019 |
| 65 | `extensions not inherited snapshot` | 30 | 30 | OQ-019 |
| 66 | `snapshot inherit extension maturity` | 30 | 0 | OQ-019 |
| 67 | `Forge added extension explicit-type-name` (topic re-read) | 40 / 60 | 40 / 60 | OQ-019 |
| 68 | `Task 9079` | 40 | 3 | OQ-019 |
| 69 | `snapshot-behavior` | 40 | 7 | OQ-019, OQ-012 |
| 70 | `survey support profile related extensions` | 30 | 2 | OQ-019 |
| 71 | `Extension.url fixedUri` | 30 | 30 | DEV-037 |
| 72 | `fixed url extension snapshot` | 30 | 30 | DEV-037 |
| 73 | `extension url fixed value snapshot` | 30 | 16 | DEV-037 |
| 74 | `nested extension url fixed` | 30 | (output truncated, not reviewed) | DEV-037 |
| 75 | `Extension.url fixedString fixedUri` (topic re-read) | 40 | ~30 | DEV-037 |
| 76 | `FixedUri not showing` / `FixedUri not showing up` | 40 / 20 | 0 / 6 | DEV-037 |
| 77 | `URL of the extension remains the same` | 30 | 30 (all noise) | DEV-037 |
| 78 | `contentReference snapshot` | 30 | 27 | contentReference |
| 79 | `contentReference constraints propagate` | 30 | 2 | contentReference |
| 80 | `contentReference profile children` | 30 | 6 | contentReference |
| 81 | `Clarification on contentReference` (topic re-read) | 60 | 60 | contentReference, OQ-017 |
| 82 | `contentReference definition resolved` | 40 | 6 | contentReference |
| 83 | `Questionnaire snapshot generation absolute` | 40 | 3 | contentReference / DEV-023 |
| 84 | `Recursive schemas validation` (topic re-read) | 40 | 40 | contentReference |

---

## 1. OQ-003 — slicing a non-repeating (max=1) element — **answered** (not allowed outside choice types; dissent on record)

- **2017-02 → 2021-02 · implementers · "Slicing non-repeating elements to define a choice"** (Rob Hausam, Grahame, Lloyd, Alexander Zautke, Ewout/Marten mentioned).
  Long-running thread. Rob Hausam re-raised it 2020-06-12; Grahame 2020-07-03: "I'm not in favour ... I don't really see the use case" and "it's hard for to imagine that slicing is better than some other alternative". Lloyd 2021-02-02: "To be clear, I am in favor of allowing slicing on non-repeating elements" but the WG resolution was "we need concrete practical benefits" that invariants can't meet. Zautke 2020-07-03 notes ".NET tooling supports it like Ewout Kramer and Marten Smits described it in 2017".
  https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Slicing.20non-repeating.20elements.20to.20define.20a.20choice/near/202788891 (Grahame) · .../near/224889548 (Lloyd) · .../near/202785542 (Zautke)
- **2020-09-28 / 2021-02-22 · FHIR-28619 "Allow slicing of a non-repeating element to define a choice"** — tracker created by Rob Hausam; Firely Bot 2021-02-22 shows **Status: Resolved - No Change**.
  https://chat.fhir.org/#narrow/stream/179226-norway/topic/HAPI.20FHIR.205.2E3.2E0/near/227259534
- **2020-04-28 · IG creation · "Binding vs Slicing one coded field for multiple value sets"** — Lloyd: slicing is allowed only if an element repeats or is polymorphic; "Slicing for non-repeating elements isn't currently supported" (he would be happy to see it supported).
  https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Binding.20vs.20Slicing.20one.20coded.20field.20for.20multiple.20value.20sets/near/195497985
- **2021-07-23 · IG creation · "Slicing non-repeating elements"** — Lloyd: understanding is it's "prohibited unless they were polymorphic", yet IPS Allergy slices `code` 1..1 by pattern and the validator is happy; Rob Hausam confirms IPS did it knowingly and was moving to Coding-level slicing.
  https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Slicing.20non-repeating.20elements/near/246927633
- **2022-08-01 · conformance · "IPS Slicing Non-Repeating Non-Choice Elements"** — Chris Moesel: "not a legal use of slicing"; Rob Hausam: IPS "no longer have any plan to push for trying to get [it] legalized" (additional bindings instead).
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/IPS.20Slicing.20Non-Repeating.20Non-Choice.20Elements/near/291632285
- **2023-11-15 · shorthand · "Slicing an 0..1 codeableConcept element"** — Chris Moesel: "Although Forge seems to allow it, slicing is only allowed on lists (max > 1) and choices".
  https://chat.fhir.org/#narrow/stream/215610-shorthand/topic/Slicing.20an.200.2E.2E1.20codeableConcept.20element/near/402272791
- **2026-03-20..23 · FHIR Validator · "Validation without snapshot"** — Chris Grenz's test profile slicing `Patient.maritalStatus`; Grahame posts the live Java error "Attempt to a slice an element that does not repeat" (the `PPP:309` throw documented in OQ-003); Chris Grenz: "honestly thought we decided to allow slicing non-repeating elements (basically a CASE switch), but guess not"; Gino Canessa: choice types may be sliced when non-repeating, `maritalStatus` "is neither choice nor repeating".
  https://chat.fhir.org/#narrow/stream/291844-FHIR-Validator/topic/Validation.20without.20snapshot/near/580899914 · .../near/581093186 · .../near/581117986
- **2023-05-26 · IG creation · "Checking max on slices doesn't make sense on non-repeats"** — Lloyd vs Grahame on Java's slice-max-sum check firing for `value[x]` (max 1, slices sum 2); Lloyd: "The max on the slices has no choice but to be 1" — relevant to the OQ-005 slice-cardinality-sum check for the choice-type carve-out.
  https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Checking.20max.20on.20slices.20doesn't.20make.20sense.20on.20non-repeats/near/361465107

**Net:** Spec/WG position = only repeating or choice elements may be sliced (FHIR-28619 closed No Change; Java rejects with a live error in 2026); Lloyd, Rob Hausam, Chris Grenz and Firely all on record wanting it allowed. Nobody on Zulip discusses Java's "intro capped to max=1" carve-out — the WGM question "which exceptions" is fresh.

## 2. OQ-005 — generator enforcement of slicing.rules (closed/openAtEnd/ordered lattice) — **discussed-unresolved** (no-loosening rule confirmed; generator role for type slicing debated, never settled)

- **2023-06-30 · conformance · "Slicing rules question"** — Rob Langezaal asks whether a derived profile may loosen `openAtEnd`/`closed`: "could not find any information on that in the specs"; Grahame: "correct" (i.e. it may not). Nothing on who enforces it.
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Slicing.20rules.20question/near/371299107
- **2019-11-30 → 2020-03-10 · conformance · "Type[x], Slices, open/closed"** (Grahame, Marten Smits, Ward, Ewout, Chris Grenz) — the one thread where the *generator's* handling of `rules` is argued. Grahame: base type slicing is closed; a derived profile saying `open` "just don't close ... it remains closed" (2019-12-10: "if the list of slices is closed ... it's still closed when you're done"). Ward 2020-02-24 restates Grahame's model as a question: diff `rules` "says something about how the diff content should be interpreted" (open = keep the base's slices, closed = only the listed slices survive). Ewout 2020-02-25 objects that open/closed "turns from an aspect of the slice to an interpretation hint" for the generator, concedes the train "has probably left the station", and proposes the final-state table (base closed → stays closed; base open + diff closed → closed). Chris Grenz 2020-03-10: "a diff of `open` over a base of `closed` is an error" — disallowed slices should be `max:0`'d, not re-listed. Marten 2019-12-10 agrees only that closed stays closed; 2020-01-09 he doubts type slices auto-closing (blocks adding a slice in a derived profile).
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Type.5Bx.5D.2C.20Slices.2C.20open.2Fclosed/near/182253089 (Grahame) · .../near/183030391 · .../near/188924523 (Ward) · .../near/189009054 (Ewout) · .../near/189009249 (Ewout table) · .../near/190231350 (Chris Grenz)
- **2024-11-21 · conformance · "Type slicing in Profiles"** — Gino: "Why is the slicing open in the differential and closed in the snapshot?" Grahame: "type slicing is always closed in practice"; Richard Townley-O'Neill notes R4 bodyweight snapshot says closed, R5 says open. Contrast **2020-11-07 · IG creation · "Validation issue with partial type slicing"** — Lloyd: "It's wrong to auto-change 'open' slices to 'closed'". Lloyd and Grahame disagree on the generator's auto-closing.
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Type.20slicing.20in.20Profiles/near/483615991 · https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Validation.20issue.20with.20partial.20type.20slicing/near/215972826
- **2025-08-28 · tooling · "validator error url must have one match"** — Alexander Henket: open type slicing in a base "is incorrectly handled as closed ... in the downstream profile"; "Why is it unclear when say open on type slicing, but not ... value or pattern slicing?" — field report of the auto-close in Java.
  https://chat.fhir.org/#narrow/stream/179239-tooling/topic/validator.20error.20url.20must.20have.20one.20match/near/536684803
- **2021-03-02 · conformance · "Is eld-1 correct in the context of an differential?"** — a diff that restates only `slicing.rules` (open→closed) without discriminators trips eld-1 in .NET; Richard Townley-O'Neill: "Isn't opening a closed slicing in a derived profile impossible?" Shows the "derived slicing SHALL repeat base discriminators" rule is not what authors do.
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Is.20eld-1.20correct.20in.20the.20context.20of.20an.20differential.3F/near/228406777
- **2017-05-11 · implementers · "Slice closed rule validation"** — Lloyd: validator "doesn't distinguish between openAtEnd and closed"; "Open is preferable to OpenAtEnd" — openAtEnd ordering effectively unenforced since 2017 (matches both engines today).
  https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Slice.20closed.20rule.20validation/near/153889374

**Net:** The lattice's no-loosening rule is affirmed by Grahame (2023) but declared nowhere; the only generator-side debate is for type slicing, where Grahame's "diff `rules` is an interpretation hint" model is on record (Ward restated it as a question, Marten agreed only that closed stays closed and separately doubted auto-closing, Ewout and Chris Grenz objected) and was never resolved. General-purpose enforcement of closed/openAtEnd by the generator was never discussed.

## 3. OQ-006 — sliceIsConstraining semantics — **not found**

- The string occurs in exactly two messages. The only substantive one: **2022-12-14 · conformance · "default values for mustSupport in R4B/R5"** — Grahame's property-by-property list of what profiles may change: "sliceName / sliceIsConstraining - can be changed by profiles". No word on generator matching semantics, reject-vs-proceed, or the .NET/Java disagreement (DEV-036).
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/default.20values.20for.20mustSupport.20in.20R4B.2FR5/near/315910873

## 4. OQ-007 — StructureDefinition.mapping inheritance — **answered** (mapping definitions propagate into the derived SD at snapshot time; .NET's non-merge is the outlier)

- **2024-08-15 → 2026-05-04 · conformance · "Inheritance of StructureDefinition.mappings"** (Lloyd, Ward, Grahame, Eric Haas, Gino, Chris Moesel, Jose Costa Teixeira, Rob Langezaal). Lloyd 2024-08-15: "Mappings definitely inherit. They always have"; the question is whether a derived profile adding element mappings must re-declare the `SD.mapping` definition — the publisher says no and propagates the definitions during snapshot generation, "Simplifier does not do that", and no documentation says which is correct. Grahame 2024-08-21 (after reading code): the code "does not change the declared mappings in the profile" but merges element mappings by identity, so element mappings can lack a definition; Lloyd shows duplicated definitions in a snapshot; Grahame 2024-08-23: "fixed. And suppress is now supported on both ElementDefinition.mapping, and StructureDefinition.mapping". Ward 2024-09-18 conclusion: "Snapshot generation should always propagate mapping definitions (SD.mapping)" and rendering ignores suppressed ones; Lloyd: "I believe that's the expectation, yes". Test cases: fhir-test-cases PR #188; Grahame 2026-04-28: "there are test cases in the test cases repository - /r5/snapshot".
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Inheritance.20of.20StructureDefinition.2Emappings/near/462578892 (Lloyd) · .../near/464179266 (Grahame code reading) · .../near/464582094 (fixed) · .../near/471272112 (Ward summary) · .../near/471297744 (Lloyd confirms) · .../near/591342420 (tests)
- **2022-03-01 · fhir/infrastructure-wg · "Constraining out element properties in a differential"** — Ward: "Forge currently has a setting whether to inherit the mapping definitions in the profile metadata ... from a parent profile" — i.e. Firely treated SD.mapping inheritance as a user choice.
  https://chat.fhir.org/#narrow/stream/179280-fhir.2Finfrastructure-wg/topic/Constraining.20out.20element.20properties.20in.20a.20differential/near/273638812
- **2021-11-30 · implementers · "how should duplicate mappings in diff be handled?"** — Eric Haas: a redundant `mapping` in the diff produced a repeated mapping in the snapshot; "Should mappings in a differential override or add? Would like this to be explicitly documented" (no answer in thread). **2016-06-28 · implementers · "Multiple mappings to the same map identity"** — Grahame: "I do not replace mappings when I am generating snapshots" (element-level mappings are additive).
  https://chat.fhir.org/#narrow/stream/179166-implementers/topic/how.20should.20duplicate.20mappings.20in.20diff.20be.20handled.3F/near/263219321 · https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Multiple.20mappings.20to.20the.20same.20map.20identity/near/153835361
- **2016-06-08 · conformance · "type profiles (#9791)"** — Michel raises the type-profile variant (does `Observation.identifier` inherit the Identifier profile's root-element mappings?) — the OQ-007 question in its original form, unanswered there.
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/type.20profiles.20(.239791)/near/153831997

## 5. OQ-008 — elementdefinition-suppress vs "complete verbose snapshots" — **answered** (suppress is sanctioned; but *remove-from-snapshot* vs *hide-at-render* is contradictory on record)

- **2022-02-15 → 2022-12-15 · fhir/infrastructure-wg · "Constraining out element properties in a differential"** (FHIR#20385). Lloyd 2022-03-01: "This is definitely not about 'don't display', it's about 'remove entirely'" — mappings/comments/examples/aliases that are "wrong" in the constrained context. Marten Smits: "Messing with snapshot generation functionality is a terrible idea", prefers a "don't display" flag; later concedes since authors opt in explicitly. Lloyd 2022-03-02: "Snapshot generation is a 'process'. It doesn't get to be 'normative'". Lloyd 2022-12-12: "this will be tooling-extension driven"; Vadim: dangerous to define "fundamental behaviour alterations" in an extension outside the spec; Grahame: "the tooling IG is a good place to define extensions for tooling".
  https://chat.fhir.org/#narrow/stream/179280-fhir.2Finfrastructure-wg/topic/Constraining.20out.20element.20properties.20in.20a.20differential/near/273651748 (Lloyd) · .../near/273640401 (Marten) · .../near/273842423 · .../near/315905611 (Grahame)
- **2023-04-25..27 · IG creation · "Mappings: to remove or not to remove, that is the question"** — Chris Moesel identifies `elementdefinition-suppress` as "custom made for exactly this purpose"; Grahame had said "I don't think there's any way right now" (quoted).
  https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Mappings.3A.20to.20remove.20or.20not.20to.20remove.2C.20that.20is.20the.20question/near/353282717
- **2024-09-18 · conformance · "Inheritance of StructureDefinition.mappings"** — Ward's reading "Rendering should ignore SD.mapping and ED.mapping if the suppress extension is applied", Lloyd: "I believe that's the expectation" — a *render-time* reading that conflicts with Lloyd's 2022 "remove entirely".
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Inheritance.20of.20StructureDefinition.2Emappings/near/471272112
- **2026-05-03/04 · same topic** — Rob Langezaal asks about `label = $all` in `r5/snapshot-generation/address-no-examples-input.xml`; Grahame: "the `$all` is an instruction to delete all the existing examples, irrespective of their label" — "this is an undocumented feature of the suppress extension"; FHIR-56831 filed to document it. So the golden suite treats suppress as a *snapshot-time deletion* of inherited examples — the concrete oracle for .NET's `RespectSuppressExtension`.
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Inheritance.20of.20StructureDefinition.2Emappings/near/592817665 · .../near/592817709
- **2020-09-02 · conformance · "Invalid Consent StructureDefinition Identifier Example"** — Brian Postlethwaite suggests the generator drop inherited examples (and mappings/invariants) when `max=0` — an earlier ask for generator-side suppression.
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Invalid.20Consent.20StructureDefinition.20Identifier.20Example/near/208899361
- **2021-03-02 · shorthand · "Exclude inherited array elements"** — Lloyd: a slice snapshot "inherits everything ... It might be noisy, but it's correct"; "we could possibly look at a new extension ... that allows the removal of certain things" — the origin of the suppress design.
  https://chat.fhir.org/#narrow/stream/215610-shorthand/topic/Exclude.20inherited.20array.20elements/near/228508108

**Net:** the profiling.html "complete verbose snapshots" sentence is stale: FHIR-I agreed (Lloyd/Grahame) that authors may remove inherited mappings/examples/aliases/comments via a tooling extension, Java implements suppress for ED.mapping, SD.mapping and examples (`$all`), and the golden `address-no-examples` test blesses snapshot-time deletion. The Ward/Lloyd 2024 "render-time" remark is the loose end.

## 6. OQ-009 — element id regeneration — **answered** (ids are derived data; both generators regenerate; author ids not preserved)

- **2019-07-25..29 · conformance · "Slicing a non-repeating element"** — Grahame: "if you're talking about the differential, ids are derivative"; "I don't mind if other tooling requires properly formed ids ... Just that mine doesn't"; Lloyd: "the paths drive, not the ids". Chris Grenz: sdf-14 requires well-formed ids in both components and warns that regenerating slice names creates "identifier inconsistencies between snapshot and differential". Michel 2019-07-29: the .NET generator generates element ids "but does not use/depend on them" — elements are identified "strictly by list order, Path & SliceName"; Grahame: "Same as java".
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Slicing.20a.20non-repeating.20element/near/171687995 · .../near/171690281 · .../near/171934808 · .../near/171935197
- **2018-05-28 · implementers · "Forge 18.6 for STU3"** — Michel: "After some discussion with FHIR core team, we decided that it would be best for Forge to auto-generate element id values according to a predefined 'canonical' format".
  https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Forge.2018.2E6.20for.20STU3.20(Colonia.202018.20Edition)/near/153965101
- **2024-10-23 → 2024-11-15 · shorthand · "Adding extensions"** — Chris Moesel: Firely Terminal 3.2.1 snapshots keep the *parent's* ids (`Patient.contact.id` → `BackboneElement.id`, duplicates, sdf-16 violation) — "I think this is a bug"; Ward: only on 3.2.1 Mac/Linux, fixed in 3.3.2; backstory: "a `GenerateElementIds` setting in the SDK that you could set to `false` ... We'll remove that option in SDK6". Field confirmation that ids must be canonical and that author-controlled ids are considered a mistake by Firely too.
  https://chat.fhir.org/#narrow/stream/215610-shorthand/topic/Adding.20extensions/near/478755142 · .../near/482640122
- **2019-02-22 · implementers · "Element.id vs snapshots of subtypes of Element in FHIR 4.0.0"** — official R4 snapshots carried wrong inherited ids; Michel shows .NET's regenerated `Observation.referenceRange.id` as the correct form.
  https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Element.2Eid.20vs.20snapshots.20of.20subtypes.20of.20Element.20in.20FHIR.204.2E0.2E0/near/159332214

**Net:** nobody on Zulip has ever argued for preserving author ids; the "may be used as the target of external references" concern has no Zulip trace.

## 7. OQ-010 — the "..." append convention — **not found**

Seven phrasings (incl. Java's `appendDerivedTextToBase` and "prepend"/"ellipsis"/"three dots"/"dot dot dot") return nothing about the convention. It has never been discussed on chat.fhir.org; it is purely an implementation convention shared by .NET and Java.

## 8. OQ-012 — fixed/pattern partial overlay vs replace — **discussed-unresolved** (rules exist "in the snapshot tests", never written down; overlay never discussed)

- **2022-12-14 · conformance · "default values for mustSupport in R4B/R5"** — Grahame's list: "fixed - profiles can introduce but not change"; "pattern - profiles can introduce but change (but can fix if there's already a pattern)" [sic — likely "but not change"]; "example - profiles can add (or remove...)"; "type.aggregation - can only remove, or add if there's none"; "constraint - profiles can add"; "alias - profiles can add"; "code - profiles can add". Under this rule a diff that *alters* an inherited fixed value is illegal outright, which makes .NET's property overlay moot for `fixed[x]` and a spec question only for `pattern[x]` (whose line is garbled).
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/default.20values.20for.20mustSupport.20in.20R4B.2FR5/near/315910873
- **2020-08-04..21 · conformance · "Array values in differentials"** — Chris Moesel: does a diff array replace or add to the inherited array? Grahame: "there's an outstanding task to write this up somewhere"; "there is a set answer, represented in the snap shot tests"; "constraints are additive. Types are special ... Differentials... not sure".
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Array.20values.20in.20differentials/near/207625254
- **2025-07-02 → 2025-10-14 · shorthand · "Sushi merging profile extension with parent content"** — Chris Moesel: array merge behavior (replace / merge / append) "is not always clearly defined"; SUSHI assumes replace; "For some extensions, anything in the differential is meant to *replace* ... For others ... *added*"; the tooling `snapshot-behavior` extension partially addresses it but nobody uses it.
  https://chat.fhir.org/#narrow/stream/215610-shorthand/topic/Sushi.20merging.20profile.20extension.20with.20parent.20content/near/526799860 · .../near/544758583
- **2024-01-16 · shorthand · "restricting aggregation type"** — Chris Moesel: from publisher behavior, `type.aggregation` in the diff *replaces* the snapshot array.
  https://chat.fhir.org/#narrow/stream/215610-shorthand/topic/restricting.20aggregation.20type/near/413233711

No thread discusses merging a diff `patternCodeableConcept` *into* a base pattern; Java's wholesale replace has never been questioned publicly.

## 9. OQ-013 — modifierExtension on ElementDefinition — **discussed-unresolved** (no use case; support "by specific request"; nothing on merging)

- **2019-04-08..10 · conformance · "Modifier extension on ElementDefinition?"** — Michel asks the use case now that ED derives from BackboneElement; Grahame: "we have no concrete examples", the change came with making Dosage/Timing/ED BackboneElements; "I don't think you need to support extensions or modifier extensions on Forge except by specific request" ("thought it would be good to preserve what exist").
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Modifier.20extension.20on.20ElementDefinition.3F/near/162849209 · .../near/162849243
- **2018-09-19 · implementers · "Modifier extension on ElementDefinition"** — Lloyd: modifierExtension is usable "in StructureDefinition.snapshot.element and StructureDefinition.differential.element" (about representation, not merging).
  https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Modifier.20extension.20on.20ElementDefinition/near/153998383
- Inheritance of a modifier extension into a derived snapshot is never discussed.

## 10. OQ-015 — generator mutating the input differential (auto slice names) — **answered** (2019: reject wrong/missing type-slice names rather than silently rewrite; repair later re-admitted only as an opt-in flag)

- **2019-07-25 · conformance · "Slicing a non-repeating element"** — Chris Grenz observes Java accepts any slice name in T43's input and rewrites it to `valueUnit` in the snapshot "While leaving the original in the differential: ... Is this intended?"; Grahame confirms he rewrites the sliceName in the snapshot "without fixing it in the differential", could instead raise an exception or fix the diff ("fixing it is super hard for me"), and invites discussion; Chris Grenz: confusing if the generator "creates identifier inconsistencies between snapshot and differential"; Lloyd: "My leaning is to raise an error if the differential slice name is wrong"; Grahame same day: "The generation now blows up if the slice names are wrong in the differential" (t29/t43 updated). Michel 2019-07-18: Forge "auto-generates slice names for named type slices" and reverts custom ones.
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Slicing.20a.20non-repeating.20element/near/171687483 (Grahame) · .../near/171687536 (Chris Grenz) · .../near/171688159 (Lloyd) · .../near/171730052 (Grahame: blows up) · .../near/171170488 (Michel)
- No Zulip mention of `autoFixSliceNames` or of .NET's in-place `GENERATE_MISSING_TYPE_SLICE_NAMES`. The 2019 decision (error, not repair) is the community precedent; .NET's default in-place repair and Java's CLI flag both postdate/contradict it.

## 11. OQ-016 — StructureDefinition without a differential — **not found**

No thread asks what a differential-less constraint SD means. Adjacent only: **2025-11-04 · shorthand · "Sushi problem for additional resources"** — Chris Moesel asks Grahame whether, for a *specialization* adding only new elements, generating a snapshot is "as simple as copying the differential into the snapshot" (no answer captured); and **2020-02-21 · shorthand · "FSH Packages"** — SUSHI requires snapshots on inputs. Neither addresses the constraint-profile case.
https://chat.fhir.org/#narrow/stream/215610-shorthand/topic/Sushi.20problem.20for.20additional.20resources/near/553651254

## 12. OQ-017 — elementdefinition-profile-element vs `url#fragment` — **discussed-unresolved** (extension semantics explained by Grahame; scope disputed; fragment syntax has zero Zulip presence)

- **2020-02-10 / 2021-02-03..05 · IG creation · "Clarification on contentReference"** — Grahame's worked example: `Composition.section` with `type.profile = <this profile>` carrying `elementdefinition-profile-element valueString="Composition.section"` — "this says that there must be a composition section, and that each section must have a section, recursively". On semantics: the profile must still express a constraint on the type, normally assessed on the profile's root element, but with the extension present "you base the decision on a different element. The type must still match"; it "doesn't change the meaning; it changes how you use it" (so not a modifierExtension); constraints on subpaths of the nominated element apply ("yes"). Chris Moesel's fleshed-out `MyQuestionnaire` differential confirmed by Grahame ("yes that's what it says"); Ewout 2021-03-01: "Never thought we'd use this extension to solve this problem. Quite elegant."
  https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Clarification.20on.20contentReference/near/225079400 · .../near/225079496 · .../near/225087084 · .../near/225089363 · .../near/225253134 · .../near/228211037
- **2021-05-11 · conformance · "profile-element Extension"** — Julian Carter/Chris Moesel ask whether it may target datatype elements; Lloyd: "Why would you need to? Profiling data types is already allowed" and "we'd need to add more documentation" — SUSHI restricted it to backbone elements.
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/profile-element.20Extension/near/238315419 · .../near/238329205
- **2022-11-06 · FHIR-39384 "Extension: profile-element doesn't work"** (Catherine Hosage Norman, priority High). **2024-05-24 / 2025-03-03 · shorthand · "define slices recursively on backbone element"** — Gregor Lichtner: works for sub-element slices, not for slices on `Composition.section` itself (publisher/validator limits).
  https://chat.fhir.org/#narrow/stream/215610-shorthand/topic/define.20slices.20recursively.20on.20backbone.20element/near/503082621
- **2023-04-21 · IG creation · "Clarification on contentReference"** — Chris Moesel: R5 §5.1.0.10 (FHIR-39753) "seems to be different than what we discussed above, which basically said that you needed to use the profile-element extension in order to recurse constraints".
  https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Clarification.20on.20contentReference/near/351737879
- Searches for canonical `#fragment` in `type.profile` return nothing relevant — the .NET `ProfileReference` fragment syntax has never been raised on Zulip.

**Net:** the extension is the only community-known syntax; its generator obligation (walk children from the nominated element) is implicit in Grahame's "constraints on subpaths apply" but no snapshot shape was ever shown, and FHIR-39384 says the Java implementation didn't work.

## 13. OQ-019 — which extensions are non-inheritable — **answered** (policy belongs on the extension definition; hard-coded lists are the acknowledged stopgap; the metadata exists but is unapplied; .NET's list was copied from Java)

- **2016-12-05 · committers · "Task 9079"** — Michel: "In Forge, it is now hardcoded that the FMM extension is not inherited by derived profiles. It would definitely be an improvement if an extension definition profile could somehow express the associated rules for snapshot generation in a computable way." (Ewout on GF#9079 = write down the base+diff→snapshot algorithm: "possible, but a daunting task".)
  https://chat.fhir.org/#narrow/stream/179165-committers/topic/Task.209079/near/153860752 · .../near/153860748
- **2020-07-28 → 2020-09-08 · tooling · "Forge added extension explicit-type-name"** — Ward: Forge will stop repeating untouched extension instances; extensions "which should not be inherited (like Normative-version) will still wrongly end up in your snapshot. This will need a fix in FHIR". Chris Moesel: "wack-a-mole". Grahame 2020-08-26: "it feels like we're going to have an extension on the extension definition here"; Ewout 2020-09-07: "This is the only real solution - the author of the extension has to indicate whether this extension propagates" (cf. .NET `Inherited`, Java `@Inherited`); Grahame: longer-term, "so we'll have to settle for a list in the short term"; Ewout files FHIR-28441 (Ward: near-duplicate of FHIR#27535).
  https://chat.fhir.org/#narrow/stream/179239-tooling/topic/Forge.20added.20extension.20explicit-type-name/near/205217073 · .../near/208143097 · .../near/209272250 · .../near/209275595 · .../near/209284565
- **2023-02-15/16 · same topic** — Chris Moesel: `structuredefinition-inheritance-control` (no-copy etc.) was created and FHIR-28441 closed, but "the new extension has not been applied to any core extensions yet". Grahame posts his working lists of non-propagating extensions per context (SD: category, wg, normative-version, build/summary, standards-status, build/notes, fmm, summary, build/introduction, type-characteristics, security-category, implements, interface; ED: explicit-type-name, translatable, standards-status, normative-version, display-hint, build/no-binding; Type: elementdefinition-pattern, fhir-type, regex, hierarchy; Binding: same as SD). 2025-12-08 Chris Moesel: `fhir-type` surely must propagate — Grahame: "that one does, yes" (so the Type list is not purely "never inherit").
  https://chat.fhir.org/#narrow/stream/179239-tooling/topic/Forge.20added.20extension.20explicit-type-name/near/328092456 · .../near/328203575 · .../near/562553051
- **2025-12-09 · same topic** — Ward: Firely resolved it in 2024 "by just keeping a hardcoded list of extensions not to inherit" (firely-net-sdk PR #2886) "Which was based on how the Java code also did it"; "we only `ignore` those extensions ... default behavior is probably `add`"; the tooling `snapshot-behavior` extension "I don't think anyone is really using". List affects snapshot only, not differential.
  https://chat.fhir.org/#narrow/stream/179239-tooling/topic/Forge.20added.20extension.20explicit-type-name/near/562720344 · .../near/562721221 · .../near/562722210
- **2026-08-10..19 · same topic** — Julian Sass: CRMI `artifact-author`/`artifact-editor` wrongly inherited; asks for configurable lists or inheritance-control support. Chris Moesel: "This would be a whole lot easier if extensions used the inheritance control extension"; Grahame 2026-08-18: "it's something we should do. I think that the snapshot generator in the validator follows them"; Chris Moesel 2026-08-19: SUSHI doesn't, "because last time I checked there wasn't a single extension that actually used them".
  https://chat.fhir.org/#narrow/stream/179239-tooling/topic/Forge.20added.20extension.20explicit-type-name/near/615666001 · .../near/617280556 · .../near/617546464
- **2023-06-17 · ig publishing requirements · "Review of Profile extensions"** — Grahame's survey of which profile-related extensions the tools support (fmm, normative-version, standards-status … "Fully Supported") — a support list, not an inheritance list.
  https://chat.fhir.org/#narrow/stream/196008-ig-publishing-requirements/topic/Review.20of.20Profile.20extensions/near/367266587

**Net:** the candidate RFC ("mark extensions inheritable/non-inheritable in the extension definition") was proposed by Ewout in 2020, accepted (FHIR-28441 → `structuredefinition-inheritance-control` in the extensions pack, `snapshot-behavior` in tools IG), and has been stalled for 3+ years because no extension definition carries the metadata; both engines run on lists, Grahame believes Java honors the metadata. WGM framing: unblock application of the existing metadata, then retire the lists.

## 14. DEV-037 — `Extension.url` fixedUri in snapshots — **discussed-unresolved** on generator synthesis; two sub-answers

- **2019-09-19 · conformance · "Extension.url - fixedString or fixedUri?"** — Michel: official R4 extension snapshots carry `fixedUri` on `.extension.url` although the element's type is a system string; Grahame: "you need magic knowledge to know that Extension.uri is actually a uri" (fixed by the technical correction); Michel: "the official snapshots now introduce fixedUri ... the spec itself does not explicitly define what the type of the fixed value should be". Also 2019-09-20 (implementers, ".Net validation issue") Michel: "after discussion with Grahame & Lloyd, I learned that Extension.url actually requires a fixedUri value". Establishes the *type* convention (fixedUri), not who must add it.
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Extension.2Eurl.20-.20fixedString.20or.20fixedUri.3F/near/176082903 · .../near/176088736 · https://chat.fhir.org/#narrow/stream/179166-implementers/topic/.2ENet.20.20validation.20issue/near/176176874
- **2025-03-07 · IG creation · "fixed vs pattern values for simple data types"** — Elliot Silver: publisher complains if `extension.url` isn't `fixedUri`; with `patternUri` the Java validator errors "The value of Extension.url is not fixed to the extension URL ..." — i.e. the fixed url is a *validator requirement on the authored SD*, which explains why Java's generator never synthesizes it (authors/FSH always state it) and why no golden test exercises synthesis.
  https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/fixed.20vs.20pattern.20values.20for.20simple.20data.20types/near/504198628
- **2022-11-28 · shorthand · "shorthand syntax to modify an extension"** — Chris Moesel quotes one line of Lloyd's ("The URL of the extension remains the same") and *asks* him to confirm that a profiled extension's `Extension.url` fixedUri stays the *original* extension's url; Lloyd's reply was not retrieved. **2023-01-20 · SUSHI 2.8.0**: "Keep parent's fixedUri on Extension.url of child extensions" — SUSHI adopted that reading. Provenance is thus Lloyd's one line plus SUSHI's implementation; it still contradicts the register's "arguably wrong for both" on `ext-sort-issue` (correction candidate for DEV-037, pending Lloyd's elaboration).
  https://chat.fhir.org/#narrow/stream/215610-shorthand/topic/shorthand.20syntax.20to.20modify.20an.20extension/near/312640274 · https://chat.fhir.org/#narrow/stream/215610-shorthand/topic/SUSHI.202.2E8.2E0/near/322412489
- **2018-10-24 · IG creation · "FixedUri not showing up in extension"** — Brian Reinhold: fixedUri on `Observation.extension.url` not rendered; Lloyd: rendering special-cases extensions. Shows authors *do* put the fixed url on profile-side extension slices, but says nothing about generators adding it.
  https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/FixedUri.20not.20showing.20up.20in.20extension/near/154012065
- **2020-01-22 · dotnet · "Possible Snapshot generator issue"** — Kurt Allen: a differential `Extension.extension:landMark.url` with `fixedUrl` (sic) and no type made the .NET generator emit odd `_code` output — only trace of .NET's url handling on Zulip; not about synthesis.
  https://chat.fhir.org/#narrow/stream/179171-dotnet/topic/Possible.20Snapshot.20generator.20issue/near/186318753

**Net:** nothing on Zulip asks whether a *generator* should add `Extension.url.fixedUri`; the convention is treated as an authoring obligation enforced by the validator. The nested-part value (relative name vs full url) is undiscussed.

## 15. contentReference — propagation (R5 §5.1.0.10) and survival after child expansion — **discussed-unresolved** (three-way evolution; R6 opt-in extension coming)

- **Sub-result, DEV-023 flavor 1 (absolute vs local form): answered** — Grahame 2022 rule "relative content references must be replaced with absolute content references" on snapshot generation; see the STU3 Questionnaire thread below.
- **2018-01-16..23 · conformance · "contentReference definition"** — Michel poses the two options: (1) resolve within the same profile → constraints cascade to all nesting levels, no per-level constraints; (2) resolve from the defining core profile → per-level constraints, no global ones; option 1 "will introduce considerable implementation challenges". Chris Grenz: "two different intents that each need their own syntax". Lloyd: "the 'type' of the element is always the type as defined in the original resource"; repeat constraints per level or define a profile; favors "supporting profiling on backbone elements".
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/contentReference.20definition/near/153930284 · .../near/153930278 · .../near/153930289
- **2021-01-20 → 2021-02-05 · IG creation · "Clarification on contentReference"** — Chris Moesel (SUSHI) asks Ewout/Ward how differentials should work with contentReferences; Ewout 2021-02-01: "the reason it remains silent is probably because......I don't know ... discussing this years ago at FHIR-I, but we've just let the topic rest". Grahame clarifies two things were conflated: extension *context* on a referenced element applies to all referencing elements (yes), but profile constraints on `Questionnaire.item` do **not** apply to `item.item` unless the profile refers back to itself via the profile-element extension ("I agree with Ewout about the second"). SUSHI implemented: contentReference resolves to the *base* definition, extensions on the referenced element considered applicable.
  https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Clarification.20on.20contentReference/near/224753366 (Ewout) · .../near/225077241 (Grahame) · .../near/225078057 · .../near/224754741 (Chris Moesel SUSHI behavior)
- **2023-04-21..28 · same topic** — Chris Moesel: R5 §5.1.0.10 (FHIR-39753) now says constraints propagate automatically, contradicting the 2021 agreement; John Moehrke: "that kind of profiling is rather useless in my needs"; Grahame 2023-04-28 reads §5.1.0.10 as: an unprofiled `Questionnaire.item.item` "automatically is profiled by the root", but an explicit profile on it "overrides the default reference" — "that's how I think it should be understood". Mint Thompson 2023-09-07 asks about min=1 infinite recursion; Lloyd: "we still need to document the rules ... Discussion about what's overridable will also need to occur".
  https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Clarification.20on.20contentReference/near/351737879 · .../near/353589362 · .../near/389727810
- **2024-03-12 → 2025-06-18 · same topic (Consent case)** — Julian Sass: Simplifier/.NET snapshot keeps `contentReference` on `Consent.provision.provision` (so the profile's `provision.type = deny` propagates and `permit` in sub-provisions fails validation); IG Publisher/Java snapshot **lacks** the contentReference there. Lloyd: fixed values do inherit via contentReference, "If you want different rules for the root and descendants, you need to use an invariant"; Grahame: "I think so?"; asked whether Java should inherit the reference, Grahame: "it doesn't?" (no follow-up). Ewout 2025-06-18: "you EITHER have a contentReference or children, but not both" (the unwritten rule); the .NET validator ignores the reference when children exist, and he will change the generator to remove the contentReference when nested constraints force child expansion and "add back the original typeRef" — the direct provenance of .NET #3177 / DEV-023 flavor 2. The Java side of Julian's case (reference missing on an *unexpanded* child) is a field data point to reconcile with DEV-023's path analysis.
  https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Clarification.20on.20contentReference/near/426098325 · .../near/426155212 · .../near/426288781 · .../near/524681048
- **2022-03-28 → 2022-05-20 · conformance · "STU3 Qustionnaire snapshot generation"** — Hans van Amstel/David Simons: Java emits `http://hl7.org/fhir/StructureDefinition/Questionnaire#Questionnaire.item` where the published spec has `#Questionnaire.item`, crashing the .NET SDK; Grahame (276935595, quoted): "the relative content references must be replaced with absolute content references when a snapshot is generated"; "I believe that this is a dot net tool problem"; Ewout 2022-04-19 asks to say so in the ED.contentReference definition; Ward 2022-05-20: resolved in .NET (firely-net-sdk #2039). → **DEV-023 flavor 1 answered**: Java's global absolutization is Grahame's stated rule; .NET's merged-children-only rewrite is the deviation.
  https://chat.fhir.org/#narrow/stream/179177-conformance/topic/STU3.20Qustionnaire.20snapshot.20generation/near/278523804 · .../near/279405130 · .../near/283088579
- **2025-08-04 · shorthand · "Slice a Slice - FHIR Composition.section"** — Chris Moesel: spec "somewhat ambiguous" — ED.contentReference says "bring across all the rules" and "always reference the non-constrained definition", R5 §5.1.0.10 says propagate.
  https://chat.fhir.org/#narrow/stream/215610-shorthand/topic/Slice.20a.20Slice.20-.20FHIR.20Composition.2Esection/near/532735316
- **2026-03-17 → 2026-05-17 · implementers · "Recursive schemas validation"** — Evgeny Mukha: SDC `sdc-1` on `Questionnaire.item` not applied to nested items by the validator. Grahame: IPS `Composition.section 3..*` read per §5.1.0.10 would make every nested section 3..*; Gino: "I would like constraints to *not* recurse by default, but have a way to indicate they should"; Brian Postlethwaite and Lloyd prefer recursive default but Lloyd: "given that current behavior *isn't* that, making that change could be breaking ... treat recursive as the exception"; Lloyd lists what could cascade (max, slicing, type.profile, constraint — not min); Grahame: "adding an extension that profiles can use on a contentReference - like they can on a type - to say that the content is profiled by profile X". Outcome: FHIR-57265 "Add extension for managing profiling of recursive elements" (`contentReferenceProfile`) and FHIR-57266 "Revise wording at profiling recursive elements" — the R6 flip recorded in ch08. Evgeny's inventory of self-recursive vs alias contentReference resources (2026-05-16) is useful test-corpus material.
  https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Recursive.20schemas.20validation/near/580017127 · .../near/580048080 · .../near/580285339 · .../near/580752316 · .../near/580780286 · .../near/595601466 · .../near/595604371 · .../near/595491463
- **2017-02-07 · implementers · "Profiling a contentReference"** — Chris Moesel asks how to profile `Observation.component.referenceRange` (unroll into the differential?) — oldest trace; **2022-05-18 · conformance · "Profiling: Set a BackboneElement to 0..0"** — Joe Paquette: contentReference "makes it impossible to profile the children" of e.g. `Bundle.entry.link`.
  https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Profiling.20a.20contentReference/near/153870569 · https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Profiling.3A.20Set.20a.20BackboneElement.20to.200.2E.2E0/near/282786692

**Net:** three eras, three answers (2021 Grahame/Ewout: no propagation without profile-element; R5 text: automatic; 2026 FHIR-I: opt-in via `contentReferenceProfile`, R6). Neither engine implements R5 propagation and the community has now walked it back — a version-aware generator should implement literal-path + opt-in and treat R5 §5.1.0.10 as superseded. Absolute-form rewriting on snapshot generation is Grahame's rule (2022).

---

## Cross-question notes for the WGM brief

- Grahame's 2022-12-14 property list (msg 315910873) is the closest thing to a public "what may a profile change" table and bears on OQ-006, OQ-011, OQ-012; several lines are informal/garbled and it was never turned into spec text.
- "Task 9079" (2016) = GF#9079 asking for the snapshot algorithm to be written down; Ewout: "possible, but a daunting task". The spec project is that task, ten years on.
- Marten Smits 2022-03-01: snapshot generation "which I thought was 'normative' by now, or at least, it should be"; Lloyd: "Snapshot generation is a 'process'. It doesn't get to be 'normative'" — the normative-contract question (RFC-012) already has an on-record disagreement.
