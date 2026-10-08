package com.jackson.migration.process;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies host-aware Maven Wrapper selection for Windows, Git Bash, and Linux/WSL execution.
 */
class ToolLocatorTest {
    private final String originalOsName = System.getProperty("os.name");

    @AfterEach
    void restoreOsName() {
        if (originalOsName == null) {
            System.clearProperty("os.name");
        } else {
            System.setProperty("os.name", originalOsName);
        }
    }

    @Test
    void linuxPrefersPosixWrapper(@TempDir Path project) throws Exception {
        System.setProperty("os.name", "Linux");
        Path windows = Files.createFile(project.resolve("mvnw.cmd"));
        Path unix = Files.createFile(project.resolve("mvnw"));

        ToolLocator locator = new ToolLocator();

        assertFalse(locator.isWindowsHost());
        assertEquals(unix, locator.projectMavenWrapper(project).orElseThrow());
        assertTrue(Files.exists(windows));
    }

    @Test
    void linuxIgnoresWindowsOnlyWrapper(@TempDir Path project) throws Exception {
        System.setProperty("os.name", "Linux");
        Files.createFile(project.resolve("mvnw.cmd"));

        ToolLocator locator = new ToolLocator();

        assertTrue(locator.projectMavenWrapper(project).isEmpty());
    }

    @Test
    void windowsPrefersCmdWrapper(@TempDir Path project) throws Exception {
        System.setProperty("os.name", "Windows 11");
        Path windows = Files.createFile(project.resolve("mvnw.cmd"));
        Files.createFile(project.resolve("mvnw"));

        ToolLocator locator = new ToolLocator();

        assertTrue(locator.isWindowsHost());
        assertEquals(windows, locator.projectMavenWrapper(project).orElseThrow());
    }
}
