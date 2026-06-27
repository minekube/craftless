# Combat Miss Retry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the external public-agent combat loop recover from bounded generated `entity.attack` misses by refreshing public state and retrying without adding product API shortcuts.

**Architecture:** Keep the change in `testkit` public-agent policy. The runner composes existing generated actions (`entity.query`, `player.query`, navigation, optional `player.move`, and `entity.attack`) and does not add new driver bindings, descriptors, routes, or CLI command catalogs.

**Tech Stack:** Kotlin/JVM, Ktor MockEngine, kotlinx.serialization JSON, JUnit 5, Gradle through mise.

---

### Task 1: Public-Agent Combat Miss Recovery

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [x] **Step 1: Write the failing regression**

Add `runner revalidates public attack target after generated attack misses`.
The fake server returns a successful first generated attack, then a generated
`entity.attack` response with `hit=false` and
`entity-target-out-of-range`, then a later successful attack after public
state is refreshed.

- [x] **Step 2: Verify RED**

Run:

```sh
mise exec -- gradle :testkit:test --rerun-tasks --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest.runner revalidates public attack target after generated attack misses'
```

Expected before implementation: fails because the runner immediately reports
`insufficient-public-evidence:entity.attack.hit`.

- [x] **Step 3: Implement retry from public evidence**

When `entity.attack` reports `hit=false` and attempts remain, call
`entity.query`, refresh the current target through the same public target
selection policy, re-run `focusAttackTarget`, and continue the bounded combat
loop.

- [x] **Step 4: Verify GREEN**

Run:

```sh
mise exec -- gradle :testkit:test --rerun-tasks --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest.runner revalidates public attack target after generated attack misses'
```

Expected: pass.

### Task 2: Documentation And Final Verification

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Record Phase 38**

Add the phase to the active sequence and checklist with the live diagnostic
evidence from the 2026-06-27 rerun.

- [x] **Step 2: Run broader checks**

Run:

```sh
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest'
mise run lint
mise run ci
git diff --check
```

Expected: all pass.

- [x] **Step 3: Rerun final gameplay**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_FABRIC_SMOKE_ACTION_TIMEOUT_MS=720000 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=600000 CRAFTLESS_FABRIC_SMOKE_READY_REMINDER_MS=60000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Result on 2026-06-27: the public agent reached `publicAgentState=RAN`, wrote
`final-gameplay-ready.json`, and did not block on generated attack-miss
handling. The hold timed out without Robin's Minecraft chat confirmation, so
final completion remains open.
