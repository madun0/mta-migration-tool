package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;

import java.util.List;

/**
 * Finds application code coupled to the PrimeFaces 6.2 org.primefaces.json package.
 */
public class PrimeFaces62JsonInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory org.primefaces.json usage";
    }

    @Override
    public String getDescription() {
        return "Finds application code coupled to the PrimeFaces 6.2 org.primefaces.json package.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTypes("org.primefaces.json..*", false)
        );
    }
}
