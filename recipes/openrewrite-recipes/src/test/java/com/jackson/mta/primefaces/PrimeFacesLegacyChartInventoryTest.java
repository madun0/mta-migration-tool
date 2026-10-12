package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFacesLegacyChartInventory}.
 */
class PrimeFacesLegacyChartInventoryTest {

    @Test
    void isConcreteRecipeWithJavaAndXmlInventoryChecks() {
        Recipe recipe = new PrimeFacesLegacyChartInventory();
        List<Recipe> children = recipe.getRecipeList();

        assertEquals("com.jackson.mta.primefaces.PrimeFacesLegacyChartInventory", recipe.getName());
        assertEquals(3, children.size());
        assertEquals(2, children.stream().filter(FindTypes.class::isInstance).count());
        assertEquals(1, children.stream().filter(FindTags.class::isInstance).count());
    }
}
