package com.jackson.mta.foundation;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.xml.Assertions.xml;

/**
 * Behavioral tests for the declarative foundation recipes loaded from
 * META-INF/rewrite/foundation.yml.
 */
class FoundationRecipeTransformationTest implements RewriteTest {

    @Test
    void migratesJavaxFacesDependencyToJakartaFaces() {
        rewriteRun(
            spec -> spec.recipeFromResources("com.jackson.mta.foundation.MigrateJakartaDependencies"),
            pomXml(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.faces</groupId>
                            <artifactId>javax.faces-api</artifactId>
                            <version>2.3</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.faces</groupId>
                            <artifactId>jakarta.faces-api</artifactId>
                            <version>4.0.1</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>jakarta.platform</groupId>
                            <artifactId>jakarta.jakartaee-api</artifactId>
                            <version>10.0.0</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
                spec -> spec.afterRecipe(pom -> {
                    assertJakartaFacesDependency(pom);
                    assertJakartaEePlatformDependency(pom);
                })
            )
        );
    }

    @Test
    void addsJakartaEe10CompileBaselineWhenLegacyPomReliedOnEapApis() {
        rewriteRun(
            spec -> spec.recipeFromResources("com.jackson.mta.foundation.MigrateJakartaDependencies"),
            pomXml(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                </project>
                """,
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.platform</groupId>
                            <artifactId>jakarta.jakartaee-api</artifactId>
                            <version>10.0.0</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
                spec -> spec.afterRecipe(FoundationRecipeTransformationTest::assertJakartaEePlatformDependency)
            )
        );
    }

    @Test
    void addsJakartaEe10BaselineWhenPrimeFacesAppReliedOnContainerProvidedApis() {
        rewriteRun(
            spec -> spec.recipeFromResources("com.jackson.mta.foundation.MigrateJakartaDependencies"),
            pomXml(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>legacy-primefaces-app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.primefaces</groupId>
                            <artifactId>primefaces</artifactId>
                            <version>6.2</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>legacy-primefaces-app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.primefaces</groupId>
                            <artifactId>primefaces</artifactId>
                            <version>6.2</version>
                        </dependency>
                        <dependency>
                            <groupId>jakarta.platform</groupId>
                            <artifactId>jakarta.jakartaee-api</artifactId>
                            <version>10.0.0</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
                spec -> spec.afterRecipe(FoundationRecipeTransformationTest::assertJakartaEePlatformDependency)
            )
        );
    }

    @Test
    void ensuresJakartaEe10CompileApiWithProvidedScope() {
        rewriteRun(
            spec -> spec.recipeFromResources("com.jackson.mta.foundation.EnsureJakartaEe10CompileApi"),
            pomXml(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>container-provided-ee-app</artifactId>
                    <version>1.0</version>
                </project>
                """,
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>container-provided-ee-app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.platform</groupId>
                            <artifactId>jakarta.jakartaee-api</artifactId>
                            <version>10.0.0</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
                spec -> spec.afterRecipe(FoundationRecipeTransformationTest::assertJakartaEePlatformDependency)
            )
        );
    }

    @Test
    void normalizesExistingJakartaEe10CompileApiToProvidedWithoutDuplicate() {
        rewriteRun(
            spec -> spec.recipeFromResources("com.jackson.mta.foundation.EnsureJakartaEe10CompileApi"),
            pomXml(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>existing-ee-app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.platform</groupId>
                            <artifactId>jakarta.jakartaee-api</artifactId>
                            <version>10.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>existing-ee-app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.platform</groupId>
                            <artifactId>jakarta.jakartaee-api</artifactId>
                            <version>10.0.0</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
                spec -> spec.afterRecipe(FoundationRecipeTransformationTest::assertJakartaEePlatformDependency)
            )
        );
    }

    @Test
    void migratesExplicitJavaxServletApiAndAddsJakartaEeCompileBaseline() {
        rewriteRun(
            spec -> spec.recipeFromResources("com.jackson.mta.foundation.MigrateJakartaDependencies"),
            pomXml(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>servlet-app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>servlet-app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.platform</groupId>
                            <artifactId>jakarta.jakartaee-api</artifactId>
                            <version>10.0.0</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>jakarta.servlet</groupId>
                            <artifactId>jakarta.servlet-api</artifactId>
                            <version>6.0.0</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
                spec -> spec.afterRecipe(pom -> {
                    assertJakartaEePlatformDependency(pom);
                    assertJakartaServletDependency(pom);
                })
            )
        );
    }

