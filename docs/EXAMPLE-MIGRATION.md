# Example Migration

Assume the application is located at:

```text
C:\work\customer-portal
```

From Git Bash:

```bash
cd /c/work/customer-portal
```

Initialize:

```bash
/c/tools/mta-migration-tool/bin/migrate init
```

Generated `migration.yaml`:

```yaml
projectName: customer-portal
profile: standard
mta:
  target: eap8
  mode: source-only
validation:
  compileAfterFamily: true
  runTestsOnVerify: true
rewrite:
  dryRunBeforeApply: true
```

Commit the configuration:

```bash
git add migration.yaml
git commit -m "chore: configure migration workbench"
```

Check tools:

```bash
migrate doctor
```

Example:

```text
[OK] Java           - 21.0.x
[OK] Git            - C:\Program Files\Git\cmd\git.exe
[OK] MTA CLI        - C:\tools\mta\mta-cli.exe
[OK] Maven          - C:\work\customer-portal\mvnw.cmd
[OK] Clean Git tree - required before first migrate
READY
```

Assess:

```bash
migrate assess
```

Plan:

```bash
migrate plan
cat .migration/MIGRATION-PLAN.md
```

Example plan excerpt:

```text
PRIMEFACES_CORE
  request-context     AUTOMATIC  31 findings
  streamed-content    ASSISTED    8 findings

PRIMEFACES_COMPONENTS
  calendar            ASSISTED   14 findings
  input-switch        ASSISTED    7 findings
```

Run migration:

```bash
migrate migrate
```

If Calendar fails compilation:

```text
[FAILED] calendar: Maven compile failed
Fix the problem and run 'migrate resume', or run 'migrate rollback'.
```

Inspect:

```bash
migrate status
git diff
```

Rollback that family:

```bash
migrate rollback
```

After resolving a project-specific issue, continue:

```bash
migrate resume
```

After the automatic pass, complete `.migration/TODO.md`. When status is `REVIEW_REQUIRED`, continue into cleanup:

```bash
migrate resume
```

Finally:

```bash
migrate verify
cat .migration/MIGRATION-REPORT.md
```
