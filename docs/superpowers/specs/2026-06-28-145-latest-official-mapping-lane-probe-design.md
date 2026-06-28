# Latest Official Mapping Lane Probe Design

## Problem

Craftless can package and attach a representative older Fabric lane, and it now
fails missing latest/current driver lanes before heavyweight cache downloads.
The next blocker is not a public API gap. It is the source/build boundary for
Minecraft 26.x: current latest releases use Fabric's official/Mojang mapping
direction, while Craftless's compiled driver lane is still Yarn/remap-oriented.

Without an executable latest-lane probe, future agents can keep restating this
blocker without making it measurable.

## Goal

Add a dedicated latest/current Fabric lane probe task that exercises the
official/Mojang-mapping build boundary and writes machine-readable evidence
about whether the latest lane compiles or which source-compatibility blocker
remains.

## Non-Goals

- Do not claim `latest-release` or Minecraft 26.x support.
- Do not add a packaged 26.x driver manifest entry until the probe compiles and
  a real artifact can be packaged.
- Do not add public gameplay actions, CLI gameplay catalogs, static route
  families, or scenario shortcuts.
- Do not convert the existing verified Yarn/remap current/older lanes in this
  phase.

## Design

Add a `mise` task named `fabric-lane-check-latest-official`. It should:

1. Resolve the repository through `mise exec -- gradle`.
2. Run a latest/current driver compile probe with explicit 26.x lane metadata:
   Minecraft `26.2`, Fabric Loader `0.19.3`, Fabric API `0.153.0+26.2`, Java
   25, lane id `fabric-latest-official-lane`, provider id
   `fabric-latest-official-lane`, artifact key
   `fabric-latest-official-jar`, mappings fingerprint
   `craftless-fabric-official-bindings-26-2`, and distribution path
   `mods/fabric-26.2/craftless-driver-fabric.jar`.
3. Use an explicit official-mapping mode flag that removes the Yarn `mappings`
   dependency for the latest lane, so the probe cannot accidentally masquerade
   as another Yarn lane.
4. Write `build/reports/fabric-lane-check-latest-official.log`.
5. Write `build/reports/fabric-lane-check-latest-official.status` with either:
   - `status=compiled`, or
   - `status=source-compatibility-blocked` plus a machine-readable blocker.

The first implementation can still report a blocker. That is useful progress
because the blocker becomes a local CI-ready gate.

## Acceptance

- A test proves `.mise.toml` exposes `fabric-lane-check-latest-official`.
- The task uses official mapping mode, does not pass
  `craftless.fabric.yarnMappings`, and the Gradle build treats official mode
  as a no-mappings dependency lane.
- The task records log and status artifacts under `build/reports/`.
- The task uses the resolved latest/current lane identity from Phase 143.
- Running the task produces either `status=compiled` or
  `status=source-compatibility-blocked` with a blocker reason.
- If the existing remap plugin rejects official mode because mappings are
  mandatory, the status must record `blockers=loom-remap-requires-mappings`.
