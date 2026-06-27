# README Current Status Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove stale diagnostic provisioning/status language from README and keep examples aligned with the current compiled Fabric lane.

**Architecture:** Documentation-only product-surface cleanup with a Bun regression guard. No runtime behavior, OpenAPI, CLI command surface, or gameplay action changes.

**Tech Stack:** Markdown, Bun tests through mise.

---

### Task 1: Add Failing README Guard

**Files:**
- Modify: `playwright/src/distribution.test.ts`

- [x] **Step 1: Add stale wording guard**

  Add a Bun test that rejects `--loader-version 0.17.2`, active provisioning
  language, and `CRAFTLESS_SMOKE_PROVISION_ITEM` in README.

- [x] **Step 2: Require honest gameplay wording**

  Assert the README states final evidence runs without server-provisioned
  inventory.

- [x] **Step 3: Verify RED**

  Run:

  ```sh
  mise exec -- bun test playwright/src/distribution.test.ts
  ```

  Expected: fails on the stale README.

### Task 2: Update README

**Files:**
- Modify: `README.md`

- [x] **Step 1: Update loader example**

  Change the cache-prepare Fabric Loader example to the current compiled lane.

- [x] **Step 2: Remove diagnostic provisioning status**

  Replace the old provisioned-item smoke wording with current generated API and
  runtime-lane evidence.

- [x] **Step 3: Add honest final gameplay status**

  Document final gameplay evidence as external public-agent composition through
  generated APIs/SSE without server-provisioned inventory or manual movement.

- [x] **Step 4: Verify GREEN**

  Run:

  ```sh
  mise exec -- bun test playwright/src/distribution.test.ts
  ```

### Task 3: Register Phase And Verify

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Register Phase 49 in `AGENTS.md`**

  Add `49. README current status alignment.` to the active phase list and note
  that README must not present legacy diagnostic provisioning as active product
  evidence.

- [x] **Step 2: Add checklist evidence**

  Add a Phase 49 checklist section with spec path, plan path, behavior, and
  verification commands.

- [x] **Step 3: Run quality gates**

  Run:

  ```sh
  git diff --check
  mise exec -- bun test playwright/src/distribution.test.ts
  mise run architecture-check
  mise run ci
  ```

### Task 4: Commit, Push, And Monitor

**Files:**
- Commit the phase files and implementation.

- [x] **Step 1: Commit and push**

  Run:

  ```sh
  git add AGENTS.md README.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-27-49-readme-current-status-alignment-design.md docs/superpowers/plans/2026-06-27-49-readme-current-status-alignment-plan.md playwright/src/distribution.test.ts
  git commit -m "docs: align readme with current gameplay evidence"
  git push origin main
  ```

- [x] **Step 2: Verify remote CI**

  Run:

  ```sh
  gh run list --repo minekube/craftless --branch main --limit 3
  gh run watch <latest-run-id> --repo minekube/craftless --exit-status
  ```
