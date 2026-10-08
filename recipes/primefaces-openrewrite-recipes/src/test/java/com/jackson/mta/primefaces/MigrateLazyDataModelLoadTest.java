package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Tests migration of legacy LazyDataModel load signatures.
 */
class MigrateLazyDataModelLoadTest implements RewriteTest {

    private static final String LEGACY_PRIMEFACES = """
            package org.primefaces.model;

            import java.util.List;
            import java.util.Map;

            public abstract class LazyDataModel<T> {
                public List<T> load(int first, int pageSize, String sortField,
                                    SortOrder sortOrder, Map<String, Object> filters) {
                    throw new UnsupportedOperationException();
                }

                public List<T> load(int first, int pageSize, List<SortMeta> multiSortMeta,
                                    Map<String, Object> filters) {
                    throw new UnsupportedOperationException();
                }

                public void setRowCount(int rowCount) {
                }
            }
            """;

    private static final String LEGACY_SORT_ORDER = """
            package org.primefaces.model;
            public enum SortOrder {
                ASCENDING, DESCENDING, UNSORTED
            }
            """;

    private static final String LEGACY_SORT_META = """
            package org.primefaces.model;
            public class SortMeta {
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateLazyDataModelLoad())
            .parser(JavaParser.fromJavaVersion()
                    .dependsOn(LEGACY_PRIMEFACES, LEGACY_SORT_ORDER, LEGACY_SORT_META))
            .afterTypeValidationOptions(TypeValidation.builder()
                    .methodInvocations(false)
                    .identifiers(false)
                    .methodDeclarations(false)
                    .build());
    }

    @Test
    void migratesSingleSortSignatureAndPreservesOldVariables() {
        rewriteRun(
            java(
                """
                import java.util.List;
                import java.util.Map;
                import org.primefaces.model.LazyDataModel;
                import org.primefaces.model.SortOrder;

                class CustomerModel extends LazyDataModel<String> {
                    @Override
                    public List<String> load(int first, int pageSize, String sortField,
                                             SortOrder sortOrder, Map<String, Object> filters) {
                        if (sortField != null && sortOrder == SortOrder.ASCENDING) {
                            System.out.println(sortField);
                        }
                        System.out.println(filters);
                        setRowCount(100);
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

                class CustomerModel extends LazyDataModel<String> {
                    @Override
                    public List<String> load(int first, int pageSize, Map<String, SortMeta> __mtaSortBy, Map<String, FilterMeta> __mtaFilterBy) {
                        SortMeta __mtaPrimarySort = __mtaSortBy == null || __mtaSortBy.isEmpty() ? null : __mtaSortBy.values().iterator().next();
                        String sortField = __mtaPrimarySort == null ? null : __mtaPrimarySort.getField();
                        SortOrder sortOrder = __mtaPrimarySort == null ? SortOrder.UNSORTED : __mtaPrimarySort.getOrder();
                        Map<String, Object> filters = __mtaFilterBy == null ? Collections.emptyMap() : __mtaFilterBy.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getFilterValue()));
                        if (sortField != null && sortOrder == SortOrder.ASCENDING) {
                            System.out.println(sortField);
                        }
                        System.out.println(filters);
                        setRowCount(100);
                        return List.of();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesLegacyMultiSortSignature() {
        rewriteRun(
            java(
                """
                import java.util.List;
                import java.util.Map;
                import org.primefaces.model.LazyDataModel;
                import org.primefaces.model.SortMeta;

                class CustomerModel extends LazyDataModel<String> {
                    @Override
                    public List<String> load(int first, int pageSize, List<SortMeta> multiSortMeta,
                                             Map<String, Object> filters) {
                        System.out.println(multiSortMeta.size());
                        System.out.println(filters);
                        return List.of();
                    }
                }
                """,
                """
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.List;
                import java.util.Map;
                import java.util.stream.Collectors;

                import org.primefaces.model.FilterMeta;
                import org.primefaces.model.LazyDataModel;
                import org.primefaces.model.SortMeta;

                class CustomerModel extends LazyDataModel<String> {
                    @Override
                    public List<String> load(int first, int pageSize, Map<String, SortMeta> __mtaSortBy, Map<String, FilterMeta> __mtaFilterBy) {
                        List<SortMeta> multiSortMeta = __mtaSortBy == null ? Collections.emptyList() : new ArrayList<>(__mtaSortBy.values());
                        Map<String, Object> filters = __mtaFilterBy == null ? Collections.emptyMap() : __mtaFilterBy.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getFilterValue()));
                        System.out.println(multiSortMeta.size());
                        System.out.println(filters);
                        return List.of();
                    }
                }
                """
            )
        );
    }
}
