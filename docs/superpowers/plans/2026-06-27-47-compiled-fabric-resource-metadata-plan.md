# Compiled Fabric Resource Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Derive Fabric resource metadata from Gradle compiled-lane values so resource metadata cannot drift from the compiled Fabric lane.

**Architecture:** Keep the module compiled against the same verified Fabric/Loom lane. Add Gradle build-time variables for the lane and expand `fabric.mod.json` placeholders during `processResources`; tests verify both the source placeholder contract and processed metadata.

**Tech Stack:** Gradle Kotlin DSL, Fabric Loom resources, Kotlin/JVM tests, Gradle through mise.

---

### Task 1: Add Failing Resource Metadata Tests

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add source-placeholder assertion**

  Add a test that reads `driver-fabric/src/main/resources/fabric.mod.json` and
  asserts it contains `${minecraftVersion}`, `${fabricApiVersion}`,
  `${fabricLoaderVersion}`, `${javaMajorVersion}`, and no literal
  `"Craftless Driver Fabric 1.21.6"` source name.

- [x] **Step 2: Add processed metadata assertions**

  Extend the existing `fabric metadata declares client entrypoint and mixin
  config` test to assert the processed `name`, `description`, `fabric-api`,
  `fabricloader`, `minecraft`, and `java` fields match
  `FabricCompiledLaneMetadata`.

- [x] **Step 3: Run focused tests and verify RED**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric mod source metadata is expanded from compiled lane placeholders*' --tests '*FabricDriverModuleTest.fabric metadata declares client entrypoint and mixin config*'
  ```

  Expected: the new source-placeholder test fails because `fabric.mod.json`
  still hard-codes the current lane.

### Task 2: Expand Resource Metadata From Gradle Values

**Files:**
- Modify: `driver-fabric/build.gradle.kts`
- Modify: `driver-fabric/src/main/resources/fabric.mod.json`

- [x] **Step 1: Define build-time lane values**

  In `build.gradle.kts`, define `fabricCompiledMinecraftVersion`,
  `fabricCompiledYarnMappings`, `fabricCompiledLoaderVersion`,
  `fabricCompiledApiVersion`, `fabricCompiledJavaMajorVersion`,
  `fabricCompiledLaneId`, and `fabricCompiledProviderId` before dependency
  declarations use them.

- [x] **Step 2: Use lane values in dependencies and smoke JSON**

  Replace repeated Gradle dependency and smoke JSON literals with the build-time
  lane values.

- [x] **Step 3: Expand resource placeholders**

  Add `minecraftVersion`, `fabricApiVersion`, `fabricLoaderVersion`, and
  `javaMajorVersion` to `tasks.processResources.inputs.property(...)` and
  `expand(...)`.

- [x] **Step 4: Replace hard-coded resource fields**

  Update `fabric.mod.json` to use placeholders in name, description, and
  dependency values while keeping the entrypoint and mixin config explicit.

- [x] **Step 5: Verify focused tests GREEN**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:processResources :driver-fabric:test --tests '*FabricDriverModuleTest.fabric mod source metadata is expanded from compiled lane placeholders*' --tests '*FabricDriverModuleTest.fabric metadata declares client entrypoint and mixin config*'
  ```

### Task 3: Register Phase And Verify

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Register Phase 47 in `AGENTS.md`**

  Add `47. compiled Fabric resource metadata.` to the active phase list and note
  that the phase expands Fabric resource metadata from build-time compiled-lane
  values only.

- [x] **Step 2: Add checklist evidence**

  Add a Phase 47 checklist section with spec path, plan path, behavior, and
  verification commands.

- [x] **Step 3: Run quality gates**

  Run:

  ```sh
  git diff --check
  mise exec -- gradle :driver-fabric:processResources :driver-fabric:test
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
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-27-47-compiled-fabric-resource-metadata-design.md docs/superpowers/plans/2026-06-27-47-compiled-fabric-resource-metadata-plan.md driver-fabric/build.gradle.kts driver-fabric/src/main/resources/fabric.mod.json driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricClientSmokeController.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricClientSmokePlan.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
  git commit -m "driver-fabric: expand compiled lane resource metadata"
  git push origin main
  ```

- [x] **Step 2: Verify remote CI**

  Run:

  ```sh
  gh run list --repo minekube/craftless --branch main --limit 3
  gh run watch <latest-run-id> --repo minekube/craftless --exit-status
  ```
