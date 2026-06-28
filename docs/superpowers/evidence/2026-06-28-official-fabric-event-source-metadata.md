# Official Fabric Event Source Metadata Evidence

## Scope

Phase 161 makes the latest/current official Fabric lane publish lane-provided
event-source evidence into the shared Fabric event graph. It does not add
gameplay actions, action adapters, static catalogs, public route families, CLI
commands, scenario shortcuts, packaging support, or a final latest/current
support claim.

## Red Check

Command:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official backend projects client state from lane provider without adding operations*'
```

Result before implementation: failed at `:driver-fabric-official:compileTestKotlin`
because `OfficialFabricDriverBackend` had no `eventSourceProvider` constructor
parameter and `OfficialFabricEventSourceProvider` did not exist.

## Focused Green Check

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

`probe-result.json` recorded:

```json
{
  "status": "CONNECTED",
  "clientId": "official-probe",
  "connectTarget": "127.0.0.1:52329"
}
```

Generated connected OpenAPI inspection:

```sh
jq -r '(.["x-craftless-events"][] | [.id,.availability,(.availabilityReason // "")] | @tsv), ("actions=" + (.["x-craftless-actions"] | length | tostring))' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi-connected.json
```

Output:

```text
event.action	available
event.capability	available
event.lifecycle	available
actions=0
```

Generated connected OpenAPI counts:

```json
{
  "resources": 10,
  "handles": 10,
  "events": 3,
  "actions": 0
}
```

## Boundary

This phase proves that official-lane Fabric lifecycle/networking callback
evidence can make the shared event graph available. It keeps generated
operations empty and leaves the broader latest/current support gate open.

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
