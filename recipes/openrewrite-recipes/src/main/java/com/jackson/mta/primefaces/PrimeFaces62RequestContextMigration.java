package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Migrates high-confidence deprecated RequestContext calls to the PrimeFaces.current() API.
 */
public class PrimeFaces62RequestContextMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 RequestContext migration";
    }

    @Override
    public String getDescription() {
        return "Migrates high-confidence deprecated RequestContext calls to the PrimeFaces.current() API.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new MigrateRequestContext()
        );
    }
}
