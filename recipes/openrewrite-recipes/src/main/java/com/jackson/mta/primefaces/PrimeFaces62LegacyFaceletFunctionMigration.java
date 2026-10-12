package com.jackson.mta.primefaces;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites p:component(...) and p:widgetVar(...) to the PrimeFaces 8+ resolveFirstComponentWithId/resolveWidgetVar functions.
 */
public class PrimeFaces62LegacyFaceletFunctionMigration extends Recipe {

    private static final Pattern COMPONENT =
            Pattern.compile("p:component\\(([^()]*)\\)");

    private static final Pattern WIDGET_VAR =
            Pattern.compile("p:widgetVar\\(([^()]*)\\)");

    @Override
    public String getDisplayName() {
        return "Migrate removed PrimeFaces Facelet functions";
    }

    @Override
    public String getDescription() {
        return "Rewrites p:component(...) and p:widgetVar(...) to the PrimeFaces 8+ resolveFirstComponentWithId/resolveWidgetVar functions.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {

            @Override
            public Xml visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                Xml.Attribute a = (Xml.Attribute) super.visitAttribute(attribute, ctx);
                String oldValue = a.getValueAsString();
                String newValue = migrate(oldValue);

                if (oldValue.equals(newValue)) {
                    return a;
                }

                return a.withValue(a.getValue().withValue(newValue));
            }

            @Override
            public Xml visitCharData(Xml.CharData charData, ExecutionContext ctx) {
                Xml.CharData c = (Xml.CharData) super.visitCharData(charData, ctx);
                String oldText = c.getText();
                String newText = migrate(oldText);
                return oldText.equals(newText) ? c : c.withText(newText);
            }

            private String migrate(String value) {
                String migrated = replace(
                        COMPONENT,
                        value,
                        "p:resolveFirstComponentWithId($1, view).clientId");

                return replace(
                        WIDGET_VAR,
                        migrated,
                        "p:resolveWidgetVar($1, view)");
            }

            private String replace(Pattern pattern, String input, String replacement) {
                Matcher matcher = pattern.matcher(input);
                return matcher.find() ? matcher.replaceAll(replacement) : input;
            }
        };
    }
}
