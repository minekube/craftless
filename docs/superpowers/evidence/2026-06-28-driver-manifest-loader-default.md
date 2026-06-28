# Driver Manifest Loader Default Evidence

## Scope

Phase 126 keeps packaged driver-mod manifest defaults in the supervisor/runtime
layer. It lets an exact Minecraft Fabric create-client request without
`loaderVersion` use the manifest-backed Fabric Loader version before cache
preparation. It does not add gameplay actions, static route families, CLI
gameplay catalogs, compiled lanes, public support claims, or scenario
shortcuts.

## Red Evidence

- `mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.*preferred loader*' --tests '*LocalSessionApiServerTest.*defaults loader version from driver mod provider preference*'`
  - Failed before implementation at `ConfiguredClientRuntimeDriverModProviderTest.kt`
    with unresolved `preferredLoaderVersion`.
- `mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*defaults loader version from packaged driver mod manifest*'`
  - Failed before implementation because the packaged create-client path did
    not return HTTP 201 for the manifest-backed `0.16.14` default lane.

## Green Evidence

- `mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.*preferred loader*' --tests '*LocalSessionApiServerTest.*defaults loader version from driver mod provider preference*'`
  - Passed after `ClientRuntimeDriverModProvider.preferredLoaderVersion(...)`
    and `WorkspaceClientRuntimeDriverFactory` cache-preparation wiring landed.
- `mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*defaults loader version from packaged driver mod manifest*'`
  - Passed after packaged distributions selected the manifest-backed Fabric
    Loader version before cache preparation.

## Local CI Evidence

- `git diff --check`
  - Passed.
- `mise run ci`
  - Passed locally. This ran `mise exec -- gradle lint`,
    `mise run unused-check`, `mise exec -- gradle test`, and
    `mise exec -- bun test playwright`.
