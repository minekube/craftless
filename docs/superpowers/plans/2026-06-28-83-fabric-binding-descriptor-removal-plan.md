# Fabric Binding Descriptor Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove descriptor/schema ownership from private Fabric execution bindings while keeping graph-owned public actions and invocation behavior unchanged.

**Architecture:** Add failing source/behavior guards that require `FabricActionBinding` to expose only private operation ids. Replace descriptor usage with `operationId` adapter registration, delete descriptor helper functions from `FabricActionBindings.kt`, and update governance/evidence.

**Tech Stack:** Kotlin/JVM, Gradle via mise, Kotlin test, Markdown.

---

### Task 1: Add Red Binding Descriptor Guards

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Replace descriptor-id transitional test**

  Replace the test named
  `transitional fabric binding ids are represented as runtime graph operations`
  so it reads binding `operationId` values instead of `descriptor.id` and
  asserts those operation ids are represented by runtime graph operations.

- [x] **Step 2: Replace source guard**

  Replace the test named
  `hand written fabric gameplay bindings stay transitional and graph represented`
  with a guard named
  `fabric action bindings do not own public descriptors or schemas`.

  The guard must read
  `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt`
  and assert it does not contain:

  - `DriverActionDescriptor`
  - `DriverActionArgument`
  - `DriverActionResultDescriptor`
  - `DriverActionResultProperty`
  - `fabricPlayerQueryDescriptor`
  - `fabricObjectDataResultDescriptor`
  - `descriptor.id`
  - `override val descriptor`

- [x] **Step 3: Add adapter source guard**

  Add a guard that reads
  `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt`
  and asserts adapter registration does not contain `binding.descriptor.id`.

- [x] **Step 4: Run red guard tests**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric action bindings do not own public descriptors or schemas*' --tests '*FabricDriverModuleTest.fabric operation adapter registration does not use binding descriptors*'
  ```

  Expected: fails because the binding file and backend still use descriptor
  metadata.

### Task 2: Remove Descriptor Ownership From Bindings

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt`

- [x] **Step 1: Change binding interface**

  Replace:

  ```kotlin
  internal interface FabricActionBinding {
      val descriptor: DriverActionDescriptor
  ```

  with:

  ```kotlin
  internal interface FabricActionBinding {
      val operationId: String
  ```

- [x] **Step 2: Replace binding descriptor properties**

  For every binding object, replace `override val descriptor = ...` or
  inline `DriverActionDescriptor(...)` with `override val operationId =
  "<same-action-id>"`.

- [x] **Step 3: Delete descriptor helpers and imports**

  Remove descriptor helper functions and unused descriptor imports from
  `FabricActionBindings.kt`.

- [x] **Step 4: Register adapters by operation id**

  Change `FabricDriverBackend` to associate bindings by `operationId` and to
  build adapter keys from `binding.operationId.fabricOperationAdapterKey()`.

### Task 3: Verify Focused Green

**Files:**
- Modified files from previous tasks

- [x] **Step 1: Run focused Fabric tests**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric action bindings do not own public descriptors or schemas*' --tests '*FabricDriverModuleTest.fabric operation adapter registration does not use binding descriptors*' --tests '*FabricDriverModuleTest.transitional fabric binding operation ids are represented as runtime graph operations*' --tests '*FabricDriverModuleTest.fabric backend exposes bootstrap bindings as graph operation adapters*'
  ```

  Expected: `BUILD SUCCESSFUL`.

- [x] **Step 2: Run full Fabric regression**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test
  ```

  Expected: `BUILD SUCCESSFUL`.

### Task 4: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-fabric-binding-descriptor-removal.md`

- [x] **Step 1: Add Phase 83 governance**

  Add Phase 83 to the active phase list and state that private Fabric bindings
  must not own public descriptors or schemas.

- [x] **Step 2: Add checklist section**

  Record the phase, tests, and the remaining broader binding-exit blocker.

- [x] **Step 3: Record evidence**

  Create evidence with red, green, local, push, and remote CI sections.

### Task 5: Final Verification And Push

**Files:**
- All modified files from previous tasks

- [x] **Step 1: Run final local gates**

  Run:

  ```sh
  git diff --check
  mise run architecture-check
  mise run ci
  ```

  Expected: all exit `0`.

- [ ] **Step 2: Commit and push**

  Run:

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-83-fabric-binding-descriptor-removal-design.md docs/superpowers/plans/2026-06-28-83-fabric-binding-descriptor-removal-plan.md docs/superpowers/evidence/2026-06-28-fabric-binding-descriptor-removal.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
  git commit -m "driver-fabric: remove binding descriptor ownership"
  git push origin main
  ```

- [ ] **Step 3: Verify remote CI**

  Run:

  ```sh
  gh run list --branch main --limit 5
  gh run watch <run-id> --exit-status
  ```

  Expected: pushed `main` CI passes.

## Self-Review

- Spec coverage: plan removes descriptor/schema metadata from private Fabric
  execution bindings and preserves graph-owned public action behavior.
- Placeholder scan: no TBD/TODO placeholders.
- Scope: no new gameplay action, route family, CLI catalog, version lane, or
  support claim.
