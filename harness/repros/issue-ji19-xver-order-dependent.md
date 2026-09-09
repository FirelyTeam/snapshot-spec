Snapshot generation: xver template selection is order-dependent — `ProfilePathProcessor` tests the raw `getXver()` field, which is null for every `ProfileUtilities` the validator constructs

**Where:** `ProfilePathProcessor` master `f725fb7` line 700 (same at b06c7ee):

```java
if (firstTypeStructureDefinition == null && profileUtilities.getXver() != null && profileUtilities.getXver().matchingUrl(firstTypeProfile.getValue())) {
```

`getXver()` (PU:4836) returns the raw field. Every other xver consumer in `ProfileUtilities` goes through the lazy `makeXVer()` (PU:1184; used at PU:1045, 2078, 2628 @ b06c7ee), which creates the `XVerExtensionManager` on first use and stores it in that same field.

**Consequence:** for a `ProfileUtilities` on which nobody called `setXver(...)`, the template-selection branch at PPP:700 is skipped for the **first** xver-typed extension slice the instance meets (template = the base row); once any later code path has triggered `makeXVer()`, subsequent xver-typed slices in the same instance do take the xver template. So the shape of the snapshot depends on the order in which elements are processed and on what ran before.

**Who is affected:** in `org.hl7.fhir.validation`, `ValidationEngine` constructs `new ProfileUtilities(context, null, null).setAutoFixSliceNames(true)` at four sites (master lines 931, 1010, 1088, 1178 — the `snapshot` task among them) and never calls `setXver`. In the parts of the repository I checked (`org.hl7.fhir.r5/.../conformance/profile`, the r5 test driver, and `org.hl7.fhir.validation/src/main`), the only `setXver` caller is the JUnit driver `SnapShotGenerationTests` (:606 @ b06c7ee), which is why the `fhir-test-cases` xver tests (`pat-xver-extension`, `es-xver`) do not show the problem. I have not checked the IG Publisher's call sites.

**Suggested fix:** use `makeXVer()` at PPP:700 (or make `getXver()` lazily create the manager, like `makeXVer()`).

Found while reverse-engineering snapshot generation for a spec write-up (Firely). Code-read finding, confirmed by the validator's construction sites; no fixture needed.
