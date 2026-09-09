Snapshot generation: NullPointerException when a differential row touches an element that carries an obligation in an inherited obligation profile

**Where:** `ProfileUtilities.updateFromDefinition`, master `f725fb7` line 2613 (b06c7ee: 2611):

```java
for (ElementDefinition ed : obligationProfileElements) {
  for (Extension ext : ed.getExtension()) {
    if (Utilities.existsInList(ext.getUrl(), ExtensionDefinitions.EXT_OBLIGATION_CORE, ExtensionDefinitions.EXT_OBLIGATION_TOOLS)) {
      dest.getExtension().add(new Extension(ExtensionDefinitions.EXT_OBLIGATION_CORE, ext.getValue().copy()));
    }
  }
}
```

`obligation` is a complex extension (nested `code` / `actor` / … extensions, no `value[x]`), so `ext.getValue()` is always `null` and `.copy()` throws.

**When it fires:** the derived profile declares `inherit-obligations`, one of the obligation profiles puts an `obligation` extension on element E, **and** the derived profile's differential has a row for E (any row — the repro changes only `short`). Elements the differential does not touch go through the copy-through path (`updateFromObligationProfiles`, PU:2529-2582 @ b06c7ee), which handles the extensions correctly — that is why `fhir-test-cases` `profile-patient-op3` passes: its differential touches `active` and `maritalStatus`, the obligations sit on `birthDate` / `deceased[x]`.

**Repro** (validator_cli 6.10.2, Git# d06577dbc5c6; same config as `SnapShotGenerationTests`, `newSlicingProcessing=true`, `setThrowException(false)`): take `r5/snapshot-generation/profile-patient-op3-input.xml`, add one differential row

```xml
<element id="Patient.birthDate">
  <path value="Patient.birthDate"/>
  <short value="Date of birth (short text changed by the derived profile only)"/>
</element>
```

and generate with `profile-patient-op-base-input`, `profile-patient-op1`, `profile-patient-op2`, `profile-patient-op2a` registered (op2/op2a carry `http://hl7.org/fhir/tools/StructureDefinition/obligation` on `Patient.birthDate`). Result:

```
java.lang.NullPointerException: Cannot invoke "org.hl7.fhir.r5.model.DataType.copy()" because the return value of "org.hl7.fhir.r5.model.Extension.getValue()" is null
	at org.hl7.fhir.r5.conformance.profile.ProfileUtilities.updateFromDefinition(ProfileUtilities.java:2611)
	at org.hl7.fhir.r5.conformance.profile.ProfilePathProcessor.processSimplePathWithOneMatchingElementInDifferential(ProfilePathProcessor.java:813)
	at org.hl7.fhir.r5.conformance.profile.ProfilePathProcessor.processSimplePath(ProfilePathProcessor.java:297)
	at org.hl7.fhir.r5.conformance.profile.ProfilePathProcessor.processPaths(ProfilePathProcessor.java:209)
	at org.hl7.fhir.r5.conformance.profile.ProfilePathProcessor.processSimplePathWithEmptyDiffMatches(ProfilePathProcessor.java:1090)
	at org.hl7.fhir.r5.conformance.profile.ProfilePathProcessor.processSimplePath(ProfilePathProcessor.java:294)
	at org.hl7.fhir.r5.conformance.profile.ProfilePathProcessor.processPaths(ProfilePathProcessor.java:209)
	at org.hl7.fhir.r5.conformance.profile.ProfilePathProcessor.processPaths(ProfilePathProcessor.java:181)
	at org.hl7.fhir.r5.conformance.profile.ProfileUtilities.generateSnapshot(ProfileUtilities.java:839)
```

The unmodified `profile-patient-op3-input.xml` generates fine with the same setup (control).

**Suggested fix:** copy the whole extension (`ext.copy()`, re-keyed to `EXT_OBLIGATION_CORE` if the tools url is to be normalised) instead of wrapping `ext.getValue()`. Separate design question worth a decision while there: on the diff-touched path only the obligation *extensions* are re-added — the `mustSupport` / additional-binding fold that `updateFromObligationProfiles` performs for untouched elements is not applied, so once the NPE is fixed a differential row that merely changes `short` on an obligated element will silently drop the obligation's `mustSupport=true`. Intentional ("author took over the element") or should the fold run on both paths?

Found while reverse-engineering snapshot generation for a spec write-up (Firely); repro files available on request.
