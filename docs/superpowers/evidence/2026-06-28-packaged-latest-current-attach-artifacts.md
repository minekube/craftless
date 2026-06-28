# Packaged Latest Current Attach Artifacts Evidence

Phase: 182

Scope: closes CL-03a, CL-03b, CL-03c.2, CL-03d, and CL-03e.3. This does not
close CL-03f; public generated primitive invocation still needs its own
transcript.

## Root Cause Fixed

The packaged `latest-release` lane previously reached client creation, then
the Minecraft `26.2` client failed before self-attach because native launch
arguments were built from multiple native library paths. Minecraft-style JVM
arguments expect `${natives_directory}` to expand to one aggregate directory.

Cache preparation now:

- gives native libraries one aggregate native directory handle;
- keeps classpath and library artifacts separate;
- extracts native files into the aggregate root;
- also creates the `java/`, `jna/`, `lwjgl/`, and `netty/` subdirectories used
  by current Minecraft JVM arguments.

The fix is shared cache/launch preparation, not a `26.2`-specific probe
workaround.

## Verification Commands

Focused daemon regression:

```sh
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.CachePreparationServiceTest.cache preparation uses one aggregate native directory for launch variables'
```

Full cache-preparation test class:

```sh
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.CachePreparationServiceTest'
```

Distribution guard:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Packaged latest-current probe:

```sh
mise run packaged-latest-current-probe
```

Result:

```text
local Minecraft server smoke collected 1 evidence event(s)
serverLog=build/craftless-packaged-latest-current-probe/logs/server.log
evidenceLog=build/craftless-packaged-latest-current-probe/artifacts/server-evidence.jsonl
exitCode=0

BUILD SUCCESSFUL in 41s
```

## Captured Product Artifacts

Artifact root:

```text
driver-fabric/build/craftless-packaged-latest-current-probe/artifacts
```

Required artifacts captured:

- `clients-create-latest-release.log`
- `clients-connect-latest-release.log`
- `client-stop.log`
- `packaged-driver-mods.json`
- `packaged-probe-summary.json`
- `supervisor-openapi.json`
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
- `server-evidence.jsonl`

Probe summary:

```json
{
  "status": "connected",
  "clientId": "latest-current",
  "minecraftVersion": "latest-release",
  "concreteLatestVersion": "26.2",
  "actionCount": 1,
  "openapiActionCount": 1,
  "subscriptionId": "subscription:latest-current:0001"
}
```

Generated resources included:

```text
runtime, registry, event, client, player, inventory, recipe, world,
world.block, world.time, entity, screen
```

## Runtime And Version Evidence

Prepared manifest:

```text
build/craftless-packaged-latest-current-probe/workspace/cache/prepared/26.2-fabric-0.19.3.json
```

Key metadata:

- Minecraft version: `26.2`
- Requested version: `latest-release`
- Fabric Loader: `0.19.3`
- Fabric API: resolved through the packaged driver manifest and cache path
- Java requirement: major `25`, component `java-runtime-epsilon`, reason
  `minecraft-version-metadata`
- Selected Java: mise-managed Temurin Java 25
- Rejected Java 21 candidates, including `/usr/bin/java`, as too low
- Native path: one aggregate native directory under `cache/natives/`

## Streaming And Server Evidence

`client-events-stream.sse` contains:

```text
client.created
client.attached
client.connected
```

`server-evidence.jsonl` contains:

```json
{"type":"PLAYER_JOINED","player":"LatestCurrent"}
```

The server log records:

```text
Starting minecraft server version 26.2
LatestCurrent joined the game
```

## Remaining Open Work

CL-03f remains open. The next packet must invoke a generated operation through
the public packaged API or adaptive CLI and capture
`client-rpc-invoke-generated.json`. That work must still use generated
per-client OpenAPI metadata as the authority and must not introduce static
gameplay commands, `task.*` shortcuts, or hard-coded survival scenarios.
