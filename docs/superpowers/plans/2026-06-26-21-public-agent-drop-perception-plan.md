# Public Agent Drop Perception Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the public-agent runner use `entity.query` to find dropped material entities and navigate to them before inventory verification.

**Architecture:** Keep drop handling in external public-agent policy. The runner composes generated `entity.query`, `navigation.plan`, `navigation.follow`, and `inventory.query`; it does not add new product actions.

**Tech Stack:** Kotlin/JVM, Ktor Client MockEngine tests, kotlinx.serialization JSON, Gradle through mise.

---

### Task 1: RED Tests For Drop Perception

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [ ] **Step 1: Update successful action sequences**

Add `entity.query` after pickup navigation and before final material
`inventory.query`.

- [ ] **Step 2: Add material drop navigation test**

Configure the fake server to return an `entity.query` response with an object
entity labeled `Oak Log` and a position. Assert the runner invokes another
`navigation.plan` using that public entity position.

- [ ] **Step 3: Run RED tests**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: FAIL because the runner does not query entities before final
inventory verification yet.

### Task 2: Public-Agent Drop Perception Implementation

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [ ] **Step 1: Parse material drop positions**

Add a helper that reads `data.entities[]`, matches public labels containing
`log`, and returns the entity `position`.

- [ ] **Step 2: Compose entity navigation**

After block-target pickup navigation, invoke `entity.query` with radius and
limit. If a material entity position is returned, call `navigateTo` for that
position before final `inventory.query`.

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

- [ ] **Step 1: Add Phase 21 to docs**

Document public drop perception as an agent composition phase.

- [ ] **Step 2: Re-run live no-hold gameplay**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=0 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_SMOKE_ACTION_TIMEOUT_MS=120000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Expected: public-agent artifacts show entity/drop perception, inventory proof,
`inventory.equip`, and selected-slot verification, or a precise blocker.

- [ ] **Step 3: Run gates**

Run:

```sh
git diff --check
mise run architecture-check
mise run lint
mise run ci
```

Expected: all pass.
