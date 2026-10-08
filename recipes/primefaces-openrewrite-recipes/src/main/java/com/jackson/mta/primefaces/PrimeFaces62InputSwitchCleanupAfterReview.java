package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.ChangeTagAttribute;
import org.openrewrite.xml.ChangeTagName;

import java.util.List;

/**
 * Final cleanup for InputSwitch cases that originally used visible text labels.
 *
 * Run only after the developer has reproduced the intended label UX outside
 * the component (or intentionally replaced it with ToggleSwitch icons).
 */
public class PrimeFaces62InputSwitchCleanupAfterReview extends Recipe {

    @Override
    public String getDisplayName() {
        return "Finalize reviewed InputSwitch migration";
    }

    @Override
    public String getDescription() {
        return "Removes legacy InputSwitch-only label attributes and renames the remaining p:inputSwitch tags after their UI semantics have been reviewed.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangeTagAttribute(
                        "//p:inputSwitch",
                        "onLabel",
                        null,
                        null,
                        false),
                new ChangeTagAttribute(
                        "//p:inputSwitch",
                        "offLabel",
                        null,
                        null,
                        false),
                new ChangeTagAttribute(
                        "//p:inputSwitch",
                        "showLabels",
                        null,
                        null,
                        false),
                new ChangeTagName(
                        "//p:inputSwitch",
                        "p:toggleSwitch")
        );
    }
}
