package com.jackson.mta.primefaces;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/**
 * Verifies conservative migration of deleted PrimeFaces jqPlot chart models to
 * PrimeFaces 16 compatible JSON-string scaffolds.
 */
class MigrateLegacyChartJavaModelsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateLegacyChartJavaModels())
                .parser(
                        JavaParser.fromJavaVersion()
                                .classpath("primefaces"));
    }

    /**
     * Converts a dedicated bar-chart factory, field and getter without leaving
     * org.primefaces.model.chart references behind.
     */
    @Test
    void migratesDedicatedBarChartFactory() {
        rewriteRun(
                java(
                        """
                        import org.primefaces.model.chart.BarChartModel;
                        import org.primefaces.model.chart.ChartSeries;

                        class Example {
                            private BarChartModel model;

                            void createModel() {
                                model = new BarChartModel();

                                ChartSeries sales = new ChartSeries();
                                sales.setLabel("Sales");
                                sales.set("Jan", 10);
                                sales.set("Feb", 20);

                                model.addSeries(sales);
                                model.setTitle("Revenue");
                            }

                            BarChartModel getModel() {
                                return model;
                            }
                        }
                        """,
                        """
                        class Example {
                            private String model;

                            void createModel() {
                                /* MTA PF16 chart migration scaffold: rebuild labels, datasets, axes, legend, styling, plugins and extender behavior before completing review. */
                                model = "{\\"type\\":\\"bar\\",\\"data\\":{\\"labels\\":[],\\"datasets\\":[]}}";
                            }

                            String getModel() {
                                return model;
                            }
                        }
                        """
                )
        );
    }

    /**
     * Preserves the horizontal orientation in the generated Chart.js scaffold.
     */
    @Test
    void migratesHorizontalBarChartFactory() {
        rewriteRun(
                java(
                        """
                        import org.primefaces.model.chart.HorizontalBarChartModel;

                        class Example {
                            private HorizontalBarChartModel model;

                            void createHorizontalModel() {
                                model = new HorizontalBarChartModel();
                                model.setTitle("Totals");
                            }

                            HorizontalBarChartModel getModel() {
                                return model;
                            }
                        }
                        """,
                        """
                        class Example {
                            private String model;

                            void createHorizontalModel() {
                                /* MTA PF16 chart migration scaffold: rebuild labels, datasets, axes, legend, styling, plugins and extender behavior before completing review. */
                                model = "{\\"type\\":\\"bar\\",\\"data\\":{\\"labels\\":[],\\"datasets\\":[]},\\"options\\":{\\"indexAxis\\":\\"y\\"}}";
                            }

                            String getModel() {
                                return model;
                            }
                        }
                        """
                )
        );
    }

    /**
     * Leaves a class unchanged when chart construction is mixed with unrelated
     * business logic; such code must be handled in the assisted review step.
     */
    @Test
    void leavesMixedBusinessLogicForManualReview() {
        rewriteRun(
                java(
                        """
                        import org.primefaces.model.chart.BarChartModel;

                        class Example {
                            private BarChartModel model;

                            void init() {
                                loadCustomerData();
                                model = new BarChartModel();
                                model.setTitle("Revenue");
                            }

                            void loadCustomerData() {
                            }
                        }
                        """
                )
        );
    }
}
