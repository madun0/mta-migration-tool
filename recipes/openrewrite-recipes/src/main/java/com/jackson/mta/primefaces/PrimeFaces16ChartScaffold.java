package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.dependencies.AddDependency;
import org.openrewrite.xml.AddCommentToXmlTag;

import java.util.List;

/**
 * Adds the XDEV Chart.js Java model dependency when legacy PrimeFaces chart APIs are used and annotates p:chart for migration.
 */
public class PrimeFaces16ChartScaffold extends Recipe {

    @Override
    public String getDisplayName() {
        return "Prepare legacy PrimeFaces charts for PrimeFaces 16";
    }

    @Override
    public String getDescription() {
        return "Adds the XDEV Chart.js Java model dependency when legacy PrimeFaces chart APIs are used and annotates p:chart for migration.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                addXdevDependency("org.primefaces.model.chart..*"),
                addXdevDependency("org.primefaces.model.charts..*"),
                new AddCommentToXmlTag(
                        "//p:chart",
                        "PF16 migration: p:chart expects Chart.js JSON via value=. Move legacy type/responsive/model options into the JSON/XDEV model, then run PrimeFaces16ChartXhtmlFinalize.")
        );
    }

    private Recipe addXdevDependency(String onlyIfUsing) {
        return new AddDependency(
                "software.xdev",
                "chartjs-java-model",
                "3.1.0",
                null,
                onlyIfUsing,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }
}
