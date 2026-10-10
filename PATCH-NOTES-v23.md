# v23 patch notes

## Single MTA findings count

- Simplifies assessment output to one primary count: `MTA findings: <n>`.
- Removes the separate `MTA actionable findings captured`, `MTA insights captured separately`, and `Workbench planning findings` counts.
- Scope diagnostics no longer print secondary numeric finding totals.
- Keeps `findings.json` as the planner input and the source of the displayed count.
- Retains `insights.json` and `dependencies.yaml` as diagnostic artifacts without presenting them as competing findings totals.
- Final verification still reports baseline vs final findings because that is a migration comparison rather than an assessment count.
- Assessment and migration semantics are otherwise unchanged from v22.
