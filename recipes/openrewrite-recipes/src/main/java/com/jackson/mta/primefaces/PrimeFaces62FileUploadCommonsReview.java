package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.AddCommentToXmlTag;

import java.util.List;

/**
 * Marks PrimeFaces FileUploadFilter and primefaces.UPLOADER configuration that should be removed or converted to native Servlet upload for PrimeFaces 15+.
 */
public class PrimeFaces62FileUploadCommonsReview extends Recipe {

    @Override
    public String getDisplayName() {
        return "Review legacy Commons FileUpload configuration";
    }

    @Override
    public String getDescription() {
        return "Marks PrimeFaces FileUploadFilter and primefaces.UPLOADER configuration that should be removed or converted to native Servlet upload for PrimeFaces 15+.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new AddCommentToXmlTag(
                        "//filter[filter-class='org.primefaces.webapp.filter.FileUploadFilter']",
                        "PF16 migration review: Commons FileUpload support and PrimeFaces FileUploadFilter were removed in PrimeFaces 15. Remove this filter after confirming native Servlet multipart upload."),
                new AddCommentToXmlTag(
                        "//context-param[param-name='primefaces.UPLOADER']",
                        "PF16 migration review: PrimeFaces 15 removed Apache Commons FileUpload support. Remove legacy commons/auto uploader configuration after confirming native Servlet multipart upload.")
        );
    }
}
