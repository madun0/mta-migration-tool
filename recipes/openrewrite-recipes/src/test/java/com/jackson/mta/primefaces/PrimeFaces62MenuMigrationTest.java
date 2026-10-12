package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/** Verifies the catalog-facing menu composite moves DynamicMenuModel to BaseMenuModel. */
class PrimeFaces62MenuMigrationTest implements RewriteTest {
    private static final String OLD = "package org.primefaces.model.menu; public class DynamicMenuModel {}";
    private static final String NEW = "package org.primefaces.model.menu; public class BaseMenuModel {}";

    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new PrimeFaces62MenuMigration())
            .parser(JavaParser.fromJavaVersion().dependsOn(OLD, NEW))
            .afterTypeValidationOptions(TypeValidation.builder().identifiers(false).build());
    }

    @Test void migratesDynamicMenuModelType() {
        rewriteRun(java(
            """
            import org.primefaces.model.menu.DynamicMenuModel;
            class Example { DynamicMenuModel model; }
            """,
            """
            import org.primefaces.model.menu.BaseMenuModel;

            class Example { BaseMenuModel model; }
            """));
    }
}
