package com.jackson.migration.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertEquals(List.of("eap7", "jsf2", "primefaces6"), config.mta.sources);
        assertEquals(List.of("eap8", "faces4", "primefaces16"), config.mta.targets);
    }

    @Test
    void explicitTargetsDoNotMergeLegacyScalarAndPreserveValidTargets() throws Exception {
        Files.writeString(temp.resolve("migration.yaml"), """
                profile: standard
                mta:
                  target: eap8
                  targets:
                    - eap82
                    - faces4
                    - primefaces16
                """);

        ConfigService service = new ConfigService();
        MigrationConfig config = service.load(temp);

        assertEquals(List.of("eap82", "faces4", "primefaces16"), config.mta.targets);
    }
}
