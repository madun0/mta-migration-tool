package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62RepeatJavaUsageInventory}.
 */
class PrimeFaces62RepeatJavaUsageInventoryTest {

    @Test
    void isConcreteJavaInventoryRecipe() {
        Recipe recipe = new PrimeFaces62RepeatJavaUsageInventory();

        assertEquals(
                "com.jackson.mta.primefaces.PrimeFaces62RepeatJavaUsageInventory",
                recipe.getName());
        assertEquals(1, recipe.getRecipeList().size());
        assertInstanceOf(FindTypes.class, recipe.getRecipeList().get(0));
    }
}
