package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/**
 * Tests migration of LazyScheduleModel Date parameters to LocalDateTime while
 * retaining the explicit timezone-review warning.
 */
class MigrateLazyScheduleModelTest implements RewriteTest {

    private static final String LEGACY = """
            package org.primefaces.model;

            import java.util.Date;

            public abstract class LazyScheduleModel {
                public abstract void loadEvents(Date start, Date end);
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateLazyScheduleModel())
            .parser(JavaParser.fromJavaVersion().dependsOn(LEGACY));
    }

    @Test
    void migratesLoadEventsWithCompatibilityDates() {
        rewriteRun(
            java(
                """
                import java.util.Date;
                import org.primefaces.model.LazyScheduleModel;

                class Example extends LazyScheduleModel {
                    @Override
                    public void loadEvents(Date start, Date end) {
                        System.out.println(start);
                        System.out.println(end);
                    }
                }
                """,
                """
                import java.time.LocalDateTime;
                import java.time.ZoneId;
                import java.util.Date;
                import org.primefaces.model.LazyScheduleModel;

                class Example extends LazyScheduleModel {

                    @Override
                    public void loadEvents(LocalDateTime __mtaStart, LocalDateTime __mtaEnd) {
                        /* MTA compatibility bridge: review ZoneId.systemDefault() against p:schedule timeZone/clientTimeZone and business timezone rules. */
                        Date start = __mtaStart == null ? null : Date.from(__mtaStart.atZone(ZoneId.systemDefault()).toInstant());
                        Date end = __mtaEnd == null ? null : Date.from(__mtaEnd.atZone(ZoneId.systemDefault()).toInstant());
                        System.out.println(start);
                        System.out.println(end);
                    }
                }
                """
            )
        );
    }
}
