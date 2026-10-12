# PrimeFaces 6.2 OpenRewrite recipe

This module contains a conservative OpenRewrite recipe for migrating the
deprecated `org.primefaces.context.RequestContext` API used by PrimeFaces 6.2.

## What it transforms

| PrimeFaces 6.2 | Replacement |
|---|---|
| `RequestContext...execute(x)` | `PrimeFaces.current().executeScript(x)` |
| `RequestContext...isAjaxRequest()` | `PrimeFaces.current().isAjaxRequest()` |
| `RequestContext...addCallbackParam(k,v)` | `PrimeFaces.current().ajax().addCallbackParam(k,v)` |
| `RequestContext...update(...)` | `PrimeFaces.current().ajax().update(...)` |
| `RequestContext...reset(...)` | `PrimeFaces.current().resetInputs(...)` |
| `RequestContext...scrollTo(x)` | `PrimeFaces.current().scrollTo(x)` |
| `RequestContext...openDialog(...)` | `PrimeFaces.current().dialog().openDynamic(...)` |
| `RequestContext...closeDialog(x)` | `PrimeFaces.current().dialog().closeDynamic(x)` |
| `RequestContext...showMessageInDialog(x)` | `PrimeFaces.current().dialog().showMessageDynamic(x)` |

The recipe intentionally leaves internal/ambiguous RequestContext APIs alone.

## Build and test

Use JDK 21:

```bash
java -version
mvn test
mvn install
```

`mvn install` publishes the recipe JAR into your local Maven repository.

## Run against the JSF application

First commit or branch the application.

Dry run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62RequestContextMigration \
  -Drewrite.exportDatatables=true
```

Apply:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:run \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62RequestContextMigration \
  -Drewrite.exportDatatables=true
```

Then inspect:

```bash
git diff
grep -RIn "RequestContext" src
```

## Important follow-up

A common PrimeFaces 6.2 pattern is:

```java
RequestContext context = RequestContext.getCurrentInstance();
context.update("form");
```

After the recipe, the method call is migrated, but the now-unused local
`RequestContext context` declaration is deliberately left in place:

```java
RequestContext context = RequestContext.getCurrentInstance();
PrimeFaces.current().ajax().update("form");
```

Remove dead locals only after reviewing residual RequestContext usages. You may
use OpenRewrite's `org.openrewrite.staticanalysis.RemoveUnusedLocalVariables`
recipe or remove them manually.

Do not upgrade the PrimeFaces dependency to 16 until a residual scan confirms
that the application no longer depends on unsupported `RequestContext` APIs.


# StreamedContent / ByteArrayContent migration

The module now also contains:

```text
com.jackson.mta.primefaces.PrimeFaces62StreamedContentMigration
```

It migrates PrimeFaces 6.2 `ByteArrayContent` constructor patterns to
`DefaultStreamedContent.builder()`.

Example:

```java
return new ByteArrayContent(
        bytes,
        "application/pdf",
        "report.pdf");
```

becomes:

```java
return DefaultStreamedContent.builder()
        .stream(() -> new ByteArrayInputStream(bytes))
        .contentType("application/pdf")
        .name("report.pdf")
        .build();
```

The recipe also preserves `contentEncoding` and converts nullable legacy
`Integer contentLength` values to the modern `Long` builder value.

Run a dry run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62StreamedContentMigration \
  -Drewrite.exportDatatables=true
```

Apply:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:run \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62StreamedContentMigration \
  -Drewrite.exportDatatables=true
```

Then inspect:

```bash
git diff
grep -RIn "ByteArrayContent\|new DefaultStreamedContent" src
```

## Deliberately left for manual review

The recipe does not automatically change:

```java
new ByteArrayContent()
```

because no-arg construction is often followed by setter-based mutation.

It also does not globally transform arbitrary:

```java
new DefaultStreamedContent(inputStream, ...)
```

to the lazy builder form. Moving arbitrary stream creation into a supplier
can change when the stream is opened, resource lifetime, and checked-exception
behavior. MTA should flag these usages so they can be migrated with application
context.


