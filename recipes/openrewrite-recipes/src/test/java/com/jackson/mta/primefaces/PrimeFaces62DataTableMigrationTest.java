package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/** Verifies the catalog-facing DataTable composite renames removed filtering APIs. */
class PrimeFaces62DataTableMigrationTest implements RewriteTest {
    private static final String LEGACY = """
            package org.primefaces.component.datatable;
            import java.util.Map;
            public class DataTable { public Map<String,Object> getFilters() { return null; } }
            """;

    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new PrimeFaces62DataTableMigration())
            .parser(JavaParser.fromJavaVersion().dependsOn(LEGACY))
            .afterTypeValidationOptions(TypeValidation.builder().methodInvocations(false).build());
    }

    @Test void migratesGetFilters() {
        rewriteRun(java(
            """
            import java.util.Map;
            import org.primefaces.component.datatable.DataTable;
            class Example { Map<String,Object> read(DataTable table) { return table.getFilters(); } }
            """,
            """
            import java.util.Map;
            import org.primefaces.component.datatable.DataTable;
            class Example { Map<String,Object> read(DataTable table) { return table.getFilterBy(); } }
            """));
    }
}
