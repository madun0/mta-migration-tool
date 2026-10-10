package com.jackson.migration.workflow;

import com.jackson.migration.catalog.CatalogService;
import com.jackson.migration.catalog.MigrationCatalog;
import com.jackson.migration.config.ConfigService;
import com.jackson.migration.config.MigrationConfig;
import com.jackson.migration.mta.MigrationFinding;
import com.jackson.migration.mta.MtaService;
import com.jackson.migration.planning.MigrationPlan;
import com.jackson.migration.planning.MigrationPlanner;
import com.jackson.migration.reporting.ReportService;
import com.jackson.migration.rewrite.RewriteService;
import com.jackson.migration.validation.ValidationResult;
import com.jackson.migration.validation.ValidationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordinates initialization, MTA assessment, planning, phased OpenRewrite execution, checkpoints, resume/rollback, and final verification.
 */
@ApplicationScoped
public class WorkflowService {
    @Inject ConfigService configs;
    @Inject CatalogService catalogs;
    @Inject MtaService mta;
    @Inject MigrationPlanner planner;
    @Inject RewriteService rewrite;
    @Inject ValidationService validation;
    @Inject StateStore states;
    @Inject CheckpointService checkpoints;
    @Inject GitService git;
    @Inject ReportService reports;

    /**
     * Initializes a project for migration by creating {@code .migration}, generating a default
     * {@code migration.yaml}, and excluding workbench artifacts from Git status when possible.
     *
     * @param project application project root
     * @throws Exception if initialization fails
     */
    public void init(Path project) throws Exception {
        Files.createDirectories(project.resolve(".migration"));
        configs.createDefault(project);
        excludeMigrationDirectory(project);
        System.out.println("Created migration.yaml");
        System.out.println("Added .migration/ to .git/info/exclude when available.");
        System.out.println("Review migration.yaml, commit it, then run: migrate doctor --project .");
    }

    /**
     * Runs the baseline MTA assessment using the project's migration configuration.
     *
     * @param project application project root
     * @return normalized MTA findings
     * @throws Exception if configuration, MTA execution, or result persistence fails
     */
    public List<MigrationFinding> assess(Path project) throws Exception {
        MigrationConfig config = configs.load(project);
        Path output = project.resolve(".migration/assessment");
        return mta.assess(project, output, config);
    }

    /**
     * Builds and persists the ordered migration plan from baseline MTA findings and the selected
     * profile in {@code migration.yaml}.
     *
     * @param project application project root
     * @return generated migration plan
     * @throws Exception if assessment output is missing or planning/report generation fails
     */
    public MigrationPlan plan(Path project) throws Exception {
        MigrationConfig config = configs.load(project);
        MigrationCatalog catalog = catalogs.load();
        List<MigrationFinding> findings = mta.loadFindings(project, project.resolve(".migration/assessment"), config);
        MigrationPlan plan = planner.build(catalog, config.profile, findings);
        planner.save(plan, project.resolve(".migration/plan.json"));
        reports.writePlan(project, plan);
        System.out.println("Plan created: " + project.resolve(".migration/MIGRATION-PLAN.md"));
        return plan;
    }

