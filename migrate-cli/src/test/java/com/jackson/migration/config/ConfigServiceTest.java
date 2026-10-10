package com.jackson.migration.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies profile-aware MTA scope normalization and legacy target compatibility.
 */
class ConfigServiceTest {

    @TempDir
    Path temp;

    @Test
    void standardDefaultsUseCanonicalTargetsWithoutLegacyDuplication() throws Exception {
        ConfigService service = new ConfigService();
        MigrationConfig config = service.load(temp);

        assertEquals(List.of("eap7"), config.mta.sources);
        assertEquals(List.of("eap8", "openjdk21", "jakarta-ee"), config.mta.targets);
    }

    @Test
    void explicitTargetsDoNotMergeLegacyScalarAndPreserveValidTargets() throws Exception {
        Files.writeString(temp.resolve("migration.yaml"), """
                profile: standard
                mta:
                  target: eap8
                  targets:
                    - eap8
                    - openjdk21
                    - jakarta-ee
                """);

        ConfigService service = new ConfigService();
        MigrationConfig config = service.load(temp);

        assertEquals(List.of("eap8", "openjdk21", "jakarta-ee"), config.mta.targets);
    }
    @Test
    void legacyWorkbenchPseudoSelectorsAreTranslatedToNativeMta83Technologies() throws Exception {
        Files.writeString(temp.resolve("migration.yaml"), """
                profile: standard
                mta:
                  sources:
                    - eap7
                    - jsf2
                    - primefaces6
                  targets:
                    - eap82
                    - faces4
                    - primefaces16
                """);

        MigrationConfig config = new ConfigService().load(temp);

        assertEquals(List.of("eap7"), config.mta.sources);
        assertEquals(List.of("eap8", "jakarta-ee", "openjdk21"), config.mta.targets);
    }

    @Test
    void fullModeComesFromMigrationYamlAndLegacyStrictProjectScopeDoesNotOverrideIt() throws Exception {
        Files.writeString(temp.resolve("migration.yaml"), """
                profile: standard
                mta:
                  mode: full
                  strictProjectScope: true
                """);

        MigrationConfig config = new ConfigService().load(temp);

        assertEquals("full", config.mta.mode);
    }

    @Test
    void generatedConfigurationExposesModeButNotLegacyStrictProjectScope() throws Exception {
        Path project = Files.createDirectories(temp.resolve("generated"));
        new ConfigService().createDefault(project);

        String yaml = Files.readString(project.resolve("migration.yaml"));

        assertTrue(yaml.contains("mode"));
        assertTrue(yaml.contains("source-only"));
        assertTrue(yaml.contains("eap7"));
        assertTrue(yaml.contains("eap8"));
        assertTrue(yaml.contains("openjdk21"));
        assertTrue(yaml.contains("jakarta-ee"));
        assertFalse(yaml.contains("jsf2"));
        assertFalse(yaml.contains("primefaces6"));
        assertFalse(yaml.contains("eap82"));
        assertFalse(yaml.contains("faces4"));
        assertFalse(yaml.contains("primefaces16"));
        assertFalse(yaml.contains("strictProjectScope"));
    }

    @Test
    void fullModeDefaultsToKnownLibraryAnalysisAndLoadsOptionalMavenSettings() throws Exception {
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
