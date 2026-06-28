# Official Client-State World Time Operation Evidence

Date: 2026-06-28

## Scope

Phase 179 advances CL-03e only. It makes the official 26.x/latest-current lane
project the existing `world.time.query` operation from shared Fabric
client-state discovery. CL-03 remains open because the official lane is not
yet packaged as a supported product lane and official invocation/gameplay
evidence is still missing.

## Red

Command:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official backend projects client state operations from lane provider*'
```

Observed before implementation:

- Exit code: `1`
- Failure: `NoSuchElementException` because `world.time.query` was absent from
  the official backend runtime graph.

## Focused Green

Command:

```sh
mise exec -- gradle :driver-fabric-discovery:test --tests '*FabricRuntimeGraphTest.client state world time operation reflects world availability*'
```

Observed:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

Command:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official backend projects client state operations from lane provider*'
```

Observed:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

Command:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricCapabilityProbeTest.world time operation is discovered from client state rather than bootstrap definitions*' --tests '*FabricDriverModuleTest.fabric runtime discovery exposes world time query only from client state*'
```

Observed:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

## Latest Official Lane Compile Probe

Command:

```sh
mise run fabric-lane-check-latest-official
cat build/reports/fabric-lane-check-latest-official.status
```

Observed:

- Exit code: `0`
- Status artifact: `status=compiled`

## Broad Local Verification

Command:

```sh
git diff --check
```

Observed:

- Exit code: `0`

Command:

```sh
mise exec -- gradle :driver-fabric-discovery:test :driver-fabric-official:test :driver-fabric:test
```

Observed:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

Command:

```sh
mise run architecture-check
```

Observed:

- Exit code: `0`
- Gradle protocol, daemon, CLI, and driver-fabric checks passed.
- Bun Playwright helper/distribution tests: `19 pass`, `0 fail`.

## Remaining CL-03 Work

- Official `world.time.query` invocation still returns unsupported.
- A connected official client still needs fresh generated OpenAPI,
  actions/resources, SSE, JSON-RPC query, and JSON-RPC subscription artifacts
  after this operation projection change.
- The packaged CLI still must create or attach the latest/current lane through
  the supervisor API before CL-03 can close.
