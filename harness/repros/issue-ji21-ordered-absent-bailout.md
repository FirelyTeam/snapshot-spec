SnapshotGenerationPreProcessor: an extension slicing entry without an explicit `ordered=false` is treated as unsupported nested slicing, and the whole profile's slice pre-processing is abandoned with only a warning

**Where:** `SnapshotGenerationPreProcessor.isExtensionSlicing`, master `f725fb7` line 1077 (same at b06c7ee):

```java
private boolean isExtensionSlicing(ElementDefinition ed) {
  if (!Utilities.existsInList(ed.getName(), "extension", "modiferExtension")) {
    return false;
  }
  if (ed.getSlicing().getRules() != SlicingRules.OPEN || (!ed.getSlicing().hasOrdered() || ed.getSlicing().getOrdered()) || ed.getSlicing().getDiscriminator().size() != 1) {
    return false;
  }
  ...
}
```

`(!hasOrdered() || getOrdered())` makes the exemption require an **explicit** `ordered = false`. `ElementDefinition.slicing.ordered` is 0..1 and its absence means "not ordered" in the spec, so a perfectly ordinary hand-written extension slicing entry

```xml
<element id="Patient.identifier.extension">
  <path value="Patient.identifier.extension"/>
  <slicing>
    <discriminator><type value="value"/><path value="url"/></discriminator>
    <rules value="open"/>
  </slicing>
</element>
```

placed between an outer slicing entry and its first named slice is *not* recognised as an extension slicing. `processSlices` (:695-720) then hits the "unsupported nested slicing" check and `return`s — abandoning slice pre-processing for the **entire** profile (cf. #2589) — with a `log.warn` only:

```
Unsupported feature in slicing at Patient.identifier: the slicing element at path Patient.identifier.extension has slicing open, by value=url, and this nested slicing is not supported
```

**Observed:** three otherwise identical profiles (validator_cli 6.10.2, JUnit-driver config): (a) with the entry above → warning, nothing from the slicer's sub-tree propagates into the named slice; (b) without any explicit extension slicing entry → pre-processing runs; (c) with the same entry plus `<ordered value="false"/>` → pre-processing runs, with the same propagation into the named slice as (b). Tools that always emit `ordered: false` (SUSHI) never see this; hand-authored or other-tool profiles do, and get a silently different snapshot.

(Aside, already filed as #2588: the same list contains the typo `"modiferExtension"`.)

**Suggested fix:** treat absent `ordered` as `false` in `isExtensionSlicing`; consider raising the bail-out to an ERROR-level `ValidationMessage` so the author learns that slice pre-processing was skipped.

Found while reverse-engineering snapshot generation for a spec write-up (Firely); the three fixtures are available on request.
