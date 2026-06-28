# Representative Older Fabric Real-Client Smoke Evidence

## Scope

This evidence records a real diagnostic Fabric client smoke for the
representative older Minecraft `1.20.6` lane. It proves the parameterized older
Gradle lane can launch a real Fabric client, attach the Craftless driver,
expose generated API artifacts, stream SSE events, invoke generated actions,
join a local server, chat, and disconnect.

It does not prove final honest survival completion. The diagnostic smoke
provisioned an iron sword through the local server harness, so it is not the
no-shortcut survival evidence required by the project completion gate. It also
does not prove installed packaged CLI older-lane operation.

## Command

```sh
rm -rf /tmp/craftless-fabric-smoke-older-lane &&
CRAFTLESS_FABRIC_CLIENT_SMOKE=1 \
CRAFTLESS_SMOKE_MINECRAFT_VERSION=1.20.6 \
CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT=/tmp/craftless-fabric-smoke-older-lane \
CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=420000 \
mise exec -- gradle :driver-fabric:fabricClientSmoke \
  -Pcraftless.fabric.minecraftVersion=1.20.6 \
  -Pcraftless.fabric.yarnMappings=1.20.6+build.3 \
  -Pcraftless.fabric.loaderVersion=0.19.3 \
  -Pcraftless.fabric.apiVersion=0.100.8+1.20.6 \
  -Pcraftless.fabric.javaMajorVersion=21 \
  -Pcraftless.fabric.laneId=fabric-1-20-6-lane \
  -Pcraftless.fabric.providerId=fabric-1-20-6-lane \
  -Pcraftless.fabric.artifactKey=fabric-1-20-6-remap-jar \
  -Pcraftless.fabric.mappingsFingerprint=craftless-fabric-bindings-1-20-6
```

Result:

```text
local Minecraft server smoke collected 3 evidence event(s)
serverLog=/tmp/craftless-fabric-smoke-older-lane/logs/server.log
evidenceLog=/tmp/craftless-fabric-smoke-older-lane/artifacts/server-evidence.jsonl
exitCode=0
BUILD SUCCESSFUL in 1m 28s
```

## Runtime Lane

Artifact:

```text
/tmp/craftless-fabric-smoke-older-lane/artifacts/runtime-lane.json
```

Content:

```json
{"id":"fabric-1-20-6-lane","status":"SUPPORTED","minecraftVersion":"1.20.6","javaMajorVersion":21,"providerId":"fabric-1-20-6-lane"}
```

## Runtime Metadata

Artifact:

```text
/tmp/craftless-fabric-smoke-older-lane/artifacts/runtime-metadata.json
```

Key evidence:

```json
{
  "loaderVersion": "0.19.3",
  "driver": "craftless-driver-fabric",
  "driverVersion": "0.1.0-SNAPSHOT",
  "mappings": "craftless-fabric-bindings-1-20-6",
  "installedModsFingerprint": "mods:71e6bcc7cfc53dd7",
  "registryFingerprint": "registries:4e0848f83d42b527",
  "serverFeatureFingerprint": "server-features:278f1ee826ffb8ca",
  "permissionsFingerprint": "permissions:local-client"
}
```

The client log also showed Fabric Loader `0.19.3`, Java `21`, Minecraft
`1.20.6`, and Fabric API modules for `1.20.6`.

## Generated API Artifacts

Artifacts written:

```text
/tmp/craftless-fabric-smoke-older-lane/artifacts/client-openapi-connected.json
/tmp/craftless-fabric-smoke-older-lane/artifacts/client-actions-connected.json
/tmp/craftless-fabric-smoke-older-lane/artifacts/client-resources-connected.json
```

Observed generated paths include:

```text
/clients/fabric-smoke/openapi.json
/clients/fabric-smoke/actions
/clients/fabric-smoke/resources
/clients/fabric-smoke/events:stream
/clients/fabric-smoke:run
/clients/fabric-smoke/player:chat
/clients/fabric-smoke/player:move
/clients/fabric-smoke/inventory:query
/clients/fabric-smoke/inventory:equip
/clients/fabric-smoke/world/block:break
/clients/fabric-smoke/world/block:interact
```

Generated action/resource counts:

```text
actions=22
resources=14
```

## Server Evidence

Artifact:

```text
/tmp/craftless-fabric-smoke-older-lane/artifacts/server-evidence.jsonl
```

Observed evidence:

```jsonl
{"type":"PLAYER_JOINED","player":"Player281"}
{"type":"ITEM_PROVISIONED","player":"Player281","itemId":"minecraft:iron_sword","itemName":"Iron Sword","count":1}
{"type":"CHAT","player":"Player281","message":"hello from Craftless Fabric smoke"}
{"type":"PLAYER_DISCONNECTED","player":"Player281"}
```

The server log shows Minecraft server version `1.20.6`, offline local mode,
the player login, the chat message, disconnect, and clean server shutdown.

## Generated Action Evidence

Artifact:

```text
/tmp/craftless-fabric-smoke-older-lane/artifacts/gameplay-results.jsonl
```

Generated action results included:

- `player.chat`;
- `player.move`;
- `screen.query`;
- `world.time.query`;
- `player.query`;
- `entity.query`;
- `inventory.query`;
- `inventory.equip`;
- `player.look`;
- `world.block.break`;
- `world.block.interact`.

The `world.block.break` result reported `hit=true`, `started=true`, and
`changed=true`.

## SSE And Events

Artifacts:

```text
/tmp/craftless-fabric-smoke-older-lane/artifacts/client-events-stream.sse
/tmp/craftless-fabric-smoke-older-lane/artifacts/client-events.jsonl
```

Observed SSE/event types included:

- `client.created`;
- `client.connected`;
- `player.chat`;
- `player.move`;
- `screen.query`;
- `world.time.query`;
- `player.query`;
- `entity.query`;
- `inventory.query`;
- `inventory.equip`;
- `player.look`;
- `world.block.break`;
- `world.block.interact`.
