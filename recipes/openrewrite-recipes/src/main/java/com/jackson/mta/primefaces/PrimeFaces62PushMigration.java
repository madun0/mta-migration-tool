package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Marks removed PrimeFaces Push usage for architectural WebSocket migration.
 */
public class PrimeFaces62PushMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 Push migration";
    }

    @Override
    public String getDescription() {
        return "Marks removed PrimeFaces Push usage for architectural WebSocket migration.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new PrimeFaces62PushReview());
    }
}
