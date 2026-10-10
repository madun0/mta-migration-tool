# v15 - Strict MTA project scope

- Default MTA mode changed from `full` to `source-only`.
- Added `mta.strictProjectScope` (default `true`).
- Strict scope forces `source-only` even when an older `migration.yaml` requests `full`.
- `full` dependency-aware analysis now requires explicit opt-in with `strictProjectScope: false`.
- Added `MtaServiceModeTest` regression coverage.
- Retains v13/v14 canonical path handling, isolated MTA working directory, and normalized finding root filtering.
