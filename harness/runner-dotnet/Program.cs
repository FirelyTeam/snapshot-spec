// .NET snapshot runner for the snapshot-spec harness.
// Runs the Firely SDK SnapshotGenerator (NuGet Hl7.Fhir.R5, pinned in the csproj) with the
// same settings as the SDK's own manifest tests (SnapshotGeneratorManifestTests):
// ForceRegenerateSnapshots + GenerateSnapshotForExternalProfiles.
//
// Modes:
//   Single: DotNetRunner -core <core.tgz> -input <sd-file> -output <out-file> [-deps f1,f2,...]
//   Batch:  DotNetRunner -core <core.tgz> -batch <jobs.tsv> [-universe <list-file>] [-pkgs t1,t2,...]
//     jobs.tsv lines: id<TAB>input<TAB>output<TAB>deps(comma-separated, may be empty)
//     One process for the whole sweep: core source loaded once. Per job prints
//     "RESULT\t<id>\t<rc>" (0 ok, 2 threw); MSG:/EXCEPTION: lines go to <output-dir>/dotnet.log.
//
// -universe: file listing fixture paths (all tests' -expected files + include/register fixtures).
//   Mirrors the Java JUnit driver's getByUrl, which resolves cross-linked test bases from other
//   tests' EXPECTED outputs (and included fixtures) before consulting the context. Universe SDs
//   are parsed once and SHARED across the whole batch (mutations persist) — deliberate: both
//   reference drivers share one context/fixture set for the entire run.
//   Resolution precedence per test: explicit deps -> universe -> core (+ -pkgs after core).
//
// -pkgs: FHIR package tgz files (THO, uv.extensions, sdc, xver) appended AFTER core, for
//   package parity with the Java oracle's context. Off unless passed.
//
// Exit codes (single mode): 0 = snapshot generated; 2 = generation threw.
// Canonical lookups strip a |version suffix (fixtures here are unversioned canonicals).

using Hl7.Fhir.Model;
using Hl7.Fhir.Serialization;
using Hl7.Fhir.Specification.Snapshot;
using Hl7.Fhir.Specification.Source;

var core = "";
var input = "";
var output = "";
string? deps = null;
string? batch = null;
string? universe = null;
string? pkgs = null;
var defaults = false; // -defaults: SDK default SnapshotGeneratorSettings instead of the manifest-test settings (DEV-029 re-run)
for (var i = 0; i < args.Length; i++)
{
    switch (args[i])
    {
        case "-core": core = args[++i]; break;
        case "-input": input = args[++i]; break;
        case "-output": output = args[++i]; break;
        case "-deps": deps = args[++i]; break;
        case "-batch": batch = args[++i]; break;
        case "-universe": universe = args[++i]; break;
        case "-pkgs": pkgs = args[++i]; break;
        case "-defaults": defaults = true; break;
        default: throw new ArgumentException($"Unknown arg: {args[i]}");
    }
}

var xmlParser = new FhirXmlParser(new ParserSettings { PermissiveParsing = true });
var jsonParser = new FhirJsonParser(new ParserSettings { PermissiveParsing = true });

Resource Parse(string file)
{
    var text = File.ReadAllText(file);
    return file.EndsWith(".json") ? jsonParser.Parse<Resource>(text) : xmlParser.Parse<Resource>(text);
}

// Core defs from specification.zip (Hl7.Fhir.Specification.Data.R5, copied to output);
// -core is accepted for CLI symmetry with JavaRunner but unused here.
_ = core;
var coreSource = ZipSource.CreateValidationSource();

// Shared tail of the resolver chain: universe -> core [-> packages], cached.
var tail = new List<ISyncOrAsyncResourceResolver>();
if (universe is not null)
{
    var map = new Dictionary<string, StructureDefinition>();
    foreach (var path in File.ReadAllLines(universe))
    {
        if (path.Trim().Length == 0) continue;
        if (Parse(path.Trim()) is StructureDefinition sd && sd.Url is not null)
            map.TryAdd(sd.Url, sd); // first wins, like getByUrl's linear scan
    }
    Console.Error.WriteLine($"universe: {map.Count} SDs");
    tail.Add(new InMemoryResolver(map));
}
tail.Add(coreSource);
if (pkgs is not null)
    tail.Add(new Firely.Fhir.Packages.FhirPackageSource(ModelInfo.ModelInspector, pkgs.Split(',')));
var shared = new CachedResolver(new MultiResolver(tail.ToArray()));

