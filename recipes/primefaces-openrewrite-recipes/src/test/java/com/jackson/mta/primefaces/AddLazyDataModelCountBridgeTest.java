package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Verifies the OpenRewrite behavior and recipe contract of {@link AddLazyDataModelCountBridge}.
 */
class AddLazyDataModelCountBridgeTest implements RewriteTest {

    private static final String MODERNISH_PRIMEFACES = """
            package org.primefaces.model;

            import java.util.List;
            import java.util.Map;

            public abstract class LazyDataModel<T> {
                public abstract int count(Map<String, FilterMeta> filterBy);
                public abstract List<T> load(int first, int pageSize,
                                             Map<String, SortMeta> sortBy,
                                             Map<String, FilterMeta> filterBy);
                public void setRowCount(int rowCount) {
                }
            }
            """;

    private static final String SORT_META = """
            package org.primefaces.model;
            public class SortMeta {
            }
            """;

    private static final String FILTER_META = """
            package org.primefaces.model;
            public class FilterMeta {
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddLazyDataModelCountBridge())
            .parser(JavaParser.fromJavaVersion()
                    .dependsOn(MODERNISH_PRIMEFACES, SORT_META, FILTER_META))
            .afterTypeValidationOptions(TypeValidation.builder()
                    .methodDeclarations(false)
                    .build());
    }

    @Test
    void addsCountBridgeWhenLoadOwnsRowCount() {
        rewriteRun(
            java(
                """
                import java.util.List;
                import java.util.Map;
                import org.primefaces.model.FilterMeta;
                import org.primefaces.model.LazyDataModel;
                import org.primefaces.model.SortMeta;

                class CustomerModel extends LazyDataModel<String> {
                    @Override
                    public List<String> load(int first, int pageSize,
                                             Map<String, SortMeta> sortBy,
                                             Map<String, FilterMeta> filterBy) {
                        setRowCount(100);
                        return List.of();
                    }
                }
                """,
                """
                import java.util.List;
                import java.util.Map;
                import org.primefaces.model.FilterMeta;
                import org.primefaces.model.LazyDataModel;
                import org.primefaces.model.SortMeta;

                class CustomerModel extends LazyDataModel<String> {
                    @Override
                    public List<String> load(int first, int pageSize,
                                             Map<String, SortMeta> sortBy,
                                             Map<String, FilterMeta> filterBy) {
                        setRowCount(100);
                        return List.of();
                    }

                    @Override
                    public int count(Map<String, FilterMeta> filterBy) {
                        return 0;
                    }
                }
                """
            )
        );
    }

    @Test
    void doesNotInventCountWhenLoadDoesNotSetRowCount() {
        rewriteRun(
            java(
                """
                import java.util.List;
                import java.util.Map;
                import org.primefaces.model.FilterMeta;
                import org.primefaces.model.LazyDataModel;
                import org.primefaces.model.SortMeta;

                abstract class CustomerModel extends LazyDataModel<String> {
                    @Override
                    public List<String> load(int first, int pageSize,
                                             Map<String, SortMeta> sortBy,
                                             Map<String, FilterMeta> filterBy) {
                        return List.of();
                    }
                }
                """
            )
        );
    }
}
