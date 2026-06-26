# Sustained Generic Block Break Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `world.block.break` drive bounded Minecraft client break progress and report whether the target block changed.

**Architecture:** Keep the existing generic action and target handle model. Improve the Fabric execution adapter and public-agent verification so the acceptance scenario depends on observed block state changes, not accepted action calls.

**Tech Stack:** Kotlin/JVM, Fabric/Yarn mapped Minecraft client APIs, kotlinx.serialization JSON, Gradle through mise.

---

### Task 1: RED Tests For Sustained Break Contract

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [ ] **Step 1: Assert descriptor exposes ticks**

In the existing block-break discovery test, add:

```kotlin
assertEquals("integer", blockBreak.arguments["ticks"]?.type)
```

- [ ] **Step 2: Assert public agent sends ticks and blocks on unchanged break**

Add a `blockBreakResponse` fixture argument to `RecordingCraftlessHttpServer`.
Create a test where `world.block.break` returns:

```json
{"action":"world.block.break","status":"ACCEPTED","data":{"started":true,"changed":false}}
```

Assert:

```kotlin
assertEquals(PublicAgentGameplayState.BLOCKED, result.state)
assertEquals("insufficient-public-evidence:world.block.break.changed", result.blocker)
assertFalse(result.actionLog.map { it.action }.contains("inventory.equip"))
assertTrue(server.requestBodies.any { it.contains(""""ticks":80""") })
```

- [ ] **Step 3: Run RED tests**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric runtime discovery exposes block break only from client state' :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: FAIL because the descriptor does not expose `ticks` and the public
agent does not check `changed`.

### Task 2: Fabric Adapter Implementation

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt`

- [ ] **Step 1: Add `ticks` argument**

Add `ticks` to `fabricWorldBlockBreakDescriptor()` as an optional integer.

- [ ] **Step 2: Bound progress budget**

Parse `ticks` with defaults:

```kotlin
val ticks = invocation.arguments.intArgument("ticks") ?: DEFAULT_BREAK_TICKS
require(ticks in 1..MAX_BREAK_TICKS) { "block break ticks must be between 1 and $MAX_BREAK_TICKS" }
```

- [ ] **Step 3: Continue block breaking**

After `attackBlock`, loop `updateBlockBreakingProgress(position, side)` up to
`ticks`, stop when `world.getBlockState(position)` changes, and return a data
payload with `started`, `changed`, `ticks`, `block`, `handle`, `side`, and
`position`.

- [ ] **Step 4: Run focused driver test**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric runtime discovery exposes block break only from client state'
```

Expected: PASS.

### Task 3: Public-Agent Verification

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [ ] **Step 1: Send bounded ticks**

Add `"ticks": 80` to the public-agent `world.block.break` invocation args.

- [ ] **Step 2: Verify changed state**

Parse `data.changed` from the break response. If it is explicitly `false`,
return `insufficient-public-evidence:world.block.break.changed`.

- [ ] **Step 3: Run focused testkit tests**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: PASS.

### Task 4: Live Evidence, Gates, Push

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [ ] **Step 1: Add Phase 19 to docs**

Document the sustained generic block-break correction as a prerequisite for
material equip evidence.

- [ ] **Step 2: Re-run live no-hold gameplay**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=0 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=120000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Expected: public-agent artifacts show `world.block.break` with `changed =
true`, inventory material proof, `inventory.equip`, and selected-slot proof, or
a precise blocker.

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
git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-26-18-public-agent-material-equip-design.md docs/superpowers/plans/2026-06-26-18-public-agent-material-equip-plan.md docs/superpowers/specs/2026-06-26-19-sustained-block-break-design.md docs/superpowers/plans/2026-06-26-19-sustained-block-break-plan.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt
git commit -m "feat: sustain generic block breaks"
git push origin main
```
