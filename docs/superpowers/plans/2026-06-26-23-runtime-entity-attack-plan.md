# Runtime Entity Attack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a generic runtime-discovered `entity.attack` operation backed by Fabric client-thread execution and composed by the public gameplay runner when available.

**Architecture:** Add a runtime graph operation next to `entity.query`, not a static scenario action. The Fabric backend resolves public entity handles internally and exposes only Craftless-owned handle/result data. The public-agent runner composes `entity.query`, navigation/look, and `entity.attack` through `POST /clients/{id}:run`.

**Tech Stack:** Kotlin/JVM, Fabric client APIs behind driver module boundaries, kotlinx.serialization JSON, Ktor Client tests, Gradle through mise.

---

### Task 1: RED Runtime Graph Tests

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbeTest.kt`
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [ ] **Step 1: Assert runtime graph exposure**

In the client-state capability test, assert `entity.attack` is available when
player, world, and interaction manager are available and exposes `target` plus
`max-distance` arguments.

- [ ] **Step 2: Assert graph adapter invocation**

Add a backend test that invokes `entity.attack` through
`backend.operationAdapters("alice").invoke(...)` with a target handle and
expects an accepted result from the generic adapter.

- [ ] **Step 3: Run RED tests**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricCapabilityProbeTest*' --tests '*FabricDriverModuleTest.fabric backend invokes entity attack through runtime graph adapter'
```

Expected: FAIL because `entity.attack` is not exposed or adapted yet.

### Task 2: RED Public-Agent Composition Test

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [ ] **Step 1: Add generated-action composition test**

Add a test where `/actions` includes `entity.attack` and `entity.query`
returns a passive living entity handle. Assert the runner posts
`entity.attack` through `:run`, passes the public handle as `target.handle`,
and does not post scenario shortcut names.

- [ ] **Step 2: Run RED test**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: FAIL because the runner does not invoke `entity.attack` yet.

### Task 3: Runtime Graph And Adapter Implementation

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt`

- [ ] **Step 1: Add `entity.attack` graph node**

Add a `RuntimeOperationNode` with id `entity.attack`, resource `entity`,
adapter `fabric.entity-attack`, required object `target`, optional number
`max-distance`, object result, and availability that requires player, world,
and interaction manager.

- [ ] **Step 2: Add backend adapter**

Register `fabric.entity-attack` in graph operation adapters. Resolve
`entity.handle-<id>` on the client thread, validate distance, call the internal
interaction manager attack method, swing the main hand, and return Craftless
result data.

- [ ] **Step 3: Run focused driver tests**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricCapabilityProbeTest*' --tests '*FabricDriverModuleTest.fabric backend invokes entity attack through runtime graph adapter'
```

Expected: PASS.

### Task 4: Public-Agent Composition Implementation

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [ ] **Step 1: Invoke optional generated attack**

After material equip verification and final `entity.query`, if `entity.attack`
is available and a public living/passive/hostile entity handle is present,
navigate near the target position, look at it, and invoke `entity.attack`
through generated dispatch.

- [ ] **Step 2: Run focused public-agent tests**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: PASS.

### Task 5: Checklist, Gates, Push

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [ ] **Step 1: Document Phase 23**

Record that `entity.attack` is generic runtime graph work and not the final
combat/loot completion proof.

- [ ] **Step 2: Run gates**

Run:

```sh
git diff --check
mise exec -- gradle :driver-fabric:test --tests '*FabricCapabilityProbeTest*' --tests '*FabricDriverModuleTest.fabric backend invokes entity attack through runtime graph adapter'
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
mise run lint
mise run architecture-check
mise run ci
```

Expected: all pass.

- [ ] **Step 3: Commit and push**

Commit with:

```sh
git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-26-23-runtime-entity-attack-design.md docs/superpowers/plans/2026-06-26-23-runtime-entity-attack-plan.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbeTest.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt
git commit -m "feat: expose generic entity attack"
git push origin main
```
