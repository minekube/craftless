# Active Unsupported Lane Fixture Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the historical latest unsupported lane id from active smoke fixtures.

**Architecture:** Keep the product matrix generic. Add a source guard in the existing Fabric module policy tests and update the testkit smoke fixture to use the generic unsupported fallback lane id and reason.

**Tech Stack:** Kotlin/JVM tests, Gradle through mise.

---

### Task 1: Add Active Fixture Guard

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add red guard**

  Add `active smoke fixtures do not keep static latest unsupported lane ids`.

- [x] **Step 2: Run red guard**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.active smoke fixtures do not keep static latest unsupported lane ids*'
  ```

  Expected initial result: FAIL because `LocalMinecraftServerSmokeTest.kt`
  contained `latest-release-26-2`.

### Task 2: Replace Active Fixture

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmokeTest.kt`

- [x] **Step 1: Use generic unsupported lane**

  Replace `latest-release-26-2` with `fabric-unsupported-26-2`, provider
  `fabric-unsupported`, and reason `unsupported-version`.

- [x] **Step 2: Run focused tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.active smoke fixtures do not keep static latest unsupported lane ids*'
  mise exec -- gradle :testkit:test --tests '*LocalMinecraftServerSmokeTest.local server smoke records unsupported runtime lane without provisioning server*'
  ```

  Expected result: PASS.

### Task 3: Document And Verify

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-active-unsupported-lane-fixture-cleanup.md`

- [x] **Step 1: Update docs**

  Register Phase 105 and mark it as active-source alignment only.

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.active smoke fixtures do not keep static latest unsupported lane ids*'
  mise exec -- gradle :testkit:test --tests '*LocalMinecraftServerSmokeTest.local server smoke records unsupported runtime lane without provisioning server*'
  ```

- [ ] **Step 3: Commit and push**

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-105-active-unsupported-lane-fixture-cleanup-design.md docs/superpowers/plans/2026-06-28-105-active-unsupported-lane-fixture-cleanup-plan.md docs/superpowers/evidence/2026-06-28-active-unsupported-lane-fixture-cleanup.md driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmokeTest.kt
  git commit -m "test: remove static latest lane smoke fixture"
  git push origin main
  ```

## Self-Review

- Scope: active smoke fixture cleanup only.
- Static gameplay scan: no gameplay action, route, CLI catalog, or scenario
  shortcut is added.
- Version support scan: no runnable latest/older support claim is added.
