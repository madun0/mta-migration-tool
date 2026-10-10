package com.jackson.migration.mta;

import com.jackson.migration.config.MigrationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies persisted findings honor migration.yaml mode all the way into planning/verification loads. */
class MtaServiceFindingScopeTest {

    @TempDir
    Path temp;

    @Test
    void fullModeRecoversExternalDependencyFindingFromRawOutputEvenWhenLegacyJsonWasFiltered() throws Exception {
        Path project = Files.createDirectories(temp.resolve("app"));
        Path assessment = Files.createDirectories(project.resolve(".migration/assessment"));
        Path appFile = Files.createDirectories(project.resolve("src/main/java/demo")).resolve("Bean.java");
        Files.writeString(appFile, "class Bean {}\n");
        Path dependency = Files.createDirectories(temp.resolve("home/.m2/repository/demo/lib/1.0")).resolve("lib.jar");
        Files.writeString(dependency, "jar");

        Files.writeString(assessment.resolve("output.yaml"), """
                - name: full-mode-test
                  violations:
                    app-rule:
                      category: mandatory
                      incidents:
                        - uri: %s
                          lineNumber: 1
                          message: app
                    dependency-rule:
                      category: potential
                      incidents:
                        - uri: %s
                          lineNumber: 1
                          message: dependency
                """.formatted(appFile.toUri(), dependency.toUri()));

        // Simulate the v19 bug: findings.json persisted only the in-project incident.
        Files.writeString(assessment.resolve("findings.json"), """
                [{"ruleId":"app-rule","message":"app","file":"%s","line":1,"category":"mandatory"}]
                """.formatted(appFile.toUri()));

        MtaService service = new MtaService();
        service.parser = new MtaOutputParser();
        MigrationConfig config = new MigrationConfig();
        config.mta.mode = "full";

        List<MigrationFinding> findings = service.loadFindings(project, assessment, config);

        assertEquals(List.of("app-rule", "dependency-rule"), findings.stream().map(MigrationFinding::ruleId).toList());
    }

    @Test
    void sourceOnlyModeKeepsApplicationFindingAndDropsExternalDependencyFinding() throws Exception {
        Path project = Files.createDirectories(temp.resolve("source-only-app"));
        Path assessment = Files.createDirectories(project.resolve(".migration/assessment"));
        Path appFile = Files.createDirectories(project.resolve("src/main/java/demo")).resolve("Bean.java");
        Files.writeString(appFile, "class Bean {}\n");
        Path dependency = Files.createDirectories(temp.resolve("source-only-home/.m2/repository/demo/lib/1.0")).resolve("lib.jar");
        Files.writeString(dependency, "jar");

        Files.writeString(assessment.resolve("output.yaml"), """
                - name: source-only-test
                  violations:
                    app-rule:
                      category: mandatory
                      incidents:
                        - uri: %s
                          lineNumber: 1
                          message: app
                    dependency-rule:
                      category: potential
                      incidents:
                        - uri: %s
                          lineNumber: 1
                          message: dependency
                """.formatted(appFile.toUri(), dependency.toUri()));

        MtaService service = new MtaService();
        service.parser = new MtaOutputParser();
        MigrationConfig config = new MigrationConfig();
        config.mta.mode = "source-only";

        List<MigrationFinding> findings = service.loadFindings(project, assessment, config);

        assertEquals(List.of("app-rule"), findings.stream().map(MigrationFinding::ruleId).toList());
    }
}
