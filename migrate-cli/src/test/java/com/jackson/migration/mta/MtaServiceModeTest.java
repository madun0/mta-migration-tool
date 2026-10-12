package com.jackson.migration.mta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests YAML-only MTA mode selection and related assessment configuration helpers. */
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
    void formatsOneMtaStyleFindingsCount() {
        assertEquals("MTA findings: 48", MtaService.formatFindingCount(48));
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

    @Test
    void loadsPersistedMtaIncidentCountWithoutUsingNormalizedFindingSize() throws Exception {
        Path output = Files.createDirectories(temp.resolve("assessment"));
        Files.writeString(output.resolve("assessment-summary.json"), "{\"mtaFindings\": 73}\n");

        MtaService service = new MtaService();
        service.parser = new MtaOutputParser();

        assertEquals(73, service.loadReportedIncidentCount(output));
    }

}
