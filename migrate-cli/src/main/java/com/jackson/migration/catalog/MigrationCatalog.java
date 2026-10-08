package com.jackson.migration.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory representation of migration profiles, phases, recipe families, rule mappings, and
 * verification metadata loaded from {@code migration-catalog.yaml}.
 */
public class MigrationCatalog {

    /** Catalog schema/content version. */
    public String version;

    /** Maven coordinates of the OpenRewrite recipe artifact used by the target application. */
    public String recipeArtifactCoordinates;

    /** OpenRewrite Maven plugin version invoked by the workbench. */
    public String rewritePluginVersion;

    /** Repository used to resolve current OpenRewrite Maven plugin releases. */
    public String rewriteRepositoryUrl;

    /** Named migration profiles keyed by CLI/profile identifier. */
    public Map<String, ProfileSpec> profiles = new LinkedHashMap<>();

    /** Migration family specifications keyed by stable family identifier. */
    public Map<String, FamilySpec> families = new LinkedHashMap<>();

    /**
     * Defines an ordered set of migration phases exposed as a named workbench profile.
     */
    public static class ProfileSpec {

        /** Human-readable purpose of the profile. */
        public String description;

        /** Ordered phase names included in the profile. */
        public List<String> phases = new ArrayList<>();
    }

    /**
     * Defines one migration family, including MTA rule mappings, OpenRewrite recipes, automation
     * level, documentation, and residual validation.
     */
    public static class FamilySpec {

        /** Human-readable description of the migration concern. */
        public String description;

        /** Phase in which the family executes. */
        public String phase;

        /** Automation classification such as AUTOMATIC, ASSISTED, or MANUAL. */
        public String automation;

        /** MTA rule identifiers that cause this family to be selected. */
        public List<String> mtaRuleIds = new ArrayList<>();

        /** Curated MTA rule-ID prefixes that cause this family to be selected. */
        public List<String> mtaRulePrefixes = new ArrayList<>();

        /** Concrete OpenRewrite recipe class names executed during the migration pass. */
        public List<String> recipes = new ArrayList<>();

        /** Post-review OpenRewrite recipes executed only after review is confirmed. */
        public List<String> cleanupRecipes = new ArrayList<>();

        /** Relative path to the family-specific migration guide. */
        public String guide;

        /** Legacy literal patterns that should not remain after successful migration. */
        public List<String> forbiddenPatterns = new ArrayList<>();

        /** Developer verification steps shown for manual and assisted migration families. */
        public List<String> reviewChecklist = new ArrayList<>();

        /** Whether the Workbench should compile immediately after applying this family. */
        public boolean compileAfterApply = true;

        /** Whether the family is planned even when no matching MTA finding is present. */
        public boolean always;
    }
}
