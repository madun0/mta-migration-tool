# Changelog

## v31

- Changed Maven execution to prefer the system `mvn` on `PATH`, matching a developer's direct Maven invocation; a project Maven Wrapper is now fallback-only.
- `migrate migrate` continues to execute Maven from the application root so Maven can load `.mvn/maven.config`, `.mvn/jvm.config`, and `.mvn/extensions.xml` normally.
- Removed the Workbench-owned temporary Maven settings/repository injection from the `migrate migrate` execution path so OpenRewrite no longer overrides project/user Maven repository configuration.
- `migrate doctor` now reports the Maven executable selected by the Workbench and whether `.mvn/maven.config` is present.

## v30

- Added a replacement `MtaServiceFindingScopeTest` using the current MTA-authoritative scope semantics so upgrading by overlay replaces stale pre-v28 test files instead of leaving them behind.
- No production behavior changes from v29.

## v29

- Removed the obsolete direct `ruleID` parser-shape regression test; MTA 8.3 `violations.<rule>.incidents[]` is the authoritative report structure.
- No production finding semantics changed from v28. `findings.json` remains one record per MTA violation incident, while `planning-findings.json` remains the Workbench-normalized planning artifact.
- Note: older worktrees may still contain `MtaServiceFindingScopeTest.java` from pre-v28 releases. That test is not part of the v28/v29 project and must be deleted when upgrading in place because archive extraction does not remove stale files.

## v28

- Made `findings.json` a one-record-per-MTA-violation-incident mirror of `output.yaml`; no Workbench deduplication or post-analysis path scoping changes the MTA finding set.
- Added `planning-findings.json` for Workbench-only deduplication and generated-file cleanup used by planning and residual classification.
- Removed post-MTA source/full path filtering; `mta.mode` is now enforced only by MTA itself.
- Made `--enable-default-rulesets=true` explicit when invoking MTA alongside Workbench custom rules.
- Changed `analyzeKnownLibraries` default to `false` to match native MTA CLI behavior.
- Log the exact MTA command so direct CLI comparisons use identical arguments.

## v27

- Removed the unnecessary CLI regression test that prohibited `--project` from being reintroduced.
- No production CLI behavior changed; commands continue to operate on the current working directory.

## v26

- Removed the `--project` option from every CLI command.
- All commands now operate exclusively on the current working directory.
- Removed Git Bash project-path translation because project paths are no longer accepted as CLI input.
- Updated command guidance, restart messages, troubleshooting, and user documentation to use `cd <application-root>` followed by `migrate ...`.

## v25

- Removed all legacy MTA configuration compatibility. `mta.target` and `strictProjectScope` are no longer part of the configuration model.
- Removed automatic translation of legacy Workbench pseudo-selectors such as `jsf2`, `primefaces6`, `eap82`, `faces4`, and `primefaces16`.
- `migration.yaml` must use native MTA source/target selectors through `mta.sources` and `mta.targets`. Invalid selectors are rejected by the installed MTA CLI preflight validation.
- Existing obsolete configuration properties now fail parsing instead of being silently ignored or translated.
- Retained the v24 incident-based `MTA findings` count.
- Consolidated release history into this file; individual patch-note and verification-report markdown files are no longer generated.

## v24

- Changed the displayed MTA findings count to the raw number of violation incidents in the MTA report.
- Persisted assessment incident totals separately from normalized planning findings.

## v23

- Simplified normal assessment output to one primary `MTA findings` count.

## v22

- Switched generated MTA scope to native MTA technology selectors and added installed-CLI source/target validation.

## v21 and earlier

Earlier development iterations established Windows-safe execution, full/source-only assessment modes, OpenRewrite migration families, Jakarta EE 10 compile baselines, checkpoints/resume/rollback, and PrimeFaces 6.2 to 16 migration recipes.
