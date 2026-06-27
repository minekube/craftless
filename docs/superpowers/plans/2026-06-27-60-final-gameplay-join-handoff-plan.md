# Final Gameplay Join Handoff Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make final gameplay ready handoff artifacts explicit enough for Robin to join the held server and send the required confirmation phrase.

**Architecture:** Keep the Fabric smoke controller's ready boundary as the single source of truth. When the controller writes `final-gameplay-ready.json`, also include the configured confirmation phrase in that JSON and write a sibling human-readable `final-gameplay-join-instructions.txt`. This is evidence plumbing only and does not change generated public APIs or gameplay execution.

**Tech Stack:** Kotlin/JVM, Fabric driver smoke tests, Gradle through mise.

---

### Task 1: RED Test For Join Handoff Artifact

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add failing assertions**

In `fabric smoke controller runs ready notification command with live session context`, configure `CRAFTLESS_FABRIC_SMOKE_CONFIRM_CHAT_CONTAINS` with `goal may be completed`. After reading `final-gameplay-ready.json`, assert it contains:

```kotlin
assertTrue(readyArtifact.contains("\"confirmation-contains\":\"goal may be completed\""))
```

Then read `final-gameplay-join-instructions.txt` and assert it includes:

```kotlin
val joinInstructions = readArtifact(artifactsDir, "final-gameplay-join-instructions.txt")
assertTrue(joinInstructions.contains("Server: localhost:25567"))
assertTrue(joinInstructions.contains("Confirmation phrase: goal may be completed"))
assertTrue(joinInstructions.contains("Client id: fabric-smoke"))
```

- [x] **Step 2: Verify RED**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric smoke controller runs ready notification command with live session context*'
```

Expected before implementation: FAIL because the ready JSON lacks
`confirmation-contains` and `final-gameplay-join-instructions.txt` is absent.

### Task 2: Ready Handoff Artifacts

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricClientSmokeController.kt`

- [x] **Step 1: Write join instructions with ready artifact**

In `runReadyNotificationCommand`, after writing `final-gameplay-ready.json`,
also write `final-gameplay-join-instructions.txt` using a private helper:

```kotlin
writeArtifact("final-gameplay-ready.json", readyNotificationArtifact(baseUrl))
writeArtifact("final-gameplay-join-instructions.txt", finalGameplayJoinInstructions(baseUrl))
```

- [x] **Step 2: Include confirmation phrase in ready JSON**

Extend `readyNotificationArtifact` with:

```kotlin
"confirmation-contains" to (confirmationChatContains ?: ""),
```

- [x] **Step 3: Add the text helper**

Add:

```kotlin
private fun finalGameplayJoinInstructions(baseUrl: String): String =
    buildString {
        appendLine("Craftless final gameplay is ready.")
        appendLine("Server: ${target.host}:${target.port}")
        appendLine("Client id: $SMOKE_CLIENT_ID")
        appendLine("Base URL: $baseUrl")
        appendLine("Artifacts: ${artifactsDir?.toString() ?: ""}")
        appendLine("Hold ms: ${holdAfterActions.inWholeMilliseconds}")
        appendLine("Confirmation phrase: ${confirmationChatContains ?: ""}")
    }
```

- [x] **Step 4: Verify GREEN**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric smoke controller runs ready notification command with live session context*'
```

Expected after implementation: PASS.

### Task 3: Guardrail Docs And Verification

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Record Phase 60 guardrails**

Add Phase 60 to the active sequence and explain that it is final-session
handoff evidence only. State that it must not add public gameplay APIs, bypass
Robin's confirmation, or mark timeout as success.

- [x] **Step 2: Run focused gates**

Run:

```sh
git diff --check
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric smoke controller runs ready notification command with live session context*' --tests '*FabricDriverModuleTest.fabric smoke controller stops final session after configured chat confirmation evidence*' --tests '*FabricDriverModuleTest.fabric smoke controller writes confirmation timeout artifact when Robin chat is not observed*'
mise run architecture-check
```

Expected: all pass.

- [ ] **Step 3: Commit and push**

Run:

```sh
git status --short
git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-27-60-final-gameplay-join-handoff-design.md docs/superpowers/plans/2026-06-27-60-final-gameplay-join-handoff-plan.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricClientSmokeController.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
git commit -m "test: add final gameplay join handoff"
git push origin main
```

Expected: commit lands on `main`; remote CI starts for the pushed commit.
