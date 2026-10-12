# PrimeFaces 6.2 org.primefaces.json migration

PrimeFaces 8 moved:

```text
org.primefaces.json
```

to:

```text
org.primefaces.shaded.json
```

## Concrete recipes

```text
PrimeFaces62JsonInventory
PrimeFaces62JsonPackageMigration
PrimeFaces62JsonMigration
```

The migration recipe uses OpenRewrite `ChangePackage` recursively so imports,
fully-qualified references, and types under the old JSON package move to the
shaded namespace.

Example:

```java
import org.primefaces.json.JSONObject;
```

becomes:

```java
import org.primefaces.shaded.json.JSONObject;
```

For long-lived application code, consider replacing direct dependency on the
PrimeFaces shaded JSON implementation with an application-owned JSON API such
as Jakarta JSON Processing or Jackson. The shaded package is primarily a
PrimeFaces-bundled implementation detail.

Run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62JsonMigration
```
