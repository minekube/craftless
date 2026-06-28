# Binding Adapter Key Derivation Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove backend derivation of private Fabric adapter keys from operation ids.

**Architecture:** Add a red source guard that rejects backend adapter-key derivation, then route private binding adapter registration through the bootstrap operation definition mapping. This keeps adapter-key ownership in the bootstrap graph definition layer while preserving current behavior.

**Tech Stack:** Kotlin/JVM, Gradle via mise, Kotlin test, Markdown.

---

### Task 1: Add Red Adapter Derivation Guard

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add backend source guard**

  Add a test named
  `fabric backend does not derive binding adapter keys from operation ids`.

  It must read
  `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt`
  and assert it does not contain:

  - `fabricOperationAdapterKey`
  - `replace(".", "-")`

- [x] **Step 2: Run red guard**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric backend does not derive binding adapter keys from operation ids*'
  ```

  Expected: fails before implementation because `FabricDriverBackend.kt`
  currently derives adapter keys from operation ids.

### Task 2: Register Bindings With Bootstrap Adapter Keys

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt`

- [x] **Step 1: Add bootstrap adapter lookup**

  Add an internal backend lookup from bootstrap operation id to bootstrap
  adapter key using `fabricBootstrapOperationDefinitions()`.

- [x] **Step 2: Use lookup in binding registration**

  Replace `binding.operationId.fabricOperationAdapterKey()` with a lookup from
  the bootstrap operation definition mapping. Fail fast if a private binding
  references an operation id without a bootstrap adapter definition.

- [x] **Step 3: Delete derivation helper**

  Remove `private fun String.fabricOperationAdapterKey()`.

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-binding-adapter-key-derivation-removal.md`

- [x] **Step 1: Add Phase 88 to AGENTS**
- [x] **Step 2: Add checklist section**
- [x] **Step 3: Record red and green evidence**

### Task 4: Final Verification And Push

**Files:**
- All modified files from previous tasks

- [x] **Step 1: Run focused green tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric backend does not derive binding adapter keys from operation ids*' --tests '*FabricDriverModuleTest.fabric backend exposes bootstrap bindings as graph operation adapters*'
  ```

- [x] **Step 2: Run source scan and forced local gates**

  ```sh
  rg -n 'fabricOperationAdapterKey|replace\("\\.", "-"\)' driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt
  git diff --check
  mise exec -- gradle lint test --rerun-tasks
  mise exec -- bun test playwright
  ```

- [x] **Step 3: Commit and push**

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-88-binding-adapter-key-derivation-removal-design.md docs/superpowers/plans/2026-06-28-88-binding-adapter-key-derivation-removal-plan.md docs/superpowers/evidence/2026-06-28-binding-adapter-key-derivation-removal.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
  git commit -m "driver-fabric: remove binding adapter key derivation"
  git push origin main
  ```

## Self-Review

- Spec coverage: guard, bootstrap lookup, helper deletion, governance, and
  verification are covered.
- Placeholder scan: no TBD/TODO placeholders.
- Scope: no new gameplay action, route family, CLI catalog, Fabric binding,
  version lane, or support claim.
