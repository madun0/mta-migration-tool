package com.jackson.mta.foundation;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;

import java.time.Duration;
import java.util.List;

/**
 * Migrates the removed JSF {@code javax.faces.bean.ManagedBean} annotation to
 * CDI {@code jakarta.inject.Named}.
 *
 * <p>The recipe intentionally separates annotation-argument normalization from
 * the type migration. The first step converts JSF-specific attributes into a
 * form accepted by CDI {@code @Named}; the second step delegates the actual
 * type/import rewrite to OpenRewrite's {@link ChangeType}. This preserves type
 * attribution and ensures {@code jakarta.inject.Named} is imported correctly.</p>
 *
 * <p>An explicit JSF managed-bean name is preserved:</p>
 * <pre>
 * {@code @ManagedBean(name = "orders") -> @Named("orders")}
 * </pre>
 *
 * <p>JSF's {@code eager} attribute has no direct CDI {@code @Named}
 * equivalent. It is intentionally dropped while the family remains ASSISTED
 * in the Workbench so lifecycle semantics are still called out for review.</p>
 */
public class MigrateManagedBeanToNamed extends Recipe {

    private static final String LEGACY_MANAGED_BEAN = "javax.faces.bean.ManagedBean";
    private static final String CDI_NAMED = "jakarta.inject.Named";

    @Override
    public String getDisplayName() {
        return "Migrate JSF @ManagedBean to CDI @Named";
    }

    @Override
    public String getDescription() {
        return "Replaces javax.faces.bean.ManagedBean with jakarta.inject.Named, preserves explicit bean names, and drops unsupported eager semantics for assisted review.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new NormalizeManagedBeanArguments(),
                new ChangeType(LEGACY_MANAGED_BEAN, CDI_NAMED, false)
        );
    }

    /**
     * Converts legacy {@code @ManagedBean} arguments into the argument shape
     * understood by {@code @Named} before {@link ChangeType} runs.
     */
    private static final class NormalizeManagedBeanArguments extends Recipe {

        @Override
        public String getDisplayName() {
            return "Normalize JSF @ManagedBean arguments for CDI @Named";
        }

        @Override
        public String getDescription() {
            return "Converts @ManagedBean(name = \"...\") to a positional value and removes the unsupported eager attribute before the annotation type is changed to jakarta.inject.Named.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                    J.Annotation a = super.visitAnnotation(annotation, ctx);
                    if (!isManagedBean(a)) {
                        return a;
                    }

                    J.Literal explicitName = explicitBeanName(a);
                    if (explicitName == null) {
                        // @ManagedBean or @ManagedBean(eager = true) -> no @Named arguments.
                        return a.withArguments(null);
                    }

                    // @ManagedBean(name = "orders", ...) -> @ManagedBean("orders")
                    // ChangeType subsequently turns the annotation into @Named("orders").
                    return a.withArguments(List.of(explicitName.withPrefix(Space.EMPTY)));
                }

                private boolean isManagedBean(J.Annotation annotation) {
                    JavaType type = annotation.getType();
                    if (type instanceof JavaType.Class clazz
                            && LEGACY_MANAGED_BEAN.equals(clazz.getFullyQualifiedName())) {
                        return true;
                    }

                    String sourceName = annotation.getAnnotationType().toString();
                    return "ManagedBean".equals(sourceName)
                            || LEGACY_MANAGED_BEAN.equals(sourceName);
                }

                private J.Literal explicitBeanName(J.Annotation annotation) {
                    if (annotation.getArguments() == null) {
                        return null;
                    }

                    for (Expression argument : annotation.getArguments()) {
                        if (argument instanceof J.Assignment assignment
                                && "name".equals(assignment.getVariable().toString())
                                && assignment.getAssignment() instanceof J.Literal literal
                                && literal.getValue() instanceof String) {
                            return literal;
                        }
                    }

                    return null;
                }
            };
        }
    }
}
