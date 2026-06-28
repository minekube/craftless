# Local Server Latest Alias Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make local server smoke provisioning accept Mojang latest aliases and cache the server jar by the resolved concrete version.

**Architecture:** Move Mojang version-index alias and version-manifest lookup helpers into the shared protocol module. Daemon runtime metadata paths and testkit server provisioning consume the same helper.

**Tech Stack:** Kotlin/JVM, Ktor client, testkit and daemon tests, Gradle through mise.

---

### Task 1: Add Red Server Provisioning Alias Test

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/MinecraftServerJarProvisionerTest.kt`

- [x] **Step 1: Add alias provisioning test**

  Add a test named
  `fixture provisions latest release server jar under resolved version`.

  Use a static version index with:

  ```json
  {
    "latest": { "release": "26.2", "snapshot": "26.3-snapshot-1" },
    "versions": [{ "id": "26.2", "url": "https://example.test/versions/26.2.json" }]
  }
  ```

  Request `layout.provisionMinecraftServerJar(version = "latest-release", ...)`.
  Assert the provisioned path is
  `artifacts/minecraft-server-26.2.jar`, the downloaded bytes are written, and
  the requested URLs use `26.2`.

- [x] **Step 2: Run red test**

  ```sh
  mise exec -- gradle :testkit:test --tests '*MinecraftServerJarProvisionerTest.fixture provisions latest release server jar under resolved version*'
  ```

  Expected: fails before implementation because `latest-release` is searched as
  an exact version id.

### Task 2: Share Version Index Helpers

**Files:**
- Create: `protocol/src/main/kotlin/com/minekube/craftless/protocol/MinecraftVersionIndex.kt`
- Delete or reduce: `daemon/src/main/kotlin/com/minekube/craftless/daemon/MinecraftVersionIndex.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/JavaRuntimeService.kt`
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/MinecraftServerJarProvisioner.kt`

- [x] **Step 1: Create protocol helper**

  Add shared functions for:

  ```kotlin
  fun String.resolveMinecraftVersion(minecraftVersion: String): String
  fun String.versionManifestUrl(minecraftVersion: String): String
  fun requireFileSafeMinecraftVersionSegment(value: String, label: String)
  ```

- [x] **Step 2: Update daemon imports**

  Use the shared protocol functions for cache preparation and Java runtime
  metadata.

- [x] **Step 3: Update testkit provisioning**

  Resolve the requested server version after fetching the Mojang index, fetch
  the concrete version metadata, and write the jar to a concrete-version path.

- [x] **Step 4: Run focused green tests**

  ```sh
  mise exec -- gradle :testkit:test --tests '*MinecraftServerJarProvisionerTest.*'
  mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest.*latest*'
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.runtimes java resolve resolves latest release alias through supervisor api*'
  ```

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-local-server-latest-alias.md`

- [x] **Step 1: Add Phase 115 to AGENTS**
- [x] **Step 2: Add Phase 115 checklist section**
- [x] **Step 3: Record red/green and local gate evidence**

### Task 4: Verify, Commit, Push

- [x] **Step 1: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 2: Commit and push**

  ```sh
  git add AGENTS.md protocol/src/main/kotlin/com/minekube/craftless/protocol/MinecraftVersionIndex.kt daemon/src/main/kotlin/com/minekube/craftless/daemon/MinecraftVersionIndex.kt daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt daemon/src/main/kotlin/com/minekube/craftless/daemon/JavaRuntimeService.kt testkit/src/main/kotlin/com/minekube/craftless/testkit/MinecraftServerJarProvisioner.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/MinecraftServerJarProvisionerTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-115-local-server-latest-alias-design.md docs/superpowers/plans/2026-06-28-115-local-server-latest-alias-plan.md docs/superpowers/evidence/2026-06-28-local-server-latest-alias.md
  git commit -m "fix: resolve latest aliases for server provisioning"
  git push origin main
  ```

## Self-Review

- Spec coverage: local server latest aliases, concrete artifact paths, shared
  helper, governance, and verification are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no new compiled lane, gameplay action, public route, CLI gameplay
  catalog, scenario shortcut, or support claim.
