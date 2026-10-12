package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Migrates removed autoUpdate attributes to the p:autoUpdate tag-handler model.
 */
public class PrimeFaces62AutoUpdateMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 autoUpdate migration";
    }

    @Override
    public String getDescription() {
        return "Migrates removed autoUpdate attributes to the p:autoUpdate tag-handler model.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new PrimeFaces62AutoUpdateLiteralMigration());
    }
}
