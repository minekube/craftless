# Shared Fabric Client State Graph Projection Plan

## Spec

`docs/superpowers/specs/2026-06-28-156-shared-fabric-client-state-graph-projection-design.md`

## Tasks

1. Add shared discovery tests for connected and disconnected
   `FabricClientStateGraphSnapshot` projection.
2. Add architecture guards that prevent the Yarn/remap lane from owning generic
   client-state resource/handle projection and require the official lane to use
   the shared helper.
3. Run the focused tests and confirm the expected red failures.
4. Implement `FabricClientStateGraphSnapshot` and
   `fabricClientStateGraphFragment` in `driver-fabric-discovery`.
5. Wire the Yarn/remap client-state probe to map live client-thread state into
   the shared snapshot and compose operations/events separately.
6. Wire the latest/current official lane to compose a disconnected client-state
   fragment without adding gameplay actions or packaging support claims.
7. Update AGENTS, README, checklist, and evidence so future agents keep the
   generic, version-agnostic boundary clear.
8. Run focused tests, lint, diff checks, and the enabled official attach probe.
9. Commit and push directly to `main`.
