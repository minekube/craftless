# Client State Operation Discovery Evidence

Date: 2026-06-28

## Scope

Phase 177 closes CL-02e only. It proves one existing Fabric operation node,
`world.time.query`, from runtime client-state discovery instead of the
bootstrap operation definition list.

This phase does not add gameplay operations, CLI commands, route families,
scenario shortcuts, or a new Minecraft version support claim. CL-02 remains
open for broader architecture guards in CL-02f.

## Red

Command:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricCapabilityProbeTest.world time operation is discovered from client state rather than bootstrap definitions*'
```

Observed before implementation:

- Exit code: `1`
- Failure: `world.time.query` was still present in
  `fabricBootstrapOperationDefinitions()`.

## Focused Green

Command:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricCapabilityProbeTest.world time operation is discovered from client state rather than bootstrap definitions*' --tests '*FabricCapabilityProbeTest.client state probe queries gateway and emits availability graph nodes*' --tests '*FabricDriverModuleTest.fabric runtime discovery exposes world time query only from client state*' --tests '*FabricDriverModuleTest.bootstrap operation definitions still project into runtime graph*'
```

Observed after implementation:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

## Broad Fabric Verification

Command:

```sh
mise exec -- gradle :driver-fabric:test
```

Observed after updating stale guards:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

## Source Scan

Command:

```sh
rg -n 'WORLD_TIME_QUERY|world.time.query|FabricBootstrapOperationAvailabilityKind\.WORLD' driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricBootstrapOperationDefinitions.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbeTest.kt -S
```

Observed:

- `FabricBootstrapOperationDefinitions.kt` keeps only constants and the
  private adapter-key mapping for `WORLD_TIME_QUERY`.
- `FabricCapabilityProbe.kt` owns the discovered operation node and
  `client-state` evidence.
- `FabricCapabilityProbeTest.kt` asserts the new ownership.

## Final Local Verification

Command:

```sh
git diff --check
```

Observed:

- Exit code: `0`

Command:

```sh
mise run architecture-check
```

Observed:

- Exit code: `0`
- Gradle protocol, daemon, CLI, and driver-fabric checks passed.
- Bun Playwright helper/distribution tests: `19 pass`, `0 fail`.
