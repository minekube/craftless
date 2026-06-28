# Explicit Unused And Dead-Code Gates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make unused/dead-code checks explicit in Detekt config, mise tasks, and CI.

**Architecture:** Use the existing pinned Detekt plugin and build-wide Gradle wiring. Add explicit rule config, a discoverable mise task, a CI step, and a policy test; do not add another analyzer dependency.

**Tech Stack:** Detekt 2.0.0-alpha.5, Gradle, mise.

---

### Task 1: Add Policy Guard

**Files:**
- Modify: `protocol/src/test/kotlin/com/minekube/craftless/protocol/NamespacePolicyTest.kt`

- [x] **Step 1: Add failing policy test**

  Add `kotlin quality gates include explicit unused and dead code checks`.

- [x] **Step 2: Run red test**

  ```sh
  mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.kotlin quality gates include explicit unused and dead code checks*'
  ```

  Expected initial result: FAIL because `config/detekt/detekt.yml` and
  `.mise.toml` did not explicitly name the unused/dead-code gate.

### Task 2: Configure Detekt And mise

**Files:**
- Modify: `config/detekt/detekt.yml`
- Modify: `.mise.toml`

- [x] **Step 1: Add explicit Detekt rules**

  Enable `UnusedImport`, `UnusedParameter`, `UnusedPrivateClass`,
  `UnusedPrivateFunction`, `UnusedPrivateProperty`, `UnusedVariable`,
  `UnreachableCatchBlock`, `UnreachableCode`, and `UnusedUnaryOperator`.

- [x] **Step 2: Add mise task and CI step**

  Add `[tasks.unused-check]` that runs `mise exec -- gradle detekt`, and add
  `mise run unused-check` to `[tasks.ci]`.

- [x] **Step 3: Run focused checks**

  ```sh
  mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.kotlin quality gates include explicit unused and dead code checks*'
  mise run unused-check
  ```

### Task 3: Verify And Commit

- [x] **Step 1: Run local gates**

  ```sh
  git diff --check
  mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.kotlin quality gates include explicit unused and dead code checks*'
  mise run unused-check
  mise run ci
  ```

- [ ] **Step 2: Commit and push**

  ```sh
  git add .mise.toml config/detekt/detekt.yml protocol/src/test/kotlin/com/minekube/craftless/protocol/NamespacePolicyTest.kt docs/superpowers/specs/2026-06-28-106-explicit-unused-dead-code-gates-design.md docs/superpowers/plans/2026-06-28-106-explicit-unused-dead-code-gates-plan.md docs/superpowers/evidence/2026-06-28-explicit-unused-dead-code-gates.md
  git commit -m "build: make unused checks explicit"
  git push origin main
  ```

## Self-Review

- Scope: build quality gates only.
- Dependency scan: no new dependency is introduced.
- Product scan: no gameplay, version, packaging, or release behavior changes.
