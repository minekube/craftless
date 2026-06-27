# Phase 47: Compiled Fabric Resource Metadata Design

## Goal

Make `driver-fabric` build-time resource metadata derive from one Gradle
compiled-lane metadata block instead of hard-coding the current Minecraft,
Fabric API, Fabric Loader, Java, and display strings directly in
`fabric.mod.json`.

## Context

Phase 46 centralized Kotlin-side runtime lane metadata. The Fabric module still
has the same compiled lane values repeated in Gradle dependencies, Gradle smoke
lane JSON, and resource metadata. Build dependencies must remain build-time
inputs, but Fabric resource metadata can be expanded from Gradle properties so
the published mod descriptor remains aligned with the compiled lane.

This does not add a new Minecraft version lane. It only reduces metadata drift
inside the current compiled Fabric/Loom target.

## Requirements

- Keep the current Loom dependencies pinned to the verified compiled lane.
- Define build-time compiled-lane metadata in `driver-fabric/build.gradle.kts`
  for Minecraft version, Yarn mappings, Fabric Loader, Fabric API, Java major,
  current lane id, and current lane provider id.
- Expand `fabric.mod.json` from those build-time values instead of hard-coding
  the current lane directly in the resource file.
- Keep the generated metadata honest: the mod name/description must say this is
  the compiled lane, not broad multi-version support.
- Keep smoke runtime-lane JSON derived from the same build-time values.
- Do not add public gameplay action ids, generated route families, CLI gameplay
  catalogs, Fabric descriptor/binding pairs, scenario shortcuts, a second
  compiled lane, or a public version-specific API.

## Non-Goals

- Do not parameterize Loom compilation for arbitrary user-selected versions.
- Do not change Minecraft, Yarn, Fabric Loader, Fabric API, or Java versions.
- Do not remove version-scoped mixin/accessor packages.
- Do not claim Minecraft `26.2` Fabric client support.
- Do not mark Craftless complete.

## Verification

- A focused Fabric module test proves the source `fabric.mod.json` uses
  placeholders for compiled-lane fields.
- A focused Fabric module test proves processed `fabric.mod.json` contains the
  same lane values used by the compiled-lane metadata.
- `:driver-fabric:processResources` and `:driver-fabric:test` pass.
- `mise run lint`, `mise run architecture-check`, and `mise run ci` pass before
  claiming this phase complete.
