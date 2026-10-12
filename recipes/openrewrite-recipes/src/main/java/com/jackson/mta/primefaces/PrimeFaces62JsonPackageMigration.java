package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangePackage;

import java.util.List;

/**
 * Renames org.primefaces.json references to org.primefaces.shaded.json as required by PrimeFaces 8+.
 */
public class PrimeFaces62JsonPackageMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "Move PrimeFaces JSON package to shaded namespace";
    }

    @Override
    public String getDescription() {
        return "Renames org.primefaces.json references to org.primefaces.shaded.json as required by PrimeFaces 8+.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangePackage(
                        "org.primefaces.json",
                        "org.primefaces.shaded.json",
                        true)
        );
    }
}
