# Parameterized Fabric Compiled Lane Build Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Parameterize the single compiled Fabric driver lane build so real
version compatibility probes can compile against selected lane metadata without
editing source constants.

**Architecture:** Keep one packaged default lane. Use Gradle properties as lane
inputs for dependencies, generated metadata, resource expansion, and private
catalog generation. Add a mise task for a representative older-lane compile
probe.

**Tech Stack:** Gradle Kotlin DSL, Fabric Loom, mise tasks, Kotlin/JVM tests.

---

### Task 1: Governance

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-134-parameterized-fabric-compiled-lane-build-design.md`
- Create: `docs/superpowers/plans/2026-06-28-134-parameterized-fabric-compiled-lane-build-plan.md`

- [x] **Step 1: Add Phase 134 to AGENTS.md**

  Define it as build/probe infrastructure only.

- [x] **Step 2: Add Phase 134 to checklist**

  Track it as support-enabling work that does not satisfy latest/older support
  by itself.

### Task 2: Add Red Tests

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add property-input source test**

  Add a test named
  `fabric compiled lane build is parameterized for compatibility probes` that
  reads `driver-fabric/build.gradle.kts` and asserts the script contains every
  `craftless.fabric.*` property name from the spec.

- [x] **Step 2: Run the test red**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric compiled lane build is parameterized for compatibility probes*'
  ```

  Expected before implementation: fail because those property names are absent.

### Task 3: Implement Property-Backed Lane Inputs

**Files:**
- Modify: `driver-fabric/build.gradle.kts`

- [x] **Step 1: Add property helpers**

  Add `fabricLaneProperty(name, default)` and
  `fabricLaneIntProperty(name, default)` helpers using
  `providers.gradleProperty(name).orElse(default).get()`.

- [x] **Step 2: Replace hard-coded lane constants**

  Read all compiled lane constants from the helper methods while preserving
  current defaults.

- [x] **Step 3: Verify generated outputs use selected values**

  Keep dependencies, `processResources`, `generateFabricCompiledLaneMetadata`,
  and `writeFabricDriverLaneCatalog` wired to the property-backed values.

### Task 4: Add Older-Lane Probe Task

**Files:**
- Modify: `.mise.toml`

- [x] **Step 1: Add `fabric-lane-check-older`**

  Add a mise task that runs:

  ```sh
  mise exec -- gradle :driver-fabric:compileKotlin :driver-fabric:processResources :driver-fabric:writeFabricDriverLaneCatalog \
    -Pcraftless.fabric.minecraftVersion=1.20.6 \
    -Pcraftless.fabric.yarnMappings=1.20.6+build.3 \
    -Pcraftless.fabric.loaderVersion=0.19.3 \
    -Pcraftless.fabric.apiVersion=0.100.8+1.20.6 \
    -Pcraftless.fabric.javaMajorVersion=21 \
    -Pcraftless.fabric.laneId=fabric-1-20-6-lane \
    -Pcraftless.fabric.providerId=fabric-1-20-6-lane \
    -Pcraftless.fabric.artifactKey=fabric-1-20-6-remap-jar \
    -Pcraftless.fabric.mappingsFingerprint=craftless-fabric-bindings-1-20-6
  ```

### Task 5: Evidence, Verification, Commit

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-parameterized-fabric-compiled-lane-build.md`

- [x] **Step 1: Run focused tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric compiled lane build is parameterized for compatibility probes*'
  mise run fabric-lane-check-older
  mise run package-cli
  ```

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [ ] **Step 3: Commit and push**

  ```sh
  git add .mise.toml AGENTS.md docs/project-completion-checklist.md driver-fabric/build.gradle.kts driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt docs/superpowers/specs/2026-06-28-134-parameterized-fabric-compiled-lane-build-design.md docs/superpowers/plans/2026-06-28-134-parameterized-fabric-compiled-lane-build-plan.md docs/superpowers/evidence/2026-06-28-parameterized-fabric-compiled-lane-build.md
  git commit -m "build: parameterize fabric driver lane"
  git push origin main
  ```

## Self-Review

- Spec coverage: property-backed lane inputs, current defaults, older probe,
  packaging safety, and non-goals are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no new packaged lane, public gameplay API, route family, CLI gameplay
  catalog, Fabric dependency default change, or support claim.
