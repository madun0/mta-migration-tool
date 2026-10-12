package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeType;
import org.openrewrite.xml.AddCommentToXmlTag;
import org.openrewrite.xml.ChangeTagAttribute;
import org.openrewrite.xml.ChangeTagName;

import java.util.List;

/**
 * High-confidence InputSwitch -> ToggleSwitch migration.
 *
 * PrimeFaces 15 removed InputSwitch. ToggleSwitch is its replacement, but the
 * legacy text-label attributes onLabel/offLabel/showLabels have no direct
 * equivalent. Those cases are deliberately left as p:inputSwitch with a
 * migration comment unless showLabels="false", in which case the labels were
 * already hidden and can be dropped safely.
 */
public class PrimeFaces62InputSwitchToToggleSwitch extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces InputSwitch to ToggleSwitch";
    }

    @Override
    public String getDescription() {
        return "Migrates high-confidence p:inputSwitch usages and Java InputSwitch references to ToggleSwitch while preserving label-sensitive cases for review.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // Programmatic component references.
                new ChangeType(
                        "org.primefaces.component.inputswitch.InputSwitch",
                        "org.primefaces.component.toggleswitch.ToggleSwitch",
                        true),

                // If labels were explicitly hidden, removing their old values
                // does not change visible behavior.
                new ChangeTagAttribute(
                        "//p:inputSwitch[@showLabels='false']",
                        "onLabel",
                        null,
                        null,
                        false),
                new ChangeTagAttribute(
                        "//p:inputSwitch[@showLabels='false']",
                        "offLabel",
                        null,
                        null,
                        false),
                new ChangeTagAttribute(
                        "//p:inputSwitch[@showLabels='false']",
                        "showLabels",
                        null,
                        null,
                        false),

                // Straightforward cases are a safe tag rename after the
                // showLabels=false cleanup above.
                new ChangeTagName(
                        "//p:inputSwitch[not(@onLabel) and not(@offLabel) and not(@showLabels)]",
                        "p:toggleSwitch"),

                // Preserve potentially user-visible semantics for manual work.
                new AddCommentToXmlTag(
                        "//p:inputSwitch[@onLabel or @offLabel or @showLabels]",
                        "PF16 migration review: InputSwitch text labels (onLabel/offLabel/showLabels) have no direct ToggleSwitch equivalent. Replace the visual labels with surrounding outputText/label markup, or use onIcon/offIcon when icon semantics are appropriate, then run PrimeFaces62InputSwitchCleanupAfterReview.")
        );
    }
}
