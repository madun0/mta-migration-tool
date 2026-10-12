package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Main PrimeFaces 6.2 DataTable filtering migration.
 *
 * Inventory remains separate so the main recipe does not add OpenRewrite
 * search markers. filterOptions is marked rather than removed.
 */
public class PrimeFaces62DataTableMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 DataTable filtering migration";
    }

    @Override
    public String getDescription() {
        return "Migrates removed DataTable filter method names and marks filterOptions usages for custom facet conversion.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new PrimeFaces62DataTableFilterApiMigration(),
                new PrimeFaces62DataTableFilterOptionsReview()
        );
    }
}
