# Alias Driver Manifest Loader Default Evidence

## Scope

Phase 127 resolves Minecraft version aliases before asking the configured
driver-mod manifest for a default Fabric Loader version. This stays in
supervisor/runtime preparation and does not add gameplay actions, static route
families, CLI gameplay catalogs, compiled lanes, support claims, or scenario
shortcuts.

## Red Evidence

- `mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.*resolves aliases before driver mod provider preference*'`
  - Failed before implementation at the expected create-client assertion
    because `latest-release` did not select the manifest-backed loader lane.
- `mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*resolves aliases before packaged driver mod manifest defaults*'`
  - Failed before implementation at the expected create-client assertion.

## Debugging Evidence

- After the daemon implementation went green, the CLI alias test still failed
  with `minecraft version index is missing latest aliases`.
- Root cause: the shared CLI fake metadata fixture did not include the
  `latest.release` / `latest.snapshot` object required for alias resolution.
- Fix: update the fixture to match Mojang version index shape so the test
  exercises the production alias and manifest-default path.

## Green Evidence

- `mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.*resolves aliases before driver mod provider preference*' --tests '*LocalSessionApiServerTest.*defaults loader version from driver mod provider preference*'`
  - Passed after `CachePreparationService.resolveMinecraftVersionAlias(...)`
    and factory resolved-version preference retry landed.
- `mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*resolves aliases before packaged driver mod manifest defaults*' --tests '*CraftlessCliTest.*defaults loader version from packaged driver mod manifest*'`
  - Passed after the alias resolution implementation and corrected CLI fake
    metadata fixture.

## Local CI Evidence

- `git diff --check`
  - Passed.
- `mise run ci`
  - Passed locally. This ran `mise exec -- gradle lint`,
    `mise run unused-check`, `mise exec -- gradle test`, and
    `mise exec -- bun test playwright`.
