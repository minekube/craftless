# Legacy Survival Task Namespace Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent the removed `task.survival.*` scenario namespace from being treated as valid public protocol data.

**Architecture:** Add validation at the protocol DTO boundary and keep Fabric task adapter tests focused on generic task unavailability. This does not add generated gameplay actions, public routes, scenario tasks, or task executor behavior.

**Tech Stack:** Kotlin/JVM, kotlinx.serialization, JUnit 5, Gradle through mise.

---

### Task 1: Protocol Guard

**Files:**
- Modify: `protocol/src/test/kotlin/com/minekube/craftless/protocol/NavigationModelsTest.kt`
- Modify: `protocol/src/main/kotlin/com/minekube/craftless/protocol/NavigationModels.kt`

- [x] **Step 1: Replace legacy positive fixture**

Use `task.generic.obtain-materials` instead of `task.survival.obtain-weapon`
in the serialization test.

- [x] **Step 2: Write failing validation assertion**

Assert that `NavigationTaskRequest(task = "task.survival.obtain-weapon")`
throws `IllegalArgumentException`.

- [x] **Step 3: Run focused protocol test for RED**

Run:

```sh
mise exec -- gradle :protocol:test --tests 'com.minekube.craftless.protocol.NavigationModelsTest.navigation models reject backend and raw implementation names'
```

Expected RED before implementation: assertion fails because the legacy task id
is still accepted.

- [x] **Step 4: Add protocol validation**

Reject `task.survival.*` in `NavigationTaskRequest`.

- [x] **Step 5: Run focused protocol test for GREEN**

Run:

```sh
mise exec -- gradle :protocol:test --tests 'com.minekube.craftless.protocol.NavigationModelsTest'
```

Expected: pass.

### Task 2: Fabric Task Adapter Test Cleanup

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricNavigationDiscoveryTest.kt`

- [x] **Step 1: Replace legacy task request**

Use `task.generic.obtain-materials` when verifying the Fabric metadata-only task
adapter reports `task-executor-unavailable`.

- [x] **Step 2: Run focused Fabric test**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests 'com.minekube.craftless.driver.fabric.v1_21_6.FabricNavigationDiscoveryTest.fabric backend task adapter keeps generic tasks unavailable without executor'
```

Expected: pass.

### Task 3: Documentation And Verification

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Record Phase 36 in repo guardrails and checklist**

Add this phase to the active sequence and record the focused evidence.

- [x] **Step 2: Run final verification**

Run:

```sh
mise exec -- gradle :protocol:test --tests 'com.minekube.craftless.protocol.NavigationModelsTest'
mise exec -- gradle :driver-fabric:test --tests 'com.minekube.craftless.driver.fabric.v1_21_6.FabricNavigationDiscoveryTest.fabric backend task adapter keeps generic tasks unavailable without executor'
mise run lint
git diff --check
```

Expected: all pass.
