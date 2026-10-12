## SNAPSHOT - persisted MTA report links

- Rebased MTA's user-facing static-report and `analysis.log` links from the temporary staging directory to the durable `.migration/assessment` or `.migration/verification` directory while output is streamed.
- Rewrites staging-directory references embedded in persisted textual MTA report assets after they are copied from staging.
- Prints canonical persisted static-report and analysis-log URIs after the report is installed.
- Added regression coverage for console-link rebasing and persisted report/log path rebasing.

## SNAPSHOT - generated build-output restore + execution timers

- Fixed MTA analysis cleanup when Maven/MTA recreates `target/` while the original build output is temporarily hidden. The transient replacement is now deleted and the original build tree is restored.
- Added per-family migration timers, overall `migrate` and `verify` timers, and MTA external-process elapsed time.
- Added regression coverage for recreated `target/` collision handling and timer formatting.

## Unreleased - MTA cleanup compile fix

- Fixed the CLI compilation regression in `MtaService`: static MTA isolation helpers now call a static `deleteQuietly(Path)` cleanup helper.
- No migration behavior changed; the descriptor recipes, `.migration` isolation, Maven `target/` isolation, and `1.0.0-SNAPSHOT` recipe coordinate remain unchanged.

## Unreleased - MTA descriptor verification isolation

- Exclude Maven `target/` trees from every MTA assessment by temporarily moving them outside the application root while `mta-cli analyze` runs, then restoring them byte-for-byte. This prevents generated/stale descriptor copies such as `target/classes/META-INF/persistence.xml` from re-triggering Java EE XSD findings.
- Strengthen the `jakarta-descriptors` verification gate to reject any remaining legacy JCP/Java EE XML namespace under application source, not only a fixed list of descriptor XSD filenames.
- Keep the recipe artifact coordinate at `com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT`.

# Changelog

## SNAPSHOT MTA verification isolation

- Fixed `migrate verify` re-analyzing `.migration/assessment/output.yaml` and other Workbench-generated artifacts during its post-migration MTA pass.
- MTA now runs with `.migration` temporarily moved outside the application root and writes its report to an external staging directory; Workbench restores `.migration` and copies only the requested fresh assessment/verification report back afterward.
- This keeps both normalized residual classification and the raw `MTA findings` incident count free of historical report snippets.
- Added regression coverage proving Workbench state is hidden/restored safely and that staged MTA output replaces only the requested assessment/verification subdirectory.
- Kept the OpenRewrite recipe artifact coordinate at `com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT`.

## SNAPSHOT descriptor follow-up

- Fixed `MigrateJakartaDescriptors` schema-location replacement so existing `xsi:schemaLocation` values are replaced exactly once instead of being duplicated by the zero-length terminal match produced by `(?s).*`.
- Removed regex matching for descriptor schema locations; `ChangeTagAttribute` now targets the existing attribute value directly with `oldValue: null`, which is multiline-safe and idempotent.
- Added an idempotency regression test for an already-migrated Jakarta Faces 4 descriptor.
- Kept `com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT` as the recipe artifact coordinate.

## v37

- Hardened Jakarta descriptor migration for multiline `xsi:schemaLocation` values in `faces-config.xml`, `beans.xml`, `web.xml`, and `persistence.xml`.
- Added descriptor migration for legacy `javax.faces.*` context-param names such as `javax.faces.PROJECT_STAGE`.
- Added multiline XML regression tests for Faces and CDI descriptors plus a web.xml Faces context-param regression test.
- Restored the OpenRewrite recipe artifact coordinate to `com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT` while the Workbench remains under active development. Run `mvn clean install` after updating the Workbench so the current snapshot replaces the previously installed recipe JAR in `~/.m2`.
- Updated `doctor` and migration logs to report the exact SNAPSHOT recipe artifact coordinate being executed.
- Updated recipe documentation to use the development SNAPSHOT coordinate.

## v36

- Made `jakarta-descriptors` always-on so descriptor migration no longer depends on MTA emitting XML findings.
- Added Jakarta CDI 4.0 `beans.xml` migration to the Jakarta EE namespace, `beans_4_0.xsd`, and version `4.0` while preserving existing `bean-discovery-mode`.
- Expanded `faces-config.xml` migration to support both legacy `java.sun.com` and `xmlns.jcp.org` Java EE namespaces and normalize to Jakarta Faces 4.0.
- Added missing-version normalization for supported descriptors and expanded descriptor residual checks.
- Corrected the Jakarta EE 10 persistence descriptor target from Persistence 3.0 to Persistence 3.1.
- Added regression coverage for JSF 2.1/2.2 `faces-config.xml`, CDI 2.0 `beans.xml`, and the Jakarta Persistence 3.1 descriptor baseline.

## v35

- Fixed the property-driven JSF migration regression test so it declares the expected transformed POM instead of incorrectly asserting that the recipe makes no change.
- Reordered `MigrateJakartaDependencies` so `javax.faces:javax.faces-api` moves to `jakarta.faces:jakarta.faces-api` before `jsf.version` is normalized to `4.0.1`, avoiding a transient resolution attempt for the invalid coordinate `javax.faces:javax.faces-api:4.0.1`.
- Added a regression assertion that the migrated POM contains no legacy-Faces resolution-failure marker.

