# Game-scoped Fabric loader matrix evidence

Phase 205 closes a matrix mismatch between `/versions/support-targets` and
`POST /clients`.

## Behavior

- `VersionDiscoveryService` now fetches Fabric Loader compatibility per game
  target from `/versions/loader/{minecraftVersion}`.
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

## Green

```bash
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.support targets reject globally listed loader versions missing from game metadata'
```

Passed after the matrix began using game-scoped loader compatibility.

```bash
mise exec -- gradle :daemon:test
```

Passed.
