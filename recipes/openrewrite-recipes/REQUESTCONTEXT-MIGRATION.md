# PrimeFaces RequestContext Migration

PrimeFaces 7 replaced the legacy `org.primefaces.context.RequestContext` API with `PrimeFaces.current()`.

Primary recipe:

```text
com.jackson.mta.primefaces.PrimeFaces62RequestContextMigration
```

The recipe handles high-confidence calls such as `execute`, `update`, `reset`, callback parameters, scrolling,
and dynamic-dialog APIs. Internal/ambiguous APIs remain review items.

After running the recipe, verify that no application code imports `org.primefaces.context.RequestContext` and
exercise Ajax updates, callbacks and dynamic dialogs in the browser.
