# Public Agent Material Equip Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the external public-agent runner select collected material through the generated `inventory.equip` action and verify the selected slot from public inventory state.

**Architecture:** Keep the behavior in `testkit` as external public-agent policy. The runner discovers required actions, invokes generic generated actions through `POST /clients/{id}:run`, and blocks with machine-readable evidence when a required generic primitive or state proof is missing.

**Tech Stack:** Kotlin/JVM, Ktor Client, MockEngine tests, kotlinx.serialization JSON, Gradle through mise.

---

### Task 1: RED Test For Generic Material Equip

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [ ] **Step 1: Update the happy-path action sequence expectation**

Change the expected `result.actionLog.map { it.action }` list to include
`inventory.equip` and a follow-up `inventory.query` between collected-material
inventory proof and `entity.query`:

```kotlin
listOf(
    "inventory.query",
    "world.block.query",
    "navigation.plan",
    "navigation.follow",
    "player.query",
    "player.look",
    "player.raycast",
    "world.block.break",
    "inventory.query",
    "inventory.equip",
    "inventory.query",
    "entity.query",
)
```

- [ ] **Step 2: Add slot-derived invocation assertion**

Assert that the runner sends the slot from public inventory state:

```kotlin
assertTrue(server.requestBodies.any { it.contains("inventory.equip") })
assertTrue(server.requestBodies.any { it.contains(""""slot":0""") })
```

- [ ] **Step 3: Add blocker test for missing selected-slot evidence**

Add a test that returns a log slot but omits or changes `selected-slot` after
equip. Assert:

```kotlin
assertEquals(PublicAgentGameplayState.BLOCKED, result.state)
assertEquals("insufficient-public-evidence:inventory.equip.selected-slot", result.blocker)
assertTrue(result.actionLog.map { it.action }.contains("inventory.equip"))
assertFalse(result.actionLog.map { it.action }.contains("entity.query"))
```

- [ ] **Step 4: Run RED test**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: FAIL because `inventory.equip` is not required or invoked by the
public-agent runner yet.

### Task 2: Public-Agent Equip Implementation

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [ ] **Step 1: Require the generic action**

Add `inventory.equip` to `requiredActions` after `inventory.query`.

- [ ] **Step 2: Parse a public hotbar log slot**

Replace the boolean log check helper with a helper that returns an `Int?`:

```kotlin
private fun JsonObject.logHotbarSlot(): Int?
```

The helper must inspect `data.slots`, require `slot` in `0..8`, require the
slot to be non-empty, and match `item-name` containing `log`, case-insensitive.

- [ ] **Step 3: Invoke equip and verify state**

After final material inventory proof:

```kotlin
val logSlot =
    finalInventory.responseObject()?.logHotbarSlot()
        ?: return blockedAndWrite("insufficient-public-evidence:inventory.query.hotbar-log")
invokeGenerated(
    action = "inventory.equip",
    args = buildJsonObject { put("slot", JsonPrimitive(logSlot)) },
)
val equippedInventory = invokeGenerated("inventory.query")
if (equippedInventory.responseObject()?.selectedSlot() != logSlot) {
    return blockedAndWrite("insufficient-public-evidence:inventory.equip.selected-slot")
}
```

- [ ] **Step 4: Run focused tests**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: PASS.

### Task 3: Docs, Live Evidence, Verification, Push

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [ ] **Step 1: Add Phase 18 to guardrails and checklist**

Document that the public-agent runner selects collected material through
generic inventory actions and public state verification.

- [ ] **Step 2: Re-run live no-hold gameplay**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=0 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=120000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Expected: Gradle succeeds. Public-agent artifacts show collected material,
`inventory.equip`, and follow-up selected-slot evidence, or a precise blocker.

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
git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-26-18-public-agent-material-equip-design.md docs/superpowers/plans/2026-06-26-18-public-agent-material-equip-plan.md testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt
git commit -m "feat: equip collected public materials"
git push origin main
```
