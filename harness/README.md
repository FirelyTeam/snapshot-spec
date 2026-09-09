# Snapshot-spec harness — Java-oracle vs .NET three-way diff

Phase 4 of the snapshot-spec project (document set in `../docs/` of this repository; until 2026-09-09 it lived
in firely-net-sdk on branch `docs/snapshot-spec`). Compares, per fhir-test-cases snapshot-generation test:

1. **.NET**: Firely SDK SnapshotGenerator — **NuGet `Hl7.Fhir.R5` 6.2.1** (pinned in
   `runner-dotnet/DotNetRunner.csproj`). Note: the DEV-018 fix (#3583) is NOT in 6.2.1 and
   is expected to reproduce.
2. **Java oracle**: `org.hl7.fhir.r5` engine from **validator_cli 6.10.2 (Git# d06577dbc5c6,
   built 2026-08-13)**, driven in **JUnit-driver configuration** (below). Source citations in
   extracts refer to clone commit `b06c7ee` (2026-08-21, slightly newer than the CLI release).
3. **Golden**: the `-expected` files from fhir-test-cases @ **1.7.67** (= our clone at `9f495e8`);
   this is the exact version pinned by Java CI, so no test drift.

## Components (Phase 4 packet 2: batch architecture)

| File | Role |
|---|---|
| `run_tests.py` | Driver: manifest parsing, universe/jobs staging, both batch runners, summary merge. `--all`, `--pkgs`, `--net-only`, `--java-only`, `--list` |
| `BatchRunner.java` | Java oracle, one JVM for the whole sweep: shared cumulative context in manifest order (the JUnit driver's model), faithful testGen/testSort/register/getSD replication, inline equalsDeep compares (java-vs-golden, net-vs-golden, net-vs-java), normalized JSON for the report. Results in `out/java-results.tsv` (stdout carries logger noise). |
| `runner-dotnet/` | .NET runner (net9.0). `-batch jobs.tsv` = one process for the sweep; `-universe` = cross-linked-test resolver; `-pkgs` = package parity (below) |
| `report.py` | Noise-classified structural report over `out/<id>/*.norm.json` → `out/report.md`; keyed-list alignment (constraint by key, mapping by identity, extension by url, type by code) |
| `Compare.java`, `JavaRunner.java` | Packet-1 single-test tools, kept for debugging one test in isolation |
| `out/<id>/` | Per-test artifacts: `java.xml`, `dotnet.xml`, `*.log` (MSG:/EXCEPTION: lines), `{java,dotnet,expected}.norm.json` |
| `out/summary.tsv` | id, mode, java, jerr, net, nerr, nvj, note — fresh per run |

## Oracle configuration (JUnit-driver parity — the config the golden files encode)

`BatchRunner` mirrors `SnapShotGenerationTests` (source @ b06c7ee):

- One shared worker context for the whole run; every test's generated output is cached into it
  (`testGen` :648), so later tests can derive from earlier outputs.
- `getSD` (:683-706): base resolution checks the **universe** (every test's `-expected` +
  include/register fixtures, = `getByUrl` :442-454) BEFORE the context; bases without snapshots get
  `sortDifferential` (**always**) + `setIds` + `generateSnapshot` (nsp=true), recursively.
- `testGen` (:558-681): `output = source.copy()` **before** `setIds(source)` — the generation copy
  never gets pre-assigned ids; pu = `(context, messages, TestPKP-replica)`, nsp per manifest,
  `setThrowException(false)`, `allow` per manifest, xver; `sortDifferential` when `sort=true`;
  golden compare = `equalsDeep` after `setText(null)` both sides.
- register block (:563-590): pu(context, localMsgs, **null pkp**) + nsp=true + allow, `setIds` on
  all included, cache-if-absent, snapshot generated for `included.get(0)` when its base resolves
  from the **context** (not the universe); ERROR messages ⇒ register failure.
- fail-test semantics: JUnit "pass" = something **THREW** (anywhere: register, base prep, sort,
  generation) **or** generation finished with ERROR-level messages (**ERRORS**; throw at :677).
  `regex` match recorded when thrown. `jerr`/`nerr` columns count ERROR messages on every test —
  JUnit fails a test on any ERROR even when the golden matches.

**Known deliberate omissions** (differences vs the JUnit driver): no narrative rendering
(:637-642 — a render crash fails a JUnit test; we don't render), no FHIRPath `<rule>` evaluation,
2-arg `fetchResource` (no version-resolution rules), TestPKP replicated inline (verbatim copy of
:244-325).

**Deliberately NOT the validator_cli `snapshot` task config**, which is
`new ProfileUtilities(context, null, null).setAutoFixSliceNames(true)` (ValidationEngine.java:1088)
— throw-on-first-error, `newSlicingProcessing=false`, autofix on. That config diverges from the
golden files on exactly the type-slicing territory we care most about (OQ-018/OQ-020).

## Packages (versions + the canLazyLoad pitfall)

Network is NAT64-blocked for the Java engine (it refuses `64:ff9b::` addresses as non-public), so
package tgzs are downloaded once by OS tools into `../tools/pkgs/` and **imported into the
`~/.fhir` package cache** (`FilesystemPackageCacheManager.addPackageToCache`), then loaded through
the cache manager like the JUnit driver does.

**Pitfall (cost us a full sweep, 2026-08-26):** `NpmPackage.fromPackage(stream)` ⇒
`canLazyLoad()=false` ⇒ `SimpleWorkerContext.loadFromPackage` takes the EAGER branch, which feeds
every file through `loader.loadBundle()` — and `TestPackageLoader.loadBundle()` is a stub returning
null, so every resource is **silently dropped** while the return count still reports hundreds
"loaded". Cache-backed packages take the lazy `.index.json` branch (the branch CI uses), which
registers resources by canonical without parsing. Always load packages through the pcm.

| package | version | pin rationale |
|---|---|---|
| hl7.fhir.r5.core | 5.0.0 | fhir-test-cases tgz, same file Java CI uses |
| hl7.terminology.r5 | 7.3.0 | driver loads unpinned latest; latest at harness-build time |
| hl7.fhir.uv.extensions | 5.2.0 | `Constants.EXTENSIONS_WORKING_VERSION` @ b06c7ee |
| hl7.fhir.uv.sdc | 4.0.0 | driver setUp loads unpinned latest (full/null loader; its broken-for-6.10.2 ImplementationGuide resource is never parsed thanks to lazy loading) |
| hl7.fhir.xver-extensions | 0.1.0 | `CommonPackages.ID_XVER/VER_XVER` |

No terminology server — binding-subset checks that need expansion are inert; re-run with tx before
classifying any binding-related diff.

### .NET runner configuration

Mirrors `SnapshotGeneratorManifestTests`: `ForceRegenerateSnapshots = true`,
`GenerateSnapshotForExternalProfiles = true`, permissive parsing. Core definitions from
`specification.zip` (`Hl7.Fhir.Specification.Data.R5` 6.2.1) via `ZipSource.CreateValidationSource()`
— a known **asymmetry** vs the Java side's core tgz; both are published R5 5.0.0 core, but if a diff
smells like a core-definition difference, check this first. With `--pkgs` the same four package tgzs
are added to the resolver chain via `Firely.Fhir.Packages.FhirPackageSource` (verified working:
t11 resolves patient-birthTime), giving package parity with the oracle. Resolution precedence per
test: explicit deps → universe (parsed once, **shared across the batch** — mutations persist, as in
both reference drivers) → core → packages. Canonical lookups strip `|version`. The vendored
FixInput/FixManifest patches are **not** applied — raw upstream fixtures on both sides.

## Comparison model

- `equalsDeep` after stripping `.text` on both sides — the JUnit oracle's own equality.
  `.base` components, element ids, extensions and list order all participate.
- Verdicts per test: `java` (oracle vs golden — harness-config signal; should be EQUAL),
  `net` (vs golden — deviation signal), `nvj` (direct .NET vs Java).
- **`java != EQUAL` rows are harness/engine-version health signals to investigate, never ch13
  material.** Residual candidates: engine drift 6.10.2-release vs golden-producing CI commits,
  package-version drift (THO/SDC unpinned upstream), harness config gaps.
- Fail-tests: THREW / ERRORS / NO-THROW per side instead of output diff (.NET side from exit code
  + MSG [Error] count).

## Noise ledger (expected diffs; each classified class must cite a register entry / source line)

| report class | Cause | Cite |
|---|---|---|
| NOISE-JAVA-BASEVERSION | Java stamps `tools/snapshot-base-version` extension on the derived SD | ProfileUtilities.java:1091-1093 |
| EXT-SLICING-STAMP | .NET emits url-discriminator `slicing` on **every** `.extension`/`.modifierExtension` element; Java/golden only where slices exist | ch06 register candidate |
| SD-MAPPING | Java copies base SD's `mapping` declarations onto the derived SD (MappingAssistant); .NET doesn't (OQ-007) | harness-first-runs #3 |
| ELEM-MAPPING | element `mapping` merge: Java comma-joins same-identity maps (R4-hack PU:949-968); .NET appends repeats (DEV-017) | harness-first-runs #4 |
| REL-LINKS | Java absolutizes spec-relative markdown links (updateURLs PU:2135/2179); .NET leaves relative | harness-first-runs #5 |
| CONSTRAINT-ORDER | same constraint keys, different order | harness-first-runs #6 |
| FALSE-VS-ABSENT | explicit `false` vs property absent (isSummary etc.); breaks equalsDeep | harness-first-runs #7 |
| TYPE-CONSTRAINT | .NET copies datatype-level constraints (`ident-1`, `cpt-2`, … with `source` = the datatype url) onto elements; Java/golden don't | packet-2 finding, ch05 candidate |
| `constrainedByDifferentialExtension` (.NET, if enabled) | Firely-invented fake canonical | firely-net-sdk#3588 |

Unclassified diffs land in the report's NEW queue. First-run findings list:
`../extracts/harness-first-runs-2026-08-26.md`; packet-2 sweep results:
`../extracts/harness-sweep-2026-08-26.md`; packet-3 mining (element-set / fail tests / min+mustSupport):
`../extracts/{element-set,failtests,min-mustsupport}-2026-08-26.md`.

## Sweep-health notes (packet 3, 2026-08-26)

- **t26 (sort mode):** golden/java sort outputs carry **no snapshot**; the .NET runner generated one
  anyway → 52 spurious net-only ELEMENT-SET rows. Fix candidate: .NET runner should skip generation (or
  the compare should skip snapshots) for `mode=sort` tests whose golden has none.
- **xt-logical:** .NET failed to resolve the EHDS logical-model base chain (`EHDSDataSet`) and the runner
  wrote an **empty-snapshot** norm.json → 57 spurious java-only rows. Investigate resolver coverage
  (universe → logical-model url lookup) before counting it as an engine deviation.
- **report.md caps each test's NEW list at 40 entries** — mine norm.json, not report.md, for full
  populations (cost us an undercount: 79 mustSupport diffs, not 61).
- **Settings sensitivity:** sushi3's fail-agreement and the ext-recursion-2/logical-goo .NET throws are
  all conditional on `ForceRegenerateSnapshots`/`GenerateSnapshotForExternalProfiles` — a
  default-settings .NET re-run of those three is queued before the WGM brief (DEV-029).

## Version stamps for every report

`.NET = Hl7.Fhir.R5 6.2.1 | Java engine = 6.10.2 (d06577dbc5c6) | golden = fhir-test-cases 1.7.67 | core = hl7.fhir.r5.core 5.0.0 | THO 7.3.0 | uv.extensions 5.2.0 | sdc 4.0.0 | xver 0.1.0`

## Default-settings mode and repros (2026-09-03)

- `python run_tests.py <ids> --net-only --defaults [--pkgs]` runs the .NET side with the SDK's **default**
  `SnapshotGeneratorSettings` (`DotNetRunner -defaults`: `ForceRegenerateSnapshots=false`,
  `GenerateSnapshotForExternalProfiles=true`) instead of the manifest-test settings. Used for the DEV-029
  re-run: ext-recursion-2 / logical-goo throw identically under defaults; sushi3 still ERRORS (different
  message); ext-recursion-1 still NO-THROW. Harness-settings originals of those four are kept in
  `out-backup-harness-settings-2026-09-03/`.
- `JavaRunner` now prints the full stack trace after `EXCEPTION:`.
- `repros/` holds standalone inputs outside the fhir-test-cases manifest, run with `JavaRunner -deps`:
  `ji18-op3-short-input.xml` (obligation NPE, PU:2611), `ji14-base.xml` + `ji14-derived-input.xml`
  (mapping identity collision), `dev033{,b,c}-input.xml` (preprocessor cross-slice contamination; b = the
  working minimal repro, a/c show the `ordered=false` exemption gotcha). Outputs + logs in `repros/out/`;
  issue texts as filed in `repros/issue-*.md`.
  Note: the JavaRunner context loads the *latest cached* `hl7.fhir.uv.extensions` (5.3.0 was pulled on
  2026-09-03), not the 5.2.0 the BatchRunner pins.
- `repros/dev031-*.xml` (2026-09-03): .NET-side probes for DEV-031 (children of a `Base`-typed new element hoisted
  to the grandparent). Run with `DotNetRunner -core <tgz> -input <file> -output <file>` (single mode; no deps needed).
  Variants: `logical-base` (repro), `logical-bbe` / `elem` (controls, correct), `z` (trailing sibling, still wrong),
  `res` (resource specialization, still wrong). Filed as FirelyTeam/firely-net-sdk#3597 (text: `repros/issue-dotnet-dev031-base-typed-children.md`).
