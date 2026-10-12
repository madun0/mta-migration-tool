package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeType;

import java.util.List;

/**
 * Migrates DynamicMenuModel to BaseMenuModel and modernizes programmatic menu APIs.
 */
public class PrimeFaces62MenuMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 programmatic menu migration";
    }

    @Override
    public String getDescription() {
        return "Migrates DynamicMenuModel to BaseMenuModel and modernizes programmatic menu APIs.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangeType(
                        "org.primefaces.model.menu.DynamicMenuModel",
                        "org.primefaces.model.menu.BaseMenuModel",
                        true),
                new MigrateProgrammaticMenuApi()
        );
    }
}
