# Launch Mod Materialization Evidence

## Scope

Phase 95 materializes cached Fabric mod artifacts from `CacheLaunchPlan.mods`
into an instance `mods` directory before process launch. This makes the Fabric
API cache resolution from Phase 94 launch-relevant without adding a new
compiled lane or claiming new Minecraft client support.

## Red Evidence

- `mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.process client runtime launcher starts prepared command*'`
  failed before implementation with `NoSuchFileException` for the expected
  materialized Fabric API jar under the instance mods directory.

## Green Evidence

- `mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.process client runtime launcher starts prepared command*'`
  passed after `ProcessClientRuntimeLauncher` copied launch mod handles into
  the instance mods directory before starting the process.

## Local Final Gates

- `git diff --check`
  passed.
- `mise exec -- gradle :daemon:test`
  passed.
- `mise exec -- gradle :daemon:ktlintCheck :daemon:detekt`
  passed.

## Remote CI

Not waited on during active development. Local forced CI is the working gate;
remote CI may continue in the background after push.

## Push Evidence

- `1d5630d daemon: materialize launch mods` pushed to `origin/main`.

## Notes

- This phase does not add latest/current or older-version support by itself.
- Future provider-backed lanes can now rely on the generic launch plan to
  materialize resolved Fabric mods.
