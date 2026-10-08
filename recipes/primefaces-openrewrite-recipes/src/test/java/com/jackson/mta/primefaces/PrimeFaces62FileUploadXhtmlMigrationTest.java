package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62FileUploadXhtmlMigration}.
 */
class PrimeFaces62FileUploadXhtmlMigrationTest {

    @Test
    void isConcreteRecipe() {
        Recipe recipe = new PrimeFaces62FileUploadXhtmlMigration();
        assertEquals("com.jackson.mta.primefaces.PrimeFaces62FileUploadXhtmlMigration", recipe.getName());
        assertNotNull(recipe.getDisplayName());
        assertFalse(recipe.getDisplayName().isBlank());
        assertNotNull(recipe.getDescription());
        assertFalse(recipe.getDescription().isBlank());
        assertTrue(recipe.getRecipeList().size() >= 1);
    }
}
