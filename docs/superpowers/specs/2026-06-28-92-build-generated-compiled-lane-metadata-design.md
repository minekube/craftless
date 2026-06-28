# Build-Generated Compiled Lane Metadata Design

## Problem

The active multi-version goal requires Craftless to add real runtime lanes
without letting Gradle dependencies, Fabric resource metadata, Kotlin runtime
metadata, and smoke/runtime-lane evidence drift apart. Today the
`driver-fabric` Gradle script owns the actual compiled Minecraft, Yarn, Loader,
Fabric API, Java, lane id, and provider id values, while
`FabricCompiledLaneMetadata.kt` repeats the same values by hand.

That split is not safe for adding latest/current and representative older
lanes. A lane can compile or launch with one set of values while runtime
projection and compatibility evidence report another.

## Goals

- Generate `FabricCompiledLaneMetadata.kt` from the same Gradle constants that
  configure Loom, Fabric dependencies, `fabric.mod.json`, and smoke lane JSON.
- Delete the hand-written Kotlin metadata file.
- Add a source guard that fails if a hand-written metadata file is restored
  under `src/main/kotlin`.
- Keep the public API generic and version-neutral.
- Keep latest `26.2` and older `1.20.6` lanes unsupported until real runnable
  support lands.

## Non-Goals

- Do not add a new compiled Fabric/Loom lane in this phase.
- Do not claim latest/current or older-version support.
- Do not add public version-specific APIs, route families, generated aliases,
  CLI catalogs, Fabric gameplay bindings, or scenario shortcuts.
- Do not change final gameplay behavior.
- Do not mark the project complete.

## Acceptance Criteria

- `FabricCompiledLaneMetadata` is generated under `driver-fabric/build`.
- No file named `FabricCompiledLaneMetadata.kt` exists under
  `driver-fabric/src/main/kotlin`.
- `compileKotlin` depends on the generator and compiles consumers of
  `FabricCompiledLaneMetadata`.
- Existing compatibility matrix, provider, smoke, and resource metadata tests
  still pass.
- The checklist and AGENTS phase sequence record Phase 92 and keep the broader
  multi-version support blocker active.
