package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Migrates the removed OverlayPanel appendToBody attribute to appendTo.
 */
public class PrimeFaces62OverlayPanelMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 OverlayPanel migration";
    }

    @Override
    public String getDescription() {
        return "Migrates the removed OverlayPanel appendToBody attribute to appendTo.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new PrimeFaces62OverlayPanelAppendToMigration());
    }
}