    @Test
    void ensuresJakartaFacesCompileApiWithProvidedScope() {
        rewriteRun(
            spec -> spec.recipeFromResources("com.jackson.mta.foundation.EnsureJakartaFacesCompileApi"),
            pomXml(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>container-provided-jsf-app</artifactId>
                    <version>1.0</version>
                </project>
                """,
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>container-provided-jsf-app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.faces</groupId>
                            <artifactId>jakarta.faces-api</artifactId>
                            <version>4.0.1</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
                spec -> spec.afterRecipe(FoundationRecipeTransformationTest::assertJakartaFacesDependency)
            )
        );
    }


    @Test
    void normalizesExistingJakartaFacesDependencyToProvidedWithoutDuplicate() {
        rewriteRun(
            spec -> spec.recipeFromResources("com.jackson.mta.foundation.EnsureJakartaFacesCompileApi"),
            pomXml(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>existing-faces-app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.faces</groupId>
                            <artifactId>jakarta.faces-api</artifactId>
                            <version>4.0.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>existing-faces-app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.faces</groupId>
                            <artifactId>jakarta.faces-api</artifactId>
                            <version>4.0.1</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
                spec -> spec.afterRecipe(FoundationRecipeTransformationTest::assertJakartaFacesDependency)
            )
        );
    }


    @Test
    void migratesSupportedJavaxImportButLeavesJavaSeNamespaceAlone() {
        String oldFaces = """
                package javax.faces.context;
                public class FacesContext {}
                """;
        String newFaces = """
                package jakarta.faces.context;
                public class FacesContext {}
                """;

        rewriteRun(
            spec -> spec
                .recipeFromResources("com.jackson.mta.foundation.MigrateJavaxImports")
                .parser(JavaParser.fromJavaVersion().dependsOn(oldFaces, newFaces)),
            java(
                """
                import javax.faces.context.FacesContext;
                import javax.crypto.Cipher;

                class Example {
                    FacesContext context;
                    Cipher cipher;
                }
                """,
                """
                import jakarta.faces.context.FacesContext;
                import javax.crypto.Cipher;

                class Example {
                    FacesContext context;
                    Cipher cipher;
                }
                """
            )
        );
    }


    @Test
    void migratesPersistenceDescriptorToJakartaPersistenceThree() {
        rewriteRun(
            spec -> spec.recipeFromResources("com.jackson.mta.foundation.MigrateJakartaDescriptors"),
            xml(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd"
                             version="2.1">
                    <persistence-unit name="migrationShowcasePU" transaction-type="JTA"/>
                </persistence>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <persistence xmlns="https://jakarta.ee/xml/ns/persistence"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence https://jakarta.ee/xml/ns/persistence/persistence_3_0.xsd"
                             version="3.0">
                    <persistence-unit name="migrationShowcasePU" transaction-type="JTA"/>
                </persistence>
                """
            )
        );
    }

    @Test
    void upgradesPrimeFacesDependencyToSixteen() {
        rewriteRun(
            spec -> spec.recipeFromResources("com.jackson.mta.foundation.UpgradePrimeFacesDependency"),
            pomXml(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.primefaces</groupId>
                            <artifactId>primefaces</artifactId>
                            <version>6.2</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.primefaces</groupId>
                            <artifactId>primefaces</artifactId>
                            <version>16.0.0</version>
                        </dependency>
                        <dependency>
                            <groupId>jakarta.platform</groupId>
                            <artifactId>jakarta.jakartaee-api</artifactId>
                            <version>10.0.0</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
                spec -> spec.afterRecipe(FoundationRecipeTransformationTest::assertJakartaEePlatformDependency)
            )
        );
    }

