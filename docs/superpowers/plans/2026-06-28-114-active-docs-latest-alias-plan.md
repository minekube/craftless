# Active Docs Latest Alias Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make active user-facing docs prefer `latest-release` aliases instead of treating a concrete latest version as the active contract.

**Architecture:** Keep historical evidence untouched. Update README, roadmap, and client file-management docs, and guard the active docs with Bun tests in `playwright/src/distribution.test.ts`.

**Tech Stack:** Markdown docs, Bun tests through mise.

---

### Task 1: Add Red Docs Guard

**Files:**
- Modify: `playwright/src/distribution.test.ts`

- [ ] **Step 1: Add active docs alias test**

  Add a test named
  `active docs prefer latest aliases over concrete latest ids`.

  Assert:

  ```ts
  const readme = read("README.md");
  const roadmap = read("docs/roadmap.md");
  const fileManagement = read("docs/client-file-management.md");

  expect(readme).toContain('"version": "latest-release"');
  expect(readme).toContain("--mc latest-release");
  expect(fileManagement).toContain("latest-release");
  expect(fileManagement).toContain("latest-snapshot");
  expect(roadmap).not.toContain("current latest `26.2`");
  expect(roadmap).toContain("latest-release");
  ```

- [ ] **Step 2: Run red test**

  ```sh
  mise exec -- bun test playwright/src/distribution.test.ts --test-name-pattern "active docs prefer latest aliases over concrete latest ids"
  ```

  Expected: fails before docs are updated.

### Task 2: Update Active Docs

**Files:**
- Modify: `README.md`
- Modify: `docs/client-file-management.md`
- Modify: `docs/roadmap.md`

- [ ] **Step 1: Update README examples**

  Change the create-client example version from `1.21.6` to
  `latest-release`. Change the cache-prepare example from `--mc 1.21.6` to
  `--mc latest-release` and remove the pinned loader-version line from the
  quick example.

- [ ] **Step 2: Update client file-management docs**

  Add a sentence that `<version>` may be a concrete Mojang id,
  `latest-release`, or `latest-snapshot`; aliases resolve to concrete ids
  before cache handles and manifests are written.

- [ ] **Step 3: Update roadmap wording**

  Replace active "latest `26.2`" wording with "latest-release alias" wording,
  while preserving that historical probe evidence recorded concrete ids such
  as `26.2`.

- [ ] **Step 4: Run green docs test**

  ```sh
  mise exec -- bun test playwright/src/distribution.test.ts --test-name-pattern "active docs prefer latest aliases over concrete latest ids"
  ```

### Task 3: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-active-docs-latest-alias.md`

- [ ] **Step 1: Add Phase 114 to AGENTS**
- [ ] **Step 2: Add Phase 114 checklist section**
- [ ] **Step 3: Record red/green and local gate evidence**

### Task 4: Verify, Commit, Push

- [ ] **Step 1: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [ ] **Step 2: Commit and push**

  ```sh
  git add AGENTS.md README.md docs/client-file-management.md docs/roadmap.md playwright/src/distribution.test.ts docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-114-active-docs-latest-alias-design.md docs/superpowers/plans/2026-06-28-114-active-docs-latest-alias-plan.md docs/superpowers/evidence/2026-06-28-active-docs-latest-alias.md
  git commit -m "docs: prefer latest aliases in active docs"
  git push origin main
  ```

## Self-Review

- Spec coverage: README examples, file-management docs, roadmap wording,
  guard test, governance, and verification are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no historical evidence rewrite, gameplay action, public route, CLI
  gameplay catalog, scenario shortcut, or support claim.
