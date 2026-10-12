package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies the catalog-facing PrimeFaces62OverlayPanelMigration performs its advertised transformation. */
class PrimeFaces62OverlayPanelMigrationTest implements RewriteTest {
    @Test void transformsRepresentativeMarkup() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62OverlayPanelMigration()),
            xml(
                """
                <p:overlayPanel xmlns:p="http://primefaces.org/ui" appendToBody="true"/>
                """,
                """
                <p:overlayPanel xmlns:p="http://primefaces.org/ui" appendTo="@(body)"/>
                """));
    }
}
