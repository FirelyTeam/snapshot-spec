# RFC-015 research extract — `Extension.url` fixed-value convention in snapshots

Date: 2026-09-02. Sources: local spec HTML (`spec-html/R5`, `spec-html/R6-build`; R4 has no
`extensibility.html`/`defining-extensions.html` locally — R4 statements are NOT verified here),
`tools/pkgs/hl7.fhir.uv.extensions.tgz` (package.json: 5.2.0, fhirVersions 5.0.0),
`fhir-test-cases/r5/snapshot-generation/`, and the .NET/Java findings already in the spec repo
(ch07, DEV-037). Version stamps: R5 pages = `hl7.fhir.core#5.0.0`, generated 2023-03-26;
R6 pages = `hl7.fhir.r6.core#6.0.0-ballot4`, generated 2026-08-18 (page footer, both files).

## (a) Findings per question

### Q1 — Does the spec say the definition/snapshot carries a fixed `Extension.url`?

**Yes, partially — and the register currently says "none".** R5 `defining-extensions.html`
§2.1.5.1.4 "Use of ElementDefinition in Extension Definitions" (lines 271-309) is a bullet list of
which ElementDefinition properties belong on which element of an extension definition. Under
`Extension.url` it lists two items: "Cardinality = 1...1 (fixed)" and "value = canonical URL (fixed)".
The same list is in R6-build lines 367-428, byte-identical for the `Extension.url` bullets (393-400).

Qualifications:
- It is framed as "guidance for the correct usage" (line 275) — no SHALL/SHOULD.
- It addresses the *extension definition* as authored; it does not distinguish differential from
  snapshot and says nothing about a generator's obligation.
- It says "fixed", not `fixed[x]` vs `pattern[x]`, and not `fixedUri` vs `fixedString`
  (the Zulip-settled `fixedUri` choice has no spec text).
- It says nothing about the value for nested parts (`Extension.extension:x.url`); the adjacent
  `Extension.extension` bullet only says ".extension is automatically sliced by url" and that
  nested extensions may be defined "in-line ... or by reference (using .type.profile)" (305-306).

The **instance-level** url rule is in `extensibility.html` §2.1.5.0.1 "Extension Element", in the
"Notes:" list after the structure table — R5 line 581: url SHALL be a URL (not URN), SHALL reference
the defining SD's canonical, and "Except for child extensions defined within complex extensions, the
URL SHALL be an absolute URL". The relative-identity rule for complex parts is R5 line 662-663
("local/relative to the reference to the extension definition"; R6 878-879 unchanged).

Nothing in `profiling.html` (R4, R5) or `elementdefinition.html` (R5) mentions `Extension.url` at all
(grep: no hits for `Extension.url` / `extension.*fixed`). profiling §5.1.0.18 (R5 line 1099) only says
an extension definition "defines the extension element using the same details used to profile" other
elements.

Register fixups implied (not applied — SDK repo is read-only for this task):
- ch07 lines 55-57 ("pure tooling convention") and ch07 spec-gap 3, and DEV-037 "Spec basis: none"
  should cite defining-extensions §2.1.5.1.4 as a *partial, non-normative* basis.

### Q2 — Nested parts of complex extensions: relative vs absolute; `fixedUri` for `Extension.extension.url`?

- R5 extensibility line 581/662: child extensions of complex extensions are the exception to the
  absolute-URL rule, and parts are "local/relative" — instance level.
- R6-build extensibility line 732 tightens the exception's *scope*: "Except for child extension
  slices introduced in a structure definition defining a complex extension whose base is
  `http://hl7.org/fhir/StructureDefinition/Extension'`" (sic — stray `'` inside `<code>`).
  I.e. only in-line parts of a *definition* may be relative.
- R6-build line 734 (NEW bullet, absent in R5): complex extensions SHOULD have open slicing on
  `.extension`; a **derived profile** on a complex extension that adds slices SHALL give them full
  URLs, "because derived profiles are not defining the extension - i.e., they are not establishing
  the 'url' value for the extension".
- No page anywhere mentions `fixedUri` on `Extension.extension.url`. The nested-part fixed value
  is unstated in R5 and R6.

