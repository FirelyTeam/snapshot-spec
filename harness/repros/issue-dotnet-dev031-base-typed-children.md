SnapshotGenerator: inline children of a new element typed `Base` are emitted one level up (segment dropped from path and id)

**Observed (Hl7.Fhir.R5 6.2.1, `SnapshotGenerator` with `ForceRegenerateSnapshots=true, GenerateSnapshotForExternalProfiles=true`; also with default settings):**

Given a specialization (logical model or resource) whose differential introduces a new element `X.a` of type `Base` with inline children `X.a.b` and `X.a.c`, the generated snapshot contains `X.b` and `X.c` as children of `X` instead of `X.a.b` / `X.a.c`. Their `base.path` is correct (`X.a.b` / `X.a.c`), so the elements are created with the right path and re-parented afterwards when they are inserted into the snapshot (the navigator rewrites `path` from its current position).

The same differential with `a` typed `Element` or `BackboneElement` produces the expected `X.a.b` / `X.a.c`. The behaviour does not depend on `kind` (logical or resource) or on whether `a` is the last element. The condition seems to be a type whose snapshot has no children (`Base`), so `expandElementType`/`copyChildren` copies nothing and the following `merge(snap, diff)` for `a`'s children ends up positioned on the grandparent.

Real-world exhibit: `FHIR/fhir-test-cases` `r5/snapshot-generation/cdshooks-services` — `CDSHooksServices.services.prefetch` (type `Base`) with `prefetch.key` / `prefetch.value`; .NET emits `services.key` / `services.value` (Java and the golden file: `services.prefetch.key` / `.value`).

**Repro** (logical model, 4 differential rows):

```xml
<StructureDefinition xmlns="http://hl7.org/fhir">
  <url value="http://example.org/fhir/StructureDefinition/Dev031Base"/><name value="Dev031Base"/><status value="draft"/>
  <fhirVersion value="5.0.0"/><kind value="logical"/><abstract value="false"/>
  <type value="http://example.org/fhir/StructureDefinition/Dev031Base"/>
  <baseDefinition value="http://hl7.org/fhir/StructureDefinition/Element"/><derivation value="specialization"/>
  <differential>
    <element id="Dev031Base"><path value="Dev031Base"/><short value="probe root"/></element>
    <element id="Dev031Base.a"><path value="Dev031Base.a"/><min value="0"/><max value="*"/><type><code value="Base"/></type></element>
    <element id="Dev031Base.a.b"><path value="Dev031Base.a.b"/><min value="1"/><max value="1"/><type><code value="code"/></type></element>
    <element id="Dev031Base.a.c"><path value="Dev031Base.a.c"/><min value="0"/><max value="1"/><type><code value="string"/></type></element>
  </differential>
</StructureDefinition>
```

Snapshot element ids produced: `Dev031Base`, `Dev031Base.id`, `Dev031Base.extension`, `Dev031Base.a`, **`Dev031Base.b`**, **`Dev031Base.c`** (with `base.path` `Dev031Base.a.b` / `Dev031Base.a.c`).

**Where to look:** `SnapshotGenerator.createNewElement` → `mergeElement` → `expandElement`/`expandElementType` (type snapshot without children) → `merge(snap, diff)`; the children are appended via `ElementNavigatorModificationExtensions.AppendChild` → `InsertFirstChild`/`InsertAfter`, which derive the new `path` from the navigator's current element — at that moment the navigator is on the grandparent. `ElementMatcher.constructNew` has an explicit "base element has no children → snapNav stays on the parent" branch that is the likely interaction point.

Found during the snapshot-generation reverse-engineering study (register entry DEV-031).
