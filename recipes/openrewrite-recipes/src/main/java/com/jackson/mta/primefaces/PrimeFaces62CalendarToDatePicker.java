package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.AddCommentToXmlTag;
import org.openrewrite.xml.AddTagAttribute;
import org.openrewrite.xml.ChangeTagAttribute;
import org.openrewrite.xml.ChangeTagAttributeKey;
import org.openrewrite.xml.ChangeTagName;

import java.util.List;

/**
 * High-confidence PrimeFaces 6.2 Calendar -> DatePicker transformations.
 *
 * Complex temporal semantics and callback options are annotated for manual
 * review rather than guessed automatically.
 */
public class PrimeFaces62CalendarToDatePicker extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces Calendar to DatePicker";
    }

    @Override
    public String getDescription() {
        return "Migrates high-confidence p:calendar markup to p:datePicker and flags options that need semantic review.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangeTagName("//p:calendar", "p:datePicker"),

                // Preserve Calendar's default popup icon behavior.
                new AddTagAttribute(
                        "//p:datePicker[not(@showOn) and not(@mode='inline')]",
                        "showIcon",
                        "true"),

                // mode="inline" -> inline="true"; popup is DatePicker default.
                new AddTagAttribute(
                        "//p:datePicker[@mode='inline']",
                        "inline",
                        "true"),
                new ChangeTagAttribute(
                        "//p:datePicker",
                        "mode",
                        null,
                        "inline",
                        false),
                new ChangeTagAttribute(
                        "//p:datePicker",
                        "mode",
                        null,
                        "popup",
                        false),

                new ChangeTagAttributeKey(
                        "//p:datePicker/@pages",
                        "numberOfMonths"),
                new ChangeTagAttributeKey(
                        "//p:datePicker/@showButtonPanel",
                        "showButtonBar"),
                new ChangeTagAttributeKey(
                        "//p:datePicker/@defaultMillisec",
                        "defaultMillisecond"),

                // navigator=true -> monthNavigator/yearNavigator.
                new AddTagAttribute(
                        "//p:datePicker[@navigator='true']",
                        "monthNavigator",
                        "true"),
                new AddTagAttribute(
                        "//p:datePicker[@navigator='true']",
                        "yearNavigator",
                        "true"),
                new ChangeTagAttribute(
                        "//p:datePicker",
                        "navigator",
                        null,
                        null,
                        false),

                // Calendar showOn semantics -> DatePicker showIcon/showOnFocus.
                new AddTagAttribute(
                        "//p:datePicker[@showOn='button']",
                        "showIcon",
                        "true"),
                new AddTagAttribute(
                        "//p:datePicker[@showOn='button']",
                        "showOnFocus",
                        "false"),
                new AddTagAttribute(
                        "//p:datePicker[@showOn='both']",
                        "showIcon",
                        "true"),
                new AddTagAttribute(
                        "//p:datePicker[@showOn='both']",
                        "showOnFocus",
                        "true"),
                new AddTagAttribute(
                        "//p:datePicker[@showOn='focus']",
                        "showIcon",
                        "false"),
                new AddTagAttribute(
                        "//p:datePicker[@showOn='focus']",
                        "showOnFocus",
                        "true"),
                new ChangeTagAttribute(
                        "//p:datePicker",
                        "showOn",
                        null,
                        null,
                        false),

                // Flag cases where a direct rewrite would be unsafe.
                new AddCommentToXmlTag(
                        "//p:datePicker[contains(@pattern,'H') or contains(@pattern,'h')]",
                        "PF16 migration review: old Calendar encoded time in pattern. DatePicker uses showTime/hourFormat/showSeconds; migrate the pattern and backing value to LocalDateTime/LocalTime as appropriate."),
                new AddCommentToXmlTag(
                        "//p:datePicker[@beforeShowDay]",
                        "PF16 migration review: beforeShowDay has no direct DatePicker mapping; use dateTemplate/disabledDates/disabledDays or application-specific client customization."),
                new AddCommentToXmlTag(
                        "//p:datePicker[@effect or @effectDuration]",
                        "PF16 migration review: legacy Calendar effect/effectDuration options do not have a direct DatePicker equivalent."),
                new AddCommentToXmlTag(
                        "//p:datePicker[@popupIconOnly]",
                        "PF16 migration review: popupIconOnly requires UX review; DatePicker uses showIcon/showOnFocus rather than Calendar popupIconOnly."),
                new AddCommentToXmlTag(
                        "//p:datePicker[@disabledWeekends]",
                        "PF16 migration review: migrate disabledWeekends to disabledDays (typically weekend indexes) using an application-owned List<Integer>."),
                new AddCommentToXmlTag(
                        "//p:datePicker[@pagedate or @timeControlType or @timeControlObject or @oneLine or @showHour or @showMinute or @showMillisec]",
                        "PF16 migration review: this legacy Calendar option does not have a safe one-to-one DatePicker mapping; review against the target DatePicker API.")
        );
    }
}
