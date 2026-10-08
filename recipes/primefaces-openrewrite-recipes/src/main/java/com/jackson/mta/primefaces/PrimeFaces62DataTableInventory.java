package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Inventory for PrimeFaces DataTable filtering changes between 6.2 and modern
 * PrimeFaces.
 */
public class PrimeFaces62DataTableInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory PrimeFaces 6.2 DataTable filtering";
    }

    @Override
    public String getDescription() {
        return "Finds DataTable/UIColumn Java coupling plus removed filterOptions usages that need migration.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTypes(
                        "org.primefaces.component.datatable.DataTable",
                        false),
                new FindTypes(
                        "org.primefaces.component.api.UIColumn",
                        false),
                new FindTags("//p:column[@filterOptions]"),
                new FindTags("//p:columns[@filterOptions]")
        );
    }
}
