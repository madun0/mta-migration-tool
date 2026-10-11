# v24 verification report

- `VERSION` is 24.
- User-facing `MTA findings` count is sourced from raw MTA violation incidents.
- Normalized `findings.json` remains deduplicated for planning/residual classification.
- Assessment uses one YAML parse for findings, insights, and incident count.
- Java sources and YAML files were structurally checked in the packaging environment.
- Maven test execution must be run on the workstation because Maven is not installed in the packaging environment.
