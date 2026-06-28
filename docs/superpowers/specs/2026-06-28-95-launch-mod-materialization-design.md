# Launch Mod Materialization Design

## Problem

Phase 94 adds Fabric API cache artifacts as `FABRIC_MOD` and exposes their
handles through `CacheLaunchPlan.mods`, but the process launcher does not yet
materialize those cached mod jars into the instance `mods` directory before
launch. A prepared Fabric client can therefore resolve Fabric API metadata and
still start without the resolved mod present in the game directory.

This is a concrete launch-foundation gap for multi-version support.

## Goals

- Copy every `CacheLaunchPlan.mods` handle into the requested instance
  `mods` directory before starting the client process.
- Keep copied filenames deterministic and derived from source handle names.
- Keep all paths constrained under the workspace root and existing instance
  file layout.
- Preserve existing command construction and generated launch arguments.

## Non-Goals

- Do not add new compiled Fabric/Loom lanes.
- Do not claim latest/current or older-version support.
- Do not add public version-specific APIs, generated route families, CLI
  gameplay catalogs, Fabric gameplay bindings, or scenario shortcuts.
- Do not mark the project complete.

## Acceptance Criteria

- `ProcessClientRuntimeLauncher` copies cached Fabric mod handles to
  `InstanceFiles.mods` before launch.
- A process-launcher test proves a cached Fabric API jar is present in the
  instance mods directory after launch.
- Launch command behavior remains unchanged except for the mod file
  materialization.
- Focused daemon tests and local gates pass.
