# Packaged Driver Mod Manifest Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Package and auto-discover a driver-mod manifest in the CLI distribution so installed users exercise the version-aware provider path.

**Architecture:** Generate `driver-mods.json` during the CLI distribution build from the current Fabric driver lane metadata. At runtime, `craftless server start` adds `CRAFTLESS_DRIVER_MOD_MANIFEST` when the distribution root contains the manifest, falling back to `CRAFTLESS_FABRIC_DRIVER_MOD` only when the manifest is absent.

**Tech Stack:** Kotlin/JVM CLI tests, Gradle application distribution, Bun distribution guard, mise package task.

---

### Task 1: Add Red Tests

**Files:**
- Modify: `cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt`
- Modify: `playwright/src/distribution.test.ts`

- [x] **Step 1: Add CLI manifest preference test**

  Add a test named `server start uses packaged driver mod manifest when env is absent`.
  It should create a fake distribution root with:

  - `driver-mods.json` pointing to `mods/manifest-driver.jar`;
  - `mods/manifest-driver.jar` containing `manifest-driver-mod`;
  - `mods/craftless-driver-fabric.jar` containing `fallback-driver-mod`.

  The test should create a Fabric client through the started API and verify
  the workspace mod cache contains `manifest-driver-mod`, not the fallback.

- [x] **Step 2: Add Bun distribution guard**

  Add assertions that `cli/build.gradle.kts` mentions `driver-mods.json` and
  `.mise.toml` verifies `driver-mods.json` in both tar and zip distributions.

- [x] **Step 3: Run red tests**

  ```sh
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.server start uses packaged driver mod manifest when env is absent*'
  mise exec -- bun test playwright/src/distribution.test.ts
  ```

### Task 2: Implement Packaged Manifest

**Files:**
- Modify: `cli/build.gradle.kts`
- Modify: `cli/src/main/kotlin/com/minekube/craftless/cli/Main.kt`
- Modify: `.mise.toml`

- [x] **Step 1: Generate manifest for distribution**

  Add a Gradle task in `cli/build.gradle.kts` that writes
  `build/generated/driver-mods/driver-mods.json` with the current Fabric
  driver entry and include it at the distribution root.

- [x] **Step 2: Auto-discover manifest first**

  Rename `withPackagedFabricDriverMod` to reflect packaged driver
  configuration and set `CRAFTLESS_DRIVER_MOD_MANIFEST` when
  `driver-mods.json` exists.

- [x] **Step 3: Verify archives contain manifest**

  Update `mise run package-cli` to check tar and zip archive entries for
  `/driver-mods.json`.

- [x] **Step 4: Run green tests**

  ```sh
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.server start uses packaged driver mod manifest when env is absent*' --tests '*CraftlessCliTest.server start uses packaged fabric driver mod when env is absent*'
  mise exec -- bun test playwright/src/distribution.test.ts
  ```

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-packaged-driver-mod-manifest.md`

- [x] **Step 1: Add Phase 109 to AGENTS**
- [x] **Step 2: Add checklist section**
- [x] **Step 3: Record red/green, package, and local-gate evidence**

### Task 4: Verify, Commit, Push

- [x] **Step 1: Run package smoke**

  ```sh
  mise run package-cli
  ```

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 3: Commit and push**

  ```sh
  git add .mise.toml AGENTS.md cli/build.gradle.kts cli/src/main/kotlin/com/minekube/craftless/cli/Main.kt cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt playwright/src/distribution.test.ts docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-109-packaged-driver-mod-manifest-design.md docs/superpowers/plans/2026-06-28-109-packaged-driver-mod-manifest-plan.md docs/superpowers/evidence/2026-06-28-packaged-driver-mod-manifest.md
  git commit -m "dist: package driver mod manifest"
  git push origin main
  ```

## Self-Review

- Spec coverage: packaged manifest, runtime auto-discovery, package checks,
  fallback compatibility, governance, and verification are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no new compiled lane, gameplay action, public route, CLI gameplay
  catalog, scenario shortcut, or support claim.
