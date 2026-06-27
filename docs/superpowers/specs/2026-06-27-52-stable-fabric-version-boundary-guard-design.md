# Phase 52: Stable Fabric Version Boundary Guard Design

## Goal

Make the stable Fabric package boundary enforceable so future multi-version
work cannot accidentally spread current-lane `v1_21_6` imports through
non-versioned production code.

## Context

Phase 51 introduced `FabricBootstrapSelector` as the one non-versioned
selection point that knows about the current compiled Fabric lane. That is the
right direction, but it is still only guarded at the entrypoint source file.
Future 26.x or other Minecraft lanes should plug into a selector/registry
boundary without changing the public Fabric entrypoint or leaking versioned
implementation packages into stable package code.

Craftless still supports one compiled Fabric client lane today. This phase
does not add a second lane, dynamic Loom compilation, or new Minecraft version
support. It makes the current one-lane state explicit and harder to regress.

## Requirements

- Treat `FabricBootstrapSelector.kt` as the only non-versioned Fabric
  production file allowed to import `com.minekube.craftless.driver.fabric.v*`.
- Add a source-level architecture test that scans stable top-level Fabric
  production Kotlin files and fails if any file except
  `FabricBootstrapSelector.kt` imports a version-scoped implementation package.
- Add selector tests that expose registered bootstrap metadata without
  initializing Minecraft.
- Ensure registered bootstrap metadata contains the current compiled provider
  id and Minecraft version.
- Ensure the selector chooses the same provider and Minecraft version as the
  supported current lane in `defaultFabricCompatibilityMatrix()`.
- Keep the stable Fabric entrypoint dependent only on the selector.
- Keep bytecode-sensitive Mixins/accessors and current-lane implementation
  classes version-scoped.

## Non-Goals

- Do not add Minecraft `26.2` Fabric client support.
- Do not add a new compiled Loom lane.
- Do not move current Fabric implementation classes out of `v1_21_6`.
- Do not add public gameplay actions, generated route families, CLI gameplay
  catalogs, Fabric descriptor/binding pairs, scenario shortcuts, or public
  version-specific APIs.
- Do not mark Craftless complete.

## Design

`FabricBootstrapSelector` remains the stable internal registration boundary.
It will expose a small metadata view of registered bootstraps for tests and
future diagnostics while keeping initialization explicit. The current compiled
bootstrap remains registered there and nowhere else in stable production code.

The architecture test will walk
`driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/*.kt`
instead of relying on a fixed file list. That makes new stable Fabric files opt
into the boundary rule automatically. The single allowed exception is
`FabricBootstrapSelector.kt`, because it is the registry bridge from stable
selection to versioned implementations.

The selector metadata test will compare the selected bootstrap with
`defaultFabricCompatibilityMatrix().resolve(...)` for the current compiled
runtime identity. This keeps the startup registry and runtime compatibility
evidence aligned without claiming broader Minecraft support.

## Verification

- Focused Phase 52 tests fail before the selector exposes registered metadata
  and pass after implementation.
- `:driver-fabric:test`, `mise run lint`, `mise run architecture-check`, and
  `mise run ci` pass before this phase is claimed complete.
