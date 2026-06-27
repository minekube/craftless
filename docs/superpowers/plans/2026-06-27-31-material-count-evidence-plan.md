# Material Count Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the process-external public agent require increased public inventory material count for repeated material collection attempts.

**Architecture:** Keep the behavior in `testkit` public-agent policy. The runner composes generated `world.block.query`, `navigation.plan`, `navigation.follow`, `entity.query`, and `inventory.query`; it does not add product action ids or scenario shortcuts.

**Tech Stack:** Kotlin/JVM, Ktor Client MockEngine tests, kotlinx.serialization JSON, Gradle through mise.

---

### Task 1: RED Test For Stale Material Evidence

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [x] **Step 1: Add failing regression test**

Add `runner keeps collecting recipe materials until public inventory count increases`.
Configure the fake generated API with `recipe.query` and `recipe.craft`
available so the runner needs two public material items. Return one `Oak Log`
after the first break, the same stale one-log inventory after the second break,
then two `Oak Log` count only on a later retry. Assert the runner reaches
`PublicAgentGameplayState.RAN`, invokes `world.block.break` at least twice,
queries inventory at least five times, and never emits a scenario shortcut.

- [x] **Step 2: Run the RED test**

Run:

```sh
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest.runner keeps collecting recipe materials until public inventory count increases'
```

Expected before implementation: FAIL with the public agent blocked on
`insufficient-public-evidence:inventory.query.recipe-material`.

### Task 2: Count-Increase Material Verification

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [x] **Step 1: Pass the known material count into each collection attempt**

Change the material collection loop to call:

```kotlin
val collection = collectMaterialInventory(finalInventoryObject?.logItemCount() ?: 0)
```

- [x] **Step 2: Require pickup evidence to exceed that count**

Change `collectMaterialInventory` so the pickup loop only stops when:

```kotlin
(finalInventory.responseObject()?.logItemCount() ?: 0) > minimumLogCountExclusive
```

Return `insufficient-public-evidence:inventory.query.recipe-material` when a
later recipe-material collection attempt cannot prove an increased count.

- [x] **Step 3: Run focused tests**

Run:

```sh
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest.runner keeps collecting recipe materials until public inventory count increases'
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest'
```

Expected after implementation: PASS.

### Task 3: Verification And Final Gameplay

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Record the live blocker and correction**

Document the 2026-06-27 held final run blocker and the count-increase
correction in the Phase 7 and Phase 31 checklist entries.

- [x] **Step 2: Run gates**

Run:

```sh
git diff --check
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest'
mise run lint
mise run architecture-check
mise run ci
```

Expected: all pass.

- [ ] **Step 3: Re-run final gameplay**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_FABRIC_SMOKE_ACTION_TIMEOUT_MS=720000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Expected: the public agent either reaches `publicAgentState=RAN` and the
ready window for Robin's confirmation, or writes a precise new generic public
evidence blocker.
