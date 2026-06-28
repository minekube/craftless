# Action Discovery Deletion Design

## Problem

Phase 79 made Fabric `invoke(...)` graph-owned, but the repository still
contains the standalone `FabricActionDiscovery` layer. That layer models
public action discovery as a separate probe/composition system returning
`DriverActionDescriptor` plus optional `FabricActionBinding`.

Keeping that file around is misleading because it preserves the old design
shape even when the backend no longer calls it. Future agents can mistake it
for the correct extension point and add one more hand-written gameplay action
inside the stale action system instead of improving the runtime capability
graph.

## Goals

- Delete standalone Fabric action discovery interfaces, probes, discovered
  action wrappers, and tests.
- Preserve the useful live-client capability snapshot model under runtime graph
  / capability-probe ownership.
- Add a source guard proving the stale action-discovery file and type names do
  not return.
- Keep `FabricDriverBackend.actions(...)` and `invoke(...)` graph-owned.
- Update AGENTS, checklist, plan, and evidence so the next phase is unambiguous.

## Non-Goals

- Do not add gameplay breadth.
- Do not add static public action descriptors, CLI gameplay catalogs, route
  families, scenario shortcuts, or Fabric descriptor/binding pairs.
- Do not complete the broader bootstrap binding exit. The current private
  `FabricActionBinding` implementations remain transitional execution adapters.
- Do not claim latest/older Minecraft version support from this cleanup.

## Acceptance Criteria

- `driver-fabric/src/main/kotlin/.../FabricActionDiscovery.kt` is removed.
- No main or test source references `FabricActionDiscovery`,
  `FabricActionProbe`, `FabricActionDiscoveryContext`,
  `FabricDiscoveredAction`, or `defaultFabricActionDiscovery` outside the
  source guard's forbidden-name list.
- A focused test fails before removal and passes after removal.
- Existing Fabric graph/action/invoke behavior remains green.
- Final local gates and pushed `main` CI are recorded in evidence.
