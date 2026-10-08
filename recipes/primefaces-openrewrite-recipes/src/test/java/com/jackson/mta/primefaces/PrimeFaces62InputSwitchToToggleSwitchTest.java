package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeType;
import org.openrewrite.xml.AddCommentToXmlTag;
import org.openrewrite.xml.ChangeTagAttribute;
import org.openrewrite.xml.ChangeTagName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62InputSwitchToToggleSwitch}.
 */
class PrimeFaces62InputSwitchToToggleSwitchTest {

    @Test
    void isConcreteMigrationRecipe() {
        Recipe recipe = new PrimeFaces62InputSwitchToToggleSwitch();

        assertEquals(
                "com.jackson.mta.primefaces.PrimeFaces62InputSwitchToToggleSwitch",
                recipe.getName());
        assertTrue(recipe.getRecipeList().stream().anyMatch(ChangeType.class::isInstance));
        assertTrue(recipe.getRecipeList().stream().anyMatch(ChangeTagName.class::isInstance));
        assertTrue(recipe.getRecipeList().stream().anyMatch(ChangeTagAttribute.class::isInstance));
        assertTrue(recipe.getRecipeList().stream().anyMatch(AddCommentToXmlTag.class::isInstance));
    }
}
