# Migration Catalog

The Workbench maps normalized MTA findings into developer-facing migration families. A family may match exact MTA rule IDs or curated rule-ID prefixes. Catalog order is significant: each finding is assigned to the **first matching family**, which consolidates overlapping built-in and custom MTA evidence into one migration task.

## Profiles

### `standard`

Full guided migration from JSF 2.2 / PrimeFaces 6.2 toward JBoss EAP 8 / Jakarta Faces 4 / PrimeFaces 16.

Phases: `FOUNDATION` → `PRIMEFACES_CORE` → `PRIMEFACES_DATA` → `PRIMEFACES_COMPONENTS` → `MANUAL_REVIEW` → `CLEANUP` → `VALIDATION`

### `primefaces-only`

Run only PrimeFaces modernization families after the Java/Jakarta foundation is already complete.

Phases: `PRIMEFACES_CORE` → `PRIMEFACES_DATA` → `PRIMEFACES_COMPONENTS` → `MANUAL_REVIEW` → `CLEANUP` → `VALIDATION`

### `assessment-only`

Assessment and planning only. No source transformations.

Phases: `FOUNDATION`

## Foundation families

The former catch-all `foundation` family has been split into focused tasks so junior developers see coherent work rather than raw MTA rule output.

| Family | Purpose |
|---|---|
| `platform-baseline` | Confirm Java 21 and EAP 8 / Jakarta EE 10 baseline. |
| `jakarta-dependencies` | Consolidate Maven `javax` → `jakarta` dependency findings. |
| `jakarta-imports` | Consolidate Java import namespace migration. |
| `jakarta-descriptors` | Consolidate deployment descriptor namespace/schema/version changes. |
| `faces-managed-beans` | Combine built-in EAP 8 Faces and custom Faces 4 managed-bean/scope/injection findings. |
| `faces-compatibility` | Remaining Jakarta Faces 4 compatibility work. |
| `jakarta-cdi` | CDI migration findings. |
| `eap8-ejb` | EJB compatibility findings. |
| `eap8-rest` | RESTEasy/Jakarta REST compatibility findings. |
| `eap8-xml-binding` | XML/JSON/SOAP binding compatibility findings. |
| `persistence-hibernate` | Hibernate/persistence migration findings. |
| `security-modernization` | Legacy EAP security/authentication migration findings. |
| `logging-modernization` | Removed/changed logging integration findings. |
| `jakarta-el` | Jakarta Expression Language findings. |

These foundation families are currently `MANUAL`; MTA supplies the evidence and the Workbench groups it into bounded tasks. Safe OpenRewrite automation can be added family-by-family without changing the planner contract.

## Rule matching

Each family supports:

- `mtaRuleIds`: exact rules owned by the family, especially Workbench custom rules.
- `mtaRulePrefixes`: curated built-in MTA rule families such as `javaee-to-jakarta-namespaces-`.

A normalized finding is assigned once. If several families could match the same rule, the first family in `migration-catalog.yaml` wins. Specific families therefore appear before broader prefix-based families.

For example, `eap8-faces-00003` maps to `faces-managed-beans` before the broader `faces-compatibility` prefix `eap8-faces-` is considered.

## Evidence in the generated plan

Each plan task records:

- total normalized finding count;
- distinct contributing MTA rule IDs;
- distinct affected files;
- automation level;
- recipes and cleanup recipes;
- migration guide;
- residual forbidden patterns.

This makes the plan the primary developer-facing abstraction while preserving traceability back to MTA.

## Unmapped findings

For profiles containing `MANUAL_REVIEW`, findings that match no catalog family are collected into `unmapped-mta-review`. They are never silently discarded. The generated plan/TODO shows the contributing rule IDs and affected files so the migration architect can decide whether to add a family or leave the item manual.

## PrimeFaces families

The PrimeFaces phases also include `primefaces-baseline` for the 6.2 → 16 dependency baseline, followed by `request-context`, `file-upload`, `streamed-content`, `programmatic-menu`, `primefaces-json`, `lazy-data-model`, `datatable-filtering`, `rows-per-page`, `calendar`, `schedule`, `timeline`, `input-switch`, `repeat`, `input-mask`, `overlay-panel`, `auto-update`, `charts`, `push`, and `facelet-watermark`.

## Automation meanings

- **AUTOMATIC** — the Workbench can apply the transformation and compile-check it.
- **ASSISTED** — the Workbench applies safe transformations and adds the family to `.migration/TODO.md` for developer review.
- **MANUAL** — MTA identifies the work and the Workbench produces guidance, but no source rewrite is applied.

Post-review cleanup recipes are scheduled in the `CLEANUP` phase. The first migration pass stops at that gate when review items exist; `migrate resume` is the explicit acknowledgement to continue after the developer completes the TODO review.

## Execution metadata

Each family can now include two execution-oriented fields:

- `reviewChecklist`: developer verification steps rendered into `MIGRATION-PLAN.md` and `TODO.md`.
- `compileAfterApply`: whether the Workbench should invoke the configured compile gate immediately after the recipe. The default is `true`; set it to `false` for transitional transformations that are not expected to compile until a later family or assisted review is complete.

Cleanup tasks are represented as follow-up work rather than new assessment findings. In the generated plan they show `Triggered by: <family>` and do not repeat the original MTA finding count.
