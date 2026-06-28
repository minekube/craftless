# Catalog-Driven Driver Artifact Staging Design

## Problem

Phase 128 made `driver-fabric` generate the lane catalog consumed by
`driver-mods.json`, but `cli` distribution packaging still stages the driver
jar through a hard-coded `into("mods") { rename { ... } }` block. That means
the catalog owns manifest data but not the packaged artifact path. Adding a
second driver lane later would still require a separate hard-coded packaging
path.

The next step is to make the generated catalog describe which built artifact
key should be staged at each distribution path, then let `cli` stage driver
artifacts by reading the catalog.

## Goals

- Add an internal `artifactKey` to the generated Fabric driver lane catalog.
- Make `cli` parse the generated catalog with a structured JSON parser during
  the build.
- Stage driver artifacts into the CLI distribution from catalog
  `distributionPath` values.
- Preserve the existing current-lane artifact path
  `mods/craftless-driver-fabric.jar`.
- Keep `driver-mods.json` manifest generation catalog-owned.

## Non-Goals

- Do not add another compiled Fabric lane.
- Do not change Fabric Loom dependency versions.
- Do not claim latest/current or older-version support.
- Do not add public gameplay APIs, generated route families, CLI gameplay
  catalogs, Fabric descriptor/binding pairs, or scenario shortcuts.
- Do not remove manual environment override fallback behavior.

## Acceptance Criteria

- A red Fabric build guard fails before implementation because the generated
  catalog has no `artifactKey`.
- A red distribution guard fails before implementation because CLI packaging
  does not parse the catalog with `JsonSlurper` and does not stage artifacts
  through `stageFabricDriverLaneArtifacts`.
- After implementation, the generated catalog entry includes
  `"artifactKey": "fabric-current-remap-jar"`.
- `cli` stages the remapped driver jar under
  `build/generated/driver-lane-artifacts/mods/craftless-driver-fabric.jar`
  based on catalog `distributionPath`.
- CLI distributions still contain `driver-mods.json` and
  `mods/craftless-driver-fabric.jar`.
- Focused tests/tasks, `mise run package-cli`, `git diff --check`, and
  `mise run ci` pass locally.
