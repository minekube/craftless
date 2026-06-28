# README Public Entrypoint Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn README into a clean public entrypoint that reflects current generated-API, distribution, compatibility, bridge, and completion evidence.

**Architecture:** This is a docs-and-guard phase. The README is rewritten as the primary user-facing product surface, and Bun distribution tests guard the required quickstarts plus the Phase 81 bridge lifecycle-only message.

**Tech Stack:** Markdown, Bun test via mise, repository docs/checklist.

---

### Task 1: Add README Guard

**Files:**
- Modify: `playwright/src/distribution.test.ts`

- [x] **Step 1: Add a README generated API and bridge guard**

  Add a test that reads `README.md` and asserts that it:

  - says gameplay breadth comes from generated per-client OpenAPI;
  - says bridge code is lifecycle/launch evidence only;
  - does not present HeadlessMC/HMC-Specifics commands as product usage.

- [x] **Step 2: Run the red guard**

  Run:

  ```sh
  mise exec -- bun test playwright
  ```

  Expected: fails before the README rewrite if the new lifecycle-only wording
  is missing.

### Task 2: Rewrite README

**Files:**
- Modify: `README.md`

- [x] **Step 1: Replace README structure**

  Rewrite README with these sections:

  - product opening and hero image;
  - status at a glance;
  - quickstart;
  - Docker;
  - GitHub Actions;
  - how Craftless works;
  - use the generated API;
  - cache and runtime preparation;
  - current verification;
  - comparison;
  - roadmap;
  - development;
  - docs.

- [x] **Step 2: Preserve tested quickstart strings**

  Keep the install script URL, `## Quickstart`, `docker run`,
  `minekube/craftless/.github/actions/setup-craftless`, “Minecraft artifacts
  are downloaded into the workspace at runtime”, and “without
  server-provisioned inventory”.

- [x] **Step 3: Keep stale surfaces out**

  Do not include TypeScript SDK, Homebrew, HMC command usage, static gameplay
  catalogs, `task.survival.*`, `CRAFTLESS_SMOKE_PROVISION_ITEM`, or
  server-provisioned final gameplay setup.

### Task 3: Update Governance And Checklist

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-readme-public-entrypoint-overhaul.md`

- [x] **Step 1: Add Phase 82 governance**

  Add Phase 82 to the active sequence and state that README must be a clean
  public entrypoint with generated-API-first status and no bridge gameplay
  product path.

- [x] **Step 2: Add checklist section**

  Add Phase 82 checklist items and verification commands.

- [x] **Step 3: Record evidence**

  Create evidence with red, green, local, push, and remote CI sections.

### Task 4: Verify And Push

**Files:**
- All modified files from previous tasks

- [x] **Step 1: Run local docs and test gates**

  Run:

  ```sh
  git diff --check
  mise exec -- bun test playwright
  mise run ci
  ```

  Expected: all exit `0`.

- [x] **Step 2: Commit and push**

  Run:

  ```sh
  git add README.md AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-82-readme-public-entrypoint-overhaul-design.md docs/superpowers/plans/2026-06-28-82-readme-public-entrypoint-overhaul-plan.md docs/superpowers/evidence/2026-06-28-readme-public-entrypoint-overhaul.md playwright/src/distribution.test.ts
  git commit -m "docs: overhaul readme public entrypoint"
  git push origin main
  ```

- [x] **Step 3: Verify remote CI**

  Run:

  ```sh
  gh run list --branch main --limit 5
  gh run watch <run-id> --exit-status
  ```

  Expected: pushed `main` CI passes.

## Self-Review

- Spec coverage: plan covers guard test, README rewrite, governance,
  checklist, evidence, local gates, push, and remote CI.
- Placeholder scan: no TBD/TODO placeholders.
- Scope: docs-only; no product code or gameplay behavior changes.
