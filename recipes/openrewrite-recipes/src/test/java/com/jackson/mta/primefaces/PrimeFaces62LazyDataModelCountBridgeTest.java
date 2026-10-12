package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/** Verifies the catalog-facing count bridge adds count only for row-count-owning models. */
class PrimeFaces62LazyDataModelCountBridgeTest implements RewriteTest {
    private static final String MODEL = """
            package org.primefaces.model;
            import java.util.List; import java.util.Map;
            public abstract class LazyDataModel<T> {
                public abstract int count(Map<String, FilterMeta> filterBy);
                public abstract List<T> load(int first, int pageSize, Map<String,SortMeta> sortBy, Map<String,FilterMeta> filterBy);
                public void setRowCount(int count) {}
            }
            """;
    private static final String SORT = "package org.primefaces.model; public class SortMeta {}";
    private static final String FILTER = "package org.primefaces.model; public class FilterMeta {}";

    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new PrimeFaces62LazyDataModelCountBridge())
            .parser(JavaParser.fromJavaVersion().dependsOn(MODEL, SORT, FILTER))
            .afterTypeValidationOptions(TypeValidation.builder().methodDeclarations(false).build());
    }

    @Test void addsCountBridge() {
        rewriteRun(java(
            """
            import java.util.List;
            import java.util.Map;
            import org.primefaces.model.FilterMeta;
            import org.primefaces.model.LazyDataModel;
            import org.primefaces.model.SortMeta;
            class Model extends LazyDataModel<String> {
                @Override public List<String> load(int first, int pageSize, Map<String,SortMeta> sortBy, Map<String,FilterMeta> filterBy) {
                    setRowCount(10); return List.of();
                }
            }
            """,
            """
            import java.util.List;
            import java.util.Map;
            import org.primefaces.model.FilterMeta;
            import org.primefaces.model.LazyDataModel;
            import org.primefaces.model.SortMeta;
            class Model extends LazyDataModel<String> {
                @Override public List<String> load(int first, int pageSize, Map<String,SortMeta> sortBy, Map<String,FilterMeta> filterBy) {
                    setRowCount(10); return List.of();
                }

                @Override
                public int count(Map<String, FilterMeta> filterBy) {
                    return 0;
                }
            }
            """));
    }
}
