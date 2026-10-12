package com.jackson.migration.mta;

import com.jackson.migration.config.MigrationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies that planning findings are separate from the exact MTA incident mirror. */
class MtaServiceFindingPersistenceTest {

    @TempDir
    Path temp;

    @Test
    void planningLoaderUsesPlanningArtifactWithoutReapplyingModeScope() throws Exception {
        Path project = Files.createDirectories(temp.resolve("app"));
        Path assessment = Files.createDirectories(project.resolve(".migration/assessment"));

        Files.writeString(assessment.resolve("planning-findings.json"), """
                [
                  {"ruleId":"app-rule","message":"app","file":"file:///app/A.java","line":1,"category":"mandatory"},
                  {"ruleId":"dependency-rule","message":"dependency","file":"file:///repo/lib.jar","line":1,"category":"potential"}
                ]
                """);

        MtaService service = new MtaService();
        service.parser = new MtaOutputParser();
        MigrationConfig config = new MigrationConfig();
        config.mta.mode = "source-only";

        List<MigrationFinding> findings = service.loadFindings(project, assessment, config);

        assertEquals(List.of("app-rule", "dependency-rule"), findings.stream().map(MigrationFinding::ruleId).toList());
    }

    @Test
    void fallbackParsingDeduplicatesForPlanningButDoesNotApplyProjectRootFiltering() throws Exception {
        Path project = Files.createDirectories(temp.resolve("fallback-app"));
        Path assessment = Files.createDirectories(project.resolve(".migration/assessment"));
        Path appFile = Files.createDirectories(project.resolve("src/main/java/demo")).resolve("Bean.java");
        Files.writeString(appFile, "class Bean {}\n");
        Path dependency = Files.createDirectories(temp.resolve("home/.m2/repository/demo/lib/1.0")).resolve("lib.jar");
        Files.writeString(dependency, "jar");

        Files.writeString(assessment.resolve("output.yaml"), """
                - name: test
                  violations:
                    app-rule:
                      category: mandatory
                      incidents:
                        - uri: %s
                          lineNumber: 1
                          message: app
                        - uri: %s
                          lineNumber: 1
                          message: app
                    dependency-rule:
                      category: potential
                      incidents:
                        - uri: %s
                          lineNumber: 1
                          message: dependency
                """.formatted(appFile.toUri(), appFile.toUri(), dependency.toUri()));

        MtaService service = new MtaService();
        service.parser = new MtaOutputParser();
        MigrationConfig config = new MigrationConfig();
        config.mta.mode = "source-only";

        List<MigrationFinding> findings = service.loadFindings(project, assessment, config);

        assertEquals(List.of("app-rule", "dependency-rule"), findings.stream().map(MigrationFinding::ruleId).toList());
    }
}
