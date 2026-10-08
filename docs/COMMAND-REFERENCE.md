# Command Reference

| Command | Purpose |
|---|---|
| `migrate init` | Create `migration.yaml` and initialize `.migration/`. |
| `migrate doctor` | Check Java, Git, Maven, MTA, recipe artifact and clean Git state. |
| `migrate assess` | Run MTA with the bundled custom rules and normalize findings. |
| `migrate plan` | Map findings to ordered phases/families. |
| `migrate migrate` | Execute the plan with dry-run, apply, checkpoint and compile validation. |
| `migrate migrate --phase NAME` | Run only one planned phase. |
| `migrate migrate --family NAME` | Explicitly run one catalog family. |
| `migrate status` | Show current state, family and checkpoint. |
| `migrate resume` | Continue the next unfinished family; after `REVIEW_REQUIRED`, continue into cleanup. |
| `migrate rollback` | Roll back to the latest checkpoint. |
| `migrate rollback --checkpoint cp-NNN` | Roll back to a selected checkpoint. |
| `migrate verify` | Compile/test, scan residual patterns, rerun MTA and write the final report. |
| `migrate report` | Print the current report artifact locations. |

All commands accept `--project <path>` where applicable. Git Bash paths such as `/c/work/app` are supported on Windows.


## `verify` result semantics

Final verification now distinguishes build failures, automatic migration regressions, and assisted/manual review work:

- `VERIFIED`: compilation/tests pass and no completed-family forbidden patterns remain.
- `REVIEW_REQUIRED`: compilation/tests pass and remaining forbidden patterns belong only to `ASSISTED` or `MANUAL` families.
- `FAILED`: compilation/tests fail, or a completed `AUTOMATIC` family still has a forbidden legacy pattern.

Residual matches are grouped by owning migration family in both console output and `.migration/MIGRATION-REPORT.md`. A successful build clears stale `lastError` state.
