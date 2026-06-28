# Reflective Movement Input Shim Design

## Problem

The representative older Fabric lane no longer fails on the optional Fabric
world-change callback, but it still failed on a direct dependency on the newer
Minecraft `PlayerInput` record. That type exists in the current lane and is
absent in the representative older lane.

## Decision

Remove `PlayerInput` from Kotlin movement bindings. Keep the existing
bootstrap movement invocation path, but move the input replacement shim into a
small Java class that:

- extends Minecraft `Input`;
- defines both known tick signatures;
- writes current-lane `playerInput` reflectively when present;
- writes older-lane boolean and float input fields reflectively when present;
- restores and delegates to the original input instance after the requested
  tick budget expires.

Java is intentionally used for this shim because the no-arg `tick()` method is
an override in newer Minecraft and merely an extra method in older Minecraft.
Kotlin cannot express that source shape without version-specific source sets.

## Non-Goals

- Do not add a new public movement action.
- Do not add a static gameplay catalog.
- Do not claim representative older runtime support is complete.
- Do not hide the remaining recipe/crafting API blocker.

## Verification

- A source-level guard must fail before the implementation because
  `FabricActionBindings.kt` mentions `PlayerInput`.
- The focused guard must pass after implementation.
- The representative older-lane probe must write only
  `blockers=RecipeDisplayApi` and fail if `PlayerInput` reappears.
