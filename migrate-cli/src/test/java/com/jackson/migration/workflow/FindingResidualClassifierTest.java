package com.jackson.migration.workflow;

import com.jackson.migration.catalog.MigrationCatalog;
import com.jackson.migration.mta.MigrationFinding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that Workbench automation level, not raw MTA category, determines verification severity. */
class FindingResidualClassifierTest {

    @Test
    void mandatoryMtaFindingOwnedByAssistedFamilyIsReviewOnly() {
        MigrationCatalog catalog = new MigrationCatalog();
        MigrationCatalog.FamilySpec schedule = new MigrationCatalog.FamilySpec();
        schedule.automation = "ASSISTED";
        schedule.mtaRuleIds.add("primefaces-schedule-0001");
        catalog.families.put("schedule", schedule);

        var result = FindingResidualClassifier.classify(catalog, List.of(
                new MigrationFinding("primefaces-schedule-0001", "Review schedule", "advanced.xhtml", 33, "mandatory")));

        assertTrue(result.automatic().isEmpty());
        assertEquals(1, result.review().get("schedule").size());
    }

    @Test
    void automaticFamilyResidualIsBlockingEvenWhenMtaCategoryIsPotential() {
        MigrationCatalog catalog = new MigrationCatalog();
        MigrationCatalog.FamilySpec repeat = new MigrationCatalog.FamilySpec();
        repeat.automation = "AUTOMATIC";
        repeat.mtaRuleIds.add("primefaces-repeat-0001");
        catalog.families.put("repeat", repeat);

        var result = FindingResidualClassifier.classify(catalog, List.of(
                new MigrationFinding("primefaces-repeat-0001", "Legacy repeat remains", "data.xhtml", 10, "potential")));

        assertEquals(1, result.automatic().get("repeat").size());
        assertTrue(result.review().isEmpty());
    }

    @Test
    void unmappedMtaFindingIsReviewOnly() {
        MigrationCatalog catalog = new MigrationCatalog();
        var result = FindingResidualClassifier.classify(catalog, List.of(
                new MigrationFinding("third-party-advisory", "Review", "pom.xml", 1, "mandatory")));

        assertTrue(result.automatic().isEmpty());
        assertEquals(1, result.review().get("unmapped-mta-findings").size());
    }
}
