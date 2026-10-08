package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Finds p:socket and Java references to the PrimeFaces Push/Atmosphere API removed in PrimeFaces 7.
 */
public class PrimeFaces62PushInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory removed PrimeFaces Push";
    }

    @Override
    public String getDescription() {
        return "Finds p:socket and Java references to the PrimeFaces Push/Atmosphere API removed in PrimeFaces 7.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTags("//p:socket"),
                new FindTypes("org.primefaces.push..*", false)
        );
    }
}
