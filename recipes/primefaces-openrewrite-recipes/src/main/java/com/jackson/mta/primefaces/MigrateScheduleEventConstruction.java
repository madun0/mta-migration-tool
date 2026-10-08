package com.jackson.mta.primefaces;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.List;

/**
 * Migrates the PrimeFaces 6.2 DefaultScheduleEvent(Date...) constructor and
 * Date setter patterns to the modern builder / LocalDateTime API.
 *
 * A ZoneId.systemDefault() bridge is used intentionally as a compatibility
 * baseline because java.util.Date represented an instant while LocalDateTime
 * does not carry a zone. Every transformed application should review this
 * bridge against its p:schedule timeZone/clientTimeZone and business rules.
 */
public class MigrateScheduleEventConstruction extends Recipe {

    private static final String DEFAULT_SCHEDULE_EVENT =
            "org.primefaces.model.DefaultScheduleEvent";

    private static final String TARGET_STUB = """
            package org.primefaces.model;

            import java.time.LocalDateTime;

            public class DefaultScheduleEvent<T> {
                public DefaultScheduleEvent() {
                }

                public static <T> Builder<T> builder() {
                    return new Builder<>();
                }

                public void setStartDate(LocalDateTime value) {
                }

                public void setEndDate(LocalDateTime value) {
                }

                public static class Builder<T> {
                    public Builder<T> title(String value) { return this; }
                    public Builder<T> startDate(LocalDateTime value) { return this; }
                    public Builder<T> endDate(LocalDateTime value) { return this; }
                    public Builder<T> allDay(boolean value) { return this; }
                    public Builder<T> styleClass(String value) { return this; }
                    public Builder<T> data(T value) { return this; }
                    public DefaultScheduleEvent<T> build() {
                        return new DefaultScheduleEvent<>();
                    }
                }
            }
            """;

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces schedule event construction";
    }

    @Override
    public String getDescription() {
        return "Migrates PrimeFaces 6.2 DefaultScheduleEvent Date-based constructors/setters to builder-based LocalDateTime APIs.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            private final MethodMatcher setStartDate = new MethodMatcher(
                    DEFAULT_SCHEDULE_EVENT + " setStartDate(java.util.Date)");

            private final MethodMatcher setEndDate = new MethodMatcher(
                    DEFAULT_SCHEDULE_EVENT + " setEndDate(java.util.Date)");

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J visited = super.visitNewClass(newClass, ctx);
                if (!(visited instanceof J.NewClass)) {
                    return visited;
                }

                J.NewClass n = (J.NewClass) visited;

                if (!TypeUtils.isOfClassType(n.getType(), DEFAULT_SCHEDULE_EVENT)) {
                    return n;
                }

                List<Expression> args = n.getArguments();

                // The no-arg constructor still exists in PrimeFaces 16.
                if (args.isEmpty()) {
                    return n;
                }

                if (args.size() != 3 && args.size() != 4) {
                    return n;
                }

                String source =
                        "DefaultScheduleEvent.builder()" +
                        ".title(#{any(java.lang.String)})" +
                        ".startDate(" + dateToLocalDateTime("#{any(java.util.Date)}") + ")" +
                        ".endDate(" + dateToLocalDateTime("#{any(java.util.Date)}") + ")";

                if (args.size() == 4) {
                    String fourthBuilderMethod = fourthArgumentBuilderMethod(n, args.get(3));
                    if (fourthBuilderMethod == null) {
                        return n;
                    }

                    String placeholder = switch (fourthBuilderMethod) {
                        case "allDay" -> "#{any(boolean)}";
                        case "styleClass" -> "#{any(java.lang.String)}";
                        default -> "#{any()}";
                    };

                    source += "." + fourthBuilderMethod + "(" + placeholder + ")";
                }

                source += ".build()";

                JavaTemplate template = JavaTemplate.builder(source)
                        .imports(DEFAULT_SCHEDULE_EVENT)
                        .imports("java.time.LocalDateTime")
                        .imports("java.time.ZoneId")
                        .imports("java.util.Optional")
                        .javaParser(targetParser())
                        .contextSensitive()
                        .build();

                maybeAddImport(DEFAULT_SCHEDULE_EVENT);
                maybeAddImport("java.time.LocalDateTime");
                maybeAddImport("java.time.ZoneId");
                maybeAddImport("java.util.Optional");

                J migrated = template.apply(
                        updateCursor(n),
                        n.getCoordinates().replace(),
                        args.toArray());

                return maybeAutoFormat(n, migrated, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method,
                                           ExecutionContext ctx) {
                J visited = super.visitMethodInvocation(method, ctx);
                if (!(visited instanceof J.MethodInvocation)) {
                    return visited;
                }

                J.MethodInvocation m = (J.MethodInvocation) visited;

                boolean start = setStartDate.matches(m);
                boolean end = setEndDate.matches(m);

                if ((!start && !end)
                        || m.getSelect() == null
                        || m.getArguments().size() != 1) {
                    return m;
                }

                String target = start ? "setStartDate" : "setEndDate";
                String source =
                        "#{any()}." + target + "(" +
                        dateToLocalDateTime("#{any(java.util.Date)}") +
                        ")";

                JavaTemplate template = JavaTemplate.builder(source)
                        .imports("java.time.LocalDateTime")
                        .imports("java.time.ZoneId")
                        .imports("java.util.Optional")
                        .javaParser(targetParser())
                        .contextSensitive()
                        .build();

                maybeAddImport("java.time.LocalDateTime");
                maybeAddImport("java.time.ZoneId");
                maybeAddImport("java.util.Optional");

                J migrated = template.apply(
                        updateCursor(m),
                        m.getCoordinates().replace(),
                        m.getSelect(),
                        m.getArguments().get(0));

                return maybeAutoFormat(m, migrated, ctx);
            }

            private String fourthArgumentBuilderMethod(J.NewClass n, Expression fourth) {
                JavaType.Method ctor = n.getConstructorType();
                if (ctor != null && ctor.getParameterTypes().size() == 4) {
                    JavaType parameterType = ctor.getParameterTypes().get(3);

                    if (parameterType == JavaType.Primitive.Boolean) {
                        return "allDay";
                    }

                    if (TypeUtils.isOfClassType(parameterType, "java.lang.String")) {
                        return "styleClass";
                    }

                    return "data";
                }

                JavaType type = fourth.getType();
                if (type == JavaType.Primitive.Boolean
                        || TypeUtils.isOfClassType(type, "java.lang.Boolean")) {
                    return "allDay";
                }

                if (TypeUtils.isOfClassType(type, "java.lang.String")) {
                    return "styleClass";
                }

                if (type != null) {
                    return "data";
                }

                return null;
            }

            private String dateToLocalDateTime(String dateExpression) {
                return "Optional.ofNullable(" + dateExpression + ")" +
                        ".map(d -> LocalDateTime.ofInstant(" +
                        "d.toInstant(), ZoneId.systemDefault()))" +
                        ".orElse(null)";
            }

            private JavaParser.Builder<?, ?> targetParser() {
                return JavaParser.fromJavaVersion().dependsOn(TARGET_STUB);
            }
        };
    }
}
