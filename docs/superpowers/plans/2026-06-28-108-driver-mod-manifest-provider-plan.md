# Driver Mod Manifest Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let configured driver-mod selection use a local manifest keyed by runtime lane before falling back to the single Fabric driver env var.

**Architecture:** Extend `ConfiguredClientRuntimeDriverModProvider` with a JSON manifest reader. Keep the provider daemon-owned, file-system based, and independent from `driver-fabric`; later packaging can generate the manifest without changing launcher code.

**Tech Stack:** Kotlin/JVM, kotlinx.serialization JSON already available in daemon, Gradle tests through mise.

---

### Task 1: Add Failing Manifest Provider Tests

**Files:**
- Create: `daemon/src/test/kotlin/com/minekube/craftless/daemon/ConfiguredClientRuntimeDriverModProviderTest.kt`

- [x] **Step 1: Add exact-match and fallback tests**

  Add tests that verify:

  - `CRAFTLESS_DRIVER_MOD_MANIFEST` selects a relative manifest entry for
    `FABRIC`, `1.21.6`, and `0.17.2`;
  - the exact manifest entry wins over `CRAFTLESS_FABRIC_DRIVER_MOD`;
  - when the manifest has no matching Minecraft version, the provider returns
    the `CRAFTLESS_FABRIC_DRIVER_MOD` fallback.

- [x] **Step 2: Run red tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest*'
  ```

  Expected: fails before implementation because
  `CRAFTLESS_DRIVER_MOD_MANIFEST` does not exist.

### Task 2: Implement Manifest Selection

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt`

- [x] **Step 1: Add environment key**

  Add `CRAFTLESS_DRIVER_MOD_MANIFEST` to
  `ConfiguredClientRuntimeDriverModProvider.Companion`.

- [x] **Step 2: Add manifest DTOs**

  Add private serializable DTOs near `ConfiguredClientRuntimeDriverModProvider`:

  ```kotlin
  @Serializable
  private data class ConfiguredDriverModManifest(val entries: List<ConfiguredDriverModManifestEntry> = emptyList())

  @Serializable
  private data class ConfiguredDriverModManifestEntry(
      val loader: Loader,
      val minecraftVersion: String,
      val loaderVersion: String? = null,
      val path: String,
  )
  ```

- [x] **Step 3: Match request before fallback**

  In `ConfiguredClientRuntimeDriverModProvider.modFor`, first call a manifest
  selector. Match exact loader and Minecraft version. Prefer exact
  `loaderVersion`; otherwise allow an entry whose `loaderVersion` is null.

- [x] **Step 4: Resolve manifest paths**

  Resolve relative entry paths against the manifest file parent. Leave absolute
  paths absolute.

- [x] **Step 5: Run green tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest*'
  ```

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-driver-mod-manifest-provider.md`

- [x] **Step 1: Add Phase 108 to AGENTS**
- [x] **Step 2: Add Phase 108 checklist section**
- [x] **Step 3: Record red/green and local gate evidence**

### Task 4: Verify, Commit, Push

- [x] **Step 1: Run compatibility tests**

  ```sh
  mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest*' --tests '*LocalSessionApiServerTest.prepared runtime asks driver mod provider for requested runtime lane*'
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.server start forwards configured fabric driver mod environment*' --tests '*CraftlessCliTest.server start uses packaged fabric driver mod when env is absent*'
  ```

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 3: Commit and push**

  ```sh
  git add AGENTS.md daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/ConfiguredClientRuntimeDriverModProviderTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-108-driver-mod-manifest-provider-design.md docs/superpowers/plans/2026-06-28-108-driver-mod-manifest-provider-plan.md docs/superpowers/evidence/2026-06-28-driver-mod-manifest-provider.md
  git commit -m "feat: support driver mod manifest selection"
  git push origin main
  ```

## Self-Review

- Spec coverage: manifest key, exact matching, fallback behavior, relative
  paths, governance, and verification are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no new compiled lane, gameplay action, public route, CLI gameplay
  catalog, scenario shortcut, or support claim.
