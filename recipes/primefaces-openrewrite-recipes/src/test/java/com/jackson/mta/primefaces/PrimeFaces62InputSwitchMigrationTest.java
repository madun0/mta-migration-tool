package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies the catalog-facing PrimeFaces62InputSwitchMigration performs its advertised transformation. */
class PrimeFaces62InputSwitchMigrationTest implements RewriteTest {
    @Test void transformsRepresentativeMarkup() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62InputSwitchMigration()),
            xml(
                """
                <p:inputSwitch xmlns:p="http://primefaces.org/ui" value="#{b.enabled}"/>
                """,
                """
                <p:toggleSwitch xmlns:p="http://primefaces.org/ui" value="#{b.enabled}"/>
                """));
    }
}
