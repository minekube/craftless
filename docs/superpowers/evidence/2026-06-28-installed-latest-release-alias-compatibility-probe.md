# Installed Latest Release Alias Compatibility Probe Evidence

Date: 2026-06-28

## Result

The installed packaged Craftless product resolves `latest-release` to Minecraft
`26.2`, prepares current Minecraft/Fabric/Java runtime metadata, and then fails
because the packaged driver-mod manifest has no Craftless Fabric driver lane
for Minecraft `26.2`.

This is current installed latest-release compatibility evidence. It is not
runnable latest/current support and does not satisfy final completion.

## External Version Inputs

Live Mojang version manifest:

```json
{
  "release": "26.2",
  "snapshot": "26.3-snapshot-1"
}
```

Manifest artifact:

```text
/tmp/craftless-packaged-latest-release-probe/artifacts/version_manifest_v2.json
```

Reference inputs for the next implementation phase:

- Mojang manifest: `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`
- Fabric porting guide: `https://docs.fabricmc.net/develop/porting/`
- Fabric mappings guide: `https://docs.fabricmc.net/develop/porting/mappings/`
- Fabric 26.1 announcement: `https://fabricmc.net/2026/03/14/261.html`

Fabric's 26.1+ guidance says Yarn-based mods must move to Mojang/official
mappings before porting, Java compatibility moves to 25, `remapJar` is replaced
by `jar`, and Fabric API names changed toward official naming. That confirms
the next Craftless 26.x phase needs a real official-mapping lane boundary, not
another Yarn-mapped `v1_21_6` variant.

## Commands

Package build:

```sh
mise run package-cli
```

Result: passed. The packaged distribution still contains driver entries for
Minecraft `1.21.6` and `1.20.6`, not `26.2`.

Packaged supervisor:

```sh
build/docker/craftless/bin/craftless server start \
  --port 18083 \
  --workspace /tmp/craftless-packaged-latest-release-probe/workspace
```

Result:

```json
{"ok":true,"url":"http://127.0.0.1:18083","openapi":"/openapi.json","events":"/events","workspace":"/tmp/craftless-packaged-latest-release-probe/workspace"}
```

Packaged latest-release create:

```sh
CRAFTLESS_HTTP_REQUEST_TIMEOUT_MS=900000 \
build/docker/craftless/bin/craftless clients create latest-cli \
  --api http://127.0.0.1:18083 \
  --version latest-release \
  --loader fabric \
  --offline-name LatestCli
```

Result:

```json
{"code":"BAD_REQUEST","message":"driver mod manifest has no Fabric entry for 26.2 0.19.3 fabricApiVersion=0.153.0+26.2 javaMajorVersion=25"}
```

## Prepared Runtime Evidence

Prepared metadata summary:

```json
{
  "minecraftVersion": "26.2",
  "loader": "FABRIC",
  "loaderVersion": "0.19.3",
  "javaMajor": 25,
  "artifacts": [
    {
      "kind": "MINECRAFT_VERSION_MANIFEST",
      "handle": "cache/minecraft/versions/26.2/version.json",
      "status": "RESOLVED"
    },
    {
      "kind": "MINECRAFT_CLIENT_JAR",
      "handle": "cache/minecraft/versions/26.2/client.jar",
      "status": "CACHED"
    },
    {
      "kind": "MINECRAFT_ASSET_INDEX",
      "handle": "cache/assets/indexes/32.json",
      "status": "RESOLVED"
    },
    {
      "kind": "JAVA_RUNTIME_MANIFEST",
      "handle": "cache/runtimes/mac-os-arm64/java-runtime-epsilon/manifest.json",
      "status": "RESOLVED"
    },
    {
      "kind": "FABRIC_LOADER_PROFILE",
      "handle": "cache/loaders/fabric/26.2/0.19.3/profile.json",
      "status": "RESOLVED"
    },
    {
      "kind": "FABRIC_MOD",
      "handle": "cache/mods/fabric/6b1a4230e29ab562628addfea1d8875ef43f0b28ca70e730e3d6f32467a959d6.jar",
      "status": "CACHED"
    }
  ],
  "launchMods": [
    "cache/mods/fabric/6b1a4230e29ab562628addfea1d8875ef43f0b28ca70e730e3d6f32467a959d6.jar"
  ]
}
```

Fabric API mod identity:

```text
fabric-api 0.153.0+26.2 ~26.2-
```

Minecraft version metadata:

```json
{
  "id": "26.2",
  "javaVersion": {
    "component": "java-runtime-epsilon",
    "majorVersion": 25
  },
  "assetIndex": "32",
  "mainClass": "net.minecraft.client.main.Main",
  "libraries": 131,
  "logging": "client-1.21.2.xml"
}
```

Packaged driver manifest:

```json
{
  "entries": [
    {
      "loader": "FABRIC",
      "minecraftVersion": "1.21.6",
      "loaderVersion": "0.19.3",
      "fabricApiVersion": "0.128.2+1.21.6",
      "javaMajorVersion": 21,
      "path": "mods/craftless-driver-fabric.jar"
    },
    {
      "loader": "FABRIC",
      "minecraftVersion": "1.20.6",
      "loaderVersion": "0.19.3",
      "fabricApiVersion": "0.100.8+1.20.6",
      "javaMajorVersion": 21,
      "path": "mods/fabric-1.20.6/craftless-driver-fabric.jar"
    }
  ]
}
```

## Artifact Root

`/tmp/craftless-packaged-latest-release-probe/artifacts/`

Key artifacts:

- `version_manifest_v2.json`
- `server-start.log`
- `clients-create-latest-release.log`
- `supervisor-version.json`
- `supervisor-openapi.json`
- `packaged-driver-mods.json`
- `cached-mods.txt`
- `prepared-26.2-summary.json`
- `launch-26.2-summary.json`

## Cleanup

After interrupting the packaged supervisor:

```sh
ps -axo pid,command | rg -i 'craftless|latest-cli|26\.2|fabric-loader' | rg -v 'rg -i|exec_command|codex' || true
```

Result: no managed Craftless, `latest-cli`, `26.2`, or Fabric loader probe
processes remained.

## Findings

- The packaged product correctly resolves the moving `latest-release` alias to
  concrete Minecraft `26.2`.
- Cache/runtime preparation understands Java 25 and current Fabric Loader/API
  metadata for `26.2`.
- The current missing piece is the provider-backed Craftless Fabric driver
  lane for `26.2`.
- The current create path discovers the missing driver lane after substantial
  cache population. A follow-up should add an earlier driver-lane preflight or
  progress reporting so users do not wait through large downloads before a
  known unsupported result.
- Final latest/current support still requires a runnable 26.x lane, packaged
  driver manifest entry, launched/attached client, generated OpenAPI/actions/
  resources, SSE, and public API/CLI gameplay evidence.
