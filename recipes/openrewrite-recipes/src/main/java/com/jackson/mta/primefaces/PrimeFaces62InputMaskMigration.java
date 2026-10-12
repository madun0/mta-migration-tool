package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Marks unsafe InputMask cases for explicit migration decisions rather than inventing masks.
 */
public class PrimeFaces62InputMaskMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 InputMask migration";
    }

    @Override
    public String getDescription() {
        return "Marks unsafe InputMask cases for explicit migration decisions rather than inventing masks.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new PrimeFaces62InputMaskReview());
    }
}
