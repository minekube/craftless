# Fabric Adapter Key Source Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove duplicated private Fabric adapter-key literals from backend adapter registration.

**Architecture:** Add red source guards that reject bootstrap `fabric.*` adapter literals in `FabricDriverBackend.kt`. Add `FabricBootstrapOperationAdapters` constants in the bootstrap definition layer, use them in graph definitions and backend adapter registration, and keep behavior verified by existing graph invocation tests.

**Tech Stack:** Kotlin/JVM, Gradle via mise, Kotlin test, Markdown.

---

### Task 1: Add Red Adapter Ownership Guards

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add backend source guard**

  Add a test named
  `fabric backend does not own bootstrap adapter key literals`.

  It must read
  `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt`
  and assert it does not contain:

  - `"fabric.entity-query"`
  - `"fabric.entity-attack"`
  - `"fabric.world-block-query"`
  - `"fabric.recipe-query"`
  - `"fabric.recipe-craft"`

- [x] **Step 2: Run red guard**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric backend does not own bootstrap adapter key literals*'
  ```

  Expected: fails before implementation because `FabricDriverBackend.kt`
  currently contains those adapter-key literals.

### Task 2: Centralize Bootstrap Adapter Keys

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricBootstrapOperationDefinitions.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt`

- [x] **Step 1: Add adapter constants**

  Add internal `FabricBootstrapOperationAdapters` constants for every bootstrap
  `fabric.*` adapter key.

- [x] **Step 2: Use adapter constants in definitions**

  Replace bootstrap definition `adapter = "fabric.*"` literals with
  `FabricBootstrapOperationAdapters` constants.

- [x] **Step 3: Use adapter constants in backend**

  Replace backend adapter map keys for entity, block query, and recipe
  operations with `FabricBootstrapOperationAdapters` constants.

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-fabric-adapter-key-source-ownership.md`

- [x] **Step 1: Add Phase 86 to AGENTS**
- [x] **Step 2: Add checklist section**
- [x] **Step 3: Record red and green evidence**

### Task 4: Final Verification And Push

**Files:**
- All modified files from previous tasks

- [x] **Step 1: Run focused green tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric backend does not own bootstrap adapter key literals*' --tests '*FabricDriverModuleTest.fabric backend exposes bootstrap bindings as graph operation adapters*'
  ```

- [x] **Step 2: Run forced local gates**

  ```sh
  git diff --check
  mise exec -- gradle lint test --rerun-tasks
  mise exec -- bun test playwright
  ```

- [ ] **Step 3: Commit and push**

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-86-fabric-adapter-key-source-ownership-design.md docs/superpowers/plans/2026-06-28-86-fabric-adapter-key-source-ownership-plan.md docs/superpowers/evidence/2026-06-28-fabric-adapter-key-source-ownership.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricBootstrapOperationDefinitions.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
  git commit -m "driver-fabric: centralize bootstrap adapter keys"
  git push origin main
  ```

## Self-Review

- Spec coverage: guard, constants, backend use, governance, and verification
  are covered.
- Placeholder scan: no TBD/TODO placeholders.
- Scope: no new gameplay action, route family, CLI catalog, Fabric binding,
  version lane, or support claim.
