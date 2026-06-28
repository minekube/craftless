# Version-Aware Driver Mod Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the daemon driver-mod provider boundary receive the requested runtime lane so future packaged driver artifacts can be selected by Minecraft/Fabric version.

**Architecture:** Add a daemon-owned request DTO for driver-mod selection. Build it after cache preparation, when the resolved Fabric loader version is known, and pass it to the provider before copying the selected mod into the prepared launch plan.

**Tech Stack:** Kotlin/JVM, Gradle tests through mise, existing Ktor local API tests.

---

### Task 1: Add Failing Version-Aware Provider Test

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`

- [x] **Step 1: Add focused test**

  Add a test named
  `prepared runtime asks driver mod provider for requested runtime lane`.

  The test should create a `ClientRuntimeDriverModProvider` lambda that records
  the received `ClientRuntimeDriverModRequest`, launches a Fabric client through
  `LocalSessionApiServer.inMemory`, and asserts the provider saw:

  ```kotlin
  ClientRuntimeDriverModRequest(
      loader = Loader.FABRIC,
      minecraftVersion = "1.21.6",
      loaderVersion = "0.17.2",
  )
  ```

- [x] **Step 2: Run red test**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.prepared runtime asks driver mod provider for requested runtime lane*'
  ```

  Expected: fails before implementation because the provider API is still
  loader-only.

### Task 2: Implement Version-Aware Provider Boundary

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt`

- [x] **Step 1: Add request DTO**

  Add:

  ```kotlin
  data class ClientRuntimeDriverModRequest(
      val loader: Loader,
      val minecraftVersion: String,
      val loaderVersion: String?,
  ) {
      init {
          require(minecraftVersion.isNotBlank()) { "driver mod Minecraft version is required" }
      }
  }
  ```

- [x] **Step 2: Change provider interface**

  Change:

  ```kotlin
  fun interface ClientRuntimeDriverModProvider {
      fun modFor(loader: Loader): Path?
  }
  ```

  To:

  ```kotlin
  fun interface ClientRuntimeDriverModProvider {
      fun modFor(request: ClientRuntimeDriverModRequest): Path?
  }
  ```

- [x] **Step 3: Pass runtime request into mod selection**

  Change `withConfiguredDriverMod` so it accepts the original create request
  and builds:

  ```kotlin
  ClientRuntimeDriverModRequest(
      loader = request.loader,
      minecraftVersion = request.version,
      loaderVersion = loaderVersion,
  )
  ```

- [x] **Step 4: Preserve configured Fabric fallback**

  Update `ConfiguredClientRuntimeDriverModProvider` and
  `NoClientRuntimeDriverModProvider` to use `request.loader`.

- [x] **Step 5: Run focused green test**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.prepared runtime asks driver mod provider for requested runtime lane*'
  ```

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-version-aware-driver-mod-selection.md`

- [x] **Step 1: Add Phase 107 to AGENTS**

  Add `107. version-aware driver mod selection.` after Phase 106.

- [x] **Step 2: Add checklist phase**

  Add Phase 107 with checked spec, plan, implementation, and verification
  bullets. Keep the final completion gate open for real runnable latest/older
  support.

- [x] **Step 3: Record evidence**

  Record red/green focused test commands and local gate commands in the
  evidence file.

### Task 4: Verify, Commit, Push

- [x] **Step 1: Run compatibility tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.prepared runtime launch plan includes configured craftless fabric driver mod*' --tests '*LocalSessionApiServerTest.prepared runtime asks driver mod provider for requested runtime lane*'
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.server start forwards configured fabric driver mod environment*' --tests '*CraftlessCliTest.server start uses packaged fabric driver mod when env is absent*'
  ```

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 3: Commit and push**

  ```sh
  git add AGENTS.md daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-107-version-aware-driver-mod-selection-design.md docs/superpowers/plans/2026-06-28-107-version-aware-driver-mod-selection-plan.md docs/superpowers/evidence/2026-06-28-version-aware-driver-mod-selection.md
  git commit -m "feat: pass runtime lane to driver mod selection"
  git push origin main
  ```

## Self-Review

- Spec coverage: provider request, resolved loader version, existing fallback,
  governance, and verification are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: this does not add a new compiled lane, gameplay action, public route,
  CLI gameplay catalog, or support claim.
