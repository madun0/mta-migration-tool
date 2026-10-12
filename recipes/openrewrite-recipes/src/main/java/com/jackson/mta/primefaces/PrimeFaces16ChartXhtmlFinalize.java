package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.ChangeTagAttribute;
import org.openrewrite.xml.ChangeTagAttributeKey;

import java.util.List;

/**
 * Renames p:chart model= to value= and removes legacy type/responsive attributes after the backing model returns Chart.js JSON.
 */
public class PrimeFaces16ChartXhtmlFinalize extends Recipe {

    @Override
    public String getDisplayName() {
        return "Finalize p:chart XHTML for PrimeFaces 16";
    }

    @Override
    public String getDescription() {
        return "Renames p:chart model= to value= and removes legacy type/responsive attributes after the backing model returns Chart.js JSON.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangeTagAttributeKey("//p:chart/@model", "value"),
                new ChangeTagAttribute("//p:chart", "type", null, null, false),
                new ChangeTagAttribute("//p:chart", "responsive", null, null, false)
        );
    }
}
