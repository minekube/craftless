# Targetable Generic Block Break Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow public agents to pass discovered block handles or positions into generic `world.block.break`.

**Architecture:** Extend the existing generated action schema and Fabric binding for `world.block.break`; keep the action id generic and use Craftless-owned handles from `world.block.query`. Update the external public-agent runner to pass discovered target evidence instead of relying only on camera raycast.

**Tech Stack:** Kotlin/JVM, Fabric client-thread bindings, kotlinx.serialization JSON, Ktor Client tests, Gradle through mise.

---

### Task 1: RED Tests For Targeted Break

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [ ] **Step 1: Add descriptor test coverage**

In the existing `fabric runtime discovery exposes block break only from client
state` test, assert:

```kotlin
assertEquals("object", blockBreak.arguments["target"]?.type)
```

Expected before implementation: FAIL because the descriptor has no `target`
argument.

- [ ] **Step 2: Add public-agent target request assertion**

In `runner fetches live client spec before invoking gameplay actions`, assert
that one `world.block.break` request body contains:

```kotlin
""""target":{"handle":"world.block:12:65:-4","position":{"x":12,"y":65,"z":-4}}"""
```

Expected before implementation: FAIL because the runner calls
`world.block.break` without target evidence.

- [ ] **Step 3: Run RED tests**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest*' :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: FAIL for missing `target` descriptor and missing public-agent target
request body.

### Task 2: Target-Aware Break Schema And Runner

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt`
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [ ] **Step 1: Add `target` to the descriptor**

Add:

```kotlin
"target" to DriverActionArgument("object")
```

to `fabricWorldBlockBreakDescriptor()`.

- [ ] **Step 2: Preserve queried target evidence in the runner**

Parse both `handle` and `position` from the selected `world.block.query` block.
When invoking `world.block.break`, pass:

```json
{
  "max-distance": 6.0,
  "include-fluids": false,
  "target": {
    "handle": "world.block:x:y:z",
    "position": {"x": x, "y": y, "z": z}
  }
}
```

- [ ] **Step 3: Verify GREEN for schema and runner**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest*' :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: PASS for descriptor and public-agent request coverage.

### Task 3: Fabric Target Execution

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt`
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [ ] **Step 1: Add focused parse tests**

Add tests that invoke `world.block.break` with malformed target handles and
missing target positions through the backend. Assert stable `FAILED` or thrown
argument validation from the adapter path.

- [ ] **Step 2: Implement target parsing**

Parse `target.handle = world.block:x:y:z` or `target.position = {x,y,z}` into
a block position. Keep raycast fallback when `target` is absent.

- [ ] **Step 3: Implement targeted Fabric execution**

For a parsed target, compute distance from the player/camera, reject targets
beyond `max-distance`, call `interactionManager.attackBlock(blockPos, side)`,
swing the main hand when started, and return public block evidence including
the target handle.

- [ ] **Step 4: Run focused Fabric tests**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest*'
```

Expected: PASS.

### Task 4: Docs, Live Evidence, Verification, Push

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [ ] **Step 1: Add Phase 16 to guardrails and checklist**

Document that Phase 16 targets discovered block handles/positions through the
generic `world.block.break` action without adding a scenario action.

- [ ] **Step 2: Run live no-hold gameplay**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=0 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Expected: Gradle succeeds. Public-agent artifacts show targeted
`world.block.break` request evidence and either collected inventory/block state
or the next generic evidence blocker.

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
git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-26-16-targetable-block-break-design.md docs/superpowers/plans/2026-06-26-16-targetable-block-break-plan.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt
git commit -m "feat: target discovered block breaks"
git push origin main
```

