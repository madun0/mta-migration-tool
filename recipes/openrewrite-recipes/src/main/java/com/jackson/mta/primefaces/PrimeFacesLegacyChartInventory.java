package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Search-only inventory for legacy PrimeFaces jqPlot chart models, interim
 * PrimeFaces Chart.js model classes, and p:chart component usages.
 */
public class PrimeFacesLegacyChartInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory legacy PrimeFaces charts";
    }

    @Override
    public String getDescription() {
        return "Finds legacy PrimeFaces chart model types and p:chart usages before PrimeFaces 16 chart migration.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTypes("org.primefaces.model.chart..*", false),
                new FindTypes("org.primefaces.model.charts..*", false),
                new FindTags("//p:chart")
        );
    }
}
