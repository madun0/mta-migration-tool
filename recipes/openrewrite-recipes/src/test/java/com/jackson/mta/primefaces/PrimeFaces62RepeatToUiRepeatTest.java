package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies p:repeat is renamed and retains a valid ui namespace declaration. */
class PrimeFaces62RepeatToUiRepeatTest implements RewriteTest {

    @Test
    void migratesRepeatAndAddsUiNamespace() {
        rewriteRun(
                spec -> spec.recipe(new PrimeFaces62RepeatToUiRepeat()),
                xml(
                        """
                        <h:body xmlns:h="http://java.sun.com/jsf/html" xmlns:p="http://primefaces.org/ui">
                            <p:repeat value="#{bean.items}" var="item">
                                <h:outputText value="#{item}"/>
                            </p:repeat>
                        </h:body>
                        """,
                        """
                        <h:body xmlns:h="http://java.sun.com/jsf/html" xmlns:p="http://primefaces.org/ui" xmlns:ui="jakarta.faces.facelets">
                            <ui:repeat value="#{bean.items}" var="item">
                                <h:outputText value="#{item}"/>
                            </ui:repeat>
                        </h:body>
                        """));
    }
}
