# Official Fabric Public Projection Endpoints Evidence

## Scope

Phase 163 extends the official/latest Fabric attach probe to capture public
projection endpoint evidence from `GET /clients/{id}/actions` and
`GET /clients/{id}/resources`. This is evidence infrastructure only: no
gameplay action, operation adapter, static catalog, public route, CLI gameplay
command, scenario shortcut, packaging support, or latest/current support claim
is added.

## Red Check

Command:

```sh
test -f driver-fabric-official/build/craftless-official-attach-probe/client-actions.json && \
  test -f driver-fabric-official/build/craftless-official-attach-probe/client-resources.json
```

Result before implementation: failed with exit code `1` because the official
probe did not write the public action/resource projection artifacts.

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

Actions artifact inspection:

```sh
jq -r '"actions=" + (length | tostring)' \
  driver-fabric-official/build/craftless-official-attach-probe/client-actions.json
```

Output:

```text
actions=0
```

Resources artifact inspection:

```sh
jq -r '.[].id' \
  driver-fabric-official/build/craftless-official-attach-probe/client-resources.json
```

Output:

```text
runtime
registry
event
client
player
inventory
recipe
world
entity
screen
```

`probe-result.json` inspection:

```sh
jq -r '{publicActionCount, publicResourceIds}' \
  driver-fabric-official/build/craftless-official-attach-probe/probe-result.json
```

Output:

```json
{
  "publicActionCount": 0,
  "publicResourceIds": [
    "runtime",
    "registry",
    "event",
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

## Boundary

This phase proves the connected official/latest lane exposes generated graph
projections through the same public action/resource endpoints that adaptive
clients use. It intentionally preserves zero official gameplay actions.

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
