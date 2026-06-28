# Launch Mod Materialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Copy cached Fabric mod artifacts from `CacheLaunchPlan.mods` into the instance mods directory before launching a prepared client.

**Architecture:** Extend `ProcessClientRuntimeLauncher.launch` to call a small helper before building/starting the process. The helper resolves each mod handle under the workspace root, creates the instance mods directory, and copies each jar using the source filename to preserve inspectable artifacts.

**Tech Stack:** Kotlin/JVM, java.nio.file, existing daemon process-launch tests.

---

### Task 1: Add Red Process Launcher Test

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`

- [x] **Step 1: Extend process launcher test**

  In `process client runtime launcher starts prepared command`, create:

  ```kotlin
  val fabricApiHandle = "cache/mods/fabric/fabric-api.jar"
  Files.createDirectories(workspace.resolve("cache/mods/fabric"))
  Files.writeString(workspace.resolve(fabricApiHandle), "fabric-api-jar")
  ```

  Add `mods = listOf(fabricApiHandle)` to the `CacheLaunchPlan`.

  After launch completion, assert:

  ```kotlin
  val materializedFabricApi =
      workspace.resolve("instances/alice-1.21.6-fabric/minecraft/mods/fabric-api.jar")
  assertEquals("fabric-api-jar", Files.readString(materializedFabricApi))
  ```

- [x] **Step 2: Run red test**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.process client runtime launcher starts prepared command*'
  ```

  Expected: fails because the launcher does not copy `launch.mods`.

### Task 2: Implement Mod Materialization

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt`

- [x] **Step 1: Call materializer before command construction**

  In `ProcessClientRuntimeLauncher.launch`, call:

  ```kotlin
  materializeLaunchMods(prepared.launch, files, workspaceRoot)
  ```

  before `launchCommand(...)`.

- [x] **Step 2: Add helper**

  Add a private helper:

  ```kotlin
  private fun materializeLaunchMods(
      launch: CacheLaunchPlan,
      files: InstanceFiles,
      workspaceRoot: Path,
  ) {
      if (launch.mods.isEmpty()) return
      val modsDirectory = workspaceRoot.resolveHandleOrPath(files.mods)
      Files.createDirectories(modsDirectory)
      launch.mods.forEach { handle ->
          val source = workspaceRoot.resolveHandleOrPath(handle)
          require(Files.isRegularFile(source)) { "prepared mod artifact does not exist: $handle" }
          val target = modsDirectory.resolve(source.fileName.toString()).normalize()
          require(target.startsWith(modsDirectory)) { "materialized mod target must stay under mods directory" }
          Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
      }
  }
  ```

- [x] **Step 3: Run focused green**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.process client runtime launcher starts prepared command*'
  ```

### Task 3: Register Phase 95 And Verify

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-launch-mod-materialization.md`

- [x] **Step 1: Register Phase 95**

  Add `95. launch mod materialization.` to AGENTS and document that resolved
  Fabric mods must be materialized into instance files before launch.

- [x] **Step 2: Update checklist and evidence**

  Add Phase 95 checklist entries and evidence with red, green, local gates, and
  remote-CI policy.

- [x] **Step 3: Run local gates**

  ```sh
  git diff --check
  mise exec -- gradle :daemon:test
  ```

### Task 4: Commit And Push

**Files:**
- All modified files from Tasks 1-3

- [x] **Step 1: Commit and push**

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-95-launch-mod-materialization-design.md docs/superpowers/plans/2026-06-28-95-launch-mod-materialization-plan.md docs/superpowers/evidence/2026-06-28-launch-mod-materialization.md daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt
  git commit -m "daemon: materialize launch mods"
  git push origin main
  ```

## Self-Review

- Spec coverage: mod copy behavior, path containment, test, docs/evidence, and
  gates are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no new support claim, public version-specific API, gameplay action,
  route family, or CLI catalog.
