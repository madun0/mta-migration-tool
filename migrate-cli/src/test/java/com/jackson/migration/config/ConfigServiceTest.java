package com.jackson.migration.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies native MTA scope defaults and the current migration.yaml schema. */
class ConfigServiceTest {

    @TempDir
    Path temp;

    @Test
    void standardDefaultsUseNativeMtaTargets() throws Exception {
        MigrationConfig config = new ConfigService().load(temp);

        assertEquals(List.of("eap7"), config.mta.sources);
        assertEquals(List.of("eap8", "openjdk21", "jakarta-ee"), config.mta.targets);
    }

    @Test
    void explicitNativeSelectorsArePreservedExactly() throws Exception {
        Files.writeString(temp.resolve("migration.yaml"), """
                profile: standard
                mta:
                  sources:
                    - eap7
                  targets:
                    - eap8
                    - openjdk21
                    - jakarta-ee
                """);

        MigrationConfig config = new ConfigService().load(temp);

        assertEquals(List.of("eap7"), config.mta.sources);
        assertEquals(List.of("eap8", "openjdk21", "jakarta-ee"), config.mta.targets);
    }

    @Test
    void selectorNormalizationOnlyTrimsAndDeduplicates() throws Exception {
        Files.writeString(temp.resolve("migration.yaml"), """
                profile: standard
                mta:
                  sources:
                    - " eap7 "
                    - eap7
                  targets:
                    - " eap8 "
                    - eap8
                    - openjdk21
                """);

        MigrationConfig config = new ConfigService().load(temp);

        assertEquals(List.of("eap7"), config.mta.sources);
        assertEquals(List.of("eap8", "openjdk21"), config.mta.targets);
    }

    @Test
    void legacyScalarTargetIsRejected() throws Exception {
        Files.writeString(temp.resolve("migration.yaml"), """
                profile: standard
                mta:
                  target: eap8
                """);

        assertThrows(IOException.class, () -> new ConfigService().load(temp));
    }

    @Test
    void legacyStrictProjectScopeIsRejected() throws Exception {
        Files.writeString(temp.resolve("migration.yaml"), """
                profile: standard
                mta:
                  mode: full
                  strictProjectScope: true
                """);

        assertThrows(IOException.class, () -> new ConfigService().load(temp));
    }

    @Test
    void generatedConfigurationUsesOnlyCurrentMtaSchema() throws Exception {
        Path project = Files.createDirectories(temp.resolve("generated"));
        new ConfigService().createDefault(project);

        String yaml = Files.readString(project.resolve("migration.yaml"));

        assertTrue(yaml.contains("sources:"));
        assertTrue(yaml.contains("targets:"));
        assertTrue(yaml.contains("mode"));
        assertTrue(yaml.contains("source-only"));
        assertTrue(yaml.contains("eap7"));
        assertTrue(yaml.contains("eap8"));
        assertTrue(yaml.contains("openjdk21"));
        assertTrue(yaml.contains("jakarta-ee"));
        assertFalse(yaml.contains("\n  target:"));
        assertFalse(yaml.contains("strictProjectScope"));
    }

    @Test
    void fullModeAndOptionalMavenSettingsLoadFromYaml() throws Exception {
        Files.writeString(temp.resolve("migration.yaml"), """
                profile: standard
                mta:
                  mode: full
                  analyzeKnownLibraries: true
                  mavenSettings: .mvn/custom-settings.xml
                  disableMavenSearch: true
                """);

        MigrationConfig config = new ConfigService().load(temp);

        assertEquals("full", config.mta.mode);
        assertTrue(config.mta.analyzeKnownLibraries);
        assertEquals(".mvn/custom-settings.xml", config.mta.mavenSettings);
        assertTrue(config.mta.disableMavenSearch);
    }
}
