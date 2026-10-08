package com.jackson.mta.primefaces;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;

/**
 * Optional follow-up recipe.
 *
 * Adds count(Map<String, FilterMeta>) returning 0 only when:
 *  - the class extends LazyDataModel,
 *  - it does not already declare count(...), and
 *  - its load method calls setRowCount(...).
 *
 * Modern PrimeFaces documents count() == 0 as the supported pattern when
 * counting and page loading are performed together in load(), provided load()
 * manages the row count itself.
 *
 * This recipe intentionally does not invent a database COUNT query.
 */
public class AddLazyDataModelCountBridge extends Recipe {

    private static final String LAZY_DATA_MODEL = "org.primefaces.model.LazyDataModel";
    private static final String FILTER_META = "org.primefaces.model.FilterMeta";

    private static final String FILTER_META_STUB = """
            package org.primefaces.model;
            public class FilterMeta {
            }
            """;

    @Override
    public String getDisplayName() {
        return "Add conservative LazyDataModel count bridge";
    }

    @Override
    public String getDescription() {
        return "Adds count(filterBy) returning 0 only for LazyDataModel classes whose load implementation already calls setRowCount().";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl,
                                                            ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

                if (cd.getType() == null || !TypeUtils.isAssignableTo(LAZY_DATA_MODEL, cd.getType())) {
                    return cd;
                }

                if (alreadyHasCount(cd) || !loadCallsSetRowCount(cd)) {
                    return cd;
                }

                JavaTemplate countMethod = JavaTemplate.builder(
                                "@Override\n" +
                                "public int count(Map<String, FilterMeta> filterBy) {\n" +
                                "    return 0;\n" +
                                "}")
                        .imports("java.util.Map", FILTER_META)
                        .javaParser(JavaParser.fromJavaVersion().dependsOn(FILTER_META_STUB))
                        .contextSensitive()
                        .build();

                maybeAddImport("java.util.Map");
                maybeAddImport(FILTER_META);

                return countMethod.apply(
                        updateCursor(cd),
                        cd.getBody().getCoordinates().lastStatement());
            }

            private boolean alreadyHasCount(J.ClassDeclaration cd) {
                for (Statement statement : cd.getBody().getStatements()) {
                    if (statement instanceof J.MethodDeclaration) {
                        J.MethodDeclaration method = (J.MethodDeclaration) statement;
                        if ("count".equals(method.getSimpleName())
                                && method.getParameters().size() == 1) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private boolean loadCallsSetRowCount(J.ClassDeclaration cd) {
                final boolean[] found = {false};

                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method,
                                                                      Integer p) {
                        if (!"load".equals(method.getSimpleName())) {
                            return method;
                        }
                        return super.visitMethodDeclaration(method, p);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method,
                                                                    Integer p) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, p);
                        if ("setRowCount".equals(m.getSimpleName())) {
                            found[0] = true;
                        }
                        return m;
                    }
                }.visit(cd, 0);

                return found[0];
            }
        };
    }
}
