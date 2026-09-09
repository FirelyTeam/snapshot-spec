Minimized standalone repro for this issue (validator_cli 6.10.2, Git# d06577dbc5c6, JUnit-driver configuration, R5 core + hl7.fhir.uv.extensions; no other packages). One Patient profile, seven differential rows:

```xml
<StructureDefinition xmlns="http://hl7.org/fhir">
  <url value="http://example.org/fhir/StructureDefinition/dev033b"/><name value="CrossSliceContamination"/>
  <status value="draft"/><fhirVersion value="5.0.0"/><kind value="resource"/><abstract value="false"/>
  <type value="Patient"/><baseDefinition value="http://hl7.org/fhir/StructureDefinition/Patient"/><derivation value="constraint"/>
  <differential>
    <element id="Patient.identifier"><path value="Patient.identifier"/>
      <slicing><discriminator><type value="value"/><path value="system"/></discriminator><rules value="open"/></slicing></element>
    <!-- "sliceStuff": rows between the slicing entry and the first named slice -->
    <element id="Patient.identifier.extension:e1"><path value="Patient.identifier.extension"/><sliceName value="e1"/>
      <type><code value="Extension"/><profile value="http://hl7.org/fhir/StructureDefinition/data-absent-reason"/></type></element>
    <element id="Patient.identifier.extension:e1.value[x]"><path value="Patient.identifier.extension.value[x]"/>
      <mustSupport value="true"/></element>
    <!-- named slice with a DIFFERENT extension slice -->
    <element id="Patient.identifier:S1"><path value="Patient.identifier"/><sliceName value="S1"/>
      <patternIdentifier><system value="http://example.org/ids/S1"/></patternIdentifier></element>
    <element id="Patient.identifier:S1.extension:e2"><path value="Patient.identifier.extension"/><sliceName value="e2"/>
      <type><code value="Extension"/><profile value="http://hl7.org/fhir/StructureDefinition/rendered-value"/></type></element>
    <element id="Patient.identifier:S1.extension:e2.value[x]"><path value="Patient.identifier.extension.value[x]"/>
      <short value="E2 value (authored short; no mustSupport authored here)"/></element>
  </differential>
</StructureDefinition>
```

Generated snapshot:

- `Patient.identifier:S1.extension:e2.value[x]` carries **`mustSupport = true`** — authored only on `e1`'s `value[x]` (contamination: `elementsMatch`/`pathsMatch` compare `(path modulo [x], sliceName-or-null)` only, so `e1.value[x]` and `e2.value[x]` are indistinguishable inside the slice).
- `Patient.identifier:S1.extension:e1` **is** injected into the slice, but has **no `value[x]` child** — the `mustSupport` the author wrote for e1's value never reaches S1 (the sliceStuff row was marked as handled by the false match, so it is not injected).

Two notes for anyone reproducing: if the extension slicing entry `Patient.identifier.extension` is stated explicitly it must carry `ordered = false`, otherwise `isExtensionSlicing` rejects it and pre-processing bails out for the whole profile with a warning (separate issue); and the same shape is what `on-questionnaire-expected.xml` in fhir-test-cases exhibits at `item:group.extension:itemControl.value[x]`.
