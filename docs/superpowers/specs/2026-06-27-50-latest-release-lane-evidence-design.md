# Phase 50: Latest Release Lane Evidence Design

## Goal

Keep the real latest Minecraft release probe lane honest: `26.2` is a real
release lane from Mojang metadata, but Craftless does not yet have a compatible
Fabric client runtime lane for it.

## Context

Phase 26 introduced multi-version architecture evidence while the compiled
Fabric client lane remained `1.21.6`. The unsupported `26.2` evidence was named
`fabric-simulated-26`, which now reads like a fake Minecraft version or a
test-only lane. That conflicts with the compatibility goal: current releases
must be represented as real runtime inputs with explicit unsupported reasons,
not as simulated support and not as public API breadth.

The current Mojang version manifest reports latest release `26.2` and latest
snapshot `26.3-snapshot-1` at the time this phase was written. This phase does
not claim Craftless can run a `26.2` Fabric client. It only renames the private
unsupported-lane evidence so code, smoke artifacts, and docs state the actual
boundary clearly.

## Requirements

- Preserve `26.2` as an unsupported latest-release compatibility lane.
- Rename private lane evidence away from `simulated` wording.
- Keep the lane status `UNSUPPORTED` with reason `runtime-lane-missing`.
- Keep provider evidence machine-readable and free of Fabric/Yarn/Minecraft
  contract leakage after sanitization.
- Update Gradle-generated smoke runtime-lane JSON so live artifacts match the
  Kotlin compatibility matrix.
- Update docs/checklist wording that describes the lane as simulated.

## Non-Goals

- Do not add `26.2` Fabric client support.
- Do not add a new compiled Loom lane.
- Do not add public gameplay actions, CLI catalogs, route families, or scenario
  shortcuts.
- Do not mark Craftless complete.

## Verification

- Focused driver/testkit tests prove `26.2` resolves to a latest-release
  unsupported lane with `runtime-lane-missing`.
- `git diff --check`, `mise run architecture-check`, and `mise run ci` pass
  before claiming the phase complete.
