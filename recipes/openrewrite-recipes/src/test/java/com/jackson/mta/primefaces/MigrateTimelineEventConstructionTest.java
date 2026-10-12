package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Tests TimelineEvent constructor migration to the builder-based
 * LocalDateTime API.
 */
class MigrateTimelineEventConstructionTest implements RewriteTest {

    private static final String LEGACY = """
            package org.primefaces.model.timeline;

            import java.util.Date;

            public class TimelineEvent {
                public TimelineEvent() {
                }

                public TimelineEvent(Object data, Date startDate) {
                }

                public TimelineEvent(Object data, Date startDate, Boolean editable) {
                }

                public TimelineEvent(Object data, Date startDate, Boolean editable,
                                     String group) {
                }

                public TimelineEvent(Object data, Date startDate, Boolean editable,
                                     String group, String styleClass) {
                }

                public TimelineEvent(Object data, Date startDate, Date endDate) {
                }

                public TimelineEvent(Object data, Date startDate, Date endDate,
                                     Boolean editable) {
                }

                public TimelineEvent(Object data, Date startDate, Date endDate,
                                     Boolean editable, String group) {
                }

                public TimelineEvent(Object data, Date startDate, Date endDate,
                                     Boolean editable, String group,
                                     String styleClass) {
                }

                public void setStartDate(Date value) {
                }

                public void setEndDate(Date value) {
                }
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateTimelineEventConstruction())
            .parser(JavaParser.fromJavaVersion().dependsOn(LEGACY))
            .afterTypeValidationOptions(TypeValidation.builder()
                    .methodInvocations(false)
                    .identifiers(false)
                    .build());
    }

    @Test
    void migratesStartOnlyConstructor() {
        rewriteRun(
            java(
                """
                import java.util.Date;
                import org.primefaces.model.timeline.TimelineEvent;

                class Example {
                    Object event(Date start) {
                        return new TimelineEvent("Deployment", start);
                    }
                }
                """,
                """
                import java.time.LocalDateTime;
                import java.time.ZoneId;
                import java.util.Date;
                import java.util.Optional;

                import org.primefaces.model.timeline.TimelineEvent;

                class Example {
                    Object event(Date start) {
                        return TimelineEvent.builder().data("Deployment").startDate(Optional.ofNullable(start).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).build();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesFullRangeConstructor() {
        rewriteRun(
            java(
                """
                import java.util.Date;
                import org.primefaces.model.timeline.TimelineEvent;

                class Example {
                    Object event(Date start, Date end) {
                        return new TimelineEvent(
                                "Deployment", start, end, true, "ops", "important");
                    }
                }
                """,
                """
                import java.time.LocalDateTime;
                import java.time.ZoneId;
                import java.util.Date;
                import java.util.Optional;

                import org.primefaces.model.timeline.TimelineEvent;

                class Example {
                    Object event(Date start, Date end) {
                        return TimelineEvent.builder().data("Deployment").startDate(Optional.ofNullable(start).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).endDate(Optional.ofNullable(end).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).editable(true).group("ops").styleClass("important").build();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesEditableFamilyWithoutEndDate() {
        rewriteRun(
            java(
                """
                import java.util.Date;
                import org.primefaces.model.timeline.TimelineEvent;

                class Example {
                    Object event(Date start) {
                        return new TimelineEvent(
                                "Deployment", start, true, "ops", "important");
                    }
                }
                """,
                """
                import java.time.LocalDateTime;
                import java.time.ZoneId;
                import java.util.Date;
                import java.util.Optional;

                import org.primefaces.model.timeline.TimelineEvent;

                class Example {
                    Object event(Date start) {
                        return TimelineEvent.builder().data("Deployment").startDate(Optional.ofNullable(start).map(d -> LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault())).orElse(null)).editable(true).group("ops").styleClass("important").build();
                    }
                }
                """
            )
        );
    }

    @Test
    void leavesNoArgConstructorAlone() {
        rewriteRun(
            java(
                """
                import org.primefaces.model.timeline.TimelineEvent;

                class Example {
                    Object event() {
                        return new TimelineEvent();
                    }
                }
                """
            )
        );
    }
}
