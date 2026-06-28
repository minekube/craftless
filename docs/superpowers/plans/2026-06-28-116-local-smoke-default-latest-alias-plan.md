# Local Smoke Default Latest Alias Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make local server smoke default to `latest-release` while preserving explicit version overrides.

**Architecture:** Introduce a single local smoke default constant and use it for both the data-class default and environment parsing fallback.

**Tech Stack:** Kotlin/JVM testkit tests, Gradle through mise.

---

### Task 1: Add Red Default-Alias Guard

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmokeTest.kt`

- [x] **Step 1: Update default assertions**

  Change `local server smoke is disabled by default` to expect
  `latest-release`.

- [x] **Step 2: Add constructor default guard**

  Add a focused assertion that `LocalMinecraftServerSmokeConfig(root = tempDir,
  enabled = false).minecraftVersion == "latest-release"`.

- [x] **Step 3: Run red test**

  ```sh
  mise exec -- gradle :testkit:test --tests '*LocalMinecraftServerSmokeTest.local server smoke is disabled by default*'
  ```

  Expected: fails before implementation because the default remains `1.21.6`.

### Task 2: Implement Single Default Constant

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmoke.kt`

- [x] **Step 1: Add default constant**

  Add one constant for `latest-release`.

- [x] **Step 2: Use the constant**

  Use it in the `LocalMinecraftServerSmokeConfig` data-class default and in
  `fromEnvironment` fallback. Leave explicit environment values untouched.

- [x] **Step 3: Run focused green test**

  ```sh
  mise exec -- gradle :testkit:test --tests '*LocalMinecraftServerSmokeTest.*'
  ```

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-local-smoke-default-latest-alias.md`

- [x] **Step 1: Add Phase 116 to AGENTS**
- [x] **Step 2: Add Phase 116 checklist section**
- [x] **Step 3: Record red/green and local gate evidence**

### Task 4: Verify, Commit, Push

- [x] **Step 1: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 2: Commit and push**

  ```sh
  git add AGENTS.md testkit/src/main/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmoke.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmokeTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-116-local-smoke-default-latest-alias-design.md docs/superpowers/plans/2026-06-28-116-local-smoke-default-latest-alias-plan.md docs/superpowers/evidence/2026-06-28-local-smoke-default-latest-alias.md
  git commit -m "fix: default local smoke to latest release alias"
  git push origin main
  ```

## Self-Review

- Spec coverage: default alias, explicit override preservation, governance,
  and verification are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no new compiled lane, gameplay action, public route, CLI gameplay
  catalog, scenario shortcut, or support claim.
