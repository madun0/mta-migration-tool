package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies unsafe InputMask markup is explicitly annotated for developer review. */
class PrimeFaces62InputMaskMigrationTest implements RewriteTest {
    @Test void marksMissingMaskForReview() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62InputMaskMigration()),
            xml(
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets" xmlns:p="http://primefaces.org/ui">
                    <p:inputMask value="#{bean.phone}"/>
                </ui:composition>
                """,
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets" xmlns:p="http://primefaces.org/ui">
                    <!--PF16 migration review: InputMask requires a mask since PrimeFaces 7. Provide a valid mask or replace this component with p:inputText.-->
                    <p:inputMask value="#{bean.phone}"/>
                </ui:composition>
                """));
    }
    @Test void leavesStaticNonEmptyMaskUnchanged() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62InputMaskMigration()),
            xml(
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets" xmlns:p="http://primefaces.org/ui">
                    <p:inputMask value="#{bean.phone}" mask="(999) 999-9999"/>
                </ui:composition>
                """));
    }

    @Test void marksDynamicMaskForReview() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62InputMaskMigration()),
            xml(
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets" xmlns:p="http://primefaces.org/ui">
                    <p:inputMask value="#{bean.phone}" mask="#{bean.mask}"/>
                </ui:composition>
                """,
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets" xmlns:p="http://primefaces.org/ui">
                    <!--PF16 migration review: this InputMask uses a dynamic mask expression. Confirm it always resolves to a non-empty valid PrimeFaces 16 mask.-->
                    <p:inputMask value="#{bean.phone}" mask="#{bean.mask}"/>
                </ui:composition>
                """));
    }

}
