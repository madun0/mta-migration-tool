package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Finds InputMask usages with missing/empty masks and server-side mask validation settings that need review.
 */
public class PrimeFaces62InputMaskInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory PrimeFaces 6.2 InputMask compatibility";
    }

    @Override
    public String getDescription() {
        return "Finds InputMask usages with missing/empty masks and server-side mask validation settings that need review.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTags("//p:inputMask"),
                new FindTags("//p:inputMask[not(@mask)]"),
                new FindTags("//p:inputMask[@mask='']"),
                new FindTags("//p:inputMask[@validateMask='false']")
        );
    }
}
