# Game-scoped Fabric loader matrix plan

## Goal

Align `/versions/support-targets` with the Fabric loader compatibility metadata
used by cache preparation and client creation.

## Steps

- [x] Add a failing daemon API regression where Fabric's global loader list
  includes `0.19.2`, but `/versions/loader/1.21.6` lists only `0.17.2`.
- [x] Change `VersionDiscoveryService` to fetch game-scoped loader metadata for
  each Fabric game target with bounded concurrency.
- [x] Generate runtime rows from the union of global loader versions,
  game-scoped loader versions, and explicit manifest loader versions.
- [x] Mark loaders absent from game-scoped metadata as
  `NO_COMPATIBLE_FABRIC_LOADER` instead of supported.
- [x] Keep wildcard driver manifest rows supported only for game-compatible
  loader versions.
- [x] Run focused and daemon verification.
