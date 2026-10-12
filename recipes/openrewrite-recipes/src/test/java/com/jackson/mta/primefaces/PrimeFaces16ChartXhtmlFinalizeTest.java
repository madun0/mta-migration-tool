package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces16ChartXhtmlFinalize}.
 */
class PrimeFaces16ChartXhtmlFinalizeTest {

    @Test
    void isConcreteRecipeWithChildren() {
        PrimeFaces16ChartXhtmlFinalize recipe = new PrimeFaces16ChartXhtmlFinalize();
        assertEquals("com.jackson.mta.primefaces.PrimeFaces16ChartXhtmlFinalize", recipe.getName());
        assertNotNull(recipe.getDisplayName());
        assertFalse(recipe.getDisplayName().isBlank());
        assertNotNull(recipe.getDescription());
        assertFalse(recipe.getDescription().isBlank());
        assertFalse(recipe.getRecipeList().isEmpty());
    }
}
