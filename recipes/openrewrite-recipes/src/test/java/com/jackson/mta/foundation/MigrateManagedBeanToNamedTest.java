package com.jackson.mta.foundation;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/** Behavioral tests for JSF ManagedBean to CDI Named migration. */
class MigrateManagedBeanToNamedTest implements RewriteTest {

    private static final String LEGACY = """
            package javax.faces.bean;
            public @interface ManagedBean {
                String name() default "";
                boolean eager() default false;
            }
            """;

    private static final String TARGET = """
            package jakarta.inject;
            public @interface Named {
                String value() default "";
            }
            """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateManagedBeanToNamed())
                .parser(JavaParser.fromJavaVersion().dependsOn(LEGACY, TARGET));
    }

    @Test
    void migratesManagedBean() {
        rewriteRun(
                java(
                        """
                        import javax.faces.bean.ManagedBean;

                        @ManagedBean
                        class Example {
                        }
                        """,
                        """
                        import jakarta.inject.Named;

                        @Named
                        class Example {
                        }
                        """));
    }

    @Test
    void preservesExplicitBeanName() {
        rewriteRun(
                java(
                        """
                        import javax.faces.bean.ManagedBean;

                        @ManagedBean(name = "showcase")
                        class Example {
                        }
                        """,
                        """
                        import jakarta.inject.Named;

                        @Named("showcase")
                        class Example {
                        }
                        """));
    }

    @Test
    void dropsUnsupportedEagerAttributeButStillMigrates() {
        rewriteRun(
                java(
                        """
                        import javax.faces.bean.ManagedBean;

                        @ManagedBean(name = "startup", eager = true)
                        class Example {
                        }
                        """,
                        """
                        import jakarta.inject.Named;

                        @Named("startup")
                        class Example {
                        }
                        """));
    }
}
