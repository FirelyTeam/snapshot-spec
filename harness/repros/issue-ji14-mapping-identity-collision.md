Snapshot generation: MappingAssistant mishandles a mapping-identity collision between base and derived profile (rename applied to the wrong side, and to `name` instead of `identity`)

**Setup:** a base profile declares `StructureDefinition.mapping` with `identity = m`, `uri = http://example.org/maps/A`, and uses it on `Patient.active`. A derived profile re-declares `identity = m` with a different `uri = http://example.org/maps/B` (different `name` too) and uses `m` (meaning B) on `Patient.gender`.

`MappingAssistant` (master `f725fb7`, same lines at b06c7ee) detects the collision and decides to rename the base's `m` to `m1` (`nameExists` → `renames.put("m", "m1")`, MA:69-76). Two things then go wrong:

1. **The rename lands in `name`, not `identity`** (MA:76): `masterList.add(m.copy().setName(n));` — so the derived SD ends up with **two** `mapping` entries whose `identity` is `m` (one named "m1"), and no declaration with `identity = m1` exists at all.

2. **The rename table is applied to the differential's element mappings instead of the inherited ones.** `updateFromDefinition` calls `mappings.merge(derived, base); // note reversal of names to be correct in .merge()` (PU:3084), and `merge(ElementDefinition base, ElementDefinition derived)` does `addMappings(list, base.getMapping(), renames)` … `addMappings(list, derived.getMapping(), null)` (MA:173-177). With the caller's reversal, `renames` (keyed on *base-SD* identities) is applied to the **differential's** mappings — which carry the derived SD's identities — while the inherited element mappings, the ones that actually use the base identity, are copied unrenamed.

**Observed output** (validator_cli 6.10.2, JUnit-driver config, R5 core):

```xml
<!-- StructureDefinition.mapping of the derived snapshot -->
<mapping><identity value="m"/><uri value="http://example.org/maps/B"/><name value="Map B (declared in the derived profile)"/></mapping>
<mapping><identity value="m"/><uri value="http://example.org/maps/A"/><name value="m1"/></mapping>
…
<!-- Patient.active (inherited from the base profile) -->
<mapping><identity value="m"/><map value="A.active (base mapping, identity m = Map A)"/></mapping>   <!-- still m: now resolves to Map B -->
<!-- Patient.gender (the derived profile's own row) -->
<mapping><identity value="m1"/><map value="B.gender (derived mapping, identity m = Map B)"/></mapping> <!-- m1: declared nowhere -->
```

Expected: `Patient.active` → `m1` (with a declaration `identity = m1`, uri A), `Patient.gender` → `m` (uri B).

**Repro inputs** (two small Patient profiles, 1 differential row each):

```xml
<!-- base -->
<StructureDefinition xmlns="http://hl7.org/fhir">
  <url value="http://example.org/fhir/StructureDefinition/ji14-base"/><name value="JI14Base"/><status value="draft"/><fhirVersion value="5.0.0"/>
  <mapping><identity value="m"/><uri value="http://example.org/maps/A"/><name value="Map A (declared in the base profile)"/></mapping>
  <kind value="resource"/><abstract value="false"/><type value="Patient"/>
  <baseDefinition value="http://hl7.org/fhir/StructureDefinition/Patient"/><derivation value="constraint"/>
  <differential><element id="Patient.active"><path value="Patient.active"/>
    <mapping><identity value="m"/><map value="A.active (base mapping, identity m = Map A)"/></mapping></element></differential>
</StructureDefinition>
<!-- derived -->
<StructureDefinition xmlns="http://hl7.org/fhir">
  <url value="http://example.org/fhir/StructureDefinition/ji14-derived"/><name value="JI14Derived"/><status value="draft"/><fhirVersion value="5.0.0"/>
  <mapping><identity value="m"/><uri value="http://example.org/maps/B"/><name value="Map B (declared in the derived profile)"/></mapping>
  <kind value="resource"/><abstract value="false"/><type value="Patient"/>
  <baseDefinition value="http://example.org/fhir/StructureDefinition/ji14-base"/><derivation value="constraint"/>
  <differential><element id="Patient.gender"><path value="Patient.gender"/>
    <mapping><identity value="m"/><map value="B.gender (derived mapping, identity m = Map B)"/></mapping></element></differential>
</StructureDefinition>
```

**Suggested fix:** `setIdentity(n)` (and keep the original as `name` if wanted) at MA:76; and apply `renames` to the *inherited* element mappings (the `derived` parameter after the caller's reversal, or drop the reversal and rename `base.getMapping()` as the parameter names suggest).

Found while reverse-engineering snapshot generation for a spec write-up (Firely).
