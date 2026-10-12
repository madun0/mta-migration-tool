package com.jackson.mta.foundation;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangePackage;

import java.time.Duration;
import java.util.List;

/**
 * Migrates Java EE package references from {@code javax.*} to their Jakarta EE
 * equivalents for the EAP 8 / Jakarta EE 10 target baseline.
 *
 * <p>This recipe intentionally uses an explicit allow-list rather than a broad
 * {@code javax -> jakarta} replacement. Several {@code javax.*} namespaces are
 * still Java SE APIs and must remain unchanged, including {@code javax.crypto},
 * {@code javax.management}, {@code javax.naming}, {@code javax.net},
 * {@code javax.sql}, {@code javax.swing}, {@code javax.transaction.xa},
 * {@code javax.annotation.processing}, and the Java SE XML APIs such as
 * {@code javax.xml.parsers} and {@code javax.xml.transform}.</p>
 *
 * <p>The removed JSF managed-bean namespace {@code javax.faces.bean} is also
 * deliberately excluded. It does not have a valid Jakarta Faces 4 package
 * equivalent and must be handled by the dedicated managed-bean-to-CDI recipe
 * before this recipe runs.</p>
 */
public class MigrateJavaxImports extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate Java EE javax packages to Jakarta EE";
    }

    @Override
    public String getDescription() {
        return "Migrates supported Java EE javax package references to their Jakarta EE equivalents while preserving Java SE javax namespaces and removed APIs.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // Jakarta Activation
                move("javax.activation", "jakarta.activation", true),

                // Jakarta Annotations. Do NOT recurse from javax.annotation because
                // javax.annotation.processing belongs to Java SE.
                move("javax.annotation", "jakarta.annotation", false),
                move("javax.annotation.security", "jakarta.annotation.security", true),
                move("javax.annotation.sql", "jakarta.annotation.sql", true),

                // Jakarta Batch
                move("javax.batch", "jakarta.batch", true),

                // CDI / dependency injection / interceptors / decorators.
                // Do not recurse from javax.enterprise because the old
                // javax.enterprise.deploy API is not a Jakarta EE 10 package move.
                move("javax.decorator", "jakarta.decorator", true),
                move("javax.enterprise.concurrent", "jakarta.enterprise.concurrent", true),
                move("javax.enterprise.context", "jakarta.enterprise.context", true),
                move("javax.enterprise.event", "jakarta.enterprise.event", true),
                move("javax.enterprise.inject", "jakarta.enterprise.inject", true),
                move("javax.enterprise.util", "jakarta.enterprise.util", true),
                move("javax.inject", "jakarta.inject", true),
                move("javax.interceptor", "jakarta.interceptor", true),

                // Jakarta Enterprise Beans
                move("javax.ejb", "jakarta.ejb", true),

                // Jakarta Expression Language
                move("javax.el", "jakarta.el", true),

                // Jakarta Faces 4. The removed javax.faces.bean namespace is
                // intentionally omitted and is migrated semantically to CDI by
                // MigrateFacesManagedBeans.
                move("javax.faces", "jakarta.faces", false),
                move("javax.faces.application", "jakarta.faces.application", true),
                move("javax.faces.component", "jakarta.faces.component", true),
                move("javax.faces.context", "jakarta.faces.context", true),
                move("javax.faces.convert", "jakarta.faces.convert", true),
                move("javax.faces.event", "jakarta.faces.event", true),
                move("javax.faces.flow", "jakarta.faces.flow", true),
                move("javax.faces.lifecycle", "jakarta.faces.lifecycle", true),
                move("javax.faces.model", "jakarta.faces.model", true),
                move("javax.faces.push", "jakarta.faces.push", true),
                move("javax.faces.render", "jakarta.faces.render", true),
                move("javax.faces.validator", "jakarta.faces.validator", true),
                move("javax.faces.view", "jakarta.faces.view", true),
                move("javax.faces.webapp", "jakarta.faces.webapp", true),

                // Messaging / JSON / Mail
                move("javax.jms", "jakarta.jms", true),
                move("javax.json", "jakarta.json", true),
                move("javax.mail", "jakarta.mail", true),

                // Persistence / Connector
                move("javax.persistence", "jakarta.persistence", true),
                move("javax.resource", "jakarta.resource", true),

                // Jakarta Security. Keep the rest of javax.security as Java SE.
                move("javax.security.auth.message", "jakarta.security.auth.message", true),
                move("javax.security.enterprise", "jakarta.security.enterprise", true),
                move("javax.security.jacc", "jakarta.security.jacc", true),

                // Servlet
                move("javax.servlet", "jakarta.servlet", true),

                // Jakarta Transactions. Do NOT recurse because
                // javax.transaction.xa remains a Java SE namespace.
                move("javax.transaction", "jakarta.transaction", false),

                // Bean Validation
                move("javax.validation", "jakarta.validation", true),

                // WebSocket and REST
                move("javax.websocket", "jakarta.websocket", true),
                move("javax.ws.rs", "jakarta.ws.rs", true),

                // XML technologies that moved to Jakarta. Do not recurse from
                // javax.xml because parser/transform/stream/etc. remain Java SE.
                move("javax.jws", "jakarta.jws", true),
                move("javax.xml.bind", "jakarta.xml.bind", true),
                move("javax.xml.soap", "jakarta.xml.soap", true),
                move("javax.xml.ws", "jakarta.xml.ws", true)
        );
    }

    /**
     * Creates one package-relocation recipe.
     *
     * @param oldPackage legacy Java EE package
     * @param newPackage Jakarta EE package
     * @param recursive whether child packages should also be migrated
     * @return configured package migration recipe
     */
    private static Recipe move(String oldPackage, String newPackage, boolean recursive) {
        return new ChangePackage(oldPackage, newPackage, recursive);
    }
}