// Default mode mirrors SnapshotGeneratorManifestTests (ForceRegenerateSnapshots + GenerateSnapshotForExternalProfiles).
// -defaults uses the SDK defaults (6.2.1: ForceRegenerateSnapshots=false, GenerateSnapshotForExternalProfiles=true,
// GenerateElementIds=true, RespectSuppressExtension=true) — what a plain `new SnapshotGenerator(resolver)` caller gets.
var settings = defaults
    ? SnapshotGeneratorSettings.CreateDefault()
    : new SnapshotGeneratorSettings
    {
        ForceRegenerateSnapshots = true,
        GenerateSnapshotForExternalProfiles = true
    };
Console.Error.WriteLine($"settings: ForceRegenerateSnapshots={settings.ForceRegenerateSnapshots} GenerateSnapshotForExternalProfiles={settings.GenerateSnapshotForExternalProfiles}");

async Task<int> RunOne(string inputFile, string outputFile, string? depList, TextWriter log)
{
    var resolvers = new List<ISyncOrAsyncResourceResolver>();
    if (depList is { Length: > 0 })
    {
        var depMap = new Dictionary<string, StructureDefinition>();
        foreach (var d in depList.Split(','))
        {
            var sd = (StructureDefinition)Parse(d);
            depMap[sd.Url] = sd;
        }
        resolvers.Add(new InMemoryResolver(depMap));
    }
    resolvers.Add(shared);
    var snapGen = new SnapshotGenerator(new MultiResolver(resolvers.ToArray()), settings);

    var source = (StructureDefinition)Parse(inputFile);
    var rc = 0;
    try
    {
        await snapGen.UpdateAsync(source);
    }
    catch (Exception e)
    {
        rc = 2;
        log.WriteLine($"EXCEPTION: {e.GetType().Name}: {e.Message}");
    }
    if (snapGen.Outcome is not null)
        foreach (var issue in snapGen.Outcome.Issue)
            log.WriteLine($"MSG: [{issue.Severity}] {string.Join('|', issue.Expression)}: {issue.Details?.Text ?? issue.Details?.Coding?.FirstOrDefault()?.Display}");
    if (rc != 0) return rc;

    source.Text = null; // narrative excluded from comparison, as in the Java golden diff
    if (outputFile.EndsWith(".json"))
        await File.WriteAllTextAsync(outputFile, await new FhirJsonSerializer().SerializeToStringAsync(source));
    else
        await File.WriteAllTextAsync(outputFile, await new FhirXmlSerializer().SerializeToStringAsync(source));
    return 0;
}

if (batch is not null)
{
    foreach (var line in File.ReadAllLines(batch))
    {
        if (line.Trim().Length == 0) continue;
        var f = line.Split('\t');
        var (id, inp, outp, depList) = (f[0], f[1], f[2], f.Length > 3 ? f[3] : "");
        var logPath = Path.Combine(Path.GetDirectoryName(Path.GetFullPath(outp))!, "dotnet.log");
        await using var log = new StreamWriter(logPath, append: false);
        int rc;
        try
        {
            rc = await RunOne(inp, outp, depList, log);
        }
        catch (Exception e) // parse/setup failures, not generation throws
        {
            log.WriteLine($"EXCEPTION: {e.GetType().Name}: {e.Message}");
            rc = 3;
        }
        Console.WriteLine($"RESULT\t{id}\t{rc}");
    }
    Console.WriteLine("DONE");
    return 0;
}

if (input.Length == 0 || output.Length == 0)
    throw new ArgumentException("-input and -output are required (or -batch)");
return await RunOne(input, output, deps, Console.Error);

// Minimal in-memory resolver over a url→SD map; lookups strip a |version suffix.
sealed class InMemoryResolver(Dictionary<string, StructureDefinition> map) : IResourceResolver, IAsyncResourceResolver
{
    public Resource? ResolveByCanonicalUri(string uri)
    {
        var bar = uri.IndexOf('|');
        return map.TryGetValue(bar < 0 ? uri : uri[..bar], out var sd) ? sd : null;
    }
    public Resource? ResolveByUri(string uri) => ResolveByCanonicalUri(uri);
    public System.Threading.Tasks.Task<Resource?> ResolveByCanonicalUriAsync(string uri) =>
        System.Threading.Tasks.Task.FromResult(ResolveByCanonicalUri(uri));
    public System.Threading.Tasks.Task<Resource?> ResolveByUriAsync(string uri) =>
        System.Threading.Tasks.Task.FromResult(ResolveByUri(uri));
}
