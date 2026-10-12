# PrimeFaces LazyDataModel Migration

Modern PrimeFaces replaces legacy string/single-sort and raw-filter maps with `SortMeta` and `FilterMeta` maps.

Recipes:

```text
com.jackson.mta.primefaces.PrimeFaces62LazyDataModelMigration
com.jackson.mta.primefaces.PrimeFaces62LazyDataModelCountBridge
```

The migration preserves existing DAO logic through compatibility locals where possible. The count bridge is
conservative and does not invent application-specific database count logic.

Review filtering semantics, multi-sort behavior, row count queries, pagination and any code that directly reads
raw filter values.
