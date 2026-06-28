# Shared Version Index Resolution Evidence

Phase: 113

## Red

Command:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.runtimes java resolve resolves latest release alias through supervisor api*'
```

Result:

- Exit code: 1
- `CraftlessCliTest.runtimes java resolve resolves latest release alias through supervisor api` failed.
- Failure reason: `JavaRuntimeService` looked up `latest-release` as an exact
  Mojang version id instead of resolving the alias first.

## Green

Commands:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.runtimes java resolve resolves latest release alias through supervisor api*'
mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest.*latest*'
```

Results:

- CLI Java runtime alias test: exit code 0.
- Cache preparation alias regression tests: exit code 0.

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

This phase only shares Mojang version-index parsing and alias resolution
between cache preparation and Java runtime resolution.

It adds no compiled Fabric lane, public gameplay action, generated route
family, CLI gameplay catalog, Fabric gameplay binding, scenario shortcut,
public version-specific API, runnable latest/older lane, or new Minecraft
support claim.
