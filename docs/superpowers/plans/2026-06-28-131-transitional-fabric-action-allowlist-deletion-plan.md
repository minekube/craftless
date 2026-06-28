# Transitional Fabric Action Allowlist Deletion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete the stale static Fabric action allowlist document and make
private binding guards use the bootstrap definition layer directly.

**Architecture:** `fabricBootstrapOperationDefinitions()` remains the private
transitional source for current executable bootstrap operations. Tests compare
private bindings and graph projection against that source. Public action
discovery remains graph-projected.

**Tech Stack:** Kotlin/JVM tests, documentation/checklist updates, mise local
verification.

---

### Task 1: Governance

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-131-transitional-fabric-action-allowlist-deletion-design.md`
- Create: `docs/superpowers/plans/2026-06-28-131-transitional-fabric-action-allowlist-deletion-plan.md`

- [x] **Step 1: Add Phase 131 to AGENTS.md**

  State that this deletes a stale static docs allowlist only and does not
  complete the bootstrap-definition exit.

- [x] **Step 2: Update checklist wording**

  Name transitional bootstrap operation definitions as the remaining blocker,
  not the deleted allowlist file.

### Task 2: Replace The Test Source

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Remove allowlist file reader**

  Delete `transitionalFabricActionAllowlist`.

- [x] **Step 2: Compare bindings to bootstrap definitions**

  In `transitional fabric binding operation ids are represented as runtime
  graph operations`, compare binding ids to
  `fabricBootstrapOperationDefinitions().map { it.id }.sorted()`.

### Task 3: Delete The Stale Static Artifact

**Files:**
- Delete: `docs/architecture/transitional-fabric-action-allowlist.txt`

- [x] **Step 1: Delete the file**

  Remove the static list so it cannot be mistaken for a durable product
  catalog.

- [x] **Step 2: Verify no active code reads it**

  ```sh
  rg "transitional-fabric-action-allowlist|transitionalFabricActionAllowlist"
  ```

### Task 4: Evidence, Verification, Commit

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-transitional-fabric-action-allowlist-deletion.md`

- [x] **Step 1: Run focused tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.transitional fabric binding operation ids are represented as runtime graph operations*'
  ```

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 3: Commit and push**

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt docs/superpowers/specs/2026-06-28-131-transitional-fabric-action-allowlist-deletion-design.md docs/superpowers/plans/2026-06-28-131-transitional-fabric-action-allowlist-deletion-plan.md docs/superpowers/evidence/2026-06-28-transitional-fabric-action-allowlist-deletion.md
  git rm docs/architecture/transitional-fabric-action-allowlist.txt
  git commit -m "docs: remove transitional fabric action allowlist"
  git push origin main
  ```

## Self-Review

- Spec coverage: stale file deletion, replacement source, unchanged runtime
  graph behavior, focused verification, and full local gates are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no runtime operation change, gameplay API, public route, compiled
  lane, Fabric dependency change, or support claim.
