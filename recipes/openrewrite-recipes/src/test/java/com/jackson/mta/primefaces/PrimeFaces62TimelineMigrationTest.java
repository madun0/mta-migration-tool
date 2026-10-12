package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies the catalog-facing PrimeFaces62TimelineMigration performs its advertised transformation. */
class PrimeFaces62TimelineMigrationTest implements RewriteTest {
    @Test void transformsRepresentativeMarkup() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62TimelineMigration()),
            xml(
                """
                <p:timeline xmlns:p="http://primefaces.org/ui" axisOnTop="false"/>
                """,
                """
                <p:timeline xmlns:p="http://primefaces.org/ui" orientationAxis="bottom"/>
                """));
    }
}
