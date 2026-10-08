package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;

import java.util.List;

/**
 * Finds legacy DefaultStreamedContent and LazyDefaultStreamedContent usages that may need builder/Supplier migration.
 */
public class PrimeFaces62StreamedContentEdgeInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory remaining StreamedContent edge cases";
    }

    @Override
    public String getDescription() {
        return "Finds legacy DefaultStreamedContent and LazyDefaultStreamedContent usages that may need builder/Supplier migration.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTypes(
                        "org.primefaces.model.DefaultStreamedContent",
                        false),
                new FindTypes(
                        "org.primefaces.model.LazyDefaultStreamedContent",
                        false)
        );
    }
}
