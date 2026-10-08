package com.jackson.migration.rewrite;

import com.jackson.migration.catalog.MigrationCatalog;
import com.jackson.migration.config.MigrationConfig;
import com.jackson.migration.process.CommandResult;
import com.jackson.migration.process.MavenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the OpenRewrite Maven plugin for one or more catalog-selected recipes, including optional dry-run validation.
 */
@ApplicationScoped
public class RewriteService {
    @Inject MavenService maven;
    @Inject RewriteMavenSettingsService settings;

    /**
     * Applies the requested OpenRewrite recipes through the target project's Maven build.
     * When configured, a dry run is executed before the mutating run so recipe resolution errors
     * fail before source files are changed.
     *
     * @param project target application project root
     * @param catalog catalog containing recipe artifact/plugin coordinates
     * @param config migration configuration controlling rewrite behavior
     * @param recipes fully qualified recipe class names to execute
     * @throws IOException if Maven/OpenRewrite execution cannot start or fails
     * @throws InterruptedException if Maven execution is interrupted
     */
    public void apply(Path project, MigrationCatalog catalog, MigrationConfig config, List<String> recipes)
            throws IOException, InterruptedException {
        if (recipes == null || recipes.isEmpty()) return;
        if (config.rewrite.dryRunBeforeApply) run(project, catalog, recipes, rewriteGoal(true));
        run(project, catalog, recipes, rewriteGoal(false));
    }

    /**
     * Returns the non-forking Maven goal used by the Workbench.
     *
     * @param dryRun whether the invocation is a preview rather than a mutating run
     * @return {@code dryRunNoFork} for previews or {@code runNoFork} for mutation
     */
    static String rewriteGoal(boolean dryRun) {
        return dryRun ? "dryRunNoFork" : "runNoFork";
    }

    /**
     * Invokes a non-forking OpenRewrite Maven goal with catalog-managed plugin and recipe coordinates.
     *
     * <p>The Workbench intentionally uses {@code dryRunNoFork} and {@code runNoFork}. The normal
     * {@code dryRun}/{@code run} goals fork the Maven lifecycle through compilation before Rewrite
     * executes. That is unsafe for modernization workflows because the legacy source may be
     * temporarily uncompilable until dependency, package, and source transformations are applied.</p>
     */
    private void run(Path project, MigrationCatalog catalog, List<String> recipes, String goal)
            throws IOException, InterruptedException {
        Path temporarySettings = null;
        try {
            List<String> args = new ArrayList<>();
            if (catalog.rewriteRepositoryUrl != null && !catalog.rewriteRepositoryUrl.isBlank()) {
                temporarySettings = settings.create(catalog.rewriteRepositoryUrl);
                args.add("-s");
                args.add(temporarySettings.toString());
            }
            args.add("-U");
            args.add("org.openrewrite.maven:rewrite-maven-plugin:" + catalog.rewritePluginVersion + ":" + goal);
            args.add("-Drewrite.recipeArtifactCoordinates=" + catalog.recipeArtifactCoordinates);
            args.add("-Drewrite.activeRecipes=" + String.join(",", recipes));
            if ("dryRunNoFork".equals(goal)) args.add("-Drewrite.exportDatatables=true");
            CommandResult result = maven.run(project, args, true);
            if (!result.success()) throw new IOException("OpenRewrite " + goal + " failed with exit code " + result.exitCode());
        } finally {
            if (temporarySettings != null) java.nio.file.Files.deleteIfExists(temporarySettings);
        }
    }
}
