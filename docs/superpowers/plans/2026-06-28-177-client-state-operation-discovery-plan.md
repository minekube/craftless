# Client State Operation Discovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use
> superpowers:subagent-driven-development or superpowers:executing-plans to
> implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for
> tracking.

**Goal:** Close CL-02e by proving one existing Fabric operation node is
discovered from runtime client-state input rather than the bootstrap list.

**Architecture:** `FabricClientStateCapabilityProbe` keeps querying the live
client state on the client thread. It projects `world.time.query` from the
observed world state with private source evidence, while the existing private
execution adapter continues to handle invocation.

**Tech Stack:** Kotlin/JVM, Gradle through `mise`, Fabric driver module.

---

### Task 1: Add The Red Guard

**Files:**

- Modify:
  `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbeTest.kt`

- [x] Add
  `world time operation is discovered from client state rather than bootstrap definitions`.
- [x] Assert `world.time.query` is absent from
  `fabricBootstrapOperationDefinitions()`.
- [x] Assert client-state discovery emits `world.time.query` with
  `client-state` evidence.
- [x] Run the focused test and capture the red failure.

### Task 2: Move `world.time.query` Out Of Bootstrap Definitions

**Files:**

- Modify:
  `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricBootstrapOperationDefinitions.kt`

- [x] Remove `FabricBootstrapOperationAvailabilityKind.WORLD`.
- [x] Remove the `world.time.query` entry from
  `fabricBootstrapOperationDefinitions()`.
- [x] Keep operation id and adapter-key constants because the private
  execution adapter still uses them.

### Task 3: Project The Operation From Client State

**Files:**

- Modify:
  `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt`

- [x] Add `world.time.query` to the operations emitted by
  `FabricClientStateCapabilityProbe`.
- [x] Derive availability from the observed world state.
- [x] Attach `RuntimeSourceEvidence(kind = "client-state", ...)`.
- [x] Keep the private adapter key lookup through
  `fabricBootstrapOperationAdapterKey(...)`.

### Task 4: Update Stale Guards

**Files:**

- Modify:
  `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] Change the execution-adapter id guard to require runtime graph coverage,
  not bootstrap-definition ownership.
- [x] Keep rejecting copied bootstrap operation literals in
  `FabricClientStateCapabilityProbe`.

### Task 5: Verify And Record Evidence

**Files:**

- Create:
  `docs/superpowers/evidence/2026-06-28-client-state-operation-discovery.md`
- Modify:
  `docs/project-completion-checklist.md`
- Modify:
  `docs/superpowers/phase-index.md`

- [x] Run focused red/green tests.
- [x] Run the full Fabric module test suite.
- [x] Run architecture and whitespace checks.
- [ ] Commit and push to `main`.
