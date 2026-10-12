package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Composite Schedule migration for Java APIs and static XHTML view names.
 */
public class PrimeFaces62ScheduleMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 Schedule migration";
    }

    @Override
    public String getDescription() {
        return "Composite Schedule migration for Java APIs and static XHTML view names.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new PrimeFaces62ScheduleJavaMigration(),
                new PrimeFaces62ScheduleXhtmlMigration()
        );
    }
}
