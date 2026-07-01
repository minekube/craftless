# Fabric Loader Request Rejection Evidence

Date: 2026-07-02
Phase: 204

## Changed

- `FabricSupportReason` now includes `NO_COMPATIBLE_FABRIC_LOADER`.
- `CachePreparationService` turns an explicitly requested Fabric Loader
  version absent from Fabric metadata into `UnsupportedClientRuntimeTarget`.
- `/clients` now returns `UNSUPPORTED_RUNTIME_TARGET` for this case and
  includes the requested Minecraft target, requested loader, and available
  loader versions in the error message.
- `LocalSessionApiServerTest` proves no launch is attempted for a
  non-discoverable loader request.

## Verification

- `mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.prepared runtime rejects non discoverable Fabric loader version with matrix reason'`
- `mise exec -- gradle :protocol:test`
- `mise exec -- gradle :daemon:test`
- `git diff --check`
- `mise run lint`
- `mise run ci`
