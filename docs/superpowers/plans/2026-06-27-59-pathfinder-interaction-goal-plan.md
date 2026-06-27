# Pathfinder Interaction Goal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the private Fabric pathfinder adapter navigate to interaction-reachable block targets when the runtime pathfinder exposes that affordance.

**Architecture:** Keep public Craftless navigation descriptors unchanged. Extend only the versioned Fabric reflective pathfinder handles so `navigation.plan` can build a private interaction goal and fall back to the existing exact block goal when the private class is absent.

**Tech Stack:** Kotlin/JVM, Fabric versioned driver code, JUnit 5 tests, Gradle through mise.

---

### Task 1: RED Test For Interaction-Reachable Goal Preference

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/ReflectiveFabricPathfinderBackendTest.kt`

- [x] **Step 1: Add failing test**

Add `reflection backend prefers interaction reachable block goal when available`.
Use `RecordingPathfinderProbe` with both `goalFactory` and
`interactionGoalFactory`. Plan and follow a generic Craftless block navigation
goal. Assert `probe.calls` starts `get-to:10:65:-4`, not `goal:10:65:-4`, and
public evidence does not contain backend names.

- [x] **Step 2: Verify RED**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*ReflectiveFabricPathfinderBackendTest.reflection backend prefers interaction reachable block goal when available*'
```

Expected before implementation: FAIL because `ReflectiveFabricPathfinderHandles`
does not expose an interaction-goal factory and planning uses the exact block
goal.

### Task 2: Private Interaction Goal Adapter

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/ReflectiveFabricPathfinderBackend.kt`
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/ReflectiveFabricPathfinderBackendTest.kt`

- [x] **Step 1: Extend private handles**

Add `interactionGoalFactory: ((Int, Int, Int) -> Any)? = null` to
`ReflectiveFabricPathfinderHandles`. Do not make it required by `available`,
so runtimes without the private interaction class keep the existing fallback.

- [x] **Step 2: Prefer the interaction goal when planning**

In `ReflectiveFabricPathfinderBackend.plan`, choose:

```kotlin
val goalFactory = handles.interactionGoalFactory ?: handles.goalFactory
val goalObject =
    queryOnClient {
        goalFactory?.invoke(position.x, position.y, position.z)
    } ?: return unavailablePlan(goal)
```

- [x] **Step 3: Discover the private interaction goal reflectively**

In `ClassLoaderReflectiveFabricPathfinderProbe`, look for
`baritone.api.pathing.goals.GoalGetToBlock` and a constructor that accepts the
compiled Fabric lane `BlockPos`. Expose it as `interactionGoalFactory` by
creating `BlockPos(x, y, z)` inside the adapter. Keep the class name private
and never include it in public status or events.

- [x] **Step 4: Verify GREEN**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*ReflectiveFabricPathfinderBackendTest*'
```

Expected after implementation: PASS.

### Task 3: Checklist And Final Gameplay Rerun

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Record Phase 59 guardrails**

Add Phase 59 to the active sequence and checklist. State that this is private
pathfinder adapter behavior only and does not add public gameplay breadth.

- [x] **Step 2: Run gates**

Run:

```sh
git diff --check
mise run lint
mise run architecture-check
mise run ci
```

Expected: all pass.

- [x] **Step 3: Re-run final gameplay**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_FABRIC_SMOKE_ACTION_TIMEOUT_MS=120000 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=1800000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Observed: public-agent gameplay advanced beyond the Phase 57/58 material
navigation blocker, reached `final-gameplay-ready.json` for
`127.0.0.1:59029`, equipped a `Wooden Sword`, killed a Pig, and picked up
`Raw Porkchop`. The hold wrote `final-gameplay-confirmation-timeout.json`
because Robin's confirmation chat was not observed.
