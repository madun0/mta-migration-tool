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
 * Migrates PrimeFaces 6.2 TimelineEvent Date-based constructors and Date
 * setters to the builder / LocalDateTime API used by modern PrimeFaces.
 *
 * Date -> LocalDateTime requires a timezone choice. This recipe deliberately
 * uses ZoneId.systemDefault() as a compatibility bridge. Review the result
 * against p:timeline timeZone/clientTimeZone and the application's business
 * timezone rules before production rollout.
 */
public class MigrateTimelineEventConstruction extends Recipe {

    private static final String TIMELINE_EVENT =
            "org.primefaces.model.timeline.TimelineEvent";

    private static final String TARGET_STUB = """
            package org.primefaces.model.timeline;

            import java.time.LocalDateTime;

            public class TimelineEvent<T> {
                public TimelineEvent() {
                }

                public static <T> Builder<T> builder() {
                    return new Builder<>();
                }

                public void setStartDate(LocalDateTime value) {
                }

                public void setEndDate(LocalDateTime value) {
                }

                public static class Builder<T> {
                    public Builder<T> data(T value) { return this; }
                    public Builder<T> startDate(LocalDateTime value) { return this; }
                    public Builder<T> endDate(LocalDateTime value) { return this; }
                    public Builder<T> editable(Boolean value) { return this; }
                    public Builder<T> group(String value) { return this; }
                    public Builder<T> styleClass(String value) { return this; }

                    public TimelineEvent<T> build() {
                        return new TimelineEvent<>();
                    }
                }
            }
            """;

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces TimelineEvent construction";
    }

    @Override
    public String getDescription() {
        return "Migrates PrimeFaces 6.2 TimelineEvent Date-based constructors and setters to the builder / LocalDateTime API.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            private final MethodMatcher setStartDate = new MethodMatcher(
                    TIMELINE_EVENT + " setStartDate(java.util.Date)");

            private final MethodMatcher setEndDate = new MethodMatcher(
                    TIMELINE_EVENT + " setEndDate(java.util.Date)");

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J visited = super.visitNewClass(newClass, ctx);
                if (!(visited instanceof J.NewClass)) {
                    return visited;
                }

                J.NewClass n = (J.NewClass) visited;

                if (!TypeUtils.isOfClassType(n.getType(), TIMELINE_EVENT)) {
                    return n;
                }

                List<Expression> args = n.getArguments();

                // The no-arg constructor still exists in current PrimeFaces.
                if (args.isEmpty()) {
                    return n;
                }

                JavaType.Method ctor = n.getConstructorType();
                if (ctor == null || ctor.getParameterTypes().size() != args.size()) {
                    return n;
                }

                // Every legacy public constructor begins with data, startDate.
                if (args.size() < 2 || !isDate(ctor.getParameterTypes().get(1))) {
                    return n;
                }

                StringBuilder source = new StringBuilder()
                        .append("TimelineEvent.builder()")
                        .append(".data(#{any()})")
                        .append(".startDate(")
                        .append(dateToLocalDateTime("#{any(java.util.Date)}"))
                        .append(")");

                int index = 2;

                // Legacy constructor families differ at argument 3:
                // Date -> endDate family, Boolean -> editable family.
                if (index < args.size()
                        && isDate(ctor.getParameterTypes().get(index))) {
                    source.append(".endDate(")
                            .append(dateToLocalDateTime("#{any(java.util.Date)}"))
                            .append(")");
                    index++;
                }

                if (index < args.size()
                        && isBoolean(ctor.getParameterTypes().get(index))) {
                    source.append(".editable(#{any(java.lang.Boolean)})");
                    index++;
                }

                if (index < args.size()
                        && isString(ctor.getParameterTypes().get(index))) {
                    source.append(".group(#{any(java.lang.String)})");
                    index++;
                }

                if (index < args.size()
                        && isString(ctor.getParameterTypes().get(index))) {
                    source.append(".styleClass(#{any(java.lang.String)})");
                    index++;
                }

                // Do not guess if an unknown overload shape is encountered.
                if (index != args.size()) {
                    return n;
                }

                source.append(".build()");

                JavaTemplate template = JavaTemplate.builder(source.toString())
                        .imports(TIMELINE_EVENT)
                        .imports("java.time.LocalDateTime")
                        .imports("java.time.ZoneId")
                        .imports("java.util.Optional")
                        .javaParser(targetParser())
                        .contextSensitive()
                        .build();

                maybeAddImport(TIMELINE_EVENT);
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

            private boolean isDate(JavaType type) {
                return TypeUtils.isOfClassType(type, "java.util.Date");
            }

            private boolean isBoolean(JavaType type) {
                return type == JavaType.Primitive.Boolean
                        || TypeUtils.isOfClassType(type, "java.lang.Boolean");
            }

            private boolean isString(JavaType type) {
                return TypeUtils.isOfClassType(type, "java.lang.String");
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
