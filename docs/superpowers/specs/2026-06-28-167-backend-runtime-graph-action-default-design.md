# Backend Runtime Graph Action Default Design

## Goal

Make the shared `DriverBackend` contract derive actions from
`runtimeGraph(clientId).operations` by default so backend sessions do not fall
back to an empty action list when a backend already exposes runtime graph
operations.

## Problem

Phase 166 made `DriverSession.actions()` graph-derived by default, but
`BackendDriverSession.actions()` still delegates to `DriverBackend.actions()`.
`DriverBackend.actions()` currently defaults to `emptyList()`, so a backend can
publish operations in its runtime graph and still expose no actions unless it
also hand-writes an action projection override.

`FabricDriverBackend.actions(clientId)` repeats the same graph-to-action
projection that now belongs in shared API/runtime code.

## Design

- Add a driver-runtime contract test with a backend that does not override
  `actions(clientId)` but does expose one runtime operation in
  `runtimeGraph(clientId)`.
- Make `DriverBackend.actions(clientId)` default to sorted
  `runtimeGraph(clientId).operations.map { it.toDriverActionDescriptor() }`.
- Keep `BackendDriverSession.actions()` delegating to the backend so existing
  explicit backend overrides keep working.
- Remove the redundant `FabricDriverBackend.actions(clientId)` override and
  its now-unused imports.
- Do not add gameplay operations, routes, CLI commands, adapters, scenario
  shortcuts, version lanes, or support claims.

## Acceptance

- The new driver-runtime test fails before implementation because the default
  backend action list is empty even though the graph contains an operation.
- The test passes after implementation and verifies id, source, availability,
  argument schema, and result schema projection.
- `FabricDriverBackend` no longer owns a graph-to-action action override.
- Focused `driver-runtime` and `driver-fabric` tests pass.
- `mise run ci` and `git diff --check` pass.
