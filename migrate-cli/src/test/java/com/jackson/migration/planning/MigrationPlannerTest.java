package com.jackson.migration.planning;

import com.jackson.migration.catalog.MigrationCatalog;
import com.jackson.migration.mta.MigrationFinding;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that MTA findings are consolidated into ordered migration families and that unmapped
 * findings remain visible for manual review.
 */
class MigrationPlannerTest {

    @Test
    void detectedFamilyIsPlanned() {
        MigrationCatalog catalog = catalogWithPhases("CORE");
        MigrationCatalog.FamilySpec family = family("CORE", "AUTOMATIC");
        family.mtaRuleIds = List.of("r1");
        catalog.families.put("request-context", family);

        MigrationPlan plan = new MigrationPlanner().build(
                catalog,
                "standard",
                List.of(new MigrationFinding("r1", "", "A.java", 10, "mandatory")));

        MigrationPlan.Task task = plan.phases.get(0).tasks.get(0);
        assertEquals("request-context", task.family);
        assertEquals(1, task.findingCount);
        assertEquals(List.of("r1"), task.matchedRuleIds);
        assertEquals(List.of("A.java"), task.affectedFiles);
    }

    @Test
    void alwaysOnFamilyIsPlannedWithoutMtaEvidence() {
        MigrationCatalog catalog = catalogWithPhases("PRIMEFACES_CORE");
        MigrationCatalog.FamilySpec family = family("PRIMEFACES_CORE", "ASSISTED");
        family.mtaRuleIds = List.of("primefaces-request-context-0001");
        family.recipes = List.of("com.jackson.mta.primefaces.PrimeFaces62RequestContextMigration");
        family.always = true;
        catalog.families.put("request-context", family);

        MigrationPlan plan = new MigrationPlanner().build(catalog, "standard", List.of());

        assertEquals(1, plan.phases.size());
        assertEquals(1, plan.phases.get(0).tasks.size());
        MigrationPlan.Task task = plan.phases.get(0).tasks.get(0);
        assertEquals("request-context", task.family);
        assertEquals(0, task.findingCount);
        assertEquals(List.of("com.jackson.mta.primefaces.PrimeFaces62RequestContextMigration"), task.recipes);
    }

    @Test
    void prefixRulesAreConsolidatedIntoOneFamily() {
        MigrationCatalog catalog = catalogWithPhases("FOUNDATION");
        MigrationCatalog.FamilySpec family = family("FOUNDATION", "MANUAL");
        family.mtaRulePrefixes = List.of("javaee-to-jakarta-namespaces-");
        catalog.families.put("jakarta-descriptors", family);

        MigrationPlan plan = new MigrationPlanner().build(catalog, "standard", List.of(
                new MigrationFinding("javaee-to-jakarta-namespaces-00001", "", "web.xml", 2, "mandatory"),
                new MigrationFinding("javaee-to-jakarta-namespaces-00017", "", "web.xml", 4, "mandatory")));

        MigrationPlan.Task task = plan.phases.get(0).tasks.get(0);
        assertEquals(2, task.findingCount);
        assertEquals(2, task.matchedRuleIds.size());
        assertEquals(List.of("web.xml"), task.affectedFiles);
    }

    @Test
    void firstMatchingFamilyWinsForOverlappingRules() {
        MigrationCatalog catalog = catalogWithPhases("FOUNDATION");
        MigrationCatalog.FamilySpec specific = family("FOUNDATION", "MANUAL");
        specific.mtaRuleIds = List.of("eap8-faces-00003");
        catalog.families.put("faces-managed-beans", specific);

        MigrationCatalog.FamilySpec broad = family("FOUNDATION", "MANUAL");
        broad.mtaRulePrefixes = List.of("eap8-faces-");
        catalog.families.put("faces-compatibility", broad);

        MigrationPlan plan = new MigrationPlanner().build(catalog, "standard", List.of(
                new MigrationFinding("eap8-faces-00003", "", "Bean.java", 17, "mandatory")));

        assertEquals(1, plan.phases.get(0).tasks.size());
        assertEquals("faces-managed-beans", plan.phases.get(0).tasks.get(0).family);
    }

    @Test
    void unmappedFindingsBecomeVisibleReviewTask() {
        MigrationCatalog catalog = catalogWithPhases("MANUAL_REVIEW");
        MigrationPlan plan = new MigrationPlanner().build(catalog, "standard", List.of(
                new MigrationFinding("unknown-rule-0001", "", "Legacy.java", 3, "potential")));

        MigrationPlan.Task task = plan.phases.get(0).tasks.get(0);
        assertEquals("unmapped-mta-review", task.family);
        assertEquals("MANUAL", task.automation);
        assertEquals(1, task.findingCount);
        assertTrue(task.matchedRuleIds.contains("unknown-rule-0001"));
    }

    @Test
    void cleanupTaskCarriesTriggerWithoutRepeatingFindingEvidence() {
        MigrationCatalog catalog = catalogWithPhases("FOUNDATION", "CLEANUP");
        MigrationCatalog.FamilySpec family = family("FOUNDATION", "ASSISTED");
        family.mtaRuleIds = List.of("calendar-rule");
        family.recipes = List.of("main.recipe");
        family.cleanupRecipes = List.of("cleanup.recipe");
        family.reviewChecklist = List.of("Verify semantic behavior");
        family.compileAfterApply = false;
        catalog.families.put("calendar", family);

        MigrationPlan plan = new MigrationPlanner().build(catalog, "standard", List.of(
                new MigrationFinding("calendar-rule", "", "index.xhtml", 10, "mandatory")));

        MigrationPlan.Task primary = plan.phases.get(0).tasks.get(0);
        MigrationPlan.Task cleanup = plan.phases.get(1).tasks.get(0);

        assertEquals(1, primary.findingCount);
        assertEquals(List.of("Verify semantic behavior"), primary.reviewChecklist);
        assertTrue(!primary.compileAfterApply);
        assertEquals(0, cleanup.findingCount);
        assertEquals("calendar", cleanup.triggeredBy);
        assertTrue(cleanup.compileAfterApply);
        assertTrue(cleanup.matchedRuleIds.isEmpty());
        assertTrue(cleanup.affectedFiles.isEmpty());
    }

    private MigrationCatalog catalogWithPhases(String... phases) {
        MigrationCatalog catalog = new MigrationCatalog();
        MigrationCatalog.ProfileSpec profile = new MigrationCatalog.ProfileSpec();
        profile.phases = List.of(phases);
        catalog.profiles.put("standard", profile);
        return catalog;
    }

    private MigrationCatalog.FamilySpec family(String phase, String automation) {
        MigrationCatalog.FamilySpec family = new MigrationCatalog.FamilySpec();
        family.phase = phase;
        family.automation = automation;
        family.description = "test";
        return family;
    }
}
