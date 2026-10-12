package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies chart views are annotated with the PrimeFaces 16 Chart.js migration action. */
class PrimeFaces16ChartScaffoldTest implements RewriteTest {
    @Test void annotatesLegacyChartMarkup() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces16ChartScaffold()),
            xml(
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets" xmlns:p="http://primefaces.org/ui">
                    <p:chart type="bar" model="#{bean.model}"/>
                </ui:composition>
                """,
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets" xmlns:p="http://primefaces.org/ui">
                    <!--PF16 migration: p:chart expects Chart.js JSON via value=. Move legacy type/responsive/model options into the JSON/XDEV model, then run PrimeFaces16ChartXhtmlFinalize.-->
                    <p:chart type="bar" model="#{bean.model}"/>
                </ui:composition>
                """));
    }
}
