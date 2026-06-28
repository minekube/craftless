# Packaged Driver Mod Manifest Evidence

Date: 2026-06-28

## Red

Command:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.server start uses packaged driver mod manifest when env is absent*'
```

Observed failure:

```text
CraftlessCliTest > server start uses packaged driver mod manifest when env is absent() FAILED
```

The CLI copied the fallback packaged driver mod because `server start` did not
auto-discover `driver-mods.json`.

Command:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Observed failure:

```text
Expected to contain: "driver-mods.json"
```

The distribution guard proved the CLI build did not package a driver-mod
manifest.

## Green

Command:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.server start uses packaged driver mod manifest when env is absent*' --tests '*CraftlessCliTest.server start uses packaged fabric driver mod when env is absent*'
```

Observed:

```text
BUILD SUCCESSFUL in 6s
12 actionable tasks: 4 executed, 8 up-to-date
```

Command:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Observed:

```text
10 pass
0 fail
56 expect() calls
```

## Package Smoke

Command:

```sh
mise run package-cli
```

Observed:

```text
BUILD SUCCESSFUL in 12s
```

The task verified both release archive shapes:

```text
tar -tf cli/build/distributions/craftless-*.tar | grep -q '/mods/craftless-driver-fabric.jar$'
tar -tf cli/build/distributions/craftless-*.tar | grep -q '/driver-mods.json$'
jar tf cli/build/distributions/craftless-*.zip | grep -q '/mods/craftless-driver-fabric.jar$'
jar tf cli/build/distributions/craftless-*.zip | grep -q '/driver-mods.json$'
```

Manual archive/staging inspection found:

```text
craftless-0.1.0-SNAPSHOT/driver-mods.json
craftless-0.1.0-SNAPSHOT/mods/craftless-driver-fabric.jar
```

Staged Docker context manifest:

```json
{
  "entries": [
    {
      "loader": "FABRIC",
      "minecraftVersion": "1.21.6",
      "loaderVersion": "0.19.3",
      "path": "mods/craftless-driver-fabric.jar"
    }
  ]
}
```

## Local Gates

Command:

```sh
git diff --check
```

Observed: exit 0.

Command:

```sh
mise run ci
```

Observed:

```text
BUILD SUCCESSFUL in 2s
BUILD SUCCESSFUL in 489ms
BUILD SUCCESSFUL in 4s
17 pass
0 fail
```

## Scope Boundary

This phase packages and auto-discovers the driver-mod manifest for installed
CLI distributions. It does not add a new compiled Fabric lane, public gameplay
action, generated route family, CLI gameplay catalog, scenario shortcut, or
broad Minecraft support claim.
