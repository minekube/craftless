# Generated Driver Lane Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move CLI driver-mod manifest generation to a generated Fabric driver lane catalog.

**Architecture:** `driver-fabric` owns build metadata for Fabric driver lanes and writes an internal JSON catalog. `cli` depends on that task, reads the catalog, and writes the installed-distribution `driver-mods.json` from catalog entries. The current distribution still ships one remapped driver jar at `mods/craftless-driver-fabric.jar`.

**Tech Stack:** Gradle Kotlin DSL, Kotlin/JVM tests, Bun distribution tests through mise.

---

### Task 1: Governance

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-128-generated-driver-lane-catalog-design.md`
- Create: `docs/superpowers/plans/2026-06-28-128-generated-driver-lane-catalog-plan.md`

- [x] **Step 1: Add Phase 128 to AGENTS.md**

  Add `128. generated driver lane catalog.` and define it as internal
  build/package metadata only.

- [x] **Step 2: Add Phase 128 to checklist**

  Track it in the final gate paragraph as support-enabling work that does not
  complete latest/older support by itself.

### Task 2: Add Red Build-Ownership Tests

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`
- Modify: `playwright/src/distribution.test.ts`

- [x] **Step 1: Add Fabric build script guard**

  Add a test named `fabric build generates driver lane catalog for distribution packaging` that reads `driver-fabric/build.gradle.kts` and asserts it contains:

  ```kotlin
  "writeFabricDriverLaneCatalog"
  "fabric-driver-lanes.json"
  "distributionPath"
  ```

- [x] **Step 2: Add distribution build guard**

  Update the Bun distribution test to assert `cli/build.gradle.kts` contains:

  ```typescript
  "fabric-driver-lanes.json"
  "writeFabricDriverLaneCatalog"
  ```

  and does not contain:

  ```typescript
  'extensions.extraProperties["fabricCompiledMinecraftVersion"]'
  'extensions.extraProperties["fabricCompiledLoaderVersion"]'
  ```

- [x] **Step 3: Run red tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric build generates driver lane catalog for distribution packaging*'
  mise exec -- bun test playwright/src/distribution.test.ts
  ```

  Expected: fail before implementation because packaging still uses direct
  single-lane extra properties.

### Task 3: Generate Driver Lane Catalog

**Files:**
- Modify: `driver-fabric/build.gradle.kts`

- [x] **Step 1: Add generated catalog output**

  Add:

  ```kotlin
  val generatedFabricDriverLaneCatalog = layout.buildDirectory.file("generated/driver-lanes/fabric-driver-lanes.json")
  ```

- [x] **Step 2: Add `writeFabricDriverLaneCatalog` task**

  Write a JSON document with:

  ```json
  {
    "entries": [
      {
        "loader": "FABRIC",
        "minecraftVersion": "<fabricCompiledMinecraftVersion>",
        "loaderVersion": "<fabricCompiledLoaderVersion>",
        "path": "mods/craftless-driver-fabric.jar",
        "providerId": "<fabricCompiledProviderId>",
        "fabricApiVersion": "<fabricCompiledApiVersion>",
        "javaMajorVersion": <fabricCompiledJavaMajorVersion>,
        "distributionPath": "mods/craftless-driver-fabric.jar"
      }
    ]
  }
  ```

  Keep `path` so the catalog can be copied directly into the provider manifest
  shape.

### Task 4: Consume Catalog In CLI Packaging

**Files:**
- Modify: `cli/build.gradle.kts`

- [x] **Step 1: Wire task dependency**

  Resolve `fabricDriverProject.tasks.named("writeFabricDriverLaneCatalog")`
  and the output file at
  `driver-fabric/build/generated/driver-lanes/fabric-driver-lanes.json`.

- [x] **Step 2: Render `driver-mods.json` from catalog**

  Change `writeDriverModManifest` so it reads the generated catalog and writes
  it to `build/generated/driver-mods/driver-mods.json`. Keep the output file
  name and distribution location unchanged.

- [x] **Step 3: Keep jar packaging unchanged**

  Continue packaging `remapJar` as `mods/craftless-driver-fabric.jar` so
  existing installed fallback behavior and distribution smoke checks remain
  valid.

### Task 5: Evidence, Verification, Commit

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-generated-driver-lane-catalog.md`

- [x] **Step 1: Run focused tests and packaging task**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric build generates driver lane catalog for distribution packaging*'
  mise exec -- bun test playwright/src/distribution.test.ts
  mise exec -- gradle :cli:writeDriverModManifest
  ```

- [x] **Step 2: Inspect generated files**

  ```sh
  cat driver-fabric/build/generated/driver-lanes/fabric-driver-lanes.json
  cat cli/build/generated/driver-mods/driver-mods.json
  ```

- [x] **Step 3: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 4: Commit and push**

  ```sh
  git add AGENTS.md driver-fabric/build.gradle.kts cli/build.gradle.kts driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt playwright/src/distribution.test.ts docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-128-generated-driver-lane-catalog-design.md docs/superpowers/plans/2026-06-28-128-generated-driver-lane-catalog-plan.md docs/superpowers/evidence/2026-06-28-generated-driver-lane-catalog.md
  git commit -m "build: generate fabric driver lane catalog"
  git push origin main
  ```

## Self-Review

- Spec coverage: build ownership, catalog generation, CLI consumption,
  unchanged distribution path, tests, evidence, and local gates are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no gameplay action, public route, compiled lane, Fabric dependency
  change, or support claim.
