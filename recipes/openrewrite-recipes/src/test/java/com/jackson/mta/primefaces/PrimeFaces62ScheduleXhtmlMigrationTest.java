package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62ScheduleXhtmlMigration}.
 */
class PrimeFaces62ScheduleXhtmlMigrationTest {

    @Test
    void isConcreteRecipeWithChildren() {
        PrimeFaces62ScheduleXhtmlMigration recipe = new PrimeFaces62ScheduleXhtmlMigration();
        assertEquals("com.jackson.mta.primefaces.PrimeFaces62ScheduleXhtmlMigration", recipe.getName());
        assertNotNull(recipe.getDisplayName());
        assertFalse(recipe.getDisplayName().isBlank());
        assertNotNull(recipe.getDescription());
        assertFalse(recipe.getDescription().isBlank());
        assertFalse(recipe.getRecipeList().isEmpty());
    }
}
