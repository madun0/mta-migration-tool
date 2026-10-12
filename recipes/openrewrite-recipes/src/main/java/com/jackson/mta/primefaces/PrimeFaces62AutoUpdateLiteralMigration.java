package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.AddCommentToXmlTag;
import org.openrewrite.xml.AddOrUpdateChildTag;
import org.openrewrite.xml.ChangeTagAttribute;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts literal autoUpdate=true to nested p:autoUpdate, removes literal false, and marks dynamic expressions for review.
 */
public class PrimeFaces62AutoUpdateLiteralMigration extends Recipe {

    private static final String[] COMPONENTS = {
            "outputPanel", "fragment", "messages", "growl"
    };

    @Override
    public String getDisplayName() {
        return "Migrate literal autoUpdate attributes";
    }

    @Override
    public String getDescription() {
        return "Converts literal autoUpdate=true to nested p:autoUpdate, removes literal false, and marks dynamic expressions for review.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        List<Recipe> recipes = new ArrayList<>();

        for (String component : COMPONENTS) {
            String tag = "//p:" + component;

            recipes.add(new AddOrUpdateChildTag(
                    tag + "[@autoUpdate='true']",
                    "<p:autoUpdate/>",
                    false));

            recipes.add(new ChangeTagAttribute(
                    tag,
                    "autoUpdate",
                    null,
                    "true",
                    false));

            recipes.add(new ChangeTagAttribute(
                    tag,
                    "autoUpdate",
                    null,
                    "false",
                    false));

            recipes.add(new AddCommentToXmlTag(
                    tag + "[@autoUpdate and not(@autoUpdate='true') and not(@autoUpdate='false')]",
                    "PF16 migration review: dynamic autoUpdate expressions must become a nested p:autoUpdate with an appropriate disabled expression."));
        }

        return recipes;
    }
}
