package com.jackson.migration.mta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void ignoresGeneratedWorkbenchAndBuildFindingsButRetainsMavenDependencies() throws Exception {
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
                        - uri: file:///home/madun/.m2/repository/org/example/lib/1.0/lib-1.0.jar
                          lineNumber: 1
                          message: Keep Maven repository dependency finding
                        - uri: file:///workspace/src/main/resources/META-INF/persistence.xml
                          lineNumber: 5
                          message: Keep application finding
                """);

        List<MigrationFinding> findings = new MtaOutputParser().parse(report);

        assertEquals(2, findings.size());
        assertTrue(findings.stream().anyMatch(finding -> finding.file().contains("/.m2/repository/")));
        assertTrue(findings.stream().anyMatch(finding -> finding.file().endsWith("/src/main/resources/META-INF/persistence.xml")));
    }

    @Test
    void parsesMtaReportLargerThanSnakeYamlDefaultLimit() throws Exception {
        Path report = temp.resolve("large-output.yaml");
        String padding = "x".repeat(4 * 1024 * 1024);
        Files.writeString(report, """
                - name: oversized-mta-output
                  description: %s
                  violations:
                    primefaces-schedule-0001:
                      category: potential
                      incidents:
                        - uri: file:///workspace/src/main/webapp/pages/advanced.xhtml
                          lineNumber: 33
                          message: Review schedule
                """.formatted(padding));

        List<MigrationFinding> findings = new MtaOutputParser().parse(report);

        assertEquals(1, findings.size());
        assertEquals("primefaces-schedule-0001", findings.get(0).ruleId());
        assertEquals(33, findings.get(0).line());
    }

    @Test
    void capturesInsightsSeparatelyWithoutMixingThemIntoActionableFindings() throws Exception {
        Path report = temp.resolve("insights-output.yaml");
        Files.writeString(report, """
                - name: mixed-report
                  violations:
                    migration-rule:
                      category: mandatory
                      incidents:
                        - uri: file:///workspace/src/main/java/demo/Bean.java
                          lineNumber: 10
                          message: migrate this
                  insights:
                    javaee-technology-usage-00020-jakarta:
                      description: JavaEE Jakarta
                      incidents:
                        - uri: file:///workspace/src/main/java/demo/Bean.java
                          lineNumber: 7
                          message: discovered CDI
                """);

        MtaOutputParser parser = new MtaOutputParser();
        List<MigrationFinding> findings = parser.parse(report);
        List<MigrationFinding> insights = parser.parseInsights(report);

        assertEquals(List.of("migration-rule"), findings.stream().map(MigrationFinding::ruleId).toList());
        assertEquals(List.of("javaee-technology-usage-00020-jakarta"), insights.stream().map(MigrationFinding::ruleId).toList());
        assertEquals("insight", insights.get(0).category());
        assertEquals(1, parser.parseReport(report).violationIncidentCount());
    }

    @Test
    void reportsRawViolationIncidentCountEvenWhenPlanningFindingsAreDeduplicated() throws Exception {
        Path report = temp.resolve("incident-count-output.yaml");
        Files.writeString(report, """
                - name: app-one
                  violations:
                    rule-a:
                      category: mandatory
                      incidents:
                        - uri: file:///workspace/src/main/java/demo/A.java
                          lineNumber: 10
                          message: duplicate incident
                        - uri: file:///workspace/src/main/java/demo/A.java
                          lineNumber: 10
                          message: duplicate incident
                        - uri: file:///workspace/src/main/java/demo/B.java
                          lineNumber: 20
                          message: another incident
                    rule-b:
                      category: potential
                      incidents:
                        - uri: file:///workspace/src/main/java/demo/C.java
                          lineNumber: 30
                          message: third incident
                - name: app-two
                  violations:
                    rule-c:
                      category: mandatory
                      incidents:
                        - uri: file:///workspace/src/main/java/demo/D.java
                          lineNumber: 40
                          message: fourth incident
                """);

        MtaOutputParser.ParsedReport parsed = new MtaOutputParser().parseReport(report);

        assertEquals(5, parsed.violationIncidentCount());
        assertEquals(5, parsed.incidents().size());
        assertEquals(4, parsed.planningFindings().size());
    }

}
