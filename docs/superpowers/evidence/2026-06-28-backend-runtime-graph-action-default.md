# Phase 167 Backend Runtime Graph Action Default Evidence

Phase 167 makes the shared backend contract graph-native for action discovery.
`DriverBackend.actions(clientId)` now projects descriptors from
`runtimeGraph(clientId).operations` by default, and the Fabric backend no
longer carries a duplicate graph-to-action override.

## Red Check

Command:

```sh
mise exec -- gradle :driver-runtime:test --tests '*BackendDriverSessionTest.driver backend default actions derive from runtime graph operations*'
```

Observed before implementation:

```text
BackendDriverSessionTest > driver backend default actions derive from runtime graph operations() FAILED
java.util.NoSuchElementException at BackendDriverSessionTest.kt:244
```

This proved `BackendDriverSession.actions()` still returned an empty list for a
backend that exposed `inventory.query` through `runtimeGraph(clientId)` but did
not override `actions(clientId)`.

## Implementation Evidence

- `DriverBackend.actions(clientId)` now sorts
  `runtimeGraph(clientId).operations` and maps each operation through
  `toDriverActionDescriptor()`.
- `BackendDriverSession.actions()` remains a backend delegation point so
  explicit backend overrides still work where they are intentional.
- `FabricDriverBackend` no longer overrides `actions(clientId)` just to repeat
  runtime graph projection.
- This phase adds no gameplay operation, public route, CLI command, adapter,
  static catalog, scenario shortcut, version lane, or support claim.

## Verification

Command:

```sh
mise exec -- gradle :driver-runtime:test --tests '*BackendDriverSessionTest.driver backend default actions derive from runtime graph operations*'
```

Result:

```text
BUILD SUCCESSFUL
10 actionable tasks: 4 executed, 6 up-to-date
```

Command:

```sh
mise exec -- gradle :driver-runtime:test :driver-fabric:test
```

Result:

```text
BUILD SUCCESSFUL
23 actionable tasks: 5 executed, 18 up-to-date
```

Command:

```sh
mise run ci
```

Result:

```text
BUILD SUCCESSFUL
lint, unused-check, Gradle test, and Bun Playwright tests passed.
19 Bun tests passed, 0 failed.
```

## Boundary

Phase 167 removes another action-list fallback. It does not prove final
latest/current runtime support, representative older support, generated
gameplay breadth, or honest survival gameplay completion.
