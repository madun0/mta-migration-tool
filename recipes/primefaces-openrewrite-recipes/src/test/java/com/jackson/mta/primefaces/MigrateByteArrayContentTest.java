package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Tests ByteArrayContent constructor migration and the no-argument safety
 * boundary.
 */
class MigrateByteArrayContentTest implements RewriteTest {

    private static final String PRIMEFACES_62_STUB = """
            package org.primefaces.model;

            public class ByteArrayContent {
                public ByteArrayContent() {}
                public ByteArrayContent(byte[] data) {}
                public ByteArrayContent(byte[] data, String contentType) {}
                public ByteArrayContent(byte[] data, String contentType, String name) {}
                public ByteArrayContent(byte[] data, String contentType, String name, Integer contentLength) {}
                public ByteArrayContent(byte[] data, String contentType, String name, String contentEncoding) {}
                public ByteArrayContent(byte[] data, String contentType, String name, String contentEncoding, Integer contentLength) {}
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateByteArrayContent())
            .parser(JavaParser.fromJavaVersion().dependsOn(PRIMEFACES_62_STUB))
            .afterTypeValidationOptions(TypeValidation.builder()
                    .methodInvocations(false)
                    .identifiers(false)
                    .build());
    }

    @Test
    void migratesBasicByteArrayContent() {
        rewriteRun(
            java(
                """
                import org.primefaces.model.ByteArrayContent;

                class Example {
                    Object create(byte[] bytes) {
                        return new ByteArrayContent(bytes, "application/pdf", "report.pdf");
                    }
                }
                """,
                """
                import org.primefaces.model.DefaultStreamedContent;

                import java.io.ByteArrayInputStream;

                class Example {
                    Object create(byte[] bytes) {
                        return DefaultStreamedContent.builder().stream(() -> new ByteArrayInputStream(bytes)).contentType("application/pdf").name("report.pdf").build();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesContentEncoding() {
        rewriteRun(
            java(
                """
                import org.primefaces.model.ByteArrayContent;

                class Example {
                    Object create(byte[] bytes) {
                        return new ByteArrayContent(bytes, "text/plain", "a.txt", "gzip");
                    }
                }
                """,
                """
                import org.primefaces.model.DefaultStreamedContent;

                import java.io.ByteArrayInputStream;

                class Example {
                    Object create(byte[] bytes) {
                        return DefaultStreamedContent.builder().stream(() -> new ByteArrayInputStream(bytes)).contentType("text/plain").name("a.txt").contentEncoding("gzip").build();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesNullableContentLengthToLong() {
        rewriteRun(
            java(
                """
                import org.primefaces.model.ByteArrayContent;

                class Example {
                    Object create(byte[] bytes, Integer length) {
                        return new ByteArrayContent(bytes, "application/octet-stream", "data.bin", length);
                    }
                }
                """,
                """
                import org.primefaces.model.DefaultStreamedContent;

                import java.io.ByteArrayInputStream;
                import java.util.Optional;

                class Example {
                    Object create(byte[] bytes, Integer length) {
                        return DefaultStreamedContent.builder().stream(() -> new ByteArrayInputStream(bytes)).contentType("application/octet-stream").name("data.bin").contentLength(Optional.ofNullable(length).map(Integer::longValue).orElse(null)).build();
                    }
                }
                """
            )
        );
    }

    @Test
    void leavesNoArgConstructionForManualReview() {
        rewriteRun(
            java(
                """
                import org.primefaces.model.ByteArrayContent;

                class Example {
                    Object create() {
                        return new ByteArrayContent();
                    }
                }
                """
            )
        );
    }
}
