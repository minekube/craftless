# Bootstrap Resource Derivation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use
> superpowers:subagent-driven-development or superpowers:executing-plans to
> implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for
> tracking.

**Goal:** Close CL-02c by removing hand-maintained public resource ownership
from transitional Fabric bootstrap operation definitions.

**Architecture:** Runtime graph operation resources are derived from operation
ids using the same convention already used by projection code. Fabric
client-state discovery owns the resource nodes that make those derived
operation resources valid.

**Tech Stack:** Kotlin/JVM, Gradle through `mise`, Fabric driver modules.

---

### Task 1: Add The Red Guard

**Files:**

- Modify:
  `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] Add
  `bootstrap operation definitions do not hand maintain public resource ownership`.
- [x] Reject `val resource: String` and known `resource = "..."`
  bootstrap catalog literals.
- [x] Run the focused test and capture the red failure.

### Task 2: Derive Bootstrap Operation Resources

**Files:**

- Modify:
  `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricBootstrapOperationDefinitions.kt`

- [x] Remove `resource` from `FabricBootstrapOperationDefinition`.
- [x] Remove per-definition resource literals.
- [x] Build `RuntimeOperationNode.resource` from
  `id.substringBeforeLast(".")`.

### Task 3: Align Discovery Resources

**Files:**

- Modify:
  `driver-fabric-discovery/src/main/kotlin/com/minekube/craftless/driver/fabric/discovery/FabricClientStateGraphSnapshot.kt`
- Modify:
  `driver-fabric-discovery/src/test/kotlin/com/minekube/craftless/driver/fabric/discovery/FabricRuntimeGraphTest.kt`

- [x] Add discovered `world.block` and `world.time` resource nodes.
- [x] Make `world.block.handle` belong to `world.block`.
- [x] Update discovery tests to assert the derived resources and disconnected
  availability.

### Task 4: Verify And Record Evidence

**Files:**

- Create:
  `docs/superpowers/evidence/2026-06-28-bootstrap-resource-derivation.md`
- Modify:
  `docs/project-completion-checklist.md`
- Modify:
  `docs/superpowers/phase-index.md`

- [x] Run focused red/green tests.
- [x] Run Fabric discovery and Fabric module tests.
- [x] Run architecture and whitespace checks.
- [ ] Commit and push to `main`.
