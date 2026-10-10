package com.jackson.migration.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
         * Whether full-mode Java analysis should also apply rules to dependencies that MTA
         * classifies as known open-source libraries. This is a separate MTA control from
         * {@code mode: full}. It is only passed to MTA when full mode is selected.
         */
        public boolean analyzeKnownLibraries = true;

        /**
         * Optional Maven settings file used by MTA's Java dependency analysis. Relative paths are
         * resolved against the application project root. Leave blank to let MTA/Maven use their
         * normal settings discovery.
         */
        public String mavenSettings = "";

        /**
         * Whether MTA should avoid the remote Maven search index when classifying dependencies.
         * Keep false for the most accurate classification. Set true in disconnected/restricted
         * environments when the Maven search index is unavailable; MTA may then report more
         * incidents because some dependencies can be classified as internal.
         */
        public boolean disableMavenSearch = false;

        /**
         * Legacy v15-v19 compatibility field. MTA scope is controlled exclusively by
         * {@link #mode} beginning with v20. The property is ignored when reading older
         * migration.yaml files and is not written to newly generated configurations.
         */
        @Deprecated
        @JsonIgnore
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
