# Latest Driver Lane Preflight Design

## Problem

Phase 143 proved that the installed packaged `latest-release` path resolves the
moving Mojang alias to Minecraft `26.2`, Java major version 25, Fabric Loader
`0.19.3`, and Fabric API `0.153.0+26.2`, then fails because no packaged
Craftless Fabric driver lane exists for that identity.

The failure is honest, but it currently happens after substantial binary cache
population. A request that cannot attach a packaged in-client driver should
fail before downloading client jars, asset objects, Java runtime files, Fabric
libraries, or Fabric API jars.

## Goal

Resolve the runtime driver-mod request early and check the packaged driver-mod
manifest before heavy cache preparation.

## Non-Goals

- Do not add a 26.x driver lane.
- Do not claim latest/current support.
- Do not add static gameplay actions, route families, CLI gameplay catalogs, or
  public protocol types.
- Do not make daemon behavior branch by Minecraft version except through
  resolver data and artifact selection.

## Design

`WorkspaceClientRuntimeDriverFactory.prepare` should keep a two-step boundary:

1. Resolve identity:
   - requested alias to concrete Minecraft version;
   - preferred Fabric Loader version;
   - Fabric API artifact version;
   - Java major version from the Mojang version manifest.
2. Check `ConfiguredClientRuntimeDriverModProvider` with that resolved identity.

Only after the driver mod provider accepts the resolved lane should full
`CachePreparationService.prepare` populate binary artifacts.

This keeps the daemon generic. The preflight uses the same resolver data as the
full cache path; it does not introduce a latest/current special case.

## Acceptance

- A focused daemon test requests `version=latest-release` where the manifest
  resolves to `26.2` and the packaged driver manifest only contains `1.21.6`.
- The request returns HTTP 400 with the resolved Minecraft version, loader
  version, Fabric API version, and Java major version in the error.
- No runtime launcher invocation occurs.
- No binary metadata fetches occur.
- No asset-object cache directory is created.
