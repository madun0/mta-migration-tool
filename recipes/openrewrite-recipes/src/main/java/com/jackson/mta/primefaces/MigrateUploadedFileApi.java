package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeType;

import java.util.List;

/**
 * Migrates the PrimeFaces 6.2 UploadedFile interface to the package/method
 * names introduced in PrimeFaces 8.
 */
public class MigrateUploadedFileApi extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces UploadedFile API";
    }

    @Override
    public String getDescription() {
        return "Migrates org.primefaces.model.UploadedFile to org.primefaces.model.file.UploadedFile and renames getContents/getInputstream.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangeMethodName(
                        "org.primefaces.model.UploadedFile getContents()",
                        "getContent",
                        true,
                        false),
                new ChangeMethodName(
                        "org.primefaces.model.UploadedFile getInputstream()",
                        "getInputStream",
                        true,
                        false),
                new ChangeType(
                        "org.primefaces.model.UploadedFile",
                        "org.primefaces.model.file.UploadedFile",
                        false)
        );
    }
}
