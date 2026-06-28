# Representative Older Product Lane Evidence

Scope: closes CL-04 only. This does not close CL-05, CL-06, CL-07, or CL-08.

## Representative Lane

- Minecraft version: `1.20.6`.
- Loader: Fabric.
- Loader version: `0.19.3`.
- Offline profile: `OlderProduct`.
- Client id: `representative-older`.
- Reason this lane is representative: it is the packaged older Fabric lane,
  uses Java 21 rather than the latest lane's Java 25 runtime, and exercises the
  Yarn/remap driver artifact path rather than the official 26.x driver module.

## Commands

Red guard added before implementation:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Observed failure before the probe script existed:

```text
ENOENT: no such file or directory, open '.../scripts/packaged-representative-older-probe.sh'
```

After the task and script were added:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Observed result:

```text
15 pass
0 fail
135 expect() calls
```

Live product probe:

```sh
mise run packaged-representative-older-probe
```

Observed result from the final rerun after the non-`task.*` invocation selector
guard was added:

```text
local Minecraft server smoke collected 2 evidence event(s)
serverLog=build/craftless-packaged-representative-older-probe/logs/server.log
evidenceLog=build/craftless-packaged-representative-older-probe/artifacts/server-evidence.jsonl
exitCode=0

BUILD SUCCESSFUL in 27s
```

## Artifact Root

`driver-fabric/build/craftless-packaged-representative-older-probe/artifacts/`

Key artifacts:

- `supervisor-openapi.json`
- `packaged-driver-mods.json`
- `clients-create-representative-older.log`
- `clients-connect-representative-older.log`
- `client-openapi-connected.json`
- `client-openapi-cli.json`
- `client-actions.json`
- `client-resources.json`
- `client-events-stream.sse`
- `client-rpc-openapi.json`
- `client-rpc-actions.json`
- `client-rpc-resources.json`
- `client-rpc-subscribe.json`
- `client-events-subscription-stream.sse`
- `client-rpc-subscriptions.json`
- `client-rpc-unsubscribe.json`
- `client-rpc-subscriptions-after-unsubscribe.json`
- `client-generated-action-selected.json`
- `client-rpc-invoke-generated.json`
- `client-cli-invoke-generated.log`
- `packaged-probe-summary.json`
- `server-evidence.jsonl`

## Product Surface Proof

`clients-create-representative-older.log` proves the packaged CLI created the
client through the supervisor API:

```json
{"id":"representative-older","instance":{"id":"representative-older-1.20.6-fabric","version":{"id":"1.20.6"},"loader":"FABRIC"},"profile":{"kind":"OFFLINE","name":"OlderProduct"},"state":"RUNNING"}
```

`clients-connect-representative-older.log` proves the same packaged client was
connected to the local server through the public client command:

```json
{"id":"representative-older","instance":{"id":"representative-older-1.20.6-fabric","version":{"id":"1.20.6"},"loader":"FABRIC"},"profile":{"kind":"OFFLINE","name":"OlderProduct"},"state":"RUNNING"}
```

`packaged-probe-summary.json` records:

```json
{
  "status": "connected",
  "api": "http://127.0.0.1:18085",
  "clientId": "representative-older",
  "minecraftVersion": "1.20.6",
  "representativeOlderVersion": "1.20.6",
  "generatedInvocationAction": "entity.query",
  "actionCount": 22,
  "openapiActionCount": 22,
  "subscriptionId": "subscription:representative-older:0001"
}
```

The captured resource projection included:

```text
runtime, registry, event, client, player, inventory, recipe, world,
world.block, world.time, entity, screen, navigation, task
```

The available generated actions included:

```text
entity.attack, entity.query, inventory.equip, inventory.query, player.chat,
player.look, player.move, player.query, player.raycast, recipe.craft,
recipe.query, screen.close, screen.query, world.block.break,
world.block.interact, world.block.query, world.time.query
```

## Generated Invocation Proof

`client-generated-action-selected.json` selected generated action
`entity.query` from live per-client OpenAPI `x-craftless-actions` with
`source = runtime-probe`, `availability = available`, and no required
arguments. The probe script now rejects `task.*` candidates when selecting a
generated action for product-lane invocation evidence.

`client-rpc-invoke-generated.json`:

```json
{"id":"invoke-generated:entity.query","result":{"action":"entity.query","status":"ACCEPTED","message":"fabric real-client action entity.query queried"},"error":null,"jsonrpc":"2.0"}
```

`client-cli-invoke-generated.log`:

```json
{"action":"entity.query","status":"ACCEPTED","message":"fabric real-client action entity.query queried"}
```

The filtered subscription stream captured lifecycle and invocation events,
including `client.created`, `client.attached`, `client.connected`, and
`entity.query`.

## Server Evidence

`server-evidence.jsonl`:

```json
{"type":"PLAYER_JOINED","player":"OlderProduct"}
{"type":"PLAYER_DISCONNECTED","player":"OlderProduct"}
```

## Negative Checks

- The probe uses the packaged CLI at `build/docker/craftless/bin/craftless`.
- The probe does not call `:driver-fabric:runClient`.
- The probe does not invoke `task.survival`.
- The probe does not select any generated `task.*` action for the public
  invocation proof.
- The probe does not use server-provisioned inventory, `/give`, or a survival
  scenario shortcut.

## Result

CL-04 is closed: the representative older `1.20.6` packaged lane now passes the
same public product gate shape as CL-03 for create, attach, connect, generated
OpenAPI, projections, SSE, JSON-RPC query/subscription, generated JSON-RPC
invocation, and adaptive CLI invocation.
