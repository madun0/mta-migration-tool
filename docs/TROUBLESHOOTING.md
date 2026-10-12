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

## Maven selection and `.mvn/maven.config`

For a target application, the Workbench prefers the system `mvn` command on `PATH` so `migrate migrate` uses the same Maven installation and project-level configuration as a direct `mvn` invocation. A host-compatible project Maven Wrapper (`mvnw.cmd` on Windows or `mvnw` on Linux/WSL/macOS) is used only when system Maven is unavailable.

All Maven processes run with the application root as their working directory. The Workbench does not parse, replace, or synthesize `.mvn/maven.config`; Maven loads it normally. With Maven 3.9+, put one argument per line in `.mvn/maven.config`. This is especially important for `-s`/`--settings`, `-D...` resolver properties, `-U`, profiles, and corporate repository options.


## Windows assessment includes `.m2` or unrelated directories

The Workbench treats the **current working directory** as the application boundary. Before running any command, change into the application root:

```bash
cd /c/work/showcase
migrate assess
```

Current Workbench behavior is:

1. resolve the current working directory as the application root;
2. canonicalize that root with `Path.toRealPath()` before MTA analysis;
3. launch MTA from an isolated temporary working directory instead of the application parent;
4. let MTA itself enforce `source-only` versus `full` analysis scope; and
5. always discard Workbench/build metadata paths such as `.migration/`, `target/`, and `.git/`.

The assessment log prints the effective root:

```text
MTA application root: C:\work\showcase
MTA execution directory: C:\Users\...\Temp\mta-workbench-...
```

If the application root is wrong, stop the command, `cd` to the intended project root, and run `migrate assess` again. In `full` mode, MTA may intentionally inspect and report dependency paths outside the application root.

## Git HEAD changed since migration started

The Workbench binds checkpoint/rollback state to the Git commit that was current when migration began.
If HEAD changes before any checkpoint exists and the application tree is clean, the baseline is refreshed automatically.
Once a checkpoint exists, rebinding would make rollback unsafe and is refused.

For a deliberate fresh run from the current commit:

```bash
migrate restart
migrate assess
migrate plan
migrate migrate
```

`restart` preserves `migration.yaml` but removes prior Workbench run state, checkpoints, assessment/verification output, plan and generated reports. It requires a clean application working tree.

## MTA reads `.m2` or unrelated directories

Choose the analysis boundary in `migration.yaml`:

```yaml
mta:
  mode: source-only
```

`source-only` is the default and asks MTA to analyze application source only. `full` analyzes
source and dependencies and can legitimately inspect and report findings from the Maven local repository, parent
POMs, or reactor modules:

```yaml
mta:
  mode: full
  analyzeKnownLibraries: false
  mavenSettings: ""
  disableMavenSearch: false
```

In full mode MTA can discover dependencies without necessarily applying rules to libraries it classifies as known
open source. `analyzeKnownLibraries: true` enables that additional analysis. If corporate Maven access requires a
specific settings file, set `mavenSettings`. In disconnected/restricted environments where MTA's remote Maven search
index is unavailable, `disableMavenSearch: true` can be used as a fallback, at the cost of less reliable dependency
classification.

MTA `mode` is configured only from `migration.yaml`; there is no CLI mode override. Use only native MTA selectors in `mta.sources` and `mta.targets`.
