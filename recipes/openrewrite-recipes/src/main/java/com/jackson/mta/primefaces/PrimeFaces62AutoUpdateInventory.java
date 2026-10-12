package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Finds autoUpdate attributes removed from outputPanel, fragment, messages, and growl in PrimeFaces 7.
 */
public class PrimeFaces62AutoUpdateInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory removed autoUpdate attributes";
    }

    @Override
    public String getDescription() {
        return "Finds autoUpdate attributes removed from outputPanel, fragment, messages, and growl in PrimeFaces 7.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTags("//p:outputPanel[@autoUpdate]"),
                new FindTags("//p:fragment[@autoUpdate]"),
                new FindTags("//p:messages[@autoUpdate]"),
                new FindTags("//p:growl[@autoUpdate]")
        );
    }
}
