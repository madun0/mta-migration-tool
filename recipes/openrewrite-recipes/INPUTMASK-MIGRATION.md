# PrimeFaces 6.2 InputMask compatibility

Two important boundaries affect InputMask:

- PrimeFaces 7 requires a non-empty `mask`
- PrimeFaces 8 validates masks on the server by default

## Concrete recipes

```text
PrimeFaces62InputMaskInventory
PrimeFaces62InputMaskReview
PrimeFaces62InputMaskMigration
```

A legacy component with no mask:

```xhtml
<p:inputMask value="#{bean.value}" />
```

or an empty conditional mask:

```xhtml
<p:inputMask
    value="#{bean.postalCode}"
    mask="#{bean.country eq 'US' ? '99999' : ''}" />
```

needs an application decision. Use a valid mask for every rendered InputMask,
or conditionally render/use `p:inputText`.

PrimeFaces 8 introduced server-side mask validation. `validateMask="false"`
may be needed temporarily for custom client mask definitions, but disabling
validation should be reviewed rather than added automatically.

This family is intentionally review-oriented; it does not invent business
validation masks.

Run inventory:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62InputMaskInventory \
  -Drewrite.exportDatatables=true
```
