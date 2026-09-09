// Batch Java-oracle runner for the snapshot-spec harness (Phase 4 packet 2).
// One JVM for the whole sweep: context loaded once, tests run in manifest order with a
// SHARED cumulative context — exactly like the upstream JUnit driver (SnapShotGenerationTests
// uses one shared worker context for the whole run and caches every generated output into it).
//
// Faithful replication of SnapShotGenerationTests (source @ b06c7ee, engine = validator_cli
// 6.10.2 on the classpath):
//   - TestDetails loading incl. include (XML only, :223) and register (xml→json fallback, :224-231)
//   - register block (:563-590): pu(context, localMsgs, null) + nsp=true + allow, setIds on all
//     included, cacheResource if absent, snapshot generated for included.get(0) when its base
//     resolves FROM CONTEXT, ERROR messages => register failure. No setThrowException call
//     (default false), no xver on that pu.
//   - getSD (:683-706): universe (all tests' expected + included) BEFORE context; bases without
//     snapshots get sortDifferential (ALWAYS) + setIds + generateSnapshot with nsp=true.
//   - testGen (:558-681): output = source.copy() BEFORE setIds(source) — the generation copy does
//     NOT get pre-assigned ids (setIds targets the source for the FHIRPath fixture() rules);
//     pu = (context, messages, TestPKP), nsp per manifest, throwException=false, allow per
//     manifest, xver; sortDifferential when sort=true; golden = equalsDeep after setText(null);
//     generated output cached into the context (:648) so later tests can build on it.
//   - testSort (:544-556): output = source.copy(), pu(context, null, null), setIds(source),
//     sortDifferential, equalsDeep vs expected.
//   - fail semantics: THREW (exception incl. register/sort/base failures) vs ERRORS (generated
//     but ERROR-level messages => JUnit throws at :677) vs NO-THROW; regex recorded when threw.
// Known deliberate omissions (documented in README): no narrative rendering (:637-642 — a render
// crash fails a JUnit test; we don't render), no FHIRPath <rule> evaluation, 2-arg fetchResource
// (no version-resolution rules).
//
// Also performs the three-way compares inline (same JVM): java-vs-golden, dotnet-vs-golden,
// dotnet-vs-java, reading out/<id>/dotnet.xml produced by the .NET batch runner beforehand.
// Normalized pretty-JSON of all three sides is written for the structural report.
//
// Usage:
//   java -Xmx4g -cp ../tools/validator_cli.jar BatchRunner.java
//     -core <hl7.fhir.r5.core.tgz> -tests <snapshot-generation dir> -out <out dir>
//     -pkgdir <dir with hl7.terminology.r5.tgz, hl7.fhir.uv.extensions.tgz, hl7.fhir.uv.sdc.tgz,
//              hl7.fhir.xver-extensions.tgz>  (local files; network is NAT64-blocked)
//     [-ids id1,id2,...]   (default: all tests in manifest order)
//
// Per test prints:  RESULT\t<id>\t<mode>\t<java>\t<jerr>\t<net>\t<nvj>\t<note>
//   mode = gen|sort|fail-gen|fail-sort
//   java = EQUAL|DIFF|THREW|ERRORS|NO-THROW|ERROR   (fail tests: THREW/ERRORS = "passed")
//   jerr = count of ERROR-level ValidationMessages on the Java side
//   net  = EQUAL|DIFF|MISSING|-   (dotnet.xml vs golden)   nvj = EQUAL|DIFF|-
// All ValidationMessages go to out/<id>/java.log (MSG:/EXCEPTION: lines, as before).

