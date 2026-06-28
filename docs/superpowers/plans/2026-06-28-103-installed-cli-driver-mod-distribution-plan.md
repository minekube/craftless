# Installed CLI Driver Mod Distribution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the install-script and reusable GitHub Action CLI distribution carry and auto-use the Fabric driver mod.

**Architecture:** Reuse the remapped `driver-fabric` jar already staged for Docker. Add it to the Gradle CLI distribution under `mods/craftless-driver-fabric.jar`, then let CLI server startup augment the environment with that distribution-local mod path when `CRAFTLESS_FABRIC_DRIVER_MOD` is unset.

**Tech Stack:** Gradle Application plugin, Fabric Loom remapJar output, Kotlin/JVM CLI, mise.

---

### Task 1: Add Packaging Policy

**Files:**
- Modify: `protocol/src/test/kotlin/com/minekube/craftless/protocol/NamespacePolicyTest.kt`

- [x] **Step 1: Strengthen distribution policy**

  Assert `cli/build.gradle.kts` includes `project(":driver-fabric")`, depends
  on `:driver-fabric:remapJar`, and packages
  `mods/craftless-driver-fabric.jar`.

- [x] **Step 2: Run red policy test**

  ```sh
  mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.package cli stages craftless fabric driver mod for docker runtime*'
  ```

### Task 2: Include Driver Mod In CLI Distribution

**Files:**
- Modify: `cli/build.gradle.kts`

- [x] **Step 1: Add distribution copy spec**

  Configure the `application` distribution to copy the remapped Fabric driver
  jar into `mods/craftless-driver-fabric.jar`.

- [x] **Step 2: Run package smoke**

  ```sh
  mise run package-cli
  tar -tf cli/build/distributions/craftless-*.tar | grep 'mods/craftless-driver-fabric.jar'
  unzip -l cli/build/distributions/craftless-*.zip | grep 'mods/craftless-driver-fabric.jar'
  ```

### Task 3: Auto-Discover Installed Driver Mod

**Files:**
- Modify: `cli/src/main/kotlin/com/minekube/craftless/cli/Main.kt`
- Modify: `cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt`

- [x] **Step 1: Add red CLI test**

  Add a test proving `server start` creates a Fabric client using a
  distribution-local `mods/craftless-driver-fabric.jar` when
  `CRAFTLESS_FABRIC_DRIVER_MOD` is not set.

- [x] **Step 2: Implement server-start environment augmentation**

  Use the explicit env var when present. Otherwise resolve the install root
  from the CLI jar location and add `CRAFTLESS_FABRIC_DRIVER_MOD` if
  `mods/craftless-driver-fabric.jar` exists.

- [x] **Step 3: Run focused CLI test**

  ```sh
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.server start uses packaged fabric driver mod when env is absent*'
  ```

### Task 4: Align Docs And Checklist

**Files:**
- Modify: `README.md`
- Modify: `docs/roadmap.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `AGENTS.md`
- Create: `docs/superpowers/evidence/2026-06-28-installed-cli-driver-mod-distribution.md`

- [x] **Step 1: Register Phase 103**

  Add Phase 103 to AGENTS and checklist. State that installed CLI, install
  script, and reusable Action users get the packaged driver mod after this
  release path.

- [x] **Step 2: Update README and roadmap**

  Mention packaged driver mod auto-discovery and keep broader completion
  status incomplete.

### Task 5: Verify, Commit, Push

- [x] **Step 1: Run local gates**

  ```sh
  git diff --check
  mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.package cli stages craftless fabric driver mod for docker runtime*' :cli:test --tests '*CraftlessCliTest.server start uses packaged fabric driver mod when env is absent*'
  mise exec -- gradle :protocol:ktlintCheck :protocol:detekt :cli:ktlintCheck :cli:detekt
  mise run package-cli
  ```

- [ ] **Step 2: Commit and push**

  ```sh
  git add AGENTS.md README.md docs/roadmap.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-103-installed-cli-driver-mod-distribution-design.md docs/superpowers/plans/2026-06-28-103-installed-cli-driver-mod-distribution-plan.md docs/superpowers/evidence/2026-06-28-installed-cli-driver-mod-distribution.md cli/build.gradle.kts cli/src/main/kotlin/com/minekube/craftless/cli/Main.kt cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt protocol/src/test/kotlin/com/minekube/craftless/protocol/NamespacePolicyTest.kt
  git commit -m "dist: include driver mod in cli archive"
  git push origin main
  ```

## Self-Review

- Scope: installed CLI distribution and docs only.
- Static gameplay scan: no new public gameplay actions or scenario shortcuts.
- HTTP stack: unchanged Ktor-only surface.
