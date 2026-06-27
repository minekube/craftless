# Final Gameplay Codex Evidence Default Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make final gameplay default to Codex-verifiable public API/CLI evidence instead of human chat confirmation.

**Architecture:** Keep the Fabric smoke controller's explicit confirmation path intact for diagnostic runs. Change only the default final gameplay plan and Gradle task wiring so no confirmation phrase, reminder loop, or macOS voice prompt is injected unless the operator explicitly provides environment variables.

**Tech Stack:** Kotlin/JVM, Fabric Loom Gradle Kotlin DSL, Gradle tests through mise, Markdown docs.

---

### Task 1: Add Failing Default-Gate Tests

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Update plan test**

  Change the final gameplay plan test to require ready evidence, optional
  co-play wording, Codex evidence completion gates, and no Robin or Minecraft
  chat completion gate.

- [x] **Step 2: Add Gradle default test**

  Add a test proving `driver-fabric/build.gradle.kts` still references the
  opt-in confirmation env var but does not default it to
  `goal may be completed`, does not ask for Minecraft chat confirmation, and
  does not inject a default Robin `say` prompt.

- [x] **Step 3: Verify RED**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric final gameplay plan gates completion on graph streams and Codex evidence*' --tests '*FabricDriverModuleTest.fabric final gameplay defaults to Codex evidence gate without chat confirmation phrase*'
  ```

  Expected: fails because the old plan and Gradle defaults still require chat
  confirmation.

### Task 2: Change Final Gameplay Defaults

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricFinalGameplayPlan.kt`
- Modify: `driver-fabric/build.gradle.kts`

- [x] **Step 1: Update the plan**

  Replace Robin-confirmation plan steps with ready-evidence and optional
  co-play hold steps. Replace the completion gate with Codex evidence wording.

- [x] **Step 2: Update Gradle task defaults**

  Change `fabricFinalGameplay` so it only sets
  `CRAFTLESS_FABRIC_SMOKE_CONFIRM_CHAT_CONTAINS`,
  `CRAFTLESS_FABRIC_SMOKE_READY_REMINDER_MS`, and
  `CRAFTLESS_FABRIC_SMOKE_READY_COMMAND_JSON` when the operator supplied those
  values.

- [x] **Step 3: Preserve controller defaults**

  Assert `FabricClientSmokeController.fromEnvironment` defaults
  `confirmationChatContains` to `null` and `readyNotificationReminder` to
  `0.milliseconds`.

- [x] **Step 4: Verify GREEN**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric final gameplay plan gates completion on graph streams and Codex evidence*' --tests '*FabricDriverModuleTest.fabric final gameplay defaults to Codex evidence gate without chat confirmation phrase*' --tests '*FabricDriverModuleTest.fabric smoke controller can hold the final gameplay session open*'
  ```

  Expected: pass.

### Task 3: Update Governance And Checklist

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-28-67-final-gameplay-codex-evidence-default-design.md`
- Create: `docs/superpowers/plans/2026-06-28-67-final-gameplay-codex-evidence-default-plan.md`

- [x] **Step 1: Register Phase 67**

  Add Phase 67 to `AGENTS.md` and state that final gameplay defaults must not
  inject human confirmation requirements.

- [x] **Step 2: Update checklist**

  Add a Phase 67 section with the focused verification and keep final
  completion open until the full Codex evidence gate is refreshed.

### Task 4: Verify, Commit, Push, And Monitor

**Files:**
- Commit all Phase 67 files and code changes.

- [x] **Step 1: Run verification**

  Run:

  ```sh
  git diff --check
  mise run architecture-check
  mise run ci
  ```

- [ ] **Step 2: Commit and push**

  Run:

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-67-final-gameplay-codex-evidence-default-design.md docs/superpowers/plans/2026-06-28-67-final-gameplay-codex-evidence-default-plan.md driver-fabric/build.gradle.kts driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricFinalGameplayPlan.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
  git commit -m "driver-fabric: default final gameplay to codex evidence"
  git push origin main
  ```

- [ ] **Step 3: Verify remote CI**

  Run:

  ```sh
  gh run list --repo minekube/craftless --branch main --limit 5 --json databaseId,headSha,status,conclusion,name,event,createdAt
  gh run watch <latest-run-id> --repo minekube/craftless --exit-status
  ```
