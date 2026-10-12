# PrimeFaces 6.2 FileUpload -> PrimeFaces 16

PrimeFaces 8 changed the FileUpload API substantially:

- file models moved from `org.primefaces.model` to `org.primefaces.model.file`
- `UploadedFile#getContents()` became `getContent()`
- `UploadedFile#getInputstream()` became `getInputStream()`
- `fileUploadListener` became `listener`
- simple multiple upload uses `UploadedFiles`

PrimeFaces 15 removed Apache Commons FileUpload support and the legacy
PrimeFaces FileUpload filter.

## Concrete recipes

```text
MigrateUploadedFileApi
PrimeFaces62FileUploadInventory
PrimeFaces62FileUploadXhtmlMigration
PrimeFaces62FileUploadCommonsReview
PrimeFaces62FileUploadMigration
```

## Safe transformations

```java
org.primefaces.model.UploadedFile
```

becomes:

```java
org.primefaces.model.file.UploadedFile
```

and:

```java
file.getContents()
file.getInputstream()
```

become:

```java
file.getContent()
file.getInputStream()
```

XHTML:

```xhtml
<p:fileUpload fileUploadListener="#{bean.upload}" />
```

becomes:

```xhtml
<p:fileUpload listener="#{bean.upload}" />
```

`dragDropSupport` is renamed to `dragDrop`.

## Review cases

Simple multiple mode:

```xhtml
<p:fileUpload mode="simple" multiple="true" value="#{bean.files}" />
```

must bind to the modern `UploadedFiles` model.

Legacy `FileUploadFilter` and `primefaces.UPLOADER=commons` configuration
should be removed after native Servlet multipart upload has been validated on
JBoss EAP 8.

Validation attributes changed in later PrimeFaces releases. Review old
`validateContentType` behavior against current `p:validateFile`.

## Run

```bash
mvn -U org.openrewrite.maven:rewrite-maven-plugin:6.49.0:dryRun \
  -Drewrite.recipeArtifactCoordinates=com.jackson.mta:primefaces-openrewrite-recipes:1.0.0-SNAPSHOT \
  -Drewrite.activeRecipes=com.jackson.mta.primefaces.PrimeFaces62FileUploadMigration
```

Verify:

```bash
grep -RIn \
  -e "fileUploadListener=" \
  -e "org.primefaces.model.UploadedFile" \
  -e "getContents()" \
  -e "getInputstream()" \
  -e "FileUploadFilter" \
  -e "primefaces.UPLOADER" \
  src
```
