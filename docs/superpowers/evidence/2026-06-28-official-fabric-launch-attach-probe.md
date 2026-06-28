# Official Fabric Launch Attach Probe Evidence

Date: 2026-06-28

## Scope

Phase 149 adds an opt-in diagnostic launch/self-attach probe harness for the
latest/current official Fabric lane.

This phase does not claim Minecraft 26.x/latest support. The official lane is
still not added to the packaged driver manifest. The enabled probe now proves
launch, self-attach, and generated OpenAPI metadata for the official 26.x lane,
but generated gameplay actions are still empty and packaged distribution, SSE,
generated gameplay resources/actions, and public API/CLI gameplay evidence
remain open. Phase 150 updates this evidence with live Fabric Loader metadata
fingerprinting for the official lane.

## Red Evidence

Before implementation, the architecture guard failed:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim'
```

Observed:

```text
FabricDriverModuleTest > official lane has opt in launch attach probe task without packaging support claim() FAILED
```

## Green Evidence

Architecture guard:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim'
```

Observed:

```text
BUILD SUCCESSFUL
```

Probe runner compile:

```sh
mise exec -- gradle :driver-fabric-official:compileTestKotlin
```

Observed:

```text
BUILD SUCCESSFUL
```

Default skipped probe:

```sh
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
cat driver-fabric-official/build/craftless-official-attach-probe/probe-result.json
```

Observed:

```json
{
    "status": "SKIPPED",
    "clientId": "official-probe",
    "daemonUrl": null,
    "message": "set CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 to run the official Fabric attach probe"
}
```

Controlled enabled failure:

```sh
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=1000 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_CLIENT_COMMAND_JSON='["sh","-c","echo client=$CRAFTLESS_CLIENT_ID daemon=$CRAFTLESS_DAEMON_URL"]' \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Observed:

```text
status=1
official Fabric probe timed out waiting for client attach for official-probe
```

The probe wrote:

```json
{
    "status": "TIMEOUT",
    "clientId": "official-probe",
    "daemonUrl": "http://127.0.0.1:<ephemeral>",
    "message": "official Fabric probe timed out waiting for client attach for official-probe"
}
```

The controlled child command log proved attach environment injection:

```text
client=official-probe daemon=http://127.0.0.1:<ephemeral>
```

Real enabled default probe:

```sh
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=120000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Observed:

```text
official Fabric probe observed client attach for official-probe
BUILD SUCCESSFUL
```

The probe wrote:

```json
{
    "status": "ATTACHED",
    "clientId": "official-probe",
    "daemonUrl": "http://127.0.0.1:<ephemeral>",
    "message": "official Fabric probe observed client attach for official-probe"
}
```

The event artifact contained:

```text
client.created
client.attached
```

The per-client OpenAPI artifact was captured before the child process was
stopped. Updated Phase 150 summary:

```json
{
  "client": "official-probe",
  "minecraft": "26.2",
  "loader": "FABRIC",
  "loaderVersion": "0.19.3",
  "driver": "craftless-driver-fabric-official",
  "installedMods": "mods:33e126b07d85b4a4",
  "registry": "registries:not-discovered",
  "serverFeatures": "server-features:not-connected",
  "actions": 0,
  "resources": 1
}
```

The previous `mods:official-lane-probe` placeholder is gone from the generated
OpenAPI evidence.

Lint and whitespace:

```sh
mise exec -- gradle lint
git diff --check
```

Observed:

```text
BUILD SUCCESSFUL
```

## Guardrails

- The probe runner lives under `driver-fabric-official/src/test`.
- The probe fetches daemon events and per-client OpenAPI before stopping the
  launched client.
- The probe suppresses expected child output-stream `IOException` during
  shutdown and does not print a reader-thread stack trace for normal teardown.
- No packaged 26.x driver manifest entry was added.
- Official runtime metadata comes from Fabric Loader mod containers instead of
  hard-coded installed-mod placeholders.
- Root and driver-local `AGENTS.md` files keep version support as shared
  system work by default, with per-version code only for documented lane
  divergence.
- No public gameplay action descriptor/catalog was added.
- No version-specific public route family was added.
- No survival shortcut was added.
- No final latest/current support claim was added.
