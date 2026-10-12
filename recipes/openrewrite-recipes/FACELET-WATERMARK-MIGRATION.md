# Legacy PrimeFaces Facelet functions and Watermark client API

PrimeFaces 8 removed two old Facelet functions:

```text
p:component(...)
p:widgetVar(...)
```

Replacement direction:

```text
p:component(id)
    -> p:resolveFirstComponentWithId(id, view).clientId

p:widgetVar(id)
    -> p:resolveWidgetVar(id, view)
```

PrimeFaces 8 also reimplemented `p:watermark` using HTML placeholder behavior
and removed:

```text
PrimeFaces.cleanWatermarks()
PrimeFaces.showWatermarks()
```

## Concrete recipes

```text
PrimeFaces62LegacyClientApiInventory
PrimeFaces62LegacyFaceletFunctionMigration
PrimeFaces62WatermarkReview
PrimeFaces62LegacyFaceletAndWatermarkMigration
```

The Facelet function recipe replaces simple non-nested function arguments in
XML/XHTML attribute values and character data.

The Watermark client methods have no equivalent source rewrite; search results
and migration comments identify views that require JavaScript cleanup.

After the rewrite, search:

```bash
grep -RIn \
  -e "p:component(" \
  -e "p:widgetVar(" \
  -e "PrimeFaces.cleanWatermarks" \
  -e "PrimeFaces.showWatermarks" \
  src
```
