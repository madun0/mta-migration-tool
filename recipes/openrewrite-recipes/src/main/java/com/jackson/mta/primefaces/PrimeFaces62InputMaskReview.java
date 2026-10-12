package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.AddCommentToXmlTag;

import java.util.List;

/**
 * Marks InputMask usages that cannot safely migrate because PrimeFaces 7 requires mask and PrimeFaces 8 validates masks server-side.
 */
public class PrimeFaces62InputMaskReview extends Recipe {

    @Override
    public String getDisplayName() {
        return "Mark InputMask compatibility risks";
    }

    @Override
    public String getDescription() {
        return "Marks InputMask usages that cannot safely migrate because PrimeFaces 7 requires mask and PrimeFaces 8 validates masks server-side.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new AddCommentToXmlTag(
                        "//p:inputMask[not(@mask)]",
                        "PF16 migration review: InputMask requires a mask since PrimeFaces 7. Provide a valid mask or replace this component with p:inputText."),
                new AddCommentToXmlTag(
                        "//p:inputMask[@mask='']",
                        "PF16 migration review: an empty InputMask mask is no longer valid. Provide a valid mask or conditionally render/use p:inputText."),
                new AddCommentToXmlTag(
                        "//p:inputMask[contains(@mask,'#{')]",
                        "PF16 migration review: this InputMask uses a dynamic mask expression. Confirm it always resolves to a non-empty valid PrimeFaces 16 mask."),
                new AddCommentToXmlTag(
                        "//p:inputMask[@validateMask='false']",
                        "PF16 migration review: validateMask=false disables server-side mask validation. Verify whether this is still required for a custom mask definition.")
        );
    }
}
