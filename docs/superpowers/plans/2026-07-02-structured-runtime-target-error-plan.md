# Structured runtime target error plan

## Goal

Make `/clients` unsupported Fabric runtime target failures expose structured
machine-readable details.

## Steps

- [x] Add a failing daemon regression requiring `details.reason`,
  runtime identity fields, and `availableLoaderVersions` for
  `UNSUPPORTED_RUNTIME_TARGET`.
- [x] Add a failing OpenAPI regression requiring optional `details` in stable
  error schemas.
- [x] Preserve `code` and `message` compatibility.
- [x] Carry the existing `UnsupportedClientRuntimeTarget` request and available
  loader metadata into the HTTP response.
- [x] Keep ordinary errors lean by omitting `details` when absent.
- [x] Run focused, protocol, and daemon verification.
