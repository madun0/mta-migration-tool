# PrimeFaces 6.2 OverlayPanel appendToBody migration

PrimeFaces 7 removed `OverlayPanel#appendToBody`.

Use:

```xhtml
appendTo="@(body)"
```

instead.

## Concrete recipes

```text
PrimeFaces62OverlayPanelInventory
PrimeFaces62OverlayPanelAppendToMigration
PrimeFaces62OverlayPanelMigration
```

Literal conversion:

```xhtml
<p:overlayPanel appendToBody="true" ... />
```

becomes:

```xhtml
<p:overlayPanel appendTo="@(body)" ... />
```

`appendToBody="false"` is removed, preserving the normal non-appended behavior.
Dynamic expressions are marked for review because `appendTo` is a search
expression rather than a Boolean.

Run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62OverlayPanelMigration
```
