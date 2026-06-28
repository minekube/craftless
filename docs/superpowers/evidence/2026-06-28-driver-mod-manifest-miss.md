# Driver Mod Manifest Miss Evidence

## Scope

Phase 125 makes configured driver-mod manifests authoritative for Fabric
runtime-lane selection. A packaged manifest miss fails client creation instead
of silently falling back to a single potentially incompatible Fabric driver
jar. This does not add gameplay APIs, compiled lanes, or support claims.

## Red

Provider command:

```sh
mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.*'
```

Result: failed before implementation because a configured manifest miss still
fell back to `CRAFTLESS_FABRIC_DRIVER_MOD`.

Packaged CLI command:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*driver mod manifest*'
```

Result: failed before implementation because a packaged manifest miss copied
the fallback driver jar and client creation succeeded.

## Green

Command:

```sh
mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.*' --tests '*LocalSessionApiServerTest.*driver mod manifest*' :cli:test --tests '*CraftlessCliTest.*driver mod manifest*'
```

Result: passed after making `CRAFTLESS_DRIVER_MOD_MANIFEST` authoritative for
Fabric lane selection and aligning the CLI test metadata fixture so the
manifest-miss test reaches driver-mod selection.

## Local Gates

Commands:

```sh
git diff --check
mise run ci
```

Result: passed. `mise run ci` completed Gradle lint, unused-check, Gradle
tests, and Bun Playwright tests successfully.
