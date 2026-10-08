package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/** Verifies the catalog-facing RequestContext composite performs a real migration. */
class PrimeFaces62RequestContextMigrationTest implements RewriteTest {
    private static final String LEGACY = """
            package org.primefaces.context;
            public class RequestContext {
                public static RequestContext getCurrentInstance() { return null; }
                public void execute(String script) {}
                public boolean isAjaxRequest() { return false; }
            }
            """;
    private static final String MODERN = """
            package org.primefaces;
            public class PrimeFaces {
                public static PrimeFaces current() { return null; }
                public void executeScript(String script) {}
                public boolean isAjaxRequest() { return false; }
            }
            """;

    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new PrimeFaces62RequestContextMigration())
            .parser(JavaParser.fromJavaVersion().dependsOn(LEGACY, MODERN))
            .afterTypeValidationOptions(TypeValidation.builder().methodInvocations(false).identifiers(false).build());
    }

    @Test void migratesRequestContextExecute() {
        rewriteRun(java(
            """
            import org.primefaces.context.RequestContext;
            class Example { void run() { RequestContext.getCurrentInstance().execute("go()"); } }
            """,
            """
            import org.primefaces.PrimeFaces;

            class Example { void run() {
                PrimeFaces.current().executeScript("go()"); } }
            """));
    }

    @Test void migratesRequestContextIsAjaxRequest() {
        rewriteRun(java(
            """
            import org.primefaces.context.RequestContext;
            class Example { boolean ajax() { return RequestContext.getCurrentInstance().isAjaxRequest(); } }
            """,
            """
            import org.primefaces.PrimeFaces;

            class Example { boolean ajax() { return PrimeFaces.current().isAjaxRequest(); } }
            """
        ));
    }
}
