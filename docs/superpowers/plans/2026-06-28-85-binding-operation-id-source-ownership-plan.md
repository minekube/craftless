# Binding Operation Id Source Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove duplicated operation-id string literals from private Fabric execution bindings.

**Architecture:** Add red guards that reject `operationId = "..."` literals in `FabricActionBindings.kt`. Add internal operation-id constants to the bootstrap definition layer, update definitions and bindings to use them, and update policy tests to validate the new source ownership.

**Tech Stack:** Kotlin/JVM, Gradle via mise, Kotlin test, Markdown.

---

### Task 1: Add Red Guards

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`
- Modify: `protocol/src/test/kotlin/com/minekube/craftless/protocol/NamespacePolicyTest.kt`

- [x] **Step 1: Add Fabric source guard**

  Add a Fabric test that reads `FabricActionBindings.kt` and asserts no
  `operationId = "..."` literal remains.

- [x] **Step 2: Update binding graph test**

  Update the existing binding graph test to get binding ids from
  `defaultFabricActionBindings().map { it.operationId }` instead of source
  regex extraction.

- [x] **Step 3: Update protocol policy guard**

  Update `NamespacePolicyTest` so it rejects operation-id literals in
  `FabricActionBindings.kt` and validates that the bootstrap definition file is
  the source that contains operation id constants.

- [x] **Step 4: Run red tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric action bindings do not own operation id literals*'
  mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.private fabric gameplay bindings are limited to bootstrap operation id references*'
  ```

  Expected: fails before implementation because `FabricActionBindings.kt`
  still contains `operationId = "..."` literals and protocol guard still expects
  source-literal ids.

### Task 2: Move Binding Ids To Bootstrap Constants

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricBootstrapOperationDefinitions.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt`

- [x] **Step 1: Add constants**

  Add internal constants for every bootstrap operation id in
  `FabricBootstrapOperationIds`.

- [x] **Step 2: Use constants in definitions**

  Replace definition `id = "..."` literals with the constants.

- [x] **Step 3: Use constants in bindings**

  Replace every binding `operationId = "..."` literal with the matching
  `FabricBootstrapOperationIds` constant.

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-binding-operation-id-source-ownership.md`

- [x] **Step 1: Add Phase 85 to AGENTS**
- [x] **Step 2: Add checklist section**
- [x] **Step 3: Record red and green evidence**

### Task 4: Final Verification And Push

**Files:**
- All modified files from previous tasks

- [x] **Step 1: Run focused green tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric action bindings do not own operation id literals*' --tests '*FabricDriverModuleTest.transitional fabric binding operation ids are represented as runtime graph operations*' --tests '*FabricDriverModuleTest.fabric backend exposes bootstrap bindings as graph operation adapters*'
  mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.private fabric gameplay bindings are limited to bootstrap operation id references*'
  ```

- [x] **Step 2: Run forced local gates**

  ```sh
  git diff --check
  mise exec -- gradle lint test --rerun-tasks
  mise exec -- bun test playwright
  ```

- [x] **Step 3: Commit and push**

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-85-binding-operation-id-source-ownership-design.md docs/superpowers/plans/2026-06-28-85-binding-operation-id-source-ownership-plan.md docs/superpowers/evidence/2026-06-28-binding-operation-id-source-ownership.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricBootstrapOperationDefinitions.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt protocol/src/test/kotlin/com/minekube/craftless/protocol/NamespacePolicyTest.kt
  git commit -m "driver-fabric: centralize binding operation ids"
  git push origin main
  ```

## Self-Review

- Spec coverage: guards, constant extraction, governance, and verification are
  covered.
- Placeholder scan: no TBD/TODO placeholders.
- Scope: no new gameplay action, route family, CLI catalog, Fabric binding,
  version lane, or support claim.
