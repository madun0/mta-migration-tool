# PrimeFaces 6.2 -> 16 Recipe Coverage

This project now covers the major migration families selected for this
modernization pack.

## Completed families

1. RequestContext -> PrimeFaces.current()
2. UploadedFile / FileUpload API
3. ByteArrayContent / DefaultStreamedContent
4. LazyDataModel
5. Programmatic menu APIs
6. Legacy jqPlot / Chart migration scaffolding
7. Calendar -> DatePicker
8. Schedule
9. Timeline
10. InputSwitch -> ToggleSwitch
11. p:repeat -> ui:repeat
12. DataTable filtering
13. org.primefaces.json -> shaded JSON package
14. removed autoUpdate attributes
15. OverlayPanel appendToBody
16. DataTable rowsPerPageTemplate star option
17. InputMask compatibility
18. removed PrimeFaces Push
19. legacy Facelet functions and Watermark JavaScript
20. remaining StreamedContent constructor/lazy edge cases

## Automation policy

Recipes are classified as:

- automatic when the target behavior is a high-confidence API/tag rename
- compatibility-bridge when preserving old business logic is safer than
  rewriting it immediately
- inventory/review when a breaking change requires an application-specific
  architecture, UI, timezone, database, validation, or JavaScript decision

Every public recipe is implemented as a concrete Java class extending
`org.openrewrite.Recipe`, and every recipe class has a matching test class.
There are no YAML-only public recipe names in this project.

The final build gate remains:

```bash
mvn clean test
```

on JDK 21 with the project's Maven dependencies available.
