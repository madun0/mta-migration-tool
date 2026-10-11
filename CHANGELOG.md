# Changelog

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
