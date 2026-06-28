# Action Discovery Deletion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete the stale standalone Fabric action-discovery layer so public-compatible Fabric actions remain owned only by the runtime capability graph.

**Architecture:** Add a source guard proving the stale file and type names are absent, then remove action-discovery-specific tests and production code. Move the shared `FabricClientCapabilitySnapshot` model into capability-probe ownership because the runtime graph still uses it to compute availability.

**Tech Stack:** Kotlin/JVM, Gradle via mise, Kotlin test, Markdown.

---

### Task 1: Add Red Guard For Deleted Discovery Layer

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add source guard test**

  Add a test near the existing graph-owned dispatch guard:

  ```kotlin
  @Test
  fun `fabric standalone action discovery layer is removed`() {
      val root = repositoryRoot()
      assertFalse(
          Files.exists(
              root.resolve(
                  "driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionDiscovery.kt",
              ),
          ),
      )
      val sourceFiles =
          root
              .resolve("driver-fabric/src")
              .toFile()
              .walkTopDown()
              .filter { file -> file.isFile && file.extension == "kt" }
              .toList()
      val staleNames =
          listOf(
              "FabricActionDiscovery",
              "FabricActionProbe",
              "FabricActionDiscoveryContext",
              "FabricDiscoveredAction",
              "defaultFabricActionDiscovery",
          )
      val offenders =
          sourceFiles
              .flatMap { file ->
                  val text = file.readText()
                  staleNames
                      .filter { staleName -> text.contains(staleName) }
                      .map { staleName -> root.relativize(file.toPath()).toString() to staleName }
              }
              .filterNot { (path, _) -> path.endsWith("FabricDriverModuleTest.kt") }

      assertEquals(emptyList(), offenders)
  }
  ```

- [x] **Step 2: Run red test**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric standalone action discovery layer is removed*'
  ```

  Expected: FAIL because `FabricActionDiscovery.kt` still exists.

  Evidence: command failed as expected before deletion because
  `FabricActionDiscovery.kt` still existed.

### Task 2: Remove Production Discovery Layer

**Files:**
- Delete: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionDiscovery.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt`

- [x] **Step 1: Move capability snapshot model**

  Add this model near `FabricCapabilityGraphFragment` in
  `FabricCapabilityProbe.kt`:

  ```kotlin
  internal data class FabricClientCapabilitySnapshot(
      val connected: Boolean,
      val player: Boolean,
      val inventory: Boolean,
      val camera: Boolean,
      val interactionManager: Boolean,
      val world: Boolean,
      val recipes: Boolean = false,
      val recipeCrafting: Boolean = false,
  ) {
      companion object {
          fun disconnected(): FabricClientCapabilitySnapshot =
              FabricClientCapabilitySnapshot(
                  connected = false,
                  player = false,
                  inventory = false,
                  camera = false,
                  interactionManager = false,
                  world = false,
              )
      }
  }
  ```

- [x] **Step 2: Delete stale production file**

  Remove `FabricActionDiscovery.kt` completely. Do not copy its action
  discovery interfaces, probes, or descriptor wrapper into another file.

### Task 3: Remove Discovery-Specific Tests

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Delete old discovery tests**

  Remove these tests because they protect the deleted stale layer:

  - `fabric default discovery is composed from runtime probes`
  - `fabric discovery rejects duplicate action ids from probes`
  - `fabric discovery rejects available actions without execution binding`

- [x] **Step 2: Keep graph behavior tests**

  Keep graph-owned action, runtime discovery availability, schema, and invoke
  tests unchanged except where the new source guard requires the stale names to
  disappear.

### Task 4: Verify Focused Green

**Files:**
- Modified files from previous tasks

- [x] **Step 1: Run focused guard**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric standalone action discovery layer is removed*'
  ```

  Expected: PASS.

  Evidence: command passed after deletion.

- [x] **Step 2: Run Fabric tests**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test
  ```

  Expected: `BUILD SUCCESSFUL`.

  Evidence: command passed with `BUILD SUCCESSFUL`.

### Task 5: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-action-discovery-deletion.md`

- [x] **Step 1: Add Phase 80 governance**

  Add Phase 80 to the active sequence and state that standalone Fabric action
  discovery has been deleted.

- [x] **Step 2: Add checklist section**

  Add a Phase 80 checklist section recording the deletion, preserved graph
  ownership, non-goals, and verification commands.

- [x] **Step 3: Record evidence**

  Create the evidence note with red, green, local, push, and remote CI evidence.

  Evidence: `docs/superpowers/evidence/2026-06-28-action-discovery-deletion.md`
  records red/green and pending final gate evidence.

### Task 6: Final Verification And Push

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

  Evidence: `git diff --check`, `mise run architecture-check`, and
  `mise run ci` exited `0` locally.

- [x] **Step 2: Commit and push**

  Run:

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-80-action-discovery-deletion-design.md docs/superpowers/plans/2026-06-28-80-action-discovery-deletion-plan.md docs/superpowers/evidence/2026-06-28-action-discovery-deletion.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionDiscovery.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
  git commit -m "driver-fabric: delete stale action discovery"
  git push origin main
  ```

  Evidence: commit `5c6f2d3a0fb166c32cccc6e261a19ea243c6c90d`
  (`driver-fabric: delete stale action discovery`) was pushed to
  `origin/main`.

- [x] **Step 3: Verify remote CI**

  Run:

  ```sh
  gh run list --branch main --limit 5
  gh run watch <run-id> --exit-status
  ```

  Expected: pushed `main` CI passes.

  Evidence: GitHub Actions run `28310453815` passed for commit
  `5c6f2d3a0fb166c32cccc6e261a19ea243c6c90d`.

## Self-Review

- Spec coverage: the plan adds a red guard, removes stale production/test code,
  preserves the shared capability snapshot model, updates governance, and
  records evidence.
- Placeholder scan: no TBD/TODO/fill-in placeholders.
- Type consistency: stale `FabricActionDiscovery` names are used only in the
  red guard's forbidden-name list and deleted elsewhere.
