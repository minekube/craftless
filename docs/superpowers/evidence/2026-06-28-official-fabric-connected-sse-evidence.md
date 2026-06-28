# Official Fabric Connected SSE Evidence

## Scope

Phase 162 extends the official/latest Fabric attach probe to capture public
client SSE evidence from `GET /clients/{id}/events:stream`. This is evidence
infrastructure only: no gameplay action, operation adapter, static catalog,
public route, CLI gameplay command, scenario shortcut, packaging support, or
latest/current support claim is added.

## Red Check

Command:

```sh
test -f driver-fabric-official/build/craftless-official-attach-probe/client-events-stream.sse
```

Result before implementation: failed with exit code `1` because the official
probe did not write a client SSE artifact.

## Focused Check

Command:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Result: `BUILD SUCCESSFUL`.

## Connected Official Attach Probe

Command:

```sh
rm -rf driver-fabric-official/build/craftless-official-attach-probe
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_CONNECT=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=180000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Result: `BUILD SUCCESSFUL`.

Probe output:

```text
official Fabric probe observed connected client state for official-probe
```

SSE artifact inspection:

```sh
grep '^event: ' driver-fabric-official/build/craftless-official-attach-probe/client-events-stream.sse
```

Output:

```text
event: client.created
event: client.attached
event: client.connected
```

Parsed event type inspection:

```sh
jq -r '.streamedEventTypes[]' driver-fabric-official/build/craftless-official-attach-probe/probe-result.json
```

Output:

```text
client.created
client.attached
client.connected
```

Generated connected OpenAPI inspection:

```sh
jq -r '"actions=" + (."x-craftless-actions" | length | tostring)' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi-connected.json
```

Output:

```text
actions=0
```

`probe-result.json` recorded:

```json
{
  "status": "CONNECTED",
  "clientId": "official-probe",
  "connectTarget": "127.0.0.1:53132",
  "streamedEventTypes": [
    "client.created",
    "client.attached",
    "client.connected"
  ]
}
```

## Boundary

This phase proves that the connected official/latest lane participates in the
public daemon SSE observation path for lifecycle events. It does not make the
official lane a packaged supported driver and does not add gameplay breadth.

## Final Verification

Commands:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
mise run fabric-lane-check-latest-official
mise run ci
```

Results:

- Focused official shared metadata tests: `BUILD SUCCESSFUL`.
- Latest-official lane check: `BUILD SUCCESSFUL`.
- Full local CI: `BUILD SUCCESSFUL`, including Gradle lint, detekt
  unused-check, Gradle tests, and Bun Playwright tests.
