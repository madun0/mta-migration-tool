package com.jackson.migration.validation;

import com.jackson.migration.process.ToolLocator;
import com.jackson.migration.util.WorkbenchHome;
import com.jackson.migration.workflow.GitService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates that a Windows 11/Git Bash workstation has the required Java, Git, Maven, MTA, project, and recipe prerequisites.
 */
@ApplicationScoped
public class DoctorService {
    @Inject ToolLocator tools;
    @Inject GitService git;
    @Inject WorkbenchHome home;

    /**
     * Runs environment readiness checks and prints a concise result for each prerequisite.
     *
     * @param project application project root
     * @return {@code true} when all required checks pass
     */
    public boolean run(Path project) {
        List<String> failures = new ArrayList<>();
        System.out.println("Migration Workbench Environment");
        System.out.println();
        check("Windows/OS", System.getProperty("os.name") + " " + System.getProperty("os.arch"), true, failures);
        check("Java", Runtime.version().toString(), Runtime.version().feature() >= 21, failures);
        check("Workbench home", home.path().toString(), Files.exists(home.path().resolve("config/migration-catalog.yaml")), failures);
        check("Project", project.toString(), Files.isDirectory(project), failures);
        check("pom.xml", project.resolve("pom.xml").toString(), Files.exists(project.resolve("pom.xml")), failures);
        check("Git", tools.git().map(Path::toString).orElse("not found"), tools.git().isPresent(), failures);
        check("MTA CLI", tools.mta().map(Path::toString).orElse("not found"), tools.mta().isPresent(), failures);
        boolean maven = tools.projectMavenWrapper(project).isPresent() || tools.systemMaven().isPresent();
        check("Maven", tools.projectMavenWrapper(project).or(tools::systemMaven).map(Path::toString).orElse("not found"), maven, failures);
        try {
            check("Clean Git tree", "required before first migrate", git.isClean(project), failures);
        } catch (Exception e) {
            failures.add("Could not inspect Git tree: " + e.getMessage());
        }
        Path recipe = Path.of(System.getProperty("user.home"), ".m2", "repository", "com", "jackson", "mta",
                "primefaces-openrewrite-recipes", "1.0.0-SNAPSHOT");
        check("Recipe artifact", recipe.toString(), Files.exists(recipe), failures);

        System.out.println();
        if (failures.isEmpty()) {
            System.out.println("READY");
            return true;
        }
        System.out.println("NOT READY");
        failures.forEach(f -> System.out.println("  - " + f));
        return false;
    }

    private void check(String name, String detail, boolean ok, List<String> failures) {
        System.out.printf("%-24s - %s%n", ok ? "[OK] " + name : "[FAIL] " + name, detail);
        if (!ok) failures.add(name + ": " + detail);
    }
}
