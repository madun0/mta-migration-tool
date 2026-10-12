package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.ChangeTagAttribute;

import java.util.List;

/**
 * Replaces legacy FullCalendar Schedule view names with dayGrid/timeGrid names.
 */
public class PrimeFaces62ScheduleXhtmlMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "Normalize PrimeFaces 6.2 Schedule view names";
    }

    @Override
    public String getDescription() {
        return "Replaces legacy FullCalendar Schedule view names with dayGrid/timeGrid names.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangeTagAttribute("//p:schedule", "view", "dayGridMonth", "month", false),
                new ChangeTagAttribute("//p:schedule", "view", "dayGridWeek", "basicWeek", false),
                new ChangeTagAttribute("//p:schedule", "view", "dayGridDay", "basicDay", false),
                new ChangeTagAttribute("//p:schedule", "view", "timeGridWeek", "agendaWeek", false),
                new ChangeTagAttribute("//p:schedule", "view", "timeGridDay", "agendaDay", false)
        );
    }
}
