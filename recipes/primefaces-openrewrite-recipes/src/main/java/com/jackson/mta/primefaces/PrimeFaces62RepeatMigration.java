package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Main p:repeat migration.
 *
 * Search-only Java coupling inventory remains separate so the main migration
 * changes only XHTML and does not inject OpenRewrite search markers into Java.
 */
public class PrimeFaces62RepeatMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 p:repeat migration";
    }

    @Override
    public String getDescription() {
        return "Migrates p:repeat markup to standard ui:repeat for PrimeFaces 13+ / Jakarta Faces.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new PrimeFaces62RepeatToUiRepeat()
        );
    }
}
