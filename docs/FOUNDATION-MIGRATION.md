# Foundation Migration: Java 21, EAP 8 and Jakarta Faces 4

The Workbench deliberately treats the foundation phase as **manual/guided** in version 1.
MTA identifies Java EE / JSF compatibility issues and this phase must be completed before
PrimeFaces-specific transformations are treated as production-ready.

Minimum target baseline:

- Java 21 source/runtime
- JBoss EAP 8 / Jakarta EE 10
- `javax.*` application APIs migrated to the corresponding `jakarta.*` APIs
- Jakarta Faces 4
- JSF `@ManagedBean` migrated to CDI where applicable
- JSP-based Faces views converted to Facelets/XHTML

Run `migrate assess` and use the MTA report under `.migration/assessment` as the source
of truth for foundation findings. When this phase is complete, rerun `migrate assess`
and regenerate the plan.

## Workbench family consolidation

The Workbench does not expose every MTA rule as a separate developer task. Built-in MTA findings and Workbench custom findings are consolidated into migration families using exact IDs and curated rule prefixes. Each finding is assigned to the first matching family in catalog order.

For example, `eap8-faces-00003` and `faces4-managed-bean-0001` both contribute evidence to `faces-managed-beans`; the developer receives one managed-bean migration task with the combined finding count, contributing rule IDs, and affected files.

Any finding not represented by the catalog is preserved in the `unmapped-mta-review` safety-net task when the active profile includes `MANUAL_REVIEW`.

## Foundation automation

The Workbench now provides first-party OpenRewrite recipes for the deterministic parts of the Java 21 / Jakarta EE 10 foundation migration. They are packaged in the existing `primefaces-openrewrite-recipes` artifact so the CLI still has a single recipe artifact to install and invoke.

| Migration family | Automation | Recipe | Compile gate |
| --- | --- | --- | --- |
| `platform-baseline` | ASSISTED | `com.jackson.mta.foundation.PlatformBaselineJava21` | immediate |
| `jakarta-dependencies` | AUTOMATIC | `com.jackson.mta.foundation.MigrateJakartaDependencies` | deferred |
| `jakarta-imports` | AUTOMATIC | `com.jackson.mta.foundation.MigrateJavaxImports` | deferred |
| `jakarta-descriptors` | AUTOMATIC | `com.jackson.mta.foundation.MigrateJakartaDescriptors` | deferred |
| `faces-managed-beans` | ASSISTED | `com.jackson.mta.foundation.MigrateFacesManagedBeans` | deferred until review |
| `primefaces-baseline` | AUTOMATIC | `com.jackson.mta.foundation.UpgradePrimeFacesDependency` | deferred |

### Why some compile gates are deferred

A Jakarta namespace migration has temporary intermediate states. For example, changing Maven dependencies before Java imports can temporarily remove the source-side `javax.*` APIs; changing imports first can create the inverse problem. The catalog therefore supports `compileAfterApply` per family. Transitional families defer compilation until the related transformations and assisted review are complete. Final verification still performs the authoritative compile/test gate.

### Import safety

`MigrateJavaxImports` is deliberately curated. It does **not** perform a blanket `javax.*` to `jakarta.*` rewrite. Java SE packages such as `javax.crypto`, `javax.net`, `javax.sql`, and `javax.xml.parsers` must remain `javax.*`. Faces packages are mapped by supported subpackage so removed `javax.faces.bean` APIs are not incorrectly rewritten to a non-existent `jakarta.faces.bean` package.

### Faces managed beans

`MigrateFacesManagedBeans` automates deterministic scope moves and converts the removed JSF `@ManagedBean` annotation to CDI `@Named`:

- `javax.faces.bean.ApplicationScoped` -> `jakarta.enterprise.context.ApplicationScoped`
- `javax.faces.bean.RequestScoped` -> `jakarta.enterprise.context.RequestScoped`
- `javax.faces.bean.SessionScoped` -> `jakarta.enterprise.context.SessionScoped`
- `javax.faces.bean.ViewScoped` -> `jakarta.faces.view.ViewScoped`
- `javax.faces.bean.NoneScoped` -> `jakarta.enterprise.context.Dependent`
- `@ManagedBean` -> `@Named`
- `@ManagedBean(name = "foo")` -> `@Named("foo")`

The recipe preserves explicit managed-bean names. JSF `eager=true`, `@ManagedProperty`, CDI passivation, and `@ViewScoped` serialization semantics remain assisted-review items because they do not have a universally safe mechanical conversion. The generated TODO contains the developer verification checklist.

### Descriptor targets

The descriptor recipe normalizes common deployment descriptors to the Jakarta EE 10 baseline used by this Workbench: Servlet 6.0 for `web.xml` and Faces 4.0 for `faces-config.xml`. Vendor-specific descriptor semantics remain outside automatic transformation.


## Required Jakarta Faces, CDI, and annotation APIs

The `com.jackson.mta.foundation.MigrateJakartaDependencies` recipe is both substitutive and additive.
In addition to replacing supported legacy `javax.*` Maven coordinates, it guarantees the compile-time
APIs needed by the migrated source and by PrimeFaces 16:

```xml
<dependency>
    <groupId>jakarta.faces</groupId>
    <artifactId>jakarta.faces-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>jakarta.annotation</groupId>
    <artifactId>jakarta.annotation-api</artifactId>
    <version>3.0.0</version>
</dependency>
<dependency>
    <groupId>jakarta.enterprise</groupId>
    <artifactId>jakarta.enterprise.cdi-api</artifactId>
    <version>4.1.0</version>
    <scope>provided</scope>
</dependency>
```

`EnsureJakartaFacesCompileApi` is also included directly in `UpgradePrimeFacesDependency`. This is
intentional: the `primefaces-only` profile does not run the FOUNDATION phase, and a legacy application
may have relied on EAP to provide JSF without declaring `javax.faces-api`. The guard ensures classes
such as `jakarta.faces.model.DataModel` are resolvable before the Jakarta-only PrimeFaces 16 artifact
is introduced. Existing Jakarta Faces declarations are normalized to `provided` scope before the
fallback add runs, avoiding duplicate dependency declarations caused solely by scope differences.

The CDI API carries Jakarta Inject as a compile dependency, so `jakarta.inject.Named` and
`jakarta.inject.Inject` are available to the migrated source while the CDI implementation remains
provided by the target application server.

## Foundation ordering requirement

`faces-managed-beans` must execute before `jakarta-dependencies`. The managed-bean migration relies on the legacy JSF API still being present so OpenRewrite can attribute `javax.faces.bean.ManagedBean` and `javax.faces.bean.ViewScoped`. Replacing `javax.faces-api` with `jakarta.faces-api` first removes those legacy types from the parser classpath and prevents `ChangeType` from matching them.

The intended foundation order is therefore:

1. `faces-managed-beans`
2. `jakarta-dependencies`
3. `jakarta-imports`
4. `jakarta-descriptors`

Compilation remains deferred until the foundation transformations have completed.

## Deterministic RequestContext migration

The `request-context` family is always scheduled in `PRIMEFACES_CORE`. MTA evidence is still retained when available, but planning no longer depends on the custom finding being emitted. This prevents supported `RequestContext` calls from surviving until the PrimeFaces 16 dependency upgrade.
