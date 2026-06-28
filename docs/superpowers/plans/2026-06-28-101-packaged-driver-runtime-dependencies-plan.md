# Packaged Driver Runtime Dependencies Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the packaged Fabric driver mod carry the runtime jars required by self-attach.

**Architecture:** Use Fabric Loom `include(...)` to nest Craftless runtime modules and self-attach runtime libraries in the remapped Fabric driver jar. Extend `mise run package-cli` so package smoke checks fail when the staged mod lacks nested runtime jars.

**Tech Stack:** Gradle/Fabric Loom, mise tasks, Kotlin repository policy tests.

---

### Task 1: Add Red Packaging Policy

**Files:**
- Modify: `protocol/src/test/kotlin/com/minekube/craftless/protocol/NamespacePolicyTest.kt`

- [x] **Step 1: Add policy test**

  Add `fabric driver mod declares nested runtime dependencies`, asserting
  `driver-fabric/build.gradle.kts` declares `include(...)` entries for
  Craftless runtime modules, Ktor client/server jars, Kotlin stdlib,
  coroutines, serialization, and representative Ktor transitive runtime jars.

- [x] **Step 2: Run red policy test**

  ```sh
  mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.fabric driver mod declares nested runtime dependencies*'
  ```

### Task 2: Add Nested Runtime Includes

**Files:**
- Modify: `driver-fabric/build.gradle.kts`

- [x] **Step 1: Add Fabric Loom includes**

  Add `include(project(...))` for Craftless runtime modules and
  `include("group:name:version")` for Kotlin/Ktor/serialization/coroutines
  runtime jars required by the self-attach endpoint.

- [x] **Step 2: Run green policy test**

  ```sh
  mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.fabric driver mod declares nested runtime dependencies*'
  ```

### Task 3: Strengthen Package Smoke

**Files:**
- Modify: `.mise.toml`

- [x] **Step 1: Add staged-jar checks**

  Extend `package-cli` after the `fabric.mod.json` check with:

  ```sh
  jar tf build/docker/craftless/mods/craftless-driver-fabric.jar | grep -q '^META-INF/jars/.\+\.jar$'
  jar tf build/docker/craftless/mods/craftless-driver-fabric.jar | grep -q '^META-INF/jars/kotlin-stdlib-'
  jar tf build/docker/craftless/mods/craftless-driver-fabric.jar | grep -q '^META-INF/jars/kotlinx-coroutines-core-jvm-'
  jar tf build/docker/craftless/mods/craftless-driver-fabric.jar | grep -q '^META-INF/jars/ktor-http-jvm-'
  ```

- [x] **Step 2: Run package smoke**

  ```sh
  mise run package-cli
  ```

### Task 4: Register Phase 101 And Verify

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-packaged-driver-runtime-dependencies.md`

- [x] **Step 1: Register Phase 101**

  Add Phase 101 as packaged Fabric driver runtime dependency closure. Keep live
  generated API/self-attach gameplay verification open.

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise exec -- gradle :protocol:test :driver-fabric:test
  mise exec -- gradle :protocol:ktlintCheck :protocol:detekt :driver-fabric:ktlintCheck :driver-fabric:detekt
  mise run package-cli
  ```

- [x] **Step 3: Record evidence**

  Write red/green, jar inspection, package smoke, and final gate outcomes to
  `docs/superpowers/evidence/2026-06-28-packaged-driver-runtime-dependencies.md`.

### Task 5: Commit And Push

**Files:**
- All modified files from Tasks 1-4

- [x] **Step 1: Commit and push**

  ```sh
  git add .mise.toml AGENTS.md driver-fabric/build.gradle.kts protocol/src/test/kotlin/com/minekube/craftless/protocol/NamespacePolicyTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-101-packaged-driver-runtime-dependencies-design.md docs/superpowers/plans/2026-06-28-101-packaged-driver-runtime-dependencies-plan.md docs/superpowers/evidence/2026-06-28-packaged-driver-runtime-dependencies.md
  git commit -m "build: nest fabric driver runtime dependencies"
  git push origin main
  ```

## Self-Review

- Spec coverage: runtime nesting policy, package smoke, evidence, gates, and
  push are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no public gameplay action, static descriptor family, CLI gameplay
  catalog, Fabric gameplay binding, scenario shortcut, version support claim,
  or completion claim.
