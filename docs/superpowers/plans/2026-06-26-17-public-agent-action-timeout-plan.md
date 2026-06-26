# Public Agent Action Timeout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert public-agent generated-action HTTP failures into explicit blocked artifacts instead of process crashes.

**Architecture:** Keep resilience in the external `testkit` public-agent runner. Do not retry generated actions; record the uncertain action outcome and stop with a machine-readable blocker.

**Tech Stack:** Kotlin/JVM, Ktor Client, MockEngine tests, kotlinx.serialization JSON, Gradle through mise.

---

### Task 1: RED Test For Generated Action Failure

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [ ] **Step 1: Add failing test**

Add a test named:

```kotlin
fun `runner records blocked artifacts when generated action request fails`()
```

Configure the recording server to throw during `navigation.follow`. Assert:

```kotlin
assertEquals(PublicAgentGameplayState.BLOCKED, result.state)
assertEquals("action-request-failed:navigation.follow", result.blocker)
assertTrue(result.actionLog.map { it.action }.contains("navigation.follow"))
assertTrue(gameplay.contains("action-request-failed:navigation.follow"))
```

- [ ] **Step 2: Run RED test**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: FAIL because the runner currently throws instead of returning a
blocked result.

### Task 2: Controlled Failure Implementation

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [ ] **Step 1: Add failure type**

Add a private exception carrying the action id and blocker:

```kotlin
private class PublicAgentActionRequestFailure(
    val blocker: String,
    cause: Throwable,
) : RuntimeException(blocker, cause)
```

- [ ] **Step 2: Wrap generated action request**

In `invokeGenerated`, catch `Exception`, append a failed action log with JSON
fields `action`, `status = "FAILED"`, `message`, and `blocker`, then throw
`PublicAgentActionRequestFailure("action-request-failed:$action", failure)`.

- [ ] **Step 3: Catch failure in `runOnce`**

Wrap the gameplay action sequence and return `blockedAndWrite(failure.blocker)`
when the failure type is caught.

- [ ] **Step 4: Verify focused tests pass**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: PASS.

### Task 3: Docs, Live Evidence, Verification, Push

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [ ] **Step 1: Add Phase 17 to guardrails and checklist**

Document that public-agent request failures become explicit blockers and are
not retried by default.

- [ ] **Step 2: Re-run live no-hold gameplay**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=0 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=120000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Expected: Gradle succeeds. Public-agent artifacts either show targeted block
break progress or a controlled `action-request-failed:*` blocker instead of a
process crash.

- [ ] **Step 3: Run gates**

Run:

```sh
git diff --check
mise run architecture-check
mise run lint
mise run ci
```

Expected: all pass.

- [ ] **Step 4: Commit and push**

Run:

```sh
git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-26-16-targetable-block-break-design.md docs/superpowers/plans/2026-06-26-16-targetable-block-break-plan.md docs/superpowers/specs/2026-06-26-17-public-agent-action-timeout-design.md docs/superpowers/plans/2026-06-26-17-public-agent-action-timeout-plan.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt
git commit -m "feat: target discovered block breaks"
git push origin main
```

