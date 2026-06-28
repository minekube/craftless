# Shared Fabric Client State Graph Projection Design

## Problem

The latest/current official Fabric lane now shares runtime, registry, event,
and graph composition infrastructure with the Yarn/remap lane. Client state
resource and handle projection is still embedded in the Yarn/remap
`FabricClientStateCapabilityProbe` even though the graph shape is generic:
resources such as `client`, `player`, `inventory`, `recipe`, `world`,
`entity`, and `screen`, plus Craftless-owned handles for inventory, recipe,
world blocks, and entities.

This projection is not gameplay action breadth. It is generated runtime
evidence that tells clients whether state resources are available,
unavailable, or not yet connected.

## Goal

Move non-gameplay Fabric client-state resource/handle projection into
`driver-fabric-discovery`:

- a shared `FabricClientStateGraphSnapshot` data type;
- a shared `fabricClientStateGraphFragment(snapshot)` helper;
- Yarn/remap lane still probes the actual Minecraft client state on the
  client thread, but delegates resource/handle projection to the shared helper;
- official lane composes runtime, registry, event, and disconnected
  client-state fragments through the shared graph composer.

The official lane may expose unavailable client-state resources and handles.
That is evidence progress and must not be treated as latest/current gameplay
support.

## Non-Goals

- Do not add public gameplay actions.
- Do not move Fabric/Minecraft client state inspection into
  `driver-fabric-discovery`.
- Do not move or widen bootstrap operation definitions.
- Do not add static action descriptors or gameplay catalogs.
- Do not add a packaged `26.2` driver manifest entry.
- Do not claim latest/current gameplay support.

## Design

Create `FabricClientStateGraphSnapshot.kt` in `driver-fabric-discovery`.

The helper should return a `FabricRuntimeGraphFragment` containing:

- resources `client`, `player`, `inventory`, `recipe`, `world`, `entity`,
  and `screen`;
- handles `inventory.slot`, `recipe.handle`, `world.block.handle`, and
  `entity.handle`;
- availability derived from a lane-provided snapshot:
  - disconnected client: `client-not-connected` for client/player/inventory,
    recipe/world/entity and handles;
  - connected but missing state: specific reasons such as
    `player-unavailable`, `inventory-unavailable`,
    `recipe-discovery-unavailable`, `world-unavailable`, and
    `camera-unavailable`;
  - screen resource is always available because screen state is queryable even
    when no screen is open.

`driver-fabric` should continue to derive booleans from actual Minecraft
objects on the client thread. It should map its lane-local snapshot into
`FabricClientStateGraphSnapshot` and compose operations/events separately.

`driver-fabric-official` should use
`FabricClientStateGraphSnapshot.disconnected()` for now. Its generated
OpenAPI should then report unavailable client-state resources and handles
while actions remain `0`.

## Acceptance

- Shared tests prove a connected snapshot emits the expected resources and
  handles with available state.
- Shared tests prove a disconnected snapshot marks client-state resources and
  handles unavailable with `client-not-connected` while `screen` remains
  available.
- Architecture guard proves the Yarn/remap lane no longer hand-builds
  `RuntimeResourceNode("client"...` or `RuntimeHandleNode(id =
  "inventory.slot"...`.
- Architecture guard proves the official backend uses
  `fabricClientStateGraphFragment` and still does not import
  `RuntimeCapabilityGraph`.
- Focused Fabric/discovery/official tests pass.
- The real enabled official attach probe still observes `client.attached`.
- The official OpenAPI artifact reports `actions=0` and client-state
  resources/handles as unavailable evidence.
- No public gameplay action, packaged 26.x manifest entry, static gameplay
  catalog, scenario shortcut, or latest/current support claim is added.
