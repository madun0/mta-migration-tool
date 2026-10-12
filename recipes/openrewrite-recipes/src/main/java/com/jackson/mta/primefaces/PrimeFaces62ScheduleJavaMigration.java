package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Migrates Date-based DefaultScheduleEvent construction and LazyScheduleModel signatures to LocalDateTime-based APIs.
 */
public class PrimeFaces62ScheduleJavaMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces 6.2 Schedule Java APIs";
    }

    @Override
    public String getDescription() {
        return "Migrates Date-based DefaultScheduleEvent construction and LazyScheduleModel signatures to LocalDateTime-based APIs.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new MigrateScheduleEventConstruction(),
                new MigrateLazyScheduleModel()
        );
    }
}
