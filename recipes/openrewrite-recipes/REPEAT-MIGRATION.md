# PrimeFaces 6.2 p:repeat -> Jakarta Faces ui:repeat

PrimeFaces 13 removed `p:repeat` in favor of the standard Facelets
`ui:repeat`. The PrimeFaces migration guide states that both Mojarra and
MyFaces now provide the behavior for which PrimeFaces originally carried its
own repeat implementation.

This project provides four concrete Java OpenRewrite recipes:

```text
com.jackson.mta.primefaces.PrimeFaces62RepeatInventory
com.jackson.mta.primefaces.PrimeFaces62RepeatToUiRepeat
com.jackson.mta.primefaces.PrimeFaces62RepeatJavaUsageInventory
com.jackson.mta.primefaces.PrimeFaces62RepeatMigration
```

## 1. Why the XHTML migration is high confidence

PrimeFaces 6.2 `org.primefaces.component.repeat.UIRepeat` exposes the iterator
properties:

```text
begin
end
offset
size
step
value
var
varStatus
```

Standard Faces `ui:repeat` supports the same iteration properties, plus
`rendered`.

Therefore markup such as:

```xhtml
<p:repeat
    value="#{ordersView.orders}"
    var="order"
    varStatus="status"
    offset="0"
    size="25"
    step="1">
    ...
</p:repeat>
```

can become:

```xhtml
<ui:repeat
    value="#{ordersView.orders}"
    var="order"
    varStatus="status"
    offset="0"
    size="25"
    step="1">
    ...
</ui:repeat>
```

without inventing replacement semantics.

## 2. Facelets namespace

Jakarta Faces 4 defines the Facelets `ui` library URI as:

```text
jakarta.faces.facelets
```

If a view containing `p:repeat` does not already declare a `ui` prefix, the
recipe adds:

```xhtml
xmlns:ui="jakarta.faces.facelets"
```

to the document root before renaming the tag.

If a view already declares `ui`, the recipe does not intentionally overwrite
that namespace. The broader JSF -> Jakarta Faces 4 namespace migration should
normalize older namespace URIs such as:

```text
http://xmlns.jcp.org/jsf/facelets
```

to the Jakarta Faces namespace as part of the Faces migration stage.

## 3. Java references are not mechanically changed

Some applications may have programmatic coupling to:

```java
org.primefaces.component.repeat.UIRepeat
```

for example:

```java
private UIRepeat repeat;

public void setRepeat(UIRepeat repeat) {
    this.repeat = repeat;
}
```

PrimeFaces 13 removes that class with the component.

Do **not** mechanically map it to:

```java
com.sun.faces.facelets.component.UIRepeat
```

That class belongs to Mojarra's implementation and creates an implementation
dependency that would not be portable to MyFaces.

Instead, run:

```text
com.jackson.mta.primefaces.PrimeFaces62RepeatJavaUsageInventory
```

and refactor the Java code toward a portable abstraction where possible, for
example:

```java
jakarta.faces.component.UIComponent
```

when only generic component operations are required.

If the application relies on UIRepeat-specific methods such as row/index
behavior, review that use case explicitly rather than inventing an
implementation-specific type.

## 4. Inventory

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62RepeatInventory \
  -Drewrite.exportDatatables=true
```

## 5. Run the migration

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62RepeatMigration
```

Review the diff, then replace `dryRun` with `run`.

## 6. Java-coupling inventory

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62RepeatJavaUsageInventory \
  -Drewrite.exportDatatables=true
```

## 7. Verify

```bash
grep -RIn \
  -e "<p:repeat" \
  -e "<ui:repeat" \
  -e "org.primefaces.component.repeat.UIRepeat" \
  -e "xmlns:ui=" \
  src

mvn clean test
```

Browser/integration testing should pay particular attention to:

- nested iterators
- component IDs inside the repeat
- Ajax `process` / `update` expressions
- validation inside repeated inputs
- nested DataTables/TreeTables
- `varStatus` index behavior
- `offset`, `size`, `begin`, `end`, and `step`
- component bindings
