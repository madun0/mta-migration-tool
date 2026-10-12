package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Finds ScheduleEvent, DefaultScheduleEvent, LazyScheduleModel, p:schedule, and explicit schedule timezone usage.
 */
public class PrimeFaces62ScheduleInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory PrimeFaces 6.2 Schedule migration risks";
    }

    @Override
    public String getDescription() {
        return "Finds ScheduleEvent, DefaultScheduleEvent, LazyScheduleModel, p:schedule, and explicit schedule timezone usage.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTypes("org.primefaces.model.DefaultScheduleEvent", false),
                new FindTypes("org.primefaces.model.ScheduleEvent", false),
                new FindTypes("org.primefaces.model.LazyScheduleModel", false),
                new FindTags("//p:schedule"),
                new FindTags("//p:schedule[@timeZone]")
        );
    }
}
