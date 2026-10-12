# PrimeFaces Programmatic Menu Migration

The programmatic menu model changed significantly after PrimeFaces 6.2.

Primary recipe:

```text
com.jackson.mta.primefaces.PrimeFaces62MenuMigration
```

The recipe migrates `DynamicMenuModel` toward `BaseMenuModel`, converts `addElement` calls to the elements
collection and modernizes supported `DefaultMenuItem` / `DefaultSubMenu` constructor patterns to builders.

Setter-driven/no-argument construction that remains supported is intentionally left alone. Verify command/action
bindings, icons, URLs, disabled/rendered conditions and submenu hierarchy after migration.