    /**
     * Executes pending migration families in phase order. Each automatic/assisted family receives
     * a checkpoint, optional OpenRewrite dry run, transformation, and compile gate. Manual families
     * are converted into TODO work and cleanup is gated until the developer resumes after review.
     *
     * @param project application project root
     * @param phaseFilter optional phase name; {@code null}/blank executes the profile plan
     * @param familyFilter optional family key; when set only that family is considered
     * @throws Exception if configuration, checkpointing, rewrite, Git, or validation operations fail
     */
    public void migrate(Path project, String phaseFilter, String familyFilter) throws Exception {
        MigrationConfig config = configs.load(project);
        MigrationCatalog catalog = catalogs.load();
        MigrationPlan plan = loadOrPlan(project);
        refreshRuntimePolicies(plan, catalog);
        MigrationState state = states.load(project).orElseGet(MigrationState::new);
        ensureGitBaseline(project, plan, state);
        boolean reviewConfirmed = "REVIEW_CONFIRMED".equals(state.status);

        List<MigrationPlan.PhasePlan> phases = select(plan, phaseFilter, familyFilter, catalog);
        for (MigrationPlan.PhasePlan phase : phases) {
            if ("CLEANUP".equals(phase.name)
                    && !state.manualReviewFamilies.isEmpty()
                    && !reviewConfirmed) {
                state.status = "REVIEW_REQUIRED";
                state.currentPhase = "CLEANUP";
                state.currentFamily = null;
                states.save(project, state);
                System.out.println("Manual review is required before cleanup. Complete .migration/TODO.md, then run 'migrate resume'.");
                return;
            }
            for (MigrationPlan.Task task : phase.tasks) {
                if (state.completedFamilies.contains(task.family)) continue;
                state.currentPhase = phase.name;
                state.currentFamily = task.family;
                state.status = "IN_PROGRESS";
                states.save(project, state);

                if ("MANUAL".equalsIgnoreCase(task.automation) || task.recipes.isEmpty()) {
                    if (!state.manualReviewFamilies.contains(task.family)) state.manualReviewFamilies.add(task.family);
                    reports.writeTodo(project, task, "This family requires a developer/architect decision; no automatic transformation is applied.");
                    state.completedFamilies.add(task.family);
                    states.save(project, state);
                    System.out.println("[MANUAL] " + task.family + " -> added to .migration/TODO.md");
                    continue;
                }

                Checkpoint cp = checkpoints.create(project, state, phase.name, task.family);
                System.out.println("[" + phase.name + "] " + task.family + " - checkpoint " + cp.id);
                try {
                    rewrite.apply(project, catalog, config, task.recipes);
                    if (config.validation.compileAfterFamily && task.compileAfterApply) validation.compile(project);
                    checkpoints.complete(project, cp);
                    state.lastError = null;
                    state.completedFamilies.add(task.family);
                    if ("ASSISTED".equalsIgnoreCase(task.automation)) {
                        if (!state.manualReviewFamilies.contains(task.family)) state.manualReviewFamilies.add(task.family);
                        reports.writeTodo(project, task, "Automatic changes were applied, but this family requires review before final cleanup.");
                    }
                    states.save(project, state);
                    System.out.println("[OK] " + task.family);
                } catch (Exception e) {
                    checkpoints.complete(project, cp);
                    state.status = "FAILED";
                    state.lastError = e.getMessage();
                    states.save(project, state);
                    System.err.println("[FAILED] " + task.family + ": " + e.getMessage());
                    System.err.println("Fix the problem and run 'migrate resume', or run 'migrate rollback'.");
                    return;
                }
            }
        }
        state.status = state.manualReviewFamilies.isEmpty() || reviewConfirmed ? "MIGRATED" : "REVIEW_REQUIRED";
        state.lastError = null;
        state.currentFamily = null;
        state.currentPhase = null;
        states.save(project, state);
        System.out.println("Migration pass complete. Status: " + state.status);
    }

    /**
     * Continues a persisted migration. When the workflow is waiting for manual review, invoking
     * resume records review confirmation and permits post-review cleanup recipes to run.
     *
     * @param project application project root
     * @throws Exception if no migration state exists or execution cannot continue
     */
    public void resume(Path project) throws Exception {
        MigrationState state = states.load(project).orElseThrow(() -> new IllegalStateException("No migration state exists."));
        if ("REVIEW_REQUIRED".equals(state.status)) {
            state.status = "REVIEW_CONFIRMED";
            states.save(project, state);
        }
        migrate(project, null, null);
    }

    /**
     * Rolls the migration back to the requested checkpoint, or to the latest checkpoint when no
     * identifier is supplied.
     *
     * @param project application project root
     * @param checkpointId optional checkpoint identifier
     * @throws Exception if checkpoint restoration is unsafe or fails
     */
    public void rollback(Path project, String checkpointId) throws Exception {
        String id = checkpointId == null || checkpointId.isBlank() ? checkpoints.latestId(project) : checkpointId;
        checkpoints.rollback(project, id);
        System.out.println("Rolled back to " + id + ". Run 'migrate resume' when ready.");
    }

