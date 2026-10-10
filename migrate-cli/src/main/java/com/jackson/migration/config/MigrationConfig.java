package com.jackson.migration.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-application configuration controlling the selected migration profile, MTA execution,
 * validation gates, and OpenRewrite behavior.
 */
public class MigrationConfig {

    /** Friendly application/project name used in reports. */
    public String projectName = "";

    /** Migration profile key from {@code migration-catalog.yaml}. */
    public String profile = "standard";

    /** MTA assessment settings. */
    public MtaSettings mta = new MtaSettings();

    /** Build and verification settings. */
    public ValidationSettings validation = new ValidationSettings();

    /** OpenRewrite execution settings. */
    public RewriteSettings rewrite = new RewriteSettings();

    /**
     * Configures how the workbench invokes MTA for an application assessment.
     */
    public static class MtaSettings {

        /**
         * Legacy single target retained for backward compatibility with existing migration.yaml files.
         * New configurations should use {@link #targets}.
         */
        public String target;

        /** Source technologies used to select relevant MTA rules. */
        public List<String> sources = new ArrayList<>();

        /** Target technologies used to select relevant MTA rules. */
        public List<String> targets = new ArrayList<>();

        /**
         * MTA analysis mode requested for {@code mta-cli analyze}.
         *
         * <p>The Workbench defaults to {@code source-only} so assessment remains scoped to the
         * selected application tree. {@code full} enables dependency analysis and can cause MTA's
         * Java/Maven provider to inspect external dependency locations such as the local Maven
         * repository or parent/reactor modules.</p>
         */
        public String mode = "source-only";

        /**
         * Enforces an application-source-only assessment boundary.
         *
         * <p>When {@code true} (the default), the Workbench forces MTA to use
         * {@code --mode source-only} even if an older migration configuration requests
         * {@code full}. Set this to {@code false} only when dependency-aware MTA analysis is
         * intentionally required and external dependency filesystem access is acceptable.</p>
         */
        public boolean strictProjectScope = true;
    }

    /**
     * Configures build and test gates executed during and after migration.
     */
    public static class ValidationSettings {

        /** Whether Maven compile must pass after each automatic/assisted family. */
        public boolean compileAfterFamily = true;

        /** Whether final verification runs the target application's tests. */
        public boolean runTestsOnVerify = true;
    }

    /**
     * Configures how OpenRewrite transformations are previewed and applied.
     */
    public static class RewriteSettings {

        /** Whether each recipe set receives a dry run before the mutating OpenRewrite run. */
        public boolean dryRunBeforeApply = true;
    }
}
