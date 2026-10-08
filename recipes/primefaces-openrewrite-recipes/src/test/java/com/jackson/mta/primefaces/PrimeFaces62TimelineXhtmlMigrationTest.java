package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62TimelineXhtmlMigration}.
 */
class PrimeFaces62TimelineXhtmlMigrationTest {

    @Test
    void isConcreteRecipeWithChildren() {
        PrimeFaces62TimelineXhtmlMigration recipe = new PrimeFaces62TimelineXhtmlMigration();
        assertEquals("com.jackson.mta.primefaces.PrimeFaces62TimelineXhtmlMigration", recipe.getName());
        assertNotNull(recipe.getDisplayName());
        assertFalse(recipe.getDisplayName().isBlank());
        assertNotNull(recipe.getDescription());
        assertFalse(recipe.getDescription().isBlank());
        assertFalse(recipe.getRecipeList().isEmpty());
    }
}