# LazyDataModel migration

Two recipes are now available:

```text
com.jackson.mta.primefaces.PrimeFaces62LazyDataModelMigration
com.jackson.mta.primefaces.PrimeFaces62LazyDataModelCountBridge
```

## Why the load migration uses compatibility locals

PrimeFaces 6.2 commonly overrides:

```java
List<T> load(
    int first,
    int pageSize,
    String sortField,
    SortOrder sortOrder,
    Map<String, Object> filters)
```

Modern PrimeFaces uses:

```java
List<T> load(
    int first,
    int pageSize,
    Map<String, SortMeta> sortBy,
    Map<String, FilterMeta> filterBy)
```

The recipe changes the override signature but injects local variables that
reconstruct the old view:

```java
SortMeta __mtaPrimarySort =
        __mtaSortBy == null || __mtaSortBy.isEmpty()
        ? null
        : __mtaSortBy.values().iterator().next();

String sortField =
        __mtaPrimarySort == null ? null : __mtaPrimarySort.getField();

SortOrder sortOrder =
        __mtaPrimarySort == null
        ? SortOrder.UNSORTED
        : __mtaPrimarySort.getOrder();

Map<String, Object> filters =
        __mtaFilterBy == null
        ? Collections.emptyMap()
        : __mtaFilterBy.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().getFilterValue()));
```

This allows existing DAO/SQL/Criteria code to continue using the old variables
while the application compiles against the newer PrimeFaces API.

For the old multi-sort overload, the recipe converts the new sort map back into
a `List<SortMeta>` before executing the existing body.

## Run the load migration

Dry run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62LazyDataModelMigration \
  -Drewrite.exportDatatables=true
```

Apply:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:run \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62LazyDataModelMigration \
  -Drewrite.exportDatatables=true
```

Then compile and inspect all LazyDataModel implementations:

```bash
mvn clean compile

grep -RIn \
  -e "extends LazyDataModel" \
  -e "List<.*> load(" \
  -e "setRowCount" \
  -e "int count(" \
  src
```

## count(Map<String, FilterMeta>)

Modern PrimeFaces has a separate `count(filterBy)` contract.

Do not automatically convert every application to:

```java
@Override
public int count(Map<String, FilterMeta> filterBy) {
    return 0;
}
```

That value is appropriate only when the application's `load()` implementation
retrieves/manages the count itself and calls `setRowCount()`.

For that case, a separate conservative recipe is provided:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62LazyDataModelCountBridge
```

Run the count bridge only after reviewing the load transformation.

For applications that already have a dedicated DAO/repository count query,
implement `count(filterBy)` using that query instead of returning zero.


# Programmatic menu migration

The project now includes:

```text
com.jackson.mta.primefaces.PrimeFaces62MenuMigration
```

It applies the high-confidence PrimeFaces 6.2 -> modern menu migrations:

```text
DynamicMenuModel
    -> BaseMenuModel

model.addElement(item)
    -> model.getElements().add(item)

submenu.addElement(item)
    -> submenu.getElements().add(item)

new DefaultMenuItem(value)
    -> DefaultMenuItem.builder().value(value).build()

new DefaultMenuItem(value, icon)
    -> DefaultMenuItem.builder().value(value).icon(icon).build()

new DefaultMenuItem(value, icon, url)
    -> DefaultMenuItem.builder().value(value).icon(icon).url(url).build()

new DefaultSubMenu(label)
    -> DefaultSubMenu.builder().label(label).build()

new DefaultSubMenu(label, icon)
    -> DefaultSubMenu.builder().label(label).icon(icon).build()
