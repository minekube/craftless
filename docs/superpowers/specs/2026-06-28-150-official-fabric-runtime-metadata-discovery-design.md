# Official Fabric Runtime Metadata Discovery Design

## Problem

The latest/current official Fabric lane now launches, self-attaches, and writes
generated per-client OpenAPI metadata. Its runtime metadata is still mostly
hard-coded inside `OfficialFabricDriverBackend`: installed mods are reported as
`mods:official-lane-probe`, registries as `registries:unavailable`, and server
features as `server-features:unavailable`.

That is useful as a probe placeholder, but it is not the durable shape. Version
support must advance through live runtime discovery and fingerprints, not
static lane constants or copied gameplay catalogs.

## Goal

Make the official lane derive non-gameplay runtime metadata from live Fabric
Loader state:

- Fabric Loader version;
- installed mod ids and versions, including Minecraft, Fabric Loader, Fabric
  API, and the Craftless official driver;
- sanitized fingerprints surfaced in `DriverRuntimeMetadata`;
- runtime graph source evidence and generated OpenAPI extensions based on that
  metadata.

This remains metadata/projection work only. It does not add gameplay actions,
execution adapters, public route families, packaged driver manifest entries, or
latest/current support claims.

## Non-Goals

- Do not add public gameplay actions.
- Do not copy the Yarn/remap `driver-fabric` gameplay bindings into
  `driver-fabric-official`.
- Do not add `driver-fabric-official` to packaged `driver-mods.json`.
- Do not model Minecraft `26.2` with a separate public API, DTO, route tree,
  CLI command tree, or session type.
- Do not claim latest/current gameplay support.

## Design

Add an official-lane runtime metadata provider that accepts a loader snapshot.
The production provider should read `FabricLoader.getInstance()` and create a
snapshot containing loader version and installed mod coordinates. A testable
snapshot provider should support deterministic unit tests without launching
Minecraft.

`OfficialFabricDriverBackend` should depend on this provider instead of
embedding hard-coded fingerprints. The generated graph can remain metadata-only
for gameplay, but its `runtime` resource evidence must reflect the provider's
metadata.

The opt-in real probe must continue to launch the official `runClient`, observe
`client.attached`, and capture generated OpenAPI while the client is attached.
The evidence summary should show a live installed-mods fingerprint instead of
`mods:official-lane-probe`.

## Acceptance

- Architecture tests fail if `OfficialFabricDriverBackend` hard-codes
  `mods:official-lane-probe`, `registries:unavailable`, or
  `server-features:unavailable`.
- Unit tests prove the official metadata provider fingerprints a supplied
  loader snapshot deterministically.
- Unit tests prove `OfficialFabricDriverBackend.runtimeMetadata()` delegates to
  the provider.
- The real enabled official attach probe still observes `client.attached`.
- The real probe's `client-openapi.json` reports
  `x-craftless-installed-mods-fingerprint` starting with `mods:` and not equal
  to `mods:official-lane-probe`.
- No gameplay action, packaged 26.x manifest entry, static gameplay catalog,
  scenario shortcut, or latest/current support claim is added.
