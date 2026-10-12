package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies the catalog-facing PrimeFaces62RowsPerPageMigration performs its advertised transformation. */
class PrimeFaces62RowsPerPageMigrationTest implements RewriteTest {
    @Test void transformsRepresentativeMarkup() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62RowsPerPageMigration()),
            xml(
                """
                <p:dataTable xmlns:p="http://primefaces.org/ui" rowsPerPageTemplate="*"/>
                """,
                """
                <p:dataTable xmlns:p="http://primefaces.org/ui" rowsPerPageTemplate="{ShowAll|'*'}"/>
                """));
    }
}
