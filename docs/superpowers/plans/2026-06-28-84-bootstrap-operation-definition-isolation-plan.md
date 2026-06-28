# Bootstrap Operation Definition Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move transitional bootstrap operation ids and schemas out of the live client-state probe while preserving generated graph/OpenAPI behavior.

**Architecture:** Add red source guards, then create an internal `FabricBootstrapOperationDefinitions.kt` file that owns transitional definitions and converts them to `RuntimeOperationNode` with availability supplied by `FabricClientStateCapabilityProbe`. Keep the probe focused on live state/resources/handles.

**Tech Stack:** Kotlin/JVM, Gradle via mise, Kotlin test, Markdown.

---

### Task 1: Add Red Ownership Guards

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add source guard**

  Add a test named
  `fabric client state probe does not own bootstrap operation definitions`.
  The test reads `FabricCapabilityProbe.kt` and asserts that it does not
  contain bootstrap operation ids such as `player.chat`, adapter ids such as
  `fabric.player-chat`, or `RuntimeOperationNode(`.

- [x] **Step 2: Add behavior guard**

  Add a test named
  `bootstrap operation definitions still project into runtime graph`.
  The test compares `fabricBootstrapOperationDefinitions().map { it.id }`
  against `FabricDriverBackend.metadataOnly().runtimeGraph("alice").operations`
  for those ids.

- [x] **Step 3: Run red tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric client state probe does not own bootstrap operation definitions*' --tests '*FabricDriverModuleTest.bootstrap operation definitions still project into runtime graph*'
  ```

  Expected: fails before implementation because `FabricCapabilityProbe.kt`
  still owns operation ids and the new definition function does not exist.

### Task 2: Extract Bootstrap Operation Definitions

**Files:**
- Create: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricBootstrapOperationDefinitions.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt`

- [x] **Step 1: Create definition model**

  Add `FabricBootstrapOperationDefinition` with `id`, `resource`, `adapter`,
  `availability`, `arguments`, and `result`.

- [x] **Step 2: Move operation schemas**

  Move the current bootstrap operation ids, adapter ids, argument schemas, and
  result schemas into `fabricBootstrapOperationDefinitions()`.

- [x] **Step 3: Convert definitions to runtime operations**

  Add `toRuntimeOperation(availability: RuntimeAvailability)` and
  `toFabricEventNode()` helpers in the definition file.

- [x] **Step 4: Use definitions from client-state probe**

  Replace the inline operation list in `FabricClientStateCapabilityProbe` with
  `fabricBootstrapOperationDefinitions().map { definition -> ... }`, selecting
  availability from the live `FabricClientCapabilitySnapshot`.

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-bootstrap-operation-definition-isolation.md`

- [x] **Step 1: Add Phase 84 to AGENTS**

  Record that bootstrap definitions are isolated behind a transitional graph
  definition layer and that this still does not complete the binding exit.

- [x] **Step 2: Add checklist section**

  Record the phase, guard tests, verification commands, and remaining broader
  blocker.

- [x] **Step 3: Record red and green evidence**

  Capture the red guard failure, focused green tests, and local final gates.

### Task 4: Final Verification And Push

**Files:**
- All modified files from previous tasks

- [x] **Step 1: Run focused green tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric client state probe does not own bootstrap operation definitions*' --tests '*FabricDriverModuleTest.bootstrap operation definitions still project into runtime graph*' --tests '*FabricDriverModuleTest.fabric backend exposes bootstrap bindings as graph operation adapters*'
  ```

  Expected: `BUILD SUCCESSFUL`.

- [x] **Step 2: Run forced local gates**

  ```sh
  git diff --check
  mise exec -- gradle lint test --rerun-tasks
  mise exec -- bun test playwright
  ```

  Expected: all exit `0`.

- [x] **Step 3: Commit and push**

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-84-bootstrap-operation-definition-isolation-design.md docs/superpowers/plans/2026-06-28-84-bootstrap-operation-definition-isolation-plan.md docs/superpowers/evidence/2026-06-28-bootstrap-operation-definition-isolation.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricBootstrapOperationDefinitions.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
  git commit -m "driver-fabric: isolate bootstrap operation definitions"
  git push origin main
  ```

## Self-Review

- Spec coverage: tasks cover ownership guards, extraction, governance, and
  verification.
- Placeholder scan: no TBD/TODO placeholders.
- Scope: no new gameplay action, route family, CLI catalog, Fabric binding,
  version lane, or support claim.
