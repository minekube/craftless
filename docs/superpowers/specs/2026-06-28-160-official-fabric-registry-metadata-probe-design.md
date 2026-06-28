# Official Fabric Registry Metadata Probe Design

## Goal

Make the latest/current official Fabric lane publish runtime registry metadata
from the running official/Mojang-mapped Minecraft client so the shared registry
resource/handle graph can become available without adding gameplay actions.

## Problem

The connected official 26.2 OpenAPI now has truthful connected server-feature
metadata, but it still reports:

```text
x-craftless-registry-fingerprint=registries:not-discovered
registry.availability=unavailable
registry.availabilityReasons=["registry-not-discovered"]
```

That keeps a core discovery input unavailable for the latest/current lane. The
current Yarn/remap lane already fingerprints runtime registries and feeds them
through `FabricRuntimeMetadataSnapshot.registryFingerprint`; the official lane
needs the same system shape with only the official-mapping adapter isolated.

## Design

- Add a narrow official-lane registry provider that reads official/Mojang-mapped
  `BuiltInRegistries` keys.
- Feed the provider into the existing shared
  `FabricRuntimeMetadataSnapshot.registryFingerprint` path.
- Mark the shared `fabricRegistryGraphFragment(...)` available when the
  metadata fingerprint is no longer `registries:not-discovered`.
- Use Craftless-owned fingerprint evidence only; do not expose Minecraft class
  names, registry object types, or mapping names in public API metadata.
- Keep generated operations empty in this phase.

## Boundaries

- Do not add public gameplay actions, route families, CLI commands, scenario
  shortcuts, action descriptors, or operation adapters.
- Do not copy the Yarn/remap `FabricClientGateway` or `FabricOperationAdapters`.
- Do not package the official lane as supported in `driver-mods.json`.
- Do not claim latest/current support. This phase is registry metadata and
  shared graph availability only.

## Acceptance

- A focused unit test fails before implementation because the official runtime
  metadata provider cannot accept lane-provided registry entries.
- The official backend runtime graph marks `registry` and registry handles
  available from the shared registry graph when registry metadata is discovered.
- The connected official attach probe writes generated OpenAPI whose
  `x-craftless-registry-fingerprint` starts with `registries:` and is not
  `registries:not-discovered`.
- The same OpenAPI reports `registry.availability=available`.
- The same OpenAPI still reports `actions=0`.
- Focused official-lane tests, latest official lane compile probe, and local CI
  pass through mise.
