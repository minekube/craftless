# Final Gameplay Child Environment Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep final gameplay helper subprocesses from inheriting local-server
owner environment that can start a second server or clear shared evidence.

**Architecture:** Add a small internal sanitizer for Fabric smoke child process
environments. Use it in public-agent and ready-notification subprocess builders
before writing explicit child-specific variables. Keep the visible Fabric client
action command unchanged because it intentionally needs the smoke controller
environment.

**Tech Stack:** Kotlin/JVM, Gradle Kotlin DSL, Fabric smoke controller tests,
mise.

---

### Task 1: RED Test For Child Environment Isolation

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add failing sanitizer test**

  Add a focused test proving inherited owner variables are removed while normal
  environment such as `PATH` remains.

- [x] **Step 2: Verify RED**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric smoke child commands do not inherit server owner environment*'
  ```

  Expected: FAIL because `removeInheritedSmokeOwnerEnvironment` does not exist.

### Task 2: Implement Sanitizer At Child Process Boundaries

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricClientSmokeController.kt`

- [x] **Step 1: Add internal sanitizer**

  Add an internal `MutableMap<String, String>.removeInheritedSmokeOwnerEnvironment()`
  helper and remove local-server/final-gameplay owner variables.

- [x] **Step 2: Apply before public-agent child env**

  In `runPublicAgentCommand`, call the sanitizer before adding
  `CRAFTLESS_PUBLIC_AGENT_*`.

- [x] **Step 3: Apply before ready-notification child env**

  In `runReadyNotificationCommand`, call the sanitizer before adding
  `CRAFTLESS_FABRIC_SMOKE_READY_*`.

- [x] **Step 4: Verify GREEN**

  Run focused sanitizer and subprocess tests.

### Task 3: Docs, Checklist, And Final Verification

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-27-57-final-gameplay-child-environment-isolation-design.md`
- Create: `docs/superpowers/plans/2026-06-27-57-final-gameplay-child-environment-isolation-plan.md`

- [ ] **Step 1: Register Phase 57**

  Add Phase 57 guardrails to `AGENTS.md` and checklist. Correct the latest
  Phase 56 evidence wording so it does not claim the failed rerun exited
  successfully.

- [ ] **Step 2: Run verification**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric smoke child commands do not inherit server owner environment*' --tests '*FabricDriverModuleTest.fabric smoke controller runs process external public agent command with live daemon url*' --tests '*FabricDriverModuleTest.fabric smoke controller runs ready notification command with live session context*' --tests '*FabricDriverModuleTest.fabric smoke controller writes confirmation timeout artifact when Robin chat is not observed*'
  mise run lint
  mise run architecture-check
  mise run ci
  ```

- [ ] **Step 3: Rerun held final gameplay**

  Run:

  ```sh
  CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_FABRIC_SMOKE_ACTION_TIMEOUT_MS=120000 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=1800000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
  ```

  Expected: the task does not fail because server evidence was cleared by a
  child process. If Robin does not confirm, it exits with
  `final-gameplay-confirmation-timeout.json`; if Robin confirms, it exits with
  `final-gameplay-confirmation.json`.

- [ ] **Step 4: Commit and push**

  Commit all Phase 57 changes and push directly to `main`.

## Self-Review

- Spec coverage: covers the observed failure, root cause, subprocess boundaries,
  tests, docs, final rerun, and no-public-gameplay-breadth rule.
- Placeholder scan: no TBD/TODO/fill-in placeholders.
- Type consistency: environment variable names match implementation.
