# Generated Driver Lane Catalog Design

## Problem

Craftless now uses packaged `driver-mods.json` to select a compatible Fabric
driver mod for a prepared runtime lane. The manifest itself is still generated
inside `cli/build.gradle.kts` by reading two single-lane extra properties from
`driver-fabric`. That keeps the CLI distribution tied to one compiled Fabric
lane and makes future multi-lane driver artifacts easy to wire incorrectly.

The next multi-version step is not to claim support for another Minecraft
version. It is to move the build handoff to a generated lane catalog owned by
`driver-fabric`, then let distribution packaging consume that catalog. The
catalog can contain one lane today and many lanes later.

## Goals

- Generate a private Fabric driver lane catalog from `driver-fabric` build
  metadata.
- Make `cli` derive `driver-mods.json` from that catalog instead of direct
  `fabricCompiledMinecraftVersion` / `fabricCompiledLoaderVersion` reads.
- Preserve the current installed distribution shape:
  `driver-mods.json` plus `mods/craftless-driver-fabric.jar`.
- Keep the catalog internal build/package metadata, not public API.
- Keep current support claims unchanged: only the existing compiled lane is
  supported; latest/current 26.x and representative older lanes remain open
  until runnable driver artifacts and evidence exist.

## Non-Goals

- Do not add a second compiled Fabric lane.
- Do not change Fabric Loom dependency versions.
- Do not change runtime capability graph, public OpenAPI, CLI gameplay
  commands, or action descriptors.
- Do not claim latest/current or older-version support.
- Do not remove the single-jar fallback path for manually configured or older
  installed distributions.

## Acceptance Criteria

- A red distribution test fails before implementation because `cli` packaging
  does not consume a generated `fabric-driver-lanes.json` catalog.
- A red Fabric module guard fails before implementation because
  `driver-fabric` does not generate the lane catalog task.
- After implementation, `driver-fabric` has a `writeFabricDriverLaneCatalog`
  task that writes `build/generated/driver-lanes/fabric-driver-lanes.json`.
- `cli:writeDriverModManifest` depends on the driver catalog task and renders
  `driver-mods.json` from catalog entries.
- The generated manifest still contains the current Fabric lane and path
  `mods/craftless-driver-fabric.jar`.
- Focused tests/tasks, `git diff --check`, and `mise run ci` pass locally.
