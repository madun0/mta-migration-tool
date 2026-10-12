# PrimeFaces 6.2 InputSwitch -> PrimeFaces 16 ToggleSwitch

PrimeFaces deprecated `InputSwitch` in 10.0 and removed it in 15.0.
`ToggleSwitch` is the replacement.

This project provides four concrete Java recipes:

```text
com.jackson.mta.primefaces.PrimeFaces62InputSwitchInventory
com.jackson.mta.primefaces.PrimeFaces62InputSwitchToToggleSwitch
com.jackson.mta.primefaces.PrimeFaces62InputSwitchCleanupAfterReview
com.jackson.mta.primefaces.PrimeFaces62InputSwitchMigration
```

## 1. Inventory

Run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62InputSwitchInventory \
  -Drewrite.exportDatatables=true
```

This locates:

```text
<p:inputSwitch>
org.primefaces.component.inputswitch.InputSwitch
org.primefaces.component.inputswitch.InputSwitchBase
onLabel / offLabel / showLabels
```

The inventory is search-only. Do not commit OpenRewrite search markers.

## 2. Safe migration

Simple PrimeFaces 6.2 markup:

```xhtml
<p:inputSwitch
    value="#{settings.enabled}"
    widgetVar="enabledSwitch">
    <p:ajax update="messages"/>
</p:inputSwitch>
```

becomes:

```xhtml
<p:toggleSwitch
    value="#{settings.enabled}"
    widgetVar="enabledSwitch">
    <p:ajax update="messages"/>
</p:toggleSwitch>
```

The common widget methods remain available on ToggleSwitch:

```text
check()
uncheck()
toggle()
disable()
enable()
```

so normal `PF('widgetVar').toggle()` style usage generally survives.

Programmatic component references are migrated from:

```java
org.primefaces.component.inputswitch.InputSwitch
```

to:

```java
org.primefaces.component.toggleswitch.ToggleSwitch
```

## 3. Legacy text labels are not silently discarded

PrimeFaces 6.2 InputSwitch supported:

```xhtml
<p:inputSwitch
    value="#{bean.enabled}"
    onLabel="Enabled"
    offLabel="Disabled"/>
```

Current ToggleSwitch does not expose `onLabel`, `offLabel`, or `showLabels`.
It exposes `onIcon` and `offIcon`, but icons are not semantically equivalent
to visible text.

For that reason, the main recipe leaves such tags as `p:inputSwitch` and adds
a migration comment.

Typical final UI:

```xhtml
<h:outputText value="Disabled"/>
<p:toggleSwitch value="#{bean.enabled}"/>
<h:outputText value="Enabled"/>
```

or use accessible surrounding labels appropriate to the page.

If the old labels were actually icon-like semantics, evaluate:

```xhtml
<p:toggleSwitch
    value="#{bean.enabled}"
    onIcon="pi pi-check"
    offIcon="pi pi-times"/>
```

instead.

## 4. showLabels="false" is safe

If the old component explicitly had:

```xhtml
<p:inputSwitch
    value="#{bean.enabled}"
    onLabel="Yes"
    offLabel="No"
    showLabels="false"/>
```

the text was already hidden.

The recipe therefore removes all three legacy label attributes and converts
the tag to `p:toggleSwitch`.

## 5. Final cleanup

After you manually reproduce the intended UX for every commented label case,
run:

```text
com.jackson.mta.primefaces.PrimeFaces62InputSwitchCleanupAfterReview
```

Dry run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62InputSwitchCleanupAfterReview
```

It removes residual:

```text
onLabel
offLabel
showLabels
```

and renames remaining `p:inputSwitch` tags.

Do not run this cleanup until the visible-label behavior has been intentionally
reimplemented or intentionally removed.

## 6. CSS and JavaScript review

InputSwitch and ToggleSwitch have different rendered markup and CSS class
structures.

Search application resources for:

```text
.ui-inputswitch
.ui-inputswitch-on
.ui-inputswitch-off
.ui-inputswitch-handle
PrimeFaces.widget.InputSwitch
.onLabel
.offLabel
.handle
.onContainer
.offContainer
```

The standard widget methods such as `toggle()`, `check()`, `uncheck()`,
`disable()`, and `enable()` are available on ToggleSwitch, but code reaching
into InputSwitch's internal DOM properties must be rewritten.

## 7. Run the main recipe

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62InputSwitchMigration
```

Review and then replace `dryRun` with `run`.

## 8. Verify

```bash
grep -RIn \
  -e "<p:inputSwitch" \
  -e "component.inputswitch.InputSwitch" \
  -e "onLabel=" \
  -e "offLabel=" \
  -e "showLabels=" \
  -e "ui-inputswitch" \
  -e "PrimeFaces.widget.InputSwitch" \
  src

mvn clean test
```

Browser testing should cover keyboard focus, Ajax change behavior, disabled
state, labels/accessibility, custom styles, and any direct widget JavaScript.
