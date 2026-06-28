# Removed Survival Namespace Wording Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace stale old-path survival namespace wording in active protocol code/tests with removed-scenario wording.

**Architecture:** Add a source guard in protocol tests, then update validation messages and test fixture wording. Keep `task.survival.*` rejection behavior unchanged.

**Tech Stack:** Kotlin/JVM protocol module, Kotlin test, Gradle through mise.

---

### Task 1: Add Red Protocol Wording Guard

**Files:**
- Modify: `protocol/src/test/kotlin/com/minekube/craftless/protocol/NavigationModelsTest.kt`

- [x] **Step 1: Add source guard**

  Add a test named `navigation protocol calls survival namespace removed not legacy`.
  It should read:

  - `protocol/src/main/kotlin/com/minekube/craftless/protocol/NavigationModels.kt`
  - `protocol/src/test/kotlin/com/minekube/craftless/protocol/NavigationModelsTest.kt`

  It should reject the split-string token `"legacy " + "survival"`.

- [x] **Step 2: Run red guard**

  ```sh
  mise exec -- gradle :protocol:test --tests '*NavigationModelsTest.navigation protocol calls survival namespace removed not legacy*'
  ```

  Expected: fail before implementation because active protocol source/tests
  still use stale old-path survival wording.

### Task 2: Rename Active Protocol Wording

**Files:**
- Modify: `protocol/src/main/kotlin/com/minekube/craftless/protocol/NavigationModels.kt`
- Modify: `protocol/src/test/kotlin/com/minekube/craftless/protocol/NavigationModelsTest.kt`

- [x] **Step 1: Rename validation messages**

  Change both `task.survival.*` rejection messages to:

  - `navigation task id must not use removed survival scenario namespace`
  - `navigation progress event type must not use removed survival scenario namespace`

- [x] **Step 2: Rename test message fixture**

  Change the rejected event fixture message to
  `removed survival task event`.

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-removed-survival-namespace-wording.md`

- [x] **Step 1: Add Phase 122 to AGENTS**
- [x] **Step 2: Add Phase 122 checklist section**
- [x] **Step 3: Record red/green/local gate evidence**

### Task 4: Verify, Commit, Push

- [x] **Step 1: Run focused tests**

  ```sh
  mise exec -- gradle :protocol:test --tests '*NavigationModelsTest.*'
  ```

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 3: Commit and push**

  ```sh
  git add AGENTS.md protocol/src/main/kotlin/com/minekube/craftless/protocol/NavigationModels.kt protocol/src/test/kotlin/com/minekube/craftless/protocol/NavigationModelsTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-122-removed-survival-namespace-wording-design.md docs/superpowers/plans/2026-06-28-122-removed-survival-namespace-wording-plan.md docs/superpowers/evidence/2026-06-28-removed-survival-namespace-wording.md
  git commit -m "test: rename removed survival namespace wording"
  git push origin main
  ```

## Self-Review

- Spec coverage: guard, protocol message rename, fixture rename, governance,
  evidence, local gates, and non-goals are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no behavior change, gameplay action, route family, CLI gameplay
  catalog, Fabric binding, scenario shortcut, compiled lane, public
  version-specific API, or support claim.
