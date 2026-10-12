package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62JsonInventory}.
 */
class PrimeFaces62JsonInventoryTest {

    @Test
    void isConcreteRecipe() {
        Recipe recipe = new PrimeFaces62JsonInventory();
        assertEquals("com.jackson.mta.primefaces.PrimeFaces62JsonInventory", recipe.getName());
        assertNotNull(recipe.getDisplayName());
        assertFalse(recipe.getDisplayName().isBlank());
        assertNotNull(recipe.getDescription());
        assertFalse(recipe.getDescription().isBlank());
        assertTrue(recipe.getRecipeList().size() >= 1);
    }
}
