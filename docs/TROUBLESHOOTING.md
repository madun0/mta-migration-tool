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
