# PrimeFaces 6.2 DataTable filtering -> PrimeFaces 16

PrimeFaces 8 removed:

```text
DataTable#getFilters()
DataTable#setFilters(...)
```

in favor of:

```text
DataTable#getFilterBy()
DataTable#setFilterBy(...)
```

PrimeFaces 13 later removed:

```text
UIColumn#filterOptions
```

in favor of a custom column filter facet.

This project provides four concrete Java recipes:

```text
com.jackson.mta.primefaces.PrimeFaces62DataTableInventory
com.jackson.mta.primefaces.PrimeFaces62DataTableFilterApiMigration
com.jackson.mta.primefaces.PrimeFaces62DataTableFilterOptionsReview
com.jackson.mta.primefaces.PrimeFaces62DataTableMigration
```

## 1. Inventory

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62DataTableInventory \
  -Drewrite.exportDatatables=true
```

This locates Java coupling to DataTable/UIColumn and all XHTML uses of:

```xhtml
filterOptions="..."
```

## 2. Java filter API

PrimeFaces 6.2 code may contain:

```java
Map<String, Object> filters = table.getFilters();
table.setFilters(filters);
```

The Java recipe renames the removed calls:

```java
table.getFilterBy();
table.setFilterBy(filters);
```

This is intentionally a method-name migration only.

Modern PrimeFaces filtering uses `FilterMeta` rather than treating each map
entry as the raw filter value. If surrounding application code does things
such as:

```java
Object value = filters.get("status");
```

that logic needs semantic migration to read the corresponding `FilterMeta`
value instead of simply renaming a variable type.

The existing LazyDataModel recipe in this project handles the larger
`Map<String, FilterMeta>` migration for lazy loading.

## 3. filterOptions

PrimeFaces 6.2 supported:

```xhtml
<p:column
    filterBy="#{car.manufacturer}"
    filterOptions="#{carBean.manufacturerOptions}"
    filterMatchMode="exact">
    ...
</p:column>
```

PrimeFaces 13 removed `filterOptions` in favor of a custom `filter` facet.

The target shape is application/UI dependent, for example:

```xhtml
<p:column
    filterBy="#{car.manufacturer}"
    filterMatchMode="exact">

    <f:facet name="filter">
        <p:selectOneMenu
            onchange="PF('carsTable').filter()">
            <f:selectItem
                itemLabel="All"
                itemValue="#{null}"
                noSelectionOption="true"/>
            <f:selectItems
                value="#{carBean.manufacturerOptions}"/>
        </p:selectOneMenu>
    </f:facet>

    ...
</p:column>
```

The recipe does **not** manufacture that structure automatically because it
would need to know:

- the enclosing table's `widgetVar`
- the desired input component
- whether the options already contain a no-selection entry
- converters for non-String values
- Ajax/onchange behavior
- accessibility and styling requirements

Instead, it adds an actionable migration comment to each `filterOptions`
column.

## 4. Why this is separate from LazyDataModel

There are two different filter migrations:

```text
DataTable component API
    getFilters/setFilters
        ->
    getFilterBy/setFilterBy

LazyDataModel backend API
    Map<String,Object>
        ->
    Map<String,FilterMeta>
```

The project already has the LazyDataModel migration. This recipe covers the
DataTable/UIColumn side.

## 5. Run

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62DataTableMigration
```

Then inspect:

```bash
grep -RIn \
  -e "getFilters()" \
  -e "setFilters(" \
  -e "filterOptions=" \
  -e "getFilterBy()" \
  -e "setFilterBy(" \
  src
```

After resolving filter facets:

```bash
mvn clean test
```

Browser testing should cover:

- global filtering
- per-column filtering
- exact/in/contains match modes
- select-based filters
- converters
- lazy filtering
- dynamic columns
- multi-view state
