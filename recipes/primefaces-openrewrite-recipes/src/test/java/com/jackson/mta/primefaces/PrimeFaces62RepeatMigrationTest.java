package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies the catalog-facing PrimeFaces62RepeatMigration performs its advertised transformation. */
class PrimeFaces62RepeatMigrationTest implements RewriteTest {
    @Test void transformsRepresentativeMarkup() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62RepeatMigration()),
            xml(
                """
                <h:body xmlns:h="http://java.sun.com/jsf/html" xmlns:p="http://primefaces.org/ui"><p:repeat value="#{b.items}" var="i"><h:outputText value="#{i}"/></p:repeat></h:body>
                """,
                """
                <h:body xmlns:h="http://java.sun.com/jsf/html" xmlns:p="http://primefaces.org/ui" xmlns:ui="jakarta.faces.facelets"><ui:repeat value="#{b.items}" var="i"><h:outputText value="#{i}"/></ui:repeat></h:body>
                """));
    }
}
