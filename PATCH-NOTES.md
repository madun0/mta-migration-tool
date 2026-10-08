# Managed Bean Migration Fixes

This revision fixes the JSF managed-bean migration path observed during the JSF 2 / PrimeFaces 6.2 -> Jakarta Faces 4 / PrimeFaces 16 migration.

## Changes

- Added `com.jackson.mta.foundation.MigrateManagedBeanToNamed`.
  - `@ManagedBean` -> `@Named`.
  - `@ManagedBean(name = "foo")` -> `@Named("foo")`.
  - `eager=true` is intentionally not reproduced because CDI `@Named` has no direct equivalent; lifecycle behavior remains an assisted-review item.
- Updated `MigrateFacesManagedBeans` to invoke the new custom recipe after scope migration.
- Corrected the `jakarta.ejb:jakarta.ejb-api:4.0.1` dependency indentation so it is part of `MigrateJakartaDependencies.recipeList`.
- Updated foundation dependency tests to expect the EJB API.
- Added behavioral tests for plain, named, and eager JSF managed beans.
- Updated `docs/FOUNDATION-MIGRATION.md` to document the new behavior.

## Validation

YAML parsing for `foundation.yml` and `config/migration-catalog.yaml` was validated in the build environment. Maven is not installed in the artifact build environment, so run the commands below locally before using the Workbench against an application.

```bash
mvn -pl recipes/primefaces-openrewrite-recipes \
  -Dtest=MigrateManagedBeanToNamedTest,FoundationRecipeTransformationTest \
  clean test

mvn -pl recipes/primefaces-openrewrite-recipes clean test
mvn -pl recipes/primefaces-openrewrite-recipes clean install
```

## Follow-up fixes after focused test run

- Reworked `MigrateManagedBeanToNamed` as a composite recipe:
  - normalizes legacy `@ManagedBean` arguments first;
  - delegates the actual `ManagedBean -> Named` type/import migration to OpenRewrite `ChangeType`;
  - preserves `name = "..."` as `@Named("...")`;
  - removes unsupported `eager` semantics while leaving lifecycle review assisted.
- Reordered the additive CDI/EJB `AddDependency` recipe declarations so OpenRewrite emits the stable dependency order expected by the existing behavioral tests (`CDI` before `EJB`).
