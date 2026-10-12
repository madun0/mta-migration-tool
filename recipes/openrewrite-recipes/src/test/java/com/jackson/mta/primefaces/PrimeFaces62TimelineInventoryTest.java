package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62TimelineInventory}.
 */
class PrimeFaces62TimelineInventoryTest {

    @Test
    void isConcreteRecipeWithChildren() {
        PrimeFaces62TimelineInventory recipe = new PrimeFaces62TimelineInventory();
        assertEquals("com.jackson.mta.primefaces.PrimeFaces62TimelineInventory", recipe.getName());
        assertNotNull(recipe.getDisplayName());
        assertFalse(recipe.getDisplayName().isBlank());
        assertNotNull(recipe.getDescription());
        assertFalse(recipe.getDescription().isBlank());
        assertFalse(recipe.getRecipeList().isEmpty());
    }
}
