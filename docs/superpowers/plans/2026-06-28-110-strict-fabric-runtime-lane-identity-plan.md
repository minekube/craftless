# Strict Fabric Runtime Lane Identity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent the Fabric compatibility matrix from treating same-game-version but mismatched loader/API/mappings identities as supported.

**Architecture:** Tighten `FabricCompatibilityLane.matches` to match the full compiled runtime identity. Teach `FabricCompatibilityMatrix.resolve` to distinguish unknown Minecraft versions from known game versions with incompatible runtime identity.

**Tech Stack:** Kotlin/JVM, Fabric driver runtime tests, Gradle through mise.

---

### Task 1: Add Red Runtime Identity Tests

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrixTest.kt`

- [x] **Step 1: Add mismatch test**

  Add a test named
  `matrix rejects same game version with mismatched runtime identity`.

  It should copy `currentLaneIdentity()` with:

  ```kotlin
  loaderVersion = "0.0.0-test-loader",
  fabricApiVersion = "0.0.0-test-api",
  mappingsFingerprint = "test-mappings-drift",
  ```

  Then assert the resolved lane has status `UNSUPPORTED`, reason
  `unsupported-runtime-identity`, and provider id `fabric-unsupported`.

- [x] **Step 2: Add provider selection test**

  Add a test named
  `matrix does not select provider for mismatched runtime identity`.

  It should call `selectProvider` with the same mismatched identity and assert
  the result is null.

- [x] **Step 3: Run red tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricCompatibilityMatrixTest.matrix rejects same game version with mismatched runtime identity*' --tests '*FabricCompatibilityMatrixTest.matrix does not select provider for mismatched runtime identity*'
  ```

  Expected: fails before implementation because current matching uses only
  Minecraft game version.

### Task 2: Implement Strict Matching

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrix.kt`

- [x] **Step 1: Require exact runtime identity**

  Change `FabricCompatibilityLane.matches` to require exact equality for:
  `gameVersion`, `loaderVersion`, `fabricApiVersion`, and
  `mappingsFingerprint`.

- [x] **Step 2: Distinguish mismatch reason**

  Update `FabricCompatibilityMatrix.resolve` to return:

  - a matching lane when full identity matches;
  - `unsupported-runtime-identity` when at least one lane has the same
    `gameVersion` but the rest of the identity differs;
  - `unsupported-version` for unknown game versions.

- [x] **Step 3: Run green focused tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricCompatibilityMatrixTest*'
  ```

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-strict-fabric-runtime-lane-identity.md`

- [x] **Step 1: Add Phase 110 to AGENTS**
- [x] **Step 2: Add Phase 110 checklist section**
- [x] **Step 3: Record red/green and local gate evidence**

### Task 4: Verify, Commit, Push

- [x] **Step 1: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 2: Commit and push**

  ```sh
  git add AGENTS.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrix.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/runtime/FabricCompatibilityMatrixTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-110-strict-fabric-runtime-lane-identity-design.md docs/superpowers/plans/2026-06-28-110-strict-fabric-runtime-lane-identity-plan.md docs/superpowers/evidence/2026-06-28-strict-fabric-runtime-lane-identity.md
  git commit -m "fix: require exact fabric runtime lane identity"
  git push origin main
  ```

## Self-Review

- Spec coverage: full identity matching, mismatch reason, provider selection,
  unknown-version preservation, governance, and verification are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no new compiled lane, gameplay action, public route, CLI gameplay
  catalog, scenario shortcut, or support claim.
