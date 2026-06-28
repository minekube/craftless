# Latest Version Alias Resolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve `latest-release` and `latest-snapshot` through Mojang metadata before Craftless prepares cache artifacts.

**Architecture:** Keep alias handling in the daemon cache-preparation layer, immediately after fetching `version_manifest_v2.json`. Build a resolved `CachePrepareRequest` with the concrete Minecraft id and use it for all downstream metadata, cache, launch, Fabric, and Java-runtime derivation.

**Tech Stack:** Kotlin/JVM, Ktor metadata fetcher, Gradle through mise.

---

### Task 1: Add Red Alias Resolution Tests

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/CachePreparationServiceTest.kt`

- [x] **Step 1: Add latest release test**

  Add a test named
  `cache preparation resolves latest release alias before building cache handles`.

  The test should use a static version index like:

  ```json
  {
    "latest": { "release": "1.21.6", "snapshot": "26.3-snapshot-1" },
    "versions": [
      { "id": "1.21.6", "url": "https://metadata.test/1.21.6.json" },
      { "id": "26.3-snapshot-1", "url": "https://metadata.test/26.3-snapshot-1.json" }
    ]
  }
  ```

  Call:

  ```kotlin
  val result = service.prepare(CachePrepareRequest("latest-release", Loader.VANILLA))
  ```

  Assert that `result.minecraftVersion == "1.21.6"`, `result.manifest` is
  `cache/prepared/1.21.6-vanilla.json`, version/client handles are under
  `cache/minecraft/versions/1.21.6/`, and no
  `cache/minecraft/versions/latest-release` directory exists.

- [x] **Step 2: Add latest snapshot test**

  Add a test named
  `cache preparation resolves latest snapshot alias before building cache handles`.

  Call:

  ```kotlin
  val result = service.prepare(CachePrepareRequest("latest-snapshot", Loader.VANILLA))
  ```

  Assert that `result.minecraftVersion == "26.3-snapshot-1"` and the prepared
  manifest is `cache/prepared/26.3-snapshot-1-vanilla.json`.

- [x] **Step 3: Run red tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest.*latest*'
  ```

  Expected: fails before implementation because `latest-release` or
  `latest-snapshot` is searched as an exact version id.

### Task 2: Resolve Aliases Before Preparation

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt`

- [x] **Step 1: Add alias resolver**

  Add a private helper that parses the version index root and returns:

  - `latest.release` for `latest-release`;
  - `latest.snapshot` for `latest-snapshot`;
  - the requested id unchanged for all other requests.

- [x] **Step 2: Use a resolved request**

  In `prepare`, fetch the version index, resolve the concrete Minecraft id,
  create `val resolvedRequest = request.copy(minecraftVersion = resolvedMinecraftVersion)`,
  and use `resolvedRequest` for version manifest lookup, client jar metadata,
  asset index metadata, logging config, Fabric metadata, cache result
  construction, launch arguments, and Java runtime selection.

- [x] **Step 3: Run green focused tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest.*latest*'
  ```

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-latest-version-alias-resolution.md`

- [x] **Step 1: Add Phase 111 to AGENTS**
- [x] **Step 2: Add Phase 111 checklist section**
- [x] **Step 3: Record red/green and local gate evidence**

### Task 4: Verify, Commit, Push

- [x] **Step 1: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 2: Commit and push**

  ```sh
  git add AGENTS.md daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/CachePreparationServiceTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-111-latest-version-alias-resolution-design.md docs/superpowers/plans/2026-06-28-111-latest-version-alias-resolution-plan.md docs/superpowers/evidence/2026-06-28-latest-version-alias-resolution.md
  git commit -m "feat: resolve latest minecraft version aliases"
  git push origin main
  ```

## Self-Review

- Spec coverage: release alias, snapshot alias, concrete cache handles,
  Fabric metadata downstream use, exact-version preservation, governance, and
  verification are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no new compiled lane, gameplay action, public route, CLI gameplay
  catalog, scenario shortcut, or support claim.
