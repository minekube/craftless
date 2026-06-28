# Shared Fabric Runtime Metadata Discovery Design

## Problem

Phase 150 made the latest/current official lane derive installed-mod metadata
from Fabric Loader, but it did so with an official-lane-only provider. The
Yarn/remap Fabric lane already has similar Fabric Loader metadata code inside
`FabricDriverBackend`.

That duplication is the wrong direction for multi-version support. Runtime
metadata discovery is generic Fabric infrastructure. Version-specific lanes may
provide different mappings fingerprints, registry probes, server-feature
probes, or execution adapters, but installed-mod and loader identity discovery
must not be copied per lane.

## Goal

Extract Fabric Loader runtime metadata primitives into a shared Fabric runtime
module consumed by both `driver-fabric` and `driver-fabric-official`:

- a shared `FabricRuntimeMetadataProvider` contract;
- a shared immutable runtime metadata snapshot;
- a shared snapshot provider that emits `DriverRuntimeMetadata`;
- a shared Fabric Loader reader for loader version, driver version, installed
  mod coordinates, installed-mod fingerprints, and development-environment
  evidence;
- shared deterministic fingerprinting.

Both lanes should keep their lane-specific details narrow:

- Yarn/remap lane supplies its compiled mappings fingerprint, registry entries,
  and server-feature evidence.
- Official lane supplies its official mappings fingerprint and explicit
  not-yet-discovered registry/server-feature gaps.

## Non-Goals

- Do not add public gameplay actions.
- Do not add static gameplay descriptors, static CLI gameplay commands,
  version-specific public APIs, scenario shortcuts, or survival task shortcuts.
- Do not package the official `26.2` lane as supported.
- Do not remove the existing Yarn/remap gameplay implementation in this phase.
- Do not claim latest/current gameplay support.

## Design

Create `driver-fabric-discovery/` as a neutral shared Fabric in-client
discovery module. It may depend on Fabric Loader and `driver-api`, but it must
not depend on `driver-fabric`, `driver-fabric-official`, daemon routes, CLI
commands, or Minecraft/Yarn/official game classes.

Move the duplicate snapshot/provider/fingerprint concepts into this module.
The shared module should expose only generic Fabric runtime metadata concepts,
not gameplay operation catalogs.

`driver-fabric` should replace its local `FabricRuntimeMetadataProvider`,
`FabricRuntimeMetadataSnapshot`, `SnapshotFabricRuntimeMetadataProvider`, and
Fabric Loader helper functions with imports from the shared module. It may keep
its registry and server-feature probes because those are currently tied to
Minecraft client classes and the Yarn/remap lane.

`driver-fabric-official` should delete its official-only provider file and
construct the shared provider with official lane constants plus the shared
Fabric Loader reader.

## Acceptance

- A shared module `driver-fabric-discovery` is included in Gradle settings.
- Both Fabric driver modules depend on the shared module and include it in
  their mod jars.
- Tests prove shared snapshot metadata fingerprinting is deterministic,
  order-independent, and change-sensitive.
- Tests prove the official lane delegates to the shared provider and no longer
  declares official-only metadata provider types.
- Architecture tests prove `driver-fabric` no longer declares lane-local
  metadata provider/snapshot/fingerprint copies.
- Focused Fabric and official tests pass.
- The real enabled official attach probe still observes `client.attached` and
  reports a live `mods:` installed-mod fingerprint.
- No public gameplay action, packaged 26.x manifest entry, static gameplay
  catalog, scenario shortcut, or latest/current support claim is added.
