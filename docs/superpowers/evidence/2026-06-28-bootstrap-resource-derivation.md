# Bootstrap Resource Derivation Evidence

Date: 2026-06-28

## Scope

Phase 175 closes CL-02c only. It removes hand-maintained public resource
ownership from transitional Fabric bootstrap operation definitions and derives
runtime operation resources from operation ids.

This phase does not add gameplay operations, CLI commands, route families,
scenario shortcuts, or a new Minecraft version support claim. CL-02 remains
open for the remaining bootstrap catalog exit work.

## Red

Command:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.bootstrap operation definitions do not hand maintain public resource ownership*'
```

Observed before implementation:

- Exit code: `1`
- Failure: production `FabricBootstrapOperationDefinitions.kt` still
  contained `val resource: String` and hand-maintained `resource = "..."`
  bootstrap catalog literals.

## Focused Green

Command:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.bootstrap operation definitions do not hand maintain public resource ownership*' --tests '*FabricDriverModuleTest.fabric backend exposes runtime capability graph from probes*' --tests '*FabricCapabilityProbeTest.client state probe queries gateway and emits availability graph nodes*'
```

Observed after implementation:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

## Discovery Regression

Initial broad discovery run exposed a stale expectation:

```sh
mise exec -- gradle :driver-fabric-discovery:test
```

Observed:

- Exit code: `1`
- Failure:
  `FabricRuntimeGraphTest > client state graph fragment exposes connected resources and handles`
- Root cause: the discovery test still expected only the old broad `world`
  resource and did not account for derived `world.block` and `world.time`
  resources.

The test was updated to assert those derived resources and the
`world.block.handle` resource owner.

## Module Verification

Command:

```sh
mise exec -- gradle :driver-fabric-discovery:test
```

Observed after updating discovery assertions:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

Command:

```sh
mise exec -- gradle :driver-fabric:test
```

Observed:

- Exit code: `0`
- Gradle result: `BUILD SUCCESSFUL`

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
