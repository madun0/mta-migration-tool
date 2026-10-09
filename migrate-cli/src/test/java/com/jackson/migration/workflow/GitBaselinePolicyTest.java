package com.jackson.migration.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for stale Git baseline recovery and checkpoint safety. */
class GitBaselinePolicyTest {

    @Test
    void refreshesStaleBaselineWhenNoCheckpointExistsAndTreeIsClean() {
        MigrationState state = new MigrationState();
        state.baselineGitHead = "old";
        assertTrue(WorkflowService.canRefreshBaseline(state, false, true));
    }

    @Test
    void refusesRefreshWhenCheckpointDirectoryExists() {
        MigrationState state = new MigrationState();
        state.baselineGitHead = "old";
        assertFalse(WorkflowService.canRefreshBaseline(state, true, true));
    }

    @Test
    void refusesRefreshWhenStateReferencesCheckpoint() {
        MigrationState state = new MigrationState();
        state.baselineGitHead = "old";
        state.lastCheckpoint = "cp-001";
        assertFalse(WorkflowService.canRefreshBaseline(state, false, true));
    }

    @Test
    void refusesRefreshWhenApplicationTreeIsDirty() {
        MigrationState state = new MigrationState();
        state.baselineGitHead = "old";
        assertFalse(WorkflowService.canRefreshBaseline(state, false, false));
    }
}
