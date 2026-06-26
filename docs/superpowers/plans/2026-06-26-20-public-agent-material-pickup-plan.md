# Public Agent Material Pickup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the public-agent runner collect dropped material by composing lower target selection, generic navigation, and inventory verification.

**Architecture:** Keep pickup as external agent policy in `testkit`. It selects from public `world.block.query` data, invokes existing generated actions, and uses inventory state as proof.

**Tech Stack:** Kotlin/JVM, Ktor Client MockEngine tests, kotlinx.serialization JSON, Gradle through mise.

---

### Task 1: RED Tests For Reachable Target And Pickup Movement

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [ ] **Step 1: Update action sequences**

Add a second `navigation.plan` and `navigation.follow` after
`world.block.break` and before final `inventory.query` in successful path
expectations.

- [ ] **Step 2: Add lower-target selection test**

Create a block query response with a higher log first and a lower log second.
Assert the break request targets the lower handle:

```kotlin
assertTrue(server.requestBodies.any {
    it.contains(""""target":{"handle":"world.block:13:63:-5"""")
})
```

- [ ] **Step 3: Run RED tests**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: FAIL because the runner picks the first target and does not perform
pickup navigation after the break.

### Task 2: Public-Agent Pickup Composition

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [ ] **Step 1: Select lower material targets**

Replace first-block selection with a target ranking that sorts by public
position `y` ascending and then public `distance` ascending.

- [ ] **Step 2: Navigate for pickup after changed break**

After `world.block.break` succeeds and does not report `changed = false`, call:

```kotlin
navigateTo(position = discoveredMaterialPosition, radius = 1.5)
    ?.let { blocker -> return blockedAndWrite(blocker) }
```

Then query inventory and proceed to equip.

- [ ] **Step 3: Run focused tests**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: PASS.

### Task 3: Live Evidence, Gates, Push

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [ ] **Step 1: Add Phase 20 to docs**

Document generic pickup composition and lower material target selection.

- [ ] **Step 2: Re-run live no-hold gameplay**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=0 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=120000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Expected: public-agent artifacts show changed block, pickup navigation,
inventory material proof, `inventory.equip`, and selected-slot verification, or
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
git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-26-18-public-agent-material-equip-design.md docs/superpowers/plans/2026-06-26-18-public-agent-material-equip-plan.md docs/superpowers/specs/2026-06-26-19-sustained-block-break-design.md docs/superpowers/plans/2026-06-26-19-sustained-block-break-plan.md docs/superpowers/specs/2026-06-26-20-public-agent-material-pickup-design.md docs/superpowers/plans/2026-06-26-20-public-agent-material-pickup-plan.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt
git commit -m "feat: collect public material drops"
git push origin main
```
