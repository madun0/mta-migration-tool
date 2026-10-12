package com.jackson.mta.primefaces;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces high-confidence PrimeFaces 6.x jqPlot chart-model factories with
 * PrimeFaces 16 compatible JSON-string scaffolds.
 *
 * <p>PrimeFaces 16 no longer contains {@code org.primefaces.model.chart.*}.
 * The modern {@code p:chart} component consumes a JSON string containing a
 * Chart.js configuration. This recipe therefore converts legacy chart model
 * fields and getter return types to {@link String} and replaces dedicated
 * chart-construction methods with valid, intentionally empty Chart.js
 * configuration strings.</p>
 *
 * <p>This recipe is deliberately conservative. It only rewrites a class when
 * every method that constructs a legacy chart model can be identified as a
 * dedicated chart-construction method. If chart construction is mixed with
 * unrelated application logic, the class is left unchanged for assisted
 * migration instead of silently deleting business logic.</p>
 *
 * <p>The generated JSON is a compile-safe migration scaffold, not an attempt
 * to infer application-specific chart semantics. Labels, datasets, axes,
 * legends, colors, plugins, extenders, and other jqPlot options must be
 * reconstructed during the chart review step before the XHTML finalization
 * recipe is run.</p>
 */
public class MigrateLegacyChartJavaModels extends Recipe {

    private static final String LEGACY_PACKAGE = "org.primefaces.model.chart.";

    private static final Set<String> LEGACY_MODEL_TYPES = Set.of(
            "BarChartModel",
            "HorizontalBarChartModel",
            "LineChartModel",
            "PieChartModel",
            "DonutChartModel",
            "BubbleChartModel",
            "OhlcChartModel",
            "MeterGaugeChartModel"
    );

    private static final Set<String> LEGACY_SUPPORT_TYPES = Set.of(
            "Axis",
            "AxisType",
            "BarChartSeries",
            "BubbleChartSeries",
            "CartesianChartModel",
            "CategoryAxis",
            "ChartModel",
            "ChartSeries",
            "DateAxis",
            "LegendPlacement",
            "LineChartSeries",
            "LinearAxis",
            "OhlcChartSeries"
    );

    private static final Set<String> ALL_LEGACY_TYPES;

    private static final Pattern MODEL_CONSTRUCTION =
            Pattern.compile(
                    "(?:this\\.)?([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*new\\s+("
                            + String.join("|", LEGACY_MODEL_TYPES)
                            + ")\\s*\\(");

    private static final Pattern LOCAL_LEGACY_DECLARATION =
            Pattern.compile(
                    "\\b("
                            + String.join("|", union(LEGACY_MODEL_TYPES, LEGACY_SUPPORT_TYPES))
                            + ")\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b");

    static {
        Set<String> types = new LinkedHashSet<>();
        types.addAll(LEGACY_MODEL_TYPES);
        types.addAll(LEGACY_SUPPORT_TYPES);
        ALL_LEGACY_TYPES = Set.copyOf(types);
    }

    @Override
    public String getDisplayName() {
        return "Migrate legacy PrimeFaces chart Java models to JSON scaffolds";
    }

