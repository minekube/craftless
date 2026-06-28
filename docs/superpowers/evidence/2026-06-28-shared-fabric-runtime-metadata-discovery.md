# Shared Fabric Runtime Metadata Discovery Evidence

Date: 2026-06-28

## Scope

Phase 151 extracts Fabric Loader runtime metadata primitives into
`driver-fabric-discovery` so the Yarn/remap and official Fabric lanes share
loader identity, installed-mod fingerprinting, snapshot metadata emission, and
deterministic fingerprinting.

This is version-agnostic discovery plumbing. It does not add gameplay actions,
package the official `26.2` lane, add static CLI gameplay commands, create
version-specific public APIs, or claim latest/current gameplay support.

## Red Evidence

Shared-boundary guard before implementation:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim'
```

Observed:

```text
FabricDriverModuleTest > official lane has opt in launch attach probe task without packaging support claim() FAILED
```

Shared provider red test before implementation:

```sh
mise exec -- gradle :driver-fabric-discovery:test
```

Observed unresolved shared metadata symbols:

```text
Unresolved reference 'FabricRuntimeMetadataSnapshot'
Unresolved reference 'fabricRuntimeFingerprint'
Unresolved reference 'SnapshotFabricRuntimeMetadataProvider'
```

## Green Evidence

Focused shared/lane tests:

```sh
mise exec -- gradle :driver-fabric-discovery:test :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim' :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Observed:

```text
BUILD SUCCESSFUL
```

Real enabled official attach probe:

```sh
rm -rf driver-fabric-official/build/craftless-official-attach-probe
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=120000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
jq -r '."x-craftless"."x-craftless-installed-mods-fingerprint"' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi.json
```

Observed:

```text
BUILD SUCCESSFUL
status=ATTACHED client=official-probe
installedMods=mods:6d85fb9272c1d2f5 actions=0 resources=1
```

## Guardrails

- `driver-fabric-discovery` does not depend on `driver-fabric`,
  `driver-fabric-official`, `daemon`, or `cli`.
- `driver-fabric-discovery` owns shared Fabric Loader metadata, not gameplay
  action catalogs.
- The Yarn/remap lane keeps Minecraft game-class registry and server-feature
  probes lane-local.
- The official lane keeps 26.x official mappings and not-yet-discovered
  registry/server-feature gaps lane-local.
