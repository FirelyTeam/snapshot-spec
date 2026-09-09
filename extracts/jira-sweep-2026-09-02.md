# HL7 JIRA sweep for the snapshot-generation open questions — 2026-09-02

Read-only sweep of `jira.hl7.org` (project FHIR) against the open-question list in
`docs/snapshot-spec/14-open-questions.md` (OQ-001…OQ-021), to find tickets that already answer, decide or discuss
each question before the WGM brief is frozen (~2026-09-14). Only GET requests were made (REST `search` + `issue`
endpoints with a browser User-Agent). Raw JSON for every search and every opened ticket is in the session
scratchpad (`…/scratchpad/jira/`), not in this repo.

**Evidence tiers used in the tables below** (so the brief does not over-cite):

| Tier | Meaning |
|---|---|
| **R** | Resolution text of a *resolved* ticket, with a recorded WG vote — HL7 decision on record |
| **C** | A comment (incl. WGM meeting notes) on a ticket — discussion, not a decision |
| **X** | Comment on a *Retracted*/*Duplicate* ticket — discussion whose vehicle was withdrawn |
| **O** | Still-open ticket (Triaged / Waiting for Input / Submitted) |

Status vocabulary is JIRA's (`Published` = applied and published in a release; `Applied` = applied to the CI build;
`Resolved - change required` = decided, change not yet applied; `Resolved - No Change`).

---

## 1. Queries run

All JQL was `project=FHIR AND <query>`, `maxResults=50`. Hit counts are JIRA `total`. Queries with `> 100` hits
were **not reviewed** individually (Lucene tokenizes hyphens, so `profile-element`, `elementdefinition-suppress`,
`standards-status` matched noise); they were re-run narrowed in later rounds. `ERR` = HTTP 400 (bracket syntax) or
connection reset.

### Round 1 (task list)

| Hits | Query |
|---:|---|
| 26 | `text~"snapshot generation"` |
| 19 | `text~"snapshot generator"` |
| 75 | `text~"snapshot" AND text~"slicing"` |
| 149 | `text~"slicing entry"` — not reviewed (re-run narrowed, round 2) |
| 19 | `text~"type slicing" AND text~"closed"` |
| 69 | `text~"type slice" AND text~"discriminator"` |
| ERR | `text~"value[x]" AND text~"slice" AND text~"snapshot"` (HTTP 400; brackets) |
| ERR→0 | `text~"sliceIsConstraining"` (reset; re-run round 2: **0 hits**) |
| 18093 | `text~"profile-element"` — tokenized, not reviewed |
| 10 | `text~"contentReference" AND text~"profile"` |
| 7 | `text~"contentReference" AND text~"constraints"` |
| 8 | `text~"Extension.url" AND text~"fixed"` |
| ERR→0 | `text~"fixedUri" AND text~"extension" AND text~"snapshot"` (reset; re-run: **0**) |
| 44 | `text~"differential" AND text~"order"` |
| 18 | `text~"element order" AND text~"differential"` |
| 55 | `text~"mustSupport" AND text~"inherit"` |
| 7 | `text~"mustSupport" AND text~"false" AND text~"derived"` |
| 2 | `text~"isModifier" AND text~"derived profile"` |
| 2 | `text~"isSummary" AND text~"derived"` |
| 39 | `text~"binding strength" AND text~"derived"` |
| 69 | `text~"element id" AND text~"slice"` |
| 618 | `text~"elementdefinition-suppress"` — tokenized, not reviewed |
| 154 | `text~"standards-status" AND text~"inherited"` — tokenized, not reviewed |
| 2 | `text~"fmm" AND text~"derived profile"` |
| 0 | `text~"additionalBase"` |
| 2 | `text~"imposeProfile" AND text~"snapshot"` (FHIR-28948 mixins — Not Persuasive; FHIR-48481 — SearchParams; neither relevant) |
| 4 | `text~"obligation" AND text~"snapshot"` |
| 17 | `text~"logical model" AND text~"snapshot"` |
| 6 | `text~"interface" AND text~"snapshot"` |
| 143 | `text~"no differential"` — not reviewed (re-run narrowed, rounds 2/4) |
| 19 | `text~"cardinality" AND text~"type profile" AND text~"root"` |
| 6 | `text~"interpretation" AND text~"ElementDefinition" AND text~"table"` |
| 12 | `text~"pattern" AND text~"inherit" AND text~"derived"` |
| 1 | `text~"eld-14"` |
| 8 | `text~"constraint key" AND text~"duplicate"` |
| 7 | `text~"openAtEnd"` |
| 18 | `text~"snapshot" AND text~"differential" AND text~"rules"` |
| 2 | `text~"snapshot" AND text~"mapping" AND text~"StructureDefinition.mapping"` |
| 47 | `text~"element id" AND text~"snapshot"` |
| 206 | `text~"slice" AND text~"non-repeating"` — not reviewed (re-run narrowed) |
| 39 | `text~"slicing" AND text~"max 1"` |
| 4 | `text~"fixed" AND text~"pattern" AND text~"inherit" AND text~"snapshot"` |
| 3 | `text~"modifierExtension" AND text~"ElementDefinition" AND text~"snapshot"` |
| 115 | `text~"type profile" AND text~"snapshot"` — not reviewed (re-run narrowed) |
| 256 | `text~"ellipsis" OR text~"\"...\" append"` — not reviewed (re-run as `three dots`, round 3) |
| 3 | `text~"definition" AND text~"append" AND text~"differential"` |
| 7 | `text~"implicit" AND text~"type" AND text~"slice"` |
| 36 | `text~"snapshot" AND text~"complete"` |
| 20 | `text~"snapshot" AND text~"extension" AND text~"inherit"` |

### Round 2 (syntax fixes / narrowing)

| Hits | Query |
|---:|---|
| ERR | `text~"value\[x\]" …` (HTTP 400 — JIRA rejects escaped brackets too) |
| 0 | `text~"sliceIsConstraining"` |
| 83 | `text~"slice is constraining"` — noise, not reviewed |
| 0 | `text~"fixedUri" AND text~"extension" AND text~"snapshot"` |
| 18217 | `text~"elementdefinition-profile-element"` — tokenized |
| 358 | `text~"profile-element" AND text~"snapshot"` — tokenized |
| 70 | `text~"elementdefinition-suppress" AND text~"snapshot"` (reviewed; yields FHIR-31406/40543) |
| 3 | `text~"suppress" AND text~"snapshot" AND text~"mapping"` |
| 92 | `text~"standards-status" AND text~"snapshot"` — tokenized |
| 171 | `text~"standards-status" AND text~"derived" AND text~"profile"` — tokenized |
| 55 | `text~"no differential" AND text~"snapshot"` (reviewed; nothing on OQ-016) |
| 15 | `text~"slice" AND text~"non-repeating" AND text~"snapshot"` |
| 206 | `text~"non-repeating" AND text~"slicing"` — not reviewed |
| 7 | `text~"type profile" AND text~"snapshot" AND text~"merge"` |
| 0 | `text~"ellipsis" AND text~"definition" AND text~"profile"` |
| 7 | `text~"slicing entry" AND text~"snapshot"` |
| 16 | `text~"slicing entry" AND text~"slices" AND text~"apply"` |
| 0 | `text~"discriminator" AND text~"type" AND text~"slicing" AND text~"generator"` |
| 0 | `text~"snapshot" AND text~"generator" AND text~"mustSupport"` |
| 0 | `text~"snapshot" AND text~"element id" AND text~"regenerat"` |
| 2 | `text~"snapshot" AND text~"id" AND text~"stable"` |
| 3 | `text~"nested extension" AND text~"snapshot"` |
| 6 | `text~"choice type" AND text~"slicing" AND text~"snapshot"` |
| 2 | `text~"snapshot" AND text~"fixed" AND text~"pattern" AND text~"derived"` |
| 0 | `text~"snapshot" AND text~"differential" AND text~"validator" AND text~"generator"` |
| 6 | `text~"slicing" AND text~"closed" AND text~"derived profile"` |
| 3 | `text~"binding" AND text~"strength" AND text~"snapshot" AND text~"loosen"` |
| 1 | `text~"snapshot" AND text~"cardinality" AND text~"datatype profile"` |
| 488 | `text~"$snapshot"` — `$` ignored by Lucene, not reviewed |
| 2 | `text~"IG publisher" AND text~"snapshot" AND text~"differ"` |

### Round 3 (gap filling)

| Hits | Query |
|---:|---|
| 2 | `text~"fixedUri" AND text~"Extension.url"` (both unrelated) |
| 0 | `text~"valueString" AND text~"slice" AND text~"snapshot"` |
| 1 | `text~"constraining" AND text~"slicing" AND text~"ancestor"` (→ FHIR-13545) |
| 0 | `text~"slicing.constraining"` |
| 7 | `text~"three dots"` (→ FHIR-8182; rest unrelated) |
| 7 | `text~"snapshot" AND text~"mapping" AND text~"inherit"` |
| 0 | `text~"snapshot" AND text~"type.profile" AND text~"children"` |
| 4 | `text~"extension" AND text~"root" AND text~"cardinality" AND text~"snapshot"` |
| 16 | `text~"datatype profile" AND text~"snapshot"` |
| 9 | `text~"data type profile" AND text~"cardinality" AND text~"snapshot"` |
| 1 | `text~"snapshot" AND text~"differential" AND text~"generator" AND text~"rules"` (→ FHIR-28441) |
| 0 | `text~"snapshot" AND text~"choice" AND text~"type" AND text~"slice" AND text~"entry"` |
| 0 | `text~"slicing" AND text~"discriminator" AND text~"$this" AND text~"type" AND text~"snapshot"` |
| 2 | `text~"slice" AND text~"minimum" AND text~"sum" AND text~"cardinality"` (→ FHIR-31054) |
| 2 | `text~"snapshot" AND text~"logical model" AND text~"differential"` |
| 4 | `text~"snapshot" AND text~"interface" AND text~"implements"` |
| 0 | `text~"element id" AND text~"reference" AND text~"stable" AND text~"profile"` |
| 8 | `text~"modifierExtension" AND text~"ElementDefinition"` (none about ED.modifierExtension merging) |
| 0 | `text~"snapshot" AND text~"pattern" AND text~"partial"` |
| 1 | `text~"snapshot" AND text~"fixed" AND text~"override" AND text~"derived"` (→ FHIR-7800) |
| 2 | `text~"eld-5"` (2015 ballot items, unrelated) |
| 2 | `text~"contentReference" AND text~"fixed"` |
| 3 | `text~"contentReference" AND text~"binding"` (→ FHIR-14958, FHIR-39350) |
| ERR | `text~"differential" AND text~"element" AND text~"missing" AND text~"parent"` (reset) |
| 8 | `text~"differential" AND text~"sort"` |
| 2 | `text~"sortDifferential"` (→ FHIR-5709) |

### Round 4 (OQ-016 targeted)

| Hits | Query |
|---:|---|
| 7 | `text~"differential" AND text~"empty" AND text~"snapshot"` (sdf-9 FHIRPath fixes only) |
| 9 | `text~"empty differential"` (sdf-9/sdf-20 invariant fixes only) |
| 32 | `text~"without a differential"` (rendering / unrelated) |

**Totals:** ~110 queries; ~1,200 distinct issues listed; 248 core-scoped candidates skimmed by summary;
**~83 tickets opened in full** (description + all comments + resolution description field `customfield_10618` +
vote field `customfield_10510`).

---

## 2. The two named tickets

### FHIR-13402 — "Clarify snapshot generation rules" (Chris Grenz, 2017-05-11)

- **Status:** Resolved – No Change · **Resolution:** Not Persuasive (2023-02-27, vote Rick Geimer/Grahame Grieve
  15-0-1) · component FHIR-core-StructureDefinition · version STU3.
- **Ask:** define (1) element lineage for aggregating constraints and (2) how each ElementDefinition property is
  merged during snapshot generation — pointing at Chris Grenz's FHIR-Primer wiki pages *Snapshots-Determining-Refines*
  and *Aggregating-Profile-Differentials* (both vendored under `FHIR-Primer.wiki/`).
- **Trajectory (the important part):**
  - 2017-09-11 Ewout Kramer: "Agree we need to put this somewhere in the spec."
  - **2018-07-09: Persuasive with Modification, 9-0-0** — "Need to create a whole section devoted to this".
    Flagged "Compatible, substantive; Enhancement".
  - 2018-08-10 Lloyd: "This won't get done until R5. The resolution stands."
  - 2022-09-23 Lloyd: "We don't think that this is realistically going to happen".
  - 2023-02-27: previous resolution **reverted**, motion to re-open 16-0-0, then closed **Not Persuasive**.
- **Final resolution text (R), paraphrased:** after six years "no time or energy has been found to do this";
  some value acknowledged, no timeline; if it happens it will be "done in Confluence, not as a formal part of the
  spec".
- **No concrete merge rules are stated anywhere on the ticket.** Its value for the brief is the trajectory: FHIR-I
  *agreed* in 2018 that a dedicated section is needed, and abandoned it in 2023 for lack of a volunteer — while
  saying the eventual home would be Confluence, not the normative spec. The snapshot-spec project is, in effect,
  the work item this ticket gave up on.

### FHIR-50391 — "Clarifications of the use of must-support and obligations in repeating elements" (Grahame Grieve, 2025-04-25)

- **Status:** Applied · **Resolution:** Persuasive with Modification · component FHIR-core-StructureDefinition.
  Cumulative wrap of the Zulip thread *#IG-creation › Slices not inheriting preferred bindings from root*.
- **Concrete rules in the proposal (R, as modified by comments):**
  1. Add to elementdefinition.html#slicing a note listing which slicer constraints apply across all slices and
     entries — paraphrased: max; type (code, profile, targetProfile, aggregation, versioning); fixed[x]; pattern[x];
     minValue[x]/maxValue[x]; maxLength; constraints; required and extensible bindings incl. additional bindings;
     mustHaveValue; valueAlternatives. Lloyd's comment adds **obligations** to this list.
  2. `mustSupport` (and obligations that don't say otherwise) "can not be assumed to apply to all slices" — "This
     is for legacy reasons"; applicability must be read from documentation. (Lloyd objects for obligations; Grahame
     notes bullet 2 changes if FHIR-50390 is adopted — it was, see table.)
  3. Obligation extension gets a set-vs-individual sub-extension (→ FHIR-50390 `applicable-number`).
  4. profiling.html#obligations: add that the slicing element "is NOT considered a parent for these purposes",
     though "some historical guides may have treated it as such" (Lloyd).
  5. Publisher hints/warnings on mustSupport on repeating elements and on mixed mustSupport across slicer/slices.
- **Generator-relevant sentence (Grahame, comment 2025-04-25), paraphrased:** slicer constraints apply "whether or
  not they are explicitly rendered" on the slicer in the snapshot; they may be missing from slices because of how
  snapshots are generated, and authors wanting them shown "might have to define them explicitly on the slices". —
  i.e. HL7 assumes the snapshot does **not** propagate slicer constraints into slices, and treats that as a
  rendering/validation matter.
- **Contradiction to flag:** FHIR-50267 (below, resolved three days later) says the opposite about mustSupport:
  "If a slicing element declares mustSupport … all slices are automatically mustSupport". FHIR-50391 was *Applied*;
  FHIR-50267's text is *Resolved – change required* and not present in the R6-build HTML snapshot in `spec-html/`.

---

## 3. Headline finding: FHIR-50267 — "Need to clarify how snapshot generation works w/ slices" (Lloyd McKenzie, 2025-04-15)

- **Status:** Resolved – change required · **Resolution:** Persuasive (2025-04-28, vote Gino Canessa/Yunwei Wang
  8-0-1; modification: "downright miserable" → "exceptionally complex") · version R5.
- **Decision text to be added as a new sub-topic "Slicing Snapshot Generation" on the profiling page (R):**
  - two sets of rules theoretically apply to a slice in a derived profile — the parent profile's slice and the
    derived profile's own slicing element — "essentially provides two 'base' elements";
  - "constraints of the base (slicing) elements are *not* included in the snapshots" produced for the slices;
  - slice base selection (paraphrased): the base for a slice in a derived profile is the *same-named slice in the
    parent profile's snapshot*; if the parent has no such slice, the base is the parent's *'base' (slicing) element*;
  - the slicer's constraints nevertheless *apply*: a slicer binding "holds for all slices"; a slicer mustSupport
    means "all slices are automatically mustSupport".
- **Discussion (C):** Eric Haas objected that interpretation should follow "explicitly defined structures and not …
  undocumented 'intrinsic meanings'" and proposed alternative text (slices "inherit the parent properties (not the
  slicer's properties!)"). Lloyd: "This isn't a change, it's documenting what the snapshot generation behavior has
  long been." Grahame: "too late to make this a SHALL … settle for a validation warning".
- **Relevance:** this is the only HL7 decision that states a *snapshot-generation algorithm rule* in so many words
  (slice base selection + no slicer→slice copy). It directly frames OQ-020/OQ-021 — and Java's preprocessor
  copy-down of slicing-entry *children* into every slice (DEV-025/DEV-033, 77% of the min/mustSupport divergence)
  is either outside the sentence's scope (it speaks of the entry's *own* constraints — properties, which both engines
  already refuse to copy: `APPLY_PROPERTIES_FROM_SLICER=false`) or contradicts a 2025 decision that the golden files
  bless. That ambiguity is the WGM question. Note the text has **not** landed in the R6 CI build we captured.

---

## 4. Relevant-ticket table

Sorted by theme. Quotes are ≤ 15 words. "Tier" per §0.

| Key | Summary | Status / Resolution (vote) | Created | OQ | Gist | Tier |
|---|---|---|---|---|---|---|
| **Slicing entry, slices, materialization** |||||||
| [FHIR-50267](https://jira.hl7.org/browse/FHIR-50267) | Need to clarify how snapshot generation works w/ slices | Resolved – change required / Persuasive (8-0-1) | 2025-04-15 | 020, 021 | See §3. Slice base = same-named slice in parent snapshot, else parent's base element; "constraints of the base (slicing) elements are *not* included" in slice snapshots, yet still apply. Grahame: validation warning, not SHALL. | R |
| [FHIR-50391](https://jira.hl7.org/browse/FHIR-50391) | must-support and obligations in repeating elements | Applied / PwM | 2025-04-25 | 021, 020, 011 | See §2. Lists which slicer constraints bind all slices; mustSupport excluded "for legacy reasons"; slicer constraints apply "whether or not they are explicitly rendered" in the snapshot. Contradicts 50267 on mustSupport. | R |
| [FHIR-50390](https://jira.hl7.org/browse/FHIR-50390) | Add "applicable-number" component to obligation extension | Published / Persuasive (11-0-0) | 2025-04-25 | 021 | Set-vs-individual for obligations on repeating/sliced elements; "on a slicing element, the applicable-number applies across all slices". | R |
| [FHIR-8969](https://jira.hl7.org/browse/FHIR-8969) | Behaviour of new constraints in slicing entry | Published / PwM (8-0-1) | 2015-11-04 | 020, 021 | Slicing entry "can apply any constraints that would be allowed if … merely profiling the element". Lloyd (C): nothing forbidden except what makes slicing nonsensical, e.g. "constraining to a single type for a type slice" — i.e. the entry should keep its type list. | R + C |
| [FHIR-8286](https://jira.hl7.org/browse/FHIR-8286) | Slicing entry needs to repeat full definition, not only root | Published / PwM (9-0-0) | 2015-06-10 | 021(a) | Ewout's ticket: the entry's "unconstrained definition includes the children, and update the tooling to populate this". Sanctions materializing entry children (cf. Java's inline dump for contentReference entries, `PPP:402-419`). Explicit open-slice idea deferred. | R |
| [FHIR-8975](https://jira.hl7.org/browse/FHIR-8975) | Re-slicing without `<name>` combined with named re-slicing | Published / Persuasive (11-0-0) | 2015-11-04 | 021(b) | An unnamed constraint on a sliced element in a derived profile "is not replacing the existing constraints, but merely adding to all slices". | R |
| [FHIR-8976](https://jira.hl7.org/browse/FHIR-8976) | Overriding element in the slicing entry using a differential | Resolved – No Change / Retracted | 2015-11-04 | 020, 014 | Ewout (X): agreed with Grahame that "slices in differentials should still have a slicing entry (with extensions as the exception)"; combined entry+first-slice is an authoring error. | X |
| [FHIR-12179](https://jira.hl7.org/browse/FHIR-12179) | Update build snapshot to inherit constraints | Published / Persuasive (8-0-3) | 2016-09-25 | 021, 002 | WGM 2016-09: "official build snapshot should inherit all applicable constraints from all applied base and type profiles" (ele-1 on every element). Chris (C): also mappings, aliases, "all aggregable elements". | R |
| [FHIR-7783](https://jira.hl7.org/browse/FHIR-7783) | Profile snapshot views are not complete | Resolved – No Change / Not Persuasive (4-0-0) | 2015-05-10 | 021(a) | 2015 complaint that snapshots omit base elements/attributes; answered as "methodology mis-understanding" (see 6066) and "we allow it … we chose that consequence" — pre-dates the current complete-snapshot expectation. | R |
| [FHIR-13839](https://jira.hl7.org/browse/FHIR-13839) | Ambiguities: combined typeref + child constraints | Resolved – No Change / Retracted | 2017-09-11 | 002, 017, 021(d) | Grahame's San Diego 2017 note (X): if the validator cannot trust completeness it must validate children *and* typeRef; "snapshot generators should be sure to generate the snap shot completely" — Chris Grenz agreed. | X |
| **Type slicing / choice types** |||||||
| [FHIR-12259](https://jira.hl7.org/browse/FHIR-12259) | Clarify implicit type slicing behavior | Published / Persuasive (14-0-**18**) | 2016-10-10 | 018, 020(c) | Chris Grenz's proposal adopted: (1) the `[x]` element "must remain in the profile snapshot as-is"; (2) a type-specific path "shall not be interpreted as constraining allowed types"; (3) choice elements are implicitly type-sliced, id `Patient.deceased[x]:deceasedBoolean`; (4) constraints on a named type slice "apply only to instances of that type"; (5) either path form is legal. Grahame (C): "retain the functionality" of `@type` discriminators. | R + C |
| [FHIR-6066](https://jira.hl7.org/browse/FHIR-6066) / [FHIR-6093](https://jira.hl7.org/browse/FHIR-6093) | `[x]` expansion in snapshots / renamed paths | Published / PwM (9-0-0) | 2015-04 | 018 | Constraining a sub-type "does not imply the other sub-types are to be omitted"; renamed snapshot paths lose the choice identity. Resolution introduced `base.path/min/max` ("it's an error not to have these if you have a base"). | R |
| [FHIR-8970](https://jira.hl7.org/browse/FHIR-8970) → [FHIR-10034](https://jira.hl7.org/browse/FHIR-10034) | Is a type required with type-slice shorthand? / eld-6, eld-7 | Duplicate → Published / Persuasive (7-0-0) | 2015/2016 | 018 | Type need not be restated in a differential when setting fixed[x]/pattern[x] (eld-6/7 relaxed) — the shorthand alone identifies the type. | R |
| [FHIR-18264](https://jira.hl7.org/browse/FHIR-18264) | ID when slicing a type in a choice | Resolved – No Change / Not Persuasive (7-0-0) | 2018-09-24 | 009, 018 | Re-slicing a type slice: "cannot occur … repeating elements cannot have a choice of types". Chris Grenz (C) disputes: a non-repeating element can still be re-sliced (`Foo.value[x]:valueCoding/SNOMED`). | R (+ dissent C) |
| [FHIR-15900](https://jira.hl7.org/browse/FHIR-15900) | Snapshot generation problem with polymorphic types | Published / Persuasive | 2018-04-08 | 018 | Diff had `occurrenceDateTime`, snapshot `occurrence[x]`: "an id in the differential that's not in the snapshot — that should not occur". Fixed in tooling. Bears on DEV-026 (Java anchors on bare `value[x]`). | R |
| **Slicing rules lattice, cardinality, non-repeating** |||||||
| [FHIR-28619](https://jira.hl7.org/browse/FHIR-28619) | Allow slicing of a non-repeating element to define a choice | Resolved – No Change / Not Persuasive (17-0-0) | 2020-09-28 | 003 | "for non-repeating elements, slicing is *only* presently supported by type"; use invariants or slice `coding`; extending tooling is "a *considerable* amount of work". Covers base `max=1`; the derived-profile-caps-to-1 carve-out is not addressed. | R |
| [FHIR-3623](https://jira.hl7.org/browse/FHIR-3623) | Clarify how re-slicing works across profiles | Published / PwM (6-0-2) | 2014-10-11 | 005, 006 | Origin of §5.1.0.17: "open to closed, unordered to ordered"; child discriminators must include parent's — "additional discriminator paths may be added, but none … removed". | R |
| [FHIR-17821](https://jira.hl7.org/browse/FHIR-17821) | Re-slicing section update | Published / PwM (7-0-2) | 2018-09-07 | 005 | Reworded to two bullets: `rules` open→closed; `ordered` false→true. openAtEnd→closed asked, not answered. | R |
| [FHIR-31054](https://jira.hl7.org/browse/FHIR-31054) | Add clarity on cardinality of sliced element | Published / PwM (30-0-0) | 2021-02-11 | 005, 020(d) | "The sum of the minimum cardinalities of the slices SHOULD be less than or equal to m"; "Will add a warning to the validator". A validator SHOULD — not a generator recompute. (Firely-reported; Ewout's PR #1274.) | R |
| [FHIR-31400](https://jira.hl7.org/browse/FHIR-31400) | Let eld-1 only be applied to snapshot | Published / PwM (20-0-4) | 2021-03-03 | 005, 020(a) | Derived profiles need not repeat the discriminator; eld-1 moved to `snapshot`. Implies the generator carries the base slicing into the snapshot when the diff restates the entry without discriminator. | R |
| [FHIR-5581](https://jira.hl7.org/browse/FHIR-5581) | Note that "openAtEnd" is discouraged | Published / Persuasive (6-0-0) | 2015-01-28 | 005 | Adds the discouragement note only. | R |
| [FHIR-13461](https://jira.hl7.org/browse/FHIR-13461) | Build should raise warnings/infos in tight slice rules | Retracted (was Persuasive 2018) | 2017-05-30 | 005, 014 | openAtEnd on non-repeating → error; openAtEnd without orderMeaning → warning. Moved to `org.hl7.fhir.core#518`. | X |
| [FHIR-17469](https://jira.hl7.org/browse/FHIR-17469) | Java validator handles closed slicings incorrectly | Published / Persuasive | 2018-07-10 | 005 | Closed slicing implies a de-facto upper bound = Σ slice max; fixed in validator — enforcement placed in the validator. | R |
| [FHIR-52917](https://jira.hl7.org/browse/FHIR-52917) | Derived profiles of complex extensions: no non-fullURL slices | Applied / Persuasive (10-0-0) | 2025-09-18 | (ext. slicing) | Complex extensions SHOULD be open-sliced; derived profiles adding extension slices "SHALL have full URLs". | R |
| **sliceIsConstraining** |||||||
| [FHIR-13545](https://jira.hl7.org/browse/FHIR-13545) | Distinguishing referencing an existing slice vs. constraining a new slice | Published / PwM (5-0-0) | 2017-06-19 | 006 | Origin of `slicing.constraining` (→ `sliceIsConstraining`): "If set to true, an ancestor profile SHALL have a slicing definition with this name"; requirement: "Allows detection of a situation where an ancestor profile adds or removes slicing". Decided San Diego 2017 evening; Zulip #conformance thread linked. No generator obligation stated. | R |
| **contentReference / recursion** |||||||
| [FHIR-14958](https://jira.hl7.org/browse/FHIR-14958) | Clarify contentReference resolving (esp. in derived profiles) | Published / PwM (7-0-0) | 2018-01-29 | 004 | contentReferences "bring across all the rules … including bindings, invariants etc."; only in specializations, "cannot be changed and always reference the non-constrained definition". Michel Rutten (C): resolving from the *referencing* profile would recursively inherit constraints — wrong. | R + C |
| [FHIR-13139](https://jira.hl7.org/browse/FHIR-13139) | Unable to profile element that use contentReference | Published / Persuasive | 2017-03-28 | 004 | Tooling fixed so children of a contentReference element can be constrained; example added to the test IG. | R |
| [FHIR-39350](https://jira.hl7.org/browse/FHIR-39350) | Limit contentReference to BackboneElements | Resolved – No Change / Not Persuasive (11-0-1) | 2022-10-28 | 004 | Declined: logical models may want to take "a set of ValueSet bindings by reference". | R |
| [FHIR-57266](https://jira.hl7.org/browse/FHIR-57266) | Revise wording at profiling recursive elements | Applied / Persuasive (6-0-0) | 2026-05-17 | 004, 021(c) | **R6 reversal** of R5 §5.1.0.10: constraints "only apply to the literally stated path"; recursion via new extension. Already captured (without ticket no.) in `R5-R6build-deltas.md` l.75-77. | R |
| [FHIR-57265](https://jira.hl7.org/browse/FHIR-57265) | Add extension for managing profiling of recursive elements | Resolved – change required / PwM (5-0-1) | 2026-05-17 | 004, 021(c) | `contentReferenceProfile` (0..1): "a profile on the element identified by the ContentReference that also applies to this element". Reference must be to an element definition. | R |
| **Root element / type profiles (OQ-001/002)** |||||||
| [FHIR-19756](https://jira.hl7.org/browse/FHIR-19756) | Need to clarify meaning of root cardinality | Published / Persuasive (5-0-0) | 2018-12-11 | 001 | "The cardinality on a type places constraints on references to that type" — referencing profiles "must fall within the cardinality bounds of the type". Chris Grenz (C): WGM decided the generator "should not pull the root element's cardinality into the snapshot". Lloyd (C): "ensuring that the snapshot complies". | R + C |
| [FHIR-36738](https://jira.hl7.org/browse/FHIR-36738) | Extension / ElementDefinition Clarifications | Published / Persuasive (16-0-2) | 2022-04-07 | 001 | New "Root elements" section: if root cardinality is 0..1, a referencing element "cannot have a maximum cardinality greater than 1"; "A regular extension cannot be 'constrained' to be a modifier." | R |
| [FHIR-48664](https://jira.hl7.org/browse/FHIR-48664) | Allow binding on first element (of datatype profiles) | Applied / PwM (11-0-0) | 2024-10-17 | 001, 002 | Interpretation table changed: datatype-profile root binding now allowed; "Will ask the Java and Firely snapshot generator authors to account for this change." Lloyd (C, 2025-01): consider invariants for the table's prohibited rows; waiting for Grahame and Ewout. | R + C |
| [FHIR-9791](https://jira.hl7.org/browse/FHIR-9791) | Clarify base for snapshot with profiled types | Resolved – No Change / Retracted | 2016-04-07 | 002 | Ewout's 2016 proposal (X): descriptive elements from type profiles ignored unless exactly one profile and (base is core type, or referenced from a new slice). "Ewout & Chris are supposed to draft something" — never landed. | X |
| **Non-inheritable extensions** |||||||
| [FHIR-28441](https://jira.hl7.org/browse/FHIR-28441) | Create an extension to control "inheritance" of extensions | Published / PwM (11-0-0) | 2020-09-07 | 019 | `snapshot-behavior` extension "that indicates what rules a snapshot generator must follow"; 5 behaviors (add-to / override-subset / override-free / must-equal-propagate / **not propagated**). Lloyd's comment classifies ~45 extensions. Behavior-5 set (17) = **.NET's blocklist exactly** (fmm, fmm-no-warnings, hierarchy, interface, normative-version, applicable-version, category, codegen-super, security-category, standards-status, summary, wg, replaces, resource-approvalDate/-effectivePeriod/-lastReviewDate, isCommonBinding). `explicit-type-name`, `bindingName`, `profile-element` are class 4 (always propagate) here — but FHIR-27535's comments (closed as "already resolved in FHIR-28441") say explicit-type-name should *not* inherit, so the record conflicts on that one url; Java's `NON_INHERITED_ED_URLS` follows 27535. | R + C |
| [FHIR-27535](https://jira.hl7.org/browse/FHIR-27535) | Add indicator that extension should not be inherited | Resolved – No Change / Not Persuasive (13-0-2) | 2020-05-20 | 019 | "already been resolved in FHIR-28441". Comments (Chris Moesel, SUSHI) confirm via Zulip: fmm, wg, standards-status, normative-version, explicit-type-name, summary, translatable should not inherit. | R + C |
| **Suppression / verbosity** |||||||
| [FHIR-6125](https://jira.hl7.org/browse/FHIR-6125) | Profiling should document rules for documentation & mappings | Published / PwM (5-0-0) | 2015-04-20 | 008, 007 | Differential "cannot remove definitions, usage notes, rationale or mappings"; a directly-authored snapshot "MAY remove usage notes, rationale, and mappings" (merely not telling the user). | R |
| [FHIR-20385](https://jira.hl7.org/browse/FHIR-20385) | No way to override mapping inclusions | Published / PwM (15-0-0) | 2019-02-11 | 008 | profiling.html updated (2023): profiles can remove code/comment/requirements/alias/example/mapping "invalidated or made irrelevant by constraints". Tooling deferred to FHIR-40543. | R |
| [FHIR-31406](https://jira.hl7.org/browse/FHIR-31406) | Support "deleting" unwanted inherited properties | Published / PwM (**4-1-3**) | 2021-03-03 | 008 | Defines `elementdefinition-suppress` (comment, requirements, alias, example, mapping, label, code): "the element property should be removed from the corresponding snapshot.element during snapshot generation". Comment: suppressed comment must match the overridden value. | R |
| [FHIR-40543](https://jira.hl7.org/browse/FHIR-40543) | Tooling support to suppress inheriting comment stuff in snapshot | **Triaged** (reopened 2026-08-13) | 2023-02-27 | 008 | Proposes `removeOnSnapshot` (SD-level, coded), `suppress` (boolean, "can only appear if in a differential"), `intrinsic` (constraint/binding: keep in snapshot, hide in rendering), IG params for mapping suppression. Deferred for ballot; still open. | O |
| **Element ids** |||||||
| [FHIR-9843](https://jira.hl7.org/browse/FHIR-9843) | Provide elementdefinition.id as reliable way to reference an element | Published / PwM (6-0-0) | 2016-04-20 | 009, 017 | id = path + slice name, "must be present and distinct within the profile", used as `#` anchors (`…#code.SNOMED.code`); Chris (C) wants it usable from contentReference, discriminator, SearchParameter. | R + C |
| [FHIR-12182](https://jira.hl7.org/browse/FHIR-12182) | element.id format and location | Published / Persuasive (8-0-3) | 2016-09-25 | 009 | WGM 2016-09: id is derived, `pathpart:slicename/reslicename`, equals path when unsliced. | R |
| [FHIR-14091](https://jira.hl7.org/browse/FHIR-14091) | IG Publisher changes element ids | Resolved – No Change / Not Persuasive (3-0-0) | 2017-10-25 | 009, 015 | Lloyd (C): "elementId values must follow a specific algorithm. If the ids weren't correct, the publishing process will 'fix' them." Closed as tooling ("open an issue at … fhir-ig-publisher"). | C |
| [FHIR-20465](https://jira.hl7.org/browse/FHIR-20465) | Element.id inconsistent between snapshot and differential | Published / Persuasive (8-0-0) | 2019-02-25 | 009 | "The differential is correct, and so the snapshot needs to be corrected" — ids must agree across diff/snapshot. | R |
| **"..." convention** |||||||
| [FHIR-8182](https://jira.hl7.org/browse/FHIR-8182) | A snapshot generator should resolve three dots to include the base narrative | Resolved – No Change / Not Persuasive (4-0-0) | 2015-05-21 | 010 | Only HL7 acknowledgement: "It applies to elements with a 'string' data type in StructureDefinition" (ideally markdown-typed) — broader than either engine's 3-property list; documenting it was declined. | R |
| **Merge semantics / what a generator enforces** |||||||
| [FHIR-9079](https://jira.hl7.org/browse/FHIR-9079) | Add computable directive for snapshot rules | Resolved – No Change / Not Persuasive (8-0-0, 2022) | 2015-11-23 | 011, 019 | Was **Persuasive 11-0-0 (2016)**: "Ewout and Chris are going to merge their docu" describing the merge algorithmically. Reverted 2022: "We have the test cases and we have the reference implementations. That will have to do." Chris (C): needed mostly for *extensions* on ED (FMM etc.) — i.e. OQ-019. | R + C |
| [FHIR-31405](https://jira.hl7.org/browse/FHIR-31405) | Clarify expected behavior of ElementDefinition properties in differential | **Waiting for Input** | 2021-03-03 | 011, 012, 013 | Asks per array property: replace vs append vs upsert, and for upserts "if the updates are 'sparse' or 'complete'", and whether slices inherit differently. Lloyd (2021): "We don't feel comfortable trying to land this without your input" (Firely + Chris). Ewout (2023-07-12) documented .NET's rules (primitive overwrite; complex "non-null subelements … overwrite base, others are kept untouched" = the OQ-012 overlay; collection equality rules). **Still open — the natural landing place for the WGM outcome.** | O |
| [FHIR-7801](https://jira.hl7.org/browse/FHIR-7801) | Complete rules for overrides in derived profiles (binding strength) | Published / PwM (18-0-2) | 2015-05-10 | 011 | Binding-strength lattice table added "to give guidance to profile authors and developers of authoring tools", not because verifiable in instances. | R |
| [FHIR-7800](https://jira.hl7.org/browse/FHIR-7800) / [FHIR-7802](https://jira.hl7.org/browse/FHIR-7802) | Tools aren't checking cardinality / binding-strength alignment | Published / Persuasive | 2015-05-10 | 011, 014 | Illegal loosening was being published in snapshots; "fixed so that they check and generate warnings" (Java tooling, 2015). Enforcement posture chosen: **warn**. | R |
| [FHIR-14272](https://jira.hl7.org/browse/FHIR-14272) | Clarify invariants for ElementDefinition.isSummary | Published / PwM (8-0-0) | 2017-12-07 | 011 | The isSummary rule applies "In resource and data type definitions" only; cannot be an invariant. Michel (C) points at the interpretation table's † rows. | R |
| [FHIR-20405](https://jira.hl7.org/browse/FHIR-20405) | SDC questionnaire overrides isModifier from core | Resolved – No Change / Not Persuasive (7-0-0) | 2019-02-20 | 011 | Reporter: "snapshot generation just ignores the illegal setting in differential" (isModifier=false vs base true). Resolution misread it as missing declarations; Henket (C) corrects. No ruling on the illegal `false`. | R + C |
| [FHIR-38134](https://jira.hl7.org/browse/FHIR-38134) | Is the "meaning" of Must Support inherited? | Published / Persuasive (18-0-0) | 2022-09-06 | 011 | The *meaning* of mustSupport defined in the base artifact applies to elements it flagged; newly flagged elements take the derived IG's meaning. | R |
| [FHIR-34434](https://jira.hl7.org/browse/FHIR-34434) | Clarify how duplicate mappings are handled | Published / PwM (11-0-0) | 2021-12-08 | 007, 011 | ED-level: a differential mapping with the same identity "*replaces* the mapping element(s) in the parent"; warning invariant one-mapping-per-identity. Says nothing about SD-level `mapping` merging. | R |
| [FHIR-37692](https://jira.hl7.org/browse/FHIR-37692) | Ensure ElementDefinition.constraint are unique in a StructureDefinition | Published / PwM (18-0-0) | 2022-06-28 | 011 (eld-14) | Constraint ids "SHALL be defined such that they are unique within a StructureDefinition", though they may recur in derived models due to slicing; enforced by tooling. Was first resolved differently (re-opened 17-0-0). | R |
| [FHIR-3621](https://jira.hl7.org/browse/FHIR-3621) | Clarify how complex types work with fixed values in profiles | Published / PwM | 2014-10-10 | 012 (adjacent) | Defines fixed[x] (exact, no additional elements) and pattern[x] (constraint by example) for *instances*; silent on diff-over-base merge. | R |
| **Author errors / taxonomy / preprocessing** |||||||
| [FHIR-13400](https://jira.hl7.org/browse/FHIR-13400) | Add sliceName constraints | Published / PwM (4-0-0) | 2017-05-11 | 014 | Root element (path without '.') may not have a sliceName in either view; first diff element may (it need not be the root); "slicename … unless a preceding element … declares slicing" cannot be expressed in FHIRPath. | R |
| [FHIR-12849](https://jira.hl7.org/browse/FHIR-12849) | Profiles w/ SliceName in Snapshot/Differential Root Element | Published / Persuasive | 2017-02-16 | 014, 015 | Tooling stopped carrying root sliceNames; Michel (C): interpretation table prohibits them, Forge warns. | R |
| [FHIR-10033](https://jira.hl7.org/browse/FHIR-10033) | sdf-8 disallows legal differentials | Published / Persuasive (7-0-0) | 2016-05-16 | 014 | Differentials need not start at the root: "all paths must equal type or start with type + '.'". | R |
| [FHIR-25537](https://jira.hl7.org/browse/FHIR-25537) / [FHIR-5709](https://jira.hl7.org/browse/FHIR-5709) | Out-of-order differential elements / `sortDifferential()` | Published | 2020 / 2015 | 014 | Both treat diff order as author responsibility fixed in the source; Java's `sortDifferential` bug history (slicing breaks the sort). | R |
| **Below-root expansion (fragment / profile-element)** |||||||
| [FHIR-13973](https://jira.hl7.org/browse/FHIR-13973) | Ability to profile Composition.section as a data type | Published / PwM (11-0-0) | 2017-09-28 | 017 | Sanctions the fragment form as "(as yet undocumented) functionality": `type.profile` may point into a StructureDefinition "by appending a # and the id of the element" — by element **id**. Example to be added. | R |
| [FHIR-13386](https://jira.hl7.org/browse/FHIR-13386) | Change Composition.section to a datatype | Published / NPwM (15-0-0) | 2017-05-11 | 017 | "the feature is currently undocumented. We need an example" — assigned to Grahame; never documented in the core spec. | R |
| [FHIR-49079](https://jira.hl7.org/browse/FHIR-49079) | Clarify definition of elementdefinition-profile-element | Published / Persuasive (10-0-0) | 2025-01-01 | 017 | Definition now: "Provides the snapshot.element.id of the element … to use as the starting point for validation" — by **id**, framed as a *validator* instruction. | R |
| **Operation / misc** |||||||
| [FHIR-13795](https://jira.hl7.org/browse/FHIR-13795) / [FHIR-41351](https://jira.hl7.org/browse/FHIR-41351) | `$snapshot` operation / idempotent | Published / Applied | 2017 / 2023 | — | Operation defined ("we'd rather keep it simple"; no dependency-resolution spec); declared idempotent 2023. | R |
| [FHIR-37791](https://jira.hl7.org/browse/FHIR-37791) | Define behaviors for interfaces | Published / PwM (15-0-0) | 2022-07-18 | (interface) | `type.profile`/`targetProfile` may name an interface; `type.code` may not; "explain ramifications of constraints being declared on interfaces and how they propagate into snapshots". | R |
| [FHIR-27839](https://jira.hl7.org/browse/FHIR-27839) | StructureDefinition.baseDefinition regarding Patterns | Published / PwM (8-0-0) | 2020-06-17 | (interface) | Interfaces removed from the `baseDefinition` hierarchy; referenced via `implements` extension 0..*. | R |
| [FHIR-8541](https://jira.hl7.org/browse/FHIR-8541) | How interpret StructureDefinition differential? | Published / Persuasive | 2015-08-07 | 016 (adjacent) | Grahame (C): specializations have no base; "you have to use the type on the root element to determine the parent". | C |

*Not opened (summaries only, judged tangential):* FHIR-50509, FHIR-53114, FHIR-55396, FHIR-56936, FHIR-19242,
FHIR-5963, FHIR-14400, FHIR-9129, FHIR-12558, FHIR-19964, FHIR-8611, FHIR-53946.

---

## 5. Per-OQ verdict map

Verdicts use exactly {**answered**, **discussed-unresolved**, **not found**}. Where sub-questions differ, each gets
a line. "Answered" means an HL7 decision on record (tier R) speaks to the question — not that either implementation
conforms to it.

| OQ | Verdict | Tickets | What the record says / what remains |
|---|---|---|---|
| **OQ-001** cardinality diamond | **answered** | FHIR-19756 (R+C), FHIR-36738 (R), FHIR-48664 (R) | A type's root cardinality is a *bound the referencing element must respect* ("must fall within the cardinality bounds of the type"), not a value to merge; WGM (Chris Grenz, C) explicitly decided the generator "should not pull the root element's cardinality into the snapshot". .NET's silent `0..1 → 0..*` is wrong twice over (it merges, and it loosens). Java's "datatype root never merged" satisfies 19756 but cannot surface a datatype-root *binding* in the snapshot, which FHIR-48664 now allows and explicitly hands to the generators ("Will ask the Java and Firely snapshot generator authors"). Residual: whether "comply" means *tighten toward* the root or *validate only*. |
| **OQ-002** type profile vs base priority | **discussed-unresolved** | FHIR-9791 (X), FHIR-13839 (X), FHIR-12179 (R), FHIR-48664 (R), FHIR-31405 (O) | Ewout's 2016 proposal (descriptive props from type profiles ignored except single-profile/new-slice cases) was never landed; San Diego 2017 note says generators "should be sure to generate the snap shot completely" (favours closing over type profiles); FHIR-12179 wants the build snapshot to inherit "from all applied base and type profiles". No ruling on precedence when both constrain the same property. FHIR-48664 is a live 2025 obligation neither engine has reconciled. |
| **OQ-003** slicing non-repeating | **answered** (unexcused case) | FHIR-28619 (R, 17-0-0) | Slicing a non-repeating element "is *only* presently supported by type (and then only for polymorphic elements)"; use invariants or slice `coding`. Java's reject matches; .NET's disabled reject does not. Ewout's carve-out (derived profile caps a *sliced* base to 0..1) and Java's "sum total … limited to 1" exception are **not** addressed by the ticket (it concerns base `max=1`). |
| **OQ-004** contentReference + children | **answered** (semantics); residual on materialization | FHIR-14958 (R), FHIR-57266 (R, R6), FHIR-57265 (R), FHIR-39350 (R) | 14958: a contentReference "bring[s] across all the rules … including bindings, invariants etc." — the target's value-domain properties *apply*; nothing says the snapshot must restate them, and the reference persists. 57266 (R6, Applied) reverses R5's recursive propagation: constraints "only apply to the literally stated path"; `contentReferenceProfile` carries profiles down. 39350 keeps contentReference legal on non-backbone elements (bindings by reference desirable). Open: should eld-5's prohibition be restated as "undefined after dereferencing"? Not asked anywhere. |
| **OQ-005** enforce closed/openAtEnd | **discussed-unresolved** (rules answered; generator obligation not) | FHIR-3623 (R), FHIR-17821 (R), FHIR-31054 (R), FHIR-31400 (R), FHIR-5581 (R), FHIR-17469 (R), FHIR-13461 (X), FHIR-7800 (R) | The lattice is decided (open→closed, false→true, discriminator superset). Every enforcement decision on record points at the **validator**: slice-min sum is a SHOULD with a validator warning (31054), closed-slicing arithmetic fixed in the validator (17469), tooling "check and generate warnings" (7800). No ticket assigns the generator an obligation; 31400 implies it must *carry* base slicing when the diff omits the discriminator. Java's CLOSED→OPENATEND tolerance and no-ordered-change rule have no basis in any ticket. |
| **OQ-006** sliceIsConstraining | **discussed-unresolved** | FHIR-13545 (R) | Origin ticket defines the semantics ("an ancestor profile SHALL have a slicing definition with this name") and the purpose ("Allows detection…") — detection language, i.e. validation. Nothing says whether a generator rejects, proceeds, or ignores. Java ignores; .NET rejects. |
| **OQ-007** SD-level `mapping` merge | **not found** | (FHIR-34434, FHIR-6125 are ED-level / removal only) | 34434 rules ED `mapping` replacement by identity; 6125 says a differential cannot remove mappings. No ticket discusses merging `StructureDefinition.mapping` declarations so ED `mapping.identity` resolves. |
| **OQ-008** verbosity / suppress | **answered** | FHIR-31406 (R, 4-1-3), FHIR-20385 (R), FHIR-6125 (R), FHIR-40543 (O) | Suppression is sanctioned: `elementdefinition-suppress` → "the element property should be removed from the corresponding snapshot.element during snapshot generation"; profiling.html (2023) allows removing code/comment/requirements/alias/example/mapping made irrelevant. The "tools generate complete verbose snapshots" sentence is stale relative to these decisions. The tooling design (removeOnSnapshot / suppress / intrinsic) is still open in 40543, reopened 2026-08. |
| **OQ-009** element id stability | **answered** | FHIR-9843 (R), FHIR-12182 (R), FHIR-20465 (R), FHIR-14091 (C), FHIR-18264 (R) | Ids are *derived* data (path + slice names, "must be present and distinct"); "If the ids weren't correct, the publishing process will 'fix' them. This is expected behavior." (Lloyd, C); diff and snapshot ids must agree. Regeneration by both engines is sanctioned. Residual (not raised anywhere): author ids as external-reference targets. |
| **OQ-010** "..." append | **discussed-unresolved** | FHIR-8182 (R, Not Persuasive) | FHIR-I acknowledged the convention in 2015 as applying "to elements with a 'string' data type in StructureDefinition" (ideally markdown) — broader than the 3 properties both engines implement — but declined to document it. Never revisited. RFC remains justified. |
| **OQ-011** what must a generator enforce | **discussed-unresolved** | FHIR-13402 (R), FHIR-9079 (R), FHIR-31405 (O), FHIR-7801 (R), FHIR-7800/7802 (R), FHIR-14272 (R), FHIR-20405 (R+C), FHIR-37692 (R), FHIR-38134 (R) | Two attempts to specify merge rules were *Persuasive* (2016, 2018) then reverted to Not Persuasive (2022, 2023: "no time or energy"; "test cases and reference implementations … will have to do"). The one live vehicle is **FHIR-31405 (Waiting for Input since 2021)** where FHIR-I asked for Firely's input and Ewout supplied .NET's rules in 2023. Enforcement posture decided piecemeal: tooling *warns* on illegal min/max and binding strength (7800/7802); isSummary rule scoped to specializations (14272); eld-14 uniqueness "enforce … with the tooling" (37692). |
| **OQ-012** partial overlay of fixed/pattern | **discussed-unresolved** | FHIR-31405 (O), FHIR-3621 (R) | 31405 asks the exact question ("define if the updates are 'sparse' or 'complete'") and Ewout's comment documents the .NET overlay; no ruling. 3621 defines fixed/pattern instance semantics only. |
| **OQ-013** ED.modifierExtension merge | **not found** | — | 8 hits for `modifierExtension AND ElementDefinition`, none about merging ED-level modifier extensions in snapshots (36738 is about *defining* modifier extensions). |
| **OQ-014** error taxonomy | **discussed-unresolved** | FHIR-7800/7802 (R), FHIR-13400 (R), FHIR-10033 (R), FHIR-12849 (R), FHIR-13461 (X), FHIR-50267 (C), FHIR-31054 (R) | Every decision on record about an author error chooses **warning in tooling/validator** (7800, 7802, 31054, 13461's intent) or says a rule cannot be a FHIRPath invariant (13400). Grahame on 50267: "too late to make this a SHALL … settle for a validation warning". No ticket discusses generator throw/drop/repair classes or a "never corrupt" floor. |
| **OQ-015** generator mutates input | **discussed-unresolved** | FHIR-14091 (C), FHIR-12849 (R) | Publisher rewriting element ids is called "expected behavior" (comment, not resolution); tooling stripping root sliceNames was the fix for 12849. Nothing on repairing constraint content (slice names, root type). |
| **OQ-016** differential-less SD | **not found** | (FHIR-8541 C adjacent) | Four targeted queries (`no differential`, `empty differential`, `without a differential`, `differential AND empty AND snapshot`) return only sdf-9/sdf-20 invariant fixes and rendering issues. 8541 only covers specializations without a base. |
| **OQ-017** below-root expansion syntax | **answered** (syntax + id semantics); residual on generator obligation | FHIR-13973 (R, 11-0-0), FHIR-13386 (R), FHIR-49079 (R), FHIR-9843 (R), FHIR-13839 (X) | *Both* syntaxes are sanctioned and both address an element **id**: the `url#id` fragment was explicitly blessed in 2017 ("as yet undocumented") and never documented (13386); the extension's 2025 definition names `snapshot.element.id` as "the starting point for validation". So .NET's sliceName comparison is wrong and Java's silent strip of `#fragment` drops a sanctioned form. Generator obligation: only the retracted-ticket note "generators should be sure to generate the snap shot completely" (13839). WGM residual: is the fragment superseded by the extension, and must children be expanded from the nominated sub-tree? |
| **OQ-018** implicit type constraint | **answered** | FHIR-12259 (R, 14-0-18), FHIR-6066/6093 (R), FHIR-10034 (R), FHIR-15900 (R) | 12259 adopted Chris Grenz's rules: the `[x]` entry "must remain … as-is" (allowed types unchanged by a type-specific path); a named type slice's constraints "apply only to instances of that type"; ids `value[x]:valueString`; both path forms legal. Read against the engines: .NET's renamed-form single-type slice matches item 4; .NET's R5-form full-choice-list slice arguably does not; Java's entry type-list collapse violates items 1-2 (and 8969's "single type for a type slice" is nonsensical); Java's bare-`value[x]` anchoring (DEV-026) sits against items 3/5 and 15900. Caveat: 18 abstentions and Grahame's "retain the functionality" note on `@type`. |
| **OQ-019** non-inheritable extensions | **answered** | FHIR-28441 (R+C, 11-0-0), FHIR-27535 (R+C) | Policy = per-extension metadata (`snapshot-behavior`, 5 classes) — exactly the candidate RFC. Lloyd's class-5 ("does not propagate") list is **.NET's 17-url blocklist verbatim**; Java's `NON_INHERITED_ED_URLS` omits hierarchy/interface/codegen-super/replaces/resource-dates; on `explicit-type-name` the record itself conflicts (28441 class 4 = always propagate vs 27535 comments = do not inherit), and Java follows 27535. Residual: the `snapshot-behavior` extension's actual publication state and whether either engine reads it. |
| **OQ-020** slicing entry of a type slicing | **discussed-unresolved** overall; (c) and (d) **answered** | FHIR-12259 (R), FHIR-8969 (R+C), FHIR-31054 (R), FHIR-50267 (R), FHIR-31400 (R), FHIR-3623 (R) | (c) type list: entry stays as-is (12259 item 1; 8969 comment) → Java's collapse is unsanctioned. (d) min: slice-min sum is a validator SHOULD/warning (31054), not a generator recompute → Java's raise is unsanctioned. (a) discriminator synthesis: nothing; 31400 only says a derived diff may *omit* it. (b) rewriting an explicit `rules` value: nothing — 3623 lets only the *author* tighten open→closed. Sub-question normalize-vs-propagate: 50267's Grahame comment favours validation warnings. |
| **OQ-021** how much must a snapshot materialize | **discussed-unresolved**; (b) narrowed, (d) leans | FHIR-50267 (R), FHIR-50391 (R), FHIR-8286 (R), FHIR-8975 (R), FHIR-12179 (R), FHIR-13839 (X), FHIR-57266 (R), FHIR-7783 (R) | (b) slicer→slice copy: 50267 says the slicing element's constraints "are *not* included in the snapshots produced for the slices" and 50391 says they apply "whether or not they are explicitly rendered". Whether "constraints" covers the entry's *child rows* (Java's preprocessor copy-down) is unstated — the examples are the entry's own properties, which both engines already don't copy. Either Java's copy-down is out of scope or a 2025 decision is violated by the reference implementation and blessed by golden files. (a) entry children: 8286 wants the entry's unconstrained definition to "include the children" and tooling to populate them. (c) recursion: R6 (57266) makes chains inert unless `contentReferenceProfile` — reshapes the question. (d) type profiles: 12179 + 13839 favour closing over type profiles (complete snapshots). |

**Summary of verdicts:** answered — OQ-001, 003, 004, 008, 009, 017, 018, 019 (8) · discussed-unresolved —
OQ-002, 005, 006, 010, 011, 012, 014, 015, 020, 021 (10) · not found — OQ-007, 013, 016 (3).

---

## 6. Cross-cutting observations for the brief

1. **HL7 has twice agreed the rules should be written and twice given up.** FHIR-9079 (Persuasive 11-0-0 in 2016:
   "Ewout and Chris are going to merge their docu") and FHIR-13402 (Persuasive 9-0-0 in 2018: "create a whole
   section") were both reverted to Not Persuasive in 2022/2023 for lack of a volunteer, with the stated fallback
   "test cases and reference implementations … will have to do" and "Confluence, not … the spec". The snapshot-spec
   project is the abandoned work item; the brief can open with that.
2. **FHIR-31405 is the live vehicle.** Waiting for Input since 2021, FHIR-I asked Firely/Chris for the merge rules;
   Ewout's 2023 comment is the last activity. Any WGM outcome on OQ-011/OQ-012 should be posted there rather than in a
   new ticket.
3. **FHIR-50267 vs FHIR-50391 contradict each other on mustSupport** (auto-inherit vs "can not be assumed"), three
   days apart, same WG. 50391 is Applied; 50267's text is not in the R6 build we captured. Raise before citing either.
4. **The one on-record generator rule (50267) is ambiguous exactly where the engines diverge** — entry *properties*
   (both engines: don't copy) vs entry *children* (Java copies, .NET doesn't).
5. **FHIR-28441's classification is .NET's blocklist**; the Java list deviates from the HL7-adopted classification on
   five urls (and the record itself conflicts on `explicit-type-name`). The OQ-019 RFC already exists as a decision —
   what's missing is publication/uptake of `snapshot-behavior`.
6. **Root-element decisions (19756/36738/48664) indict .NET on OQ-001 and Java on the 2025 root-binding change** —
   neither engine implements "referencing element must comply with the type root" as decided.
7. **Every enforcement decision on record lands in the validator as a warning** (7800, 7802, 17469, 31054, 13461,
   50267-comment). There is no HL7 precedent for a generator throwing or repairing — Java's throw census and .NET's
   silent-keep-base are both without a mandate.
8. **R6 changed the recursion premise** (57266/57265): OQ-004 and OQ-021(c) must be asked against R6 text.

## 7. Top five tickets

1. **FHIR-50267** — the only on-record snapshot-generation *algorithm* rule (slice base selection; no slicer→slice copy).
2. **FHIR-31405** — the open ticket where FHIR-I is waiting for Firely's merge-rule input (OQ-011/012).
3. **FHIR-28441** — `snapshot-behavior` extension + the 45-extension classification that equals .NET's blocklist (OQ-019).
4. **FHIR-12259** — implicit type slicing decided in Chris Grenz's terms (OQ-018, OQ-020(c)).
5. **FHIR-19756** (+36738, 48664) — root cardinality/binding as a bound on the referencing element (OQ-001/002).

Framing pair: **FHIR-13402 + FHIR-9079** (agreed, then abandoned). R6 wildcard: **FHIR-57266**.

---

## 8. Corrections and verifications (2026-09-03, WGM brief v2 pass)

- **FHIR-28441 — the classification is resolution-tier, not comment-tier.** The resolution field reads "The rules
  will be those listed in the comment below", so Lloyd's 5-class list and the per-extension classification are
  incorporated by reference into the decision. Re-tier the §4 row from "R + C" to **R (list incorporated)**; the
  only comment-tier item left on OQ-019 is the FHIR-27535 discussion on `explicit-type-name`.
- **Count fix (§4 FHIR-28441 row, §6 item 5, §5 OQ-019):** Java's `NON_INHERITED_ED_URLS` omits **nine** of the 17
  class-5 urls, not five — it carries 8 (isCommonBinding, fmm, standards-status, category, security-category, wg,
  normative-version, summary) and omits fmm-no-warnings, hierarchy, interface, applicable-version, codegen-super,
  replaces, resource-approvalDate, resource-effectivePeriod, resource-lastReviewDate.
- **FHIR-15900 is weaker than the §4 row implies:** resolution field = "Auto-approved" (tooling fix, no WG vote);
  "that should not occur" is the reporter's description. Tier **C-equivalent** (description of an auto-approved
  ticket), not R.
- **FHIR-50267 tiers:** the "Slicing Snapshot Generation" text is Lloyd's description adopted verbatim by the
  resolution ("Do this, changing 'downright miserable' to 'exceptionally complex'") → R. Lloyd's "This isn't a
  change …" = comment #2; Grahame's "too late to make this a SHALL … validation warning" = comment #3 (both C).
- **Resolution-field location:** the HL7 JIRA "Resolution Description" is `customfield_10618` in the REST payload
  (`/rest/api/2/issue/FHIR-nnnn?fields=summary,resolution,status,description,comment,customfield_10618`, anonymous
  GET with a browser User-Agent). Verified verbatim against that field on 2026-09-03: FHIR-8969 ("merely profiling"
  = R; "single type" = Lloyd comment #1), FHIR-12259 (resolution "Make change as proposed" → the five numbered
  items in the description are R), FHIR-14958 ("bring across all the rules" = R), FHIR-31054 ("SHOULD be less than
  or equal to m" + "Will add a warning to the validator" = R), FHIR-19756 ("must fall within the cardinality
  bounds" = R), FHIR-13973 ("appending a # and the id of the element" = R), FHIR-49079 ("starting point for
  validation" = R), FHIR-48664 ("account for this change" = R), FHIR-34434 ("*replaces* the mapping element(s) in
  the parent" = R), FHIR-3623 ("none of the existing paths can be removed" = R), FHIR-28619 ("*only* presently
  supported by type" = R), FHIR-14091 ("publishing process will 'fix' them" = Lloyd comment #2, C).
