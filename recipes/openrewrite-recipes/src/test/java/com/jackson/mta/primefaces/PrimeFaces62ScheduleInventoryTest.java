package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62ScheduleInventory}.
 */
class PrimeFaces62ScheduleInventoryTest {

    @Test
    void isConcreteRecipeWithChildren() {
        PrimeFaces62ScheduleInventory recipe = new PrimeFaces62ScheduleInventory();
        assertEquals("com.jackson.mta.primefaces.PrimeFaces62ScheduleInventory", recipe.getName());
        assertNotNull(recipe.getDisplayName());
        assertFalse(recipe.getDisplayName().isBlank());
        assertNotNull(recipe.getDescription());
        assertFalse(recipe.getDescription().isBlank());
        assertFalse(recipe.getRecipeList().isEmpty());
    }
}
