# Game-scoped Fabric loader matrix evidence

Phase 205 closes a matrix mismatch between `/versions/support-targets` and
`POST /clients`.

## Behavior

- `VersionDiscoveryService` now fetches Fabric Loader compatibility from
  `/versions/loader/{minecraftVersion}` for game targets with configured driver
  rows.
- Game targets without configured driver rows stay explicitly unsupported with
  `NO_DRIVER_MOD` without extra per-game metadata fetches.
- Runtime targets are generated from the union of global loader versions,
  game-scoped loader versions, and explicit manifest loader versions.
- A globally listed loader version that Fabric does not list for a specific
  Minecraft target is visible but unsupported with
  `NO_COMPATIBLE_FABRIC_LOADER`.
- Wildcard driver manifest rows are projected only onto game-compatible loader
  versions.

## Red

```bash
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.support targets reject globally listed loader versions missing from game metadata'
```

Failed before implementation because the endpoint marked the globally listed
`0.19.2` loader as supported for `1.21.6`.

```bash
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.support targets do not fetch game loader metadata for targets without driver rows'
```

Failed before the fanout fix because the endpoint tried to fetch game-scoped
loader metadata for a target that had no configured Craftless driver rows.

## Green

```bash
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.support targets reject globally listed loader versions missing from game metadata'
```

Passed after the matrix began using game-scoped loader compatibility.

```bash
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.support targets do not fetch game loader metadata for targets without driver rows'
```

Passed after no-driver targets stopped fetching unnecessary per-game loader
metadata.

```bash
mise exec -- gradle :daemon:test
```

Passed.