```

## Why DynamicMenuModel becomes BaseMenuModel

PrimeFaces' migration guide explicitly says `DynamicMenuModel` was removed and
to use `BaseMenuModel` instead. This preserves the intent of a menu that can
change after initialization.

`DefaultMenuModel` is optimized for a menu that is effectively static after it
has been built, so this recipe does not substitute it for DynamicMenuModel.

## Setter-based menu construction

This recipe intentionally does not rewrite:

```java
DefaultMenuItem item = new DefaultMenuItem();
item.setValue("Save");
item.setIcon("pi pi-save");
item.setCommand("#{bean.save}");
```

or the analogous `DefaultSubMenu` setter pattern.

Those APIs still exist in current PrimeFaces, and collapsing a sequence of
possibly conditional mutations into one builder expression requires data-flow
analysis. Leaving them alone is safer and still compatible with the target.

You can modernize such blocks later for style if desired.

## Run it

Build/install the recipe module first:

```bash
mvn test
mvn install
```

Dry run in the application:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62MenuMigration \
  -Drewrite.exportDatatables=true
```

Apply:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:run \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62MenuMigration \
  -Drewrite.exportDatatables=true
```

Then review:

```bash
git diff

grep -RIn \
  -e "DynamicMenuModel" \
  -e "\\.addElement(" \
  -e "new DefaultMenuItem(" \
  -e "new DefaultSubMenu(" \
  src
