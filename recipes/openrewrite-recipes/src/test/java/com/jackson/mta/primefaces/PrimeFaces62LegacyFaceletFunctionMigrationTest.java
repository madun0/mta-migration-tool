package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62LegacyFaceletFunctionMigration}.
 */
class PrimeFaces62LegacyFaceletFunctionMigrationTest {

    @Test
    void isConcreteVisitorRecipe() {
        Recipe recipe = new PrimeFaces62LegacyFaceletFunctionMigration();
        assertEquals("com.jackson.mta.primefaces.PrimeFaces62LegacyFaceletFunctionMigration", recipe.getName());
        assertNotNull(recipe.getVisitor());
        assertNotNull(recipe.getDisplayName());
        assertFalse(recipe.getDescription().isBlank());
    }
}
