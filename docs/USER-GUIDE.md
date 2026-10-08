# User Guide

## 1. Build once

```bash
cd /c/tools/mta-migration-tool
mvn clean install
export PATH="$PWD/bin:$PATH"
```

## 2. Initialize a target application

```bash
cd /c/src/customer-portal
migrate init
```

Edit `migration.yaml` if required. Commit it before running migration:

```bash
git add migration.yaml
git commit -m "chore: configure migration workbench"
```

## 3. Validate the environment

```bash
migrate doctor
```

`doctor` checks Java 21, Git, Maven/Maven Wrapper, MTA, the Workbench catalog and the local recipe artifact.

## 4. Assess with MTA

```bash
migrate assess
```

Outputs:

```text
.migration/assessment/output.yaml
.migration/assessment/findings.json
.migration/assessment/static-report/
```

The Workbench passes its bundled `rules/` directory to MTA in addition to the selected EAP 8 target.

## 5. Generate the migration plan

```bash
migrate plan
```

Outputs:

```text
.migration/plan.json
.migration/MIGRATION-PLAN.md
```

MTA rule IDs map to migration families through `config/migration-catalog.yaml`.

## 6. Execute migration

```bash
migrate migrate
```

For each automatic/assisted family, the Workbench:

1. creates a checkpoint
2. runs OpenRewrite dry-run
3. applies the recipe(s)
4. runs Maven compile
5. records the resulting diff hash
6. records assisted/manual follow-up in `.migration/TODO.md`

Target a phase or family:

```bash
migrate migrate --phase PRIMEFACES_COMPONENTS
migrate migrate --family calendar
```

Before `CLEANUP`, the Workbench stops with `REVIEW_REQUIRED` when assisted/manual families exist. Complete `.migration/TODO.md`; `resume` is the explicit acknowledgement to continue into cleanup.

## 7. Status / resume / rollback

```bash
migrate status
migrate resume
migrate rollback
migrate rollback --checkpoint cp-004
```

Rollback is intentionally conservative. It refuses if Git HEAD changed or if the worktree differs from the
recorded Workbench state.

## 8. Verify

```bash
migrate verify
```

Verification performs:

- Maven compile
- Maven tests when enabled
- residual forbidden-pattern scans for completed families
- a second MTA analysis under `.migration/verification`
- final `.migration/MIGRATION-REPORT.md`

## Profiles

Select a profile in `migration.yaml`:

```yaml
profile: standard
```

Available profiles:

- `standard`
- `primefaces-only`
- `assessment-only`

## Important operating rule

Start the first migration from a **clean Git worktree**. Keep manual source edits out of an active automatic family;
finish/rollback the family first, then make manual changes. This is what makes checkpoint rollback predictable.
