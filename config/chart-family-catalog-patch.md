# Chart family catalog patch

Use the package prefix that matches the current project (`com.jackson.mta` below).

Recommended chart family:

```yaml
- id: legacy-charts
  phase: PRIMEFACES_COMPONENTS
  mode: ASSISTED
  description: >
    Replace removed PrimeFaces jqPlot chart Java models with PrimeFaces 16
    Chart.js JSON strings, review chart semantics, then finalize p:chart markup.
  automation:
    - com.jackson.mta.primefaces.MigrateLegacyChartJavaModels
    - com.jackson.mta.primefaces.PrimeFaces16ChartScaffold
  postReviewAutomation:
    - com.jackson.mta.primefaces.PrimeFaces16ChartXhtmlFinalize
  forbiddenResidualPatterns:
    - org.primefaces.model.chart.
    - org.primefaces.model.charts.
  developerVerification:
    - Rebuild each empty JSON scaffold with the original labels and data series.
    - Recreate axis limits, titles, legends and orientation in Chart.js options.
    - Review jqPlot extender JavaScript for Chart.js compatibility.
    - Verify every p:chart backing property evaluates to java.lang.String.
    - Run the residual scan before upgrading/validating against PrimeFaces 16.
```

Sequencing requirement:

```text
legacy-charts must complete before the first compile gate that uses PrimeFaces 16.
```

Either move `primefaces-baseline` after PrimeFaces removed-API families or explicitly
defer compilation across the PrimeFaces migration phases.
