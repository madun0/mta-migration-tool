# Verification Report

## Foundation Automation Increment

Implemented and structurally verified:

- Six declarative OpenRewrite recipes under `META-INF/rewrite/foundation.yml`.
- Java 21 compiler normalization.
- Curated Jakarta Maven dependency migration.
- Curated `javax.*` -> `jakarta.*` Java package migration that excludes Java SE `javax` packages and removed `javax.faces.bean` APIs.
- Jakarta EE 10 descriptor normalization for `web.xml` and `faces-config.xml`.
- Assisted JSF managed-bean scope migration with explicit developer verification steps.
- PrimeFaces dependency upgrade to 16.0.0.
- Catalog-level `reviewChecklist` and `compileAfterApply` controls.
- Project-relative paths in generated `MIGRATION-PLAN.md` and `TODO.md`.
- Cleanup tasks now use `Triggered by` semantics and report zero new findings.
- Resume workflow preserves review confirmation so post-review execution can reach `MIGRATED`.

## Assessment mapping regression

The supplied 48-finding assessment was replayed against the updated catalog model:

- Primary findings assigned: **48**
- Unmapped findings: **0**
- `jakarta-dependencies`: 6
- `jakarta-imports`: 12
- `jakarta-descriptors`: 12
- `faces-managed-beans`: 10
- `primefaces-baseline`: 1
- `request-context`: 1
- `file-upload`: 5
- `calendar`: 1

All recipe names referenced by the migration catalog resolve to either a Java recipe class or a declarative recipe in the packaged recipe source tree.

## Compilation / smoke verification

The modified Workbench production classes (`MigrationCatalog`, `MigrationPlan`, `MigrationPlanner`, `ReportService`, and `WorkflowService`) were compiled with Java 21 against the dependency/runtime jars from the previously built Workbench distribution.

A Java smoke harness also verified:

- first-match family planning,
- `compileAfterApply=false` propagation,
- cleanup tasks carry `Triggered by` and zero findings,
- cleanup compile gates remain enabled,
- `file:///...` MTA locations render as project-relative paths in the generated plan.

The environment does not provide Maven, so the complete multi-module `mvn clean install` and execution of the new declarative OpenRewrite recipes must be run on the target workstation before release acceptance.

## 2026-10-02 pre-migrate catalog correction

Before the first real `migrate` run, the migration catalog was corrected so developer guidance matches execution behavior:

- `jakarta-dependencies` now explicitly defers compilation until the Jakarta dependency/import/descriptor/Faces source transformations are complete.
- `jakarta-imports` now instructs developers to compile during phase validation rather than immediately after the import recipe.
- `file-upload` now surfaces four assisted-review checks covering UploadedFile/listener signatures, `p:validateFile`, removal of legacy Commons FileUpload configuration, and EAP 8 multipart testing.
- `calendar` now surfaces four assisted-review checks covering Java date/time types, pattern/locale/timezone behavior, date restrictions/navigation, and Ajax/converter/validator behavior.
- `CatalogStructureTest` contains regression assertions for these catalog behaviors.

Validation performed in this environment:

- `config/migration-catalog.yaml` parses successfully as YAML.
- `jakarta-dependencies.compileAfterApply == false`.
- `jakarta-imports.compileAfterApply == false`.
- `file-upload` contains four developer verification checks.
- `calendar` contains four developer verification checks.

A full Maven test run still requires Maven on the target workstation.

## 2026-10-01 test compilation fix
- Fixed `CatalogStructureTest` to use `MigrationCatalog.FamilySpec` instead of the non-existent `MigrationCatalog.Family` nested type.
- This addresses the Maven `testCompile` failures reported at lines 226-237 of the user build log.
- Full Maven execution was not possible in this environment because `mvn` is not installed.

## OpenRewrite repository compatibility

Current OpenRewrite releases may resolve from the Code Genome repository rather than Maven Central. The Workbench now creates a temporary Maven user-settings file that preserves existing user settings and activates `https://artifacts.codegenomeproject.org/maven` for both dependencies and Maven plugins. The temporary file is deleted after each OpenRewrite invocation and the target application's `pom.xml` is not modified for repository setup.

