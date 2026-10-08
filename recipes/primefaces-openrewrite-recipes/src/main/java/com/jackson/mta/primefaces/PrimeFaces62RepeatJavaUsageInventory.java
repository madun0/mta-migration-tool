package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;

import java.util.List;

/**
 * Search-only recipe for Java code coupled to PrimeFaces' old UIRepeat class.
 *
 * There is intentionally no ChangeType here. Jakarta Faces specifies ui:repeat
 * as a Facelets tag/component contract but does not expose a portable public
 * jakarta.faces.component.UIRepeat type. Mapping to Mojarra's
 * com.sun.faces.facelets.component.UIRepeat would create implementation
 * coupling and would not be portable to MyFaces.
 */
public class PrimeFaces62RepeatJavaUsageInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory Java references to PrimeFaces UIRepeat";
    }

    @Override
    public String getDescription() {
        return "Finds Java code bound to org.primefaces.component.repeat.UIRepeat so it can be decoupled from the removed implementation.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTypes(
                        "org.primefaces.component.repeat.UIRepeat",
                        false)
        );
    }
}
