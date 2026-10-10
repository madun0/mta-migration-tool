# Patch Notes v19

## Regression-test hardening

- Corrects the remaining `FoundationRecipeTransformationTest` expectation for the explicit Servlet API migration. OpenRewrite emits the Jakarta EE 10 platform API before the migrated Servlet API; Maven dependency semantics are unchanged by this ordering.
- Adds a semantic `assertJakartaServletDependency` assertion that verifies exactly one `jakarta.servlet:jakarta.servlet-api` dependency, version `6.0.0`, with `provided` scope.
- The existing Jakarta EE 10 platform assertion remains active in the same test.
- No production migration recipe behavior changed from v18.

This patch addresses the single test failure observed after v18 (`110` tests executed, `1` failure, `0` errors).
