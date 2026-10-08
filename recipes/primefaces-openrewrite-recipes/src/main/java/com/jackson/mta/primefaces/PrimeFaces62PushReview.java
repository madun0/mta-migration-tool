package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.AddCommentToXmlTag;

import java.util.List;

/**
 * Marks removed p:socket usages for migration to Jakarta Faces f:websocket, Jakarta WebSocket, OmniFaces, or another application-specific messaging design.
 */
public class PrimeFaces62PushReview extends Recipe {

    @Override
    public String getDisplayName() {
        return "Mark PrimeFaces Push for WebSocket redesign";
    }

    @Override
    public String getDescription() {
        return "Marks removed p:socket usages for migration to Jakarta Faces f:websocket, Jakarta WebSocket, OmniFaces, or another application-specific messaging design.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new AddCommentToXmlTag(
                        "//p:socket",
                        "PF16 migration review: PrimeFaces Push was removed in PrimeFaces 7. Replace p:socket and org.primefaces.push APIs with Jakarta Faces f:websocket/Jakarta WebSocket, OmniFaces, or an application-specific messaging solution.")
        );
    }
}
