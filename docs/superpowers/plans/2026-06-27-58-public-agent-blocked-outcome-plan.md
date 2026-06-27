# Public Agent Blocked Outcome Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Fabric final gameplay fail fast when the public-agent helper reports a blocked outcome.

**Architecture:** Keep the behavior inside `FabricClientSmokeController`. After the external helper exits with code `0`, scan the public-agent gameplay artifact for a `public-agent-blocked` JSON event, write `public-agent-blocked.json`, and raise an error before ready notification or confirmation hold can start.

**Tech Stack:** Kotlin/JVM, kotlinx.serialization JSON already available as `smokeJson`, Fabric smoke controller tests, Gradle through mise.

---

### Task 1: RED Test For Blocked Public-Agent Outcome

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add failing test**

  Add a test named:

  ```kotlin
  @Test
  fun `fabric smoke controller does not enter ready hold when public agent reports blocked`() {
      val gateway = RecordingFabricClientGateway()
      val backend = smokeBackend(gateway)
      val artifactsDir = Files.createTempDirectory("craftless-fabric-public-agent-blocked")
      val controller =
          FabricClientSmokeController.fromEnvironment(
              mapOf(
                  "CRAFTLESS_FABRIC_CLIENT_SMOKE" to "1",
                  "CRAFTLESS_SMOKE_SERVER_HOST" to "localhost",
                  "CRAFTLESS_SMOKE_SERVER_PORT" to "25567",
                  "CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS" to "1000",
                  "CRAFTLESS_FABRIC_SMOKE_STARTUP_SETTLE_MS" to "0",
                  "CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS" to "1",
                  "CRAFTLESS_SMOKE_ARTIFACTS_DIR" to artifactsDir.toString(),
                  "CRAFTLESS_PUBLIC_AGENT_COMMAND_JSON" to
                      """["/bin/sh","-c","printf '%s\n' '{\"event\":\"public-agent-blocked\",\"clientId\":\"fabric-smoke\",\"blocker\":\"insufficient-public-evidence:navigation.follow.succeeded\"}' >> \"$CRAFTLESS_PUBLIC_AGENT_ARTIFACTS_DIR/public-agent-gameplay-results.jsonl\""]""",
              ),
          )
      enqueueBasicSmokeQueryResults(gateway)

      assertTrue(controller.start(backend, gateway, pollInterval = 1.milliseconds))

      val blockedArtifact = readArtifact(artifactsDir, "public-agent-blocked.json")
      assertTrue(blockedArtifact.contains("\"event\":\"public-agent-blocked\""))
      assertTrue(blockedArtifact.contains("insufficient-public-evidence:navigation.follow.succeeded"))
      assertFalse(Files.exists(artifactsDir.resolve("final-gameplay-ready.json")))
      assertFalse(Files.exists(artifactsDir.resolve("final-gameplay-confirmation-timeout.json")))
  }
  ```

- [x] **Step 2: Verify RED**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric smoke controller does not enter ready hold when public agent reports blocked*'
  ```

  Expected: FAIL because the controller does not yet write
  `public-agent-blocked.json` after a blocked helper artifact.

### Task 2: Implement Blocked Artifact Propagation

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricClientSmokeController.kt`

- [x] **Step 1: Parse blocked artifact**

  Add a small internal helper near the smoke artifact helpers:

  ```kotlin
  private fun publicAgentBlockedArtifact(): String? {
      val artifact = artifactsDir?.resolve("public-agent-gameplay-results.jsonl") ?: return null
      if (!Files.isRegularFile(artifact)) {
          return null
      }
      return Files.readAllLines(artifact).firstNotNullOfOrNull { line ->
          runCatching {
              val entry = smokeJson.parseToJsonElement(line).jsonObject
              if (entry["event"]?.jsonPrimitive?.content != "public-agent-blocked") {
                  return@runCatching null
              }
              entry["blocker"]?.jsonPrimitive?.contentOrNull ?: "unknown-blocker"
          }.getOrNull()
      }
  }
  ```

- [x] **Step 2: Fail before ready hold**

  After the helper process exits with code `0` in `runPublicAgentCommand`,
  call the helper and raise:

  ```kotlin
  publicAgentBlockedArtifact()?.let { blocker ->
      writeArtifact("public-agent-blocked.json", publicAgentBlockedArtifactContent(blocker))
      error("public agent command reported blocked: $blocker; log=$log")
  }
  ```

- [x] **Step 3: Verify GREEN**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric smoke controller does not enter ready hold when public agent reports blocked*' --tests '*FabricDriverModuleTest.fabric smoke controller runs ready notification command with live session context*' --tests '*FabricDriverModuleTest.fabric smoke controller stops final session after configured chat confirmation evidence*'
  ```

  Expected: PASS.

### Task 3: Docs And Verification

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-27-58-public-agent-blocked-outcome-design.md`
- Create: `docs/superpowers/plans/2026-06-27-58-public-agent-blocked-outcome-plan.md`

- [x] **Step 1: Register Phase 58**

  Add Phase 58 to `AGENTS.md` and checklist. Mark the Phase 57 rerun as fixing
  evidence clearing, and record that Phase 58 handles the separate blocked
  outcome propagation bug.

- [x] **Step 2: Run gates**

  Run:

  ```sh
  git diff --check
  mise run lint
  mise run architecture-check
  mise run ci
  ```

- [ ] **Step 3: Commit and push**

  Commit Phase 57 and Phase 58 together or separately if already split cleanly,
  then push directly to `main`.

## Self-Review

- Spec coverage: covers blocked helper artifact detection, no ready artifact on
  blocked outcome, successful ready path preservation, docs, and verification.
- Placeholder scan: no TBD/TODO/fill-in placeholders.
- Type consistency: helper names and environment variables match current
  `FabricClientSmokeController` and tests.
