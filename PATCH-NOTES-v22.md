# v22 — MTA 8.3 native source/target selectors

- `migration.yaml` now uses only native MTA technology selectors in `mta.sources` / `mta.targets`.
- Standard defaults: source `eap7`; targets `eap8`, `openjdk21`, `jakarta-ee`.
- Removed Workbench pseudo-selectors (`jsf2`, `primefaces6`, `eap82`, `faces4`, `primefaces16`) from generated MTA CLI scope.
- Existing v21-and-earlier migration.yaml files are translated on load to native selectors.
- Custom JSF/PrimeFaces rules now use reserved `konveyor.io/source=eap7` and `konveyor.io/target=eap8` labels; framework/version intent is retained under `workbench.io/*` metadata labels.
- Added regression tests preventing Workbench pseudo-values from reappearing in reserved MTA source/target labels.
- The installed MTA CLI remains the authority for custom user-selected technologies: `mta-cli rules list-sources` and `mta-cli rules list-targets`.
- Before analysis, the Workbench now validates configured technologies against the installed MTA CLI using `mta-cli rules list-sources` and `mta-cli rules list-targets`; invalid values fail fast with a migration.yaml-specific error.
