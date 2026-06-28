# Client State Operation Discovery Design

## Problem

CL-02e requires at least one Fabric operation node to be proven from runtime
discovery inputs instead of the hand-maintained bootstrap list.

Before this phase, `world.time.query` was executable from the public generated
API, but its runtime graph node still came from
`fabricBootstrapOperationDefinitions()`. That left the bootstrap list as the
only source of gameplay operation shape for this operation.

## Design

Move the existing `world.time.query` runtime operation node out of bootstrap
definitions and into client-state discovery.

`FabricClientStateCapabilityProbe` already queries the live client on the
client thread and captures whether a world is present. That client-state
snapshot now projects the `world.time.query` operation with:

- resource `world.time`;
- private adapter key `fabric.world-time-query`;
- availability derived from the observed world state;
- `client-state` source evidence.

The private execution adapter remains unchanged. This phase changes the
operation node source, not gameplay behavior.

## Non-Goals

- Do not add a new gameplay operation.
- Do not add a CLI command or route family.
- Do not remove all bootstrap operation definitions.
- Do not claim CL-02 is complete.
- Do not make a new Minecraft version support claim.

## Acceptance

- `world.time.query` is absent from `fabricBootstrapOperationDefinitions()`.
- `defaultFabricCapabilityDiscovery(probes = listOf(FabricClientStateCapabilityProbe))`
  still emits `world.time.query`.
- The discovered operation carries `client-state` source evidence.
- Existing world time invocation behavior remains unchanged.
- Full `:driver-fabric:test` passes.
