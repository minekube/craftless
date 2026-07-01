# Fabric Loader Request Rejection Plan

Date: 2026-07-02
Phase: 204

## Checklist

- [x] Add a failing `/clients` regression for a requested Fabric Loader version
  absent from Fabric metadata for the Minecraft target.
- [x] Prove the old behavior returned generic `BAD_REQUEST`.
- [x] Add `NO_COMPATIBLE_FABRIC_LOADER` to the shared support reason enum.
- [x] Throw `UnsupportedClientRuntimeTarget` from Fabric metadata resolution
  when the requested loader version is not discoverable.
- [x] Verify the request rejects before launch with
  `UNSUPPORTED_RUNTIME_TARGET`, the new reason, target version, and loader
  version.
- [x] Run focused tests, daemon tests, protocol tests, lint, full repository
  CI, commit, push, and watch main CI.
