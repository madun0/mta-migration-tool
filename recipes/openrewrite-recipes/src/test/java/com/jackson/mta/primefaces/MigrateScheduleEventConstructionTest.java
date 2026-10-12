package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Tests DefaultScheduleEvent constructor migration to the builder-based
 * LocalDateTime API.
 */
class MigrateScheduleEventConstructionTest implements RewriteTest {

    private static final String LEGACY = """
            package org.primefaces.model;

            import java.util.Date;

            public class DefaultScheduleEvent {
                public DefaultScheduleEvent() {
                }

                public DefaultScheduleEvent(String title, Date start, Date end) {
                }

                public DefaultScheduleEvent(String title, Date start, Date end, boolean allDay) {
                }

                public DefaultScheduleEvent(String title, Date start, Date end, String styleClass) {
                }

                public DefaultScheduleEvent(String title, Date start, Date end, Object data) {
                }

                public void setStartDate(Date value) {
                }

                public void setEndDate(Date value) {
                }
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateScheduleEventConstruction())
            .parser(JavaParser.fromJavaVersion().dependsOn(LEGACY))
            .afterTypeValidationOptions(TypeValidation.builder()
                    .methodInvocations(false)
                    .identifiers(false)
                    .build());
    }

    @Test
    void migratesThreeArgumentConstructor() {
        rewriteRun(
            java(
                """
                import java.util.Date;
                import org.primefaces.model.DefaultScheduleEvent;

                class Example {
                    Object event(Date start, Date end) {
                        return new DefaultScheduleEvent("Meeting", start, end);
                    }
                }
                """,
                """
                import java.time.LocalDateTime;
                import java.time.ZoneId;
                import java.util.Date;
                import java.util.Optional;

                import org.primefaces.model.DefaultScheduleEvent;

                class Example {
                    Object event(Date start, Date end) {
                        return DefaultScheduleEvent.builder().title("Meeting").startDate(Optional.ofNullable(start).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).endDate(Optional.ofNullable(end).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).build();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesFourthArgumentOverloads() {
        rewriteRun(
            java(
                """
                import java.util.Date;
                import org.primefaces.model.DefaultScheduleEvent;

                class Example {
                    Object allDay(Date start, Date end) {
                        return new DefaultScheduleEvent("A", start, end, true);
                    }

                    Object styled(Date start, Date end) {
                        return new DefaultScheduleEvent("B", start, end, "important");
                    }

                    Object data(Date start, Date end, Integer id) {
                        return new DefaultScheduleEvent("C", start, end, id);
                    }
                }
                """,
                """
                import java.time.LocalDateTime;
                import java.time.ZoneId;
                import java.util.Date;
                import java.util.Optional;

                import org.primefaces.model.DefaultScheduleEvent;

                class Example {
                    Object allDay(Date start, Date end) {
                        return DefaultScheduleEvent.builder().title("A").startDate(Optional.ofNullable(start).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).endDate(Optional.ofNullable(end).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).allDay(true).build();
                    }

                    Object styled(Date start, Date end) {
                        return DefaultScheduleEvent.builder().title("B").startDate(Optional.ofNullable(start).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).endDate(Optional.ofNullable(end).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).styleClass("important").build();
                    }

                    Object data(Date start, Date end, Integer id) {
                        return DefaultScheduleEvent.builder().title("C").startDate(Optional.ofNullable(start).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).endDate(Optional.ofNullable(end).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).data(id).build();
                    }
                }
                """
            )
        );
    }
}
