package com.jackson.mta.primefaces;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;

/**
 * Finds p:component(), p:widgetVar(), PrimeFaces.cleanWatermarks(), and PrimeFaces.showWatermarks() in XML/XHTML attribute values or character data.
 */
public class PrimeFaces62LegacyClientApiInventory extends Recipe {

    @Override
    public String getDisplayName() {
        return "Inventory removed PrimeFaces Facelet/client APIs";
    }

    @Override
    public String getDescription() {
        return "Finds p:component(), p:widgetVar(), PrimeFaces.cleanWatermarks(), and PrimeFaces.showWatermarks() in XML/XHTML attribute values or character data.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {

            @Override
            public Xml visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                Xml.Attribute a = (Xml.Attribute) super.visitAttribute(attribute, ctx);
                String value = a.getValueAsString();
                return containsLegacyApi(value)
                        ? SearchResult.found(a, "PrimeFaces 6.2 legacy Facelet/client API")
                        : a;
            }

            @Override
            public Xml visitCharData(Xml.CharData charData, ExecutionContext ctx) {
                Xml.CharData c = (Xml.CharData) super.visitCharData(charData, ctx);
                return containsLegacyApi(c.getText())
                        ? SearchResult.found(c, "PrimeFaces 6.2 legacy Facelet/client API")
                        : c;
            }

            private boolean containsLegacyApi(String text) {
                return text.contains("p:component(")
                        || text.contains("p:widgetVar(")
                        || text.contains("PrimeFaces.cleanWatermarks(")
                        || text.contains("PrimeFaces.showWatermarks(");
            }
        };
    }
}
