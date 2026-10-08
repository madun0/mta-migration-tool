package com.jackson.migration.mta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MtaOutputParserTest {

    @TempDir
    Path temp;

    @Test
    void parsesViolationsMapWhereRuleIdIsTheMapKey() throws Exception {
        Path report = temp.resolve("output.yaml");
        Files.writeString(report, """
                - name: eap8/eap7
                  description: sample
                  violations:
                    javax-to-jakarta-import-00001:
                      description: Replace javax import
                      category: mandatory
                      incidents:
                        - uri: file:///workspace/src/main/java/demo/Bean.java
                          lineNumber: 12
                          message: Replace javax.faces with jakarta.faces
                        - uri: file:///workspace/src/main/java/demo/Other.java
                          lineNumber: 7
                          message: Replace javax.faces with jakarta.faces
                """);

        List<MigrationFinding> findings = new MtaOutputParser().parse(report);

        assertEquals(2, findings.size());
        assertEquals("javax-to-jakarta-import-00001", findings.get(0).ruleId());
        assertEquals("mandatory", findings.get(0).category());
        assertEquals(12, findings.get(0).line());
    }

    @Test
    void stillParsesDirectRuleIdShape() throws Exception {
        Path report = temp.resolve("output.yaml");
        Files.writeString(report, """
                - ruleID: primefaces-calendar-0001
                  category: mandatory
                  incidents:
                    - uri: file:///workspace/view.xhtml
                      lineNumber: 4
                      message: Migrate p:calendar
                """);

        List<MigrationFinding> findings = new MtaOutputParser().parse(report);

        assertEquals(1, findings.size());
        assertEquals("primefaces-calendar-0001", findings.get(0).ruleId());
    }
    @Test
    void ignoresGeneratedWorkbenchAndBuildFindings() throws Exception {
        Path report = temp.resolve("output.yaml");
        Files.writeString(report, """
                - name: eap8/eap7
                  violations:
                    javaee-to-jakarta-namespaces-00033:
                      category: mandatory
                      incidents:
                        - uri: file:///workspace/.migration/assessment/output.yaml
                          lineNumber: 601
                          message: Ignore generated assessment output
                        - uri: file:///workspace/target/generated.xml
                          lineNumber: 2
                          message: Ignore build output
                        - uri: file:///workspace/.git/config
                          lineNumber: 1
                          message: Ignore Git metadata
                        - uri: file:///workspace/src/main/resources/META-INF/persistence.xml
                          lineNumber: 5
                          message: Keep application finding
                """);

        List<MigrationFinding> findings = new MtaOutputParser().parse(report);

        assertEquals(1, findings.size());
        assertEquals("file:///workspace/src/main/resources/META-INF/persistence.xml", findings.get(0).file());
    }

}
