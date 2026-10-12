package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.AddCommentToXmlTag;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Inventories p:watermark and scripts because cleanWatermarks/showWatermarks were removed when Watermark moved to HTML placeholder behavior.
 */
public class PrimeFaces62WatermarkReview extends Recipe {

    @Override
    public String getDisplayName() {
        return "Review PrimeFaces Watermark client API";
    }

    @Override
    public String getDescription() {
        return "Inventories p:watermark and scripts because cleanWatermarks/showWatermarks were removed when Watermark moved to HTML placeholder behavior.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTags("//p:watermark"),
                new FindTags("//script"),
                new AddCommentToXmlTag(
                        "//p:watermark",
                        "PF16 migration note: Watermark now uses HTML placeholder behavior. Remove any PrimeFaces.cleanWatermarks()/showWatermarks() calls associated with this view.")
        );
    }
}
