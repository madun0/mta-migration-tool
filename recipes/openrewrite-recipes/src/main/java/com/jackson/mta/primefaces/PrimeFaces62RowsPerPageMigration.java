package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.ChangeTagAttribute;

import java.util.List;

/**
 * Replaces the removed rowsPerPageTemplate * shorthand with {ShowAll|'*'}.
 */
public class PrimeFaces62RowsPerPageMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate rowsPerPageTemplate star option";
    }

    @Override
    public String getDescription() {
        return "Replaces the removed rowsPerPageTemplate * shorthand with {ShowAll|'*'}.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangeTagAttribute(
                        "//p:dataTable",
                        "rowsPerPageTemplate",
                        "{ShowAll|'*'}",
                        "\\*",
                        true)
        );
    }
}
