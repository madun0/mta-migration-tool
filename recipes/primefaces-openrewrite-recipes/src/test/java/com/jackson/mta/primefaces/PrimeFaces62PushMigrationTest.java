package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies removed PrimeFaces Push markup is annotated with the redesign requirement. */
class PrimeFaces62PushMigrationTest implements RewriteTest {
    @Test void marksSocketForWebSocketRedesign() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62PushMigration()),
            xml(
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets" xmlns:p="http://primefaces.org/ui">
                    <p:socket channel="/events"/>
                </ui:composition>
                """,
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets" xmlns:p="http://primefaces.org/ui">
                    <!--PF16 migration review: PrimeFaces Push was removed in PrimeFaces 7. Replace p:socket and org.primefaces.push APIs with Jakarta Faces f:websocket/Jakarta WebSocket, OmniFaces, or an application-specific messaging solution.-->
                    <p:socket channel="/events"/>
                </ui:composition>
                """));
    }
}
