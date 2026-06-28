# Shared Fabric Client State Graph Projection Evidence

Date: 2026-06-28

## Summary

Phase 156 moves non-gameplay Fabric client-state resource/handle projection into
`driver-fabric-discovery`.

The Yarn/remap Fabric lane still probes real Minecraft client state on the
client thread, then maps that lane-local snapshot into the shared
`FabricClientStateGraphSnapshot` from
`driver-fabric-discovery/src/main/kotlin/com/minekube/craftless/driver/fabric/discovery/FabricClientStateGraphSnapshot.kt`.
The latest/current official lane composes the same shared projection with a
disconnected snapshot while it remains metadata-only.

This adds no gameplay actions, no packaged 26.x driver manifest entry, no
scenario shortcut, and no latest/current support claim.

## Verification

Focused tests:

```sh
mise exec -- gradle :driver-fabric-discovery:test :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim' :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Result: `BUILD SUCCESSFUL`.

Local CI:

```sh
mise run ci
```

Result: `BUILD SUCCESSFUL`.

The first local CI run exposed two broader regressions that were fixed before
commit:

- `WorkspaceClientRuntimeDriverFactory` called the driver-mod provider twice
  with the same resolved lane request. It now reuses the preflight result for
  artifact materialization.
- `FabricLoaderRuntimeMetadataReader.installedModsFingerprint()` crashed in
  non-Fabric unit-test runtimes when Fabric Loader returned no mod containers.
  It now reports `mods:not-discovered` for that explicit no-evidence case.

Enabled official attach probe:

```sh
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=120000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Result: `BUILD SUCCESSFUL`, with `official Fabric probe observed client attach
for official-probe`.

Probe artifacts:

```sh
jq -r '"status=" + .status + " client=" + .clientId' \
  driver-fabric-official/build/craftless-official-attach-probe/probe-result.json
```

Output:

```text
status=ATTACHED client=official-probe
```

```sh
jq -r '"installedMods=" + ."x-craftless"."x-craftless-installed-mods-fingerprint" + " runtimeFingerprint=" + ."x-craftless"."x-craftless-runtime-fingerprint" + " actions=" + ((."x-craftless-actions"|length)|tostring) + " resources=" + ((."x-craftless-resources"|length)|tostring) + " handles=" + ((."x-craftless-handles"|length)|tostring) + " events=" + ((."x-craftless-events"|length)|tostring)' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi.json
```

Output:

```text
installedMods=mods:6d85fb9272c1d2f5 runtimeFingerprint=graph:3cc76876d5e4a673 actions=0 resources=10 handles=10 events=3
```

Client-state availability:

```text
client=unavailable:client-not-connected
entity=unavailable:client-not-connected
inventory=unavailable:client-not-connected
player=unavailable:client-not-connected
recipe=unavailable:client-not-connected
screen=available:
world=unavailable:client-not-connected
```

Handle ids:

```text
entity.handle,inventory.slot,recipe.handle,registry.block,registry.effect,registry.entity,registry.event,registry.item,registry.screen,world.block.handle
```
