package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.AddCommentToXmlTag;
import org.openrewrite.xml.AddTagAttribute;
import org.openrewrite.xml.ChangeTagAttribute;

import java.util.List;

/**
 * Converts appendToBody=true to appendTo=@(body), removes literal false, and marks dynamic expressions for review.
 */
public class PrimeFaces62OverlayPanelAppendToMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate OverlayPanel appendToBody";
    }

    @Override
    public String getDescription() {
        return "Converts appendToBody=true to appendTo=@(body), removes literal false, and marks dynamic expressions for review.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new AddTagAttribute(
                        "//p:overlayPanel[@appendToBody='true']",
                        "appendTo",
                        "@(body)"),
                new ChangeTagAttribute(
                        "//p:overlayPanel",
                        "appendToBody",
                        null,
                        "true",
                        false),
                new ChangeTagAttribute(
                        "//p:overlayPanel",
                        "appendToBody",
                        null,
                        "false",
                        false),
                new AddCommentToXmlTag(
                        "//p:overlayPanel[@appendToBody and not(@appendToBody='true') and not(@appendToBody='false')]",
                        "PF16 migration review: appendToBody was removed. Convert this dynamic expression to an appendTo search expression.")
        );
    }
}
