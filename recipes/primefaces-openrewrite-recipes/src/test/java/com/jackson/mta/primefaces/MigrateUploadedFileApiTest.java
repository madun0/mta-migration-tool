package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Validates the complete UploadedFile type and method migration using explicit
 * source/target API stubs so the test does not depend on Maven runtime JAR names.
 */
class MigrateUploadedFileApiTest implements RewriteTest {

    private static final String LEGACY_UPLOADED_FILE = """
            package org.primefaces.model;

            import java.io.InputStream;

            public interface UploadedFile {
                byte[] getContents();
                InputStream getInputstream();
            }
            """;

    private static final String MODERN_UPLOADED_FILE = """
            package org.primefaces.model.file;

            import java.io.InputStream;

            public interface UploadedFile {
                byte[] getContent();
                InputStream getInputStream();
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateUploadedFileApi())
            .parser(JavaParser.fromJavaVersion().dependsOn(
                    LEGACY_UPLOADED_FILE,
                    MODERN_UPLOADED_FILE))
            .afterTypeValidationOptions(TypeValidation.builder()
                    .methodInvocations(false)
                    .build());
    }

    @Test
    void migratesUploadedFileImportTypeAndMethods() {
        rewriteRun(
            java(
                """
                import org.primefaces.model.UploadedFile;

                class CustomerBean {
                    private UploadedFile file;

                    byte[] content() {
                        return file.getContents();
                    }

                    java.io.InputStream input() {
                        return file.getInputstream();
                    }
                }
                """,
                """
                import org.primefaces.model.file.UploadedFile;

                class CustomerBean {
                    private UploadedFile file;

                    byte[] content() {
                        return file.getContent();
                    }

                    java.io.InputStream input() {
                        return file.getInputStream();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesMethodParameterAndReturnTypes() {
        rewriteRun(
            java(
                """
                import org.primefaces.model.UploadedFile;

                class UploadService {
                    UploadedFile echo(UploadedFile file) {
                        return file;
                    }
                }
                """,
                """
                import org.primefaces.model.file.UploadedFile;

                class UploadService {
                    UploadedFile echo(UploadedFile file) {
                        return file;
                    }
                }
                """
            )
        );
    }
}
