# Catalog-Driven Driver Artifact Staging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make CLI distribution artifact staging read Fabric driver artifact paths from the generated lane catalog.

**Architecture:** `driver-fabric` adds an internal `artifactKey` to each catalog entry. `cli` uses Groovy's `JsonSlurper` in Gradle to parse the generated catalog at task execution time, maps `artifactKey=fabric-current-remap-jar` to the current `remapJar` output, and stages each artifact into `build/generated/driver-lane-artifacts/<distributionPath>` before adding that staging directory to the distribution.

**Tech Stack:** Gradle Kotlin DSL, Groovy `JsonSlurper`, Kotlin/JVM tests, Bun distribution tests through mise.

---

### Task 1: Governance

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-129-catalog-driven-driver-artifact-staging-design.md`
- Create: `docs/superpowers/plans/2026-06-28-129-catalog-driven-driver-artifact-staging-plan.md`

- [x] **Step 1: Add Phase 129 to AGENTS.md**

  Add `129. catalog-driven driver artifact staging.` and define it as internal
  distribution plumbing only.

- [x] **Step 2: Add Phase 129 to checklist**

  Track it as support-enabling work that does not satisfy latest/older support
  by itself.

### Task 2: Add Red Build Guards

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`
- Modify: `playwright/src/distribution.test.ts`

- [x] **Step 1: Require artifact key in Fabric catalog**

  Extend `fabric build generates driver lane catalog for distribution packaging`
  to assert `driver-fabric/build.gradle.kts` contains:

  ```kotlin
  "artifactKey"
  "fabric-current-remap-jar"
  ```

- [x] **Step 2: Require catalog-driven staging in CLI build**

  Extend the Bun distribution test to assert `cli/build.gradle.kts` contains:

  ```typescript
  "JsonSlurper"
  "stageFabricDriverLaneArtifacts"
  "fabric-current-remap-jar"
  "driver-lane-artifacts"
  "distributionPath"
  ```

  and no longer contains:

  ```typescript
  'into("mods")'
  ```

- [x] **Step 3: Run red tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric build generates driver lane catalog for distribution packaging*'
  mise exec -- bun test playwright/src/distribution.test.ts
  ```

  Expected: fail before implementation because artifact staging is still
  hard-coded.

### Task 3: Add Catalog Artifact Key

**Files:**
- Modify: `driver-fabric/build.gradle.kts`

- [x] **Step 1: Add artifact key constant**

  Add:

  ```kotlin
  val fabricCompiledArtifactKey = "fabric-current-remap-jar"
  ```

- [x] **Step 2: Emit artifact key**

  Add `"artifactKey": ${jsonString(fabricCompiledArtifactKey)}` to the
  generated lane catalog entry.

### Task 4: Stage CLI Driver Artifacts From Catalog

**Files:**
- Modify: `cli/build.gradle.kts`

- [x] **Step 1: Import JSON parser**

  Add:

  ```kotlin
  import groovy.json.JsonSlurper
  import java.nio.file.Path
  ```

- [x] **Step 2: Create staging task**

  Add `stageFabricDriverLaneArtifacts` that depends on
  `writeFabricDriverLaneCatalog` and `remapJar`, reads catalog `entries`, maps
  `artifactKey == "fabric-current-remap-jar"` to the `remapJar` output file,
  validates the distribution path, and copies it to
  `build/generated/driver-lane-artifacts/<distributionPath>`.

- [x] **Step 3: Use staging directory in distribution**

  Replace the hard-coded `into("mods")` copy with:

  ```kotlin
  from(stageFabricDriverLaneArtifacts)
  ```

### Task 5: Evidence, Verification, Commit

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-catalog-driven-driver-artifact-staging.md`

- [x] **Step 1: Run focused tests and tasks**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric build generates driver lane catalog for distribution packaging*'
  mise exec -- bun test playwright/src/distribution.test.ts
  mise exec -- gradle :cli:stageFabricDriverLaneArtifacts :cli:writeDriverModManifest
  ```

- [x] **Step 2: Inspect staged artifact**

  ```sh
  test -f cli/build/generated/driver-lane-artifacts/mods/craftless-driver-fabric.jar
  jar tf cli/build/generated/driver-lane-artifacts/mods/craftless-driver-fabric.jar | grep -q '^fabric.mod.json$'
  ```

- [x] **Step 3: Run local gates**

  ```sh
  mise run package-cli
  git diff --check
  mise run ci
  ```

- [x] **Step 4: Commit and push**

  ```sh
  git add AGENTS.md cli/build.gradle.kts driver-fabric/build.gradle.kts driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt playwright/src/distribution.test.ts docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-129-catalog-driven-driver-artifact-staging-design.md docs/superpowers/plans/2026-06-28-129-catalog-driven-driver-artifact-staging-plan.md docs/superpowers/evidence/2026-06-28-catalog-driven-driver-artifact-staging.md
  git commit -m "build: stage fabric driver artifacts from catalog"
  git push origin main
  ```

## Self-Review

- Spec coverage: artifact key, JSON parser, catalog-driven staging,
  unchanged distribution shape, focused verification, and full local gates are
  covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no gameplay action, public route, compiled lane, Fabric dependency
  change, or support claim.
