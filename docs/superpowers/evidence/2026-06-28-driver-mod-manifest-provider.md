# Driver Mod Manifest Provider Evidence

Date: 2026-06-28

## Red

Command:

```sh
mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest*'
```

Observed failure:

```text
Unresolved reference 'CRAFTLESS_DRIVER_MOD_MANIFEST'.
```

The failing tests proved the configured provider did not yet expose a
manifest-backed selection path.

## Green

Command:

```sh
mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest*'
```

Observed:

```text
BUILD SUCCESSFUL in 1s
14 actionable tasks: 4 executed, 10 up-to-date
```

The tests verify that an exact manifest entry for loader `FABRIC`, Minecraft
version `1.21.6`, and loader version `0.17.2` wins over
`CRAFTLESS_FABRIC_DRIVER_MOD`, and that a missing manifest lane falls back to
the single Fabric driver mod path.

## Compatibility

Command:

```sh
mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest*' --tests '*LocalSessionApiServerTest.prepared runtime asks driver mod provider for requested runtime lane*'
```

Observed:

```text
BUILD SUCCESSFUL in 1s
14 actionable tasks: 1 executed, 13 up-to-date
```

Command:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.server start forwards configured fabric driver mod environment*' --tests '*CraftlessCliTest.server start uses packaged fabric driver mod when env is absent*'
```

Observed:

```text
BUILD SUCCESSFUL in 2s
12 actionable tasks: 3 executed, 9 up-to-date
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
BUILD SUCCESSFUL in 399ms
BUILD SUCCESSFUL in 14s
16 pass
0 fail
```

## Scope Boundary

This phase adds manifest-backed local driver-mod selection. It does not add a
new compiled Fabric lane, public gameplay action, generated route family, CLI
gameplay catalog, scenario shortcut, or broad Minecraft support claim.
