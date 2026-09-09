# Zulip sweep 2026-09-02 (b) — slicing normalization + generator enforcement

- **Date:** 2026-09-02 (read-only sweep of chat.fhir.org via `tools/zulip-search.sh`, all public streams, GET only).
- **Cluster:** OQ-020/DEV-020 (type-slicing entry rewrite), OQ-018/DEV-026 (implicit type constraint, renamed-choice anchoring), OQ-011/OQ-014/DEV-028/DEV-035 (what a generator must enforce; error taxonomy; unmatched/out-of-order rows), plus cross-cutting "generator vs validator" and shared-test-suite threads.
- **Method notes:** Zulip search ANDs stemmed terms and returns the *newest* N hits, so old threads were re-fetched with a large window (150–250) and a topic filter. Permalinks follow the form observed in message bodies (`/#narrow/stream/<id>-<stream-with-dashes>/topic/<hex-dot-encoded>/near/<msgid>`; `✔ ` prefix = `.E2.9C.94.20`). Quotes are trimmed to ≤15 words; the message id gives the full text. Sender names as displayed.

## Queries run (hit counts as returned; "→ N in topic" = after topic filter)

| # | query | hits | # | query | hits |
|---|---|---|---|---|---|
| 1 | type slicing closed | 30 | 43 | Snapshot generation Lloyd implementation | 3 |
| 2 | type slicing discriminator $this | 30 | 44 | snapshot generation formal rules | 3 |
| 3 | slicing rules closed snapshot | 13 | 45 | differential order matters | 4 |
| 4 | value[x] slicing closed | 30 | 46 | Modifier Extension mismatch | 30 |
| 5 | type slice entry min | 29 | 47 | constraints on Types, and profiles | 40 |
| 6 | snapshot slicing entry types | 14 | 48 | Snapshot generation validation test cases pass | 3 |
| 7 | choice type slicing restrict | 16 | 49 | Slicing rules on differential | 30 |
| 8 | type slice implicit type | 30 | 50 | Clarify snapshot generation rules | 10 |
| 9 | valueString slice type | 30 | 51 | isModifier profile change | 7 |
| 10 | renamed choice snapshot | 30 | 52 | cardinality wider than base | 1 |
| 11 | slice name valueQuantity | 24 | 53 | profile loosen cardinality | 1 |
| 12 | type slicing sliceName | 30 | 54 | snapshot generator sort differential | 0 |
| 13 | choice type rename R5 | 2 | 55 | Named items are out of order | 30 |
| 14 | snapshot generator validate | 30 | 56 | 13402 snapshotting rules | 4 → 3 in topic |
| 15 | snapshot generation error | 30 | 57 | default values for mustSupport | 44 → 37 in topic |
| 16 | differential order snapshot | 30 | 58 | Type slicing in Profiles (150) | 150 → 0 in topic |
| 17 | no match found differential | 24 | 59 | constraints on Types profiles (100) | 100 → 0 in topic |
| 18 | illegal constraint mustSupport | 1 | 60 | implementation and test cases | 60 → 0 in topic |
| 19 | cardinality loosen differential | 0 | 61 | Recent Snapshot generation changes | 13 → 2 in topic |
| 20 | snapshot generator throw | 10 | 62 | differential mustSupport false snapshot | 15 → 1 in topic |
| 21 | differential path not in base | 30 | 63 | Invalid path name portion | 0 |
| 22 | constraint profile new element | 30 | 64 | Differential contains path not found base | 15 |
| 23 | differential element order | 30 | 65 | Element id constrained choice type | 39 → 20 in topic |
| 24 | url must have one match | 40 | 66 | Slicer vs Slice (60) | 60 → 0 in topic |
| 25 | Type[x] Slices open/closed | 41 | 67 | Snapshot-generation Slicing rules differential | 56 → 55 in topic |
| 26 | Choice Type Renaming (60) | 60 | 68 | New Snapshot functionality | 52 → 45 in topic |
| 27 | Choice Type Renaming (200) | 200 → 61 in topic | 69 | isSummary profile | 40 |
| 28 | Slicing a non-repeating element (60) | 60 | 70 | min greater than base | **failed** (curl connection reset) |
| 29 | Slicing a non-repeating element (250) | 250 → 154 in topic | 71 | differential invalid snapshot generator accept | 0 |
| 30 | MustSupport=false | 30 | 72 | snapshot awry | 3 |
| 31 | No match found generated snapshot order | 18 | 73 | snapshot generator subsequent validation | 1 |
| 32 | snapshot generation test cases | 40 | 74 | validation test cases do you pass | 40 |
| 33 | snapshot generator responsibility validator | 1 | 75 | must-support false base profile | 4 → 1 in topic |
| 34 | Validation issue with partial type slicing | 30 | 76 | slice entry constraints named slices | 29 |
| 35 | Type slicing in Profiles (30) | 30 | 77 | Allowed Types not sliced | 40 |
| 36 | Type slicing with slicing.rules | 40 | 78 | Slice name must be | 40 |
| 37 | Polymorphics type slicing | 15 | 79 | snapshot generator should fail | 7 |
| 38 | Slicing polymorphic Elements | 30 | 80 | slice minimum cardinality sum | 1 |
| 39 | StructDef.differential.MustSupport | 5 → 5 in topic | 81 | golden snapshot expected files | 0 |
| 40 | snapshot generation specified where rules | 11 → 0 in topic | 82 | snapshot generator differences Java .NET | 0 |
| 41 | type slices rule out | 30 → 0 relevant | 83 | Type slicing Profiles effective Timing instant | 0 |
| 42 | effectiveInstant | 10 → 2 in topic | 84 | snapshot generator reject repair | 0 |
| 85 | constraints Types profiles second case | 5 → 2 in topic | 86 | MedicationDispense.quantity snapshot | 0 |
| 87 | SimpleQuantity profile constraints Types | 9 → 3 in topic | 88 | Either you have a single type | 20 → 0 relevant |
| 89 | situation doesn't arise single type element properties | 2 | 90 | Type choice elements issue instances derived profiles | 11 → 10 in topic |

