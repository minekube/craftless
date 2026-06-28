# Alias Driver Manifest Loader Default Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make packaged driver-mod manifest loader defaults work for alias requests such as `latest-release`.

**Architecture:** Expose Minecraft alias resolution from `CachePreparationService` as a reusable method backed by the same Mojang version index used during prepare. `WorkspaceClientRuntimeDriverFactory` resolves the requested version before asking `ClientRuntimeDriverModProvider.preferredLoaderVersion(...)`, then passes the original request version plus the preferred loader into `CachePrepareRequest` so cache preparation remains the canonical artifact owner.

**Tech Stack:** Kotlin/JVM daemon and CLI tests, Ktor test HTTP, Gradle through mise.

---

### Task 1: Governance

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-127-alias-driver-manifest-loader-default-design.md`
- Create: `docs/superpowers/plans/2026-06-28-127-alias-driver-manifest-loader-default-plan.md`

- [x] **Step 1: Add Phase 127 to AGENTS.md**

  Add `127. alias driver manifest loader default.` and define that it resolves
  aliases for runtime driver lane selection only.

- [x] **Step 2: Add Phase 127 to the checklist**

  Track the slice as support-enabling work that still does not complete latest
  or older runtime support by itself.

### Task 2: Add Red Alias Tests

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`
- Modify: `cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt`

- [x] **Step 1: Add daemon alias create-client test**

  Add `prepared runtime resolves aliases before driver mod provider preference`.
  Use `latest-release` in the request, a manifest entry for resolved `1.21.6`
  / `0.16.14`, and assert the launched prepared manifest is
  `cache/prepared/1.21.6-fabric-0.16.14.json`.

- [x] **Step 2: Add packaged CLI alias test**

  Add `server start resolves aliases before packaged driver mod manifest defaults`.
  Use a packaged manifest entry for `1.21.6` / `0.16.14`, request
  `version=latest-release` without `loaderVersion`, and assert HTTP 201 plus
  the `1.21.6-fabric-0.16.14` prepared manifest.

- [x] **Step 3: Run red alias tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.*resolves aliases before driver mod provider preference*'
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*resolves aliases before packaged driver mod manifest defaults*'
  ```

  Expected: fail before implementation because the manifest provider sees
  `latest-release`, not `1.21.6`.

### Task 3: Implement Alias Resolution Reuse

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt`

- [x] **Step 1: Extract alias resolution method**

  Add:

  ```kotlin
  suspend fun resolveMinecraftVersionAlias(minecraftVersion: String): String {
      val versionIndex = metadataFetcher.fetchText(MINECRAFT_VERSION_INDEX_URL)
      return versionIndex.resolveMinecraftVersion(minecraftVersion)
  }
  ```

  Update `prepare(...)` to call this method instead of duplicating the version
  index fetch inline.

- [x] **Step 2: Use resolved version for provider preference**

  In `WorkspaceClientRuntimeDriverFactory.prepare`, resolve the requested
  version only when `request.loaderVersion == null`, then pass the resolved
  version to `preferredLoaderVersion(...)`. Keep the original
  `request.version` in `CachePrepareRequest.minecraftVersion`.

### Task 4: Evidence, Verification, Commit

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-alias-driver-manifest-loader-default.md`

- [x] **Step 1: Run focused tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.*resolves aliases before driver mod provider preference*' --tests '*LocalSessionApiServerTest.*defaults loader version from driver mod provider preference*'
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*resolves aliases before packaged driver mod manifest defaults*' --tests '*CraftlessCliTest.*defaults loader version from packaged driver mod manifest*'
  ```

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [ ] **Step 3: Record evidence and push**

  ```sh
  git add AGENTS.md daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-127-alias-driver-manifest-loader-default-design.md docs/superpowers/plans/2026-06-28-127-alias-driver-manifest-loader-default-plan.md docs/superpowers/evidence/2026-06-28-alias-driver-manifest-loader-default.md
  git commit -m "feat: resolve aliases before driver manifest defaults"
  git push origin main
  ```

## Self-Review

- Spec coverage: alias resolution, cache service ownership, exact-version
  preservation, explicit-loader precedence, tests, evidence, and local gates
  are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no gameplay actions, scenario shortcuts, public protocol additions,
  compiled lanes, or support claims.
