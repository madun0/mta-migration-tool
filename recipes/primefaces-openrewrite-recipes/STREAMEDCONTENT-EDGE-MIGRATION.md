# Remaining PrimeFaces StreamedContent migration

PrimeFaces 8 deprecated `DefaultStreamedContent` constructors/setters in favor
of `DefaultStreamedContent.builder()` and removed
`LazyDefaultStreamedContent` in favor of a lazy stream Supplier.

PrimeFaces 13 changed content length from `int`/`Integer` to `long`/`Long`.

## Concrete recipes

```text
MigrateDefaultStreamedContentConstructors
PrimeFaces62StreamedContentEdgeInventory
PrimeFaces62StreamedContentEdgeMigration
PrimeFaces62StreamedContentMigration
```

The existing project already migrates `ByteArrayContent`.

The new constructor recipe migrates only high-confidence cases where the first
constructor argument is:

- an already-created InputStream local variable
- an InputStream field
- `new ByteArrayInputStream(...)`

Example:

```java
new DefaultStreamedContent(stream, "application/pdf", "report.pdf")
```

becomes:

```java
DefaultStreamedContent.builder()
    .stream(() -> stream)
    .contentType("application/pdf")
    .name("report.pdf")
    .build()
```

It deliberately does not move arbitrary method calls or constructors such as
`new FileInputStream(...)` into a Supplier because doing so can change checked
exception handling and resource lifetime.

`LazyDefaultStreamedContent` subclasses remain an inventory/manual case. Move
their `initializeStream()` logic into the builder's `stream(() -> ...)`
supplier.

Run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62StreamedContentMigration
```
