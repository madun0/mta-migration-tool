# v21 patch notes

- Restores the Workbench default MTA target to `eap82` (v20 had regressed to `eap8`).
- Keeps `mta.mode` YAML-only.
- Adds `mta.analyzeKnownLibraries` (default `true`), applied only when `mode: full`; maps to MTA `--analyze-known-libraries`.
- Adds optional `mta.mavenSettings`, passed to MTA as `--maven-settings` after validation/canonicalization.
- Adds YAML-only `mta.disableMavenSearch` for restricted/disconnected environments; maps to `--disable-maven-search=true`.
- Captures MTA `insights` separately in `.migration/assessment/insights.json`; insights are not mixed into planner findings.
- Logs effective mode, known-library analysis, Maven settings, actionable finding count, insight count, and dependency report location.
- Preserves full-mode external dependency issue incidents in `findings.json`.
