package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;

import java.util.List;

/**
 * Main InputSwitch migration recipe.
 *
 * The search-only inventory and destructive post-review cleanup remain
 * separate so running this composite does not inject search markers or discard
 * visible label semantics.
 */
public class PrimeFaces62InputSwitchMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "PrimeFaces 6.2 InputSwitch migration";
    }

    @Override
    public String getDescription() {
        return "Migrates safe InputSwitch usages to ToggleSwitch and leaves label-sensitive usages explicitly marked for review.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new PrimeFaces62InputSwitchToToggleSwitch()
        );
    }
}
