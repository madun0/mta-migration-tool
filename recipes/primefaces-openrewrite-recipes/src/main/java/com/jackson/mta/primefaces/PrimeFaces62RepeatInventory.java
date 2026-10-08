package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Inventory for the PrimeFaces p:repeat -> standard Facelets ui:repeat migration.
 *
 * PrimeFaces 13 removed p:repeat because modern Mojarra and MyFaces implement
 * the iterator behavior that originally motivated the PrimeFaces copy.
 */
public class PrimeFaces62RepeatInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory PrimeFaces p:repeat usage";
    }

    @Override
    public String getDescription() {
        return "Finds p:repeat tags and programmatic references to PrimeFaces' removed UIRepeat implementation.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTags("//p:repeat"),
                new FindTypes(
                        "org.primefaces.component.repeat.UIRepeat",
                        false)
        );
    }
}
