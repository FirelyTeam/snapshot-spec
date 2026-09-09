# SnapShotGenerationTests.java — test-driver analysis (orientation pass)
Source: org.hl7.fhir.core @ commit b06c7ee (sparse clone 2026-08-21)
File: org.hl7.fhir.r5/src/test/java/org/hl7/fhir/r5/test/SnapShotGenerationTests.java (707 lines)
Generated: 2026-08-26 (Phase 3 packet 1). Read inline by main session, not a subagent.

## Fixture source & version pin (VERIFIED)

- Fixtures load via `TestingUtilities.loadTestResource("r5", "snapshot-generation", ...)` (:207-232, :494)
  → the **fhir-test-cases Maven artifact**, NOT in-repo resources. Confirms the 2026-08-21 finding that
  `org.hl7.fhir.r5/src/test/resources/snapshot-generation` is absent by design.
- Maven pin: `org.hl7.fhir.testcases:fhir-test-cases:${validator_test_case_version}` (r5/pom.xml:164) with
  `validator_test_case_version = 1.7.67` (root pom.xml:28).
- Our local `C:\Git\snapshot-spec-materials\fhir-test-cases` clone is at `9f495e8` ("Updating version to:
  1.7.68-SNAPSHOT", 2026-08-12) — i.e. **exactly the 1.7.67 release content** plus the version-bump commit.
  **No test drift between our clone and what Java CI runs.** (Phase 4 can diff against the clone directly.)

## Oracle assertion model (feeds Phase 4 harness design + ch13 methodology)

Java's oracle is **golden-file comparison**, with FHIRPath rules as a secondary layer — the reverse of
.NET's vendored driver (FHIRPath rules only, no golden files anywhere):

1. **Golden file**: `expected.equalsDeep(output)` after `setText(null)` on both sides (:659-675). Narrative
   is excluded; everything else (ids, Base components, extensions, order) participates in deep equality.
2. **FHIRPath rules** from the manifest `<rule fhirpath=...>` elements are evaluated *in addition* (:537-541).
   Quirk: they are evaluated against a `new StructureDefinition()` (empty!, `sdn`) — the expressions reach
   real data only through the custom `fixture(id)` host function (:394-420), which resolves `<id>`,
   `<id>-output`, `<id>-include` to the test's source/generated-output/included SDs (:336-374), plus magic
   ids `patient|valueset|organization|operationoutcome|parameters` → core SDs.
3. **Message gate**: any ValidationMessage of severity ERROR collected during generation → FHIRException,
   even when the snapshot matched the golden file (:627-632, :677-679).
4. **fail tests**: expected-to-throw; optional `regex` attribute must match the exception message (:515-532).
   Note the generated-but-shouldn't-have sentinel message "Should have failed".

## Manifest `<test>` attributes (:125-152)

| attribute | meaning | default |
|---|---|---|
| `id` | fixture basename (`<id>-input.xml|json`, `<id>-expected.xml|json`) | required |
| `gen` | run generateSnapshot (vs sort-only test) | false |
| `sort` | run `sortDifferential` before generation | false |
| `fail` | expect exception | false |
| `regex` | required pattern for the failure message | none |
| `new-slice-processing` | → `setNewSlicingProcessing()` | **true** (absent ≠ false; only literal "false" disables) |
| `debug` | → `setDebug()` | false |
| `include` | single SD fixture loaded as XML into `included` | none |
| `register` | comma-separated SD fixtures: cached into context; **first one gets its own snapshot generated first** (:563-590), errors → fail | none |
| `allow` | → `setAllowUnknownProfile`: absent/other→ALL_TYPES, "no-extensions"→NON_EXTNEIONS (sic, typo in enum), "none"→NONE | ALL_TYPES |

## ProfileUtilities configuration set by the driver (testGen, :596-606)

```java
pu = new ProfileUtilities(testContext, messages, new TestPKP());
pu.setNewSlicingProcessing(test.isNewSliceProcessing());  // manifest-driven, default TRUE
pu.setThrowException(false);                              // errors become messages, not throws
pu.setDebug(test.isDebug());
pu.setIds(test.getSource(), false);                       // pre-assigns element ids on the DIFFERENTIAL
pu.setAllowUnknownProfile(test.allow);
pu.setXver(XVerExtensionManagerFactory.createExtensionManager(testContext)); // xver package loaded
```
- Packages loaded into context: core R5 (shared worker context), `hl7.fhir.uv.sdc` (:487-489),
  `hl7.fhir.xver` common package (:602-605).
- **NOT set: `setAutoFixSliceNames`** — contrast with validator_cli, which sets it `true`
  (ValidationEngine.java:1010). The JUnit driver and the CLI oracle run DIFFERENT configs; Phase 4 must
  account for this when using validator_cli as the oracle (check ProfileUtilities default value — see
  ProfileUtilities map).
- TestPKP (:244-325) is a ProfileKnowledgeProvider stub — binding/link resolution returns dummies; only
  isDatatype/isPrimitiveType/isResource answer real questions from the context. PKP appears to be
  rendering-oriented; verify in deep-read whether any generation logic consults it.

## Flow details worth remembering

- **Preprocessor dump** (:614-620): the driver explicitly runs
  `new SnapshotGenerationPreProcessor(pu).process(sdc.getDifferential(), sdc)` on a *copy* and writes the
  preprocessed differential to `[tmp]/snapshot/input/` — debugging aid only; the real `generateSnapshot`
  call at :626 runs on an unpreprocessed copy (preprocessor presumably invoked inside generateSnapshot —
  verify in ProfileUtilities map).
- **getSD base preparation** (:683-706): any base in the chain lacking a snapshot gets
  `sortDifferential` (**always**, with newSlicingProcessing=true) + `setIds` + `generateSnapshot`,
  recursively. So Java test bases are always *sorted* before generation — .NET never sorts. Order
  differences between the implementations may be masked/created by this.
- **testSort** (:544-556): sort-only tests compare `sortDifferential` output deep-equals against expected.
- generateSnapshot signature used: `pu.generateSnapshot(base, derived, url, "http://test.org/profile", name)`
  — 4th arg is the web/base path for narrative links.
- After generation the driver renders the SD narrative (:637-642) — render crashes fail tests too.
- Generated output is cached into the context (:648) so later tests can derive from earlier outputs
  (test chaining via `fixture()` / getByUrl :442-454 which searches *expected* and *included* of all tests).

## Chapter mapping

- ch02 (preprocessing): sortDifferential usage, SnapshotGenerationPreProcessor invocation point.
- ch10 (ids): `setIds(sd, false)` pre-assigns ids on differential before generation — Java ids may
  originate *outside* generateSnapshot; deep-read must check how setIds and generation interact.
- ch12 (errors/settings): setThrowException(false), AllowUnknownProfile enum, message-severity gate.
- ch13/Phase 4: oracle = equalsDeep minus narrative; driver-vs-CLI config drift (autoFixSliceNames);
  base-chain pre-sorting.
