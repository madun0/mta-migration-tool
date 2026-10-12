# Jackson Migration Workbench


> **Descriptor migration note:** the bundled OpenRewrite recipes currently use the development coordinate `com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT`. Run `mvn clean install` from this Workbench root after updating the Workbench so the current snapshot recipe JAR is installed before invoking `bin/migrate`. Descriptor migration handles multiline schema locations and `javax.faces.*` context-param names.

A Windows 11 / Git Bash-first CLI that orchestrates **Red Hat Migration Toolkit for Applications (MTA)**
and **OpenRewrite** to guide junior developers through a JSF 2.2 / PrimeFaces 6.2 modernization toward
JBoss EAP 8, Jakarta Faces 4, PrimeFaces 16 and Java 21.

The Workbench is intentionally thin:

> **MTA finds. The catalog decides. OpenRewrite changes. Maven validates. Git checkpoints.**

## What is included

- Java 21 + Quarkus + Picocli CLI (`migrate`)
- `doctor`, `init`, `assess`, `plan`, `migrate`, `status`, `resume`, `rollback`, `verify`, `report`
- standard / primefaces-only / assessment-only profiles
- ordered migration phases
- Git-backed checkpoints without automatic source commits
- custom MTA rules for Faces 4 and PrimeFaces 6.2 -> 16
- the complete PrimeFaces OpenRewrite recipe project
- machine-readable `migration-catalog.yaml`
- migration guides and an end-to-end example
- source-level Javadocs for all Java types plus documented Workbench public APIs

## Supported developer environment

- Windows 11
- Git for Windows / Git Bash (default terminal)
- JDK 21
- Maven 3.9.x on PATH (a target-project Maven Wrapper is used only as a fallback)
- MTA CLI 8.2 on PATH as `mta-cli.exe` (or set `MTA_CLI`)

No MTA Hub, WSL, Docker/Podman, database, REST API, or web UI is required.

All Workbench commands operate on the current working directory. `cd` to the target application root before running `migrate`; there is no project-path CLI option.

## Build the Workbench

From Git Bash in the Workbench root:

```bash
mvn clean install
```

This builds the Quarkus CLI and installs the custom OpenRewrite recipe artifact in the local Maven repository.

Then add the Workbench `bin` directory to your Git Bash PATH, or run it directly:

```bash
./bin/migrate --help
```

## Five-step migration flow

```bash
cd /c/src/customer-portal

/c/tools/mta-migration-tool/bin/migrate init
# Review migration.yaml and commit it before migration.

git add migration.yaml
git commit -m "chore: add migration workbench configuration"

migrate doctor
migrate assess
migrate plan
migrate migrate
migrate verify
```

When assisted/manual work is detected, the first migration pass stops before cleanup with `REVIEW_REQUIRED`. Complete `.migration/TODO.md`, then run `migrate resume` to execute post-review cleanup.

If a family fails:

```bash
migrate status
migrate rollback
# fix/configure as needed
migrate resume
```

See `docs/USER-GUIDE.md`, `docs/EXAMPLE-MIGRATION.md`, and `docs/CODE-DOCUMENTATION.md`.

### Foundation automation

The standard profile now automates the deterministic portion of the Java 21 / Jakarta EE 10 foundation before PrimeFaces-specific modernization. The recipe artifact includes Java 21 compiler normalization, curated Java EE `javax.*` package migration, common Jakarta Maven dependency changes, a `jakarta.platform:jakarta.jakartaee-api:10.0.0` provided compile baseline for EAP-hosted APIs, Servlet/Faces descriptor normalization, safe JSF scope migration, and the PrimeFaces 16 dependency upgrade. Ambiguous JSF managed-bean/injection semantics remain assisted work with generated verification checklists.

Generated migration plans use project-relative affected-file paths, show the active OpenRewrite recipes, and distinguish MTA-backed migration tasks from post-review cleanup tasks.

### Maven and OpenRewrite behavior

The default catalog pins `rewrite-maven-plugin` 6.46.1, aligned with the Rewrite 8.90.2 core used by the Workbench recipe module. `migrate migrate` invokes Maven from the target application's root directory and does not generate or inject a separate Maven settings file.

The Workbench prefers the system `mvn` command on `PATH`, matching a developer's direct Maven invocation; a project Maven Wrapper is only a fallback when system Maven is unavailable. Maven therefore owns normal project configuration discovery, including `.mvn/maven.config`, `.mvn/jvm.config`, `.mvn/extensions.xml`, user/global `settings.xml`, mirrors, repositories, resolver properties, and corporate Artifactory configuration.

### MTA analysis mode and project scope

MTA analysis mode is configured only in the application's `migration.yaml`. The default is `source-only`,
which asks MTA to analyze application source only. Set `mode: full` when dependency-aware
analysis is required; full mode intentionally preserves dependency findings from locations such as the local
Maven repository and parent/reactor modules. There is no command-line mode override.

```yaml
mta:
  mode: full
  analyzeKnownLibraries: false
  # Optional when MTA must use a non-default corporate Maven settings file.
  mavenSettings: "C:/Users/me/.m2/settings.xml"
  # Optional fallback when the remote Maven search index is blocked/unavailable.
  disableMavenSearch: false
```

`mode: full` enables dependency-aware provider analysis, but MTA treats analysis of known open-source libraries as
a separate option. The Workbench defaults `analyzeKnownLibraries` to `false`, matching the MTA CLI default. Enable it only when dependency findings from known open-source libraries are intentionally required.
`mavenSettings` is passed directly to MTA as `--maven-settings`. Use `disableMavenSearch: true` only when Maven
search is unavailable; dependency classification is then less reliable and may produce more incidents.

`mta.sources`, `mta.targets`, and `mta.mode` are the only supported MTA scope controls. Legacy scalar `mta.target`, `strictProjectScope`, and Workbench pseudo-selectors are not supported.

Workbench-generated state is not part of the application analysis surface. Before invoking `mta-cli analyze`, the Workbench temporarily moves `.migration` outside the application root and directs MTA output to an external staging directory. After MTA completes, `.migration` is restored and the fresh report is copied into `.migration/assessment` or `.migration/verification`. This prevents a verification run from treating historical `output.yaml`, `findings.json`, checkpoints, or reports as application source.

### Findings count

Assessment output intentionally exposes one primary count:

```text
MTA findings: <n>
```

This count represents the raw number of incidents in MTA `output.yaml` under `violations.<rule>.incidents[]`. `.migration/assessment/findings.json` mirrors those incidents one-for-one. Workbench-only deduplication and generated-file cleanup are stored separately in `.migration/assessment/planning-findings.json` for planning and residual classification. Insights and dependency inventory remain diagnostic artifacts and are not presented as additional findings totals.


### MTA source-only verification boundary

During `assess` and `verify`, the Workbench temporarily moves `.migration` and every Maven `target/` directory outside the application tree before invoking MTA. This prevents historical reports and generated descriptor copies from being evaluated as migration source. The directories are restored immediately after MTA exits.

