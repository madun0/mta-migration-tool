# Code Documentation Standard

The Migration Workbench is intended to be maintained by developers with different levels of
experience. Java documentation is therefore part of the source-code contract rather than an
optional cleanup task.

## Required Javadocs

Every named Java class, record, and interface must have class-level Javadoc that explains its
responsibility in the migration workflow.

For Workbench production code, every public method must document:

- what operation it performs;
- important side effects, such as writing `.migration` state or invoking an external tool;
- parameters whose purpose is not obvious from the type alone;
- return semantics;
- checked failure modes with `@throws` where applicable.

Public fields in serialized configuration/state/plan models must document the meaning of the data
because those fields form the JSON/YAML persistence contract.

Complex private helpers should receive documentation when they implement migration policy,
checkpoint safety, report normalization, or other behavior that is not obvious from the method
name alone.

## OpenRewrite recipe documentation

Each public OpenRewrite recipe must remain a concrete `org.openrewrite.Recipe` class and must have:

- class-level Javadoc explaining the migration purpose and safety boundary;
- a meaningful `getDisplayName()`;
- a meaningful `getDescription()`;
- a matching test class with class-level Javadoc.

For custom visitor recipes, comments should explain why particular legacy patterns are transformed
or deliberately left for manual review. Do not document an unsafe transformation as automatic.

## Package documentation

The Workbench production packages and the PrimeFaces recipe package contain `package-info.java`
files describing the package boundary. New top-level packages should add their own package
Javadoc.

## Documentation style

Prefer documentation that explains **why** a class or method exists and what migration guarantee it
provides. Avoid comments that simply repeat the Java identifier.

Good:

```java
/**
 * Creates a checkpoint before a migration family runs. The checkpoint records the baseline Git
 * HEAD and workflow state needed for a guarded rollback.
 */
```

Avoid:

```java
/** Creates a checkpoint. */
```

## Review checklist

When adding a class or recipe:

1. Add class-level Javadoc.
2. Document public methods and persisted public fields.
3. Document non-obvious safety decisions or side effects.
4. Add or update the matching test.
5. Update the migration family guide when behavior changes.
6. Run the full Maven build and Javadoc/source audit before publishing a Workbench release.
