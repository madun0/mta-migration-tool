package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Inventory recipe for the PrimeFaces InputSwitch -> ToggleSwitch migration.
 */
public class PrimeFaces62InputSwitchInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory PrimeFaces 6.2 InputSwitch usage";
    }

    @Override
    public String getDescription() {
        return "Finds p:inputSwitch tags, programmatic InputSwitch references, and legacy text-label options that need review before PrimeFaces 16.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTypes(
                        "org.primefaces.component.inputswitch.InputSwitch",
                        false),
                new FindTypes(
                        "org.primefaces.component.inputswitch.InputSwitchBase",
                        false),
                new FindTags("//p:inputSwitch"),
                new FindTags(
                        "//p:inputSwitch[@onLabel or @offLabel or @showLabels]")
        );
    }
}