    @Test
    void migratesJavaxEjbDependencyToJakartaEjb() {
        rewriteRun(
            spec -> spec.recipeFromResources("com.jackson.mta.foundation.MigrateJakartaDependencies"),
            pomXml(
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.ejb</groupId>
                            <artifactId>javax.ejb-api</artifactId>
                            <version>3.2.2</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
                """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.ejb</groupId>
                            <artifactId>jakarta.ejb-api</artifactId>
                            <version>4.0.1</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>jakarta.platform</groupId>
                            <artifactId>jakarta.jakartaee-api</artifactId>
                            <version>10.0.0</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
                spec -> spec.afterRecipe(FoundationRecipeTransformationTest::assertJakartaEePlatformDependency)
            )
        );
    }

    /**
     * Verifies the EAP migration compile baseline without depending on dependency ordering.
     *
     * @param pom rewritten Maven POM
     */
    private static void assertJakartaEePlatformDependency(Xml.Document pom) {
        String xml = pom.printAll();
        Pattern platformDependency = Pattern.compile(
                "<dependency>\\s*" +
                "<groupId>jakarta\\.platform</groupId>\\s*" +
                "<artifactId>jakarta\\.jakartaee-api</artifactId>" +
                ".*?</dependency>",
                Pattern.DOTALL);

        Matcher matcher = platformDependency.matcher(xml);
        int matches = 0;
        String dependency = null;
        while (matcher.find()) {
            matches++;
            dependency = matcher.group();
        }

        assertEquals(1, matches, "Expected exactly one jakarta.platform:jakarta.jakartaee-api dependency");
        assertTrue(dependency != null && dependency.contains("<version>10.0.0</version>"),
                "Jakarta EE API must be version 10.0.0");
        assertTrue(dependency != null && dependency.contains("<scope>provided</scope>"),
                "Jakarta EE API must use provided scope for the EAP runtime");
    }

    /**
     * Verifies an explicitly migrated Servlet API dependency without depending on
     * its position relative to the Jakarta EE platform compile baseline.
     *
     * @param pom rewritten Maven POM
     */
    private static void assertJakartaServletDependency(Xml.Document pom) {
        String xml = pom.printAll();
        Pattern servletDependency = Pattern.compile(
                "<dependency>\\s*" +
                "<groupId>jakarta\\.servlet</groupId>\\s*" +
                "<artifactId>jakarta\\.servlet-api</artifactId>" +
                ".*?</dependency>",
                Pattern.DOTALL);

        Matcher matcher = servletDependency.matcher(xml);
        int matches = 0;
        String dependency = null;
        while (matcher.find()) {
            matches++;
            dependency = matcher.group();
        }

        assertEquals(1, matches, "Expected exactly one jakarta.servlet:jakarta.servlet-api dependency");
        assertTrue(dependency != null && dependency.contains("<version>6.0.0</version>"),
                "Jakarta Servlet API must be version 6.0.0");
        assertTrue(dependency != null && dependency.contains("<scope>provided</scope>"),
                "Jakarta Servlet API must use provided scope for the EAP runtime");
    }

    /**
     * Verifies the semantic Maven contract required by PrimeFaces 16 without
     * depending on where OpenRewrite chooses to insert the dependency.
     *
     * @param pom rewritten Maven POM
     */
    private static void assertJakartaFacesDependency(Xml.Document pom) {
        String xml = pom.printAll();
        Pattern facesDependency = Pattern.compile(
                "<dependency>\\s*" +
                "<groupId>jakarta\\.faces</groupId>\\s*" +
                "<artifactId>jakarta\\.faces-api</artifactId>" +
                ".*?</dependency>",
                Pattern.DOTALL);

        Matcher matcher = facesDependency.matcher(xml);
        int matches = 0;
        String dependency = null;
        while (matcher.find()) {
            matches++;
            dependency = matcher.group();
        }

        assertEquals(1, matches, "Expected exactly one jakarta.faces:jakarta.faces-api dependency");
        assertTrue(dependency != null && dependency.contains("<version>4.0.1</version>"),
                "Jakarta Faces API must be version 4.0.1");
        assertTrue(dependency != null && dependency.contains("<scope>provided</scope>"),
                "Jakarta Faces API must use provided scope for the EAP runtime");
    }

}
