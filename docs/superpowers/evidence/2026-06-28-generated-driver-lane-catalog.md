# Generated Driver Lane Catalog Evidence

## Scope

Phase 128 moves Fabric driver manifest packaging to a generated internal lane
catalog owned by `driver-fabric`. The catalog currently contains only the
existing compiled lane and does not claim new Minecraft/Fabric version support.
No gameplay APIs, action descriptors, route families, CLI gameplay catalogs, or
scenario shortcuts were added.

## Red Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric build generates driver lane catalog for distribution packaging*'`
  - Failed before implementation because `driver-fabric/build.gradle.kts` had
    no `writeFabricDriverLaneCatalog`, `fabric-driver-lanes.json`, or
    `distributionPath` ownership.
- `mise exec -- bun test playwright/src/distribution.test.ts`
  - Failed before implementation because `cli/build.gradle.kts` did not
    consume `fabric-driver-lanes.json` and still used direct single-lane
    `extensions.extraProperties[...]` reads.

## Green Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric build generates driver lane catalog for distribution packaging*'`
  - Passed after `writeFabricDriverLaneCatalog` was added.
- `mise exec -- bun test playwright/src/distribution.test.ts`
  - Passed after CLI manifest generation consumed the generated catalog.
- `mise exec -- gradle :cli:writeDriverModManifest`
  - Passed and ran `:driver-fabric:writeFabricDriverLaneCatalog` first.

## Generated Artifacts

`driver-fabric/build/generated/driver-lanes/fabric-driver-lanes.json` and
`cli/build/generated/driver-mods/driver-mods.json` both contained the current
single Fabric lane:

- `minecraftVersion`: `1.21.6`
- `loaderVersion`: `0.19.3`
- `fabricApiVersion`: `0.128.2+1.21.6`
- `javaMajorVersion`: `21`
- `path`: `mods/craftless-driver-fabric.jar`
- `providerId`: `fabric-current-lane`

## Local CI Evidence

- `mise run package-cli`
  - Passed. `:driver-fabric:writeFabricDriverLaneCatalog` and
    `:cli:writeDriverModManifest` ran before distribution archives were
    produced, and the package checks still found
    `/mods/craftless-driver-fabric.jar` plus `/driver-mods.json`.
- `git diff --check`
  - Passed.
- `mise run ci`
  - Passed locally. This ran Gradle lint, unused-check/detekt, Gradle tests,
    and Bun Playwright tests through mise.
