package com.jackson.migration.workflow;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies persistence and recovery of migration workflow state.
 */
class StateStoreTest {
    @Test
    void roundTripsState() throws Exception {
        Path project = Files.createTempDirectory("migrate-state");
        StateStore store = new StateStore();
        MigrationState state = new MigrationState();
        state.profile = "standard";
        state.completedFamilies.add("request-context");
        store.save(project, state);
        MigrationState loaded = store.load(project).orElseThrow();
        assertEquals("standard", loaded.profile);
        assertEquals(List.of("request-context"), loaded.completedFamilies);
    }
}
