# Bounded Attack Exploration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the process-external public agent continue bounded generated attack exploration beyond the first waypoint ring before blocking on missing combat target evidence.

**Architecture:** Keep the behavior in `testkit` public-agent policy. The runner composes generated `player.query`, `navigation.plan`, `navigation.follow`, and `entity.query`; it does not add product action ids or scenario shortcuts.

**Tech Stack:** Kotlin/JVM, Ktor Client MockEngine tests, kotlinx.serialization JSON, Gradle through mise.

---

### Task 1: RED Test For Multi-Ring Attack Exploration

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [x] **Step 1: Add failing regression test**

Add `runner continues bounded generated attack exploration beyond first waypoint ring`.
Configure the fake generated API so the first empty query and 24 subsequent
entity queries expose only empty state or generic aquatic living targets, then
expose a Cow. Assert the runner reaches `PublicAgentGameplayState.RAN`, attacks
the Cow handle, never attacks the Salmon handle, and never emits a scenario
shortcut.

- [x] **Step 2: Run the RED test**

Run:

```sh
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest.runner continues bounded generated attack exploration beyond first waypoint ring'
```

Expected before implementation: FAIL with the public agent blocked before
reaching the Cow response.

### Task 2: Bounded Generated Attack Exploration

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [x] **Step 1: Add attack exploration rings**

Add `ATTACK_EXPLORATION_RINGS = 3`.

- [x] **Step 2: Let waypoint generation produce bounded rings**

Change `CraftlessPoint.explorationWaypoints()` to accept `rings: Int = 1` and
return the established eight waypoint directions for each ring using
`EXPLORATION_STEP * ring`.

- [x] **Step 3: Use multi-ring waypoints only for attack search**

Change `exploreAttackTarget()` to call:

```kotlin
origin.explorationWaypoints(rings = ATTACK_EXPLORATION_RINGS)
```

Material exploration keeps the default one-ring behavior.

- [x] **Step 4: Run the focused test**

Run:

```sh
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest.runner continues bounded generated attack exploration beyond first waypoint ring'
```

Expected after implementation: PASS.

### Task 3: Verification And Final Gameplay

**Files:**
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Record the live blocker and correction**

Document the 2026-06-27 final run blocker and the bounded generated attack
exploration correction in the Phase 7 checklist.

- [x] **Step 2: Run gates**

Run:

```sh
git diff --check
mise exec -- gradle :testkit:test
mise run lint
mise run architecture-check
mise run ci
```

Expected: all pass.

- [x] **Step 3: Re-run final gameplay**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_FABRIC_SMOKE_ACTION_TIMEOUT_MS=720000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Observed: the public agent reached `publicAgentState=RAN` and the harness wrote
`final-gameplay-ready.json`. Robin did not join or confirm in Minecraft chat
before the hold expired, so `final-gameplay-confirmation.json` was not written
and final completion remains open in `docs/project-completion-checklist.md`.
