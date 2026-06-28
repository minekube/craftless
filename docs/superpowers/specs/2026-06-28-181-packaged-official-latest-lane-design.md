# Packaged Official Latest Lane Design

## Problem

CL-03 requires `latest-release` to reach the product distribution path. The
official 26.x lane now compiles, launches in the opt-in probe, self-attaches,
projects runtime graph operations, and invokes generated `world.time.query`.
However `mise run package-cli` still packages only the compiled remap lane and
the representative older lane. The packaged `driver-mods.json` therefore has
no Minecraft `26.2` Fabric entry, so a packaged `latest-release` client still
fails with a missing driver-lane manifest entry.

## Goal

Package the official Minecraft `26.2` lane into the CLI/Docker distribution as
a normal driver-mod manifest lane so the packaged supervisor can resolve
`latest-release` to `26.2` and select the official Craftless driver artifact.

## Non-Goals

- Do not add static gameplay descriptors, CLI gameplay commands, route
  families, or scenario shortcuts.
- Do not copy Yarn/remap gameplay bindings into `driver-fabric-official`.
- Do not mark CL-03 closed from packaging alone. Connected packaged-lane
  OpenAPI/actions/resources, SSE, JSON-RPC query/subscription artifacts, and
  public smoke still need evidence.
- Do not make `26.2` a separate public API. It is a packaged compatibility
  lane behind the same supervisor and generated per-client API surfaces.

## Design

Extend the distribution packaging workflow so it stages a second extra Fabric
driver lane root for the official latest/current module:

- build `:driver-fabric-official:jar` using the Java 25 mise runtime;
- copy that jar to
  `build/driver-lanes/mods/fabric-26.2/craftless-driver-fabric-official.jar`;
- write
  `build/driver-lanes/latest-official/fabric-driver-lanes.json` with a single
  Fabric entry:
  - `minecraftVersion`: `26.2`;
  - `loaderVersion`: `0.19.3`;
  - `fabricApiVersion`: `0.153.0+26.2`;
  - `javaMajorVersion`: `25`;
  - `mappingsFingerprint`: official lane fingerprint;
  - `artifactKey`: an extra-lane key, not `fabric-current-remap-jar`;
  - `distributionPath`: the official jar path above.

The existing `cli` Gradle distribution merge already supports extra
`fabric-driver-lanes.json` files under `craftless.extraFabricDriverLaneRoot`.
Keep using that generic merge path by placing lane catalog files under
`build/driver-lanes/older/` and `build/driver-lanes/latest-official/`, while
staging all extra artifacts under `build/driver-lanes/<distributionPath>`.
That layout matters because `stageFabricDriverLaneArtifacts` resolves extra
artifacts relative to the configured extra root plus each entry's
`distributionPath`, not relative to the catalog file directory.

## Acceptance

- A failing distribution test is added before implementation and proves the
  package task must mention and verify the latest official lane.
- `mise run package-cli` builds archives that contain:
  - `mods/craftless-driver-fabric.jar`;
  - `mods/fabric-1.20.6/craftless-driver-fabric.jar`;
  - `mods/fabric-26.2/craftless-driver-fabric-official.jar`;
  - `driver-mods.json` with entries for `1.21.6`, `1.20.6`, and `26.2`.
- Packaged `driver-mods.json` includes `fabricApiVersion`,
  `javaMajorVersion`, and `mappingsFingerprint`, but not internal
  `artifactKey` or `distributionPath`.
- `mise run fabric-lane-check-latest-official` still passes.
- `git diff --check` passes.
