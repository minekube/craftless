# Reflective Movement Input Shim Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans
> to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for
> tracking.

**Goal:** Remove the direct `PlayerInput` source dependency from movement
bindings while preserving current movement behavior through a version-tolerant
input shim.

**Architecture:** Keep the public action surface unchanged. Replace the Kotlin
`Input` subclass with a Java shim that uses reflection for version-shaped input
fields and constructors.

**Tech Stack:** Kotlin/JVM, Java, Fabric/Minecraft client input, Gradle/mise.

---

### Task 1: Governance

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-136-reflective-movement-input-shim-design.md`
- Create: `docs/superpowers/plans/2026-06-28-136-reflective-movement-input-shim-plan.md`

- [x] **Step 1: Add Phase 136 governance**

  Define this as compatibility plumbing only, not gameplay API breadth.

### Task 2: Add Red Guard

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add source-level guard**

  Assert `FabricActionBindings.kt` no longer contains `PlayerInput`.

- [x] **Step 2: Run the guard red**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric movement bindings do not compile against player input record*'
  ```

### Task 3: Implement Shim

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt`
- Create: `driver-fabric/src/main/java/com/minekube/craftless/driver/fabric/v1_21_6/CraftlessMovementInput.java`

- [x] **Step 1: Remove direct `PlayerInput` Kotlin usage**

  Pass movement booleans to the shim instead of constructing a typed record.

- [x] **Step 2: Add Java input shim**

  Implement both tick signatures and reflectively set whichever current or
  older input fields exist.

### Task 4: Update Probe And Evidence

**Files:**
- Modify: `.mise.toml`
- Create: `docs/superpowers/evidence/2026-06-28-reflective-movement-input-shim.md`

- [x] **Step 1: Remove `PlayerInput` from expected older-lane blockers**

  Keep the remaining recipe/crafting API family as `RecipeDisplayApi`.

- [x] **Step 2: Run verification**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric movement bindings do not compile against player input record*'
  mise run fabric-lane-check-older
  mise run package-cli
  git diff --check
  mise run ci
  ```

### Task 5: Commit And Push

- [x] **Step 1: Commit and push**

  ```sh
  git add .mise.toml AGENTS.md docs/project-completion-checklist.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt driver-fabric/src/main/java/com/minekube/craftless/driver/fabric/v1_21_6/CraftlessMovementInput.java driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt docs/superpowers/specs/2026-06-28-136-reflective-movement-input-shim-design.md docs/superpowers/plans/2026-06-28-136-reflective-movement-input-shim-plan.md docs/superpowers/evidence/2026-06-28-reflective-movement-input-shim.md
  git commit -m "build: reflect movement input shim"
  git push origin main
  ```

## Self-Review

- Spec coverage: direct `PlayerInput` removal, source guard, older-lane probe,
  and no support claim are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no public gameplay action, route family, static catalog, or support
  claim.