```


# Legacy chart migration

Chart migration is intentionally staged because PrimeFaces 16 no longer uses
the old jqPlot Java model or the interim PrimeFaces-owned Chart.js Java model.

Available recipes:

```text
com.jackson.mta.primefaces.PrimeFacesLegacyChartInventory
com.jackson.mta.primefaces.PrimeFaces16ChartScaffold
com.jackson.mta.primefaces.PrimeFaces16ChartXhtmlFinalize
```

Recommended sequence:

1. Run `PrimeFacesLegacyChartInventory` as a dry run.
2. Run `PrimeFaces16ChartScaffold` to add XDEV Chart.js Java Model 3.1.0 and
   annotate chart views.
3. Rewrite each backing chart bean to return a Chart.js JSON `String`
   (typically with XDEV and `.toJson()`).
4. Run `PrimeFaces16ChartXhtmlFinalize` to rename `model=` to `value=` and
   remove legacy `type=` / `responsive=` attributes.
5. Compile and visually test every chart.

See `CHART-MIGRATION.md` for examples and migration notes.


# Calendar -> DatePicker migration

The project now includes staged PrimeFaces Calendar migration recipes:

```text
com.jackson.mta.primefaces.PrimeFaces62CalendarInventory
com.jackson.mta.primefaces.PrimeFaces62CalendarToDatePicker
com.jackson.mta.primefaces.PrimeFaces62CalendarCleanupAfterReview
```

The main recipe safely handles the `p:calendar` -> `p:datePicker` tag migration
plus `mode`, `pages`, `navigator`, `showOn`, `showButtonPanel`, and
`defaultMillisec` mappings. It adds review comments rather than guessing for
legacy time patterns, callbacks, animations, and time-control options.

See `CALENDAR-MIGRATION.md` before running the cleanup recipe.


# Schedule migration

The project now includes PrimeFaces 6.2 Schedule modernization recipes:

```text
com.jackson.mta.primefaces.PrimeFaces62ScheduleInventory
com.jackson.mta.primefaces.PrimeFaces62ScheduleJavaMigration
com.jackson.mta.primefaces.PrimeFaces62ScheduleXhtmlMigration
com.jackson.mta.primefaces.PrimeFaces62ScheduleMigration
```

They migrate Date-based `DefaultScheduleEvent` construction to the builder API,
bridge `LazyScheduleModel.loadEvents(Date, Date)` to `LocalDateTime`, and
normalize legacy FullCalendar view names.

The Java recipe deliberately uses `ZoneId.systemDefault()` as a temporary
compatibility bridge. Review it against the application's schedule `timeZone`,
browser timezone, DST requirements, and business timezone before rollout.

See `SCHEDULE-MIGRATION.md`.


# Build prerequisites and recipe dependencies

This custom recipe project targets JDK 21 and uses the OpenRewrite recipe BOM.
It also depends on:

```text
org.openrewrite:rewrite-java
org.openrewrite:rewrite-xml
org.openrewrite:rewrite-java-21
org.openrewrite.recipe:rewrite-java-dependencies
org.openrewrite:rewrite-test          (test)
org.primefaces:primefaces:6.2         (test baseline)
javax.faces:javax.faces-api:2.2       (test baseline)
```

`rewrite-java-dependencies` is required by the chart scaffold's
`org.openrewrite.java.dependencies.AddDependency` declarative recipe.

Current OpenRewrite documentation may require Code Genome repository
authentication to resolve some OpenRewrite artifacts. Keep credentials in the
user's Maven `settings.xml`; never commit tokens into this project.

Before publishing or using the recipe JAR, run:

```bash
mvn clean test
mvn install
```


# Recipe implementation policy

Every public/callable recipe in this project is implemented as a concrete Java
class under `src/main/java/com/jackson/mta/primefaces/`.

There are no public YAML-only recipes under `META-INF/rewrite`. Composite
recipes implement `getRecipeList()` directly in Java.


# Timeline migration

The project now includes PrimeFaces 6.2 Timeline modernization:

```text
MigrateTimelineEventConstruction.java
META-INF/rewrite/timeline.yml
TIMELINE-MIGRATION.md
```

The Java recipe migrates Date-based TimelineEvent constructors/setters to
builder-based LocalDateTime APIs. The Timeline XHTML recipe maps
`axisOnTop`, `groupsChangeable`, `timeChangeable`, and static `snapEvents`
settings while flagging removed or timezone-sensitive options for review.

See `TIMELINE-MIGRATION.md`.


# InputSwitch -> ToggleSwitch migration

The project includes concrete Java recipes for the PrimeFaces 6.2
`p:inputSwitch` -> PrimeFaces 16 `p:toggleSwitch` migration:

```text
PrimeFaces62InputSwitchInventory
PrimeFaces62InputSwitchToToggleSwitch
PrimeFaces62InputSwitchCleanupAfterReview
PrimeFaces62InputSwitchMigration
```

The main migration safely converts simple usages and programmatic component
types. Legacy `onLabel`, `offLabel`, and visible `showLabels` cases are marked
for manual UX review rather than silently discarded.

See `INPUTSWITCH-MIGRATION.md`.


# p:repeat -> ui:repeat migration

The project now includes concrete Java recipes for PrimeFaces' removed
`p:repeat` component:

```text
PrimeFaces62RepeatInventory
PrimeFaces62RepeatToUiRepeat
PrimeFaces62RepeatJavaUsageInventory
PrimeFaces62RepeatMigration
```

XHTML can be migrated directly because the standard Faces `ui:repeat` supports
the relevant PrimeFaces repeat iteration attributes. Java code bound directly
to `org.primefaces.component.repeat.UIRepeat` is inventoried rather than mapped
to a Mojarra-internal implementation class.

See `REPEAT-MIGRATION.md`.


# DataTable filtering migration

The project includes concrete Java recipes for PrimeFaces 6.2 DataTable
filtering changes:

```text
PrimeFaces62DataTableInventory
PrimeFaces62DataTableFilterApiMigration
PrimeFaces62DataTableFilterOptionsReview
PrimeFaces62DataTableMigration
```

The Java recipe migrates the removed `getFilters` / `setFilters` method names.
The XHTML recipe marks removed `filterOptions` usages for conversion to a
custom `filter` facet rather than silently deleting filter behavior.

See `DATATABLE-MIGRATION.md`.


# Completed PrimeFaces 6.2 -> 16 migration families

The modernization pack now includes all major recipe families selected for the
project scope, including FileUpload, JSON package migration, autoUpdate,
OverlayPanel appendToBody, rowsPerPageTemplate, InputMask, Push, legacy
Facelet/Watermark APIs, and remaining StreamedContent edge cases.

See:

```text
FILEUPLOAD-MIGRATION.md
JSON-MIGRATION.md
AUTOUPDATE-MIGRATION.md
OVERLAYPANEL-MIGRATION.md
ROWSPERPAGE-MIGRATION.md
INPUTMASK-MIGRATION.md
PUSH-MIGRATION.md
FACELET-WATERMARK-MIGRATION.md
STREAMEDCONTENT-EDGE-MIGRATION.md
MIGRATION-COVERAGE.md
```