## Non-forking OpenRewrite execution fix

- `RewriteService` now invokes `dryRunNoFork` and `runNoFork` instead of the lifecycle-forking `dryRun` and `run` goals.
- This prevents Maven from compiling legacy source before Jakarta/PrimeFaces recipes have had a chance to transform it.
- Added `RewriteServiceTest` to lock the non-forking goal selection.


## 2026-10-02 verification semantics and MTA scope correction

- Final verification classifies forbidden-pattern residuals by owning family instead of flattening all matches into one list.
- Residuals from `AUTOMATIC` families set workflow status to `FAILED` and cause `migrate verify` to return a command failure after writing the report.
- Residuals from `ASSISTED`/`MANUAL` families set workflow status to `REVIEW_REQUIRED`.
- Successful family execution and successful build/test verification clear stale `lastError` values.
- `MIGRATION-REPORT.md` now includes compilation/test PASS status and grouped residual-family sections.
- `request-context` is now `ASSISTED`, matching the recipe contract that intentionally preserves unsupported/ambiguous RequestContext usages for manual remediation.
- Legacy `mta.target` is now fallback-only and is never merged into an explicit `mta.targets` list.
- Legacy `mta.target` is fallback-only; explicit `mta.targets` values are preserved as supplied. `eap82` is treated as a valid MTA target and is not rewritten to `eap8`.
- Added regression coverage for request-context catalog semantics, MTA target normalization, and grouped final report output.

Container validation: catalog YAML parsed successfully and modified Java sources passed structural brace checks. Full Maven/JUnit validation must run on the target workstation because Maven is not installed in this execution environment.

## 2026-10-10 v17 Jakarta Faces regression-test stabilization

The user-provided Maven run executed 107 recipe tests and reported four failures, all in `FoundationRecipeTransformationTest`. The diffs showed the expected Jakarta Faces dependency was present with version `4.0.1` and `provided` scope; only dependency ordering differed.

v17 therefore leaves production recipes unchanged from v16 and updates the affected test expectations to the observed OpenRewrite ordering. It also adds semantic `afterRecipe` assertions that require exactly one `jakarta.faces:jakarta.faces-api` dependency with version `4.0.1` and `provided` scope.

Structural verification performed here:

- v16 and v17 `src/main` recipe trees are identical.
- `VERSION` is `17`.
- `PATCH-NOTES-v17.md` documents the test-only change.
- Java source delimiters/braces and package layout were inspected after modification.

Maven is not installed in this execution environment, so final release acceptance requires running the focused foundation test and full reactor build on the target workstation.

## v18 verification addendum

- `foundation.yml` parses successfully as eight OpenRewrite YAML recipe documents.
- `config/migration-catalog.yaml` parses successfully.
- `FoundationRecipeTransformationTest.java` passes a JDK 21 syntax scan; only expected missing test-classpath errors occur because Maven/OpenRewrite/JUnit dependencies are not installed in this execution environment.
- v18 changes are limited to the Jakarta EE compile-baseline recipe/configuration, foundation regression tests, and associated documentation/version metadata.
- Maven test execution remains required in the target build environment:

```bash
mvn -pl recipes/primefaces-openrewrite-recipes \
  -Dtest=FoundationRecipeTransformationTest \
  clean test
mvn clean install
```

## v20 verification addendum

v20 fixes the MTA full-mode findings pipeline while keeping analysis mode YAML-only.

Static/source verification performed in the packaging environment:

- MTA mode selection is sourced from `MigrationConfig.mta.mode` only.
- `AssessCommand` exposes no mode override.
- `WorkflowService.plan()` and `verify()` pass the loaded `MigrationConfig` into finding loads.
- `.m2/repository` is no longer treated as generated content.
- `source-only` retains canonical application-root filtering.
- `full` retains external dependency findings.
- full-mode loads prefer raw `output.yaml` to recover findings stripped by v19 `findings.json`.
- YAML/documentation examples were updated to remove `strictProjectScope`.
- New regression tests cover source-only/full scoping and v19 recovery behavior.

Maven is not installed in this packaging environment, so the JUnit/Maven suite must be executed on the target development workstation.
