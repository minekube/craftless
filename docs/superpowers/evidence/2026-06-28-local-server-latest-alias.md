# Local Server Latest Alias Evidence

Phase: 115

## Red

Command:

```sh
mise exec -- gradle :testkit:test --tests '*MinecraftServerJarProvisionerTest.fixture provisions latest release server jar under resolved version*'
```

Result:

- Exit code: 1
- Failure reason: `MinecraftServerJarProvisioner` looked up
  `latest-release` as an exact Mojang version id and failed before requesting
  concrete `26.2` metadata.

## Green

Commands:

```sh
mise exec -- gradle :testkit:test --tests '*MinecraftServerJarProvisionerTest.*'
mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest.*latest*'
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.runtimes java resolve resolves latest release alias through supervisor api*'
```

Results:

- `:testkit:test` focused provisioner tests: exit code 0.
- `:daemon:test` cache alias regression guard: exit code 0.
- `:cli:test` Java runtime alias regression guard: exit code 0.

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

This phase only updates shared Mojang version-index parsing and local server
jar provisioning. It does not add a compiled Fabric lane, public gameplay
action, generated route family, CLI gameplay catalog, Fabric gameplay binding,
scenario shortcut, public version-specific API, runnable latest/older lane, or
new Minecraft support claim.
