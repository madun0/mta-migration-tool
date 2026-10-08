# Patch Notes v11

## Schedule and Timeline post-migration rule precision

The custom MTA Schedule and Timeline rules previously matched the mere presence of
`DefaultScheduleEvent`, `LazyScheduleModel`, `TimelineEvent`, or `TimelineModel`.
Those types can legitimately remain after a successful PrimeFaces 16 migration,
which caused modern builder/`LocalDateTime` code to be reported as review findings.

### Changes

- `primefaces-schedule-0001` continues to flag `<p:schedule>` for assisted XHTML review.
- Java Schedule detection is now limited to legacy multi-argument
  `new DefaultScheduleEvent(...)` construction and the old
  `loadEvents(Date, Date)` signature.
- `primefaces-timeline-0001` continues to flag `<p:timeline>` for assisted XHTML review.
- Java Timeline detection is now limited to legacy multi-argument
  `new TimelineEvent(...)` construction.
- Modern `DefaultScheduleEvent.builder()`, `TimelineEvent.builder()`,
  `ScheduleModel`, and `TimelineModel` usage no longer creates findings simply
  because the target-side types are present.
- `CustomRuleSemanticsTest` protects these rules from regressing to broad type-name matches.

Expected validation result for the comprehensive sample after re-assessment: two
chart review findings, two Schedule XHTML review findings, and two Timeline XHTML
review findings; all are non-blocking assisted/manual review items.
