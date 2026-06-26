# Version-Agnostic Driver Audit

Date: 2026-06-26

## Purpose

Classify the current Minecraft/Fabric version coupling before introducing
runtime/provider facades. This audit follows Phase 26 Task 1 and does not add
new gameplay actions or broaden the public API.

## Build Target For Current Compiled Driver Lane

These are acceptable as bootstrap state, but must become explicit compiled-lane
metadata before additional lanes are advertised:

- `driver-fabric/build.gradle.kts` compiles one Loom lane:
  - Minecraft `1.21.6`;
  - Yarn `1.21.6+build.1`;
  - Fabric Loader `0.19.3`;
  - Fabric API `0.128.2+1.21.6`.
- `driver-fabric/src/main/resources/craftless-driver-fabric.mixins.json`
  declares `JAVA_21`, matching the current compiled lane.
- `driver-fabric/build.gradle.kts` launches the Fabric client through
  `:driver-fabric:runClient`, so smoke and final gameplay currently inherit the
  one compiled Loom lane.

Blocker: a requested Minecraft version cannot yet select a matching compiled
or runtime-compatible Fabric lane. The `26.2` compatibility probe showed the
server can run with Java 25 while the client still launches the `1.21.6` lane.

## Internal Source And Package Organization

These are internal implementation details, not public API, but they make the
single-lane coupling visible throughout the module:

- Main Kotlin package root:
  `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/`.
- Java mixin package root:
  `driver-fabric/src/main/java/com/minekube/craftless/driver/fabric/v1_21_6/`.
- Test package root:
  `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/`.
- Fabric client entrypoint:
  `com.minekube.craftless.driver.fabric.v1_21_6.CraftlessFabricClientEntrypoint`.
- Mixin package:
  `com.minekube.craftless.driver.fabric.v1_21_6.mixin`.

Blocking shape: `CraftlessFabricClientEntrypoint` currently instantiates
`MinecraftFabricClientGateway`, `FabricDriverBackend.real(...)`, and
`FabricClientSmokeController` directly from the versioned package. There is no
stable non-versioned entrypoint that identifies the runtime and selects an
internal provider.

## Runtime Metadata And Evidence

These are valid evidence inputs, but they are not yet structured as provider
selection metadata:

- `FabricCapabilityProbeContext` carries `DriverRuntimeMetadata`, mode id,
  gateway, and bindings.
- `FabricRuntimeMetadataCapabilityProbe` records installed-mod, registry,
  server-feature, and permission fingerprints.
- `FabricEventSourceCapabilityProbe` records event-source evidence from driver
  version and Fabric event hooks/callbacks.
- `FabricClientSmokePlan` and `FabricFinalGameplayPlan` record expected
  Minecraft version as `1.21.6`.
- `docs/superpowers/evidence/2026-06-26-version-26-compatibility-probe.md`
  records that Java 25 is required for the `26.2` server and that the current
  Fabric client still launches `1.21.6`.
- `docs/superpowers/evidence/2026-06-26-java-runtime-resolution-smoke.md`
  records resolver-selected Java 25 for a `26.2` server smoke.

Blocker: graph evidence does not yet record selected Fabric provider id,
compiled lane id, supported version predicate, or unavailable reasons such as
`unsupported-version`, `missing-provider`, or `runtime-lane-mismatch`.

## Test Fixture Inputs

These are acceptable test fixtures when clearly marked as fixture defaults:

- `testkit/src/main/kotlin/.../LocalMinecraftServerSmoke.kt` defaults the local
  server smoke to Minecraft `1.21.6`.
- `CRAFTLESS_SMOKE_MINECRAFT_VERSION` can override the server version.
- The smoke can now consume `CRAFTLESS_SMOKE_JAVA_SELECTION_JSON` or
  `CRAFTLESS_SMOKE_JAVA_SELECTION_FILE` and record
  `artifacts/java-runtime-selection.json`.
- `MinecraftServerJarProvisionerTest` uses `1.21.6` fake metadata as a
  deterministic server-jar fixture.

Blocking shape: local server version selection is independent from Fabric
client lane selection. The smoke can start a `26.2` server, but the Fabric
client action command still defaults to `:driver-fabric:runClient` for the
compiled `1.21.6` lane.

## Public-Facing Debt

These references are user-facing or public-ish and must be generalized or made
explicitly current-lane-only before broad compatibility is claimed:

- `fabric.mod.json` names the mod `Craftless Driver Fabric 1.21.6`, describes
  it as a Minecraft `1.21.6` scaffold, depends on exactly
  `minecraft: 1.21.6`, and uses a versioned entrypoint.
- README quickstart examples use `--mc 1.21.6` and create a client with
  version `1.21.6`.
- README status text says Fabric smoke is proven for current actions, but it
  also documents server-provisioned `Iron Sword` smoke setup. That is valid
  historical smoke evidence, not final gameplay evidence.
- README comparison/status sections need to distinguish current compiled
  Fabric lane from the planned version/runtime compatibility matrix.

Blocking shape: public docs can make the product look broader than the current
compiled Fabric lane. They must keep implemented state versus roadmap explicit.

## Bootstrap State That Is Acceptable For Now

- Keeping one compiled `driver-fabric` Loom lane while runtime/provider facades
  are introduced.
- Keeping bytecode-sensitive mixins and accessors in a version-family package.
- Keeping deterministic tests that use `1.21.6` fixture metadata.
- Keeping opt-in smoke tasks as evidence infrastructure.

## Multi-Version Blockers

1. No stable non-versioned Fabric entrypoint boundary delegates to provider
   selection.
2. No internal compatibility matrix owns compiled lane metadata, Java runtime
   requirements, loader/Fabric API constraints, or unsupported reasons.
3. Runtime graph evidence lacks selected provider/lane metadata and
   machine-readable unsupported/failure reasons.
4. Fabric client smoke and final gameplay still launch the current compiled
   `1.21.6` lane regardless of the requested server version.
5. Server-observed join evidence is not yet required to prove a connect action
   succeeded across version lanes.
6. Supervisor client creation cannot yet prepare, install, and launch a real
   versioned Fabric client runtime outside the direct Gradle `runClient` smoke.
7. README and Fabric metadata still expose current-lane assumptions as active
   wording.

## Next Implementation Step

Proceed to Phase 26 Task 2: add stable internal runtime identity, runtime
access, provider selection, and provider evidence types plus tests. Do not add
new public gameplay action descriptors while doing that work.
