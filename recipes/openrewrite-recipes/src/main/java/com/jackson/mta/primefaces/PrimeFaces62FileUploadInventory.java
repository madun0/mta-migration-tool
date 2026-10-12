package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.xml.search.FindTags;

import java.util.List;

/**
 * Finds legacy UploadedFile API usage, FileUpload listener attributes, simple multiple upload, and Commons FileUpload configuration.
 */
public class PrimeFaces62FileUploadInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory PrimeFaces 6.2 FileUpload usage";
    }

    @Override
    public String getDescription() {
        return "Finds legacy UploadedFile API usage, FileUpload listener attributes, simple multiple upload, and Commons FileUpload configuration.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new FindTypes("org.primefaces.model.UploadedFile", false),
                new FindTypes("org.primefaces.webapp.filter.FileUploadFilter", false),
                new FindTags("//p:fileUpload[@fileUploadListener]"),
                new FindTags("//p:fileUpload[@mode='simple' and @multiple='true']"),
                new FindTags("//p:fileUpload[@validateContentType]"),
                new FindTags("//filter[filter-class='org.primefaces.webapp.filter.FileUploadFilter']"),
                new FindTags("//context-param[param-name='primefaces.UPLOADER']")
        );
    }
}
