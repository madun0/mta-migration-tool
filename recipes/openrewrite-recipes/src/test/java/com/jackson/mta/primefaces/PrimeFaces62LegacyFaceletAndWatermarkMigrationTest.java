package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies the legacy Facelet helper functions are rewritten by the catalog composite. */
class PrimeFaces62LegacyFaceletAndWatermarkMigrationTest implements RewriteTest {
    @Test void migratesLegacyWidgetVarFunction() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62LegacyFaceletAndWatermarkMigration()),
            xml(
                """
                <h:panelGroup xmlns:h="http://java.sun.com/jsf/html" xmlns:p="http://primefaces.org/ui"
                              styleClass="#{p:widgetVar('table')}"/>
                """,
                """
                <h:panelGroup xmlns:h="http://java.sun.com/jsf/html" xmlns:p="http://primefaces.org/ui"
                              styleClass="#{p:resolveWidgetVar('table', view)}"/>
                """));
    }
}
