# Catalog-Driven Driver Artifact Staging Evidence

## Scope

Phase 129 makes CLI distribution artifact staging consume private
`artifactKey` and `distributionPath` metadata from the generated Fabric driver
lane catalog. The catalog still contains only the current compiled lane. This
does not add a compiled lane, change Fabric versions, claim latest/older
support, or add gameplay APIs/actions/routes/scenario shortcuts.

## Red Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric build generates driver lane catalog for distribution packaging*'`
  - Failed before implementation because the generated catalog did not include
    `artifactKey` / `fabric-current-remap-jar`.
- `mise exec -- bun test playwright/src/distribution.test.ts`
  - Failed before implementation because `cli/build.gradle.kts` did not use
    `JsonSlurper`, had no `stageFabricDriverLaneArtifacts`, and still staged
    artifacts through a hard-coded `into("mods")` block.

## Green Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric build generates driver lane catalog for distribution packaging*'`
  - Passed after the catalog emitted `artifactKey`.
- `mise exec -- bun test playwright/src/distribution.test.ts`
  - Passed after CLI packaging used catalog-driven staging.
- `mise exec -- gradle :cli:stageFabricDriverLaneArtifacts :cli:writeDriverModManifest`
  - Passed. The task ran `:driver-fabric:writeFabricDriverLaneCatalog`,
    `:driver-fabric:remapJar`, staged the driver jar, and generated the
    provider manifest.

## Artifact Evidence

Generated catalog and manifest both contained the current lane with:

- `minecraftVersion`: `1.21.6`
- `loaderVersion`: `0.19.3`
- `artifactKey`: `fabric-current-remap-jar`
- `distributionPath`: `mods/craftless-driver-fabric.jar`

The staged artifact existed at
`cli/build/generated/driver-lane-artifacts/mods/craftless-driver-fabric.jar`
and included Fabric mod contents such as `fabric.mod.json`.

## Local CI Evidence

- `jar tf cli/build/generated/driver-lane-artifacts/mods/craftless-driver-fabric.jar | grep -q '^fabric.mod.json$'`
  - Passed.
- `mise run package-cli`
  - Passed. Built CLI `distZip` and `distTar`, verified packaged driver mod
    contents, refreshed `build/docker/craftless`, and verified the Docker
    staging driver jar contents.
- `git diff --check`
  - Passed.
- `mise run ci`
  - Passed. Ran lint, unused/dead-code checks through detekt, Gradle tests, and
    Bun Playwright helper/distribution tests.