Stream ids used in permalinks: conformance 179177 · IG creation 179252 · tooling 179239 · implementers 179166 · hapi 179167 · shorthand 215610 · FHIR Validator 291844 · committers 179165 · fhir/infrastructure-wg 179280 · fmg 179192.

---

## Q1 — OQ-020 / DEV-020: may/must a generator rewrite the slicing entry of a type slicing?

**Verdict: discussed-unresolved** (overall). Per sub-item:
- **(a) inject `type:$this` when the diff omits it — answered (permitted, not best practice):** Grahame 2019-12-10 and the Redmond DevDays 2019 decision Michel records (always emit the type-slicing entry in the snapshot).
- **(b) force `closed` over an authored `open` — discussed-unresolved, decided by the Java implementer and objected to each time it surfaced** (2019/20 Marten, Ward, Ewout, Chris Grenz; 2020-11 Lloyd; 2025-08 Chris Moesel, Alexander Henket). Grahame's position has hardened from "remains closed because base is closed" (2020) to "type slicing is always closed" (2025), yet in 2020-11 he conceded the auto-closed snapshot "is wrong" and changed the test cases — the probable origin of the *rebuild-CLOSED-then-reopen* mechanism pinned in DEV-020.
- **(c) collapse the entry's type list — split:** the renamed-form collapse (cholesterol profile) was declared a **bug** by Grahame (2022-09-19, wrong style parameter in publication) and fixed; the obs-2b `min>0`/`fixedType` collapse on the R5 form is **not found** on Zulip. Community statements consistently say a type slice does *not* narrow the entry's types.
- **(d) raise the entry `min` — not found.**
- **(e) sliced-base variant stays CLOSED — live exhibit found (2026-07, unresolved):** a derived profile constraining one type slice over an already type-sliced base is rejected by the validator for the other types.

### Threads

