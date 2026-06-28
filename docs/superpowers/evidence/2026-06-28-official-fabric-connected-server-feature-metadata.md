# Official Fabric Connected Server Feature Metadata Evidence

Phase 159 fixes a truth gap in the latest/current official Fabric lane: a
connected official 26.2 client no longer publishes
`server-features:not-connected` in generated OpenAPI runtime metadata. This is
still metadata and graph evidence only. The official lane still exposes
`actions=0`, is not packaged as a supported driver lane, and does not claim
latest/current gameplay support.

## Red Check

Focused provider seam test:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official runtime metadata uses lane server feature provider*'
```

Before implementation, this failed during `compileTestKotlin` with:

```text
No parameter with name 'serverFeatureProvider' found.
Unresolved reference 'OfficialFabricServerFeatureProvider'.
```

## Green Checks

Focused official metadata tests:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Result: `BUILD SUCCESSFUL`.

The tests prove:

- `officialFabricRuntimeMetadataProvider(...)` accepts lane-provided
  server-feature evidence.
- `OfficialFabricDriverBackend.runtimeGraph(...)` exposes that fingerprint on
  the generated `runtime` resource evidence.
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
  "daemonUrl": "http://127.0.0.1:50189",
  "message": "official Fabric probe observed connected client state for official-probe",
  "connectTarget": "127.0.0.1:50188",
  "connectedResources": [
    "runtime",
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
jq -r '."x-craftless"."x-craftless-server-feature-fingerprint", (."x-craftless-actions" | length), (."x-craftless-resources" | length), (."x-craftless-handles" | length), (."x-craftless-events" | length)' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi-connected.json
```

Output:

```text
server-features:bd2c005ee588f506
0
10
10
3
```

The server-feature fingerprint is no longer
`server-features:not-connected`. The official lane still reports zero
generated actions.

## Boundary

- No public gameplay API, static gameplay catalog, static route family, CLI
  command, or scenario shortcut was added.
- `driver-fabric-official` still does not depend on `driver-fabric`.
- Registry metadata remains `registries:not-discovered` until a real official
  registry probe exists.
- This phase does not package the 26.x official lane and does not satisfy the
  full latest/current support gate.
