package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies the catalog-facing PrimeFaces62AutoUpdateMigration performs its advertised transformation. */
class PrimeFaces62AutoUpdateMigrationTest implements RewriteTest {
    @Test void transformsRepresentativeMarkup() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62AutoUpdateMigration()),
            xml(
                """
                <p:messages xmlns:p="http://primefaces.org/ui" autoUpdate="false"/>
                """,
                """
                <p:messages xmlns:p="http://primefaces.org/ui"/>
                """));
    }
}
