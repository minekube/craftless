# Shared Fabric Event Graph Projection Evidence

## Scope

Phase 155 moved non-gameplay Fabric event graph projection into
`driver-fabric-discovery`. Fabric lanes still own actual Fabric API callback
registration, mixin hooks, event streaming behavior, and source evidence; the
shared module owns only Craftless-owned event resource/event projection and
availability from lane-provided evidence.

Non-goals verified for this phase:

- no public gameplay action added;
- no static gameplay catalog added;
- no packaged 26.x driver manifest entry added;
- no version-specific public route family added;
- no official-lane SSE completion claim added;
- no latest/current gameplay support claim added.

## Red Evidence

Before implementation, this command failed because
`fabricEventGraphFragment` did not exist in shared discovery infrastructure:

```sh
mise exec -- gradle :driver-fabric-discovery:test :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim'
```

Observed failure:

```text
Unresolved reference 'fabricEventGraphFragment'
```

## Green Evidence

Focused tests:

```sh
mise exec -- gradle :driver-fabric-discovery:test :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim' :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Result: `BUILD SUCCESSFUL`.

Lint and whitespace:

```sh
mise exec -- gradle lint
git diff --check
```

Result: `BUILD SUCCESSFUL`; `git diff --check` produced no output.

Real official attach probe:

```sh
rm -rf driver-fabric-official/build/craftless-official-attach-probe
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=120000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Result: `BUILD SUCCESSFUL`; probe log reported:

```text
official Fabric probe observed client attach for official-probe
```

Machine-readable probe summary:

```text
status=ATTACHED client=official-probe
installedMods=mods:6d85fb9272c1d2f5 runtimeFingerprint=graph:d53a992b228132ce actions=0 resources=3 handles=6 events=3
eventAvailability=unavailable availabilityReasons=event-source-not-discovered
events=event.action,event.capability,event.lifecycle
```

The official lane remains metadata/registry/event-evidence-only after this
phase: `actions=0` is expected here and does not satisfy latest/current
gameplay support.
