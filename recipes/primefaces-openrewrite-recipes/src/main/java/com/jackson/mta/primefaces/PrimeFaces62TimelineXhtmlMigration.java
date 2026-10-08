package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.AddCommentToXmlTag;
import org.openrewrite.xml.AddTagAttribute;
import org.openrewrite.xml.ChangeTagAttribute;
import org.openrewrite.xml.ChangeTagAttributeKey;

import java.util.List;

/**
 * Migrates high-confidence deprecated Timeline attributes and flags ambiguous or removed options for review.
 */
public class PrimeFaces62TimelineXhtmlMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces 6.2 Timeline attributes";
    }

    @Override
    public String getDescription() {
        return "Migrates high-confidence deprecated Timeline attributes and flags ambiguous or removed options for review.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new AddTagAttribute("//p:timeline[@axisOnTop='true']", "orientationAxis", "top"),
                new AddTagAttribute("//p:timeline[@axisOnTop='false']", "orientationAxis", "bottom"),
                new ChangeTagAttribute("//p:timeline[@axisOnTop='true' or @axisOnTop='false']", "axisOnTop", null, null, false),
                new AddCommentToXmlTag(
                        "//p:timeline[@axisOnTop and not(@axisOnTop='true') and not(@axisOnTop='false')]",
                        "PF16 migration review: axisOnTop became orientationAxis (top/bottom). Convert this dynamic expression explicitly."),
                new ChangeTagAttributeKey("//p:timeline/@groupsChangeable", "editableGroup"),
                new ChangeTagAttributeKey("//p:timeline/@timeChangeable", "editableTime"),
                new AddTagAttribute("//p:timeline[@snapEvents='false']", "snap", "null"),
                new ChangeTagAttribute("//p:timeline[@snapEvents='false' or @snapEvents='true']", "snapEvents", null, null, false),
                new AddCommentToXmlTag(
                        "//p:timeline[@snapEvents and not(@snapEvents='true') and not(@snapEvents='false')]",
                        "PF16 migration review: snapEvents became the snap JavaScript-function option. Preserve true as default snapping; false maps to snap=\"null\"."),
                new AddCommentToXmlTag(
                        "//p:timeline[@dragAreaWidth or @unselectable or @groupsOnRight or @animate or @animateZoom or @showButtonNew or @showNavigation or @browserTimeZone]",
                        "PF16 migration review: one or more Timeline attributes were removed without a direct replacement."),
                new AddCommentToXmlTag(
                        "//p:timeline[@groupsWidth or @groupMinHeight]",
                        "PF16 migration review: groupsWidth/groupMinHeight require review against current Timeline layout/styling options."),
                new AddCommentToXmlTag(
                        "//p:timeline[@start or @end or @min or @max]",
                        "PF16 migration review: Timeline start/end/min/max expect LocalDateTime. Verify bean properties and timezone conversion.")
        );
    }
}
