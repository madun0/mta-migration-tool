package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Tests the RequestContext-to-PrimeFaces.current() migration with an explicit
 * source-side/target-side API model instead of relying on Maven's test runtime
 * classpath artifact names.
 */
class MigrateRequestContextTest implements RewriteTest {

    private static final String REQUEST_CONTEXT_STUB = """
            package org.primefaces.context;

            import javax.faces.application.FacesMessage;
            import java.util.Map;

            public class RequestContext {
                public static RequestContext getCurrentInstance() { return null; }
                public void execute(String script) {}
                public boolean isAjaxRequest() { return false; }
                public void update(String id) {}
                public void addCallbackParam(String name, Object value) {}
                public void reset(String id) {}
                public void scrollTo(String id) {}
                public void openDialog(String name) {}
                public void closeDialog(Object data) {}
                public void showMessageInDialog(FacesMessage message) {}
                public Map<String, Object> getCallbackParams() { return null; }
            }
            """;

    private static final String PRIME_FACES_STUB = """
            package org.primefaces;

            import javax.faces.application.FacesMessage;

            public class PrimeFaces {
                public static PrimeFaces current() { return null; }
                public void executeScript(String script) {}
                public boolean isAjaxRequest() { return false; }
                public Ajax ajax() { return null; }
                public void resetInputs(String id) {}
                public void scrollTo(String id) {}
                public Dialog dialog() { return null; }

                public static class Ajax {
                    public void update(String id) {}
                    public void addCallbackParam(String name, Object value) {}
                }

                public static class Dialog {
                    public void openDynamic(String name) {}
                    public void closeDynamic(Object data) {}
                    public void showMessageDynamic(FacesMessage message) {}
                }
            }
            """;

    private static final String FACES_MESSAGE_STUB = """
            package javax.faces.application;
            public class FacesMessage {}
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateRequestContext())
            .parser(JavaParser.fromJavaVersion().dependsOn(
                    REQUEST_CONTEXT_STUB,
                    PRIME_FACES_STUB,
                    FACES_MESSAGE_STUB))
            .afterTypeValidationOptions(TypeValidation.builder()
                    .methodInvocations(false)
                    .identifiers(false)
                    .build());
    }

    @Test
    void migratesDirectChainedCalls() {
        rewriteRun(
            java(
                """
                import javax.faces.application.FacesMessage;
                import org.primefaces.context.RequestContext;

                class Example {
                    void run(FacesMessage message) {
                        RequestContext.getCurrentInstance().execute("PF('dlg').show()");
                        boolean ajax = RequestContext.getCurrentInstance().isAjaxRequest();
                        RequestContext.getCurrentInstance().update("form:table");
                        RequestContext.getCurrentInstance().addCallbackParam("ok", true);
                        RequestContext.getCurrentInstance().reset("form");
                        RequestContext.getCurrentInstance().scrollTo("form:table");
                        RequestContext.getCurrentInstance().openDialog("details");
                        RequestContext.getCurrentInstance().closeDialog("done");
                        RequestContext.getCurrentInstance().showMessageInDialog(message);
                    }
                }
                """,
                """
                import javax.faces.application.FacesMessage;

                import org.primefaces.PrimeFaces;

                class Example {
                    void run(FacesMessage message) {
                        PrimeFaces.current().executeScript("PF('dlg').show()");
                        boolean ajax = PrimeFaces.current().isAjaxRequest();
                        PrimeFaces.current().ajax().update("form:table");
                        PrimeFaces.current().ajax().addCallbackParam("ok", true);
                        PrimeFaces.current().resetInputs("form");
                        PrimeFaces.current().scrollTo("form:table");
                        PrimeFaces.current().dialog().openDynamic("details");
                        PrimeFaces.current().dialog().closeDynamic("done");
                        PrimeFaces.current().dialog().showMessageDynamic(message);
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesCallsThroughLocalRequestContextVariableButLeavesVariableForCleanup() {
        rewriteRun(
            java(
                """
                import org.primefaces.context.RequestContext;

                class Example {
                    void run() {
                        RequestContext context = RequestContext.getCurrentInstance();
                        context.execute("doSomething()");
                        context.update("form:table");
                    }
                }
                """,
                """
                import org.primefaces.PrimeFaces;
                import org.primefaces.context.RequestContext;

                class Example {
                    void run() {
                        RequestContext context = RequestContext.getCurrentInstance();
                        PrimeFaces.current().executeScript("doSomething()");
                        PrimeFaces.current().ajax().update("form:table");
                    }
                }
                """
            )
        );
    }

    @Test
    void leavesUnsupportedRequestContextApisForManualRemediation() {
        rewriteRun(
            java(
                """
                import java.util.Map;
                import org.primefaces.context.RequestContext;

                class Example {
                    Map<String, Object> callbackParams() {
                        return RequestContext.getCurrentInstance().getCallbackParams();
                    }
                }
                """
            )
        );
    }
}
