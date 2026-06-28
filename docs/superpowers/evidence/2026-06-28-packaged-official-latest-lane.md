# Packaged Official Latest Lane Evidence

Date: 2026-06-28

## Scope

Phase 181 packages the official Minecraft `26.2` Fabric lane into the
Craftless CLI/Docker distribution manifest. This removes the previous packaged
`latest-release` manifest blocker where `26.2` had no Craftless Fabric driver
entry.

CL-03 remains open. This phase does not prove packaged latest-current
create/attach, connected per-client OpenAPI/actions/resources, SSE, JSON-RPC
query/subscription artifacts, or public gameplay smoke.

## Red

Command:

```sh
mise exec -- bun test playwright/src/distribution.test.ts
```

Observed before implementation:

- Exit code: `1`
- Failure: the new distribution guard expected
  `build/driver-lanes/latest-official` and
  `mods/fabric-26.2/craftless-driver-fabric-official.jar` in `.mise.toml`,
  but the package task only staged the representative older lane.

## Implementation

`mise run package-cli` now:

- builds `:driver-fabric-official:jar` through
  `mise exec java@temurin-25.0.3+9.0.LTS gradle@9.6.0`;
- stages the official jar at
  `build/driver-lanes/mods/fabric-26.2/craftless-driver-fabric-official.jar`;
- writes an extra-lane catalog under
  `build/driver-lanes/latest-official/fabric-driver-lanes.json`;
- passes `-Pcraftless.extraFabricDriverLaneRoot=build/driver-lanes` to the CLI
  distribution task;
- verifies tar, zip, and Docker-context contents for the official lane.

One package run initially failed because extra-lane artifacts resolve relative
to the configured extra root plus `distributionPath`. The staged jar was moved
from `build/driver-lanes/latest-official/mods/...` to
`build/driver-lanes/mods/...` so the existing generic distribution merge path
can resolve it without special-casing official lanes.

## Green

Command:

```sh
mise exec -- bun test playwright
```

Observed:

- Exit code: `0`
- Bun result: `20 pass`, `0 fail`

Command:

```sh
mise run package-cli
```

Observed:

- Exit code: `0`
- Gradle distribution tasks completed successfully.
- Tar/zip checks found the official 26.2 driver jar.
- Driver manifest checks found the official 26.2 entry.
- Docker-context checks found the official 26.2 driver jar with nested runtime
  jars.

Packaged `driver-mods.json`:

```json
{
    "entries": [
        {
            "loader": "FABRIC",
            "minecraftVersion": "1.21.6",
            "loaderVersion": "0.19.3",
            "fabricApiVersion": "0.128.2+1.21.6",
            "javaMajorVersion": 21,
            "mappingsFingerprint": "craftless-fabric-bindings",
            "path": "mods/craftless-driver-fabric.jar"
        },
        {
            "loader": "FABRIC",
            "minecraftVersion": "26.2",
            "loaderVersion": "0.19.3",
            "fabricApiVersion": "0.153.0+26.2",
            "javaMajorVersion": 25,
            "mappingsFingerprint": "craftless-fabric-official-26-2",
            "path": "mods/fabric-26.2/craftless-driver-fabric-official.jar"
        },
        {
            "loader": "FABRIC",
            "minecraftVersion": "1.20.6",
            "loaderVersion": "0.19.3",
            "fabricApiVersion": "0.100.8+1.20.6",
            "javaMajorVersion": 21,
            "mappingsFingerprint": "craftless-fabric-bindings-1-20-6",
            "path": "mods/fabric-1.20.6/craftless-driver-fabric.jar"
        }
    ]
}
```

Official jar nested runtime check:

```text
fabric.mod.json
META-INF/jars/driver-api-0.1.0-SNAPSHOT.jar
META-INF/jars/driver-fabric-attach-0.1.0-SNAPSHOT.jar
META-INF/jars/driver-runtime-0.1.0-SNAPSHOT.jar
META-INF/jars/kotlin-stdlib-2.4.0.jar
META-INF/jars/ktor-http-jvm-3.5.0.jar
META-INF/jars/protocol-0.1.0-SNAPSHOT.jar
```

Command:

```sh
mise run fabric-lane-check-latest-official
```

Observed:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`
- Status artifact: `status=compiled`

Command:

```sh
git diff --check
```

Observed:

- Exit code: `0`

## Remaining CL-03 Work

- Packaged `latest-release` still needs a create/attach run through
  `build/docker/craftless/bin/craftless` and the supervisor API.
- A connected official 26.x packaged client still needs generated per-client
  OpenAPI, actions/resources, SSE, JSON-RPC query, and JSON-RPC subscription
  artifacts.
- Latest/current public API/CLI gameplay smoke remains open.