    /**
     * Clears persisted run-specific migration artifacts so the current Git commit can be assessed
     * and migrated as a new run. The user-owned {@code migration.yaml} configuration is preserved.
     * A clean application working tree is required because old rollback checkpoints are discarded.
     *
     * @param project application project root
     * @throws Exception if the working tree is dirty or run artifacts cannot be removed
     */
    public void restart(Path project) throws Exception {
        if (!git.isClean(project)) {
            throw new IllegalStateException(
                    "Working tree must be clean before restarting migration. Commit or stash application changes first.");
        }

        Path migration = project.resolve(".migration");
        deleteTree(migration.resolve("checkpoints"));
        deleteTree(migration.resolve("assessment"));
        deleteTree(migration.resolve("verification"));
        Files.deleteIfExists(migration.resolve("state.json"));
        Files.deleteIfExists(migration.resolve("plan.json"));
        Files.deleteIfExists(migration.resolve("MIGRATION-PLAN.md"));
        Files.deleteIfExists(migration.resolve("MIGRATION-REPORT.md"));
        Files.deleteIfExists(migration.resolve("TODO.md"));

        System.out.println("Migration run state cleared for current Git HEAD " + abbreviate(git.head(project)) + ".");
        System.out.println("migration.yaml was preserved. Run: migrate assess --project . && migrate plan --project . && migrate migrate --project .");
    }

    /** Deletes a Workbench-owned directory tree when it exists. */
    private void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /**
     * Performs final compilation/tests, residual forbidden-pattern scanning, a second MTA analysis,
     * and final report generation.
     *
     * @param project application project root
     * @throws Exception if build, MTA, state, or report operations fail
     */
    public void verify(Path project) throws Exception {
        MigrationConfig config = configs.load(project);
        MigrationCatalog catalog = catalogs.load();
        MigrationState state = states.load(project)
                .orElseThrow(() -> new IllegalStateException("No migration state exists. Run migrate first."));

        try {
            validation.compile(project);
            if (config.validation.runTestsOnVerify) validation.test(project);
            state.lastError = null;
        } catch (Exception e) {
            state.status = "FAILED";
            state.lastError = e.getMessage();
            states.save(project, state);
            throw e;
        }

        Map<String, List<String>> automaticResiduals = new LinkedHashMap<>();
        Map<String, List<String>> reviewResiduals = new LinkedHashMap<>();

        for (String family : state.completedFamilies) {
            MigrationCatalog.FamilySpec spec = catalog.families.get(family);
            if (spec == null || spec.forbiddenPatterns == null || spec.forbiddenPatterns.isEmpty()) continue;

            ValidationResult familyResidual = validation.forbiddenPatterns(project, spec.forbiddenPatterns);
            if (familyResidual.success) continue;

            if ("AUTOMATIC".equalsIgnoreCase(spec.automation)) {
                automaticResiduals.put(family, List.copyOf(familyResidual.errors));
            } else {
                reviewResiduals.put(family, List.copyOf(familyResidual.errors));
            }
        }

        List<MigrationFinding> before = mta.loadFindings(project, project.resolve(".migration/assessment"), config);
        List<MigrationFinding> after = mta.assess(project, project.resolve(".migration/verification"), config);

        FindingResidualClassifier.Result findingResiduals = FindingResidualClassifier.classify(catalog, after);
        mergeResiduals(automaticResiduals, findingResiduals.automatic());
        mergeResiduals(reviewResiduals, findingResiduals.review());

        printResiduals("Blocking automatic migration residuals", automaticResiduals);
        printResiduals("Outstanding assisted/manual review residuals", reviewResiduals);

        for (String family : reviewResiduals.keySet()) {
            if (!state.manualReviewFamilies.contains(family)) state.manualReviewFamilies.add(family);
        }

        if (!automaticResiduals.isEmpty()) {
            state.status = "FAILED";
            state.lastError = "Automatic migration residuals remain: "
                    + String.join(", ", automaticResiduals.keySet());
        } else if (!reviewResiduals.isEmpty()) {
            state.status = "REVIEW_REQUIRED";
            state.lastError = null;
        } else {
            state.status = "VERIFIED";
            state.lastError = null;
        }

        states.save(project, state);
        reports.writeFinal(project, state, before, after, automaticResiduals, reviewResiduals, config.validation.runTestsOnVerify);
        System.out.println("Verification status: " + state.status);
        System.out.println("Verification complete. Report: " + project.resolve(".migration/MIGRATION-REPORT.md"));

        if ("FAILED".equals(state.status)) {
            throw new IllegalStateException(state.lastError);
        }
    }

