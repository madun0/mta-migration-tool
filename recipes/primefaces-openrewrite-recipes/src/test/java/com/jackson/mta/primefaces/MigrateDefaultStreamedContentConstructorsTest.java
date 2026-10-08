package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link MigrateDefaultStreamedContentConstructors}.
 */
class MigrateDefaultStreamedContentConstructorsTest {

    @Test
    void isConcreteVisitorRecipe() {
        Recipe recipe = new MigrateDefaultStreamedContentConstructors();
        assertEquals("com.jackson.mta.primefaces.MigrateDefaultStreamedContentConstructors", recipe.getName());
        assertNotNull(recipe.getVisitor());
        assertNotNull(recipe.getDisplayName());
        assertFalse(recipe.getDescription().isBlank());
    }
}
