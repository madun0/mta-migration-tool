# Development Guide

## Build

```bash
mvn clean install
```

## CLI module only

```bash
mvn -pl workbench-cli -am test
```

## Recipe module only

```bash
mvn -pl recipes/primefaces-openrewrite-recipes test
```

## Adding a migration family

1. Add/validate MTA rule IDs under `rules/`.
2. Add concrete OpenRewrite recipe classes to the recipe module.
3. Add tests for every recipe class.
4. Add the family to `config/migration-catalog.yaml`.
5. Add a migration guide.
6. Add forbidden residual patterns where useful.
7. Run the full build and catalog validation.

Keep command classes thin. New migration intelligence belongs in MTA rules, OpenRewrite recipes, or the catalog.


## Java documentation

Java documentation is required for new source. See `CODE-DOCUMENTATION.md` for the full standard.

At minimum:

1. every named class, record, and interface has class-level Javadoc;
2. Workbench public service methods document behavior, parameters, return values, and checked failures;
3. serialized configuration/state/plan fields document their persistence meaning;
4. every OpenRewrite recipe has a meaningful class Javadoc, `getDisplayName()`, `getDescription()`, and matching documented test class;
5. non-obvious migration safety boundaries are explained in code rather than only in external guides.

## Custom MTA rule loading

The workbench passes custom rules to MTA as explicit rule files rather than as ruleset directories:

- `rules/migration/ruleset.yaml`
- `rules/migration/faces4.yaml`
- `rules/migration/primefaces.yaml`
- `rules/migration/workbench-extra.yaml`

The `ruleset.yaml` files remain as ruleset metadata, but are not passed by the workbench during analysis. This avoids the same custom rule being discovered more than once when directory ruleset loading is combined with the workbench source/target selection. Each executable rule already carries its own `konveyor.io/source` and `konveyor.io/target` labels.
