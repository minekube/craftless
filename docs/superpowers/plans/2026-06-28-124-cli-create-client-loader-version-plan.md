# CLI Create Client Loader Version Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let `craftless clients create` send the optional create-client loader-version lane.

**Architecture:** Extend CLI argument parsing and usage text for the stable client-create command. The CLI continues to post the supervisor `CreateClientRequest`; generated gameplay commands remain runtime-discovered.

**Tech Stack:** Kotlin/JVM CLI module, Ktor test server, Gradle through mise.

---

### Task 1: Add Red CLI Guard

**Files:**
- Modify: `cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt`

- [x] **Step 1: Add create request body test**

  Add a recording create API server and a test named
  `clients create sends requested loader version lane`.

  The test should run:

  ```sh
  craftless clients create alice --version 1.21.6 --loader FABRIC --loader-version 0.16.14 --offline-name Alice --api <url>
  ```

  It should assert the posted JSON contains `loaderVersion = 0.16.14`.

- [x] **Step 2: Add usage assertion**

  Assert the missing-argument usage for `clients create` mentions
  `[--loader-version <version>]`.

- [x] **Step 3: Run red test**

  ```sh
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*loader version*'
  ```

  Expected: fail before implementation because the CLI does not send
  `loaderVersion`.

### Task 2: Implement CLI Argument

**Files:**
- Modify: `cli/src/main/kotlin/com/minekube/craftless/cli/Main.kt`

- [x] **Step 1: Parse `--loader-version`**
- [x] **Step 2: Pass it to `CreateClientRequest`**
- [x] **Step 3: Update usage string**

### Task 3: Governance, Evidence, Verification

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-cli-create-client-loader-version.md`

- [x] **Step 1: Record Phase 124 governance/checklist**
- [x] **Step 2: Record red/green/local evidence**
- [x] **Step 3: Run local gates**

  ```sh
  mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*loader version*'
  git diff --check
  mise run ci
  ```

- [x] **Step 4: Commit and push**

  ```sh
  git add AGENTS.md cli/src/main/kotlin/com/minekube/craftless/cli/Main.kt cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-124-cli-create-client-loader-version-design.md docs/superpowers/plans/2026-06-28-124-cli-create-client-loader-version-plan.md docs/superpowers/evidence/2026-06-28-cli-create-client-loader-version.md
  git commit -m "feat: add loader version to client create cli"
  git push origin main
  ```

## Self-Review

- Spec coverage: CLI flag, request body, usage, governance, evidence, and
  local gates are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no gameplay action, route family, CLI gameplay catalog, Fabric
  binding, scenario shortcut, compiled lane, public version-specific API, or
  support claim.
