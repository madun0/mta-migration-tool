package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Migrates the PrimeFaces 6.2 JSON package to the shaded package used by later PrimeFaces versions.
 */
public class PrimeFaces62JsonMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 JSON migration";
    }

    @Override
    public String getDescription() {
        return "Migrates the PrimeFaces 6.2 JSON package to the shaded package used by later PrimeFaces versions.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new PrimeFaces62JsonPackageMigration());
    }
}
