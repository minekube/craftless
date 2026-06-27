# Compiled Fabric Lane Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Centralize current compiled Fabric lane metadata for Kotlin runtime/provider/matrix/smoke code without changing public gameplay APIs.

**Architecture:** Add an internal `FabricCompiledLaneMetadata` object in the `driver-fabric` runtime package. Runtime matrix, current provider, and smoke/final gameplay plans read from that object while Gradle/Fabric resource constants remain build-time inputs for the compiled Loom lane.

**Tech Stack:** Kotlin/JVM, Fabric driver runtime facades, Gradle through mise.

---

### Task 1: Add Failing Metadata Tests

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrixTest.kt`
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCurrentLaneRuntimeProviderTest.kt`
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add matrix metadata assertion**

  Add a test proving `defaultFabricCompatibilityMatrix()` resolves the current
  lane from `FabricCompiledLaneMetadata`.

- [x] **Step 2: Add provider metadata assertion**

  Add assertions that `FabricCurrentLaneRuntimeProvider().id` and matching
  support use `FabricCompiledLaneMetadata.providerId` and
  `FabricCompiledLaneMetadata.minecraftVersion`.

- [x] **Step 3: Add smoke-plan metadata assertion**

  Add a test proving `FabricClientSmokePlan.default().minecraftVersion` and
  `FabricFinalGameplayPlan.default().minecraftVersion` equal
  `FabricCompiledLaneMetadata.minecraftVersion`.

- [x] **Step 4: Run focused tests and verify RED**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricCompatibilityMatrixTest*' --tests '*FabricCurrentLaneRuntimeProviderTest*' --tests '*FabricDriverModuleTest*'
  ```

  Expected: compilation fails because `FabricCompiledLaneMetadata` does not
  exist yet.

### Task 2: Add Metadata Object And Use It

**Files:**
- Create: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompiledLaneMetadata.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrix.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCurrentLaneRuntimeProvider.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricClientSmokePlan.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricFinalGameplayPlan.kt`

- [x] **Step 1: Add internal metadata object**

  Create `FabricCompiledLaneMetadata` with `id`, `providerId`,
  `minecraftVersion`, `loaderVersion`, `fabricApiVersion`, `javaMajorVersion`,
  and `mappingsFingerprint`.

- [x] **Step 2: Use metadata in compatibility matrix**

  Replace repeated current-lane literals in `defaultFabricCompatibilityMatrix`
  with `FabricCompiledLaneMetadata`.

- [x] **Step 3: Use metadata in current provider and plans**

  Replace repeated current-lane literals in the provider and plan defaults with
  `FabricCompiledLaneMetadata`.

- [x] **Step 4: Run focused tests and verify GREEN**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricCompatibilityMatrixTest*' --tests '*FabricCurrentLaneRuntimeProviderTest*' --tests '*FabricDriverModuleTest*'
  ```

### Task 3: Register Phase And Verify

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Register Phase 46 in `AGENTS.md`**

  Add `46. compiled Fabric lane metadata.` to the active phase list and note
  that the phase centralizes Kotlin runtime metadata only.

- [x] **Step 2: Add checklist evidence**

  Add a Phase 46 checklist section with spec path, plan path, behavior, and
  verification commands.

- [x] **Step 3: Run quality gates**

  Run:

  ```sh
  git diff --check
  mise exec -- gradle :driver-fabric:test
  mise run lint
  mise run architecture-check
  mise run ci
  ```

### Task 4: Commit, Push, And Monitor

**Files:**
- Commit the phase files and implementation.

- [x] **Step 1: Commit and push**

  Run:

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-27-46-compiled-fabric-lane-metadata-design.md docs/superpowers/plans/2026-06-27-46-compiled-fabric-lane-metadata-plan.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompiledLaneMetadata.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrix.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCurrentLaneRuntimeProvider.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricClientSmokePlan.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricFinalGameplayPlan.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrixTest.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCurrentLaneRuntimeProviderTest.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
  git commit -m "driver-fabric: centralize compiled lane metadata"
  git push origin main
  ```

- [x] **Step 2: Verify remote CI**

  Run:

  ```sh
  gh run list --repo minekube/craftless --branch main --limit 3
  gh run watch <latest-run-id> --repo minekube/craftless --exit-status
  ```
