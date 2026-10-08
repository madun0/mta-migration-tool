=======
# Jackson Migration Workbench

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
- Maven 3.9.x or a target-project Maven Wrapper
- MTA CLI 8.2 on PATH as `mta-cli.exe` (or set `MTA_CLI`)

No MTA Hub, WSL, Docker/Podman, database, REST API, or web UI is required.

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

The standard profile now automates the deterministic portion of the Java 21 / Jakarta EE 10 foundation before PrimeFaces-specific modernization. The recipe artifact includes Java 21 compiler normalization, curated Java EE `javax.*` package migration, common Jakarta Maven dependency changes, Servlet/Faces descriptor normalization, safe JSF scope migration, and the PrimeFaces 16 dependency upgrade. Ambiguous JSF managed-bean/injection semantics remain assisted work with generated verification checklists.

Generated migration plans use project-relative affected-file paths, show the active OpenRewrite recipes, and distinguish MTA-backed migration tasks from post-review cleanup tasks.

### OpenRewrite repository behavior

The default catalog pins `rewrite-maven-plugin` 6.46.1, which is resolvable from Maven Central and is aligned with the Rewrite 8.90.2 core used by the Workbench recipe module. This keeps the default migration workflow credential-free.

`rewriteRepositoryUrl` is optional. Leave it blank for the default Maven Central flow. If a future catalog intentionally points it at an authenticated repository such as Code Genome, configure a Maven `<server>` with id `codegenome` or set `CODE_GENOME_USERNAME` and `CODE_GENOME_TOKEN`; the Workbench will use those credentials only in its temporary Maven settings file.
