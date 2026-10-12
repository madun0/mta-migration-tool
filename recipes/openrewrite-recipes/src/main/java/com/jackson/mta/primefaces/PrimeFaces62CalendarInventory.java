package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Inventory recipe for PrimeFaces 6.2 p:calendar usages and migration-sensitive
 * attributes before converting to PrimeFaces DatePicker.
 */
public class PrimeFaces62CalendarInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory PrimeFaces 6.2 Calendar migration risks";
    }

    @Override
    public String getDescription() {
        return "Finds p:calendar usages and legacy Calendar attributes that require review during DatePicker migration.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTags("//p:calendar"),
                new FindTags("//p:calendar[@effect]"),
                new FindTags("//p:calendar[@effectDuration]"),
                new FindTags("//p:calendar[@popupIconOnly]"),
                new FindTags("//p:calendar[@beforeShowDay]"),
                new FindTags("//p:calendar[@disabledWeekends]"),
                new FindTags("//p:calendar[@pagedate]"),
                new FindTags("//p:calendar[@timeControlType]"),
                new FindTags("//p:calendar[@timeControlObject]"),
                new FindTags("//p:calendar[@oneLine]"),
                new FindTags("//p:calendar[@showHour]"),
                new FindTags("//p:calendar[@showMinute]"),
                new FindTags("//p:calendar[@showMillisec]"),
                new FindTags("//p:calendar[contains(@pattern,'H') or contains(@pattern,'h')]")
        );
    }
}
