package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Finds rowsPerPageTemplate values using the PrimeFaces 6.2 star shorthand removed in PrimeFaces 7.
 */
public class PrimeFaces62RowsPerPageInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory DataTable rowsPerPageTemplate star usage";
    }

    @Override
    public String getDescription() {
        return "Finds rowsPerPageTemplate values using the PrimeFaces 6.2 star shorthand removed in PrimeFaces 7.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTags("//p:dataTable[contains(@rowsPerPageTemplate,'*')]")
        );
    }
}
