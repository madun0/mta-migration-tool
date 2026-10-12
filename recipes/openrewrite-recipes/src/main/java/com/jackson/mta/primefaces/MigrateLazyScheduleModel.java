package com.jackson.mta.primefaces;

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
import java.util.stream.Collectors;

/**
 * Migrates {@code LazyScheduleModel#loadEvents(Date, Date)} to the
 * {@code LocalDateTime} contract while preserving the existing method body
 * through {@code Date} compatibility locals.
 *
 * <p>The compatibility bridge uses {@code ZoneId.systemDefault()}. The
 * generated source includes an explicit MTA review comment because the
 * application's {@code p:schedule} {@code timeZone}/{@code clientTimeZone}
 * and business timezone rules may require a different zone.</p>
 *
 * <p>This implementation replaces the complete method in one
 * {@link JavaTemplate} operation. OpenRewrite 8.x does not always refresh the
 * types of parameter usages when only a method's parameter list is replaced.
 * Rebuilding the complete method causes the new {@code __mtaStart} and
 * {@code __mtaEnd} declarations and all references to them to be parsed and
 * type-attributed together.</p>
 */
public class MigrateLazyScheduleModel extends Recipe {

    private static final String LAZY_SCHEDULE_MODEL =
            "org.primefaces.model.LazyScheduleModel";

    /**
     * Target-side LazyScheduleModel shape used to type-attribute the migrated
     * {@code @Override} method inside JavaTemplate.
     */
    private static final String TARGET_LAZY_SCHEDULE_MODEL_STUB = """
            package org.primefaces.model;

            import java.time.LocalDateTime;

            public abstract class LazyScheduleModel {
                public abstract void loadEvents(
                        LocalDateTime start,
                        LocalDateTime end);
            }
            """;

    @Override
    public String getDisplayName() {
        return "Migrate LazyScheduleModel Date parameters";
    }

    @Override
    public String getDescription() {
        return "Migrates LazyScheduleModel.loadEvents(Date, Date) to "
                + "LocalDateTime parameters with compatibility Date locals.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method,
                    ExecutionContext ctx) {

                J.MethodDeclaration m =
                        super.visitMethodDeclaration(method, ctx);

                /*
                 * Match only the legacy LazyScheduleModel method.
                 *
                 * Testing the original declaration's parameter types prevents
                 * the recipe from matching its own migrated output on a later
                 * cycle.
                 */
                if (!"loadEvents".equals(m.getSimpleName())
                        || m.getBody() == null
                        || !isInsideLazyScheduleModel()
                        || m.getParameters().size() != 2
                        || !isDate(m.getParameters().get(0))
                        || !isDate(m.getParameters().get(1))) {
                    return m;
                }

                String oldStart =
                        parameterName(m.getParameters().get(0));
                String oldEnd =
                        parameterName(m.getParameters().get(1));

                if (oldStart == null || oldEnd == null) {
                    return m;
                }

                /*
                 * Preserve all existing method annotations. This includes
                 * @Override while also retaining any application-specific
                 * annotations that may already be present.
                 */
                String annotations =
                        m.getLeadingAnnotations().stream()
                                .map(annotation ->
                                        annotation.print(getCursor()))
                                .collect(Collectors.joining("\n"));

                /*
                 * Preserve existing modifiers such as public, protected,
                 * synchronized, etc. LazyScheduleModel implementations are
                 * normally public, but retaining the original modifiers is
                 * safer than hard-coding them.
                 */
                String modifiers =
                        m.getModifiers().stream()
                                .map(J.Modifier::toString)
                                .collect(Collectors.joining(" "));

                String migratedBody =
                        migratedBody(
                                m,
                                oldStart,
                                oldEnd);

                /*
                 * Replace the WHOLE method at once.
                 *
                 * The #{} substitutions here are deliberately untyped strings.
                 * They are inserted into the template source before parsing.
                 * Consequently, javac sees the new LocalDateTime parameters,
                 * compatibility locals, and all __mtaStart/__mtaEnd usages in
                 * one compilation unit and assigns consistent symbols/types.
                 */
                JavaTemplate template =
                        JavaTemplate.builder(
                                        "#{}\n"
                                                + "#{} void loadEvents("
                                                + "LocalDateTime __mtaStart, "
                                                + "LocalDateTime __mtaEnd) "
                                                + "#{}")
                                .contextSensitive()
                                .imports(
                                        "java.time.LocalDateTime",
                                        "java.time.ZoneId",
                                        "java.util.Date")
                                .javaParser(
                                        JavaParser.fromJavaVersion()
                                                .dependsOn(
                                                        TARGET_LAZY_SCHEDULE_MODEL_STUB))
                                .build();

                J.MethodDeclaration migrated =
                        template.apply(
                                getCursor(),
                                m.getCoordinates().replace(),
                                annotations,
                                modifiers,
                                migratedBody);

                maybeAddImport("java.time.LocalDateTime");
                maybeAddImport("java.time.ZoneId");

                /*
                 * java.util.Date remains required by the generated
                 * compatibility variables, so ensure it is present as well.
                 */
                maybeAddImport("java.util.Date");

                return maybeAutoFormat(m, migrated, ctx);
            }

