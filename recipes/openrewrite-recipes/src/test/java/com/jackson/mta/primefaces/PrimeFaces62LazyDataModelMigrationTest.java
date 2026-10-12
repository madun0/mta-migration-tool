package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/** Verifies the catalog-facing LazyDataModel recipe changes the legacy load signature. */
class PrimeFaces62LazyDataModelMigrationTest implements RewriteTest {
    private static final String LEGACY = """
            package org.primefaces.model;
            import java.util.List; import java.util.Map;
            public abstract class LazyDataModel<T> {
                public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String,Object> filters) { return null; }
                public void setRowCount(int count) {}
            }
            """;
    private static final String ORDER = "package org.primefaces.model; public enum SortOrder { ASCENDING, DESCENDING, UNSORTED }";
    private static final String SORT = "package org.primefaces.model; public class SortMeta {}";

    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new PrimeFaces62LazyDataModelMigration())
            .parser(JavaParser.fromJavaVersion().dependsOn(LEGACY, ORDER, SORT))
            .afterTypeValidationOptions(TypeValidation.builder().methodInvocations(false).identifiers(false).methodDeclarations(false).build());
    }

    @Test void migratesLegacyLoadSignature() {
        rewriteRun(java(
            """
            import java.util.List;
            import java.util.Map;
            import org.primefaces.model.LazyDataModel;
            import org.primefaces.model.SortOrder;
            class Model extends LazyDataModel<String> {
                @Override public List<String> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String,Object> filters) {
                    return List.of();
                }
            }
            """,
            """
            import java.util.Collections;
            import java.util.List;
            import java.util.Map;
            import java.util.stream.Collectors;

            import org.primefaces.model.FilterMeta;
            import org.primefaces.model.LazyDataModel;
            import org.primefaces.model.SortMeta;
            import org.primefaces.model.SortOrder;

            class Model extends LazyDataModel<String> {
                @Override
                public List<String> load(int first, int pageSize, Map<String, SortMeta> __mtaSortBy, Map<String, FilterMeta> __mtaFilterBy) {
                    SortMeta __mtaPrimarySort = __mtaSortBy == null || __mtaSortBy.isEmpty() ? null : __mtaSortBy.values().iterator().next();
                    String sortField = __mtaPrimarySort == null ? null : __mtaPrimarySort.getField();
                    SortOrder sortOrder = __mtaPrimarySort == null ? SortOrder.UNSORTED : __mtaPrimarySort.getOrder();
                    Map<String, Object> filters = __mtaFilterBy == null ? Collections.emptyMap() : __mtaFilterBy.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getFilterValue()));
                    return List.of();
                }
            }
            """));
    }
}
