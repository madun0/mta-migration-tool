package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.xml.ChangeTagAttribute;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62CalendarCleanupAfterReview}.
 */
class PrimeFaces62CalendarCleanupAfterReviewTest {

    @Test
    void isConcreteRecipeWithExpectedCleanupSteps() {
        Recipe recipe = new PrimeFaces62CalendarCleanupAfterReview();
        List<Recipe> children = recipe.getRecipeList();

        assertEquals("com.jackson.mta.primefaces.PrimeFaces62CalendarCleanupAfterReview", recipe.getName());
        assertEquals(12, children.size());
        assertTrue(children.stream().allMatch(ChangeTagAttribute.class::isInstance));
    }
}
