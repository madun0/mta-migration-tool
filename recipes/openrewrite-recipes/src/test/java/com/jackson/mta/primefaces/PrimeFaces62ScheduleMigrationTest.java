package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies the catalog-facing PrimeFaces62ScheduleMigration performs its advertised transformation. */
class PrimeFaces62ScheduleMigrationTest implements RewriteTest {
    @Test void transformsRepresentativeMarkup() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62ScheduleMigration()),
            xml(
                """
                <p:schedule xmlns:p="http://primefaces.org/ui" view="agendaDay"/>
                """,
                """
                <p:schedule xmlns:p="http://primefaces.org/ui" view="timeGridDay"/>
                """));
    }
}
