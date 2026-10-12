package com.jackson.mta.primefaces;

import org.openrewrite.Recipe;
import org.openrewrite.xml.AddCommentToXmlTag;

import java.util.List;

/**
 * PrimeFaces 13 removed UIColumn#filterOptions in favor of a custom "filter"
 * facet. Generating the facet automatically would require knowing the desired
 * filter component and DataTable widgetVar, so this recipe marks each usage
 * with an actionable migration comment instead of deleting behavior.
 */
public class PrimeFaces62DataTableFilterOptionsReview extends Recipe {

    @Override
    public String getDisplayName() {
        return "Mark DataTable filterOptions for facet migration";
    }

    @Override
    public String getDescription() {
        return "Marks removed filterOptions attributes for conversion to a column filter facet.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new AddCommentToXmlTag(
                        "//p:column[@filterOptions]",
                        "PF16 migration review: filterOptions was removed. Replace it with an f:facet name=\\\"filter\\\" containing an appropriate input/select component and invoke the DataTable widget filter() on change."),
                new AddCommentToXmlTag(
                        "//p:columns[@filterOptions]",
                        "PF16 migration review: filterOptions was removed. Replace it with an f:facet name=\\\"filter\\\" containing an appropriate input/select component and invoke the DataTable widget filter() on change.")
        );
    }
}
