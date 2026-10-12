package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Finds OverlayPanel appendToBody attributes removed in PrimeFaces 7.
 */
public class PrimeFaces62OverlayPanelInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory OverlayPanel appendToBody usage";
    }

    @Override
    public String getDescription() {
        return "Finds OverlayPanel appendToBody attributes removed in PrimeFaces 7.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new FindTags("//p:overlayPanel[@appendToBody]"));
    }
}
