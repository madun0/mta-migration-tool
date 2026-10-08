package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Migrates legacy LazyDataModel load signatures to SortMeta/FilterMeta maps while preserving existing bodies.
 */
public class PrimeFaces62LazyDataModelMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 LazyDataModel load migration";
    }

    @Override
    public String getDescription() {
        return "Migrates legacy LazyDataModel load signatures to SortMeta/FilterMeta maps while preserving existing bodies.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new MigrateLazyDataModelLoad()
        );
    }
}
