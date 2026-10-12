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
 * Migrates PrimeFaces 6.2 {@code ByteArrayContent} constructor usages to
 * {@code DefaultStreamedContent.builder()}.
 *
 * <p>The no-argument constructor is deliberately left unchanged because it
 * commonly represents later setter-based mutation and cannot be safely mapped
 * to a lazy stream supplier without additional data-flow analysis.</p>
 *
 * <p>This recipe intentionally does not rewrite arbitrary legacy
 * {@code DefaultStreamedContent(InputStream, ...)} constructors. Moving an
 * arbitrary InputStream creation into a lazy Supplier can alter evaluation
 * timing, resource lifetime, and checked-exception behavior.</p>
 */
public class MigrateByteArrayContent extends Recipe {

    private static final String BYTE_ARRAY_CONTENT =
            "org.primefaces.model.ByteArrayContent";
    private static final String DEFAULT_STREAMED_CONTENT =
            "org.primefaces.model.DefaultStreamedContent";

    /**
     * Minimal target-side stub used only to type-attribute JavaTemplate output.
     */
    private static final String TARGET_STREAMED_CONTENT_STUB = """
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

                    public Builder contentType(String contentType) {
                        return this;
                    }

                    public Builder name(String name) {
                        return this;
                    }

                    public Builder contentEncoding(String contentEncoding) {
                        return this;
                    }

                    public Builder contentLength(Long contentLength) {
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
        return "Migrate PrimeFaces ByteArrayContent to DefaultStreamedContent builder";
    }

    @Override
    public String getDescription() {
        return "Migrates PrimeFaces 6.2 ByteArrayContent constructor usages "
                + "to the modern DefaultStreamedContent.builder() API.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
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

                if (!TypeUtils.isOfClassType(n.getType(), BYTE_ARRAY_CONTENT)) {
                    return n;
                }

                /*
                 * OpenRewrite represents an empty constructor argument list
                 * with a synthetic J.Empty in some parser contexts. Filtering
                 * it is therefore required; checking List.isEmpty() alone is
                 * not sufficient.
                 */
                List<Expression> args = meaningfulArguments(n);

                /*
                 * No-arg construction often implies later setter-based
                 * mutation. Return before scheduling imports or templates so
                 * this case completes in zero recipe cycles.
                 */
                if (args.isEmpty()) {
                    return n;
                }

                if (args.size() > 5) {
                    return n;
                }

                boolean fourthArgumentIsContentLength = false;
                if (args.size() == 4) {
                    Expression fourth = args.get(3);
                    fourthArgumentIsContentLength =
                            TypeUtils.isOfClassType(
                                    fourth.getType(),
                                    "java.lang.Integer");
                }

                String templateSource =
                        buildTemplate(
                                args.size(),
                                fourthArgumentIsContentLength);

                if (templateSource == null) {
                    return n;
                }

                JavaTemplate template =
                        JavaTemplate.builder(templateSource)
                                .imports(DEFAULT_STREAMED_CONTENT)
                                .imports("java.io.ByteArrayInputStream")
                                .imports("java.util.Optional")
                                .javaParser(
                                        JavaParser.fromJavaVersion()
                                                .dependsOn(
                                                        TARGET_STREAMED_CONTENT_STUB))
                                .contextSensitive()
                                .build();

                maybeAddImport(DEFAULT_STREAMED_CONTENT);
                maybeAddImport("java.io.ByteArrayInputStream");

                if ((args.size() == 4
                        && fourthArgumentIsContentLength)
                        || args.size() == 5) {
                    maybeAddImport("java.util.Optional");
                }

                J.NewClass original = n;

                J migrated =
                        template.apply(
                                updateCursor(n),
                                n.getCoordinates().replace(),
                                args.toArray());

                maybeRemoveImport(BYTE_ARRAY_CONTENT);

                return maybeAutoFormat(original, migrated, ctx);
            }

            /**
             * Returns constructor arguments excluding OpenRewrite's synthetic
             * empty placeholder.
             */
            private List<Expression> meaningfulArguments(J.NewClass n) {
                return n.getArguments().stream()
                        .filter(argument -> !(argument instanceof J.Empty))
                        .toList();
            }

            private String buildTemplate(
                    int argCount,
                    boolean fourthIsContentLength) {

                return switch (argCount) {
                    case 1 ->
                            "DefaultStreamedContent.builder()"
                            + ".stream(() -> new ByteArrayInputStream(#{any(byte[])}))"
                            + ".build()";

                    case 2 ->
                            "DefaultStreamedContent.builder()"
                            + ".stream(() -> new ByteArrayInputStream(#{any(byte[])}))"
                            + ".contentType(#{any(java.lang.String)})"
                            + ".build()";

                    case 3 ->
                            "DefaultStreamedContent.builder()"
                            + ".stream(() -> new ByteArrayInputStream(#{any(byte[])}))"
                            + ".contentType(#{any(java.lang.String)})"
                            + ".name(#{any(java.lang.String)})"
                            + ".build()";

                    case 4 -> fourthIsContentLength
                            ? "DefaultStreamedContent.builder()"
                              + ".stream(() -> new ByteArrayInputStream(#{any(byte[])}))"
                              + ".contentType(#{any(java.lang.String)})"
                              + ".name(#{any(java.lang.String)})"
                              + ".contentLength(Optional.ofNullable(#{any(java.lang.Integer)})"
                              + ".map(Integer::longValue).orElse(null))"
                              + ".build()"
                            : "DefaultStreamedContent.builder()"
                              + ".stream(() -> new ByteArrayInputStream(#{any(byte[])}))"
                              + ".contentType(#{any(java.lang.String)})"
                              + ".name(#{any(java.lang.String)})"
                              + ".contentEncoding(#{any(java.lang.String)})"
                              + ".build()";

                    case 5 ->
                            "DefaultStreamedContent.builder()"
                            + ".stream(() -> new ByteArrayInputStream(#{any(byte[])}))"
                            + ".contentType(#{any(java.lang.String)})"
                            + ".name(#{any(java.lang.String)})"
                            + ".contentEncoding(#{any(java.lang.String)})"
                            + ".contentLength(Optional.ofNullable(#{any(java.lang.Integer)})"
                            + ".map(Integer::longValue).orElse(null))"
                            + ".build()";

                    default -> null;
                };
            }
        };
    }
}
