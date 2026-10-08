package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Migrates removed Facelet helper functions and marks Watermark client-side API usage for cleanup.
 */
public class PrimeFaces62LegacyFaceletAndWatermarkMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces legacy Facelet function and Watermark migration";
    }

    @Override
    public String getDescription() {
        return "Migrates removed Facelet helper functions and marks Watermark client-side API usage for cleanup.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new PrimeFaces62LegacyFaceletFunctionMigration(),
                new PrimeFaces62WatermarkReview()
        );
    }
}
