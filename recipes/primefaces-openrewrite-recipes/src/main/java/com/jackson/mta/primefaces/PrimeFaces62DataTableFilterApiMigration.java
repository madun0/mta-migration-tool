package com.jackson.mta.primefaces;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.time.Duration;

/**
 * PrimeFaces 8 removed DataTable#getFilters()/setFilters() in favor of
 * getFilterBy()/setFilterBy().
 *
 * This recipe performs the documented method-name migration. It intentionally
 * does not rewrite surrounding Map<String,Object> declarations to FilterMeta:
 * applications that inspect individual filter values need semantic migration
 * because modern filtering represents values through FilterMeta.
 */
public class PrimeFaces62DataTableFilterApiMigration extends Recipe {

    private static final String DATA_TABLE =
            "org.primefaces.component.datatable.DataTable";

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces DataTable filter API";
    }

    @Override
    public String getDescription() {
        return "Renames removed DataTable getFilters/setFilters calls to getFilterBy/setFilterBy while leaving FilterMeta value semantics for review.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            private final MethodMatcher getFilters =
                    new MethodMatcher(DATA_TABLE + " getFilters()");

            private final MethodMatcher setFilters =
                    new MethodMatcher(DATA_TABLE + " setFilters(java.util.Map)");

            @Override
            public J.MethodInvocation visitMethodInvocation(
                    J.MethodInvocation method,
                    ExecutionContext ctx) {

                J.MethodInvocation m =
                        super.visitMethodInvocation(method, ctx);

                if (getFilters.matches(m)) {
                    return m.withName(
                            m.getName().withSimpleName("getFilterBy"));
                }

                if (setFilters.matches(m)) {
                    return m.withName(
                            m.getName().withSimpleName("setFilterBy"));
                }

                return m;
            }
        };
    }
}
