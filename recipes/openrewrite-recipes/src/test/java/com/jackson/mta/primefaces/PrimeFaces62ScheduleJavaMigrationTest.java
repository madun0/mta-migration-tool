package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62ScheduleJavaMigration}.
 */
class PrimeFaces62ScheduleJavaMigrationTest {

    @Test
    void isConcreteRecipeWithChildren() {
        PrimeFaces62ScheduleJavaMigration recipe = new PrimeFaces62ScheduleJavaMigration();
        assertEquals("com.jackson.mta.primefaces.PrimeFaces62ScheduleJavaMigration", recipe.getName());
        assertNotNull(recipe.getDisplayName());
        assertFalse(recipe.getDisplayName().isBlank());
        assertNotNull(recipe.getDescription());
        assertFalse(recipe.getDescription().isBlank());
        assertFalse(recipe.getRecipeList().isEmpty());
    }
}
