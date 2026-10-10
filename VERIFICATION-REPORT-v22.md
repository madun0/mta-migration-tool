# v22 Verification Report

- YAML files parsed successfully, including multi-document OpenRewrite recipe resources.
- No custom MTA rule uses Workbench pseudo-values in reserved `konveyor.io/source` or `konveyor.io/target` labels.
- Generated/example MTA scope uses native selectors: source `eap7`; targets `eap8`, `openjdk21`, `jakarta-ee`.
- Runtime preflight validates configured source/target values against the installed CLI via `mta-cli rules list-sources` and `mta-cli rules list-targets`.
- Legacy Workbench pseudo-selectors are translated on configuration load for upgrade compatibility.
- Maven/JUnit execution was not available in the packaging environment; run `mvn clean install` in the target development environment.
