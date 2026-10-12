package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Migrates UploadedFile Java APIs and FileUpload XHTML while marking Commons uploader and semantic edge cases for review.
 */
public class PrimeFaces62FileUploadMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 FileUpload migration";
    }

    @Override
    public String getDescription() {
        return "Migrates UploadedFile Java APIs and FileUpload XHTML while marking Commons uploader and semantic edge cases for review.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new MigrateUploadedFileApi(),
                new PrimeFaces62FileUploadXhtmlMigration(),
                new PrimeFaces62FileUploadCommonsReview()
        );
    }
}
