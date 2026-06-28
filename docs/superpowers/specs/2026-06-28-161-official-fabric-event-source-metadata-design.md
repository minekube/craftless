# Official Fabric Event Source Metadata Design

## Goal

Make the latest/current official Fabric lane publish event-source evidence from
the running official/Mojang-mapped Fabric runtime so the shared event graph can
become available without adding gameplay actions or a copied Yarn/remap
gateway.

## Problem

After Phase 160, official 26.2 connected OpenAPI has discovered runtime,
registry, server-feature, and client-state metadata. The event graph still
reports:

```text
event.action      unavailable event-source-not-discovered
event.capability  unavailable event-source-not-discovered
event.lifecycle   unavailable event-source-not-discovered
```

That leaves the generated SSE/event surface incomplete for the latest/current
lane. The shared `fabricEventGraphFragment(...)` already owns the protocol
shape; the official lane needs lane-provided event-source evidence only.

## Design

- Add a narrow official-lane event-source provider that emits
  Craftless-owned source evidence.
- Include driver-level evidence and lifecycle callback names that the official
  Fabric lane can register through Fabric API.
- Register official Fabric lifecycle callbacks during official entrypoint
  startup to make the evidence true at runtime.
- Feed the evidence into the existing shared `fabricEventGraphFragment(...)`
  path and mark the event graph available only when evidence exists.
- Keep generated operations empty in this phase.

## Boundaries

- Do not add public gameplay actions, action descriptors, operation adapters,
  route families, CLI commands, scenario shortcuts, or static gameplay
  catalogs.
- Do not copy the Yarn/remap `FabricEventHooks`, mixins, `FabricClientGateway`,
  or `FabricOperationAdapters` into `driver-fabric-official`.
- Do not package the official lane as supported in `driver-mods.json`.
- Do not claim latest/current support. This phase is event-source metadata and
  shared graph availability only.

## Acceptance

- A focused test fails before implementation because the official backend
  cannot accept lane-provided event-source evidence.
- The official backend runtime graph marks the shared `event` resource and
  event nodes available when event-source evidence is present.
- The connected official attach probe writes generated OpenAPI whose
  `x-craftless-events` entries are available.
- The same OpenAPI still reports `actions=0`.
- Focused official-lane tests, latest official lane compile probe, and local CI
  pass through mise.
