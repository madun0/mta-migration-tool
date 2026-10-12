package com.jackson.migration.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies descriptor-scoped javax-to-jakarta residual detection.
 */
class ValidationServiceDescriptorTest {

    @TempDir
    Path tempDir;

    @Test
    void catchesJakartaEeJavaxReferencesInWebXmlAndIgnoresJavaSeNamespaces() throws Exception {
        Path webXml = tempDir.resolve("module/src/main/webapp/WEB-INF/web.xml");
        Files.createDirectories(webXml.getParent());
        Files.writeString(webXml, """
                <web-app>
                    <error-page><exception-type>javax.servlet.ServletException</exception-type></error-page>
                    <resource-env-ref><resource-env-ref-type>javax.jms.Queue</resource-env-ref-type></resource-env-ref>
                    <resource-ref><res-type>javax.sql.DataSource</res-type></resource-ref>
                    <env-entry><env-entry-type>javax.transaction.xa.XAResource</env-entry-type></env-entry>
                    <context-param><param-value>javax.annotation.processing.Processor</param-value></context-param>
                    <context-param><param-value>javax.crypto.Cipher</param-value></context-param>
                </web-app>
                """);

        ValidationResult result = new ValidationService().jakartaDescriptorReferences(tempDir);

        assertFalse(result.success);
        assertTrue(result.errors.stream().anyMatch(e -> e.contains("javax.servlet")));
        assertTrue(result.errors.stream().anyMatch(e -> e.contains("javax.jms")));
        assertTrue(result.errors.stream().noneMatch(e -> e.contains("javax.sql")));
        assertTrue(result.errors.stream().noneMatch(e -> e.contains("javax.transaction.xa")));
        assertTrue(result.errors.stream().noneMatch(e -> e.contains("javax.annotation.processing")));
        assertTrue(result.errors.stream().noneMatch(e -> e.contains("javax.crypto")));
    }

    @Test
    void ignoresGeneratedDescriptorCopies() throws Exception {
        Path generatedWebXml = tempDir.resolve("module/target/classes/WEB-INF/web.xml");
        Files.createDirectories(generatedWebXml.getParent());
        Files.writeString(generatedWebXml,
                "<web-app><error-page><exception-type>javax.servlet.ServletException</exception-type></error-page></web-app>");

        ValidationResult result = new ValidationService().jakartaDescriptorReferences(tempDir);

        assertTrue(result.success);
        assertTrue(result.errors.isEmpty());
    }
}
