package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.AddCommentToXmlTag;
import org.openrewrite.xml.ChangeTagAttributeKey;

import java.util.List;

/**
 * Renames fileUploadListener to listener and dragDropSupport to dragDrop, while flagging simple-multiple and legacy validation cases.
 */
public class PrimeFaces62FileUploadXhtmlMigration extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces FileUpload XHTML";
    }

    @Override
    public String getDescription() {
        return "Renames fileUploadListener to listener and dragDropSupport to dragDrop, while flagging simple-multiple and legacy validation cases.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangeTagAttributeKey(
                        "//p:fileUpload/@fileUploadListener",
                        "listener"),
                new ChangeTagAttributeKey(
                        "//p:fileUpload/@dragDropSupport",
                        "dragDrop"),
                new AddCommentToXmlTag(
                        "//p:fileUpload[@mode='simple' and @multiple='true']",
                        "PF16 migration review: simple multiple upload must bind to org.primefaces.model.file.UploadedFiles rather than a single UploadedFile."),
                new AddCommentToXmlTag(
                        "//p:fileUpload[@validateContentType]",
                        "PF16 migration review: file validation APIs changed in later PrimeFaces releases. Verify content-type validation against p:validateFile and the target FileUpload API.")
        );
    }
}
