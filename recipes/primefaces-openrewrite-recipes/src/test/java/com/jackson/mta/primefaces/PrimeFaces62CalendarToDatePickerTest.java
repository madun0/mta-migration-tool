package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

/** Verifies Calendar markup is transformed to DatePicker markup. */
class PrimeFaces62CalendarToDatePickerTest implements RewriteTest {
    @Test void migratesSafeCalendarAttributes() {
        rewriteRun(
            spec -> spec.recipe(new PrimeFaces62CalendarToDatePicker()),
            xml(
                "<p:calendar xmlns:p=\"http://primefaces.org/ui\" pages=\"2\"/>",
                "<p:datePicker xmlns:p=\"http://primefaces.org/ui\" numberOfMonths=\"2\" showIcon=\"true\"/>"));
    }
}