import org.hl7.fhir.r5.conformance.profile.BindingResolution;
import org.hl7.fhir.r5.conformance.profile.ProfileKnowledgeProvider;
import org.hl7.fhir.r5.conformance.profile.ProfileUtilities;
import org.hl7.fhir.r5.conformance.profile.ProfileUtilities.AllowUnknownProfile;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.ElementDefinition.ElementDefinitionBindingComponent;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r5.model.StructureDefinition.TypeDerivationRule;
import org.hl7.fhir.r5.test.utils.TestPackageLoader;
import org.hl7.fhir.r5.utils.xver.XVerExtensionManagerFactory;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.npm.CommonPackages;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.exceptions.FHIRException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BatchRunner {

  static SimpleWorkerContext context;
  static PrintStream results; // out/java-results.tsv — RESULT lines duplicated here, away from logger noise
  static File testsDir, outDir, pkgDir;
  static Map<String, TestDetails> tests = new LinkedHashMap<>();
  static List<ValidationMessage> messages; // per-test, like the driver's field
  static boolean xverInstalled = false;

  static class TestDetails {
    String id, include, register, regex;
    boolean gen, sort, fail, nsp, debug, sourceJson, json;
    AllowUnknownProfile allow = AllowUnknownProfile.ALL_TYPES;
    StructureDefinition source, expected;
    List<StructureDefinition> included = new ArrayList<>();
  }

  public static void main(String[] args) throws Exception {
    String core = null, ids = null;
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "-core": core = args[++i]; break;
        case "-tests": testsDir = new File(args[++i]); break;
        case "-out": outDir = new File(args[++i]); break;
        case "-pkgdir": pkgDir = new File(args[++i]); break;
        case "-ids": ids = args[++i]; break;
        default: throw new IllegalArgumentException("Unknown arg: " + args[i]);
      }
    }
    if (core == null || testsDir == null || outDir == null)
      throw new IllegalArgumentException("-core, -tests and -out are required");

    loadManifest();
    outDir.mkdirs();
    results = new PrintStream(new FileOutputStream(new File(outDir, "java-results.tsv")), true, "UTF-8");
    buildContext(core);

    List<String> selected = new ArrayList<>();
    if (ids == null) selected.addAll(tests.keySet());
    else for (String s : ids.split(",")) selected.add(s.trim());

    for (String id : selected) {
      TestDetails t = tests.get(id);
      if (t == null) { result("RESULT\t" + id + "\t?\tNOT-IN-MANIFEST\t0\t-\t-\t"); continue; }
      System.err.println("---- " + id + " -----------------------------------------");
      runOne(t);
    }
    result("DONE");
    results.close();
  }

  static void result(String line) {
    System.out.println(line);
    results.println(line);
  }

  // ---- manifest + fixtures (mirrors TestDetails ctor :125-152 and load() :206-233) ----

  static void loadManifest() throws Exception {
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new File(testsDir, "manifest.xml"));
    for (Node n = doc.getDocumentElement().getFirstChild(); n != null; n = n.getNextSibling()) {
      if (!(n instanceof Element) || !n.getNodeName().equals("test")) continue;
      Element e = (Element) n;
      TestDetails t = new TestDetails();
      t.id = e.getAttribute("id");
      t.gen = "true".equals(e.getAttribute("gen"));
      t.sort = "true".equals(e.getAttribute("sort"));
      t.fail = "true".equals(e.getAttribute("fail"));
      t.nsp = !"false".equals(e.getAttribute("new-slice-processing"));
      t.debug = "true".equals(e.getAttribute("debug"));
      t.include = e.getAttribute("include");
      t.register = e.getAttribute("register");
      t.regex = e.getAttribute("regex");
      String aa = e.getAttribute("allow");
      t.allow = "no-extensions".equals(aa) ? AllowUnknownProfile.NON_EXTNEIONS
          : "none".equals(aa) ? AllowUnknownProfile.NONE : AllowUnknownProfile.ALL_TYPES;
      // load fixtures eagerly, as data()/load() does for every test up front
      File ij = new File(testsDir, t.id + "-input.json");
      t.sourceJson = ij.exists();
      t.source = (StructureDefinition) parse(t.sourceJson ? ij : new File(testsDir, t.id + "-input.xml"));
      if (!t.fail) {
        File xj = new File(testsDir, t.id + "-expected.json");
        t.json = xj.exists();
        t.expected = (StructureDefinition) parse(t.json ? xj : new File(testsDir, t.id + "-expected.xml"));
      }
      if (!Utilities.noString(t.include)) // include: XML only (driver :223)
        t.included.add((StructureDefinition) parse(new File(testsDir, t.include + ".xml")));
      if (!Utilities.noString(t.register)) {
        for (String s : t.register.split("\\,")) {
          File fx = new File(testsDir, s + ".xml");
          t.included.add((StructureDefinition) parse(fx.exists() ? fx : new File(testsDir, s + ".json")));
        }
      }
      tests.put(t.id, t);
    }
    System.err.println("Manifest: " + tests.size() + " tests loaded");
  }

  // ---- context (mirrors TestingUtilities.getWorkerContext + setUp SDC load) ----
  // Packages are loaded THROUGH the FilesystemPackageCacheManager (pre-populated from local
  // tgzs since this network is NAT64-blocked). This matters beyond connectivity: a cache-backed
  // NpmPackage canLazyLoad(), so loadFromPackage takes the lazy .index.json branch — the branch
  // CI uses. A stream-loaded NpmPackage takes the EAGER branch, where TestPackageLoader's
  // loadBundle() stub returns null and every resource is silently dropped (verified 2026-08-26:
  // "loadFromPackage returned 739" yet zero uv.extensions SDs resolvable).

  static FilesystemPackageCacheManager pcm;

  static void buildContext(String core) throws Exception {
    context = new SimpleWorkerContext.SimpleWorkerContextBuilder()
        .withAllowLoadingDuplicates(true).withUserAgent("snapshot-spec-harness")
        .fromPackage(NpmPackage.fromPackage(new FileInputStream(core)));
    context.setExpansionParameters(new Parameters());
    pcm = new FilesystemPackageCacheManager.Builder().build();
    // versions: THO + SDC = latest at harness-build time (driver loads unpinned latest),
    // uv.extensions = Constants.EXTENSIONS_WORKING_VERSION @ b06c7ee, xver = CommonPackages
    loadPkg("hl7.terminology.r5", "7.3.0", new TestPackageLoader(Utilities.stringSet("CodeSystem", "ValueSet", "NamingSystem")));
    loadPkg("hl7.fhir.uv.extensions", "5.2.0", new TestPackageLoader(Utilities.stringSet("CodeSystem", "ValueSet", "StructureDefinition")));
    loadPkg("hl7.fhir.uv.sdc", "4.0.0", null); // full load, as in setUp (:487-489)
  }

  static NpmPackage cachedPkg(String id, String ver) throws Exception {
    NpmPackage npm = null;
    try { npm = pcm.loadPackageFromCacheOnly(id, ver); } catch (Exception e) { /* not cached */ }
    if (npm == null) {
      File f = new File(pkgDir, id + ".tgz");
      try (FileInputStream fs = new FileInputStream(f)) {
        npm = pcm.addPackageToCache(id, ver, fs, f.getAbsolutePath());
      }
    }
    return npm;
  }

  static void loadPkg(String id, String ver, TestPackageLoader loader) {
    try {
      NpmPackage npm = cachedPkg(id, ver);
      context.loadFromPackage(npm, loader);
      System.err.println("Loaded " + id + "#" + npm.version() + " (lazy=" + npm.canLazyLoad() + ")");
    } catch (Exception e) { System.err.println("WARN: " + id + " load failed: " + e.getMessage()); }
  }

  // xver: driver loads it lazily inside testGen (:602-606), guarded by hasPackage
  static void ensureXver(ProfileUtilities pu) {
    try {
      if (!xverInstalled && !context.hasPackage(CommonPackages.ID_XVER, CommonPackages.VER_XVER)) {
        NpmPackage npm = cachedPkg(CommonPackages.ID_XVER, CommonPackages.VER_XVER);
        context.loadFromPackage(npm, new TestPackageLoader(Utilities.stringSet("StructureDefinition")));
        xverInstalled = true;
      }
      pu.setXver(XVerExtensionManagerFactory.createExtensionManager(context));
    } catch (Exception e) { System.err.println("WARN: xver setup failed: " + e.getMessage()); }
  }

  // ---- per-test ----

  static void runOne(TestDetails t) {
    File tdir = new File(outDir, t.id);
    tdir.mkdirs();
    messages = new ArrayList<>();
    String mode = (t.fail ? "fail-" : "") + (t.gen ? "gen" : "sort");
    String javaV, note = "";
    int jerr = 0;
    PrintStream log = null;
    try {
      log = new PrintStream(new FileOutputStream(new File(tdir, "java.log")), true, "UTF-8");
      StructureDefinition output = null;
      Throwable thrown = null;
      List<ValidationMessage> ml = new ArrayList<>();
      try {
        output = t.gen ? testGen(t) : testSort(t);
        for (ValidationMessage vm : messages)
          if (vm.getLevel() == IssueSeverity.ERROR) ml.add(vm);
      } catch (Throwable e) {
        thrown = e;
        log.println("EXCEPTION: " + e.getClass().getSimpleName() + ": " + e.getMessage());
      }
      for (ValidationMessage vm : messages) {
        log.println("MSG: [" + vm.getLevel() + "] " + vm.getLocation() + ": " + vm.getMessage());
        if (vm.getLevel() == IssueSeverity.ERROR) jerr++;
      }

      if (t.fail) {
        // JUnit pass = something threw, OR generated with ERROR messages (throw at :677)
        javaV = thrown != null ? "THREW" : !ml.isEmpty() ? "ERRORS" : "NO-THROW";
        if (thrown != null && !Utilities.noString(t.regex))
          note = (thrown.getMessage() != null && thrown.getMessage().matches(t.regex))
              ? "regex-ok" : "regex-MISMATCH";
        result("RESULT\t" + t.id + "\t" + mode + "\t" + javaV + "\t" + jerr + "\t-\t-\t" + note);
        return;
      }

      if (thrown != null) {
        javaV = "THREW";
        note = trim(thrown.getMessage());
      } else {
        // write output + norms, golden compare (equalsDeep, text already stripped)
        compose(new File(tdir, "java.xml"), output, false);
        compose(new File(tdir, "java.norm.json"), output, true);
        StructureDefinition exp = t.expected.copy();
        exp.setText(null);
        compose(new File(tdir, "expected.norm.json"), exp, true);
        javaV = exp.equalsDeep(output) ? "EQUAL" : "DIFF";
        if (!ml.isEmpty()) note = "gen-errors:" + ml.size(); // JUnit would fail this test at :677
      }

      // .NET compares (dotnet.xml written earlier by the .NET batch runner)
      String netV = "-", nvj = "-";
      File dn = new File(tdir, "dotnet.xml");
      if (dn.exists()) {
        try {
          StructureDefinition net = (StructureDefinition) parse(dn);
          net.setText(null);
          compose(new File(tdir, "dotnet.norm.json"), net, true);
          StructureDefinition exp = t.expected.copy();
          exp.setText(null);
          netV = exp.equalsDeep(net) ? "EQUAL" : "DIFF";
          if (output != null) nvj = net.equalsDeep(output) ? "EQUAL" : "DIFF";
        } catch (Exception e) { netV = "PARSE-FAIL"; note = note + " net:" + trim(e.getMessage()); }
      } else netV = "MISSING";

      result("RESULT\t" + t.id + "\t" + mode + "\t" + javaV + "\t" + jerr + "\t" + netV + "\t" + nvj + "\t" + note);
    } catch (Throwable e) {
      result("RESULT\t" + t.id + "\t" + mode + "\tERROR\t" + jerr + "\t-\t-\t" + trim(e.getMessage()));
    } finally {
      if (log != null) log.close();
    }
  }

  // ---- testGen replica (:558-681, minus rendering and FHIRPath rules) ----

  static StructureDefinition testGen(TestDetails test) throws Exception {
    if (!Utilities.noString(test.register)) {
      List<ValidationMessage> regMessages = new ArrayList<>();
      ProfileUtilities pu = new ProfileUtilities(context, regMessages, null);
      pu.setNewSlicingProcessing(true);
      pu.setAllowUnknownProfile(test.allow);
      for (StructureDefinition sd : test.included)
        pu.setIds(sd, false);
      for (StructureDefinition sd : test.included)
        if (!context.hasResource(StructureDefinition.class, sd.getUrl(), sd.getVersion(), sd))
          context.getManager().cacheResource(sd);
      StructureDefinition base = context.fetchResource(StructureDefinition.class,
          test.included.get(0).getBaseDefinition()); // context only, per :576 (no universe)
      if (base != null)
        pu.generateSnapshot(base, test.included.get(0), test.included.get(0).getUrl(),
            "http://test.org/profile", test.included.get(0).getName());
      int ec = 0;
      for (ValidationMessage vm : regMessages)
        if (vm.getLevel() == IssueSeverity.ERROR) { System.err.println(vm.summary()); ec++; }
      if (ec > 0)
        throw new FHIRException("register gen failed: " + regMessages.toString());
    }

    StructureDefinition base = getSD(test.source.getBaseDefinition());
    if (!base.getUrl().equals(test.source.getBaseDefinition())
        && !base.getVersionedUrl().equals(test.source.getBaseDefinition()))
      throw new Exception("URL mismatch on base: " + base.getUrl() + " wanting " + test.source.getBaseDefinition());

    StructureDefinition output = test.source.copy(); // copy taken BEFORE setIds (:595 vs :600)
    ProfileUtilities pu = new ProfileUtilities(context, messages, new TestPKP());
    pu.setNewSlicingProcessing(test.nsp);
    pu.setThrowException(false);
    pu.setDebug(test.debug);
    pu.setIds(test.source, false); // ids pre-assigned on the SOURCE, not the generation copy
    pu.setAllowUnknownProfile(test.allow);
    ensureXver(pu);
    if (test.sort) {
      List<String> errors = new ArrayList<>();
      pu.sortDifferential(base, output, test.source.getName(), errors, false);
      if (!errors.isEmpty()) throw new FHIRException("Sort failed: " + errors.toString());
    }
    // (driver's SnapshotGenerationPreProcessor dump to [tmp] is a debug aid only — skipped)
    messages.clear();
    pu.generateSnapshot(base, output, test.source.getUrl(), "http://test.org/profile", test.source.getName());
    output.setText(null);
    if (!test.fail)
      context.getManager().cacheResource(output); // :648 — later tests can derive from this output
    return output;
  }

  // ---- testSort replica (:544-556) ----

  static StructureDefinition testSort(TestDetails test) throws Exception {
    StructureDefinition base = getSD(test.source.getBaseDefinition());
    StructureDefinition output = test.source.copy();
    ProfileUtilities pu = new ProfileUtilities(context, null, null);
    pu.setIds(test.source, false);
    List<String> errors = new ArrayList<>();
    pu.sortDifferential(base, output, output.getUrl(), errors, false);
    if (!errors.isEmpty()) throw new FHIRException(errors.get(0));
    output.setText(null);
    return output;
  }

  // ---- getSD replica (:683-706): universe first, then context; recursive base preparation ----

  static StructureDefinition getSD(String url) throws Exception {
    StructureDefinition sd = getByUrl(url);
    if (sd == null)
      sd = context.fetchResource(StructureDefinition.class, url);
    if (sd == null) {
      if (url.contains("|")) url = url.substring(0, url.indexOf("|"));
      throw new FHIRException("Unable to find profile " + url);
    }
    if (!sd.hasSnapshot()) {
      StructureDefinition base = getSD(sd.getBaseDefinition());
      ProfileUtilities pu = new ProfileUtilities(context, messages, new TestPKP());
      pu.setNewSlicingProcessing(true);
      List<String> errors = new ArrayList<>();
      pu.sortDifferential(base, sd, url, errors, false); // Java test bases are ALWAYS sorted
      if (!errors.isEmpty()) throw new FHIRException(errors.get(0));
      pu.setIds(sd, false);
      pu.generateSnapshot(base, sd, sd.getUrl(), "http://test.org/profile", sd.getName());
    }
    return sd;
  }

  // getByUrl replica (:442-454): every test's expected, then every test's included
  static StructureDefinition getByUrl(String url) {
    if (url == null) return null;
    for (TestDetails t : tests.values()) {
      if (t.expected != null && url.equals(t.expected.getUrl())) return t.expected;
      for (StructureDefinition sd : t.included)
        if (url.equals(sd.getUrl())) return sd;
    }
    return null;
  }

  // ---- helpers ----

  static Resource parse(File f) throws Exception {
    try (FileInputStream fs = new FileInputStream(f)) {
      return f.getName().endsWith(".json") ? new JsonParser().parse(fs) : new XmlParser().parse(fs);
    }
  }

  static void compose(File f, StructureDefinition sd, boolean json) throws Exception {
    try (FileOutputStream fs = new FileOutputStream(f)) {
      if (json) new JsonParser().setOutputStyle(OutputStyle.PRETTY).compose(fs, sd);
      else new XmlParser().setOutputStyle(OutputStyle.PRETTY).compose(fs, sd);
    }
  }

  static String trim(String s) {
    if (s == null) return "";
    s = s.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    return s.length() > 180 ? s.substring(0, 180) : s;
  }

  // TestPKP replica (:244-325) — same dummy binding/link answers, real type checks via context
  static class TestPKP implements ProfileKnowledgeProvider {
    public boolean isDatatype(String name) {
      StructureDefinition sd = context.fetchTypeDefinition(name);
      return sd != null && sd.getDerivation() == TypeDerivationRule.SPECIALIZATION
          && (sd.getKind() == StructureDefinitionKind.PRIMITIVETYPE || sd.getKind() == StructureDefinitionKind.COMPLEXTYPE);
    }
    public boolean isPrimitiveType(String name) {
      StructureDefinition sd = context.fetchTypeDefinition(name);
      return sd != null && sd.getDerivation() == TypeDerivationRule.SPECIALIZATION
          && sd.getKind() == StructureDefinitionKind.PRIMITIVETYPE;
    }
    public boolean isResource(String name) {
      StructureDefinition sd = context.fetchTypeDefinition(name);
      return sd != null && sd.getDerivation() == TypeDerivationRule.SPECIALIZATION
          && sd.getKind() == StructureDefinitionKind.RESOURCE;
    }
    public boolean hasLinkFor(String name) { return isDatatype(name); }
    public String getLinkFor(String corePath, String name) { return Utilities.pathURL(corePath, "datatypes.html#" + name); }
    public BindingResolution resolveBinding(StructureDefinition def, ElementDefinitionBindingComponent binding, String path) {
      BindingResolution br = new BindingResolution();
      br.url = path + "/something.html";
      br.display = "something";
      return br;
    }
    public BindingResolution resolveBinding(StructureDefinition def, String url, String path, org.hl7.fhir.r5.model.Element ctxt) {
      BindingResolution br = new BindingResolution();
      br.url = path + "/something.html";
      br.display = "something";
      return br;
    }
    public String getLinkForProfile(StructureDefinition profile, String url) {
      StructureDefinition sd = context.fetchResource(StructureDefinition.class, url);
      return sd == null ? url + "|" + url : sd.getId() + ".html|" + sd.present();
    }
    public boolean prependLinks() { return false; }
    public String getLinkForUrl(String corePath, String s) { return null; }
    public String getCanonicalForDefaultContext() { return null; }
    public String getDefinitionsName(Resource r) { return null; }
  }
}
