package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link PrimeFaces62DataTableFilterApiMigration}.
 */
class PrimeFaces62DataTableFilterApiMigrationTest implements RewriteTest {

    private static final String LEGACY_DATA_TABLE = """
            package org.primefaces.component.datatable;

            import java.util.Map;

            public class DataTable {
                public Map<String, Object> getFilters() {
                    return null;
                }

                public void setFilters(Map<String, Object> filters) {
                }
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PrimeFaces62DataTableFilterApiMigration())
            .parser(JavaParser.fromJavaVersion()
                    .dependsOn(LEGACY_DATA_TABLE))
            .afterTypeValidationOptions(TypeValidation.builder()
                    .methodInvocations(false)
                    .build());
    }

    @Test
    void renamesRemovedFilterMethods() {
        rewriteRun(
            java(
                """
                import java.util.Map;
                import org.primefaces.component.datatable.DataTable;

                class Example {
                    Map<String, Object> read(DataTable table) {
                        return table.getFilters();
                    }

                    void write(DataTable table, Map<String, Object> filters) {
                        table.setFilters(filters);
                    }
                }
                """,
                """
                import java.util.Map;
                import org.primefaces.component.datatable.DataTable;

                class Example {
                    Map<String, Object> read(DataTable table) {
                        return table.getFilterBy();
                    }

                    void write(DataTable table, Map<String, Object> filters) {
                        table.setFilterBy(filters);
                    }
                }
                """
            )
        );
    }
}
