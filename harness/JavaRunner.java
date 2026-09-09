// Java-oracle snapshot runner for the snapshot-spec harness.
// Runs the org.hl7.fhir.r5 snapshot generator (from validator_cli.jar on the classpath)
// with the SAME configuration as the upstream JUnit driver (SnapShotGenerationTests.testGen),
// which is the config the fhir-test-cases golden files were produced with.
// NOTE: this deliberately differs from validator_cli's own `snapshot` task, which uses
// `new ProfileUtilities(context, null, null).setAutoFixSliceNames(true)` (throw-on-error,
// newSlicingProcessing=false) — see ValidationEngine.java:1088.
//
// Usage:
//   java -cp ../tools/validator_cli.jar JavaRunner.java \
//     -core <path-to-hl7.fhir.r5.core.tgz> -input <sd-file> -output <out-file> \
//     [-deps f1,f2,...] [-sort] [-nsp true|false] [-autofix] [-no-xver] [-pkg id#ver]...
//
// Exit codes: 0 = snapshot generated; 2 = generation threw (message on stderr, line prefixed EXCEPTION:).
// All ValidationMessages are printed to stderr prefixed MSG:.

import org.hl7.fhir.r5.conformance.profile.ProfileUtilities;
import org.hl7.fhir.r5.conformance.profile.ProfileUtilities.AllowUnknownProfile;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.test.utils.TestPackageLoader;
import org.hl7.fhir.r5.utils.xver.XVerExtensionManagerFactory;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.npm.CommonPackages;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.validation.ValidationMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class JavaRunner {

  static SimpleWorkerContext context;
  static List<ValidationMessage> messages = new ArrayList<>();

  public static void main(String[] args) throws Exception {
    String core = null, input = null, output = null, deps = null;
    boolean sort = false, nsp = true, autofix = false, xver = true;
    List<String> pkgs = new ArrayList<>();
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-core": core = args[++i]; break;
        case "-input": input = args[++i]; break;
        case "-output": output = args[++i]; break;
        case "-deps": deps = args[++i]; break;
        case "-sort": sort = true; break;
        case "-nsp": nsp = Boolean.parseBoolean(args[++i]); break;
        case "-autofix": autofix = true; break;
        case "-no-xver": xver = false; break;
        case "-pkg": pkgs.add(args[++i]); break;
        default: throw new IllegalArgumentException("Unknown arg: " + args[i]);
      }
    }
    if (core == null || input == null || output == null)
      throw new IllegalArgumentException("-core, -input and -output are required");

    // Context: mirrors TestingUtilities.getWorkerContext — core from the test-cases tgz,
    // THO + uv.extensions from the local package cache.
    NpmPackage corePkg = NpmPackage.fromPackage(new FileInputStream(core));
    context = new SimpleWorkerContext.SimpleWorkerContextBuilder()
        .withAllowLoadingDuplicates(true).withUserAgent("snapshot-spec-harness")
        .fromPackage(corePkg);
    context.setExpansionParameters(new Parameters());
    FilesystemPackageCacheManager pcm = new FilesystemPackageCacheManager.Builder().build();
    try {
      NpmPackage utg = pcm.loadPackage("hl7.terminology.r5");
      context.loadFromPackage(utg, new TestPackageLoader(Utilities.stringSet("CodeSystem", "ValueSet", "NamingSystem")));
    } catch (Exception e) { System.err.println("WARN: THO load failed: " + e.getMessage()); }
    try {
      NpmPackage ext = pcm.loadPackage("hl7.fhir.uv.extensions"); // 6.10.2's Constants has no EXTENSIONS_WORKING_VERSION; latest cached

      context.loadFromPackage(ext, new TestPackageLoader(Utilities.stringSet("CodeSystem", "ValueSet", "StructureDefinition")));
    } catch (Exception e) { System.err.println("WARN: uv.extensions load failed: " + e.getMessage()); }
    for (String p : pkgs) {
      String[] idv = p.split("#");
      NpmPackage npm = idv.length > 1 ? pcm.loadPackage(idv[0], idv[1]) : pcm.loadPackage(idv[0]);
      context.loadFromPackage(npm, null);
    }

    // Dependencies (manifest include/register files): setIds + cache, as testGen does.
    List<StructureDefinition> included = new ArrayList<>();
    if (deps != null) {
      ProfileUtilities pud = new ProfileUtilities(context, messages, null);
      for (String d : deps.split(",")) {
        StructureDefinition sd = (StructureDefinition) parse(d);
        pud.setIds(sd, false);
        included.add(sd);
      }
      for (StructureDefinition sd : included)
        if (!context.hasResource(StructureDefinition.class, sd.getUrl()))
          context.cacheResource(sd);
    }

    StructureDefinition source = (StructureDefinition) parse(input);

    // Ensure base chain has snapshots (mirrors SnapShotGenerationTests.getSD: always sorted).
    StructureDefinition base = ensureSnapshot(source.getBaseDefinition());

    StructureDefinition out = source.copy();
    ProfileUtilities pu = new ProfileUtilities(context, messages, null);
    pu.setNewSlicingProcessing(nsp);
    pu.setThrowException(false);
    pu.setDebug(false);
    pu.setIds(out, false);
    pu.setAllowUnknownProfile(AllowUnknownProfile.ALL_TYPES);
    pu.setAutoFixSliceNames(autofix);
    if (xver) {
      try {
        if (!context.hasPackage(CommonPackages.ID_XVER, CommonPackages.VER_XVER)) {
          NpmPackage npm = pcm.loadPackage(CommonPackages.ID_XVER, CommonPackages.VER_XVER);
          context.loadFromPackage(npm, new TestPackageLoader(Utilities.stringSet("StructureDefinition")));
        }
        pu.setXver(XVerExtensionManagerFactory.createExtensionManager(context));
      } catch (Exception e) { System.err.println("WARN: xver setup failed: " + e.getMessage()); }
    }
    if (sort) {
      List<String> errors = new ArrayList<>();
      pu.sortDifferential(base, out, source.getName(), errors, false);
      if (!errors.isEmpty()) { System.err.println("EXCEPTION: sort failed: " + errors); System.exit(2); }
    }

    try {
      pu.generateSnapshot(base, out, source.getUrl(), "http://test.org/profile", source.getName());
    } catch (Throwable e) {
      dumpMessages();
      System.err.println("EXCEPTION: " + e.getClass().getSimpleName() + ": " + e.getMessage());
      e.printStackTrace(System.err); // full trace for repro reports (2026-09-03)
      System.exit(2);
    }
    dumpMessages();

    out.setText(null); // narrative excluded from comparison, as in the JUnit golden diff
    if (output.endsWith(".json"))
      new JsonParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(output), out);
    else
      new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(output), out);
  }

  // Mirrors SnapShotGenerationTests.getSD: bases without snapshots get sortDifferential +
  // setIds + generateSnapshot (newSlicingProcessing=true), recursively.
  static StructureDefinition ensureSnapshot(String url) throws Exception {
    StructureDefinition sd = context.fetchResource(StructureDefinition.class, url);
    if (sd == null) throw new Exception("Unable to find profile " + url);
    if (!sd.hasSnapshot()) {
      StructureDefinition b = ensureSnapshot(sd.getBaseDefinition());
      ProfileUtilities pu = new ProfileUtilities(context, messages, null);
      pu.setNewSlicingProcessing(true);
      List<String> errors = new ArrayList<>();
      pu.sortDifferential(b, sd, url, errors, false);
      if (!errors.isEmpty()) throw new Exception("sort of base failed: " + errors.get(0));
      pu.setIds(sd, false);
      pu.generateSnapshot(b, sd, sd.getUrl(), "http://test.org/profile", sd.getName());
    }
    return sd;
  }

  static Resource parse(String file) throws Exception {
    try (FileInputStream fs = new FileInputStream(file)) {
      return file.endsWith(".json") ? new JsonParser().parse(fs) : new XmlParser().parse(fs);
    }
  }

  static void dumpMessages() {
    for (ValidationMessage vm : messages)
      System.err.println("MSG: [" + vm.getLevel() + "] " + vm.getLocation() + ": " + vm.getMessage());
  }
}
