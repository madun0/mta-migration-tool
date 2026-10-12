package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.xml.ChangeTagAttribute;
import org.openrewrite.xml.ChangeTagName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62InputSwitchCleanupAfterReview}.
 */
class PrimeFaces62InputSwitchCleanupAfterReviewTest {

    @Test
    void isConcreteCleanupRecipe() {
        Recipe recipe = new PrimeFaces62InputSwitchCleanupAfterReview();

        assertEquals(
                "com.jackson.mta.primefaces.PrimeFaces62InputSwitchCleanupAfterReview",
                recipe.getName());
        assertTrue(recipe.getRecipeList().stream().anyMatch(ChangeTagAttribute.class::isInstance));
        assertTrue(recipe.getRecipeList().stream().anyMatch(ChangeTagName.class::isInstance));
    }
}
