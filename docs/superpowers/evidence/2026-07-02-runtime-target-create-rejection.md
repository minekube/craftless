# Runtime Target Create Rejection Evidence

Date: 2026-07-02
Phase: 202

## Changed

- `POST /clients` now maps unsupported configured Fabric driver lanes to
  `UNSUPPORTED_RUNTIME_TARGET` instead of generic `BAD_REQUEST`.
- `ConfiguredClientRuntimeDriverModProvider` now reports `NO_DRIVER_MOD` when a
  Minecraft target has no configured driver rows and `NO_COMPATIBLE_DRIVER_MOD`
  when the target has rows but not for the requested loader/runtime identity.
- `LocalSessionApiServerTest` covers a resolvable Fabric Loader `1.21.6 /
  0.16.14` request against a manifest that only supports `1.21.6 / 0.17.2`,
  verifies `NO_COMPATIBLE_DRIVER_MOD`, and proves no client launch is attempted.

## Verification

- `mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.prepared runtime rejects unsupported Fabric loader lane with matrix reason'`
- `mise exec -- gradle :daemon:test`
- `git diff --check`
- `mise run lint`
- `mise run ci`