    @Override
    public String getDescription() {
        return "Removes high-confidence org.primefaces.model.chart usages by "
                + "converting chart model properties to String and replacing "
                + "dedicated jqPlot chart factories with PrimeFaces 16 compatible "
                + "Chart.js JSON scaffolds. Complex mixed-logic factories are "
                + "left unchanged for assisted review.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(20);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            private final JavaType.Class stringType =
                    JavaType.ShallowClass.build("java.lang.String");

            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration classDecl,
                    ExecutionContext ctx) {

                /*
                 * Decide before traversing children. Partial chart conversion is
                 * more dangerous than leaving the class for assisted review.
                 */
                if (containsLegacyChartUsage(classDecl)
                        && !isSafeToScaffold(classDecl)) {
                    return classDecl;
                }

                return super.visitClassDeclaration(classDecl, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method,
                    ExecutionContext ctx) {

                J.MethodDeclaration original = method;

                if (method.getBody() != null
                        && containsLegacyModelConstruction(original)) {
                    method = replaceChartFactoryBody(method);
                }

                method = super.visitMethodDeclaration(method, ctx);

                if (isLegacyModelType(method.getReturnTypeExpression())) {
                    method = changeMethodReturnTypeToString(method);
                }

                return method;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(
                    J.VariableDeclarations declarations,
                    ExecutionContext ctx) {

                J.VariableDeclarations variables =
                        super.visitVariableDeclarations(declarations, ctx);

                if (!isLegacyModelType(variables.getTypeExpression())) {
                    return variables;
                }

                TypeTree oldType = variables.getTypeExpression();

                TypeTree newType =
                        TypeTree.build("String")
                                .withType(stringType)
                                .withPrefix(
                                        oldType == null
                                                ? variables.getPrefix()
                                                : oldType.getPrefix());

                List<J.VariableDeclarations.NamedVariable> migratedVariables =
                        new ArrayList<>(variables.getVariables().size());

                for (J.VariableDeclarations.NamedVariable variable
                        : variables.getVariables()) {

                    J.VariableDeclarations.NamedVariable migrated = variable;

                    JavaType.Variable variableType =
                            variable.getVariableType();

                    if (variableType != null) {
                        variableType =
                                variableType.withType(stringType);

                        migrated =
                                migrated.withVariableType(variableType)
                                        .withName(
                                                migrated.getName()
                                                        .withType(stringType)
                                                        .withFieldType(variableType));
                    } else {
                        migrated =
                                migrated.withName(
                                        migrated.getName()
                                                .withType(stringType));
                    }

                    migrated = migrated.withType(stringType);

                    /*
                     * A legacy model field may have been initialized directly,
                     * e.g. "private BarChartModel model = new BarChartModel();".
                     * Replace only that initializer with the appropriate JSON
                     * scaffold.
                     */
                    Expression initializer = migrated.getInitializer();

                    if (initializer instanceof J.NewClass) {
                        String constructedType =
                                simpleTypeName(
                                        ((J.NewClass) initializer).getClazz());

                        if (LEGACY_MODEL_TYPES.contains(constructedType)) {
                            JavaTemplate template =
                                    JavaTemplate.builder(
                                                    javaStringLiteral(
                                                            scaffoldJson(
                                                                    constructedType)))
                                            .build();

                            Expression replacement =
                                    template.apply(
                                            new Cursor(getCursor(), initializer),
                                            initializer.getCoordinates().replace());

                            migrated =
                                    migrated.withInitializer(replacement);
                        }
                    }

                    migratedVariables.add(migrated);
                }

                return variables
                        .withType(stringType)
                        .withTypeExpression(newType)
                        .withVariables(migratedVariables);
            }

            /**
             * Retypes references to legacy chart fields after their declarations
             * have been migrated to {@link String}. JavaTemplate-generated
             * assignments can otherwise contain identifiers with missing type
             * attribution because the enclosing cursor still reflects the
             * pre-migration field declaration.
             */
            @Override
            public J.Identifier visitIdentifier(
                    J.Identifier identifier,
                    ExecutionContext ctx) {

                J.Identifier migrated =
                        super.visitIdentifier(identifier, ctx);

                Map<String, String> chartFields =
                        legacyChartFields();

                if (!chartFields.containsKey(migrated.getSimpleName())) {
                    return migrated;
                }

                JavaType.Variable fieldType =
                        migrated.getFieldType();

                if (fieldType != null) {
                    JavaType.Variable migratedFieldType =
                            fieldType.withType(stringType);

                    return migrated
                            .withType(stringType)
                            .withFieldType(migratedFieldType);
                }

                return migrated.withType(stringType);
            }

            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit compilationUnit,
                    ExecutionContext ctx) {

                J.CompilationUnit cu =
                        super.visitCompilationUnit(compilationUnit, ctx);

                /*
                 * Remove legacy imports only when their simple names no longer
                 * appear in class source. This avoids making an intentionally
                 * deferred/manual class harder to understand.
                 */
                String classSource =
                        cu.getClasses().stream()
                                .map(type -> type.printTrimmed(
                                        new Cursor(getCursor(), type)))
                                .reduce("", (left, right) -> left + "\n" + right);

                /*
                 * Remove legacy chart imports directly from the transformed
                 * compilation unit. maybeRemoveImport() schedules a follow-up
                 * import cleanup and can miss imports whose type attribution
                 * was rewritten earlier in the same visitor cycle.
                 */
                List<J.Import> retainedImports = new ArrayList<>();
                boolean importsChanged = false;

                for (J.Import importDecl : cu.getImports()) {
                    String importedType = importDecl.getTypeName();

                    if (importedType == null
                            || !importedType.startsWith(LEGACY_PACKAGE)) {
                        retainedImports.add(importDecl);
                        continue;
                    }

                    String simpleImportedType =
                            importedType.substring(LEGACY_PACKAGE.length());

                    if ("*".equals(simpleImportedType)) {
                        boolean anyLegacyTypeStillUsed =
                                ALL_LEGACY_TYPES.stream()
                                        .anyMatch(type ->
                                                containsWord(classSource, type));

                        if (anyLegacyTypeStillUsed) {
                            retainedImports.add(importDecl);
                        } else {
                            importsChanged = true;
                        }

                        continue;
                    }

                    if (containsWord(classSource, simpleImportedType)) {
                        retainedImports.add(importDecl);
                    } else {
                        importsChanged = true;
                    }
                }

                J.CompilationUnit cleaned =
                        importsChanged
                                ? cu.withImports(retainedImports)
                                : cu;

                /*
                 * When the last import is removed, its trailing whitespace may
                 * remain attached to the first top-level type. Normalize only
                 * when whitespace is actually present so the visitor is
                 * idempotent and stabilizes in one OpenRewrite cycle.
                 */
                if (cleaned.getImports().isEmpty()
                        && !cleaned.getClasses().isEmpty()) {
                    J.ClassDeclaration firstClass =
                            cleaned.getClasses().get(0);

                    if (!firstClass.getPrefix()
                            .getWhitespace()
                            .isEmpty()) {

                        List<J.ClassDeclaration> classes =
                                new ArrayList<>(cleaned.getClasses());

                        classes.set(
                                0,
                                firstClass.withPrefix(
                                        firstClass.getPrefix()
                                                .withWhitespace("")));

                        cleaned = cleaned.withClasses(classes);
                    }
                }

                return cleaned;
            }

            /**
             * Replaces a dedicated chart-construction method body with JSON
             * scaffold assignments or a JSON return value.
             */
            private J.MethodDeclaration replaceChartFactoryBody(
                    J.MethodDeclaration method) {

                J.Block body = method.getBody();

                if (body == null) {
                    return method;
                }

                Map<String, String> chartFields =
                        legacyChartFields();

                Map<String, String> assignments =
                        findFieldAssignments(
                                methodSource(method),
                                chartFields);

                String returnType =
                        simpleTypeName(
                                method.getReturnTypeExpression());

                StringBuilder replacement =
                        new StringBuilder()
                                .append("/* MTA PF16 chart migration scaffold: ")
                                .append("rebuild labels, datasets, axes, legend, styling, ")
                                .append("plugins and extender behavior before completing review. */\n");

                if (LEGACY_MODEL_TYPES.contains(returnType)) {
                    replacement.append("return ")
                            .append(
                                    javaStringLiteral(
                                            scaffoldJson(returnType)))
                            .append(";\n");
                } else {
                    for (Map.Entry<String, String> assignment
                            : assignments.entrySet()) {
                        replacement.append(assignment.getKey())
                                .append(" = ")
                                .append(
                                        javaStringLiteral(
                                                scaffoldJson(
                                                        assignment.getValue())))
                                .append(";\n");
                    }
                }

                JavaTemplate template =
                        JavaTemplate.builder(replacement.toString())
                                .contextSensitive()
                                .build();

                J.MethodDeclaration migratedBody =
                        template.apply(
                                updateCursor(method),
                                method.getCoordinates().replaceBody());

                return migratedBody;
            }

            /**
             * Changes a getter/factory return type from a deleted legacy chart
             * model to String while keeping method type attribution consistent.
             */
            private J.MethodDeclaration changeMethodReturnTypeToString(
                    J.MethodDeclaration method) {

                TypeTree oldReturn =
                        method.getReturnTypeExpression();

                if (oldReturn == null) {
                    return method;
                }

                J.MethodDeclaration migrated =
                        method.withReturnTypeExpression(
                                TypeTree.build("String")
                                        .withType(stringType)
                                        .withPrefix(oldReturn.getPrefix()));

                JavaType.Method methodType =
                        migrated.getMethodType();

                if (methodType != null) {
                    methodType =
                            methodType.withReturnType(stringType);

                    migrated =
                            migrated.withMethodType(methodType)
                                    .withName(
                                            migrated.getName()
                                                    .withType(methodType));
                }

                return migrated;
            }

            /**
             * Returns legacy model fields declared directly on the enclosing
             * class and their original model types.
             */
            private Map<String, String> legacyChartFields() {
                Map<String, String> result =
                        new LinkedHashMap<>();

                J.ClassDeclaration enclosing =
                        getCursor().firstEnclosing(
                                J.ClassDeclaration.class);

                if (enclosing == null) {
                    return result;
                }

                for (Statement statement
                        : enclosing.getBody().getStatements()) {

                    if (!(statement instanceof J.VariableDeclarations)) {
                        continue;
                    }

                    J.VariableDeclarations declarations =
                            (J.VariableDeclarations) statement;

                    String type =
                            simpleTypeName(
                                    declarations.getTypeExpression());

                    if (!LEGACY_MODEL_TYPES.contains(type)) {
                        continue;
                    }

                    for (J.VariableDeclarations.NamedVariable variable
                            : declarations.getVariables()) {
                        result.put(variable.getSimpleName(), type);
                    }
                }

                return result;
            }

            /**
             * Extracts assignments to known legacy chart fields from a chart
             * factory method.
             */
            private Map<String, String> findFieldAssignments(
                    String source,
                    Map<String, String> chartFields) {

                Map<String, String> result =
                        new LinkedHashMap<>();

                Matcher matcher =
                        MODEL_CONSTRUCTION.matcher(source);

                while (matcher.find()) {
                    String variable = matcher.group(1);
                    String type = matcher.group(2);

                    if (chartFields.containsKey(variable)) {
                        result.put(variable, type);
                    }
                }

                return result;
            }

            /**
             * Checks whether every method that constructs a legacy chart model
             * is sufficiently isolated to replace without discarding unrelated
             * business logic.
             */
            private boolean isSafeToScaffold(
                    J.ClassDeclaration classDecl) {

                Set<String> chartFields =
                        new LinkedHashSet<>();

                for (Statement statement
                        : classDecl.getBody().getStatements()) {
                    if (statement instanceof J.VariableDeclarations) {
                        J.VariableDeclarations declarations =
                                (J.VariableDeclarations) statement;

                        if (isLegacyModelType(
                                declarations.getTypeExpression())) {
                            for (J.VariableDeclarations.NamedVariable variable
                                    : declarations.getVariables()) {
                                chartFields.add(
                                        variable.getSimpleName());
                            }
                        }
                    }
                }

                for (Statement statement
                        : classDecl.getBody().getStatements()) {

                    if (!(statement instanceof J.MethodDeclaration)) {
                        continue;
                    }

                    J.MethodDeclaration method =
                            (J.MethodDeclaration) statement;

                    if (!containsLegacyModelConstruction(method)) {
                        continue;
                    }

                    /*
                     * Constructors are not rewritten because dropping unrelated
                     * constructor work would be unsafe.
                     */
                    if (method.isConstructor()) {
                        return false;
                    }

                    if (!isDedicatedChartMethod(
                            method,
                            chartFields)) {
                        return false;
                    }
                }

                return true;
            }

            /**
             * Determines whether a method consists only of legacy chart
             * construction/configuration statements.
             */
            private boolean isDedicatedChartMethod(
                    J.MethodDeclaration method,
                    Set<String> chartFields) {

                J.Block body = method.getBody();

                if (body == null) {
                    return false;
                }

                String source =
                        methodSource(method);

                Set<String> chartVariables =
                        new LinkedHashSet<>(chartFields);

                Matcher declarations =
                        LOCAL_LEGACY_DECLARATION.matcher(source);

                while (declarations.find()) {
                    chartVariables.add(
                            declarations.group(2));
                }

                for (Statement statement
                        : body.getStatements()) {

                    String statementSource =
                            statement.printTrimmed(
                                    new Cursor(
                                            new Cursor(
                                                    getCursor(),
                                                    body),
                                            statement));

                    if (statementSource.isBlank()) {
                        continue;
                    }

                    boolean chartRelated =
                            ALL_LEGACY_TYPES.stream()
                                    .anyMatch(type ->
                                            containsWord(
                                                    statementSource,
                                                    type));

                    if (!chartRelated) {
                        chartRelated =
                                chartVariables.stream()
                                        .anyMatch(variable ->
                                                containsWord(
                                                        statementSource,
                                                        variable));
                    }

                    /*
                     * Return statements from a legacy-model factory are safe
                     * when they return a tracked chart variable.
                     */
                    if (!chartRelated
                            && statement instanceof J.Return) {
                        chartRelated =
                                chartVariables.stream()
                                        .anyMatch(variable ->
                                                containsWord(
                                                        statementSource,
                                                        variable));
                    }

                    if (!chartRelated) {
                        return false;
                    }
                }

                return true;
            }

            private boolean containsLegacyChartUsage(
                    J.ClassDeclaration classDecl) {

                String source =
                        classDecl.printTrimmed(
                                getCursor());

                return source.contains(
                        "org.primefaces.model.chart")
                        || ALL_LEGACY_TYPES.stream()
                                .anyMatch(type ->
                                        containsWord(
                                                source,
                                                type));
            }

            private boolean containsLegacyModelConstruction(
                    J.MethodDeclaration method) {

                return MODEL_CONSTRUCTION
                        .matcher(methodSource(method))
                        .find()
                        || LEGACY_MODEL_TYPES.stream()
                                .anyMatch(type ->
                                        methodSource(method)
                                                .contains(
                                                        "new "
                                                                + type
                                                                + "("));
            }

            private String methodSource(
                    J.MethodDeclaration method) {

                return method.printTrimmed(
                        getCursor());
            }

            private boolean isLegacyModelType(
                    TypeTree typeTree) {

                return LEGACY_MODEL_TYPES.contains(
                        simpleTypeName(typeTree));
            }
        };
    }

    /**
     * Maps the deleted jqPlot model type to a minimal valid Chart.js JSON
     * configuration. The data is intentionally empty because inferring business
     * semantics from arbitrary legacy chart-building code is unsafe.
     */
    private static String scaffoldJson(
            String legacyModelType) {

        switch (legacyModelType) {
            case "BarChartModel":
                return "{\"type\":\"bar\",\"data\":{\"labels\":[],\"datasets\":[]}}";
            case "HorizontalBarChartModel":
                return "{\"type\":\"bar\",\"data\":{\"labels\":[],\"datasets\":[]},"
                        + "\"options\":{\"indexAxis\":\"y\"}}";
            case "LineChartModel":
                return "{\"type\":\"line\",\"data\":{\"labels\":[],\"datasets\":[]}}";
            case "PieChartModel":
                return "{\"type\":\"pie\",\"data\":{\"labels\":[],\"datasets\":[]}}";
            case "DonutChartModel":
                return "{\"type\":\"doughnut\",\"data\":{\"labels\":[],\"datasets\":[]}}";
            case "BubbleChartModel":
                return "{\"type\":\"bubble\",\"data\":{\"datasets\":[]}}";
            case "OhlcChartModel":
                return "{\"type\":\"line\",\"data\":{\"labels\":[],\"datasets\":[]}}";
            case "MeterGaugeChartModel":
                return "{\"type\":\"doughnut\",\"data\":{\"labels\":[],\"datasets\":[]}}";
            default:
                return "{\"type\":\"bar\",\"data\":{\"labels\":[],\"datasets\":[]}}";
        }
    }

    /**
     * Returns the simple type name for a source-level type expression.
     */
    private static String simpleTypeName(
            TypeTree typeTree) {

        if (typeTree == null) {
            return "";
        }

        String printed =
                typeTree.toString()
                        .trim();

        int generic =
                printed.indexOf('<');

        if (generic >= 0) {
            printed =
                    printed.substring(0, generic);
        }

        int dot =
                printed.lastIndexOf('.');

        return dot >= 0
                ? printed.substring(dot + 1)
                : printed;
    }

    /**
     * Produces a Java source string literal for generated JSON.
     */
    private static String javaStringLiteral(
            String value) {

        return "\""
                + value.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                + "\"";
    }

    /**
     * Performs a Java-identifier-style whole-word match.
     */
    private static boolean containsWord(
            String source,
            String word) {

        return Pattern.compile(
                        "(?<![A-Za-z0-9_$])"
                                + Pattern.quote(word)
                                + "(?![A-Za-z0-9_$])")
                .matcher(source)
                .find();
    }

    private static Set<String> union(
            Set<String> left,
            Set<String> right) {

        Set<String> result =
                new LinkedHashSet<>(left);

        result.addAll(right);

        return result;
    }
}
