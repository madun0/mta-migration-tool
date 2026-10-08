package com.jackson.migration.reporting;

import com.jackson.migration.planning.MigrationPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that developer-facing reports remain portable and distinguish cleanup from MTA evidence. */
class ReportServiceTest {

    @TempDir
    Path project;

    @Test
    void rendersProjectRelativePathsAndCleanupTrigger() throws Exception {
        MigrationPlan plan = new MigrationPlan();
        plan.profile = "standard";

        MigrationPlan.PhasePlan foundation = new MigrationPlan.PhasePlan();
        foundation.name = "FOUNDATION";
        MigrationPlan.Task task = new MigrationPlan.Task();
        task.family = "faces-managed-beans";
        task.automation = "ASSISTED";
        task.findingCount = 1;
        task.affectedFiles.add(project.resolve("src/main/java/example/Bean.java").toUri().toString());
        task.recipes.add("com.example.Recipe");
        task.reviewChecklist.add("Verify bean scope");
        foundation.tasks.add(task);
        plan.phases.add(foundation);

        MigrationPlan.PhasePlan cleanup = new MigrationPlan.PhasePlan();
        cleanup.name = "CLEANUP";
        MigrationPlan.Task cleanupTask = new MigrationPlan.Task();
        cleanupTask.family = "calendar-cleanup";
        cleanupTask.automation = "AUTOMATIC";
        cleanupTask.triggeredBy = "calendar";
        cleanupTask.recipes.add("com.example.Cleanup");
        cleanup.tasks.add(cleanupTask);
        plan.phases.add(cleanup);

        new ReportService().writePlan(project, plan);
        String report = Files.readString(project.resolve(".migration/MIGRATION-PLAN.md"));

        assertTrue(report.contains("`src/main/java/example/Bean.java`"));
        assertFalse(report.contains(project.toUri().toString()));
        assertTrue(report.contains("Developer verification:"));
        assertTrue(report.contains("Triggered by: `calendar`"));
        assertFalse(report.contains("calendar-cleanup** — AUTOMATIC — findings:"));
    }

    @Test
    void rendersVerificationResidualsByFamily() throws Exception {
        com.jackson.migration.workflow.MigrationState state = new com.jackson.migration.workflow.MigrationState();
        state.status = "REVIEW_REQUIRED";
        state.completedFamilies.add("request-context");
        state.manualReviewFamilies.add("request-context");

        new ReportService().writeFinal(
                project,
                state,
                java.util.List.of(new com.jackson.migration.mta.MigrationFinding("rule", "message", "file", 1, "mandatory")),
                java.util.List.of(new com.jackson.migration.mta.MigrationFinding(
                        "advisory-0001", "Review deployment descriptor",
                        project.resolve("src/main/webapp/WEB-INF/web.xml").toString(), 7, "optional")),
                java.util.Map.of(),
                java.util.Map.of("request-context", java.util.List.of("src/main/java/Bean.java:10 contains RequestContext")));

        String report = Files.readString(project.resolve(".migration/MIGRATION-REPORT.md"));
        assertTrue(report.contains("Status: **REVIEW_REQUIRED**"));
        assertTrue(report.contains("Assisted/manual residual families: **1**"));
        assertTrue(report.contains("### request-context"));
        assertTrue(report.contains("src/main/java/Bean.java:10 contains RequestContext"));
        assertTrue(report.contains("Remaining MTA findings: **1**"));
        assertTrue(report.contains("`advisory-0001` [optional]"));
        assertTrue(report.contains("`src/main/webapp/WEB-INF/web.xml:7`"));
        assertTrue(report.contains("Review deployment descriptor"));
        assertTrue(report.contains("## Families that required manual review during migration"));
        assertFalse(report.contains("## Manual review families"));
    }
}
