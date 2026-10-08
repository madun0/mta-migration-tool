package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.xml.search.FindTags;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62InputSwitchInventory}.
 */
class PrimeFaces62InputSwitchInventoryTest {

    @Test
    void isConcreteInventoryRecipe() {
        Recipe recipe = new PrimeFaces62InputSwitchInventory();

        assertEquals(
                "com.jackson.mta.primefaces.PrimeFaces62InputSwitchInventory",
                recipe.getName());
        assertTrue(recipe.getRecipeList().stream().anyMatch(FindTypes.class::isInstance));
        assertTrue(recipe.getRecipeList().stream().anyMatch(FindTags.class::isInstance));
    }
}
