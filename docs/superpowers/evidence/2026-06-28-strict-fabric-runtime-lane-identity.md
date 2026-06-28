# Strict Fabric Runtime Lane Identity Evidence

Date: 2026-06-28

## Red

Command:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricCompatibilityMatrixTest.matrix rejects same game version with mismatched runtime identity*' --tests '*FabricCompatibilityMatrixTest.matrix does not select provider for mismatched runtime identity*'
```

Observed:

```text
FabricCompatibilityMatrixTest > matrix rejects same game version with mismatched runtime identity() FAILED
FabricCompatibilityMatrixTest > matrix does not select provider for mismatched runtime identity() FAILED
```

The failures proved the matrix was resolving a supported lane using only the
Minecraft game version.

## Green

Command:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricCompatibilityMatrixTest*'
```

Observed:

```text
BUILD SUCCESSFUL in 1s
17 actionable tasks: 4 executed, 13 up-to-date
```

The focused matrix suite verifies that:

- the current compiled runtime identity resolves to the current supported lane;
- unknown Minecraft versions still report `unsupported-version`;
- same-game-version runtime drift reports `unsupported-runtime-identity`;
- provider selection returns null for mismatched runtime identity.

## Affected Fixture Repair

Command:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricRuntimeProviderTest*' --tests '*FabricCurrentLaneRuntimeProviderTest*' --tests '*FabricDriverModuleTest.fabric backend runtime graph includes sanitized compatibility lane evidence*' --tests '*FabricCompatibilityMatrixTest*'
```

Observed:

```text
BUILD SUCCESSFUL in 2s
17 actionable tasks: 4 executed, 13 up-to-date
```

These tests verify that metadata-only and current-lane test fixtures now use
the generated compiled lane loader/API/mappings metadata instead of partial
placeholder identities.

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
BUILD SUCCESSFUL in 393ms
BUILD SUCCESSFUL in 4s
17 pass
0 fail
```

## Scope Boundary

This phase prevents false positives in Fabric runtime support selection. It
does not add a new compiled Fabric lane, public gameplay action, generated
route family, CLI gameplay catalog, scenario shortcut, or latest/current or
representative older support claim.
