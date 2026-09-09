// Golden-file comparer for the snapshot-spec harness.
// Mirrors the JUnit oracle's comparison (SnapShotGenerationTests.testGen:659-675):
// deep equality after stripping narrative text on both sides.
// Also writes both sides as normalized pretty JSON for line diffing.
//
// Usage: java -cp ../tools/validator_cli.jar Compare.java <fileA> <fileB> <normA.json> <normB.json>
// Exit codes: 0 = EQUAL (equalsDeep), 1 = DIFFERENT, 2 = error.

import org.hl7.fhir.r5.formats.IParser.OutputStyle;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.formats.XmlParser;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StructureDefinition;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Compare {
  public static void main(String[] args) throws Exception {
    StructureDefinition a = load(args[0]);
    StructureDefinition b = load(args[1]);
    a.setText(null);
    b.setText(null);
    new JsonParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(args[2]), a);
    new JsonParser().setOutputStyle(OutputStyle.PRETTY).compose(new FileOutputStream(args[3]), b);
    boolean eq = a.equalsDeep(b);
    System.out.println(eq ? "EQUAL" : "DIFFERENT");
    System.exit(eq ? 0 : 1);
  }

  static StructureDefinition load(String file) throws Exception {
    try (FileInputStream fs = new FileInputStream(file)) {
      Resource r = file.endsWith(".json") ? new JsonParser().parse(fs) : new XmlParser().parse(fs);
      return (StructureDefinition) r;
    }
  }
}
