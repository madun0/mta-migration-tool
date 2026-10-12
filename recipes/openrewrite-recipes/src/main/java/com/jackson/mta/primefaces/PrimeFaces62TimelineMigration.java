package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Composite Timeline migration for TimelineEvent Java construction and XHTML attributes.
 */
public class PrimeFaces62TimelineMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 Timeline migration";
    }

    @Override
    public String getDescription() {
        return "Composite Timeline migration for TimelineEvent Java construction and XHTML attributes.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new MigrateTimelineEventConstruction(),
                new PrimeFaces62TimelineXhtmlMigration()
        );
    }
}
