package com.jackson.mta.primefaces;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

import static org.openrewrite.Tree.randomId;

/**
 * Migrates PrimeFaces {@code p:repeat} to the standard Jakarta Faces
 * {@code ui:repeat} tag while ensuring the document root declares the
 * {@code ui} namespace.
 *
 * <p>The namespace and tag rename are applied in one visitor pass. This is
 * important because separate XML recipe cycles may remove an apparently unused
 * namespace before the renamed {@code ui:repeat} node exists.</p>
 */
public class PrimeFaces62RepeatToUiRepeat extends Recipe {

    private static final String UI_NAMESPACE_ATTRIBUTE = "xmlns:ui";
    private static final String UI_NAMESPACE_VALUE = "jakarta.faces.facelets";

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces p:repeat to ui:repeat";
    }

    @Override
    public String getDescription() {
        return "Replaces removed PrimeFaces p:repeat tags with standard Facelets ui:repeat and ensures a Jakarta Faces ui namespace is available.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            private boolean migrateDocument;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                migrateDocument = containsPrimeFacesRepeat(document.getRoot());
                Xml.Document migrated = super.visitDocument(document, ctx);
                if (!migrateDocument || migrated.getRoot() == null) {
                    return migrated;
                }

                Xml.Tag root = migrated.getRoot();
                if (!hasAttribute(root, UI_NAMESPACE_ATTRIBUTE)) {
                    Xml.Ident name = new Xml.Ident(
                            randomId(), "", Markers.EMPTY, UI_NAMESPACE_ATTRIBUTE);
                    Xml.Attribute.Value value = new Xml.Attribute.Value(
                            randomId(), "", Markers.EMPTY,
                            Xml.Attribute.Value.Quote.Double, UI_NAMESPACE_VALUE);
                    Xml.Attribute namespace = new Xml.Attribute(
                            randomId(), " ", Markers.EMPTY, name, "", value);
                    root = root.withAttributes(ListUtils.concat(root.getAttributes(), namespace));
                    migrated = migrated.withRoot(root);
                }
                return migrated;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag visited = super.visitTag(tag, ctx);
                if ("p:repeat".equals(visited.getName())) {
                    return visited.withName("ui:repeat");
                }
                return visited;
            }
        };
    }

    /**
     * Returns whether the source document contains at least one PrimeFaces repeat tag.
     */
    private boolean containsPrimeFacesRepeat(Xml.Tag tag) {
        if (tag == null) {
            return false;
        }
        if ("p:repeat".equals(tag.getName())) {
            return true;
        }
        List<? extends Content> content = tag.getContent();
        if (content == null) {
            return false;
        }
        for (Content child : content) {
            if (child instanceof Xml.Tag childTag && containsPrimeFacesRepeat(childTag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the tag already declares the requested attribute.
     */
    private boolean hasAttribute(Xml.Tag tag, String attributeName) {
        return tag.getAttributes().stream()
                .anyMatch(attribute -> attributeName.equals(attribute.getKeyAsString()));
    }
}
