package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Migrates ByteArrayContent and high-confidence DefaultStreamedContent constructors to the modern builder/Supplier API.
 */
public class PrimeFaces62StreamedContentMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 StreamedContent migration";
    }

    @Override
    public String getDescription() {
        return "Migrates ByteArrayContent and high-confidence DefaultStreamedContent constructors to the modern builder/Supplier API.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new MigrateByteArrayContent(),
                new PrimeFaces62StreamedContentEdgeMigration()
        );
    }
}
