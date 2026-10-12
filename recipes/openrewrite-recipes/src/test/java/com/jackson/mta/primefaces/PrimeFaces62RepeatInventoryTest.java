package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.xml.search.FindTags;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62RepeatInventory}.
 */
class PrimeFaces62RepeatInventoryTest {

    @Test
    void isConcreteInventoryRecipe() {
        Recipe recipe = new PrimeFaces62RepeatInventory();

        assertEquals(
                "com.jackson.mta.primefaces.PrimeFaces62RepeatInventory",
                recipe.getName());
        assertTrue(recipe.getRecipeList().stream().anyMatch(FindTags.class::isInstance));
        assertTrue(recipe.getRecipeList().stream().anyMatch(FindTypes.class::isInstance));
    }
}
