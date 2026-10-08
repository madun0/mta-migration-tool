# PrimeFaces 6.2 Timeline -> PrimeFaces 16

PrimeFaces 8 upgraded Timeline to a newer vis Timeline and changed its event
date model from `java.util.Date` to `java.time.LocalDateTime`. It also introduced
the fluent `TimelineEvent.builder()` API.

This project adds:

```text
com.jackson.mta.primefaces.MigrateTimelineEventConstruction
com.jackson.mta.primefaces.PrimeFaces62TimelineInventory
com.jackson.mta.primefaces.PrimeFaces62TimelineXhtmlMigration
com.jackson.mta.primefaces.PrimeFaces62TimelineMigration
```

## Java event migration

PrimeFaces 6.2:

```java
new TimelineEvent(
        data,
        startDate,
        endDate,
        true,
        "operations",
        "important");
```

Modern direction:

```java
TimelineEvent.builder()
        .data(data)
        .startDate(
                Optional.ofNullable(startDate)
                        .map(d -> LocalDateTime.ofInstant(
                                d.toInstant(),
                                ZoneId.systemDefault()))
                        .orElse(null))
        .endDate(
                Optional.ofNullable(endDate)
                        .map(d -> LocalDateTime.ofInstant(
                                d.toInstant(),
                                ZoneId.systemDefault()))
                        .orElse(null))
        .editable(true)
        .group("operations")
        .styleClass("important")
        .build();
```

The recipe recognizes both legacy constructor families:

```text
data, start
data, start, editable
data, start, editable, group
data, start, editable, group, styleClass

data, start, end
data, start, end, editable
data, start, end, editable, group
data, start, end, editable, group, styleClass
```

It also migrates `setStartDate(Date)` and `setEndDate(Date)`.

## Timezone review

`Date` identifies an instant; `LocalDateTime` does not carry a timezone.

The recipe uses:

```java
ZoneId.systemDefault()
```

only as a compatibility bridge.

Review it against:

```xhtml
<p:timeline timeZone="..." clientTimeZone="..." />
```

and the business timezone. If the application intentionally operates in a
known zone, replace the bridge with an explicit `ZoneId`.

## Timeline attributes

The XHTML recipe safely maps:

```text
axisOnTop="true"   -> orientationAxis="top"
axisOnTop="false"  -> orientationAxis="bottom"

groupsChangeable   -> editableGroup
timeChangeable     -> editableTime

snapEvents="false" -> snap="null"
snapEvents="true"  -> current default snapping
```

Dynamic `axisOnTop` / `snapEvents` expressions are marked for review.

The PrimeFaces 8 migration guide identifies these legacy Timeline attributes as
removed without direct replacements:

```text
dragAreaWidth
unselectable
groupsOnRight
animate
animateZoom
showButtonNew
showNavigation
browserTimeZone
```

The recipe annotates those usages rather than deleting behavior silently.

`groupsWidth` and `groupMinHeight` are also marked for review because there is
no safe application-independent rename.

## Bound timeline dates

Modern Timeline uses `LocalDateTime` for:

```text
start
end
min
max
```

The XHTML recipe adds a review comment when those attributes are present.
The actual bean-property migration should be performed with application
knowledge because database/API timezone semantics can differ.

## Model generics

PrimeFaces 6.2 used raw:

```java
TimelineModel
TimelineEvent
```

Modern PrimeFaces types are generic:

```java
TimelineModel<E, G>
TimelineEvent<E>
TimelineGroup<G>
```

Raw usages will generally compile with warnings, so this recipe does not invent
generic types. Add the application's real event/group types during cleanup.

## Run

Inventory:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62TimelineInventory \
  -Drewrite.exportDatatables=true
```

Full dry run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62TimelineMigration
```

Apply after review by replacing `dryRun` with `run`.

## Verify

```bash
grep -RIn \
  -e "new TimelineEvent(" \
  -e "setStartDate(" \
  -e "setEndDate(" \
  -e "java.util.Date" \
  -e "<p:timeline" \
  -e "axisOnTop" \
  -e "groupsChangeable" \
  -e "timeChangeable" \
  -e "snapEvents" \
  -e "browserTimeZone" \
  src

mvn clean test
```

Then test timeline rendering, event move/resize behavior, group changes,
selection, server/browser timezone conversion, and DST boundaries.
