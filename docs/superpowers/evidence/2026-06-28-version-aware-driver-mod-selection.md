# Version-Aware Driver Mod Selection Evidence

Date: 2026-06-28

## Red

Command:

```sh
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.prepared runtime asks driver mod provider for requested runtime lane*'
```

Observed failure:

```text
Unresolved reference 'ClientRuntimeDriverModRequest'.
```

The failing test proved the production provider boundary was still
loader-only.

## Green

Command:

```sh
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.prepared runtime asks driver mod provider for requested runtime lane*'
```

Observed:

```text
BUILD SUCCESSFUL in 5s
14 actionable tasks: 4 executed, 10 up-to-date
```

The focused test verifies that client creation for Fabric `1.21.6` passes a
`ClientRuntimeDriverModRequest` containing loader `FABRIC`, Minecraft version
`1.21.6`, and resolved loader version `0.17.2` into the driver-mod provider.

## Compatibility

Command:

```sh
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.prepared runtime launch plan includes configured craftless fabric driver mod*' --tests '*LocalSessionApiServerTest.prepared runtime asks driver mod provider for requested runtime lane*'
```

Observed:

```text
BUILD SUCCESSFUL in 2s
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
BUILD SUCCESSFUL in 425ms
BUILD SUCCESSFUL in 14s
16 pass
0 fail
```

## Scope Boundary

This phase adds the version-aware driver-mod selection boundary required by
future multi-lane runtime artifacts. It does not add a new compiled Fabric
lane, public gameplay action, generated route family, CLI gameplay catalog,
scenario shortcut, or broad Minecraft support claim.
