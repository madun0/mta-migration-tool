# PrimeFaces 6.2 autoUpdate attribute migration

PrimeFaces 7 removed the `autoUpdate` attribute from:

```text
p:outputPanel
p:fragment
p:messages
p:growl
```

The replacement is a nested:

```xhtml
<p:autoUpdate />
```

## Concrete recipes

```text
PrimeFaces62AutoUpdateInventory
PrimeFaces62AutoUpdateLiteralMigration
PrimeFaces62AutoUpdateMigration
```

Literal true:

```xhtml
<p:growl autoUpdate="true" />
```

becomes conceptually:

```xhtml
<p:growl>
    <p:autoUpdate />
</p:growl>
```

Literal false simply loses the obsolete attribute.

Dynamic expressions are marked for review. Current `p:autoUpdate` supports a
`disabled` expression, but the correct inversion of an arbitrary legacy
`autoUpdate` expression should be reviewed rather than guessed.

Run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62AutoUpdateMigration
```
