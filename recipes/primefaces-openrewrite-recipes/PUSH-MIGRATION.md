# PrimeFaces 6.2 Push migration

PrimeFaces Push was deprecated in the 6.2 timeframe and removed in PrimeFaces
7. The old implementation was based on Atmosphere.

## Concrete recipes

```text
PrimeFaces62PushInventory
PrimeFaces62PushReview
PrimeFaces62PushMigration
```

The recipe inventories:

```text
<p:socket>
org.primefaces.push.*
```

and marks `p:socket` for architectural migration.

There is no safe one-to-one source rewrite because the replacement depends on
the application's messaging topology. On JBoss EAP 8 / Jakarta EE 10, typical
options include:

- Jakarta Faces `<f:websocket>`
- Jakarta WebSocket endpoints
- OmniFaces WebSocket helpers
- an external broker/event-stream integration

Review endpoint paths, authentication, user/channel addressing, message
serialization, reconnection behavior, clustering, and load-balancer affinity.

Run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62PushInventory \
  -Drewrite.exportDatatables=true
```
