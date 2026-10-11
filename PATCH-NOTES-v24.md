# v24 patch notes

## MTA findings now equals MTA incident count

- `MTA findings: <n>` now reports the raw number of entries under `violations.<rule>.incidents[]` in MTA `output.yaml`.
- The displayed count no longer uses the size of Workbench `findings.json`, which is intentionally deduplicated and may be scope-filtered for planning.
- Assessment parses `output.yaml` once and derives findings, insights, and the incident count from the same report tree.
- Persists `.migration/<assessment>/assessment-summary.json` with the MTA incident count.
- Final verification report uses the baseline/final MTA incident counts for its headline MTA finding totals.
- Added regression coverage proving duplicate report incidents count separately even when Workbench planning findings deduplicate them.
