package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/** Verifies the catalog-facing StreamedContent composite transforms legacy ByteArrayContent. */
class PrimeFaces62StreamedContentMigrationTest implements RewriteTest {
    private static final String LEGACY = """
            package org.primefaces.model;
            public class ByteArrayContent { public ByteArrayContent(byte[] data, String type, String name) {} }
            """;

    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new PrimeFaces62StreamedContentMigration())
            .parser(JavaParser.fromJavaVersion().dependsOn(LEGACY))
            .afterTypeValidationOptions(TypeValidation.builder().methodInvocations(false).identifiers(false).build());
    }

    @Test void migratesByteArrayContentToBuilder() {
        rewriteRun(java(
            """
            import org.primefaces.model.ByteArrayContent;
            class Example { Object create(byte[] bytes) { return new ByteArrayContent(bytes, "text/plain", "a.txt"); } }
            """,
            """
            import org.primefaces.model.DefaultStreamedContent;

            import java.io.ByteArrayInputStream;

            class Example { Object create(byte[] bytes) { return DefaultStreamedContent.builder().stream(() -> new ByteArrayInputStream(bytes)).contentType("text/plain").name("a.txt").build(); } }
            """));
    }
}
