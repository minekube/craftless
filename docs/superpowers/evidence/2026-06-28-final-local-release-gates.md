# CL-06 Final Local Release Gates Evidence

Gate: CL-06 final local release-quality gates.

Result: closed for this gate only. This evidence does not close CL-07 final
honest public gameplay, CL-08 publish completion, or the overall project goal.

## Commands And Results

| Command | Result |
| --- | --- |
| `mise run lint` | exit `0`, `BUILD SUCCESSFUL in 1s` after formatting-only fixes. |
| `mise run architecture-check` | exit `0`; protocol, daemon, CLI, driver-fabric, and Bun architecture tests passed. |
| `mise run ci` | exit `0`; Gradle lint, Detekt unused/dead-code check, Gradle tests, and Bun tests passed. |
| `mise run package-cli` | exit `0`; CLI archives and Docker context rebuilt with `1.20.6` and `26.2` lanes. |
| `docker build -t craftless:cl06 .` | exit `0`; image copied `build/docker/craftless/` into `/opt/craftless/`. |
| `docker run --rm craftless:cl06 /opt/craftless/bin/craftless server start --once --port 0 --workspace /tmp/craftless` | exit `0`, `ok=true`. |
| install script smoke | exit `0`, installed `craftless 0.1.1` into a temp dir and `server start --once` returned `ok=true`. |
| `mise run packaged-latest-current-probe` | exit `0`, local Minecraft server smoke collected `1` evidence event. |
| `mise run packaged-representative-older-probe` | exit `0`, local Minecraft server smoke collected `2` evidence events. |
| `git diff --check` | exit `0`, no output. |

## Lint Fixes

`mise run lint` initially exposed formatting debt:

- `cli/src/main/kotlin/com/minekube/craftless/cli/Main.kt`: one long generated
  help line.
- `daemon/src/test/kotlin/com/minekube/craftless/daemon/CachePreparationServiceTest.kt`:
  long assertion lines.
- `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt`:
  import ordering.
- `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt`:
  import ordering.

The fixes were formatting-only and did not change product behavior.

## Architecture Guard Update

`mise run architecture-check` initially failed because
`FabricDriverModuleTest` still rejected packaged official-lane support:

```text
official lane has opt in launch attach probe task without packaging support claim()
```

That invariant was stale after CL-03/CL-05. The guard now enforces the current
design:

- official lane attach probing still exists;
- generated packaged manifests may include the official `26.2` lane;
- the packaged lane must carry runtime identity fields:
  `minecraftVersion=26.2`, `fabricApiVersion=0.153.0+26.2`,
  `javaMajorVersion=25`, `mappingsFingerprint=craftless-fabric-official-26-2`,
  and `path=mods/fabric-26.2/craftless-driver-fabric-official.jar`;
- build-only fields `artifactKey` and `distributionPath` are not projected into
  runtime manifests.

## Docker Smoke

Output:

```json
{"ok":true,"url":"http://127.0.0.1:34275","openapi":"/openapi.json","events":"/events","workspace":"/tmp/craftless"}
```

## Install Smoke

Output:

```text
craftless 0.1.1 installed to /var/folders/1y/cjgf53nj31n_dxsspqnjfjvc0000gn/T/tmp.NvLDW3TSW5/bin/craftless
```

```json
{"ok":true,"url":"http://127.0.0.1:49567","openapi":"/openapi.json","events":"/events","workspace":"/var/folders/1y/cjgf53nj31n_dxsspqnjfjvc0000gn/T/tmp.NvLDW3TSW5/workspace"}
```

## Latest/Current Product Probe

Task:

```sh
mise run packaged-latest-current-probe
```

Summary artifact:

```json
{
  "status": "connected",
  "clientId": "latest-current",
  "minecraftVersion": "latest-release",
  "concreteLatestVersion": "26.2",
  "generatedInvocationAction": "world.time.query",
  "actionCount": 1,
  "openapiActionCount": 1,
  "subscriptionId": "subscription:latest-current:0001"
}
```

JSON-RPC generated invocation:

```json
{"id":"invoke-generated:world.time.query","result":{"action":"world.time.query","status":"ACCEPTED","message":"official lane action world.time.query queried","data":{"time":2502,"time-of-day":2502}},"error":null,"jsonrpc":"2.0"}
```

Adaptive CLI generated invocation:

```json
{"action":"world.time.query","status":"ACCEPTED","message":"official lane action world.time.query queried","data":{"time":2512,"time-of-day":2512}}
```

Artifact root:

```text
driver-fabric/build/craftless-packaged-latest-current-probe/artifacts/
```

## Representative Older Product Probe

Task:

```sh
mise run packaged-representative-older-probe
```

Summary artifact:

```json
{
  "status": "connected",
  "clientId": "representative-older",
  "minecraftVersion": "1.20.6",
  "representativeOlderVersion": "1.20.6",
  "generatedInvocationAction": "entity.query",
  "actionCount": 22,
  "openapiActionCount": 22,
  "subscriptionId": "subscription:representative-older:0001"
}
```

JSON-RPC generated invocation:

```json
{"id":"invoke-generated:entity.query","result":{"action":"entity.query","status":"ACCEPTED","message":"fabric real-client action entity.query queried","data":{"origin":{"x":-270.5,"y":68.0,"z":166.5},"radius":16.0,"count":0,"entities":[]}},"error":null,"jsonrpc":"2.0"}
```

Adaptive CLI generated invocation:

```json
{"action":"entity.query","status":"ACCEPTED","message":"fabric real-client action entity.query queried","data":{"origin":{"x":-270.5,"y":68.0,"z":166.5},"radius":16.0,"count":0,"entities":[]}}
```

Artifact root:

```text
driver-fabric/build/craftless-packaged-representative-older-probe/artifacts/
```

## Remaining Gates

CL-07 remains open: final honest survival gameplay must be performed through
public generated API/CLI only, without `/give`, creative inventory,
server-provisioned inventory, hard-coded survival scenario actions, or direct
in-process test calls.

CL-08 remains open: final state must be committed, pushed, clean, and indexed
after CL-07 evidence exists.
