package com.jackson.migration.mta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Regression tests for the Workbench MTA source-boundary policy. */
class MtaServiceModeTest {

    @Test
    void strictProjectScopeForcesSourceOnlyEvenWhenLegacyConfigRequestsFull() {
        assertEquals("source-only", MtaService.effectiveAnalysisMode("full", true));
    }

    @Test
    void strictProjectScopeUsesSourceOnlyForBlankMode() {
        assertEquals("source-only", MtaService.effectiveAnalysisMode("", true));
        assertEquals("source-only", MtaService.effectiveAnalysisMode(null, true));
    }

    @Test
    void dependencyAwareFullModeRequiresExplicitScopeOptOut() {
        assertEquals("full", MtaService.effectiveAnalysisMode("full", false));
    }

    @Test
    void sourceOnlyRemainsAvailableWhenStrictScopeIsDisabled() {
        assertEquals("source-only", MtaService.effectiveAnalysisMode("source-only", false));
    }

    @Test
    void invalidModeIsRejectedWhenNotOverriddenByStrictScope() {
        assertThrows(IllegalArgumentException.class,
                () -> MtaService.effectiveAnalysisMode("everything", false));
    }
}
