package com.jackson.mta.primefaces;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.List;

/**
 * Migrates the two PrimeFaces 6.2 LazyDataModel#load override shapes to the
 * Map<String, SortMeta> / Map<String, FilterMeta> contract introduced in
 * later PrimeFaces versions.
 *
 * The recipe deliberately preserves the original method body by adding
 * compatibility local variables at the start of the method. This means
 * existing repository / SQL / Criteria code can continue to use the old
 * sortField, sortOrder, filters, or multiSortMeta variables while the method
 * satisfies the modern PrimeFaces signature.
 *
 * It does NOT automatically add count(Map<String, FilterMeta>). That is a
 * separate concern because the correct count implementation is application
 * specific.
 */
public class MigrateLazyDataModelLoad extends Recipe {

    private static final String LAZY_DATA_MODEL = "org.primefaces.model.LazyDataModel";
    private static final String SORT_ORDER = "org.primefaces.model.SortOrder";
    private static final String SORT_META = "org.primefaces.model.SortMeta";
    private static final String FILTER_META = "org.primefaces.model.FilterMeta";

    private static final String TARGET_META_STUBS = """
            package org.primefaces.model;

            public enum SortOrder {
                ASCENDING, DESCENDING, UNSORTED
            }
            """;

    private static final String TARGET_SORT_META_STUB = """
            package org.primefaces.model;

            public class SortMeta {
                public String getField() { return null; }
                public SortOrder getOrder() { return SortOrder.UNSORTED; }
            }
            """;

