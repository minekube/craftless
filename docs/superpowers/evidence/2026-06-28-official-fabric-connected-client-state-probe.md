# Official Fabric Connected Client State Probe Evidence

Phase 158 moves the latest/current official Fabric lane from title-screen
attachment evidence to connected-client state evidence. This does not claim
full Minecraft 26.x/latest support: the official lane is still not packaged in
`driver-mods.json`, still exposes zero gameplay actions, and still needs
generic gameplay discovery/invocation, SSE, packaging, public API/CLI smoke,
and honest gameplay evidence before it can be treated as supported.

## Official 26.2 Connect API

`javap` against
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/minecraft-merged-deobf-26.2.jar`
confirmed the official/Mojang-mapped lifecycle connect surface:

- `ConnectScreen.startConnecting(Screen, Minecraft, ServerAddress, ServerData, boolean, TransferState)`
- `ServerAddress.parseString(String)`
- `ServerData(String, String, ServerData.Type)`
- `ServerData.Type.OTHER`
- `TitleScreen()`

The implementation uses those as private official-lane lifecycle inputs only.
No public API names, action ids, CLI commands, or docs contracts expose those
Minecraft implementation names.

## Red/Green Checks

Focused backend connect delegation red check:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official backend connect delegates to lifecycle connector*'
```

Before the seam existed, this failed with:

```text
No parameter with name 'clientConnector' found.
Unresolved reference 'OfficialFabricClientConnector'.
```

Focused production connector red check:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official lane has production minecraft client connector*'
```

Before the production connector existed, this failed with:

```text
Unresolved reference 'MinecraftOfficialFabricClientConnector'.
```

Focused green checks:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
mise exec -- gradle :driver-fabric-official:compileKotlin
mise exec -- gradle :driver-fabric-official:compileTestKotlin
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim*'
```

All completed with `BUILD SUCCESSFUL`.

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
  "message": "official Fabric probe observed connected client state for official-probe",
  "connectTarget": "127.0.0.1:65150",
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

OpenAPI summary from
`driver-fabric-official/build/craftless-official-attach-probe/client-openapi-connected.json`:

```json
{
  "client": "official-probe",
  "minecraft": "26.2",
  "loader": "FABRIC",
  "loaderVersion": "0.19.3",
  "driver": "craftless-driver-fabric-official",
  "actions": 0,
  "resources": 10,
  "handles": 10,
  "events": 3,
  "availableResources": [
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

Connected client-state resources:

```json
{"id":"client","availability":"available","availabilityReasons":[]}
{"id":"player","availability":"available","availabilityReasons":[]}
{"id":"inventory","availability":"available","availabilityReasons":[]}
{"id":"recipe","availability":"available","availabilityReasons":[]}
{"id":"world","availability":"available","availabilityReasons":[]}
{"id":"screen","availability":"available","availabilityReasons":[]}
```

## Boundary Check

The official lane remains an internal probe lane:

- `driver-fabric-official` still does not depend on `driver-fabric`.
- The official connector is lifecycle-only and uses `ConnectScreen` privately.
- The official lane still reports `actions=0`.
- The architecture guard blocks `FabricClientGateway`,
  `FabricOperationAdapters`, static gameplay catalogs, packaged
  `driver-fabric-official` manifest entries, and public gameplay action drift.
