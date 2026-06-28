# Official Fabric Connected Server Feature Metadata Design

## Goal

Make the latest/current official Fabric lane report server-feature runtime
metadata from the running official/Mojang-mapped client instead of continuing
to publish `server-features:not-connected` after a real connection has been
observed.

## Problem

Phase 158 proved that the official lane can launch, self-attach, connect to a
real local Minecraft 26.2 server, and project connected client-state resources
through shared Fabric graph infrastructure. The connected OpenAPI artifact
still reports:

```text
x-craftless-server-feature-fingerprint=server-features:not-connected
```

That is stale runtime metadata. It makes the generated runtime graph contradict
the connected client-state resources and weakens later generated-discovery and
compatibility evidence.

## Design

- Add a narrow official-lane server-feature provider that reads only lifecycle
  facts available from the official/Mojang-mapped `Minecraft` client.
- Feed the provider into the existing shared
  `FabricRuntimeMetadataSnapshot.serverFeatureFingerprint` path.
- Fingerprint stable Craftless-owned evidence strings such as connection state,
  server kind, and enabled feature-set identity.
- Keep registry discovery unavailable until a real official registry probe is
  added.
- Keep generated actions empty in this phase. This is runtime metadata and graph
  evidence only.

## Boundaries

- Do not add public gameplay actions, route families, CLI commands, scenario
  shortcuts, or action catalogs.
- Do not copy the Yarn/remap `FabricClientGateway` or operation adapters.
- Do not package the official lane as supported in `driver-mods.json`.
- Do not claim latest/current support. This phase fixes a metadata truth gap
  required by later generic discovery/invocation work.

## Acceptance

- A focused unit test fails before implementation because the official runtime
  metadata provider cannot accept lane-provided server-feature evidence.
- The official backend runtime graph uses the lane-provided server-feature
  fingerprint without adding operations.
- The connected official attach probe writes generated OpenAPI whose
  `x-craftless-server-feature-fingerprint` starts with `server-features:` and
  is not `server-features:not-connected`.
- The same OpenAPI still reports `actions=0`.
- Focused official-lane tests, latest official lane compile probe, and local CI
  pass through mise.
