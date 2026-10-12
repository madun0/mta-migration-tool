package com.jackson.mta.primefaces;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.List;

/**
 * Migrates high-confidence PrimeFaces 6.2 programmatic menu API patterns:
 *
 * <ul>
 *   <li>{@code addElement(x)} to {@code getElements().add(x)}</li>
 *   <li>legacy {@code DefaultMenuItem(...)} constructors to builder APIs</li>
 *   <li>legacy {@code DefaultSubMenu(...)} constructors to builder APIs</li>
 * </ul>
 *
 * <p>No-argument {@code DefaultMenuItem()} and {@code DefaultSubMenu()}
 * constructors remain supported and are intentionally left untouched. This
 * prevents the recipe from generating invalid {@code value()} or
 * {@code label()} calls with no arguments.</p>
 */
public class MigrateProgrammaticMenuApi extends Recipe {

    private static final String MENU_MODEL =
            "org.primefaces.model.menu.MenuModel";
    private static final String BASE_MENU_MODEL =
            "org.primefaces.model.menu.BaseMenuModel";
    private static final String DEFAULT_MENU_ITEM =
            "org.primefaces.model.menu.DefaultMenuItem";
    private static final String DEFAULT_SUB_MENU =
            "org.primefaces.model.menu.DefaultSubMenu";

    private static final String TARGET_MENU_ITEM_STUB = """
            package org.primefaces.model.menu;

            public class DefaultMenuItem {
                public DefaultMenuItem() {
                }

                public static Builder builder() {
                    return new Builder();
                }

                public static class Builder {
                    public Builder value(Object value) {
                        return this;
                    }

                    public Builder icon(String icon) {
                        return this;
                    }

                    public Builder url(String url) {
                        return this;
                    }

                    public DefaultMenuItem build() {
                        return new DefaultMenuItem();
                    }
                }
            }
            """;

    private static final String TARGET_SUB_MENU_STUB = """
            package org.primefaces.model.menu;

            import java.util.ArrayList;
            import java.util.List;

            public class DefaultSubMenu {
                public DefaultSubMenu() {
                }

                public List<Object> getElements() {
                    return new ArrayList<>();
                }

                public static Builder builder() {
                    return new Builder();
                }

                public static class Builder {
                    public Builder label(String label) {
                        return this;
                    }

                    public Builder icon(String icon) {
                        return this;
                    }

                    public DefaultSubMenu build() {
                        return new DefaultSubMenu();
                    }
                }
            }
            """;

    private static final String TARGET_MENU_MODEL_STUB = """
            package org.primefaces.model.menu;

            import java.util.List;

            public interface MenuModel {
                List<Object> getElements();
            }
            """;

    private static final String TARGET_BASE_MENU_MODEL_STUB = """
            package org.primefaces.model.menu;

            import java.util.ArrayList;
            import java.util.List;

            public class BaseMenuModel implements MenuModel {
                private final List<Object> elements = new ArrayList<>();

                @Override
                public List<Object> getElements() {
                    return elements;
                }
            }
            """;

    @Override
    public String getDisplayName() {
        return "Migrate PrimeFaces 6.2 programmatic menu APIs";
    }

    @Override
    public String getDescription() {
        return "Migrates removed/deprecated PrimeFaces menu constructors "
                + "and addElement calls to current builder and "
                + "getElements().add APIs.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitNewClass(
                    J.NewClass newClass,
                    ExecutionContext ctx) {

                J visited = super.visitNewClass(newClass, ctx);
                if (!(visited instanceof J.NewClass)) {
                    return visited;
                }

                J.NewClass n = (J.NewClass) visited;

                if (TypeUtils.isOfClassType(
                        n.getType(),
                        DEFAULT_MENU_ITEM)) {
                    return migrateMenuItemConstructor(n, ctx);
                }

                if (TypeUtils.isOfClassType(
                        n.getType(),
                        DEFAULT_SUB_MENU)) {
                    return migrateSubMenuConstructor(n, ctx);
                }

                return n;
            }

