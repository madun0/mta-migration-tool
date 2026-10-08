package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.xml.AddCommentToXmlTag;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62DataTableFilterOptionsReview}.
 */
class PrimeFaces62DataTableFilterOptionsReviewTest {

    @Test
    void isConcreteReviewRecipe() {
        Recipe recipe = new PrimeFaces62DataTableFilterOptionsReview();

        assertEquals(
                "com.jackson.mta.primefaces.PrimeFaces62DataTableFilterOptionsReview",
                recipe.getName());
        assertEquals(2, recipe.getRecipeList().size());
        assertTrue(recipe.getRecipeList().stream()
                .allMatch(AddCommentToXmlTag.class::isInstance));
    }
}
