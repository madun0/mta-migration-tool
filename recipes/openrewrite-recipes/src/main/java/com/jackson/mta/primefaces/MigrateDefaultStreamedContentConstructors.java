package com.jackson.mta.primefaces;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.List;

/**
 * Converts only high-confidence legacy DefaultStreamedContent constructors.
 *
 * Safe first arguments:
 * - an InputStream identifier/field that already exists before construction
 * - new ByteArrayInputStream(...), whose constructor has no checked exception
 *
 * Method calls and arbitrary new InputStream implementations are deliberately
 * left alone because moving their evaluation into Supplier#get() can change
 * checked-exception, timing, and resource-lifetime semantics.
 */
public class MigrateDefaultStreamedContentConstructors extends Recipe {

    private static final String DEFAULT_STREAMED_CONTENT =
            "org.primefaces.model.DefaultStreamedContent";

    private static final String TARGET_STUB = """
            package org.primefaces.model;

            import java.io.InputStream;
            import java.util.function.Supplier;

            public class DefaultStreamedContent {
                public static Builder builder() {
                    return new Builder();
                }

                public static class Builder {
                    public Builder stream(Supplier<InputStream> supplier) {
                        return this;
                    }

                    public Builder contentType(String value) {
                        return this;
                    }

                    public Builder name(String value) {
                        return this;
                    }

                    public Builder contentEncoding(String value) {
                        return this;
                    }

                    public Builder contentLength(Long value) {
                        return this;
                    }

                    public DefaultStreamedContent build() {
                        return new DefaultStreamedContent();
                    }
                }
            }
            """;

    @Override
    public String getDisplayName() {
        return "Migrate safe DefaultStreamedContent constructors";
    }

    @Override
    public String getDescription() {
        return "Migrates high-confidence PrimeFaces 6.2 DefaultStreamedContent constructors to builder().stream(Supplier) while preserving ambiguous stream creation for review.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J visited = super.visitNewClass(newClass, ctx);
                if (!(visited instanceof J.NewClass)) {
                    return visited;
                }

                J.NewClass n = (J.NewClass) visited;
                if (!TypeUtils.isOfClassType(n.getType(), DEFAULT_STREAMED_CONTENT)) {
                    return n;
                }

                List<Expression> args = n.getArguments();

                // No-arg constructor still exists, and often implies setter mutation.
                if (args.isEmpty() || args.size() > 5) {
                    return n;
                }

                if (!safeStreamExpression(args.get(0))) {
                    return n;
                }

                boolean fourthIsLength = false;
                if (args.size() == 4) {
                    fourthIsLength =
                            TypeUtils.isOfClassType(
                                    args.get(3).getType(),
                                    "java.lang.Integer")
                            || args.get(3).getType()
                                    == org.openrewrite.java.tree.JavaType.Primitive.Int;
                }

                String templateSource =
                        template(args.size(), fourthIsLength);

                if (templateSource == null) {
                    return n;
                }

                JavaTemplate template = JavaTemplate.builder(templateSource)
                        .imports(DEFAULT_STREAMED_CONTENT)
                        .imports("java.util.Optional")
                        .javaParser(JavaParser.fromJavaVersion()
                                .dependsOn(TARGET_STUB))
                        .contextSensitive()
                        .build();

                maybeAddImport(DEFAULT_STREAMED_CONTENT);

                if ((args.size() == 4 && fourthIsLength) || args.size() == 5) {
                    maybeAddImport("java.util.Optional");
                }

                J migrated = template.apply(
                        updateCursor(n),
                        n.getCoordinates().replace(),
                        args.toArray());

                return maybeAutoFormat(n, migrated, ctx);
            }

            private boolean safeStreamExpression(Expression expression) {
                if (expression instanceof J.Identifier
                        || expression instanceof J.FieldAccess) {
                    return true;
                }

                if (expression instanceof J.NewClass) {
                    return TypeUtils.isOfClassType(
                            expression.getType(),
                            "java.io.ByteArrayInputStream");
                }

                return false;
            }

            private String template(int count, boolean fourthIsLength) {
                return switch (count) {
                    case 1 ->
                            "DefaultStreamedContent.builder()" +
                            ".stream(() -> #{any(java.io.InputStream)})" +
                            ".build()";

                    case 2 ->
                            "DefaultStreamedContent.builder()" +
                            ".stream(() -> #{any(java.io.InputStream)})" +
                            ".contentType(#{any(java.lang.String)})" +
                            ".build()";

                    case 3 ->
                            "DefaultStreamedContent.builder()" +
                            ".stream(() -> #{any(java.io.InputStream)})" +
                            ".contentType(#{any(java.lang.String)})" +
                            ".name(#{any(java.lang.String)})" +
                            ".build()";

                    case 4 -> fourthIsLength
                            ? "DefaultStreamedContent.builder()" +
                              ".stream(() -> #{any(java.io.InputStream)})" +
                              ".contentType(#{any(java.lang.String)})" +
                              ".name(#{any(java.lang.String)})" +
                              ".contentLength(Optional.ofNullable(#{any(java.lang.Integer)})" +
                              ".map(Integer::longValue).orElse(null))" +
                              ".build()"
                            : "DefaultStreamedContent.builder()" +
                              ".stream(() -> #{any(java.io.InputStream)})" +
                              ".contentType(#{any(java.lang.String)})" +
                              ".name(#{any(java.lang.String)})" +
                              ".contentEncoding(#{any(java.lang.String)})" +
                              ".build()";

                    case 5 ->
                            "DefaultStreamedContent.builder()" +
                            ".stream(() -> #{any(java.io.InputStream)})" +
                            ".contentType(#{any(java.lang.String)})" +
                            ".name(#{any(java.lang.String)})" +
                            ".contentEncoding(#{any(java.lang.String)})" +
                            ".contentLength(Optional.ofNullable(#{any(java.lang.Integer)})" +
                            ".map(Integer::longValue).orElse(null))" +
                            ".build()";

                    default -> null;
                };
            }
        };
    }
}
