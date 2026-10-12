package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.ChangeTagAttribute;

import java.util.List;

/**
 * Removes legacy Calendar-only attributes after the intended behavior has
 * already been reproduced and reviewed on PrimeFaces DatePicker.
 */
public class PrimeFaces62CalendarCleanupAfterReview extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove reviewed legacy Calendar-only DatePicker attributes";
    }

    @Override
    public String getDescription() {
        return "Removes legacy p:calendar attributes from migrated p:datePicker tags after manual behavior review.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                remove("effect"),
                remove("effectDuration"),
                remove("popupIconOnly"),
                remove("beforeShowDay"),
                remove("disabledWeekends"),
                remove("pagedate"),
                remove("timeControlType"),
                remove("timeControlObject"),
                remove("oneLine"),
                remove("showHour"),
                remove("showMinute"),
                remove("showMillisec")
        );
    }

    private static Recipe remove(String attributeName) {
        return new ChangeTagAttribute(
                "//p:datePicker",
                attributeName,
                null,
                null,
                false);
    }
}
