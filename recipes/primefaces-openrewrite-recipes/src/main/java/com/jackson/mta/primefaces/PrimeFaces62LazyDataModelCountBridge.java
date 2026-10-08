package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Adds count(filterBy) only when the existing LazyDataModel load implementation already owns row-count management.
 */
public class PrimeFaces62LazyDataModelCountBridge extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces LazyDataModel conservative count bridge";
    }

    @Override
    public String getDescription() {
        return "Adds count(filterBy) only when the existing LazyDataModel load implementation already owns row-count management.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new AddLazyDataModelCountBridge()
        );
    }
}
