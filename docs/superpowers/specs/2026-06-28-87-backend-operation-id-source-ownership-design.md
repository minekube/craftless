# Backend Operation Id Source Ownership Design

## Problem

Phase 85 moved bootstrap operation ids into the bootstrap definition layer, and
Phase 86 moved bootstrap adapter keys there as well. `FabricDriverBackend`
still repeats several bootstrap operation ids in adapter guard checks:

- `entity.query`
- `entity.attack`
- `world.block.query`
- `recipe.query`
- `recipe.craft`

Those checks are internal safety guards, but the duplicated literals still act
like a second operation catalog. Operation ids must remain owned by the runtime
graph/bootstrap definition source until the bootstrap actions are deleted in
favor of full runtime discovery.

## Goals

- Make backend operation-id guards reference `FabricBootstrapOperationIds`
  constants instead of raw public operation-id strings.
- Add a source guard that fails if `FabricDriverBackend.kt` reintroduces those
  bootstrap operation-id literals.
- Keep operation ids, adapter keys, generated OpenAPI, and invocation behavior
  unchanged.
- Keep this as cleanup of transitional bootstrap execution, not new public API
  breadth.

## Non-Goals

- Do not add gameplay actions, route families, CLI commands, generated aliases,
  Fabric execution adapters, version lanes, or support claims.
- Do not remove the private bootstrap execution adapters in this phase.
- Do not change navigation or task operation ownership in this phase.
- Do not claim the broader generated-discovery exit is complete.

## Acceptance Criteria

- `FabricDriverBackend.kt` no longer contains the raw bootstrap operation-id
  literals listed above.
- Backend operation guards use `FabricBootstrapOperationIds` constants.
- Existing graph-owned invocation tests still prove graph operations dispatch to
  the correct private adapters.
- AGENTS, checklist, plan, and evidence record Phase 87 and keep the broader
  generated-discovery blocker active.
