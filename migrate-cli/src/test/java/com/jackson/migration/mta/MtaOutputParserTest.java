package com.jackson.migration.mta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void filtersFindingsOutsideSelectedApplicationRoot() throws Exception {
        Path project = Files.createDirectories(temp.resolve("workspace/app"));
        Path source = Files.createDirectories(project.resolve("src/main/java/demo"))
                .resolve("Bean.java");
        Files.writeString(source, "class Bean {}\n");

        Path sibling = Files.createDirectories(temp.resolve("workspace/unrelated/src"))
                .resolve("Other.java");
        Files.writeString(sibling, "class Other {}\n");

        Path mavenRepo = Files.createDirectories(temp.resolve("home/.m2/repository/demo/lib/1.0"))
                .resolve("lib-1.0.jar");
        Files.writeString(mavenRepo, "not-a-real-jar");

        List<MigrationFinding> scoped = new MtaOutputParser().filterToProjectRoot(List.of(
                new MigrationFinding("inside", "", source.toUri().toString(), 1, "mandatory"),
                new MigrationFinding("sibling", "", sibling.toUri().toString(), 1, "mandatory"),
                new MigrationFinding("maven", "", mavenRepo.toUri().toString(), 1, "mandatory")
        ), project.toRealPath());

        assertEquals(1, scoped.size());
        assertEquals("inside", scoped.get(0).ruleId());
    }

    @Test
    void windowsAndGitBashPathsUseSameApplicationBoundary() {
        String root = "C:\\Users\\madun\\wksp\\showcase";

        assertTrue(MtaOutputParser.isWithinProjectPath(
                root,
                "file:///C:/Users/madun/wksp/showcase/src/main/java/demo/Bean.java"));
        assertTrue(MtaOutputParser.isWithinProjectPath(
                root,
                "/c/Users/madun/wksp/showcase/src/main/webapp/index.xhtml"));
        assertFalse(MtaOutputParser.isWithinProjectPath(
                root,
                "file:///C:/Users/madun/wksp/unrelated/src/main/java/demo/Other.java"));
        assertFalse(MtaOutputParser.isWithinProjectPath(
                root,
                "file:///C:/Users/madun/wksp/showcase-other/src/main/java/demo/Other.java"));
        assertFalse(MtaOutputParser.isWithinProjectPath(
                root,
                "file:///C:/Users/madun/.m2/repository/org/example/lib/1.0/lib-1.0.jar"));
    }

    @Test
    void relativeMtaFindingPathsAreScopedToProjectRoot() {
        String root = "C:/work/showcase";
        assertTrue(MtaOutputParser.isWithinProjectPath(root, "src/main/java/demo/Bean.java"));
        assertFalse(MtaOutputParser.isWithinProjectPath(root, "C:/work/other/Bean.java"));
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
    }

}
