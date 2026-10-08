package com.jackson.migration.rewrite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewriteMavenSettingsServiceTest {
    @TempDir Path tempDir;
    private final String originalUserHome = System.getProperty("user.home");

    @AfterEach
    void restoreUserHome() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void createsTemporarySettingsWithCodeGenomeRepositories() throws Exception {
        System.setProperty("user.home", tempDir.toString());
        RewriteMavenSettingsService service = new RewriteMavenSettingsService();
        Path generated = service.create("https://artifacts.codegenomeproject.org/maven", "developer@example.com", "secret-token");
        try {
            String xml = Files.readString(generated);
            assertTrue(xml.contains(RewriteMavenSettingsService.PROFILE_ID));
            assertTrue(xml.contains("https://artifacts.codegenomeproject.org/maven"));
            assertTrue(xml.contains("<repositories>"));
            assertTrue(xml.contains("<pluginRepositories>"));
            assertTrue(xml.contains("<activeProfile>" + RewriteMavenSettingsService.PROFILE_ID + "</activeProfile>"));
        } finally {
            Files.deleteIfExists(generated);
        }
    }

    @Test
    void preservesExistingUserSettings() throws Exception {
        Path m2 = Files.createDirectories(tempDir.resolve(".m2"));
        Files.writeString(m2.resolve("settings.xml"), """
                <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">
                  <mirrors><mirror><id>corp</id><url>https://repo.example.invalid</url><mirrorOf>internal</mirrorOf></mirror></mirrors>
                  <servers><server><id>codegenome</id><username>existing-user</username><password>existing-token</password></server></servers>
                </settings>
                """);
        System.setProperty("user.home", tempDir.toString());

        RewriteMavenSettingsService service = new RewriteMavenSettingsService();
        Path generated = service.create("https://artifacts.codegenomeproject.org/maven", null, null);
        try {
            String xml = Files.readString(generated);
            assertTrue(xml.contains("repo.example.invalid"));
            assertTrue(xml.contains("codegenome"));
        } finally {
            Files.deleteIfExists(generated);
        }
    }
    @Test
    void injectsEnvironmentStyleCredentialsIntoTemporarySettings() throws Exception {
        System.setProperty("user.home", tempDir.toString());
        RewriteMavenSettingsService service = new RewriteMavenSettingsService();
        Path generated = service.create("https://artifacts.codegenomeproject.org/maven",
                "developer@example.com", "token<&value");
        try {
            String xml = Files.readString(generated);
            assertTrue(xml.contains("<id>codegenome</id>"));
            assertTrue(xml.contains("developer@example.com"));
            assertTrue(xml.contains("token&lt;&amp;value"));
        } finally {
            Files.deleteIfExists(generated);
        }
    }

    @Test
    void failsFastWhenCredentialsAreUnavailable() {
        System.setProperty("user.home", tempDir.toString());
        RewriteMavenSettingsService service = new RewriteMavenSettingsService();
        Exception error = assertThrows(Exception.class, () ->
                service.create("https://artifacts.codegenomeproject.org/maven", null, null));
        assertTrue(error.getMessage().contains("CODE_GENOME_USERNAME"));
        assertTrue(error.getMessage().contains("CODE_GENOME_TOKEN"));
    }

}
