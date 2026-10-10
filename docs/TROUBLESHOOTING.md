# Troubleshooting

## `jdtls requires at least Java 17` or Java 8 is active

The Workbench standardizes on Java 21. Verify:

```bash
java -version
javac -version
```

Set `JAVA_HOME` to JDK 21 and ensure its `bin` directory appears before older Java installations on `PATH`.

## `mta-cli.exe not found`

Add the MTA CLI directory to the Windows PATH visible to Git Bash, or set:

```bash
export MTA_CLI='C:\tools\mta\mta-cli.exe'
```

Then run `migrate doctor` again.

## Recipe artifact is missing

From the Workbench root run:

```bash
mvn clean install
```

This installs `com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT` into the local Maven repository.

## Worktree is not clean

The first automatic migration requires a clean Git worktree. Commit or stash application changes before running `migrate migrate`.

`.migration/` is added to `.git/info/exclude` by `migrate init` so generated reports do not make the repository dirty.

## Rollback is refused

Rollback intentionally stops if:

- Git HEAD changed after migration started, or
- the current source diff differs from the diff recorded after the checkpoint.

This protects manual developer changes. Commit/stash the manual changes and decide explicitly whether to continue or restore them before rollback.

## Maven Wrapper vs Maven

For a target application, the Workbench selects Maven according to the JVM host OS. On Windows it prefers `mvnw.cmd` and executes batch wrappers through `cmd.exe`. On Linux/WSL/macOS it prefers the POSIX `mvnw` wrapper and otherwise falls back to system `mvn`; Windows-only `.cmd` wrappers are ignored on non-Windows hosts. This keeps Git Bash and WSL execution distinct and prevents accidental `cmd.exe` launches from Linux environments.


## Windows assessment includes `.m2` or unrelated directories

The Workbench treats the selected `--project` directory as a hard application boundary. On Windows, Git Bash path aliases, Maven dependency discovery, parent POM resolution, and filesystem junctions can expose paths outside the application while MTA builds analysis context.

Current Workbench behavior is:

1. normalize Git Bash paths such as `/c/work/app`;
2. canonicalize the application root with `Path.toRealPath()`;
3. launch MTA from an isolated temporary working directory instead of the application parent;
4. retain only normalized findings whose file belongs to the canonical application root; and
5. discard generated/build/dependency metadata paths such as `.migration/`, `target/`, `.git/`, and `.m2/repository/`.

The assessment log prints the effective root:

```text
MTA application root: C:\work\showcase
MTA execution directory: C:\Users\...\Temp\mta-workbench-...
```

If the first path is broader than the application itself, rerun the command with an explicit project root, for example:

```bash
migrate assess --project /c/work/showcase
```

MTA may still read Maven repository artifacts internally for dependency/type information. Those external paths are analyzer context and are not allowed into Workbench `findings.json`.

## Git HEAD changed since migration started

The Workbench binds checkpoint/rollback state to the Git commit that was current when migration began.
If HEAD changes before any checkpoint exists and the application tree is clean, the baseline is refreshed automatically.
Once a checkpoint exists, rebinding would make rollback unsafe and is refused.

For a deliberate fresh run from the current commit:

```bash
migrate restart --project .
migrate assess --project .
migrate plan --project .
migrate migrate --project .
```

`restart` preserves `migration.yaml` but removes prior Workbench run state, checkpoints, assessment/verification output, plan and generated reports. It requires a clean application working tree.

## MTA reads `.m2` or unrelated directories

Use the default strict source boundary:

```yaml
mta:
  strictProjectScope: true
  mode: source-only
```

`full` MTA mode analyzes source and dependencies and can legitimately inspect the Maven local repository,
parent POMs, or reactor modules. With `strictProjectScope: true`, the Workbench forces `source-only` even
when an older configuration still requests `full`. Set `strictProjectScope: false` only when dependency-aware
analysis is intentional.
