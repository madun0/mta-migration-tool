package com.jackson.mta.primefaces;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Migrates the public, deprecated PrimeFaces 6.2 {@code RequestContext}
 * methods that have direct public equivalents on {@code PrimeFaces.current()}.
 *
 * <p>The transformation logic lives inside a {@link JavaIsoVisitor}, which is
 * required for visitor-scoped APIs such as {@code maybeAddImport()},
 * {@code maybeRemoveImport()}, and {@code updateCursor()} in OpenRewrite
 * 8.90.2.</p>
 *
 * <p>Templates deliberately inherit the target project's parser/type context.
 * Production recipes must not request named runtime JARs from the recipe/plugin
 * classloader because those dependencies belong to the application being
 * migrated, not to the recipe artifact.</p>
 *
 * <p>Unsupported or ambiguous RequestContext APIs are intentionally left
 * unchanged for manual remediation.</p>
 */
public class MigrateRequestContext extends Recipe {

    private static final String REQUEST_CONTEXT =
            "org.primefaces.context.RequestContext";
    private static final String PRIME_FACES =
            "org.primefaces.PrimeFaces";

    private static final MethodMatcher EXECUTE =
            new MethodMatcher(REQUEST_CONTEXT + " execute(..)", true);
    private static final MethodMatcher IS_AJAX_REQUEST =
            new MethodMatcher(REQUEST_CONTEXT + " isAjaxRequest()", true);
    private static final MethodMatcher UPDATE =
            new MethodMatcher(REQUEST_CONTEXT + " update(..)", true);
    private static final MethodMatcher ADD_CALLBACK_PARAM =
            new MethodMatcher(REQUEST_CONTEXT + " addCallbackParam(..)", true);
    private static final MethodMatcher RESET =
            new MethodMatcher(REQUEST_CONTEXT + " reset(..)", true);
    private static final MethodMatcher SCROLL_TO =
            new MethodMatcher(REQUEST_CONTEXT + " scrollTo(..)", true);
    private static final MethodMatcher OPEN_DIALOG =
            new MethodMatcher(REQUEST_CONTEXT + " openDialog(..)", true);
    private static final MethodMatcher CLOSE_DIALOG =
            new MethodMatcher(REQUEST_CONTEXT + " closeDialog(..)", true);
    private static final MethodMatcher SHOW_MESSAGE_IN_DIALOG =
            new MethodMatcher(REQUEST_CONTEXT + " showMessageInDialog(..)", true);

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces RequestContext to PrimeFaces.current()";
    }

    @Override
    public String getDescription() {
        return "Migrates supported PrimeFaces 6.2 RequestContext calls to "
                + "the public PrimeFaces.current() API while leaving "
                + "unsupported APIs for manual review.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodInvocation visitMethodInvocation(
                    J.MethodInvocation method,
                    ExecutionContext ctx) {

                J.MethodInvocation invocation =
                        super.visitMethodInvocation(method, ctx);

                if (EXECUTE.matches(invocation)) {
                    return migrate(
                            invocation,
                            "PrimeFaces.current().executeScript");
                }

                if (IS_AJAX_REQUEST.matches(invocation)) {
                    return migrate(
                            invocation,
                            "PrimeFaces.current().isAjaxRequest");
                }

                if (UPDATE.matches(invocation)) {
                    return migrate(
                            invocation,
                            "PrimeFaces.current().ajax().update");
                }

                if (ADD_CALLBACK_PARAM.matches(invocation)) {
                    return migrate(
                            invocation,
                            "PrimeFaces.current().ajax().addCallbackParam");
                }

                if (RESET.matches(invocation)) {
                    return migrate(
                            invocation,
                            "PrimeFaces.current().resetInputs");
                }

                if (SCROLL_TO.matches(invocation)) {
                    return migrate(
                            invocation,
                            "PrimeFaces.current().scrollTo");
                }

                if (OPEN_DIALOG.matches(invocation)) {
                    return migrate(
                            invocation,
                            "PrimeFaces.current().dialog().openDynamic");
                }

                if (CLOSE_DIALOG.matches(invocation)) {
                    return migrate(
                            invocation,
                            "PrimeFaces.current().dialog().closeDynamic");
                }

                if (SHOW_MESSAGE_IN_DIALOG.matches(invocation)) {
                    return migrate(
                            invocation,
                            "PrimeFaces.current().dialog().showMessageDynamic");
                }

                return invocation;
            }

            /**
             * Replaces one legacy RequestContext invocation with its modern
             * PrimeFaces equivalent.
             *
             * <p>The template inherits the compilation unit's parser/type context.
             * Do not attach a {@code JavaParser.classpath(...)} here: that lookup
             * searches the recipe/plugin runtime rather than the migrated
             * application's dependency graph.</p>
             *
             * @param invocation legacy RequestContext invocation
             * @param replacementMethod target PrimeFaces method expression
             * @return migrated invocation
             */
            private J.MethodInvocation migrate(
                    J.MethodInvocation invocation,
                    String replacementMethod) {

                List<Expression> arguments =
                        invocation.getArguments();

                String parameters =
                        arguments.stream()
                                .map(argument -> "#{any()}")
                                .collect(Collectors.joining(", "));

                JavaTemplate template =
                        JavaTemplate.builder(
                                        replacementMethod
                                                + "("
                                                + parameters
                                                + ")")
                                .imports(PRIME_FACES)
                                .contextSensitive()
                                .build();

                J.MethodInvocation migrated =
                        template.apply(
                                updateCursor(invocation),
                                invocation.getCoordinates().replace(),
                                arguments.toArray());

                /*
                 * OpenRewrite 8.90.2's one-argument maybeAddImport overload
                 * adds only when an attributed reference is detected.
                 * PrimeFaces is introduced in this visitor cycle, so request
                 * the import explicitly.
                 */
                maybeAddImport(PRIME_FACES, false);

                /*
                 * Remove RequestContext only when no remaining attributed
                 * references require it.
                 */
                maybeRemoveImport(REQUEST_CONTEXT);

                return migrated;
            }
        };
    }
}
