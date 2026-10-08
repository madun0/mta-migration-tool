# PrimeFaces DataTable rowsPerPageTemplate migration

PrimeFaces 7 removed the bare star shorthand in `rowsPerPageTemplate`.

Old:

```xhtml
<p:dataTable rowsPerPageTemplate="5,10,25,*" />
```

Target:

```xhtml
<p:dataTable rowsPerPageTemplate="5,10,25,{ShowAll|'*'}" />
```

## Concrete recipes

```text
PrimeFaces62RowsPerPageInventory
PrimeFaces62RowsPerPageMigration
```

The recipe performs a regex replacement inside the attribute value, preserving
the existing numeric page-size options.

Run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62RowsPerPageMigration
```
