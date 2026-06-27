# Asset Index Id Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Use Mojang `assetIndex.id` for prepared asset index handles and launch arguments instead of assuming it equals the Minecraft version.

**Architecture:** Keep the fix in daemon cache preparation. The selected version manifest remains the source of truth for asset index id/url and launch variable resolution. No gameplay OpenAPI, Fabric action, or CLI gameplay catalog changes are allowed.

**Tech Stack:** Kotlin/JVM, kotlinx.serialization JSON parsing, daemon cache tests, Gradle through mise.

---

### Task 1: Add Asset Index Id Regression Tests

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/CachePreparationServiceTest.kt`

- [x] **Step 1: Write the failing handle and launch argument test**

  In `cache preparation resolves and stores minecraft version metadata`, change
  the version manifest fixture to:

  ```json
  "assetIndex": {
    "id": "26",
    "url": "$assetIndexUrl"
  }
  ```

  Add `"--assetIndex", "${assets_index_name}"` to the fixture's game
  arguments.

  Add assertions:

  ```kotlin
  assertEquals("cache/assets/indexes/26.json", result.artifacts.single { it.kind == CachePreparedArtifactKind.MINECRAFT_ASSET_INDEX }.handle)
  assertTrue(launchArgumentsJson.contains("\"--assetIndex\""))
  assertTrue(launchArgumentsJson.contains("\"26\""))
  assertTrue(!launchArgumentsJson.contains("{{assets_index_name}}"))
  assertTrue(Files.readString(workspace.resolve("cache/assets/indexes/26.json")).contains("test.ogg"))
  ```

- [x] **Step 2: Run the focused test and verify RED**

  Run:

  ```sh
  mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.CachePreparationServiceTest.cache preparation resolves and stores minecraft version metadata'
  ```

  Expected: assertion failure because cache preparation still stores the index
  as `cache/assets/indexes/1.21.6.json` and launch arguments do not resolve to
  `26`.

- [x] **Step 3: Write the invalid id test**

  Add:

  ```kotlin
  @Test
  fun `cache preparation rejects invalid asset index ids before writing cache handles`() =
      runBlocking {
          val failure =
              assertFailsWith<IllegalArgumentException> {
                  service.prepare(CachePrepareRequest("1.21.6", Loader.VANILLA))
              }

          assertEquals("Minecraft asset index id must be a file-safe segment", failure.message)
      }
  ```

  The full fixture should set `assetIndex.id = "../26"`.

- [x] **Step 4: Run the validation test and verify RED**

  Run:

  ```sh
  mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.CachePreparationServiceTest.cache preparation rejects invalid asset index ids before writing cache handles'
  ```

  Expected: the test fails because invalid asset index ids are not rejected
  before cache handle construction.

### Task 2: Implement Asset Index Metadata Id

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt`

- [x] **Step 1: Carry asset index id in metadata**

  Change `AssetIndexMetadata` to:

  ```kotlin
  private data class AssetIndexMetadata(
      val id: String,
      val url: String,
  )
  ```

  Validate `id` with `requireFileSafeCacheSegment(id, "Minecraft asset index id")`.

- [x] **Step 2: Parse id and url**

  Update `String.assetIndexMetadata` to read `assetIndex.id` and
  `assetIndex.url`, returning `AssetIndexMetadata(id, url)`.

- [x] **Step 3: Use id in the prepared artifact handle**

  When resolving the base `MINECRAFT_ASSET_INDEX` artifact, copy both `source`
  and:

  ```kotlin
  handle = "cache/assets/indexes/${assetIndexMetadata.id}.json"
  ```

- [x] **Step 4: Resolve launch variable**

  Add `"assets_index_name" to assetIndexMetadata.id` to the cache-time launch
  variables in `launchArgumentsJson`.

- [x] **Step 5: Run focused tests and verify GREEN**

  Run:

  ```sh
  mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.CachePreparationServiceTest.cache preparation resolves and stores minecraft version metadata' --tests 'com.minekube.craftless.daemon.CachePreparationServiceTest.cache preparation rejects invalid asset index ids before writing cache handles'
  ```

  Expected: both tests pass.

### Task 3: Update Guardrails And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Register Phase 44 in `AGENTS.md`**

  Add `44. asset index id.` to the active phase list and note that the phase
  changes supervisor cache/launch metadata only.

- [x] **Step 2: Add checklist evidence**

  Add a Phase 44 checklist section with spec path, plan path, behavior, and
  verification commands.

- [x] **Step 3: Verify docs formatting**

  Run:

  ```sh
  git diff --check
  ```

### Task 4: Verify, Commit, And Push

**Files:**
- Verify the whole repository.

- [x] **Step 1: Run daemon tests**

  Run:

  ```sh
  mise exec -- gradle :daemon:test
  ```

- [x] **Step 2: Run repository quality gates**

  Run:

  ```sh
  mise run lint
  mise run architecture-check
  mise run ci
  ```

- [ ] **Step 3: Commit and push**

  Run:

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-27-44-asset-index-id-design.md docs/superpowers/plans/2026-06-27-44-asset-index-id-plan.md daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/CachePreparationServiceTest.kt
  git commit -m "daemon: honor minecraft asset index ids"
  git push origin main
  ```

- [ ] **Step 4: Verify remote CI**

  Run:

  ```sh
  gh run list --branch main --limit 3
  gh run watch <latest-run-id> --exit-status
  ```

  Expected: the latest `main` CI run passes.
