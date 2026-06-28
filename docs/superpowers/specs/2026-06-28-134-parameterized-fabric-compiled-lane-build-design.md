# Parameterized Fabric Compiled Lane Build Design

## Problem

`driver-fabric/build.gradle.kts` still hard-codes the compiled lane metadata:
Minecraft version, Yarn mappings, Fabric Loader, Fabric API, Java major,
provider id, lane id, artifact key, and mappings fingerprint. That prevents
honest local compatibility probes for latest/current or representative older
runtime lanes without editing source files.

The active goal requires real multi-version support. The next useful step is to
make the compiled driver lane build parameterized so Codex and CI can ask:

- does the current driver source compile against `1.20.6` metadata?
- does a latest lane fail because mappings are missing, because source APIs
  changed, or because a runtime dependency is unavailable?

## Goals

- Let Gradle properties override all compiled Fabric lane metadata fields.
- Preserve the current packaged default lane exactly when no properties are
  provided.
- Add a local mise task that compiles/probes the representative older
  `1.20.6` lane from real Fabric/Mojang metadata values.
- Keep generated `FabricCompiledLaneMetadata`, `fabric.mod.json`, and
  `fabric-driver-lanes.json` in sync with the selected lane properties.

## Non-Goals

- Do not package more than one Fabric driver artifact in this phase.
- Do not add a latest `26.2` lane, because live Yarn metadata currently has no
  `26.2+...` mappings artifact.
- Do not claim latest/current or older-version runtime support is complete.
- Do not add public gameplay APIs, generated route families, CLI gameplay
  catalogs, Fabric descriptor/binding pairs, or scenario shortcuts.

## Design

`driver-fabric/build.gradle.kts` reads property-backed lane values:

- `craftless.fabric.minecraftVersion`
- `craftless.fabric.yarnMappings`
- `craftless.fabric.loaderVersion`
- `craftless.fabric.apiVersion`
- `craftless.fabric.javaMajorVersion`
- `craftless.fabric.laneId`
- `craftless.fabric.providerId`
- `craftless.fabric.artifactKey`
- `craftless.fabric.mappingsFingerprint`

Defaults preserve the current `1.21.6` packaged lane. Property overrides affect:

- Gradle `minecraft`, `mappings`, Fabric Loader, and Fabric API dependencies;
- `fabric.mod.json` expansion;
- generated `FabricCompiledLaneMetadata`;
- generated private `fabric-driver-lanes.json`.

The mise task `fabric-lane-check-older` runs a representative older compile and
metadata generation for:

- Minecraft `1.20.6`
- Yarn `1.20.6+build.3`
- Fabric Loader `0.19.3`
- Fabric API `0.100.8+1.20.6`
- Java major `21`

## Acceptance Criteria

- A red source-level test fails before implementation because
  `driver-fabric/build.gradle.kts` does not read the lane property names.
- After implementation, existing default current-lane tests still pass.
- `mise run fabric-lane-check-older` compiles or produces a precise compile
  blocker for the representative older lane without editing source files.
- `mise run package-cli` still packages only the default current lane and its
  manifest metadata.
- `git diff --check` and `mise run ci` pass locally before commit.
