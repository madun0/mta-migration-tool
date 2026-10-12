# PrimeFaces 6.2 Schedule -> PrimeFaces 16

PrimeFaces 8 upgraded Schedule to FullCalendar 4.x. That migration changed two
important Java contracts:

- `ScheduleEvent` moved from `java.util.Date` to `java.time.LocalDateTime`.
- `DefaultScheduleEvent` moved toward the fluent `builder()` API.
- `LazyScheduleModel.loadEvents(Date, Date)` became
  `loadEvents(LocalDateTime, LocalDateTime)`.

Current PrimeFaces still uses `LocalDateTime` for schedule event start/end
values and exposes builder-based event construction.

This project provides four recipes:

```text
com.jackson.mta.primefaces.PrimeFaces62ScheduleInventory
com.jackson.mta.primefaces.PrimeFaces62ScheduleJavaMigration
com.jackson.mta.primefaces.PrimeFaces62ScheduleXhtmlMigration
com.jackson.mta.primefaces.PrimeFaces62ScheduleMigration
```

## 1. Inventory first

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62ScheduleInventory \
  -Drewrite.exportDatatables=true
```

The search identifies:

```text
DefaultScheduleEvent
ScheduleEvent
LazyScheduleModel
<p:schedule>
<p:schedule timeZone="...">
```

Do not commit the OpenRewrite search markers.

## 2. DefaultScheduleEvent constructor migration

PrimeFaces 6.2 allowed:

```java
new DefaultScheduleEvent(title, startDate, endDate);
new DefaultScheduleEvent(title, startDate, endDate, allDay);
new DefaultScheduleEvent(title, startDate, endDate, styleClass);
new DefaultScheduleEvent(title, startDate, endDate, data);
```

where `startDate` and `endDate` were `java.util.Date`.

The recipe migrates those calls toward:

```java
DefaultScheduleEvent.builder()
        .title(title)
        .startDate(
                Optional.ofNullable(startDate)
                        .map(d -> LocalDateTime.ofInstant(
                                d.toInstant(),
                                ZoneId.systemDefault()))
                        .orElse(null))
        .endDate(
                Optional.ofNullable(endDate)
                        .map(d -> LocalDateTime.ofInstant(
                                d.toInstant(),
                                ZoneId.systemDefault()))
                        .orElse(null))
        .build();
```

and adds `.allDay(...)`, `.styleClass(...)`, or `.data(...)` for the matching
legacy four-argument overload.

The no-argument constructor is intentionally left alone because it still exists
in current PrimeFaces.

## 3. Why ZoneId.systemDefault() is only a compatibility bridge

`java.util.Date` is an instant on the timeline.

`LocalDateTime` deliberately contains **no timezone**.

Therefore:

```text
Date -> LocalDateTime
```

cannot be semantically correct without choosing a timezone.

The recipe uses:

```java
ZoneId.systemDefault()
```

as a migration bridge because it most closely matches applications that
historically relied on the server JVM timezone.

You must review this when the application uses:

```xhtml
<p:schedule timeZone="GMT+2">
```

or any business-specific timezone rules.

Current PrimeFaces exposes both:

```text
timeZone
clientTimeZone
```

on `p:schedule`.

A better final application design often centralizes the intended ZoneId:

```java
private static final ZoneId BUSINESS_ZONE =
        ZoneId.of("America/Chicago");
```

then uses:

```java
LocalDateTime.ofInstant(date.toInstant(), BUSINESS_ZONE)
```

instead of the temporary `ZoneId.systemDefault()` bridge.

Do not mechanically choose a business timezone without verifying how the
existing application interpreted its Dates.

## 4. LazyScheduleModel

PrimeFaces 6.2:

```java
@Override
public void loadEvents(Date start, Date end) {
    List<Event> rows = repository.find(start, end);
    ...
}
```

Modern contract:

```java
@Override
public void loadEvents(LocalDateTime start, LocalDateTime end) {
    ...
}
```

The recipe changes the method signature but initially preserves old repository
logic:

```java
@Override
public void loadEvents(
        LocalDateTime __mtaStart,
        LocalDateTime __mtaEnd) {

    /* MTA compatibility bridge: review ZoneId.systemDefault()... */

    Date start = __mtaStart == null
            ? null
            : Date.from(__mtaStart
                    .atZone(ZoneId.systemDefault())
                    .toInstant());

    Date end = __mtaEnd == null
            ? null
            : Date.from(__mtaEnd
                    .atZone(ZoneId.systemDefault())
                    .toInstant());

    List<Event> rows = repository.find(start, end);
}
```

This lets the PrimeFaces-facing API move to LocalDateTime before forcing an
immediate rewrite of repository/database code.

The better final state is normally to move the repository contract to
`LocalDateTime`, `Instant`, or another explicitly chosen temporal type.

## 5. Schedule view names

The XHTML recipe normalizes the old FullCalendar names:

```text
month       -> dayGridMonth
basicWeek   -> dayGridWeek
basicDay    -> dayGridDay
agendaWeek  -> timeGridWeek
agendaDay   -> timeGridDay
```

PrimeFaces documented these new view names as part of the FullCalendar 4
Schedule migration.

## 6. ScheduleEvent getters need separate review

Old code may contain:

```java
Date start = event.getStartDate();
Date end = event.getEndDate();
```

In modern PrimeFaces those getters return `LocalDateTime`.

The recipe intentionally does **not** globally wrap every getter back into a
Date because the correct target should usually be to modernize the surrounding
application to java.time.

Preferred final direction:

```java
LocalDateTime start = event.getStartDate();
LocalDateTime end = event.getEndDate();
```

Only convert to an Instant/Date at an integration boundary that genuinely needs
the legacy type.

## 7. isEditable() also needs review

PrimeFaces 6.2 exposed:

```java
event.isEditable()
```

Modern Schedule events separate draggable and resizable state:

```java
event.isDraggable()
event.isResizable()
```

`DefaultScheduleEvent.Builder#editable(boolean)` remains a convenience for
setting both.

Do not globally replace `isEditable()` with one of those methods without
understanding the old application's intent.

## 8. SelectEvent generics

Older code often contains:

```java
public void onEventSelect(SelectEvent event) {
    this.event = (ScheduleEvent) event.getObject();
}
```

Modern PrimeFaces event APIs are generic. A later cleanup can move this toward:

```java
public void onEventSelect(SelectEvent<ScheduleEvent<MyData>> event) {
    this.event = event.getObject();
}
```

Likewise, a date/range selection should use the temporal type emitted by the
target Schedule API rather than casting back to `Date`.

These are not rewritten generically because the correct generic data type is
application-specific.

## 9. Run the Java migration

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62ScheduleJavaMigration
```

Review, then replace `dryRun` with `run`.

## 10. Run the XHTML migration

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62ScheduleXhtmlMigration
```

Or run the composite:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62ScheduleMigration
```

## 11. Verification

```bash
grep -RIn \
  -e "DefaultScheduleEvent(" \
  -e "ScheduleEvent" \
  -e "LazyScheduleModel" \
  -e "loadEvents(Date" \
  -e "getStartDate()" \
  -e "getEndDate()" \
  -e "isEditable()" \
  -e '<p:schedule' \
  -e 'view="month"' \
  -e 'view="agenda' \
  -e 'timeZone=' \
  src

mvn clean test
```

Schedule migration also requires behavioral testing:

- event start/end times
- daylight-saving boundaries
- server timezone vs browser timezone
- all-day events
- drag/drop
- resize
- event selection
- lazy date-range loading
- locale
- default/initial view
