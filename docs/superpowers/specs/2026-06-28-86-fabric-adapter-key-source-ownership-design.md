# Fabric Adapter Key Source Ownership Design

## Problem

Phase 84 and Phase 85 made bootstrap operation ids graph-owned and stopped
private bindings from owning their own operation-id list. `FabricDriverBackend`
still repeats several private Fabric adapter keys directly:

- `fabric.entity-query`
- `fabric.entity-attack`
- `fabric.world-block-query`
- `fabric.recipe-query`
- `fabric.recipe-craft`

Those adapter keys are already part of the runtime operation definitions. A
backend-side literal map can drift from the graph definitions and keeps another
catalog-shaped maintenance point alive.

## Goals

- Make Fabric bootstrap adapter ids explicit constants in the bootstrap
  definition layer.
- Update `FabricDriverBackend` to register bootstrap operation adapters through
  those constants instead of string literals.
- Keep operation ids, generated action descriptors, OpenAPI output, and
  invocation behavior unchanged.
- Add tests that fail if backend code reintroduces duplicated `fabric.*`
  adapter-key literals.

## Non-Goals

- Do not add gameplay actions, route families, CLI commands, generated aliases,
  Fabric execution adapters, version lanes, or support claims.
- Do not remove private execution adapters yet.
- Do not claim the broader generated-discovery exit is complete.

## Acceptance Criteria

- `FabricDriverBackend.kt` no longer contains private gameplay adapter literals
  such as `fabric.entity-query` or `fabric.recipe-craft`.
- Bootstrap operation definitions and backend adapter registration use
  `FabricBootstrapOperationAdapters` constants.
- Existing graph-owned invocation tests still prove graph operations dispatch to
  the correct private adapters.
- AGENTS, checklist, plan, and evidence record Phase 86 and keep the broader
  generated-discovery blocker active.
