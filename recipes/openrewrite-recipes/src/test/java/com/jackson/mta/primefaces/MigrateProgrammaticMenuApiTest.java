package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Tests programmatic menu API migration, including preservation of supported
 * no-argument menu constructors.
 */
class MigrateProgrammaticMenuApiTest implements RewriteTest {

    private static final String MENU_ELEMENT = """
            package org.primefaces.model.menu;
            public interface MenuElement {
            }
            """;

    private static final String MENU_MODEL = """
            package org.primefaces.model.menu;

            import java.util.List;

            public interface MenuModel {
                List<MenuElement> getElements();
                void addElement(MenuElement element);
            }
            """;

    private static final String BASE_MENU_MODEL = """
            package org.primefaces.model.menu;

            import java.util.ArrayList;
            import java.util.List;

            public class BaseMenuModel implements MenuModel {
                private final List<MenuElement> elements = new ArrayList<>();

                @Override
                public List<MenuElement> getElements() {
                    return elements;
                }

                @Override
                public void addElement(MenuElement element) {
                    elements.add(element);
                }
            }
            """;

    private static final String DYNAMIC_MENU_MODEL = """
            package org.primefaces.model.menu;
            public class DynamicMenuModel extends BaseMenuModel {
            }
            """;

    private static final String DEFAULT_MENU_ITEM = """
            package org.primefaces.model.menu;

            public class DefaultMenuItem implements MenuElement {
                public DefaultMenuItem() {
                }

                public DefaultMenuItem(Object value) {
                }

                public DefaultMenuItem(Object value, String icon) {
                }

                public DefaultMenuItem(Object value, String icon, String url) {
                }
            }
            """;

    private static final String DEFAULT_SUB_MENU = """
            package org.primefaces.model.menu;

            import java.util.ArrayList;
            import java.util.List;

            public class DefaultSubMenu implements MenuElement {
                private final List<MenuElement> elements = new ArrayList<>();

                public DefaultSubMenu() {
                }

                public DefaultSubMenu(String label) {
                }

                public DefaultSubMenu(String label, String icon) {
                }

                public List<MenuElement> getElements() {
                    return elements;
                }

                public void addElement(MenuElement element) {
                    elements.add(element);
                }
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateProgrammaticMenuApi())
            .parser(JavaParser.fromJavaVersion().dependsOn(
                    MENU_ELEMENT,
                    MENU_MODEL,
                    BASE_MENU_MODEL,
                    DYNAMIC_MENU_MODEL,
                    DEFAULT_MENU_ITEM,
                    DEFAULT_SUB_MENU))
            .afterTypeValidationOptions(TypeValidation.builder()
                    .methodInvocations(false)
                    .identifiers(false)
                    .build());
    }

    @Test
    void migratesLegacyMenuItemConstructors() {
        rewriteRun(
            java(
                """
                import org.primefaces.model.menu.DefaultMenuItem;

                class Example {
                    Object one() {
                        return new DefaultMenuItem("Home");
                    }

                    Object two() {
                        return new DefaultMenuItem("Home", "fa fa-home");
                    }

                    Object three() {
                        return new DefaultMenuItem("Home", "fa fa-home", "/home.xhtml");
                    }
                }
                """,
                """
                import org.primefaces.model.menu.DefaultMenuItem;

                class Example {
                    Object one() {
                        return DefaultMenuItem.builder().value("Home").build();
                    }

                    Object two() {
                        return DefaultMenuItem.builder().value("Home").icon("fa fa-home").build();
                    }

                    Object three() {
                        return DefaultMenuItem.builder().value("Home").icon("fa fa-home").url("/home.xhtml").build();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesLegacySubMenuConstructors() {
        rewriteRun(
            java(
                """
                import org.primefaces.model.menu.DefaultSubMenu;

                class Example {
                    Object one() {
                        return new DefaultSubMenu("Administration");
                    }

                    Object two() {
                        return new DefaultSubMenu("Administration", "fa fa-cog");
                    }
                }
                """,
                """
                import org.primefaces.model.menu.DefaultSubMenu;

                class Example {
                    Object one() {
                        return DefaultSubMenu.builder().label("Administration").build();
                    }

                    Object two() {
                        return DefaultSubMenu.builder().label("Administration").icon("fa fa-cog").build();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesAddElementCalls() {
        rewriteRun(
            java(
                """
                import org.primefaces.model.menu.BaseMenuModel;
                import org.primefaces.model.menu.DefaultMenuItem;
                import org.primefaces.model.menu.DefaultSubMenu;
                import org.primefaces.model.menu.MenuModel;

                class Example {
                    void build(MenuModel model, BaseMenuModel base,
                               DefaultSubMenu submenu, DefaultMenuItem item) {
                        model.addElement(item);
                        base.addElement(submenu);
                        submenu.addElement(item);
                    }
                }
                """,
                """
                import org.primefaces.model.menu.BaseMenuModel;
                import org.primefaces.model.menu.DefaultMenuItem;
                import org.primefaces.model.menu.DefaultSubMenu;
                import org.primefaces.model.menu.MenuModel;

                class Example {
                    void build(MenuModel model, BaseMenuModel base,
                               DefaultSubMenu submenu, DefaultMenuItem item) {
                        model.getElements().add(item);
                        base.getElements().add(submenu);
                        submenu.getElements().add(item);
                    }
                }
                """
            )
        );
    }

    @Test
    void leavesNoArgConstructorsBecauseTheyRemainSupported() {
        rewriteRun(
            java(
                """
                import org.primefaces.model.menu.DefaultMenuItem;
                import org.primefaces.model.menu.DefaultSubMenu;

                class Example {
                    Object item() {
                        return new DefaultMenuItem();
                    }

                    Object submenu() {
                        return new DefaultSubMenu();
                    }
                }
                """
            )
        );
    }
}
