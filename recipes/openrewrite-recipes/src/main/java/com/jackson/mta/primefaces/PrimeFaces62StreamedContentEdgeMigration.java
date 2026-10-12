package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Migrates safe DefaultStreamedContent constructors while leaving LazyDefaultStreamedContent and ambiguous stream creation for review.
 */
public class PrimeFaces62StreamedContentEdgeMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate remaining safe StreamedContent constructors";
    }

    @Override
    public String getDescription() {
        return "Migrates safe DefaultStreamedContent constructors while leaving LazyDefaultStreamedContent and ambiguous stream creation for review.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new MigrateDefaultStreamedContentConstructors());
    }
}
