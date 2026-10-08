package com.jackson.migration.process;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

/**
 * Locates MTA, Git, Maven, and Maven Wrapper executables from configuration and PATH.
 *
 * <p>Launcher preference follows the host operating system rather than the user's shell.
 * Windows prefers {@code .cmd}/{@code .exe} launchers, while Linux/WSL/macOS prefers
 * POSIX launchers. This matters when the Workbench is started from Git Bash or WSL, where
 * shell appearance alone does not determine whether {@code cmd.exe} is available.</p>
 */
@ApplicationScoped
public class ToolLocator {
    /**
     * Locates {@code mta-cli.exe}, honoring the {@code MTA_CLI} environment override first.
     *
     * @return MTA executable path when available
     */
    public Optional<Path> mta() {
        String explicit = System.getenv("MTA_CLI");
        if (explicit != null && !explicit.isBlank() && Files.isRegularFile(Path.of(explicit))) {
            return Optional.of(Path.of(explicit).toAbsolutePath().normalize());
        }
        return find("mta-cli.exe", "mta-cli");
    }

    /** @return Git executable path when available on {@code PATH}. */
    public Optional<Path> git() { return find("git.exe", "git"); }

    /**
     * Locates system Maven on {@code PATH} using host-appropriate launcher preference.
     *
     * @return system Maven executable path when available
     */
    public Optional<Path> systemMaven() {
        return isWindowsHost()
                ? find("mvn.cmd", "mvn.exe", "mvn")
                : find("mvn");
    }

    /**
     * Locates a Maven Wrapper in the target project using host-appropriate launcher preference.
     *
     * <p>On Windows, {@code mvnw.cmd} is preferred. On Linux/WSL/macOS, only the POSIX
     * {@code mvnw} launcher is selected; a lone {@code mvnw.cmd} is intentionally ignored so
     * the caller can fall back to a usable system {@code mvn} executable.</p>
     *
     * @param project application project root
     * @return project Maven Wrapper path when present and executable on the current host
     */
    public Optional<Path> projectMavenWrapper(Path project) {
        if (isWindowsHost()) {
            Path windows = project.resolve("mvnw.cmd");
            if (Files.isRegularFile(windows)) return Optional.of(windows);

            Path unix = project.resolve("mvnw");
            if (Files.isRegularFile(unix)) return Optional.of(unix);
            return Optional.empty();
        }

        Path unix = project.resolve("mvnw");
        return Files.isRegularFile(unix) ? Optional.of(unix) : Optional.empty();
    }

    /**
     * Returns whether the JVM is running on a Windows host.
     *
     * <p>This deliberately checks {@code os.name} rather than environment variables such as
     * {@code MSYSTEM}; Git Bash can be used on Windows, while WSL presents a Linux JVM even
     * when invoked from a Windows workstation.</p>
     *
     * @return {@code true} when the JVM host operating system is Windows
     */
    boolean isWindowsHost() {
        return System.getProperty("os.name", "")
                .toLowerCase()
                .contains("win");
    }

    /**
     * Searches the current {@code PATH} for the first matching executable name.
     *
     * @param names executable names in preference order
     * @return resolved executable path when a match is found
     */
    public Optional<Path> find(String... names) {
        String path = System.getenv("PATH");
        if (path == null) return Optional.empty();
        return Arrays.stream(path.split(File.pathSeparator))
                .map(Path::of)
                .flatMap(dir -> Arrays.stream(names).map(dir::resolve))
                .filter(Files::isRegularFile)
                .findFirst()
                .map(p -> p.toAbsolutePath().normalize());
    }
}
