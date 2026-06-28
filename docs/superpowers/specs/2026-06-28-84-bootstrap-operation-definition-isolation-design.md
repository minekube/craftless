# Bootstrap Operation Definition Isolation Design

## Problem

Phase 83 removed public descriptor/schema ownership from private Fabric
execution bindings, but `FabricClientStateCapabilityProbe` still embeds a
catalog-shaped block of bootstrap operation ids, adapter ids, and schemas while
also querying live client state. That mixes two responsibilities:

- live client state/resource availability probing;
- transitional bootstrap operation definition projection.

The broader completion blocker remains that future gameplay breadth still
depends on hand-maintained bootstrap operation definitions instead of generic
runtime discovery. Until that blocker is fully removed, the bootstrap
definitions should live behind an explicit graph-discovery boundary and the
client-state probe should not be the place future agents add gameplay ids.

## Goals

- Move the current bootstrap operation ids, adapter ids, and argument/result
  schemas out of `FabricCapabilityProbe.kt`.
- Introduce an explicit internal bootstrap operation definition layer that
  creates `RuntimeOperationNode` values from definitions plus live
  availability.
- Keep `FabricClientStateCapabilityProbe` focused on client resources, handles,
  capability snapshot, and availability calculation.
- Add source guards that prevent `FabricClientStateCapabilityProbe` from
  owning bootstrap operation ids, Fabric adapter ids, or `RuntimeOperationNode`
  construction.
- Keep generated public actions, OpenAPI, invocation, and final behavior
  unchanged.

## Non-Goals

- Do not add new gameplay actions, route families, CLI commands, generated
  aliases, Fabric execution adapters, version lanes, or support claims.
- Do not claim the broader binding-exit work is finished. This phase still
  leaves transitional bootstrap operation definitions in code.
- Do not replace generic future runtime discovery with another durable static
  catalog. The new definition layer is transitional and must stay named and
  documented as such.

## Acceptance Criteria

- `FabricCapabilityProbe.kt` no longer contains bootstrap operation ids such as
  `player.chat`, adapter ids such as `fabric.player-chat`, or direct
  `RuntimeOperationNode` construction for bootstrap gameplay operations.
- A focused internal file owns transitional bootstrap operation definitions and
  converts them into graph operation nodes.
- Operation availability is selected from live capability snapshot state, not
  hard-coded as always available.
- Existing runtime graph, generated action descriptor, OpenAPI, and private
  adapter behavior remain unchanged.
- AGENTS, checklist, plan, and evidence record Phase 84 and keep the broader
  generated-discovery blocker active.
