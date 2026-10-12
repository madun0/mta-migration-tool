package com.jackson.mta.foundation;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

/**
 * Verifies the Jakarta EE namespace safety boundary implemented by
 * {@link MigrateJavaxImports}.
 */
class MigrateJavaxImportsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateJavaxImports())
                .typeValidationOptions(TypeValidation.none());
    }

    /** Verifies the namespaces reported missing in the migration workbench. */
    @Test
    void migratesInjectServletAndFacesPackages() {
        rewriteRun(
                java(
                        """
                        import javax.inject.Inject;
                        import javax.faces.application.FacesMessage;
                        import javax.faces.context.FacesContext;
                        import javax.servlet.Filter;
                        import javax.servlet.http.HttpServletRequest;

                        class Example {
                            @Inject Object service;
                            FacesMessage message;
                            FacesContext context;
                            Filter filter;
                            HttpServletRequest request;
                        }
                        """,
                        """
                        import jakarta.inject.Inject;
                        import jakarta.faces.application.FacesMessage;
                        import jakarta.faces.context.FacesContext;
                        import jakarta.servlet.Filter;
                        import jakarta.servlet.http.HttpServletRequest;

                        class Example {
                            @Inject Object service;
                            FacesMessage message;
                            FacesContext context;
                            Filter filter;
                            HttpServletRequest request;
                        }
                        """
                )
        );
    }

    /** Verifies additional Jakarta EE package families are covered. */
    @Test
    void migratesRepresentativeJakartaEePackages() {
        rewriteRun(
                java(
                        """
                        import javax.ejb.EJB;
                        import javax.enterprise.context.RequestScoped;
                        import javax.persistence.Entity;
                        import javax.validation.Valid;
                        import javax.ws.rs.GET;
                        import javax.xml.bind.JAXBContext;

                        class Example {
                            @EJB Object bean;
                            @Valid Object value;
                            @GET void get() {}
                            RequestScoped scope;
                            Entity entity;
                            JAXBContext context;
                        }
                        """,
                        """
                        import jakarta.ejb.EJB;
                        import jakarta.enterprise.context.RequestScoped;
                        import jakarta.persistence.Entity;
                        import jakarta.validation.Valid;
                        import jakarta.ws.rs.GET;
                        import jakarta.xml.bind.JAXBContext;

                        class Example {
                            @EJB Object bean;
                            @Valid Object value;
                            @GET void get() {}
                            RequestScoped scope;
                            Entity entity;
                            JAXBContext context;
                        }
                        """
                )
        );
    }

    /**
     * Ensures Java SE namespaces and the removed JSF managed-bean API are not
     * blindly rewritten to invalid Jakarta packages.
     */
    @Test
    void preservesJavaSeJavaxAndRemovedFacesBeanNamespace() {
        rewriteRun(
                java(
                        """
                        import javax.annotation.processing.Processor;
                        import javax.crypto.Cipher;
                        import javax.faces.bean.ManagedBean;
                        import javax.management.MBeanServer;
                        import javax.naming.InitialContext;
                        import javax.net.ssl.SSLContext;
                        import javax.sql.DataSource;
                        import javax.transaction.xa.XAResource;
                        import javax.xml.parsers.DocumentBuilder;
                        import javax.xml.transform.Transformer;

                        @ManagedBean
                        class Example {
                            Processor processor;
                            Cipher cipher;
                            MBeanServer mbeanServer;
                            InitialContext naming;
                            SSLContext ssl;
                            DataSource dataSource;
                            XAResource xaResource;
                            DocumentBuilder parser;
                            Transformer transformer;
                        }
                        """
                )
        );
    }
}
