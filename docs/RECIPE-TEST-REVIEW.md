# Recipe Test Review

## Scope

This review focuses on the OpenRewrite recipe module after the package refactor to `com.jackson.mta`.
The goal is to make catalog-facing tests prove actual source transformations rather than only recipe metadata/composition.

## Changes

- Removed production `JavaParser.classpath("primefaces", "javax.faces-api")` usage from `MigrateRequestContext`.
  JavaTemplate now inherits the migrated application's parser/type context instead of searching the recipe/plugin runtime.
- Reworked `MigrateRequestContextTest` to use explicit `JavaParser.dependsOn(...)` API stubs and before/after source assertions.
- Reworked `MigrateUploadedFileApiTest` to validate:
  - `org.primefaces.model.UploadedFile` -> `org.primefaces.model.file.UploadedFile`
  - `getContents()` -> `getContent()`
  - `getInputstream()` -> `getInputStream()`
  - field, parameter, and return type migration.
- Added catalog-facing composite behavioral tests for RequestContext, FileUpload, StreamedContent, menus, JSON, LazyDataModel,
  DataTable filtering, rows-per-page, Calendar, Schedule, Timeline, InputSwitch, Repeat, InputMask review, OverlayPanel,
  autoUpdate, Charts, Push review, and legacy Facelet/Watermark migration.
- Added behavioral tests for declarative foundation recipes in `META-INF/rewrite/foundation.yml`, including Jakarta dependency/import
  migration and the PrimeFaces dependency upgrade.
- Added a cross-recipe XHTML regression suite for safe markup transformations.

## Testing policy

Transformation recipes must include at least one representative before/after source assertion.
Review-only recipes must include source that proves the actionable review annotation is inserted.
Inventory/search recipes may remain marker-oriented because they intentionally do not perform source migration.

Java tests should prefer `JavaParser.dependsOn(...)` source stubs over artifact-name `.classpath(...)` lookups when the purpose of the
test is recipe semantics. This keeps the test deterministic and independent of the test runner's runtime JAR names.

## Structural checks performed in this environment

- Root and recipe-module Maven POMs parse as XML.
- `config/migration-catalog.yaml` parses successfully.
- Every recipe referenced by the catalog resolves to a Java class or declarative YAML recipe.
- Every PrimeFaces recipe referenced by the catalog has a behavioral source test.
- No production recipe uses `JavaParser.classpath(...)`.
- No `com.redhat.mta` references remain under recipe source/test YAML/Java trees.

## Build gate

Maven is not installed in this execution environment, so the dependency-aware JUnit suite could not be executed here.
Run the following on the development workstation:

```bash
mvn -pl recipes/primefaces-openrewrite-recipes clean test
mvn clean install
```

Then exercise the sample migration from a clean baseline before using `resume` on an older persisted plan.
