# Architecture

## Design goal

Keep the Workbench small enough to understand and support, while making migration execution safe enough for
junior developers.

```text
Git Bash
   |
   v
migrate (Quarkus + Picocli)
   |
   +-- MtaService --------> mta-cli.exe
   +-- RewriteService ----> Maven -> OpenRewrite
   +-- ValidationService -> Maven + residual pattern scan
   +-- WorkflowService
          |
          +-- migration-catalog.yaml
          +-- .migration/state.json
          +-- Git checkpoints
```

## Architectural rules

1. Git Bash is the developer shell, not the orchestration engine.
2. Java `ProcessBuilder` launches MTA, Git and Maven; source logic never depends on `grep`, `sed`, or `awk`.
3. `java.nio.file.Path` is used internally. `/c/...` Git Bash paths are normalized at the CLI boundary.
4. MTA performs assessment and re-assessment. Before MTA starts, the application root is canonicalized with `toRealPath()`. MTA runs from an isolated temporary working directory rather than the application or its parent directory, and normalized findings are accepted only when their source path is inside the canonical application root.
5. OpenRewrite is invoked through the Maven plugin in version 1; the Workbench does not embed the Rewrite runtime.
6. The catalog contains sequencing and recipe knowledge. CLI command classes remain thin.
7. `.migration/state.json` is the only workflow-state file.
8. Git checkpoints use `git stash create` snapshots plus diff hashes. No automatic commits are created.
9. Rollback refuses to run if Git HEAD changed or the worktree no longer matches the recorded Workbench diff.
10. Manual/assisted work is surfaced in `.migration/TODO.md` with links to the relevant guide.

## Profiles and phases

The `standard` profile uses:

```text
FOUNDATION
PRIMEFACES_CORE
PRIMEFACES_DATA
PRIMEFACES_COMPONENTS
MANUAL_REVIEW
CLEANUP
VALIDATION
```

Only families detected by MTA are planned, except the Foundation family, which is always shown. Assisted/manual work creates `.migration/TODO.md`; the workflow stops at the CLEANUP gate until the developer explicitly runs `resume`.
A developer can explicitly run a family with `migrate migrate --family <name>`.

## Checkpoint behavior

Before each automatic/assisted family:

1. verify Git HEAD still equals the migration baseline
2. create a `git stash create` snapshot of the current tracked worktree
3. record state in `.migration/checkpoints/cp-NNN/checkpoint.json`
4. run Rewrite
5. compile
6. hash the resulting Git diff

Rollback verifies the current diff hash before resetting to the baseline and reapplying the checkpoint snapshot.
This prevents the Workbench from silently destroying developer edits made after the checkpoint.
