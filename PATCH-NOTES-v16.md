# v16 - Guarantee Jakarta Faces API before PrimeFaces 16

## Problem

A migration could fail with `jakarta.faces.model.DataModel not found` when the legacy application relied on JBoss EAP to provide JSF and therefore did not declare `javax.faces:javax.faces-api` in its Maven POM. PrimeFaces 16 is Jakarta Faces based and its classes must be resolved against the Jakarta Faces API during OpenRewrite parsing and Maven compilation.

The `primefaces-only` profile also skips the FOUNDATION phase, so fixing only `MigrateJakartaDependencies` would not protect that path.

## Changes

- Added `com.jackson.mta.foundation.EnsureJakartaFacesCompileApi`.
- The guard normalizes an existing `jakarta.faces:jakarta.faces-api` dependency to `provided` scope before adding the dependency when missing.
- `MigrateJakartaDependencies` invokes the guard.
- `UpgradePrimeFacesDependency` invokes the guard before upgrading `org.primefaces:primefaces` to 16.0.0.
- Added regression tests for:
  - legacy PrimeFaces POM with no explicit JSF dependency;
  - existing Jakarta Faces dependency with default/compile scope;
  - PrimeFaces 16 upgrade adding the required Faces API;
  - legacy `javax.faces-api` migration retaining the Jakarta Faces API with `provided` scope.

## Expected result

Before PrimeFaces 16 is introduced, the target POM contains:

```xml
<dependency>
    <groupId>jakarta.faces</groupId>
    <artifactId>jakarta.faces-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>
```

This supplies `jakarta.faces.model.DataModel` to Maven/OpenRewrite while leaving the Faces implementation to JBoss EAP at runtime.