Consequences for the RFC. R6 line 732 exempts "child extension slices introduced in a structure
definition defining a complex extension" from the absolute-URL rule without distinguishing in-line
from by-reference (`type.profile`) parts, so the spec settles only: (i) in-line parts of a definition
are relative (name), (ii) slices added by a *derived profile* are absolute (line 734). The value for a
by-reference part *inside a definition* is genuinely unstated; the proposal's "referenced canonical"
branch for that case is a proposed resolution drawn from .NET's `fixExtensionUrl` split (type-profile
url, else slice name) and the 56 absolute slice urls in the goldens, not from spec text. (A profile's
extension slice fixing the extension's canonical is plain instance identity and uncontroversial.)

### Q3 — R6-build vs R5 differences

| Item | R5 (5.0.0) | R6-build (6.0.0-ballot4, 2026-08-18) |
|---|---|---|
| defining-extensions §2.1.5.1.4 `Extension.url` bullets | "1..1 (fixed)", "value = canonical URL (fixed)" | identical (lines 393-400) |
| defining-extensions §2.1.5.1.4 new text | — | adds: definition has no context so must-support on the definition is discouraged (433-437) — not url-related |
| defining-extensions context section | 3-row context table | reworked bullet list on specialization matching (280-302) — not url-related |
| extensibility §2.1.5.0.1 Notes, url bullet | exception = "child extensions defined within complex extensions" (581) | exception narrowed to "child extension slices introduced in a structure definition defining a complex extension whose base is .../Extension" (732) |
| extensibility Notes, derived-profile bullet | absent | NEW (734): derived profiles adding slices SHALL use full URLs; derived profiles "are not establishing the 'url' value" |
| extensibility complex-content paragraph | "local/relative" (662) | unchanged (878) |
| `Extension.url` type | uri (string in R4) | uri; Σ flag added (per ch07 R6 note) |

The R6 derived-profile bullet directly answers one DEV-037 sub-question: a derived extension profile
does **not** establish a url, so its snapshot keeping the *base* extension's `fixedUri`
(`ext-sort-issue`) is correct under R6 text — DEV-037's "arguably wrong for both" should be
retracted. **Erratum candidate for RFC-014:** stray apostrophe after `.../StructureDefinition/Extension`
inside `<code>` at R6-build extensibility.html line 732.

### Q4 — Core extension pack practice (hl7.fhir.uv.extensions 5.2.0)

Four SDs inspected in detail, then a census of all extension SDs in the package
(`type == "Extension"`; all 632 have `baseDefinition = .../Extension`, `derivation = constraint` —
the pack contains **no derived-extension profiles**, so it cannot test the R6 derived-profile rule).

| SD | differential `Extension.url` | snapshot `Extension.url` | nested `Extension.extension:x.url` (diff and snap) |
|---|---|---|---|
| patient-birthPlace (simple) | `fixedUri` = canonical | `fixedUri` = canonical; min/max 1..1; type `System.String` (+fhir-type ext) | n/a (`Extension.extension` 0..0) |
| iso21090-ADXP-additionalLocator (simple) | `fixedUri` = canonical | same | n/a |
| patient-citizenship (complex) | `fixedUri` = canonical | same | `code`, `period` — relative slice name, `fixedUri`, type uri |
| geolocation (complex) | `fixedUri` = canonical | same | `latitude`, `longitude` — relative |

Census (632 extension SDs): 632/632 snapshot root `fixedUri == canonical`; 632/632 **differential**
root `fixedUri == canonical`; 272 nested `.url` elements — 272 relative, 0 absolute, 0 unfixed;
0 `patternUri`. Of the 272, **270 have `fixedUri == parent sliceName`**; 2 differ
(`capabilitystatement-search-parameter-use` `:required.url` = `allow-standalone`;
`questionnaire-optionRestriction` `:expression.url` = `requirements`) — so the authored local name,
not the slice name, is the source of truth. **0** nested parts are defined by reference
(`type.profile` on `Extension.extension:x`), so the pack has no evidence for the by-reference branch.
So the pack's convention is universal and *authored* (present in the differential),
which means both .NET and Java produce the same snapshot for every pack SD. The pack shows the
convention is expected to be present; it does **not** discriminate "generator synthesizes" from
"author states" because every author did state it.

### Q5 — fhir-test-cases r5/snapshot-generation evidence

| Case (manifest) | Kind | Input differential `Extension.url` | Expected snapshot | Nested |
|---|---|---|---|---|
| `ext-recursion-2` ("extension profile refers to itself") | complex extension definition, base Extension | element absent; 0 `fixedUri` in file | `Extension.url` present (line 166), **no fixed**, type `System.String` | none fixed |
| `au2` ("more work on extensions") | complex extension definition, base Extension | root NOT fixed; nested parts fixed `namespace-id`, `universal-id`, `universal-id-type` (relative) | root **no fixed**; nested relative kept | relative |
| `ext-sort-issue` (register `ext-sort-issue-base`) | derived extension profile, base `extension-Communication.topic` | `Extension.url` element present (min 1, mustSupport) — **no fixedUri** | 1 `fixedUri` (line 136) = **base extension's** canonical, inherited from base snapshot | n/a |
| `ext-codeable-reference` (workflow-reason) | extension definition | `fixedUri` = canonical (authored) | `fixedUri` = canonical (line 193) | n/a |
| `extension-type-slice` ("type slicing in sub-extension value") | complex extension definition (`url` line 4 = `.../capstmt/StructureDefinition/feature`) | root `fixedUri` = canonical (input line 122, element `Extension.url`, verified equal); in-line parts `code`, `value` relative | preserved (246, 404, 508) | relative |
| `ext-ccuk` (`fail="true"`, "trying to slice on the root") | complex extension definition | root absolute + parts relative, authored | no expected file | — |
| `ext-mgmt`/`ext-mgmt2`/`ext-profile` | Patient/HumanName profiles | no `.url` elements | no fixedUri | — |

Golden census over all `*-expected.*` snapshots, extension `.url` elements: 6 root `fixedUri`,
73 nested relative, 56 slice/by-reference absolute, **10 unfixed** — two are definition roots
(`ext-recursion-2`, `au2`), the other eight are profile extension slices (`ca-patient`
`Patient.identifier:JHN.extension:myExtension.url` — slice has `type.profile` = ext-identifierversion,
url left unfixed; `nl-med`, `sd-comp-hist`, `t28`, `t31`, `telus-oo`, `choice_min_1`). .NET's
`fixExtensionUrl` would fill all ten (canonical for the two roots; type-profile url / slice name for the
eight slices), so these are the golden files on which the two engines provably differ.

### .NET and Java (from the spec repo; re-verified file positions on this checkout)

- .NET: `SnapshotGenerator.cs` `fixExtensionUrl` (this checkout `src/Hl7.Fhir.Conformance/Specification/Snapshot/SnapshotGenerator.cs:1743`, invoked at `:1120-1124` "generate [...]extension.url/fixedUri if missing"); comment at `:1776-1777` records the Zulip decision for `fixedUri` not `fixedString`. Rule: definition root → canonical; profile extension element → type-profile url, else slice name; only when no `fixed[x]` is present; never for `modifierExtension` (DEV-019).
- Java: no generator-side fixed-url write in the generation walk (DEV-037 / ch07 "Java never fixes it"); inherits an authored/base one; golden files bless unfixed roots (`ext-recursion-2`, `au2`).

## (b) Evidence table

| Question | Spec text (R5 / R6-build) | Core extension pack 5.2.0 | fhir-test-cases (Java goldens) | .NET | Java |
|---|---|---|---|---|---|
| Definition root `Extension.url` fixed to canonical? | defining-extensions §2.1.5.1.4: "value = canonical URL (fixed)" — guidance, no SHALL; unchanged R6 | 632/632 yes, in differential AND snapshot | 4 of 6 definitions authored it; 2 (`ext-recursion-2`, `au2`) not, and goldens keep it unfixed | synthesizes if absent | inherits only |
| Which property (`fixedUri`/`pattern`/`fixedString`)? | unstated | `fixedUri` always (0 pattern) | `fixedUri` always | `fixedUri` (Zulip) | whatever authored |
| Nested in-line part value | instance rule: relative to definition (R5 662; R6 732 limits to defs based on Extension) | 272/272 relative; 270 = sliceName, 2 = other authored local name | 73 relative (all in-line parts) | slice name | as authored |
| By-reference part inside a definition (`Extension.extension:x` with `type.profile`) | unstated (R6 732 does not distinguish in-line from by-reference) | 0 cases in pack | (not isolated in census) | type-profile url | inherited from referenced extension's snapshot if fixed there |
| Profile extension slice (`Patient.extension:x.url`) | instance identity: absolute canonical (R5 581) | n/a | 56 absolute; 8 left unfixed (`ca-patient` etc.) | type-profile url else slice name | inherited if the extension's snapshot fixes it, else none |
| Slice added by a derived profile on a complex extension | R6 734: SHALL be a full URL — derived profiles don't establish the url | pack has no derived-extension profiles | none observed | type-profile url else slice name | as authored |
| Derived extension profile keeps base url? | R6 734: yes, implicitly (not establishing a url) | no derived-extension profiles in pack | `ext-sort-issue`: yes | yes (never overwrites) | yes |

## (c) Drop-in RFC-015 body

### RFC-015 — `Extension.url` fixed-value convention in snapshots
defining-extensions §2.1.5.1.4 lists, as authoring *guidance*, that an extension definition's
`Extension.url` has "value = canonical URL (fixed)"; extensibility §2.1.5.0.1 states the url rule for
*instances* (absolute canonical, except in-line parts of complex extensions, which are relative). Neither
says whether the fixed value lives in the differential or may be filled into the snapshot by a generator,
which property carries it (`fixedUri` — Zulip-settled, no spec text — vs `pattern[x]`), or what value a
nested part's `Extension.extension:x.url` fixes. Two live behaviors: .NET (`fixExtensionUrl`) backfills
a missing `fixedUri` — canonical on a definition root, type-profile url or else slice name on an
extension slice; Java never writes one and only inherits an authored/base value, and its golden files
bless unfixed definition roots (`ext-recursion-2`, `au2`) and unfixed profile slices (`ca-patient`,
`telus-oo`, …) ([DEV-037](13-deviation-register.md#dev-037--extensionurl-fixeduri-net-synthesizes-java-inherits-only-ch7)).
De-facto convention: hl7.fhir.uv.extensions 5.2.0 fixes `Extension.url` to the canonical in every
differential and snapshot (632/632) and every nested in-line part to its local name (272/272 relative,
zero `patternUri`) — so the fixed value is universally *expected*, but being authored it does not
discriminate the two engines. The R6 build settles one sub-question: a derived profile on a complex
extension "is not establishing the 'url' value" (extensibility Notes, new bullet), so a derived
extension profile inheriting its base's `fixedUri` (`ext-sort-issue`) is correct for both engines.
**Proposal** (defining-extensions §2.1.5.1.4, promote guidance to conformance language): "An extension
definition SHALL fix `Extension.url` (as `fixedUri`) to its canonical URL; an in-line nested extension of
a complex extension SHALL fix its `url` to its local name (normally the slice name); a nested extension
defined by reference SHALL fix its `url` to the referenced extension's canonical URL. A snapshot generator
MAY supply these fixed values when the differential omits them and SHALL NOT alter an inherited one."
Option A (.NET) makes the MAY a SHALL; option B (Java) drops the MAY and leaves authoring tools
responsible. Either way the snapshot contract becomes stated, and validation of instances against a
snapshot no longer depends on which generator produced it.
**Status:** draft — verified against R6 build 6.0.0-ballot4 (page build 2026-08-18, checked 2026-09-02);
research extract `rfc-015-extension-url-fixed-2026-09-02.md` (materials dir) lists the census numbers
and three register fixups (ch07/DEV-037 spec-basis, `ext-sort-issue` verdict, R6 line-732 erratum).

### Register fixups implied by this extract (SDK repo untouched; apply in the next docs session)
1. ch07 lines 55-57 and spec-gap 3; DEV-037 "Spec basis: none" → cite defining-extensions §2.1.5.1.4
   ("value = canonical URL (fixed)") as a partial, non-normative basis.
2. DEV-037 "arguably wrong for both" on `ext-sort-issue` → retract: R6-build extensibility Notes (line
   734) says derived profiles do not establish the url, so inheriting the base's `fixedUri` is correct.
3. DEV-037 test evidence: add `au2` (second golden with an unfixed definition root) and the eight
   unfixed profile-slice urls (`ca-patient` etc.) as the concrete files where the engines differ.
4. RFC-014 errata list: R6-build `extensibility.html` line 732, stray `'` after
   `.../StructureDefinition/Extension` inside `<code>`.

## (d) WGM question (3 lines)

1. Decision: should a conforming snapshot of an extension definition carry `Extension.url.fixedUri`
   (canonical at the root; slice name for in-line parts; referenced canonical for by-reference parts)
   regardless of whether the author stated it — i.e. is the fixed url part of the snapshot *contract*?
2. Option A (.NET): the generator SHALL synthesize a missing fixed url. Option B (Java): only authored
   or inherited fixed urls appear; the definition-level guidance in §2.1.5.1.4 becomes a SHALL on authors.
3. The core extension pack (632/632 authored `fixedUri`, all differentials) shows the fixed url is
   universally *expected* to be present, favouring "it belongs in the snapshot" — but because it is
   authored everywhere, the pack is consistent with either A or B; only the Java goldens
   (`ext-recursion-2`, `au2`) and ca-patient-style unfixed profile slices force the choice.
