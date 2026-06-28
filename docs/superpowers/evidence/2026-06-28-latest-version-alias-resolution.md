# Latest Version Alias Resolution Evidence

Phase: 111

## Red

Command:

```sh
mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest.cache preparation resolves latest release alias before building cache handles*' --tests '*CachePreparationServiceTest.cache preparation resolves latest snapshot alias before building cache handles*'
```

Result:

- Exit code: 1
- `CachePreparationServiceTest.cache preparation resolves latest release alias before building cache handles` failed.
- `CachePreparationServiceTest.cache preparation resolves latest snapshot alias before building cache handles` failed.
- Failure reason: cache preparation searched `latest-release` and
  `latest-snapshot` as exact Mojang version ids.

## Green

Command:

```sh
mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest.*latest*'
```

Result:

- Exit code: 0
- `CachePreparationServiceTest.cache preparation resolves latest release alias before building cache handles` passed.
- `CachePreparationServiceTest.cache preparation resolves latest snapshot alias before building cache handles` passed.
- `CachePreparationServiceTest.fabric cache preparation resolves latest release before requesting fabric metadata` passed.

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

## Scope Guard

This phase only resolves supervisor cache-preparation aliases to concrete
Mojang version ids before downstream cache, Fabric metadata, Java runtime, and
launch metadata derivation.

It adds no compiled Fabric lane, public gameplay action, generated route
family, CLI gameplay catalog, Fabric gameplay binding, scenario shortcut,
public version-specific API, runnable latest/older lane, or new Minecraft
support claim.
