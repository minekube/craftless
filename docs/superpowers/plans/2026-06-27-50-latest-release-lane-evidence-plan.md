# Latest Release Lane Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the real latest-release `26.2` unsupported compatibility lane away from simulated wording while preserving the unsupported Fabric client boundary.

**Architecture:** Keep the compiled Fabric client lane at `1.21.6`. Update the internal compatibility matrix and Gradle-generated smoke lane JSON so `26.2` is represented as a real latest-release runtime input with `UNSUPPORTED/runtime-lane-missing`, not as a simulated lane. Tests guard the sanitized public evidence.

**Tech Stack:** Kotlin/JVM, Fabric Loom Gradle Kotlin DSL, Gradle tests through mise, Markdown docs.

---

### Task 1: Add Failing Lane Naming Guards

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrixTest.kt`
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbeTest.kt`
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmokeTest.kt`

- [x] **Step 1: Update matrix expectations**

  Change the `26.2` matrix test to expect lane id `latest-release-26-2` and
  provider id `no-compatible-client-lane`.

- [x] **Step 2: Update runtime probe expectations**

  Change runtime metadata evidence expectations to require
  `latest-release-26-2` and `no-compatible-client-lane`, and reject
  `simulated`.

- [x] **Step 3: Update smoke fixture expectations**

  Change the runtime-lane JSON test fixture to use the latest-release lane ids.

- [x] **Step 4: Verify RED**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricCompatibilityMatrixTest*' --tests '*FabricCapabilityProbeTest.runtime metadata probe emits sanitized compatibility lane evidence*' :testkit:test --tests '*LocalMinecraftServerSmokeTest.local server smoke records unsupported runtime lane without provisioning server*'
  ```

  Expected: fails because production code still emits simulated lane ids.

### Task 2: Rename Compatibility Lane Evidence

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrix.kt`
- Modify: `driver-fabric/build.gradle.kts`

- [x] **Step 1: Rename Kotlin matrix lane**

  Change the `26.2` lane id from `fabric-simulated-26` to
  `latest-release-26-2`, and provider id from `fabric-simulated-provider` to
  `no-compatible-client-lane`.

- [x] **Step 2: Rename Gradle smoke lane JSON**

  Apply the same ids in `fabricSmokeRuntimeLaneJson("26.2")`.

- [x] **Step 3: Verify GREEN**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricCompatibilityMatrixTest*' --tests '*FabricCapabilityProbeTest.runtime metadata probe emits sanitized compatibility lane evidence*' :testkit:test --tests '*LocalMinecraftServerSmokeTest.local server smoke records unsupported runtime lane without provisioning server*'
  ```

### Task 3: Update Phase Guidance And Checklist

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/specs/2026-06-26-26-version-agnostic-driver-architecture-design.md`
- Modify: `docs/superpowers/plans/2026-06-26-26-version-agnostic-driver-architecture-plan.md`
- Modify: `docs/superpowers/specs/2026-06-27-46-compiled-fabric-lane-metadata-design.md`

- [x] **Step 1: Register Phase 50**

  Add `50. latest release lane evidence.` to `AGENTS.md` and document that
  latest-release lanes must not be described as simulated when they come from
  real Mojang metadata.

- [x] **Step 2: Update compatibility wording**

  Replace docs/checklist references to simulated `26.2` lane evidence with real
  latest-release unsupported lane evidence.

- [x] **Step 3: Verify quality gates**

  Run:

  ```sh
  git diff --check
  mise run architecture-check
  mise run ci
  ```

### Task 4: Commit, Push, And Monitor

**Files:**
- Commit all phase 50 files and changes.

- [ ] **Step 1: Commit and push**

  Run:

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-27-50-latest-release-lane-evidence-design.md docs/superpowers/plans/2026-06-27-50-latest-release-lane-evidence-plan.md docs/superpowers/specs/2026-06-26-26-version-agnostic-driver-architecture-design.md docs/superpowers/plans/2026-06-26-26-version-agnostic-driver-architecture-plan.md docs/superpowers/specs/2026-06-27-46-compiled-fabric-lane-metadata-design.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrix.kt driver-fabric/build.gradle.kts driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrixTest.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbeTest.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmokeTest.kt
  git commit -m "driver-fabric: clarify latest release lane evidence"
  git push origin main
  ```

- [ ] **Step 2: Verify remote CI**

  Run:

  ```sh
  gh run list --repo minekube/craftless --branch main --limit 3
  gh run watch <latest-run-id> --repo minekube/craftless --exit-status
  ```
