package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62CalendarInventory}.
 */
class PrimeFaces62CalendarInventoryTest {

    @Test
    void isConcreteRecipeWithExpectedInventoryChecks() {
        Recipe recipe = new PrimeFaces62CalendarInventory();
        List<Recipe> children = recipe.getRecipeList();

        assertEquals("com.jackson.mta.primefaces.PrimeFaces62CalendarInventory", recipe.getName());
        assertEquals(14, children.size());
        assertTrue(children.stream().allMatch(FindTags.class::isInstance));
    }
}