            @Override
            public J visitMethodInvocation(
                    J.MethodInvocation method,
                    ExecutionContext ctx) {

                J visited =
                        super.visitMethodInvocation(method, ctx);

                if (!(visited instanceof J.MethodInvocation)) {
                    return visited;
                }

                J.MethodInvocation m =
                        (J.MethodInvocation) visited;

                List<Expression> args =
                        meaningfulArguments(m.getArguments());

                if (!"addElement".equals(m.getSimpleName())
                        || m.getSelect() == null
                        || args.size() != 1) {
                    return m;
                }

                if (!isMenuContainer(m.getSelect())) {
                    return m;
                }

                JavaTemplate template =
                        JavaTemplate.builder(
                                        "#{any()}.getElements().add(#{any()})")
                                .javaParser(targetParser())
                                .contextSensitive()
                                .build();

                J original = m;
                J migrated =
                        template.apply(
                                updateCursor(m),
                                m.getCoordinates().replace(),
                                m.getSelect(),
                                args.get(0));

                return maybeAutoFormat(original, migrated, ctx);
            }

            private J migrateMenuItemConstructor(
                    J.NewClass n,
                    ExecutionContext ctx) {

                List<Expression> args =
                        meaningfulArguments(n.getArguments());

                /*
                 * Modern PrimeFaces still supports DefaultMenuItem().
                 * Return before adding imports or applying templates.
                 */
                if (args.isEmpty()) {
                    return n;
                }

                String source =
                        switch (args.size()) {
                            case 1 ->
                                    "DefaultMenuItem.builder()"
                                    + ".value(#{any()})"
                                    + ".build()";
                            case 2 ->
                                    "DefaultMenuItem.builder()"
                                    + ".value(#{any()})"
                                    + ".icon(#{any(java.lang.String)})"
                                    + ".build()";
                            case 3 ->
                                    "DefaultMenuItem.builder()"
                                    + ".value(#{any()})"
                                    + ".icon(#{any(java.lang.String)})"
                                    + ".url(#{any(java.lang.String)})"
                                    + ".build()";
                            default -> null;
                        };

                if (source == null) {
                    return n;
                }

                JavaTemplate template =
                        JavaTemplate.builder(source)
                                .imports(DEFAULT_MENU_ITEM)
                                .javaParser(targetParser())
                                .contextSensitive()
                                .build();

                maybeAddImport(DEFAULT_MENU_ITEM);

                J original = n;
                J migrated =
                        template.apply(
                                updateCursor(n),
                                n.getCoordinates().replace(),
                                args.toArray());

                return maybeAutoFormat(original, migrated, ctx);
            }

            private J migrateSubMenuConstructor(
                    J.NewClass n,
                    ExecutionContext ctx) {

                List<Expression> args =
                        meaningfulArguments(n.getArguments());

                /*
                 * Modern PrimeFaces still supports DefaultSubMenu().
                 * Return before adding imports or applying templates.
                 */
                if (args.isEmpty()) {
                    return n;
                }

                String source =
                        switch (args.size()) {
                            case 1 ->
                                    "DefaultSubMenu.builder()"
                                    + ".label(#{any(java.lang.String)})"
                                    + ".build()";
                            case 2 ->
                                    "DefaultSubMenu.builder()"
                                    + ".label(#{any(java.lang.String)})"
                                    + ".icon(#{any(java.lang.String)})"
                                    + ".build()";
                            default -> null;
                        };

                if (source == null) {
                    return n;
                }

                JavaTemplate template =
                        JavaTemplate.builder(source)
                                .imports(DEFAULT_SUB_MENU)
                                .javaParser(targetParser())
                                .contextSensitive()
                                .build();

                maybeAddImport(DEFAULT_SUB_MENU);

                J original = n;
                J migrated =
                        template.apply(
                                updateCursor(n),
                                n.getCoordinates().replace(),
                                args.toArray());

                return maybeAutoFormat(original, migrated, ctx);
            }

            /**
             * Removes OpenRewrite's synthetic J.Empty placeholder from
             * argument lists. This is essential for recognizing no-argument
             * constructor invocations.
             */
            private List<Expression> meaningfulArguments(
                    List<Expression> arguments) {

                return arguments.stream()
                        .filter(argument ->
                                !(argument instanceof J.Empty))
                        .toList();
            }

            private boolean isMenuContainer(Expression select) {
                return TypeUtils.isAssignableTo(
                                MENU_MODEL,
                                select.getType())
                        || TypeUtils.isAssignableTo(
                                BASE_MENU_MODEL,
                                select.getType())
                        || TypeUtils.isAssignableTo(
                                DEFAULT_SUB_MENU,
                                select.getType());
            }

            private JavaParser.Builder<?, ?> targetParser() {
                return JavaParser.fromJavaVersion()
                        .dependsOn(
                                TARGET_MENU_ITEM_STUB,
                                TARGET_SUB_MENU_STUB,
                                TARGET_MENU_MODEL_STUB,
                                TARGET_BASE_MENU_MODEL_STUB);
            }
        };
    }
}
