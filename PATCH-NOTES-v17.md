# v17 - Stabilize Jakarta Faces dependency regression tests

## Problem

v16 correctly adds `jakarta.faces:jakarta.faces-api:4.0.1` with `provided` scope, but four `FoundationRecipeTransformationTest` cases failed because their expected POM text assumed a specific insertion order inside `<dependencies>`. OpenRewrite emitted the same dependency set in a different order.

The failure was test-only: the migrated POM contained the required Jakarta Faces API dependency and there were no runtime/test errors from the recipe implementation itself.

## Changes

- Left all production OpenRewrite recipes unchanged from v16.
- Updated the four failing expected POMs to match OpenRewrite's observed deterministic output order.
- Added semantic `afterRecipe` validation for every foundation test that requires Jakarta Faces.
- The semantic assertion verifies:
  - exactly one `jakarta.faces:jakarta.faces-api` dependency;
  - version `4.0.1`;
  - scope `provided`.
- The tests therefore validate the actual Maven contract in addition to rendered POM output.

## Regression coverage

The Jakarta Faces assertion is applied to:

- `migratesJavaxFacesDependencyToJakartaFaces`
- `addsRequiredJakartaAnnotationAndCdiApisWhenLegacyPomDoesNotDeclareThem`
- `addsProvidedJakartaFacesWhenPrimeFacesAppReliedOnContainerProvidedJsf`
- `ensuresJakartaFacesCompileApiWithProvidedScope`
- `normalizesExistingJakartaFacesDependencyToProvidedWithoutDuplicate`
- `upgradesPrimeFacesDependencyToSixteen`
- `migratesJavaxEjbDependencyToJakartaEjb`

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
