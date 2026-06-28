# Local Smoke Default Latest Alias Evidence

Phase: 116

## Red

Command:

```sh
mise exec -- gradle :testkit:test --tests '*LocalMinecraftServerSmokeTest.local server smoke is disabled by default*'
```

Result:

- Exit code: 1
- Failure reason: `LocalMinecraftServerSmokeConfig.fromEnvironment(emptyMap())`
  still returned `minecraftVersion = "1.21.6"`.

## Green

Command:

```sh
mise exec -- gradle :testkit:test --tests '*LocalMinecraftServerSmokeTest.*'
```

Result:

- Exit code: 0
- The local server smoke config default is now `latest-release`.
- Explicit `CRAFTLESS_SMOKE_MINECRAFT_VERSION=1.21.6` override behavior remains
  covered by the existing environment parsing test.

## Local Gates

Commands:

```sh
git diff --check
mise run ci
```

Results:

- `git diff --check`: exit code 0.
- `mise run ci`: exit code 0.
- `mise run ci` completed:
  - `mise exec -- gradle lint`
  - `mise run unused-check`
  - `mise exec -- gradle test`
  - `mise exec -- bun test playwright`
- Bun reported 18 passing tests across the Playwright helper suite.

## Scope Guard

This phase only updates the active local server smoke default. It does not add
a compiled Fabric lane, public gameplay action, generated route family, CLI
gameplay catalog, Fabric gameplay binding, scenario shortcut, public
version-specific API, runnable latest/older lane, or new Minecraft support
claim.
