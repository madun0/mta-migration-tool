package com.jackson.migration.mta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regression tests for YAML-only MTA mode selection and mode-aware finding scope. */
class MtaServiceModeTest {

    @TempDir
    Path temp;

    @Test
    void blankModeDefaultsToSourceOnly() {
        assertEquals("source-only", MtaService.effectiveAnalysisMode(""));
        assertEquals("source-only", MtaService.effectiveAnalysisMode(null));
    }

    @Test
    void fullModeIsAcceptedDirectlyFromConfiguration() {
        assertEquals("full", MtaService.effectiveAnalysisMode("full"));
        assertEquals("full", MtaService.effectiveAnalysisMode(" FULL "));
    }

    @Test
    void sourceOnlyModeIsAcceptedDirectlyFromConfiguration() {
        assertEquals("source-only", MtaService.effectiveAnalysisMode("source-only"));
    }

    @Test
    void invalidModeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> MtaService.effectiveAnalysisMode("everything"));
    }

    @Test
    void sourceOnlyDropsExternalDependencyFindingsWhileFullRetainsThem() throws Exception {
        Path project = Files.createDirectories(temp.resolve("app"));
        Path source = Files.createDirectories(project.resolve("src/main/java/demo")).resolve("Bean.java");
        Files.writeString(source, "class Bean {}\n");
        Path dependency = Files.createDirectories(temp.resolve("home/.m2/repository/demo/lib/1.0")).resolve("lib.jar");
        Files.writeString(dependency, "jar");

        List<MigrationFinding> findings = List.of(
                new MigrationFinding("app", "", source.toUri().toString(), 1, "mandatory"),
                new MigrationFinding("dependency", "", dependency.toUri().toString(), 1, "potential"));

        MtaService service = new MtaService();
        service.parser = new MtaOutputParser();

        assertEquals(List.of("app"), service.scopeFindings(findings, project.toRealPath(), "source-only")
                .stream().map(MigrationFinding::ruleId).toList());
        assertEquals(List.of("app", "dependency"), service.scopeFindings(findings, project.toRealPath(), "full")
                .stream().map(MigrationFinding::ruleId).toList());
    }
    @Test
    void resolvesRelativeMavenSettingsAgainstProjectRoot() throws Exception {
        Path project = Files.createDirectories(temp.resolve("settings-app"));
        Path settings = Files.createDirectories(project.resolve(".mvn")).resolve("settings.xml");
        Files.writeString(settings, "<settings/>\n");

        assertEquals(settings.toRealPath(), MtaService.resolveMavenSettings(project.toRealPath(), ".mvn/settings.xml"));
    }

    @Test
    void parsesNativeTechnologyNamesFromMta83ListOutput() {
        String output = """
                Available migration targets:
                  - eap8
                  - openjdk21
                  - jakarta-ee
                  - cloud-readiness
                """;

        var values = MtaService.parseTechnologyNames(output);

        assertTrue(values.contains("eap8"));
        assertTrue(values.contains("openjdk21"));
        assertTrue(values.contains("jakarta-ee"));
    }

}
