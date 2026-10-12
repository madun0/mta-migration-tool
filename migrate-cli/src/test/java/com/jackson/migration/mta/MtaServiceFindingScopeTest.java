package com.jackson.migration.mta;

import com.jackson.migration.config.MigrationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that MTA, not the Workbench, owns source-only versus full analysis scope.
 *
 * <p>Once MTA has produced {@code output.yaml}, the Workbench preserves those violation
 * incidents for planning instead of applying another project-root filter. This keeps
 * Workbench findings consistent with the MTA report in both analysis modes.</p>
 */
class MtaServiceFindingScopeTest {

    @TempDir
    Path temp;

    @Test
    void sourceOnlyModePreservesIncidentsReportedByMta() throws Exception {
        assertReportedScopeIsPreserved("source-only");
    }

    @Test
    void fullModePreservesIncidentsReportedByMta() throws Exception {
        assertReportedScopeIsPreserved("full");
    }

    private void assertReportedScopeIsPreserved(String mode) throws Exception {
        Path project = Files.createDirectories(temp.resolve("app-" + mode));
        Path assessment = Files.createDirectories(project.resolve(".migration/assessment"));
        Path appFile = Files.createDirectories(project.resolve("src/main/java/demo")).resolve("Bean.java");
        Files.writeString(appFile, "class Bean {}\n");

        Path dependency = Files.createDirectories(
                temp.resolve("home/.m2/repository/demo/lib/1.0")).resolve("lib.jar");
        Files.writeString(dependency, "jar");

        Files.writeString(assessment.resolve("output.yaml"), """
                - name: test
                  violations:
                    app-rule:
                      category: mandatory
                      incidents:
                        - uri: %s
                          lineNumber: 1
                          message: application incident
                    dependency-rule:
                      category: potential
                      incidents:
                        - uri: %s
                          lineNumber: 1
                          message: dependency incident already reported by MTA
                """.formatted(appFile.toUri(), dependency.toUri()));

        MtaService service = new MtaService();
        service.parser = new MtaOutputParser();
        MigrationConfig config = new MigrationConfig();
        config.mta.mode = mode;

        List<MigrationFinding> findings = service.loadFindings(project, assessment, config);

        assertEquals(
                List.of("app-rule", "dependency-rule"),
                findings.stream().map(MigrationFinding::ruleId).toList());
    }
}