            /**
             * Builds the complete target method body as source text.
             *
             * <p>The original body statements are preserved after two
             * compatibility variables. Since this complete body is parsed
             * together with the new method parameters, references to
             * {@code __mtaStart} and {@code __mtaEnd} receive valid
             * {@link org.openrewrite.java.tree.JavaType.Variable} attribution.</p>
             *
             * @param method legacy loadEvents method
             * @param oldStart original start parameter name
             * @param oldEnd original end parameter name
             * @return complete source for the migrated method body
             */
            private String migratedBody(
                    J.MethodDeclaration method,
                    String oldStart,
                    String oldEnd) {

                J.Block body = method.getBody();

                if (body == null) {
                    return "{}";
                }

                /*
                 * print(getCursor()) is used only to preserve the application's
                 * existing body as source. JavaTemplate reparses the final body,
                 * so stale legacy parameter symbols are not carried into the
                 * migrated LST.
                 */
                String originalBody =
                        body.print(getCursor());

                int openBrace =
                        originalBody.indexOf('{');
                int closeBrace =
                        originalBody.lastIndexOf('}');

                String existingStatements = "";

                if (openBrace >= 0
                        && closeBrace > openBrace) {
                    existingStatements =
                            originalBody.substring(
                                            openBrace + 1,
                                            closeBrace)
                                    .strip();
                }

                StringBuilder migrated =
                        new StringBuilder();

                migrated.append("{\n");

                migrated.append(
                        "/* MTA compatibility bridge: "
                                + "review ZoneId.systemDefault() against "
                                + "p:schedule timeZone/clientTimeZone "
                                + "and business timezone rules. */\n");

                migrated.append("Date ")
                        .append(oldStart)
                        .append(" = __mtaStart == null ? null : ")
                        .append("Date.from(__mtaStart")
                        .append(".atZone(ZoneId.systemDefault())")
                        .append(".toInstant());\n");

                migrated.append("Date ")
                        .append(oldEnd)
                        .append(" = __mtaEnd == null ? null : ")
                        .append("Date.from(__mtaEnd")
                        .append(".atZone(ZoneId.systemDefault())")
                        .append(".toInstant());");

                if (!existingStatements.isBlank()) {
                    migrated.append('\n')
                            .append(existingStatements);
                }

                migrated.append("\n}");

                return migrated.toString();
            }

            /**
             * Determines whether the currently visited method belongs to a
             * LazyScheduleModel implementation.
             *
             * @return {@code true} when the enclosing class is assignable to
             *         LazyScheduleModel
             */
            private boolean isInsideLazyScheduleModel() {
                J.ClassDeclaration enclosing =
                        getCursor().firstEnclosing(
                                J.ClassDeclaration.class);

                return enclosing != null
                        && enclosing.getType() != null
                        && TypeUtils.isAssignableTo(
                                LAZY_SCHEDULE_MODEL,
                                enclosing.getType());
            }

            /**
             * Determines whether a parameter is a {@code java.util.Date}.
             *
             * @param parameter method parameter
             * @return {@code true} when the parameter type is Date
             */
            private boolean isDate(Statement parameter) {
                if (!(parameter
                        instanceof J.VariableDeclarations)) {
                    return false;
                }

                J.VariableDeclarations declarations =
                        (J.VariableDeclarations) parameter;

                JavaType type =
                        declarations.getTypeExpression() == null
                                ? null
                                : declarations
                                        .getTypeExpression()
                                        .getType();

                return TypeUtils.isOfClassType(
                        type,
                        "java.util.Date");
            }

            /**
             * Gets the simple name of a single-variable method parameter.
             *
             * @param parameter method parameter
             * @return parameter name, or {@code null} for unsupported shapes
             */
            private String parameterName(Statement parameter) {
                if (!(parameter
                        instanceof J.VariableDeclarations)) {
                    return null;
                }

                J.VariableDeclarations declarations =
                        (J.VariableDeclarations) parameter;

                if (declarations.getVariables().size() != 1) {
                    return null;
                }

                return declarations
                        .getVariables()
                        .get(0)
                        .getSimpleName();
            }
        };
    }
}
