package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/** Verifies the catalog-facing FileUpload composite performs the Java API migration. */
class PrimeFaces62FileUploadMigrationTest implements RewriteTest {
    private static final String OLD = """
            package org.primefaces.model;
            public interface UploadedFile { byte[] getContents(); }
            """;
    private static final String NEW = """
            package org.primefaces.model.file;
            public interface UploadedFile { byte[] getContent(); }
            """;

    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new PrimeFaces62FileUploadMigration())
            .parser(JavaParser.fromJavaVersion().dependsOn(OLD, NEW))
            .afterTypeValidationOptions(TypeValidation.builder().methodInvocations(false).build());
    }

    @Test void migratesUploadedFilePackageAndMethod() {
        rewriteRun(java(
            """
            import org.primefaces.model.UploadedFile;
            class Example { byte[] read(UploadedFile file) { return file.getContents(); } }
            """,
            """
            import org.primefaces.model.file.UploadedFile;

            class Example { byte[] read(UploadedFile file) { return file.getContent(); } }
            """));
    }
}