**1. #conformance — "Type[x], Slices, open/closed" (2019-11-30 → 2020-03-10)** — Grahame Grieve, Christiaan Knaap, Marco Visser, Marten Smits, Ward Weistra, Ewout Kramer, Chris Grenz.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Type.5Bx.5D.2C.20Slices.2C.20open.2Fclosed/near/182253089
Gist: this is the birth of the `obs-1/2/3` tests (Grahame adds them 2019-12-03, commit `81ae5978`, and asks Lloyd/Marco to review). Grahame's framing: in the base the choice is "sliced by type on $this, and closed"; a derived profile saying `open` "remains closed, irrespective of what C|D|E claim". Marten (2019-12-09, id 182965439) reports the .NET generator "only copies over slicing.rules from the diff" and does not do the discriminator "magic", and objects that obs-2 should not close; Grahame: "actually, you're right. I updated the tests and added some more" (2019-12-11, 183116652). On why closed: "type slices re always closed, because you can't add any more slices" (2020-02-06, 187578005). Ward (2020-02-24, 188924523) reformulates `rules` as an *interpretation hint* for the generator, Grahame agrees (188957834). Ewout (2020-02-25, 189009054): open/closed "turns from an aspect of the slice to an interpretation hint" and proposes the resulting-state table (189009249) — Grahame: "yes. I don't see how it can be any other way" (189010505/10). Chris Grenz (2020-03-10, 190231350): "a diff of `open` over a base of `closed` is an error."
Answer status for (b): Grahame's rule was stated; four Firely/Grenz voices disagreed; no WG resolution.
Other key ids: 182407333 (tests added), 183030333 ("no. but it's allowed" — omitting the discriminator, answers (a)), 183030391 ("still closed when you're done, you just left the others untouched"), 185200262 (Marten: closing prevents a derived profile from adding a slice on valueQuantity), 187027404-187027582 (Grahame's four-bullet ruling).

**2. #IG creation — "Validation issue with partial type slicing" (2020-11-06 → 2020-11-12)** — Grahame Grieve, Lloyd McKenzie, Jean Duteau.
Permalink: https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Validation.20issue.20with.20partial.20type.20slicing/near/215807285
Gist: an `effective[x]` diff with one optional Period slice produced a `closed` snapshot; Grahame: "this is an issue in the snapshot generator" and "17 of the existing 104 snapshot generation tests explore this area" (215807302). Lloyd: "The snapshot is indeed wrong. The differential lists the slicing as open" (215910257); Grahame: "because if it doesn't, every single extension will allow any type" (215917940) — i.e. auto-closing exists to rescue R4 extensions written in the STU3 shorthand. Lloyd (215942937): "methodologically wrong to force all type slices to be closed"; (215972826) "wrong to auto-change 'open' slices to 'closed'". Grahame lists the affected tests with the Java diagnostic "Allowed Types not sliced" for t6, t28, t34, t44a, au2 (215972550), then after "several days of work" pushes fhir-test-cases commit `acceaea7` (2020-11-12, 216432721) for review. This is the most direct on-record statement that forcing `closed` is a normalization compromise, not a rule — and the likely origin of the reopen-if-uncovered branch (`PPP:646-667`).

**3. #tooling — "validator error url must have one match" (2025-08-25 → 2025-08-29)** — Alexander Henket, Grahame Grieve, Chris Moesel.
Permalink: https://chat.fhir.org/#narrow/stream/179239-tooling/topic/validator.20error.20url.20must.20have.20one.20match/near/536543648
Gist: Nictiz profile with an *open* type slicing on `component.value[x]` (slices for Attachment/Quantity/CodeableConcept) fails on a string instance. Grahame: "type slicing is always closed" (536543648) and "we decided after much discussion that type slicing is always closed" (536550386). Chris Moesel asks whether unsliced-but-allowed types are then forbidden downstream; Grahame: fine "if the root type" allows it, and (536681653) that profiles with type slices but no explicit type list were agreed "a few years ago" to be "closing the type slices to the slices they provide" (paraphrase; see id). Alexander Henket (536684803): why is `open` unclear for type slicing but not for value/pattern slicing — "a profile that says open and a validator that claims closed" is more confusing. No resolution; Henket added a "bogus" valueString slice as a workaround (536609350).

**4. #hapi — "snapshot generator throwing Exceptions" (2020-04-26/27)** — Patrick Werner, Grahame Grieve.
Permalink: https://chat.fhir.org/#narrow/stream/179167-hapi/topic/snapshot.20generator.20throwing.20Exceptions/near/195333774
Gist: Java throws `Type slicing with slicing.rules != closed` (and rejects `ordered` type slicing: "there's no such thing as ordered type slicing", 195350355). Patrick: "it's not a stated constraint... Simplifier/.net snapshots/validates the same profile without a problem" (195379072); Grahame: "it's not a stated constraint. But how does it make sense?" (195393399). Shows the Java throw for authored `open` type slicing was a hard error in 2020 and had no spec basis.

**5. #implementers — "Error at path null: Type slicing with slicing.rules != close" (2024-12-17 → 2024-12-20)** — Angela, Lloyd McKenzie, Grahame Grieve.
Permalink: https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Error.20at.20path.20null.3A.20Type.20slicing.20with.20slicing.2Erules.20!.3D.20close/near/489789826
Gist: the same Java error is still live in 2024 (on an HL7 Italy extension). Lloyd: slicing a single-type `value[x]` by type "doesn't make sense" (489789826, 490023251); Grahame: "that means that there's something wrong with the profile" (489915451). Treats the throw as an author-error signal, not a generator defect.

**6. #conformance — "Type slicing in Profiles" (2024-11-21)** — Grahame Grieve.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Type.20slicing.20in.20Profiles/near/483599855
Gist: Grahame's own example — diff `effective[x]` with `open`, `min=1`, and four `max=0` type slices — yields a `closed` entry with the full type list; "something goes awry". His reading (483599935): the base keeps all types, then "the type slices rule out the same types as the differential". Data point that `rules=open`→`closed` rewriting surprises even its author; type list *not* collapsed here (no `min>0` slice).

**7. #tooling — "Type choice elements issue in instances of derived profiles" (2026-07-23 → 2026-07-27, open)** — Victoria Ayvasky, Grahame Grieve.
Permalink: https://chat.fhir.org/#narrow/stream/179239-tooling/topic/Type.20choice.20elements.20issue.20in.20instances.20of.20derived.20profiles/near/612905754
Gist: `SpecimenBase` type-slices `fastingStatus[x]` (CC + Duration, both MS); `SpecimenDerived` constrains only the CC slice; instances with Duration fail: "definition allows for the type CodeableConcept but found type Duration". Grahame: "not for type slicing - you have to list all the types" (612905754), then "I'm not sure - what is the issue?" — unresolved at last message. This is the **sliced-base variant of DEV-020 (OQ-020 e)** observed in the wild: `PPP:1584-1588` (no reopen) collapses the derived entry to the one re-stated slice type.

**8. #conformance — "Slicing a non-repeating element" (2019-08-20, Michel Rutten)** — see Q2 thread 1 for the full thread.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Slicing.20a.20non-repeating.20element/near/173665164
Gist: Michel records the DevDays US (Redmond) agreement to "always emit a type slicing entry to the snapshot component" even when the differential omits it — the community sanction for (a) discriminator synthesis; also 173664806: Michel re-ran the whole Java snapshot test suite on .NET (gist linked).

**9. #conformance — "Choice Type Renaming" (2022-09-17/19, type-list collapse = bug)** — see Q2 thread 3.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Choice.20Type.20Renaming/near/299624346
Gist: Ewout shows the published R4B cholesterol snapshot collapsing `value[x]` to Quantity with a `closed` type slicing after a renamed-form diff (299610757); Chris Moesel: "It should still be allowing all types" (299611698); Grahame: "a very unpleasant discovery" — a version-style parameter was set correctly for the JUnit tests but not for publication/IG publisher (299624346, 299630291/341); fixed with a changelog note (299632771). So for the renamed form, entry type-list collapse is on record as a **bug**, and the JUnit-vs-publisher configuration split is a caution when reading shipped snapshots as golden.

**10. #conformance — "Choice Type Renaming Should Not Constrain Types" (2021-03-24 → 2021-04-22)** — Chris Moesel, Grahame Grieve.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Choice.20Type.20Renaming.20Should.20Not.20Constrain.20Types/near/234799793
Gist: IG Publisher snapshot constrained `value[x]` to the renamed types in the diff; Grahame: "I think it looks like a bug" and points to fhir-test-cases `r5/snapshot-generation` + `manifest.xml` as the authority (234799793); Chris Moesel (235762213) finds no test exercising renaming without an explicit `type` on the entry.

---

## Q2 — OQ-018 / DEV-026: does a type slice imply a type constraint? Renamed form: synthesize a slice or fold onto bare `value[x]`?

**Verdict: answered** for the implicit-constraint question, with a precise split the register should adopt:
- The **entry's** allowed types are **not** narrowed by the presence of a type slice (R4 decision, Lloyd/Chris Grenz/Chris Moesel/Ewout/Grahame all on record; FHIR-12259 → FHIR-33233).
- The **slice itself** is by definition the slice *for one type* — "Type slices implicitly exist" — so a type-less `value[x]:valueString` slice *is* a string slice. Java stamping the inferred type on the slice (J-a) matches Zulip; .NET's full-choice-list slice on the R5 form is the outlier (OQ-018 c).
- **Slice-name convention: answered** — canonical `<stem><Type>` names; Java moved from *silently rewriting* the name in the snapshot to *throwing* on wrong names on 2019-07-25 after Lloyd asked for an error (a documented repair→reject precedent; relevant to Q3).
- **DEV-026 (anchoring): discussed-unresolved (Java rationale on record).** Grahame 2019-07-25: "Either you have a single type, or you can only talk about element properties". Fixture check (2026-09-02): t16's base `…latitude.value[x]` is already `decimal`-only (golden `t16-expected.xml`), and t31's diff *explicitly* states `type=string` on the renamed path, so in both the element is single-typed when children are constrained — folding onto bare `value[x]` is that rule applied. .NET's synthesized `value[x]:valueDecimal` slice also contradicts Michel's own 2017 statement that a single-type constraint "does not introduce an actual slice entry". No thread discusses the R5-form vs renamed-form *representation* choice as such.

### Threads

**1. #conformance — "Slicing a non-repeating element" (2019-05 → 2019-08; 154 messages)** — Grahame Grieve, Lloyd McKenzie, Chris Grenz, Michel Rutten, Ewout Kramer, Alexander Zautke.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Slicing.20a.20non-repeating.20element/near/170899260
Gist: the R4 type-slicing working-out, with the Java tests t6/t29/t43/t44 built live. Key statements:
- Lloyd (170899260): a `valueQuantity` row "constraints the valueQuantity slice. It doesn't prevent the other types" — "a very clear outcome of our meeting two WGMs ago"; (170989124) "Constraining what types are allowed needs to be done on value[x]."
- Chris Grenz (171112789): `Observation.valueQuantity` "is making *no* statement about the other value types"; (171689347) "Type slices implicitly exist."; (171689773) "value[x] establishes the implicit type slices".
- Grahame (171689469): "the path Extension.valueQuantity.value establishes the implicit type slice"; (171687504) "Either you have a single type, or you can only talk about element properties" (children under `[x]`); Lloyd agrees "the paths drive, not the ids" (171690281).
- Slice names: Lloyd wants standard names enforced (168862743); Grahame "should the snapshot generator reject other names?" (169677708); Michel (171434244): spec §id "suggests, but does not actually *require*" the pattern; Chris Grenz (171324051) "T43's input should be rejected"; Grahame (171687483) admits "I rewrite the sliceName in the snapshot without fixing it in the differential"; Lloyd (171688159) "My leaning is to raise an error"; Grahame (171730052): "The generation now blows up if the slice names are wrong in the differential" (t29/t43 updated + clones added → today's t29a/t43a fail tests).
- Michel (171169794): the STU3 compact notation "no longer limits the list of allowed types in R4" — most R4 extensions are therefore mis-stated; not fixable by regenerating snapshots. Michel (171934883): .NET identifies elements "strictly by list order, Path & SliceName", never by id.
- Chris Grenz on t6 (171690123): a closed slicing "needs to enumerate all the possible slices"; Grahame (171690397): "I don't want to have to enumerate all the slices" — snapshots would be massive (materialization — cf. OQ-021).

**2. #conformance — "Element id for constrained choice type elements" + "Choice type element renaming" (2017-09-28 → 2017-10-03)** — Michel Rutten, Chris Grenz, Sean McIlvenna.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Element.20id.20for.20constrained.20choice.20type.20elements/near/153912077 and https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Choice.20type.20element.20renaming/near/153912102
Gist: Chris Grenz: "Type choices are implicitly sliced by type with slices for each allowed type" and it is not valid to slice `value[x]` directly (153911262); "the snapshot should include valueString as slice name for the type slice" (153911711); crystal-clear summary (153912102): (a) `Extension.valueString` is not a valid id, (b) `value[x]:valueString` alone "does NOT constrain valid types". Michel (153912077): a single-type constraint is "considered an 'implicit slice'" that "does not introduce an actual slice entry" — .NET's original design, now contradicted by DEV-026's synthesized slice. Chris Grenz on STU3 (153911855): some implementations read `valueString` as limiting all instances to string, some did not — the ambiguity R4 resolved.

**3. #conformance — "Choice Type Renaming" (2021-09-28 → 2022-10-16; 61 messages)** — Chris Moesel, Lloyd McKenzie, Chris Grenz, Ewout Kramer, Alexander Zautke, Grahame Grieve, Patrick Werner, Josh Mandel.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Choice.20Type.20Renaming/near/297952166
Gist: Lloyd rules the long id form (`…value[x]:valueCoding`) is required (258128039); Chris Grenz: FHIR-33233 "reaffirmed" it and "there is no distinction in the spec between the differential and snapshot forms" (259280420/600). Ewout (259967146): renaming "has been discussed, decided on, been made part of a normative section" — why remove it? Outcome per Grahame (via linked IG-creation thread, summarized by Chris Moesel 261638266): you can "continue to get away with" renaming; the long form is "guaranteed to work"; SUSHI 3.0 drops renaming (261299651, 292858621). Ewout's 2022-09-09 pre-Baltimore summary (297952166) documents both generators' behaviour: Java's renamed-form snapshot = closed/`$this` entry, types collapsed to Quantity; Firely's = `$this` entry (no rules stated), full base type list, single slice. Grahame's test (299305926/6022): `valueString` + `fixedString` → snapshot entry `open`, full 12-type list — "I don't understand the claim that there's a problem" (299306122) — then the cholesterol bug (Q1 thread 9). Patrick/Lloyd float declaring `type:$this` slicing on every `[x]` element in core; Grahame declines: "there's always a downside with slicing" (304283349).

**4. #shorthand — "Error w/ type slices missing discriminator" (2022-06-13/14)** — Chris Moesel, Alexander Zautke.
Permalink: https://chat.fhir.org/#narrow/stream/215610-shorthand/topic/Error.20w.2F.20type.20slices.20missing.20discriminator/near/286076884
Gist: Chris Moesel: "as of R4, slicing *never* implicitly limits the allowed types" (286076884); raises "whether the type slice can just be implicit in the differential, but should be made *explicit* in the snapshot" (286064489) — the OQ-018/OQ-020(a) question in one sentence, unanswered there.

**5. #implementers — "Slicings on polymorphics" (2023-06-27)** — Lloyd McKenzie, Chris Moesel.
Permalink: https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Slicings.20on.20polymorphics/near/370115326
Gist: Lloyd: different rules for `valueCodeableConcept` vs `valueQuantity` "by definition, means you're slicing... by type" (370115326); a profile using `.valueCodeableConcept` as path "isn't really valid"; old tools "play a bit fast and loose with type slicing" (370128520). Chris Moesel: renaming was "deprecated (although it was never *clearly* documented)" and only ever allowed in the differential (370125805, 408365292).

**6. #implementers — "Polymorphics type slicing" (2025-03-17/18)** — Evgeny Mukha, Grahame Grieve.
Permalink: https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Polymorphics.20type.20slicing/near/506428209
Gist: Grahame answers "what is the correct syntax" by pasting a test-case differential: `value[x]` with `type=CodeableConcept`, plus `value[x]:valueCodeableConcept` slice **with explicit type**, plus `.coding` child under the slice (506428209). All exemplars state the type explicitly — consistent with OQ-018's note that no vendored R5-form test omits it.

**7. #committers — "Binding for CodeableConcept inside a choice element" (2026-04-10)** — Lloyd McKenzie.
Permalink: https://chat.fhir.org/#narrow/stream/179165-committers/topic/Binding.20for.20CodeableConcept.20inside.20a.20choice.20element/near/584722236
Gist: for `parameterFocus[x]:parameterFocusCodeableConcept`, "The discriminator is implicit as type-based slicing" — a 2026 restatement that the slicing entry may be left implicit by the author (OQ-020 a).

---

## Q3 — OQ-011 / OQ-014 / DEV-028 / DEV-035: what must a generator validate/enforce; error taxonomy; unmatched and out-of-order rows

**Verdict: discussed-unresolved.** The sharpest finding: Grahame's *stated* policy is identical to .NET's ("its task is to generate the snapshot, not validate the profile", 2022; "doesn't mean that the snapshot generator should fail", 2024) — yet Java throws in at least seven Zulip-attested places, several added by explicit decision (slice names 2019-07; `rules != closed` 2020-04; discriminator equality with base 2022-08, defended by Grahame; mustSupport 2022-02 hard `DefinitionException`; isSummary `java.lang.Error` 2021/2025; out-of-order named slices 2024-07, which Grahame concedes is an implementation artifact; `Differential contains path … not found in the base` since 2017). Both engines profess "don't validate"; both violate it, in different places. The closest thing to a normative *what-may-change* table is Grahame's 2022-12-14 property list — it answers "what the rules are", not "who enforces them". **Drift data point:** mustSupport `false` over `true` was a hard throw in validator 5.6.35 (2022-02) but is warn-and-take in J-b (2026); cause not established here.

### Threads

**1. #conformance — "Modifier Extension mismatch" (2022-03-08/09)** — Grahame Grieve, David Pyke, Josh Mandel, Lloyd McKenzie.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Modifier.20Extension.20mismatch/near/274604861
Gist: the one explicit "where in the stack" debate. Grahame asks whether the generator should "process that, and leave it to the validator" or "fail with an error" (274604272). Josh: the generator "should bail... early and loud" and "snapshots shouldn't define elements that can't exist" (274604740/854). Grahame: the generator misses many things because "it's task is to generate the snapshot, not validate the profile" (274604861). David Pyke: accept if the path is valid, "the validator should scream bloody murder" (274604985); Josh concedes. Lloyd: the generator may not even have the extension definition unless the diff drills in (274607741); Grahame confirms (274609612). Outcome: profile-level validation added to the validator (274648462), i.e. the check went to the *validator*, not the generator. Direct support for the .NET-side policy; contradicts Java's actual throw census.

**2. #conformance — "constraints on Types, and profiles" (2024-03-29 → 2024-04-28)** — Grahame Grieve, Marten Smits, Chris Moesel, Eric Haas.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/constraints.20on.20Types.2C.20and.20profiles/near/435801435
Gist: FHIR-43932 (Quantity declared where base says SimpleQuantity). Marten posts .NET output for both cases (433472231/3123); Chris Moesel enumerates three parent-profile/child-profile scenarios and suggests `type` "must be *complete*" when restated (433523280). Grahame: the second case is an error, "though that doesn't mean that the snapshot generator should fail" — subsequent validation can pick it up (435801435). Second explicit generator-vs-validator statement; relevant to DEV-028 group (e) type/targetProfile derivation.

**3. #conformance — "default values for mustSupport in R4B/R5" (2022-12-14 → 2022-12-20)** — Rob Langezaal, Grahame Grieve, Lloyd McKenzie, Eric Haas, John Moehrke, Chris Moesel, Frank Oemig.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/default.20values.20for.20mustSupport.20in.20R4B.2FR5/near/315910873
Gist: Grahame's per-property mutability list (315910873) — the nearest thing on Zulip to OQ-011's missing generator contract: path/representation/base/contentReference/defaultValue/meaningWhenMissing/isModifier/isModifierReason/isSummary "can't change"; min increase / max decrease; type "can reduce the list", type.code "can't change"; fixed "introduce but not change"; mustHaveValue & mustSupport "false -> true but not true -> false"; binding "introduce or narrow"; obligations "add but not delete"; etc. Also: "R4+ snapshots always populate mustSupport - because the spec says it should" (315905234). Lloyd (315891772) argued `false` can't become `true` either; Grahame: "if it did, it was wrong" about a publisher that once yelled (315908646). Lloyd asks whether the rules hold for other booleans (315908663) — the list is the answer. Chris Moesel questions "sliceName can be changed" (315911991); Grahame: "it can't 'change' it in that sense" (315914835).

**4. #tooling — "✔ HL7 Validator: StructDef.differential.MustSupport=false" (2022-02-25)** — Ken Sinn, Elliot Silver.
Permalink: https://chat.fhir.org/#narrow/stream/179239-tooling/topic/.E2.9C.94.20HL7.20Validator.3A.20StructDef.2Edifferential.2EMustSupport.3Dfalse/near/273172859
Gist: validator 5.6.35 aborts snapshot generation with `DefinitionException: Illegal constraint [must-support = false] when [must-support = true] in the base profile` (273172859). Elliot Silver diagnoses inherited MS from a datatype-profile root (273173936). Evidence that DEV-028 group (h) was a **hard throw** in 2022 — compare J-b's ERROR-and-take in 2026 (drift, unexplained).

**5. isSummary — three threads.** #implementers "sum sign not allowed?" (2025-06-16, Rikke Vestesen Christiansen, Grahame): `java.lang.Error: ... Base isSummary = false, derived isSummary = true`; Grahame: "you can't change isSummary in a profile" (524199848). Permalink: https://chat.fhir.org/#narrow/stream/179166-implementers/topic/sum.20sign.20not.20allowed.3F/near/524199848 — #IG creation "Profiling use of StructureDefinition.isSummary" (2022-01-20, Lloyd 268632758): spec says presence and value "must match the definition in the base definition" — "isSummary is fixed in the resource definition". Permalink: https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Profiling.20use.20of.20StructureDefinition.2EisSummary/near/268632758 — #shorthand "Restrict Observation category" (2021-04-22, Christian Nau 235645433) shows the same `java.lang.Error` in 2021; #fmg "FHIR IG Incubator Creation" (2025-11-07, Bryn Rhodes 554224447) shows it on core `Citation`. Confirms J-b: isSummary is the one frozen property Java enforces by *abort*, consistently since ≤2021.

**6. #committers — "Missing IsModifierReason" (2018-04-02)** — Lloyd McKenzie.
Permalink: https://chat.fhir.org/#narrow/stream/179165-committers/topic/Missing.20IsModifierReason/near/153949659
Gist: "isModifier is never profile-specific. It's fundamental to the meaning of an element." Spec-intent statement behind the †-rule; nothing on generator behaviour.

**7. #implementers — "Java: Snapshot-generation:  Slicing rules on differential" (2022-08-09 → 2023-04-04; 55 messages)** — Patrick Werner, Lloyd McKenzie, Ewout Kramer, Grahame Grieve, Chris Moesel.
Permalink: https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Java.3A.20Snapshot-generation.3A.20.20Slicing.20rules.20on.20differential/near/299370917
Gist: Java throws `Slicing rules on differential (value:system (/open)) do not match those on base (pattern:type (/open))` when a derived profile restates a different discriminator; Patrick: ".net validator accepts this input and just overwrites the whole slicing". Lloyd: "If you're changing slicing, you need to specify the whole set" (292588818); Ewout: then "isn't overwriting the whole slicing the right thing to do?" (293067091). Grahame defends the throw: the tool silently adding a discriminator "doesn't at all seem intuitive" (299370917); "The purpose of the diff is to repeat enough information to connect the dots" (299381709). Chris Moesel pushes back with spec text that the diff states only differences (304459363/304464311). A generator-enforced author-error check (Java) vs diff-wins (.NET), argued and unresolved — sibling of DEV-028.

**8. #conformance — "No match found in the generated snapshot" (2020-11-09)** — Patrick Werner, ryan moehrke.
Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/No.20match.20found.20in.20the.20generated.20snapshot/near/216098025
Gist: the DEV-035 ERROR text in the wild; diagnosis: `.extension` listed before `.id/.meta` — "the bit that sticks out... is '(including order)'". Shows the orphan-drop diagnostic is how authors learn the ordering rule; no discussion of whether the generator *should* sort.

**9. #implementers — "ReSlicing Validation Error: Named items are out of order" (2024-07-05 → 2024-07-08)** — Patrick Werner, Chris Moesel, Grahame Grieve.
Permalink: https://chat.fhir.org/#narrow/stream/179166-implementers/topic/ReSlicing.20Validation.20Error.3A.20Named.20items.20are.20out.20of.20order/near/449788833
Gist: SUSHI groups re-slices under their parent slice; Java throws `Named items are out of order in the slice`. Chris Moesel cites the "adjacent, or... compatible with" spec text (449741686). Grahame: "sushi is clearly not wrong here, against the spec" and it makes intuitive sense (449788833) but "the existing java code doesn't work that way" and rewriting it "isn't going to be fun" (449788915); asks SUSHI to change "nothing for now". Patrick: "hopefully the validator doesn't care about order of slice definitions anymore in the future" (449884525). On-record admission that DEV-035's `NAMED_ITEMS_ARE_OUT_OF_ORDER` throw is an implementation limitation, not a rule.

**10. Ordering / sorting, other.** #IG creation "Elements suddenly missing from snapshot" (2024-01-03 → 2024-01-17, Chris Moesel 412434917: the IGP is "re-ordering them and then complaining that the new order is the wrong order"; 416459658: snapshot in right order but a diff `short` silently not applied) — https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Elements.20suddenly.20missing.20from.20snapshot/near/412434917 . #IG creation "Issue sorting logical model differentials" (2023-12-28, Lloyd 410337103; Chris Moesel 410851066: SUSHI believed it listed differential elements in the base snapshot's order) — https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Issue.20sorting.20logical.20model.20differentials/near/410337103 . #IG creation "order of elements in a structuredefinition" (2020-11-02, Lloyd 215355479: order in snapshot/differential "matters regardless of syntax"). #IG creation "Errors with obligations and logical models" (2026-04-01, Grahame 583029346: "something is wrong with the snapshot generation... presentedForm element is out of order" — a *generator-produced* ordering defect). Together: sorting is a Java-side pre-step (`sortDifferential`) that is itself a bug source; no thread states a generator *must* or *must not* sort.

**11. Path grammar / path not in base.** Java's `Differential contains path X which is not found in the base` has been the author-facing error since 2017 (#german "Adresse Basisprofile", Stefan Lang 153895646) and is thrown from the *sort* step ("Error sorting Differential: ... Differential contains path ... which is actually Element.system, which is not found in the in base Element" — #tooling "Snapshot Issue Hl7 Validator 5.6.50", Patrick Werner 296346159, Ronny Koch 314449964; https://chat.fhir.org/#narrow/stream/179239-tooling/topic/Snapshot.20Issue.20Hl7.20Validator.205.2E6.2E50/near/296346159 ). #implementers "Validation mismatch .NET and Java" (2021-06-15, Thomas Tveit Rosenlund 242697834): profiles validate clean on Simplifier (.NET) but Java aborts with the sort error — the DEV-035 asymmetry reported by a user in 2021. No thread on `..`/empty-segment grammar (obs-unit); query "Invalid path name portion" → 0 hits.

**12. #FHIR Validator — "Validation without snapshot" (2026-03-20/23)** — Chris Grenz, Grahame Grieve, Gino Canessa.
Permalink: https://chat.fhir.org/#narrow/stream/291844-FHIR-Validator/topic/Validation.20without.20snapshot/near/581107653
Gist: Java refuses to generate a snapshot for slicing a non-repeating, non-choice element (`Patient.maritalStatus`) and the validator then reports "has no snapshot". Chris Grenz: "thought we decided to allow slicing non-repeating elements" as a CASE switch (581093186); Grahame: "it automatically generates snapshots, and automatically gives you that error" (581107653); Gino: "You can slice on choice types that are non-repeating" only (581117986). DEV-028 group (g) — Java generator-fatal, .NET check compiled out.

**13. #IG creation — "Slices not inheriting preferred bindings from root" (2025-04-10 → 2025-04-24)** — Eric Haas, Rob Hausam, Grahame Grieve.
Permalink: https://chat.fhir.org/#narrow/stream/179252-IG-creation/topic/Slices.20not.20inheriting.20preferred.20bindings.20from.20root/near/512389512
Gist: Grahame fixed an old bug and now regenerates snapshots on load; propagating the slicer's binding to slices also propagates `mustSupport=true`, which "changes... from false to true for the slice" — "not a safe outcome if I (re)generate the snapshots on the fly" (512389512). His census over test cases (514232999): slicer MS=false but only slice MS: 708; slicer MS=true but all N slices not MS: 67, etc. Cross-cuts OQ-011 (mustSupport direction) and OQ-021 (copy-down); shows Java's regenerate-on-load makes generator rules *retroactively* affect published IGs. Follow-up: #conformance "Recent Snapshot generation changes." (2025-06-27, Eric Haas 526158294 / Grahame "yes" 526266931): snapshots now materialize all children of every slice because of FHIR-50391 slicing-rule clarifications.

**14. #committers — "Fix bug in IG generation" (2019-03-16)** — Grahame Grieve.
Permalink: https://chat.fhir.org/#narrow/stream/179165-committers/topic/Fix.20bug.20in.20IG.20generation/near/160970342
Gist: when early snapshot generation failed, "the partially generated snapshot remains in place" and later checks saw an existing snapshot, so errors went unreported. Historical instance of the "never corrupt" floor being violated by *incomplete* output; the fix was to trap errors, not to define what a failed generation must leave behind.

---

## General findings (cross-cutting)

**G1. Who owns the rules — the shared test suite, by explicit choice.** Lloyd McKenzie, #conformance "How to differentiate extensions in StructureDefinitions?" (2026-01-27, 570433270): the "extensive set of shared tests" is what "define[s] the rules for snapshot generation and validation"; formal rules as spec text were considered but "get super ugly". Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/How.20to.20differentiate.20extensions.20in.20StructureDefinitions.3F/near/570433270 . Same stance in #implementers "Snapshot generation" (2023-02-17, Lloyd 328497646: "By implementation and test cases."; Grahame 2023-03-06, 339781020: "how many of the validation test cases do you pass?"). Permalink: https://chat.fhir.org/#narrow/stream/179166-implementers/topic/Snapshot.20generation/near/328497646 . **FHIR-13402** ("Clarify snapshot generation rules", filed by Chris Grenz 2017-05-11, 153889549) was explicitly deferred: Grahame's FHIR-I status report 2023-02-16 (328143250): "FHIR-13402 Clarify snapshot generation rules: Not happening for R5". Permalink: https://chat.fhir.org/#narrow/stream/179280-fhir.2Finfrastructure-wg/topic/Status.20Report/near/328143250 .

**G2. Ewout's own thread is live.** #conformance "Issue 13402 - Clarify snapshotting rules" (2026-09-01): Ewout 620626646 announces the reverse-engineering effort and asks to file Java/.NET bugs; Grahame 620628162: "sure we can look at that"; **Gino Canessa 620787360 links an independent comparison: https://ginoc.io/202609-snapshot/snapshot-generation-comparison.html ("Concrete observable cases" section)** — a 2026-09 external artifact to fold into Phase 5. Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Issue.2013402.20-.20Clarify.20snapshotting.20rules/near/620787360 . Related: Ward Weistra, #conformance "Inheritance from parent profile or datatype profile?" (2026-02-19, 574760587) lists the prior-art trail: StackOverflow "FHIR Snapshot - Base selection", FHIR-9791 (withdrawn), Chris Grenz's FHIR-Primer wiki "Snapshots Determining Refines", FHIR-13402, FHIR-14400.

**G3. Test-suite history (fhir-test-cases `snapshot-generation`).** 2019-06/07: t6/t29/t43/t44 built in "Slicing a non-repeating element" (Grahame 169784941, 171296588, 171730052; slice-name throw added, t29/t43 cloned). 2019-08-20: Michel re-implements the whole Java suite on .NET and publishes results (173664806, gist). 2019-12-03: obs-1/2/3 added (commit `81ae5978`, 182407403), revised 2019-12-11 after Marten's objection. 2020-11-06: "17 of the existing 104" tests touch type-slicing closure; 2020-11-12: commit `acceaea7` changes expectations (Lloyd reviews). 2021-04-16: Grahame names `r5/snapshot-generation` + `manifest.xml` as *the* authority for bug reports (234799793). 2024-10/11: Pieter Edelman adds tests via PRs #193 and #201 (#tooling "Adding test cases for snapshot generation", 475244917 → 483309013; Grahame: "sure add with all dependencies", 475550678). 2025-05-06: Grahame "we should also discuss if we can merge test cases - I have 100s of them now" (516384650), Ewout agrees (516384899) — #tooling "New Snapshot functionality". Caution from Q1 thread 9: the JUnit run used a different style parameter than publication (2022-09-19), so *shipped* core snapshots are not automatically the same oracle as the suite.

**G4. Generator vs validator — the stated policy and its breaches.** Stated: Grahame 274604861 (2022) and 435801435 (2024); Michel 2017 (#conformance "Slicer vs Slice", 153880251): slicing-entry constraints "SHOULD be enforced! This is the responsibility of the validator" and the .NET generator "do[es] NOT actively merge slice entry constraints onto named slices" (153880264) — the design origin of .NET's no-copy-down (cf. OQ-021, DEV-025). Permalink: https://chat.fhir.org/#narrow/stream/179177-conformance/topic/Slicer.20vs.20Slice/near/153880251 . Breaches (Java throws attested on Zulip): slice names (2019-07), `rules != closed` (2020-04, 2024-12), discriminator equality (2022-08), mustSupport (2022-02), isSummary (2021→2025), named-slice order (2024-07), path-not-in-base / sort failures (2017→2022), slicing non-repeating (2026-03). Grahame has called at least one of these a code limitation rather than a rule (449788915).

**G5. Additional-base inheritance is coming.** #tooling "New Snapshot functionality" (2025-04-29 → 2025-05-06): `structuredefinition-additionalBase` extension; the additional bases' differentials are "merged into the differential prior to generating the snapshot" (515058881); hard rule that slicing in additional bases "must be identical" (515058680); Ewout's alternative sequential-application algorithm (515669845), Grahame: "as long as it's the same outcome" (515722342); merging two differentials "might fail even though they are valid" (516381461). Any generator contract from Phase 5 should leave room for this. Permalink: https://chat.fhir.org/#narrow/stream/179239-tooling/topic/New.20Snapshot.20functionality/near/515058881 .

**G6. JIRA / issue references to sweep next:** FHIR-12259 (R4 type-slicing shorthand origin), FHIR-33233 (long-form ids reaffirmed, 2021-10), FHIR-13402 (clarify rules; deferred R5; Ewout active 2026-09), FHIR-9791 (withdrawn), FHIR-14400, FHIR-43932 (Quantity vs SimpleQuantity), FHIR-50391 (slicing-rule clarifications → 2025-06 materialization change), FHIR-41885 (slice-min arithmetic, validator-side), FHIR-27047 (isSummary review), FHIR-47136 (core vital-signs `component.value[x]` slicing set to `closed`, 2025-03); firely-net-sdk #2065 (choice-type renaming bug, 2022-07).

**G7. Not found on Zulip (for the record):** any discussion of a generator raising the slicing *entry's* `min` (OQ-020 d); of `min`/`max` loosening in a differential and what a generator should do (queries 19, 52, 53, 70 → 0/1/1/failed); of the `..` path-grammar case; of "reject, repair or propagate — but never corrupt" as a principle (query 84 → 0).
