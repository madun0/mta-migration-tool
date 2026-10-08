package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62PushReview}.
 */
class PrimeFaces62PushReviewTest {

    @Test
    void isConcreteRecipe() {
        Recipe recipe = new PrimeFaces62PushReview();
        assertEquals("com.jackson.mta.primefaces.PrimeFaces62PushReview", recipe.getName());
        assertNotNull(recipe.getDisplayName());
        assertFalse(recipe.getDisplayName().isBlank());
        assertNotNull(recipe.getDescription());
        assertFalse(recipe.getDescription().isBlank());
        assertTrue(recipe.getRecipeList().size() >= 1);
    }
}
