package com.jackson.migration.rewrite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Verifies that migration execution never forks the target Maven compile lifecycle before Rewrite runs. */
class RewriteServiceTest {

    @Test
    void usesNonForkingDryRunGoal() {
        assertEquals("dryRunNoFork", RewriteService.rewriteGoal(true));
    }

    @Test
    void usesNonForkingApplyGoal() {
        assertEquals("runNoFork", RewriteService.rewriteGoal(false));
    }
}
