# Final Gameplay Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete Craftless only after a real multiplayer gameplay session verifies the product with Robin in Minecraft chat.

**Architecture:** This phase creates an end-to-end harness and manual-assisted runbook. It uses the graph-backed API, SSE event stream, adaptive consumers, real Fabric client, local server, and artifact capture.

**Tech Stack:** Gradle, Fabric client, Ktor daemon, testkit local server, macOS `say`, mise.

---

### Task 1: Gameplay Harness

**Files:**
- Create: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricGameplayCompletionController.kt`
- Test: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricGameplayCompletionControllerTest.kt`

- [ ] **Step 1: Add failing harness tests**

Assert the controller fetches graph-backed OpenAPI, subscribes to SSE, invokes discovered operations, records event transcript, and refuses static fallback bypass.

- [ ] **Step 2: Implement controller**

Build on current smoke controller but require graph-backed metadata and SSE evidence.

- [ ] **Step 3: Verify**

Run: `mise exec -- gradle :driver-fabric:test`

Expected: pass.

### Task 2: Real Session Runbook

**Files:**
- Create: `docs/final-gameplay-runbook.md`
- Modify: `docs/project-completion-checklist.md`

- [ ] **Step 1: Write runbook**

Include exact commands to start the server, launch Craftless client, watch SSE, collect artifacts, use `say` to ask Robin to join, and wait for Robin's Minecraft chat completion message.

- [ ] **Step 2: Verify docs**

Run: `git diff --check`

Expected: pass.

### Task 3: Execute Real Gameplay

**Files:**
- Evidence: `driver-fabric/build/craftless-final-gameplay/artifacts/`

- [ ] **Step 1: Run final gameplay task**

Run: `CRAFTLESS_FINAL_GAMEPLAY=1 mise exec -- gradle :driver-fabric:fabricFinalGameplay`

Expected: client joins server, sends chat, streams events, equips tool, mines/builds, records artifacts.

- [ ] **Step 2: Invite Robin**

Run: `say "Robin, join the Craftless test server now."`

Expected: Robin joins and plays with the Craftless-controlled client.

- [ ] **Step 3: Wait for chat completion signal**

Expected in Minecraft chat: Robin writes that the goal may be completed.

- [ ] **Step 4: Final verification**

Run: `mise run ci`

Expected: pass.

- [ ] **Step 5: Mark goal complete**

Only after Step 3 and Step 4, call `update_goal` with status `complete`.
