package com.jackson.migration.planning;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable ordered migration plan produced from a profile, the migration catalog, and MTA
 * findings.
 */
public class MigrationPlan {

    /** Profile used to build the plan. */
    public String profile;

    /** Ordered phases selected for execution. */
    public List<PhasePlan> phases = new ArrayList<>();

    /**
     * Groups migration tasks that execute within the same ordered migration phase.
     */
    public static class PhasePlan {

        /** Stable phase name, for example {@code PRIMEFACES_CORE}. */
        public String name;

        /** Ordered tasks selected within this phase. */
        public List<Task> tasks = new ArrayList<>();
    }

    /**
     * Represents one migration family selected for execution, review, cleanup, and residual
     * validation.
     */
    public static class Task {

        /** Stable migration family key. */
        public String family;

        /** Human-readable description copied from the migration catalog. */
        public String description;

        /** Automation classification such as AUTOMATIC, ASSISTED, or MANUAL. */
        public String automation;

        /** Number of normalized MTA findings consolidated into this family. */
        public int findingCount;

        /** Distinct MTA rule identifiers that contributed evidence to this task. */
        public List<String> matchedRuleIds = new ArrayList<>();

        /** Distinct source/configuration files affected by the contributing findings. */
        public List<String> affectedFiles = new ArrayList<>();

        /** Concrete OpenRewrite recipe class names to execute before review. */
        public List<String> recipes = new ArrayList<>();

        /** Cleanup recipes eligible after manual review is confirmed. */
        public List<String> cleanupRecipes = new ArrayList<>();

        /** Family-specific migration guide path. */
        public String guide;

        /** Developer verification steps associated with this family. */
        public List<String> reviewChecklist = new ArrayList<>();

        /** Source migration family that activates this follow-up task, when applicable. */
        public String triggeredBy;

        /** Whether to run the compile gate immediately after this transformation. */
        public boolean compileAfterApply = true;

        /** Legacy patterns expected to be absent after successful migration. */
        public List<String> forbiddenPatterns = new ArrayList<>();
    }
}
