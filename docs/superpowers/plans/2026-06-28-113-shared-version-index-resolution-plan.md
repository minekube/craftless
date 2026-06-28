# Shared Version Index Resolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Java runtime resolution and cache preparation share Mojang version-index alias resolution.

**Architecture:** Extract version-index helpers into a daemon package file. Cache preparation and `JavaRuntimeService` both resolve aliases through the same helper before fetching the concrete version manifest.

**Tech Stack:** Kotlin/JVM, daemon and CLI tests, Gradle through mise.

---

### Task 1: Add Red Java Runtime Alias Test

**Files:**
- Modify: `cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt`

- [x] **Step 1: Add CLI alias test**

  Add a test named
  `runtimes java resolve resolves latest release alias through supervisor api`.

  Use a static version index with:

  ```json
  {
    "latest": { "release": "26.2", "snapshot": "26.3-snapshot-1" },
    "versions": [{ "id": "26.2", "url": "https://metadata.test/26.2.json" }]
  }
  ```

  Run:

  ```kotlin
  CraftlessCli.run(
      listOf("runtimes", "java", "resolve", "--mc", "latest-release", "--api", server.url),
      stdout = { output.appendLine(it) },
  )
  ```

  Assert exit code `0`, selected status, and Java major version `25`.

- [x] **Step 2: Run red test**

  ```sh
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.runtimes java resolve resolves latest release alias through supervisor api*'
  ```

  Expected: fails before implementation because `JavaRuntimeService` looks up
  `latest-release` as an exact version id.

### Task 2: Extract Shared Version Index Helper

**Files:**
- Create: `daemon/src/main/kotlin/com/minekube/craftless/daemon/MinecraftVersionIndex.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/JavaRuntimeService.kt`

- [x] **Step 1: Create helper file**

  Move the version-index helpers into daemon scope:

  ```kotlin
  internal fun String.resolveMinecraftVersion(minecraftVersion: String): String
  internal fun String.versionManifestUrl(minecraftVersion: String): String
  internal fun requireFileSafeCacheSegment(value: String, label: String)
  ```

- [x] **Step 2: Use helper in cache preparation**

  Remove the private duplicate helper functions from
  `CachePreparationService.kt`; keep existing call sites unchanged.

- [x] **Step 3: Use helper in Java runtime service**

  In `JavaRuntimeService.versionManifest`, resolve the alias first and fetch
  the concrete version manifest URL.

- [x] **Step 4: Run focused green tests**

  ```sh
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.runtimes java resolve resolves latest release alias through supervisor api*'
  mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest.*latest*'
  ```

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-shared-version-index-resolution.md`

- [x] **Step 1: Add Phase 113 to AGENTS**
- [x] **Step 2: Add Phase 113 checklist section**
- [x] **Step 3: Record red/green and local gate evidence**

### Task 4: Verify, Commit, Push

- [x] **Step 1: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 2: Commit and push**

  ```sh
  git add AGENTS.md cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt daemon/src/main/kotlin/com/minekube/craftless/daemon/MinecraftVersionIndex.kt daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt daemon/src/main/kotlin/com/minekube/craftless/daemon/JavaRuntimeService.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-113-shared-version-index-resolution-design.md docs/superpowers/plans/2026-06-28-113-shared-version-index-resolution-plan.md docs/superpowers/evidence/2026-06-28-shared-version-index-resolution.md
  git commit -m "fix: share minecraft version alias resolution"
  git push origin main
  ```

## Self-Review

- Spec coverage: Java runtime alias resolution, shared helper, cache alias
  preservation, governance, and verification are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no new compiled lane, gameplay action, public route, CLI gameplay
  catalog, scenario shortcut, or support claim.
