// Scratch probe for harness debugging (contents change per investigation).
// Current: why don't hl7.fhir.uv.extensions SDs resolve from the BatchRunner context?
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.test.utils.TestPackageLoader;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.npm.NpmPackage;

import java.io.FileInputStream;

public class ProbeRunner {
  public static void main(String[] args) throws Exception {
    SimpleWorkerContext context = new SimpleWorkerContext.SimpleWorkerContextBuilder()
        .withAllowLoadingDuplicates(true).withUserAgent("probe")
        .fromPackage(NpmPackage.fromPackage(new FileInputStream(args[0]))); // core tgz
    context.setExpansionParameters(new Parameters());
    NpmPackage ext = NpmPackage.fromPackage(new FileInputStream(args[1])); // uv.extensions tgz
    System.out.println("pkg id=" + ext.name() + " ver=" + ext.version() + " fhirVersion=" + ext.fhirVersion()
        + " canonical=" + ext.canonical());
    System.out.println("canLazyLoad=" + ext.canLazyLoad());
    System.out.println("listResources(SD) count=" + ext.listResources(Utilities.stringSet("StructureDefinition")).size());
    System.out.println("listIndexedResources(SD) count=" + ext.listIndexedResources(Utilities.stringSet("StructureDefinition")).size());
    int loaded = context.loadFromPackage(ext, new TestPackageLoader(Utilities.stringSet("CodeSystem", "ValueSet", "StructureDefinition")));
    System.out.println("loadFromPackage returned " + loaded);
    StructureDefinition sd = context.fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/patient-birthTime");
    System.out.println("patient-birthTime: " + (sd == null ? "NOT FOUND" : "found, version=" + sd.getVersion() + " hasSnapshot=" + sd.hasSnapshot()));
    int n = 0, ext5 = 0;
    for (StructureDefinition s : context.fetchResourcesByType(StructureDefinition.class)) {
      n++;
      if (s.getUrl() != null && s.getUrl().contains("birthTime"))
        System.out.println("  SD in context: url=" + s.getUrl() + " ver=" + s.getVersion());
      if ("5.2.0".equals(s.getVersion())) ext5++;
    }
    System.out.println("total SDs=" + n + " with version 5.2.0=" + ext5);
  }
}
