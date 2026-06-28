# Metadata Binary Checksums Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Feed upstream SHA-1 metadata into cache binary artifacts so corrupt cached client, library, native, runtime, and Fabric profile downloads are replaced instead of reused.

**Architecture:** Keep `CachePreparedArtifact.sha1` as the public cache metadata field. Update daemon metadata parsers to carry optional SHA-1 beside each URL, then rely on the existing `writeFetchedBytesArtifact(...)` verification path from Phase 73.

**Tech Stack:** Kotlin/JVM, kotlinx.serialization JSON parsing, Ktor Client, Gradle through mise.

---

### Task 1: Add Failing Binary Checksum Test

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/CachePreparationServiceTest.kt`

- [x] **Step 1: Write failing test**

  Add a test named `cache preparation refetches corrupt metadata checksum binaries`.

  The test should:

  - create fake metadata for a client jar, one Minecraft library, one selected
    native classifier, one Java runtime executable, one Java runtime file, and
    one Fabric profile `downloads.artifact` library;
  - include correct SHA-1 fields for the downloaded fake bytes;
  - pre-write corrupt bytes into the cache handles for each artifact;
  - call `CachePreparationService.prepare(CachePrepareRequest("1.21.6", Loader.FABRIC))`;
  - assert each corrupt file was replaced with valid downloaded bytes;
  - assert each URL was fetched exactly once;
  - call `prepare(...)` a second time;
  - assert each URL fetch count remains one because valid cached bytes are
    reused.

- [x] **Step 2: Verify RED**

  Run:

  ```sh
  mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest.cache preparation refetches corrupt metadata checksum binaries'
  ```

  Expected before implementation: FAIL because checksum metadata is not carried
  for these artifact types, so corrupt files are reused.

### Task 2: Carry SHA-1 Through Metadata Parsers

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt`

- [x] **Step 1: Add internal download metadata type**

  Add:

  ```kotlin
  private data class DownloadArtifactMetadata(
      val url: String,
      val sha1: String?,
  )
  ```

- [x] **Step 2: Parse client jar download metadata**

  Replace `String.clientJarUrl(minecraftVersion: String): String` with
  `String.clientJarDownload(minecraftVersion: String): DownloadArtifactMetadata`
  and read both `url` and optional `sha1`.

- [x] **Step 3: Parse library download metadata**

  Update `minecraftLibraries()` and `minecraftNativeLibraries()` to pass
  optional SHA-1 into `MinecraftLibraryArtifact` and
  `MinecraftNativeLibraryArtifact`.

- [x] **Step 4: Parse Java runtime raw file SHA-1**

  Update `javaRuntimeFiles(...)` to pass optional raw download SHA-1 into
  `JavaRuntimeFileArtifact`.

- [x] **Step 5: Parse Fabric profile artifact SHA-1**

  Update `fabricLibraries()` so `downloads.artifact` entries keep optional
  SHA-1. Maven-coordinate-derived Fabric library URLs should keep `sha1 = null`.

### Task 3: Populate Cache Artifacts

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt`

- [x] **Step 1: Populate client jar SHA-1**

  When resolving `CachePreparedArtifactKind.MINECRAFT_CLIENT_JAR`, copy both
  source URL and optional SHA-1 into the artifact.

- [x] **Step 2: Populate Minecraft library/native SHA-1**

  Add `sha1` properties to `MinecraftLibraryArtifact` and
  `MinecraftNativeLibraryArtifact`, then set `CachePreparedArtifact.sha1`.

- [x] **Step 3: Populate Java runtime SHA-1**

  Add `sha1` to `JavaRuntimeFileArtifact` and set it on executable/file
  artifacts.

- [x] **Step 4: Populate Fabric artifact SHA-1**

  Add `sha1` to `FabricLibraryArtifact` and set it on `CachePreparedArtifact`
  only when metadata provided it.

- [x] **Step 5: Verify GREEN**

  Run:

  ```sh
  mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest.cache preparation refetches corrupt metadata checksum binaries'
  ```

### Task 4: Register Phase 74 And Verify

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-74-metadata-binary-checksums-design.md`
- Create: `docs/superpowers/plans/2026-06-28-74-metadata-binary-checksums-plan.md`

- [x] **Step 1: Register governance**

  Add Phase 74 to `AGENTS.md` and the checklist as metadata-backed binary
  checksum validation only. State that it adds no gameplay API, compiled lane,
  or support claim.

- [x] **Step 2: Run verification**

  Run:

  ```sh
  mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest.cache preparation refetches corrupt metadata checksum binaries'
  mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest*'
  git diff --check
  mise run architecture-check
  mise run ci
  ```

- [x] **Step 3: Commit, push, and verify CI**

  Run:

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-74-metadata-binary-checksums-design.md docs/superpowers/plans/2026-06-28-74-metadata-binary-checksums-plan.md daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/CachePreparationServiceTest.kt
  git commit -m "daemon: verify metadata binary checksums"
  git push origin main
  gh run watch <latest-run-id> --repo minekube/craftless --exit-status
  ```

  Evidence: `cb72b3b` pushed to `main`; GitHub Actions run
  `28307751811` completed successfully for `mise run ci`.

### Guardrails

- [x] No public gameplay action, route family, Fabric descriptor/binding pair,
  CLI gameplay catalog, scenario shortcut, compiled Fabric lane, public
  version-specific API, or Minecraft support claim is added.
- [x] Artifacts without upstream SHA-1 metadata keep the previous reuse
  behavior.
- [x] This phase improves cache integrity for multi-version probes only.
