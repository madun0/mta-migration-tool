# v18 - Jakarta EE 10 compile baseline for EAP-hosted applications

## Problem

After the Faces classpath fix, migrations could still fail during compile with errors such as:

```text
package jakarta.servlet.http does not exist
```

The underlying issue was broader than Servlet. Legacy JBoss EAP applications commonly rely on the
application server for Java EE APIs and therefore omit explicit API dependencies from their Maven POMs.
The migration changed source imports from `javax.*` to `jakarta.*`, but v17 only guaranteed a narrow set
of compile APIs.

## Changes

- Added `com.jackson.mta.foundation.EnsureJakartaEe10CompileApi`.
- The guard guarantees `jakarta.platform:jakarta.jakartaee-api:10.0.0` with `provided` scope.
- `MigrateJakartaDependencies` now always establishes that Jakarta EE 10 compile baseline.
- `UpgradePrimeFacesDependency` establishes the same baseline before upgrading PrimeFaces to 16, so the
  `primefaces-only` profile is also protected.
- Explicit legacy Java EE API dependencies continue to migrate to their specific Jakarta coordinates.
- Explicit Jakarta API dependencies for Faces, Validation, Annotation, Servlet, Persistence, REST,
  Transactions, XML Binding, and EJB are normalized to `provided` scope for the EAP runtime.
- The standalone `EnsureJakartaFacesCompileApi` recipe remains for compatibility but is no longer the
  Workbench's primary baseline guard.

## Regression coverage

Foundation recipe tests now cover:

- an EAP application with no explicit Java EE API dependencies;
- a PrimeFaces application that relied on container-provided APIs;
- normalization of an existing Jakarta EE platform dependency to `provided`;
- explicit `javax.servlet:javax.servlet-api` migration to Servlet 6.0 with `provided` scope;
- explicit JSF and EJB dependency migration while preserving the Jakarta EE 10 compile baseline;
- PrimeFaces 16 upgrade with the platform API available before the Jakarta-only dependency is introduced.

## Validation

Run:

```bash
mvn -pl recipes/primefaces-openrewrite-recipes \
  -Dtest=FoundationRecipeTransformationTest \
  clean test
```

Then run the complete reactor:

```bash
mvn clean install
```
