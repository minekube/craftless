# Official Fabric Registry Metadata Probe Evidence

Phase 160 makes the latest/current official Fabric lane report a discovered
registry fingerprint and project available registry resources/handles through
the shared Fabric registry graph. This is still metadata and graph evidence
only: the official lane still reports `actions=0`, is not packaged as a
supported driver lane, and does not claim latest/current gameplay support.

## Red Checks

Focused registry-provider seam test:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official runtime metadata uses lane registry provider*'
```

Before implementation, this failed during `compileTestKotlin` with:

```text
No parameter with name 'registryProvider' found.
Unresolved reference 'OfficialFabricRegistryProvider'.
```

Focused shared-registry graph availability test:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official backend projects client state from lane provider without adding operations*'
```

Before availability wiring, this failed at the registry availability assertion
because `OfficialFabricDriverBackend` still called the shared registry graph
with `available = false`.

## Green Checks

Focused official metadata tests:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Result: `BUILD SUCCESSFUL`.

The tests prove:

- `officialFabricRuntimeMetadataProvider(...)` accepts lane-provided registry
  entries.
- `OfficialFabricDriverBackend.runtimeGraph(...)` exposes that fingerprint on
  the generated `runtime` resource evidence.
- The shared `registry` resource and registry handles become available when
  official registry metadata is discovered.
- Official graph operations remain empty.

## Connected Live Probe

Command:

```sh
rm -rf driver-fabric-official/build/craftless-official-attach-probe
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_CONNECT=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=180000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Result: `BUILD SUCCESSFUL`.

Probe result:

```json
{
  "status": "CONNECTED",
  "clientId": "official-probe",
  "daemonUrl": "http://127.0.0.1:51114",
  "message": "official Fabric probe observed connected client state for official-probe",
  "connectTarget": "127.0.0.1:51113",
  "connectedResources": [
    "runtime",
    "registry",
    "client",
    "player",
    "inventory",
    "recipe",
    "world",
    "entity",
    "screen"
  ]
}
```

Generated OpenAPI inspection:

```sh
jq -r '."x-craftless"."x-craftless-registry-fingerprint", (."x-craftless-resources"[] | select(.id=="registry") | .availability), (."x-craftless-actions" | length), (."x-craftless-resources" | length), (."x-craftless-handles" | length), (."x-craftless-events" | length)' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi-connected.json
```

Output:

```text
registries:6797dc89ef586485
available
0
10
10
3
```

Registry handles:

```text
registry.block    available
registry.effect   available
registry.entity   available
registry.event    available
registry.item     available
registry.screen   available
```

## Boundary

- No public gameplay API, static gameplay catalog, static route family, CLI
  command, action descriptor, operation adapter, or scenario shortcut was
  added.
- `driver-fabric-official` still does not depend on `driver-fabric`.
- The official provider reads official/Mojang-mapped registry keys and exposes
  only a Craftless-owned fingerprint through generated OpenAPI.
- This phase does not package the 26.x official lane and does not satisfy the
  full latest/current support gate.
