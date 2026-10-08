# Patch Notes v10

This revision aligns verification with the Workbench automation contract and removes the final validation false positive observed after a successful JSF 2 / PrimeFaces 6.2 migration.

## Changes

- `primefaces-input-mask-review-0001` no longer matches every `p:inputMask` tag.
  - Static non-empty masks no longer generate an MTA finding.
  - Dynamic mask expressions and `validateMask="false"` remain review findings.
- `input-mask` is now `always: true` so the OpenRewrite XML review recipe still detects missing/empty masks even when MTA does not emit a finding.
- `PrimeFaces62InputMaskReview` now annotates dynamic mask expressions for developer review.
- `primefaces-schedule-0001` and `primefaces-timeline-0001` are now `potential` review findings instead of raw MTA `mandatory` findings.
- Added `FindingResidualClassifier`.
  - MTA findings are mapped back to the owning migration family.
  - `AUTOMATIC` family residuals are blocking.
  - `ASSISTED` and `MANUAL` family residuals are review-only regardless of raw MTA category.
  - Unmapped MTA findings are conservative review items rather than automatic failures.
- Verification now merges MTA residuals with forbidden-pattern residuals before determining final status.
- Report wording now says `Remaining MTA findings` instead of implying all remaining findings are non-blocking.
- Added regression tests for InputMask behavior, catalog semantics, and MTA residual classification.