## v34

- Update an existing Maven `jsf.version` property to `4.0.1` during `MigrateJakartaDependencies`, fixing property-driven `javax.faces-api` dependencies that retained a JSF 2.x version after coordinate migration.
- Added a regression test covering a POM whose JSF dependency uses `${jsf.version}`.

## v33

- Made `jakarta-imports` an always-on FOUNDATION family so `migrate migrate` does not depend on MTA emitting every `javax` namespace finding.
- Reordered FOUNDATION migration to run `faces-managed-beans` → `jakarta-imports` → `jakarta-dependencies` → `jakarta-descriptors`, preserving legacy source type attribution until namespace migration is complete.
- Added explicit Maven migration for `javax.inject:javax.inject` and `javax.inject:javax.inject-api` to `jakarta.inject:jakarta.inject-api:2.0.1` with `provided` scope.
- Expanded automatic residual checks to cover all curated Jakarta EE source namespaces, including `javax.inject`, while excluding removed `javax.faces.bean` APIs from the generic namespace family.
- Added regression tests for Jakarta Inject dependency/import migration and catalog ordering.
- Explicit `migrate migrate --family NAME` now reruns the selected family even if an older migration state marked it complete, enabling targeted Jakarta namespace repair after upgrading the Workbench.

## v31

- Changed Maven execution to prefer the system `mvn` on `PATH`, matching a developer's direct Maven invocation; a project Maven Wrapper is now fallback-only.
- `migrate migrate` continues to execute Maven from the application root so Maven can load `.mvn/maven.config`, `.mvn/jvm.config`, and `.mvn/extensions.xml` normally.
- Removed the Workbench-owned temporary Maven settings/repository injection from the `migrate migrate` execution path so OpenRewrite no longer overrides project/user Maven repository configuration.
- `migrate doctor` now reports the Maven executable selected by the Workbench and whether `.mvn/maven.config` is present.

## v30

- Added a replacement `MtaServiceFindingScopeTest` using the current MTA-authoritative scope semantics so upgrading by overlay replaces stale pre-v28 test files instead of leaving them behind.
- No production behavior changes from v29.

## v29

- Removed the obsolete direct `ruleID` parser-shape regression test; MTA 8.3 `violations.<rule>.incidents[]` is the authoritative report structure.
- No production finding semantics changed from v28. `findings.json` remains one record per MTA violation incident, while `planning-findings.json` remains the Workbench-normalized planning artifact.
- Note: older worktrees may still contain `MtaServiceFindingScopeTest.java` from pre-v28 releases. That test is not part of the v28/v29 project and must be deleted when upgrading in place because archive extraction does not remove stale files.

## v28

- Made `findings.json` a one-record-per-MTA-violation-incident mirror of `output.yaml`; no Workbench deduplication or post-analysis path scoping changes the MTA finding set.
- Added `planning-findings.json` for Workbench-only deduplication and generated-file cleanup used by planning and residual classification.
- Removed post-MTA source/full path filtering; `mta.mode` is now enforced only by MTA itself.
- Made `--enable-default-rulesets=true` explicit when invoking MTA alongside Workbench custom rules.
- Changed `analyzeKnownLibraries` default to `false` to match native MTA CLI behavior.
- Log the exact MTA command so direct CLI comparisons use identical arguments.

## v27

- Removed the unnecessary CLI regression test that prohibited `--project` from being reintroduced.
- No production CLI behavior changed; commands continue to operate on the current working directory.

## v26

- Removed the `--project` option from every CLI command.
- All commands now operate exclusively on the current working directory.
- Removed Git Bash project-path translation because project paths are no longer accepted as CLI input.
- Updated command guidance, restart messages, troubleshooting, and user documentation to use `cd <application-root>` followed by `migrate ...`.

## v25

- Removed all legacy MTA configuration compatibility. `mta.target` and `strictProjectScope` are no longer part of the configuration model.
- Removed automatic translation of legacy Workbench pseudo-selectors such as `jsf2`, `primefaces6`, `eap82`, `faces4`, and `primefaces16`.
- `migration.yaml` must use native MTA source/target selectors through `mta.sources` and `mta.targets`. Invalid selectors are rejected by the installed MTA CLI preflight validation.
- Existing obsolete configuration properties now fail parsing instead of being silently ignored or translated.
- Retained the v24 incident-based `MTA findings` count.
- Consolidated release history into this file; individual patch-note and verification-report markdown files are no longer generated.

## v24

- Changed the displayed MTA findings count to the raw number of violation incidents in the MTA report.
- Persisted assessment incident totals separately from normalized planning findings.

## v23

- Simplified normal assessment output to one primary `MTA findings` count.

## v22

- Switched generated MTA scope to native MTA technology selectors and added installed-CLI source/target validation.

## v21 and earlier

Earlier development iterations established Windows-safe execution, full/source-only assessment modes, OpenRewrite migration families, Jakarta EE 10 compile baselines, checkpoints/resume/rollback, and PrimeFaces 6.2 to 16 migration recipes.