    /**
     * Establishes or validates the Git baseline used by checkpoint/rollback safety. A stale baseline
     * may be refreshed only when no Workbench checkpoint exists and the application working tree
     * is clean; once a checkpoint exists, changing HEAD would invalidate rollback semantics.
     */
    private void ensureGitBaseline(Path project, MigrationPlan plan, MigrationState state) throws Exception {
        String currentHead = git.head(project);
        boolean clean = git.isClean(project);

        if (state.baselineGitHead == null || state.baselineGitHead.isBlank()) {
            if (!clean) {
                throw new IllegalStateException(
                        "Working tree must be clean before the first migration. Commit or stash changes first.");
            }
            state.baselineGitHead = currentHead;
            state.profile = plan.profile;
            state.status = "IN_PROGRESS";
            states.save(project, state);
            return;
        }

        if (currentHead.equals(state.baselineGitHead)) return;

        boolean hasCheckpoints = checkpoints.hasAny(project);
        if (canRefreshBaseline(state, hasCheckpoints, clean)) {
            String previousHead = state.baselineGitHead;
            state.baselineGitHead = currentHead;
            state.profile = plan.profile;
            state.status = "IN_PROGRESS";
            state.currentPhase = null;
            state.currentFamily = null;
            state.lastError = null;
            states.save(project, state);
            System.out.println("Git HEAD changed before any checkpointed migration work; refreshed baseline from "
                    + abbreviate(previousHead) + " to " + abbreviate(currentHead) + ".");
            return;
        }

        throw new IllegalStateException(
                "Git HEAD changed since this migration run started (baseline "
                        + abbreviate(state.baselineGitHead) + ", current " + abbreviate(currentHead) + "). "
                        + "Existing checkpointed state cannot be rebound safely. "
                        + "To start a fresh migration from the current commit, run 'migrate restart --project .'. "
                        + "To resume or rollback this run, restore the repository to the baseline commit first.");
    }

    /**
     * Returns whether a stale baseline can be rebound without invalidating a rollback checkpoint.
     */
    static boolean canRefreshBaseline(MigrationState state, boolean hasCheckpoints, boolean cleanWorkingTree) {
        return cleanWorkingTree
                && !hasCheckpoints
                && (state.lastCheckpoint == null || state.lastCheckpoint.isBlank());
    }

    /** Shortens a Git object name for console diagnostics without changing stored state. */
    private static String abbreviate(String commit) {
        if (commit == null) return "<none>";
        return commit.length() <= 12 ? commit : commit.substring(0, 12);
    }

    /** Merges residual details without discarding forbidden-pattern or MTA evidence already collected. */
    private void mergeResiduals(Map<String, List<String>> destination, Map<String, List<String>> source) {
        source.forEach((family, errors) -> destination
                .computeIfAbsent(family, ignored -> new java.util.ArrayList<>())
                .addAll(errors));
    }

    /** Prints residual matches grouped by the migration family that owns the policy. */
    private void printResiduals(String heading, Map<String, List<String>> residuals) {
        if (residuals.isEmpty()) return;
        System.out.println(heading + ":");
        residuals.forEach((family, errors) -> {
            System.out.println("  " + family + ":");
            errors.forEach(error -> System.out.println("    - " + error));
        });
    }


