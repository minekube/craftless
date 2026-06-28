# Official Client-State World Time Operation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the official 26.x lane project the existing `world.time.query`
operation from shared Fabric client-state discovery.

**Architecture:** The shared `driver-fabric-discovery` module owns the
client-state operation projection helper. Lane modules supply private adapter
keys and observed client-state snapshots, then compose the returned operation
into their runtime graph.

**Tech Stack:** Kotlin/JVM, Gradle through `mise`, Fabric discovery/runtime
graph DTOs.

---

### Task 1: Official Client-State Operation Projection

**Files:**
- Modify: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt`
- Modify: `driver-fabric-discovery/src/main/kotlin/com/minekube/craftless/driver/fabric/discovery/FabricClientStateGraphSnapshot.kt`
- Modify: `driver-fabric-discovery/src/test/kotlin/com/minekube/craftless/driver/fabric/discovery/FabricRuntimeGraphTest.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt`
- Modify: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt`

- [x] **Step 1: Write the failing official-lane test**

Change the official backend test so connected client-state expects
`world.time.query` in `runtimeGraph().operations` and `backend.actions()`.

- [x] **Step 2: Run the test to verify red**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official backend projects client state operations from lane provider*'
```

Expected before implementation: failure because `world.time.query` is absent.

- [x] **Step 3: Add shared client-state operation projection**

Add `fabricClientStateWorldTimeQueryOperation(...)` and
`RuntimeOperationNode.toFabricRuntimeEventNode()` to
`FabricClientStateGraphSnapshot.kt`.

- [x] **Step 4: Reuse the helper from Yarn/remap and official lanes**

Replace the Yarn/remap local `world.time.query` shape with the shared helper,
then compose the same helper into the official backend runtime graph.

- [x] **Step 5: Add direct discovery coverage**

Add a discovery-module test proving the shared helper reflects world
availability and emits `client-state` evidence.

- [x] **Step 6: Verify focused green**

Run:

```sh
mise exec -- gradle :driver-fabric-discovery:test --tests '*FabricRuntimeGraphTest.client state world time operation reflects world availability*'
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official backend projects client state operations from lane provider*'
mise exec -- gradle :driver-fabric:test --tests '*FabricCapabilityProbeTest.world time operation is discovered from client state rather than bootstrap definitions*' --tests '*FabricDriverModuleTest.fabric runtime discovery exposes world time query only from client state*'
```

Expected: all commands pass.

- [x] **Step 7: Verify latest official lane compile probe**

Run:

```sh
mise run fabric-lane-check-latest-official
cat build/reports/fabric-lane-check-latest-official.status
```

Expected: `status=compiled`.
