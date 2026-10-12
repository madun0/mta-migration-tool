# PrimeFaces 6.2 Calendar -> PrimeFaces 16 DatePicker

PrimeFaces DatePicker was introduced as the replacement for the old
`p:calendar`. The migration is not only a tag rename because several Calendar
attributes have different names or semantics.

This project provides three staged OpenRewrite recipes:

```text
com.jackson.mta.primefaces.PrimeFaces62CalendarInventory
com.jackson.mta.primefaces.PrimeFaces62CalendarToDatePicker
com.jackson.mta.primefaces.PrimeFaces62CalendarCleanupAfterReview
```

## 1. Inventory first

Run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62CalendarInventory \
  -Drewrite.exportDatatables=true
```

This identifies all `p:calendar` tags and highlights usages that need semantic
review, including:

```text
effect / effectDuration
popupIconOnly
beforeShowDay
disabledWeekends
pagedate
timeControlType / timeControlObject
oneLine
showHour / showMinute / showMillisec
date/time patterns containing H or h
```

Search recipes insert markers during a dry run. Do not commit those markers.

## 2. Apply the safe Calendar -> DatePicker migration

Run a dry run:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62CalendarToDatePicker
```

Then apply after review by changing `dryRun` to `run`.

### Transformations

#### Component name

```xhtml
<p:calendar .../>
```

becomes:

```xhtml
<p:datePicker .../>
```

#### Inline mode

```xhtml
<p:calendar mode="inline" .../>
```

becomes:

```xhtml
<p:datePicker inline="true" .../>
```

`mode="popup"` is removed because popup is DatePicker's normal mode.

#### Multiple months

```xhtml
<p:calendar pages="3" .../>
```

becomes:

```xhtml
<p:datePicker numberOfMonths="3" .../>
```

#### Navigator

```xhtml
<p:calendar navigator="true" .../>
```

becomes:

```xhtml
<p:datePicker monthNavigator="true" yearNavigator="true" .../>
```

For PrimeFaces 15+, `yearNavigator` accepts String modes. `"true"` remains a
supported value and displays the accessible numeric-input navigator.

#### Button panel

```xhtml
showButtonPanel="true"
```

becomes:

```xhtml
showButtonBar="true"
```

#### Millisecond default

```xhtml
defaultMillisec="0"
```

becomes:

```xhtml
defaultMillisecond="0"
```

#### Popup trigger semantics

Old Calendar:

```xhtml
showOn="button"
```

becomes:

```xhtml
showIcon="true" showOnFocus="false"
```

Old:

```xhtml
showOn="both"
```

becomes:

```xhtml
showIcon="true" showOnFocus="true"
```

Old:

```xhtml
showOn="focus"
```

becomes:

```xhtml
showIcon="false" showOnFocus="true"
```

This recipe also preserves Calendar's old default behavior. Calendar defaulted
to `showOn="both"`, while DatePicker defaults to focus-open with no icon. For
popup calendars with no explicit `showOn`, the recipe therefore adds:

```xhtml
showIcon="true"
```

## 3. Time-enabled calendars need semantic review

PrimeFaces 6.2 Calendar commonly enabled time by embedding time tokens into the
pattern:

```xhtml
<p:calendar
    value="#{bean.start}"
    pattern="MM/dd/yyyy HH:mm:ss"/>
```

DatePicker exposes time selection explicitly:

```xhtml
<p:datePicker
    value="#{bean.start}"
    pattern="MM/dd/yyyy"
    showTime="true"
    showSeconds="true"
    hourFormat="24"/>
```

and the preferred Java type is:

```java
java.time.LocalDateTime
```

instead of:

```java
java.util.Date
```

The automatic recipe adds a migration comment but does not rewrite the pattern
or Java property. That is intentional: the correct target may be `LocalDate`,
`LocalDateTime`, or `LocalTime`, and timezone semantics can be business-critical.

### Typical Java migrations

Date only:

```java
private Date effectiveDate;
```

often becomes:

```java
private LocalDate effectiveDate;
```

Date and time:

```java
private Date appointmentTime;
```

often becomes:

```java
private LocalDateTime appointmentTime;
```

Time only:

```java
private Date startTime;
```

often becomes:

```java
private LocalTime startTime;
```

Do not globally change `java.util.Date` to `LocalDate`. JPA mappings, database
column types, REST/SOAP contracts, serialization, timezones, and downstream APIs
must be considered.

PrimeFaces DatePicker has built-in support for `LocalDate`, `LocalDateTime`, and
`LocalTime`.

## 4. SelectEvent listener types

Old applications may have:

```java
public void onDateSelect(SelectEvent event) {
    Date date = (Date) event.getObject();
}
```

When the backing value is migrated to `LocalDate`, prefer:

```java
public void onDateSelect(SelectEvent<LocalDate> event) {
    LocalDate date = event.getObject();
}
```

Use `LocalDateTime` or `LocalTime` instead when that matches the DatePicker
value.

This is intentionally not globally rewritten because the event's generic type
must match the migrated value property.

## 5. Legacy options needing manual decisions

### beforeShowDay

Calendar's `beforeShowDay` callback does not have a direct DatePicker mapping.

Depending on the intent, use:

```text
dateTemplate
disabledDates
disabledDays
```

or custom client-side behavior.

### disabledWeekends

Model the disabled weekdays explicitly in the backing bean:

```java
private List<Integer> disabledDays = List.of(0, 6);
```

and bind:

```xhtml
<p:datePicker disabledDays="#{bean.disabledDays}" .../>
```

Verify the weekday indexing expected by the target PrimeFaces version and your
locale before rollout.

### effect / effectDuration

These old Calendar animation options do not have a direct DatePicker
equivalent. Remove them after UI verification.

### popupIconOnly

Review the intended UX and reproduce it with DatePicker's `showIcon`,
`showOnFocus`, styling, or surrounding markup.

### old time-control options

Review options such as:

```text
timeControlType
timeControlObject
oneLine
showHour
showMinute
showMillisec
```

against the target DatePicker API instead of mechanically renaming them.

## 6. Cleanup only after review

After the migration comments have been resolved, the optional recipe:

```text
com.jackson.mta.primefaces.PrimeFaces62CalendarCleanupAfterReview
```

removes the legacy Calendar-only attributes.

Run it as a dry run first:

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62CalendarCleanupAfterReview
```

Do not use this cleanup recipe before implementing the intended behavior.

## 7. Verify

After transformation:

```bash
grep -RIn \
  -e "<p:calendar" \
  -e "<p:datePicker" \
  -e "java.util.Date" \
  -e "SelectEvent" \
  -e "beforeShowDay" \
  -e "disabledWeekends" \
  -e "showOn=" \
  -e "navigator=" \
  -e "pages=" \
  src

mvn clean test
```

Date inputs require browser testing in addition to compilation:

- popup trigger/icon behavior
- inline calendars
- navigator behavior
- date parsing and formatting
- time selection
- timezone behavior
- min/max dates
- disabled dates/days
- Ajax `dateSelect` listeners
- validation and converters
- DataTable date filters
