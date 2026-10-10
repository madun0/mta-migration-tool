# v20 — YAML-only MTA mode and true full-mode findings

## Fixed

- `mta.mode: full` now remains full throughout the Workbench pipeline.
- Maven-repository findings are no longer globally discarded by `MtaOutputParser`.
- `source-only` applies the canonical project-root finding boundary.
- `full` preserves external dependency findings emitted by MTA, including `.m2/repository` and parent/reactor-module incidents.
- Planning and verification now load findings with the same `MigrationConfig` mode used by assessment.
- In full mode, `loadFindings` prefers raw `output.yaml` when available so v20 can recover dependency findings from v19 assessments whose `findings.json` was incorrectly project-filtered.

## Configuration

MTA mode is configured only in `migration.yaml`:

```yaml
mta:
  mode: source-only   # default
```

or:

```yaml
mta:
  mode: full
```

There is intentionally no CLI `--mode` option. The legacy `strictProjectScope` field from v15-v19 is ignored in v20 and is no longer written to generated configuration files.

## Tests

Added regression coverage for:

- full mode retaining `.m2` dependency findings;
- source-only mode dropping out-of-project findings;
- recovery of v19 full-mode dependency findings from raw `output.yaml`;
- `mode: full` remaining authoritative even when an older YAML file still contains `strictProjectScope: true`;
- generated `migration.yaml` exposing `mode` but not the legacy scope property.
