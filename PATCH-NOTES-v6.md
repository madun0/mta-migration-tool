# v6 patch notes

- Added migration for `RequestContext.getCurrentInstance().isAjaxRequest()` to `PrimeFaces.current().isAjaxRequest()`.
- Added behavioral coverage in both the direct recipe test and catalog-facing composite test.
- This prevents the legacy `org.primefaces.context.RequestContext` import from surviving solely because of `isAjaxRequest()` usage before PrimeFaces is upgraded to 16.
