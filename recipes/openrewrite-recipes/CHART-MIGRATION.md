# PrimeFaces 6.2 → 16 chart migration

## Why the old chart package fails

PrimeFaces 6.x used the jqPlot-backed Java package:

```java
org.primefaces.model.chart.*
```

PrimeFaces 13 removed the legacy jqPlot chart component. PrimeFaces 15 then removed the
deprecated PrimeFaces Chart.js Java model classes as well. PrimeFaces 16 `p:chart`
expects a **String containing Chart.js JSON**.

Therefore this is not a package-rename migration.

Do not change:

```java
org.primefaces.model.chart.*
```

to:

```java
org.primefaces.model.charts.*
```

as a final PrimeFaces 16 solution.

## Family workflow

The chart family should run in four stages:

1. `PrimeFacesLegacyChartInventory`
   - identify `org.primefaces.model.chart.*`;
   - identify intermediate `org.primefaces.model.charts.*`;
   - identify `<p:chart>` usages and legacy `type`, `model`, `responsive`, and `extender`
     behavior.

2. `MigrateLegacyChartJavaModels`
   - convert high-confidence legacy chart fields/getters to `String`;
   - replace isolated chart factory methods with valid Chart.js JSON scaffolds;
   - remove obsolete chart imports when no longer referenced;
   - intentionally leave mixed business/chart logic unchanged for assisted review.

3. `PrimeFaces16ChartScaffold`
   - optionally add `software.xdev:chartjs-java-model` for developers who want a
     typed Java builder;
   - annotate remaining chart views for migration review;
   - developers replace the empty JSON scaffold with real datasets/options.
   - if XDEV is used, the backing bean property must still be `String`, populated
     with `chart.toJson()`.

4. `PrimeFaces16ChartXhtmlFinalize`
   - run only after chart review is complete;
   - rename `model=` to `value=`;
   - remove legacy `type=` / `responsive=` attributes after those settings have
     been moved into JSON;
   - retain supported attributes such as `widgetVar`, `style`, `styleClass`, and
     `extender` when still applicable.

## Generated scaffold

Example:

```java
private String model;

void createModel() {
    /* MTA PF16 chart migration scaffold: rebuild labels, datasets, axes, legend,
       styling, plugins and extender behavior before completing review. */
    model = "{\"type\":\"bar\",\"data\":{\"labels\":[],\"datasets\":[]}}";
}

String getModel() {
    return model;
}
```

The scaffold is deliberately empty. The recipe does not guess business data.

## XDEV option

PrimeFaces' own showcase uses:

```xml
<dependency>
    <groupId>software.xdev</groupId>
    <artifactId>chartjs-java-model</artifactId>
    <version>3.1.0</version>
</dependency>
```

The important point is that `p:chart` still receives a JSON `String`:

```java
chartModel = new BarChart(
        new BarData()
                .addLabels("Jan", "Feb")
                .addDataset(
                        new BarDataset()
                                .setLabel("Sales")
                                .addData(10)
                                .addData(20)))
        .toJson();
```

Do not expose `BarChart` itself as the value of `p:chart`.

## Unsupported/semantic-review cases

Treat these as manual/assisted:

- `OhlcChartModel`;
- `MeterGaugeChartModel`;
- mixed chart + business logic in one method;
- jqPlot extenders that depend on jqPlot-specific JavaScript;
- multiple axes;
- custom tick formatters;
- custom jqPlot renderers/plugins;
- code that mutates a chart model outside its dedicated factory.

The migration must not be marked complete while either of these remain:

```text
org.primefaces.model.chart.
org.primefaces.model.charts.
```

## Required ordering

Do not compile against PrimeFaces 16 immediately after upgrading the dependency while
legacy chart Java code still exists.

Preferred sequence:

```text
PF6.2 source
  -> inventory
  -> chart Java scaffold
  -> assisted chart review
  -> XHTML finalize
  -> residual check
  -> PrimeFaces 16 dependency baseline
  -> compile/test
```

If the Workbench keeps `primefaces-baseline` earlier for other reasons, phase validation
must defer compilation until the chart family and other removed-API families have run.
