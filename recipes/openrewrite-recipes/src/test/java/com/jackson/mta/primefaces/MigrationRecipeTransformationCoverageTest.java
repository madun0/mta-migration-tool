package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/**
 * Behavioral regression coverage for catalog-facing PrimeFaces XHTML recipes.
 *
 * <p>These tests intentionally execute recipes against representative legacy
 * markup and compare the resulting document. They complement the focused Java
 * API tests and prevent metadata-only tests from giving a false sense that a
 * recipe actually performs its advertised transformation.</p>
 */
class MigrationRecipeTransformationCoverageTest implements RewriteTest {

    @Test
    void fileUploadRenamesLegacyListenerAndDragDropAttributes() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62FileUploadXhtmlMigration()),
            xml(
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:fileUpload fileUploadListener="#{bean.upload}" dragDropSupport="false"/>
                </ui:composition>
                """,
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:fileUpload listener="#{bean.upload}" dragDrop="false"/>
                </ui:composition>
                """
            )
        );
    }

    @Test
    void calendarBecomesDatePickerAndRenamesSafeAttributes() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62CalendarToDatePicker()),
            xml(
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:calendar value="#{bean.date}" pages="2" showButtonPanel="true"/>
                </ui:composition>
                """,
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:datePicker value="#{bean.date}" numberOfMonths="2" showButtonBar="true" showIcon="true"/>
                </ui:composition>
                """
            )
        );
    }

    @Test
    void overlayPanelConvertsAppendToBodyTrue() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62OverlayPanelAppendToMigration()),
            xml(
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:overlayPanel appendToBody="true"/>
                </ui:composition>
                """,
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:overlayPanel appendTo="@(body)"/>
                </ui:composition>
                """
            )
        );
    }

    @Test
    void rowsPerPageStarUsesShowAllSyntax() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62RowsPerPageMigration()),
            xml(
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:dataTable rowsPerPageTemplate="*"/>
                </ui:composition>
                """,
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:dataTable rowsPerPageTemplate="{ShowAll|'*'}"/>
                </ui:composition>
                """
            )
        );
    }

    @Test
    void scheduleConvertsLegacyAgendaView() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62ScheduleXhtmlMigration()),
            xml(
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:schedule view="agendaWeek"/>
                </ui:composition>
                """,
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:schedule view="timeGridWeek"/>
                </ui:composition>
                """
            )
        );
    }

    @Test
    void timelineConvertsAxisAndEditableAttributes() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62TimelineXhtmlMigration()),
            xml(
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:timeline axisOnTop="true" groupsChangeable="true" timeChangeable="false"/>
                </ui:composition>
                """,
                """
                <ui:composition xmlns:ui="http://java.sun.com/jsf/facelets"
                                xmlns:p="http://primefaces.org/ui">
                    <p:timeline editableGroup="true" editableTime="false" orientationAxis="top"/>
                </ui:composition>
                """
            )
        );
    }

    @Test
    void repeatBecomesStandardFaceletsRepeat() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62RepeatToUiRepeat()),
            xml(
                """
                <h:body xmlns:h="http://java.sun.com/jsf/html"
                        xmlns:p="http://primefaces.org/ui">
                    <p:repeat value="#{bean.items}" var="item">
                        <h:outputText value="#{item}"/>
                    </p:repeat>
                </h:body>
                """,
                """
                <h:body xmlns:h="http://java.sun.com/jsf/html"
                        xmlns:p="http://primefaces.org/ui" xmlns:ui="jakarta.faces.facelets">
                    <ui:repeat value="#{bean.items}" var="item">
                        <h:outputText value="#{item}"/>
                    </ui:repeat>
                </h:body>
                """
            )
        );
    }
}
