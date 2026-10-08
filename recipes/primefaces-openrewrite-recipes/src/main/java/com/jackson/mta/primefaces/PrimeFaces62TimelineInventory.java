package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Finds TimelineEvent, TimelineModel, p:timeline, Date-backed timeline bounds, and legacy Timeline attributes.
 */
public class PrimeFaces62TimelineInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory PrimeFaces 6.2 Timeline migration risks";
    }

    @Override
    public String getDescription() {
        return "Finds TimelineEvent, TimelineModel, p:timeline, Date-backed timeline bounds, and legacy Timeline attributes.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTypes("org.primefaces.model.timeline.TimelineEvent", false),
                new FindTypes("org.primefaces.model.timeline.TimelineModel", false),
                new FindTags("//p:timeline"),
                new FindTags("//p:timeline[@axisOnTop or @groupsChangeable or @groupsWidth or @groupMinHeight or @snapEvents or @timeChangeable]"),
                new FindTags("//p:timeline[@dragAreaWidth or @unselectable or @groupsOnRight or @animate or @animateZoom or @showButtonNew or @showNavigation or @browserTimeZone]"),
                new FindTags("//p:timeline[@start or @end or @min or @max or @timeZone or @clientTimeZone]")
        );
    }
}