    private static final String TARGET_FILTER_META_STUB = """
            package org.primefaces.model;

            public class FilterMeta {
                public Object getFilterValue() { return null; }
            }
            """;

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces LazyDataModel load signatures";
    }

    @Override
    public String getDescription() {
        return "Migrates PrimeFaces 6.2 LazyDataModel load overrides to SortMeta/FilterMeta maps while preserving the old body through compatibility locals.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(10);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
                                                              ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (!"load".equals(m.getSimpleName()) || m.getBody() == null) {
                    return m;
                }

                if (!isInsideLazyDataModelSubclass()) {
                    return m;
                }

                List<Statement> params = m.getParameters();

                if (isLegacySingleSort(params)) {
                    return migrateSingleSort(m, params, ctx);
                }

                if (isLegacyMultiSort(params)) {
                    return migrateMultiSort(m, params, ctx);
                }

                return m;
            }

            private boolean isInsideLazyDataModelSubclass() {
                J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosing == null || enclosing.getType() == null) {
                    return false;
                }
                return TypeUtils.isAssignableTo(LAZY_DATA_MODEL, enclosing.getType());
            }

            private boolean isLegacySingleSort(List<Statement> params) {
                if (params.size() != 5) {
                    return false;
                }
                return isType(params.get(0), "int")
                        && isType(params.get(1), "int")
                        && isType(params.get(2), "java.lang.String")
                        && isType(params.get(3), SORT_ORDER)
                        && isType(params.get(4), "java.util.Map");
            }

            private boolean isLegacyMultiSort(List<Statement> params) {
                if (params.size() != 4) {
                    return false;
                }
                return isType(params.get(0), "int")
                        && isType(params.get(1), "int")
                        && isType(params.get(2), "java.util.List")
                        && isType(params.get(3), "java.util.Map");
            }

            private J.MethodDeclaration migrateSingleSort(J.MethodDeclaration m,
                                                          List<Statement> params,
                                                          ExecutionContext ctx) {
                String first = parameterName(params.get(0));
                String pageSize = parameterName(params.get(1));
                String oldSortField = parameterName(params.get(2));
                String oldSortOrder = parameterName(params.get(3));
                String oldFilters = parameterName(params.get(4));

                if (first == null || pageSize == null || oldSortField == null
                        || oldSortOrder == null || oldFilters == null) {
                    return m;
                }

                final String newSortBy = "__mtaSortBy";
                final String newFilterBy = "__mtaFilterBy";
                final String selectedSort = "__mtaPrimarySort";

                JavaParser.Builder<?, ?> parser = targetParser();

                JavaTemplate parameters = JavaTemplate.builder(
                                "int " + first + ", int " + pageSize +
                                ", Map<String, SortMeta> " + newSortBy +
                                ", Map<String, FilterMeta> " + newFilterBy)
                        .imports("java.util.Map", SORT_META, FILTER_META)
                        .javaParser(parser)
                        .contextSensitive()
                        .build();

                m = parameters.apply(
                        updateCursor(m),
                        m.getCoordinates().replaceParameters());

                String preludeSource =
                        "SortMeta " + selectedSort + " = " + newSortBy +
                        " == null || " + newSortBy + ".isEmpty() ? null : " +
                        newSortBy + ".values().iterator().next();\n" +
                        "String " + oldSortField + " = " + selectedSort +
                        " == null ? null : " + selectedSort + ".getField();\n" +
                        "SortOrder " + oldSortOrder + " = " + selectedSort +
                        " == null ? SortOrder.UNSORTED : " + selectedSort + ".getOrder();\n" +
                        "Map<String, Object> " + oldFilters + " = " + newFilterBy +
                        " == null ? Collections.emptyMap() : " + newFilterBy +
                        ".entrySet().stream().collect(Collectors.toMap(" +
                        "Map.Entry::getKey, e -> e.getValue().getFilterValue()));";

                JavaTemplate prelude = JavaTemplate.builder(preludeSource)
                        .imports("java.util.Map",
                                "java.util.Collections",
                                "java.util.stream.Collectors",
                                SORT_ORDER, SORT_META, FILTER_META)
                        .javaParser(targetParser())
                        .contextSensitive()
                        .build();

                J.Block body = m.getBody();
                Cursor methodCursor = updateCursor(m);
                J.Block migratedBody = prelude.apply(
                        new Cursor(methodCursor, body),
                        body.getCoordinates().firstStatement());

                maybeAddImport("java.util.Map");
                maybeAddImport("java.util.Collections");
                maybeAddImport("java.util.stream.Collectors");
                maybeAddImport(SORT_ORDER);
                maybeAddImport(SORT_META);
                maybeAddImport(FILTER_META);

                return maybeAutoFormat(m, m.withBody(migratedBody), ctx);
            }

            private J.MethodDeclaration migrateMultiSort(J.MethodDeclaration m,
                                                         List<Statement> params,
                                                         ExecutionContext ctx) {
                String first = parameterName(params.get(0));
                String pageSize = parameterName(params.get(1));
                String oldMultiSort = parameterName(params.get(2));
                String oldFilters = parameterName(params.get(3));

                if (first == null || pageSize == null || oldMultiSort == null || oldFilters == null) {
                    return m;
                }

                final String newSortBy = "__mtaSortBy";
                final String newFilterBy = "__mtaFilterBy";

                JavaTemplate parameters = JavaTemplate.builder(
                                "int " + first + ", int " + pageSize +
                                ", Map<String, SortMeta> " + newSortBy +
                                ", Map<String, FilterMeta> " + newFilterBy)
                        .imports("java.util.Map", SORT_META, FILTER_META)
                        .javaParser(targetParser())
                        .contextSensitive()
                        .build();

                m = parameters.apply(
                        updateCursor(m),
                        m.getCoordinates().replaceParameters());

                String preludeSource =
                        "List<SortMeta> " + oldMultiSort + " = " + newSortBy +
                        " == null ? Collections.emptyList() : new ArrayList<>(" +
                        newSortBy + ".values());\n" +
                        "Map<String, Object> " + oldFilters + " = " + newFilterBy +
                        " == null ? Collections.emptyMap() : " + newFilterBy +
                        ".entrySet().stream().collect(Collectors.toMap(" +
                        "Map.Entry::getKey, e -> e.getValue().getFilterValue()));";

                JavaTemplate prelude = JavaTemplate.builder(preludeSource)
                        .imports("java.util.List",
                                "java.util.Map",
                                "java.util.ArrayList",
                                "java.util.Collections",
                                "java.util.stream.Collectors",
                                SORT_META, FILTER_META)
                        .javaParser(targetParser())
                        .contextSensitive()
                        .build();

                J.Block body = m.getBody();
                Cursor methodCursor = updateCursor(m);
                J.Block migratedBody = prelude.apply(
                        new Cursor(methodCursor, body),
                        body.getCoordinates().firstStatement());

                maybeAddImport("java.util.List");
                maybeAddImport("java.util.Map");
                maybeAddImport("java.util.ArrayList");
                maybeAddImport("java.util.Collections");
                maybeAddImport("java.util.stream.Collectors");
                maybeAddImport(SORT_META);
                maybeAddImport(FILTER_META);

                return maybeAutoFormat(m, m.withBody(migratedBody), ctx);
            }

            private JavaParser.Builder<?, ?> targetParser() {
                return JavaParser.fromJavaVersion()
                        .dependsOn(TARGET_META_STUBS,
                                   TARGET_SORT_META_STUB,
                                   TARGET_FILTER_META_STUB);
            }

            private boolean isType(Statement parameter, String fqn) {
                if (!(parameter instanceof J.VariableDeclarations)) {
                    return false;
                }
                J.VariableDeclarations declarations = (J.VariableDeclarations) parameter;
                JavaType type = declarations.getTypeExpression() == null
                        ? null
                        : declarations.getTypeExpression().getType();

                if ("int".equals(fqn)) {
                    return type == JavaType.Primitive.Int;
                }
                return TypeUtils.isOfClassType(type, fqn);
            }

            private String parameterName(Statement parameter) {
                if (!(parameter instanceof J.VariableDeclarations)) {
                    return null;
                }
                J.VariableDeclarations declarations = (J.VariableDeclarations) parameter;
                if (declarations.getVariables().size() != 1) {
                    return null;
                }
                return declarations.getVariables().get(0).getSimpleName();
            }
        };
    }
}
