# Targetable Block Interact Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make generic block placement/building evidence possible by adding target support and change evidence to `world.block.interact`.

**Architecture:** Extend the existing Fabric action binding for `world.block.interact` with optional Craftless block target and side arguments. Public-agent composition reads generated action descriptor arguments before invoking placement through `POST /clients/{id}:run`.

**Tech Stack:** Kotlin/JVM, Fabric client APIs behind driver boundaries, kotlinx.serialization JSON, Ktor MockEngine tests, Gradle through mise.

---

### Task 1: RED Driver Tests

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [ ] **Step 1: Extend block interact assertions**

Assert `world.block.interact` exposes `target` and `side` arguments, then
invoke it with a target handle and expect accepted change evidence.

- [ ] **Step 2: Run RED driver test**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric runtime discovery exposes block interact only from client state'
```

Expected: FAIL because `target` and `side` are not part of the descriptor yet.

### Task 2: RED Public-Agent Test

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [ ] **Step 1: Add descriptor-gated placement test**

Mock `/actions` so `world.block.interact` includes a `target` argument. Return
a support block from `world.block.query` and assert the runner posts
`world.block.interact` with `target.handle` and `side = up`.

- [ ] **Step 2: Run RED public-agent test**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: FAIL because the runner does not compose targetable interaction yet.

### Task 3: Fabric Targetable Interaction

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt`

- [ ] **Step 1: Add descriptor arguments**

Add `target` object and `side` string arguments to
`fabricWorldBlockInteractDescriptor()`.

- [ ] **Step 2: Add target execution path**

Parse `target` using the existing Craftless block handle/position parser. Parse
`side` into an internal direction. Compare the adjacent block state before and
after interaction, then return `changed` and adjacent position evidence.

- [ ] **Step 3: Run focused driver test**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric runtime discovery exposes block interact only from client state'
```

Expected: PASS.

### Task 4: Public-Agent Placement Composition

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [ ] **Step 1: Parse action descriptor argument support**

Add a helper that detects whether `/clients/{id}/actions` advertises a given
argument for a given action.

- [ ] **Step 2: Invoke placement when supported**

After equipping collected material, query a nearby support block and invoke
`world.block.interact` with `target` and `side = up`. Block if returned
`changed` is explicitly false.

- [ ] **Step 3: Run focused public-agent tests**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: PASS.

### Task 5: Checklist, Gates, Push

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [ ] **Step 1: Document Phase 24**

Record targetable block interaction as generic placement/building support, not
final structure completion.

- [ ] **Step 2: Run gates and live smoke**

Run:

```sh
git diff --check
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric runtime discovery exposes block interact only from client state'
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
mise run lint
mise run architecture-check
mise run ci
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=0 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=120000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Expected: all non-live gates pass; live artifacts either show targetable
`world.block.interact` with `changed = true` or report the next precise public
evidence blocker.

- [ ] **Step 3: Commit and push**

Commit with:

```sh
git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-26-24-targetable-block-interact-design.md docs/superpowers/plans/2026-06-26-24-targetable-block-interact-plan.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt
git commit -m "feat: target block interactions"
git push origin main
```