    /**
     * Refreshes mutable execution policy from the current catalog for an existing persisted plan.
     * Assessment evidence (finding counts, matched rule IDs, and affected files) remains untouched,
     * while catalog corrections made after planning can safely affect resume behavior.
     *
     * @param plan persisted migration plan
     * @param catalog current migration catalog
     */
    private void refreshRuntimePolicies(MigrationPlan plan, MigrationCatalog catalog) {
        for (MigrationPlan.PhasePlan phase : plan.phases) {
            for (MigrationPlan.Task task : phase.tasks) {
                String familyKey = task.triggeredBy != null && !task.triggeredBy.isBlank()
                        ? task.triggeredBy
                        : task.family;
                MigrationCatalog.FamilySpec spec = catalog.families.get(familyKey);
                if (spec == null) continue;

                if (task.triggeredBy != null && !task.triggeredBy.isBlank()) {
                    task.automation = "AUTOMATIC";
                    task.recipes.clear();
                    if (spec.cleanupRecipes != null) task.recipes.addAll(spec.cleanupRecipes);
                    task.compileAfterApply = true;
                } else {
                    task.description = spec.description;
                    task.automation = spec.automation;
                    task.recipes.clear();
                    if (spec.recipes != null) task.recipes.addAll(spec.recipes);
                    task.cleanupRecipes.clear();
                    if (spec.cleanupRecipes != null) task.cleanupRecipes.addAll(spec.cleanupRecipes);
                    task.compileAfterApply = spec.compileAfterApply;
                }

                task.guide = spec.guide;
                task.reviewChecklist.clear();
                if (spec.reviewChecklist != null) task.reviewChecklist.addAll(spec.reviewChecklist);
                task.forbiddenPatterns.clear();
                if (spec.forbiddenPatterns != null) task.forbiddenPatterns.addAll(spec.forbiddenPatterns);
            }
        }
    }

    /** Loads an existing plan, creating one from the baseline assessment when necessary. */
    private MigrationPlan loadOrPlan(Path project) throws Exception {
        Path file = project.resolve(".migration/plan.json");
        return Files.exists(file) ? planner.load(file) : plan(project);
    }

    /** Resolves optional phase/family filters into the exact phase plan list to execute. */
    private List<MigrationPlan.PhasePlan> select(MigrationPlan plan, String phase, String family, MigrationCatalog catalog) {
        if (family != null && !family.isBlank()) {
            MigrationCatalog.FamilySpec spec = catalog.families.get(family);
            if (spec == null) throw new IllegalArgumentException("Unknown family: " + family);
            MigrationPlan.Task task = new MigrationPlan.Task();
            task.family = family;
            task.description = spec.description;
            task.automation = spec.automation;
            task.recipes.addAll(spec.recipes);
            task.cleanupRecipes.addAll(spec.cleanupRecipes);
            task.guide = spec.guide;
            task.reviewChecklist.addAll(spec.reviewChecklist);
            task.compileAfterApply = spec.compileAfterApply;
            task.forbiddenPatterns.addAll(spec.forbiddenPatterns);
            MigrationPlan.PhasePlan p = new MigrationPlan.PhasePlan();
            p.name = spec.phase;
            p.tasks.add(task);
            return List.of(p);
        }
        if (phase == null || phase.isBlank()) return plan.phases;
        return plan.phases.stream().filter(p -> phase.equalsIgnoreCase(p.name)).toList();
    }

    /** Adds {@code .migration/} to the repository-local Git exclude file when one is available. */
    private void excludeMigrationDirectory(Path project) {
        try {
            Path exclude = project.resolve(".git/info/exclude");
            if (!Files.exists(exclude.getParent())) return;
            String existing = Files.exists(exclude) ? Files.readString(exclude) : "";
            if (existing.lines().noneMatch(l -> l.trim().equals(".migration/"))) {
                String separator = existing.isEmpty() || existing.endsWith("\n") ? "" : "\n";
                Files.writeString(exclude, existing + separator + ".migration/\n");
            }
        } catch (IOException ignored) {
            // .migration can still be filtered by GitService even if info/exclude cannot be updated.
        }
    }
}
