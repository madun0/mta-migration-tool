package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/** Verifies the catalog-facing JSON composite moves application imports. */
class PrimeFaces62JsonMigrationTest implements RewriteTest {
    private static final String OLD = "package org.primefaces.json; public class JSONObject {}";
    private static final String NEW = "package org.primefaces.shaded.json; public class JSONObject {}";

    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new PrimeFaces62JsonMigration())
            .parser(JavaParser.fromJavaVersion().dependsOn(OLD, NEW))
            .afterTypeValidationOptions(TypeValidation.builder().identifiers(false).build());
    }

    @Test void migratesPrimeFacesJsonPackage() {
        rewriteRun(java(
            """
            import org.primefaces.json.JSONObject;
            class Example { JSONObject value; }
            """,
            """
            import org.primefaces.shaded.json.JSONObject;
            class Example { JSONObject value; }
            """));
    }
}
