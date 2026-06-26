# Public Agent Material Exploration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the public-agent runner recover from an empty local material query by exploring through generic generated navigation and perception actions.

**Architecture:** Keep the behavior in `testkit` as external agent policy. The runner composes the live generated action catalog through `POST /clients/{id}:run`; product modules must not gain a scenario-specific survival action or static route.

**Tech Stack:** Kotlin/JVM, Ktor Client, kotlinx.serialization JSON, Gradle through mise.

---

### Task 1: Public Agent Exploration Tests

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [ ] **Step 1: Add RED test for exploration after empty local material query**

Add a test named:

```kotlin
fun `runner explores with generic navigation when local material query is empty`()
```

Configure the recording HTTP server so the first `world.block.query` response
has `{"count":0,"blocks":[]}` and the second `world.block.query` response
contains a log block position. Assert the action log starts with:

```kotlin
listOf(
    "inventory.query",
    "world.block.query",
    "player.query",
    "navigation.plan",
    "navigation.follow",
    "world.block.query",
    "navigation.plan",
    "navigation.follow",
    "player.query",
    "player.look",
    "player.raycast",
    "world.block.break",
    "inventory.query",
    "entity.query",
)
```

Assert the request bodies include at least two `"category":"log"` calls and
contain none of `task.survival`, `find.tree`, `mine.log`, `collect.wood`,
`craft.sword`, or `kill.cow`.

- [ ] **Step 2: Update blocked material-query test**

Configure all `world.block.query` responses as empty. Assert the runner returns
`BLOCKED` with `insufficient-public-evidence:world.block.query.log`, records
multiple `world.block.query` actions, records exploration navigation attempts,
and still does not call scenario-specific actions.

- [ ] **Step 3: Run focused tests to verify RED**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: FAIL before implementation because the runner stops after the first
empty `world.block.query`.

### Task 2: Public Agent Exploration Policy

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [ ] **Step 1: Extract material query helper**

Create a local helper in `runOnce` that invokes `world.block.query` with
`radius = 32.0`, `limit = 16`, and `category = "log"`, then returns the first
public block position when present.

- [ ] **Step 2: Extract navigation helper**

Create a local helper in `runOnce` that invokes `navigation.plan` with a generic
block-position goal, reads the public plan id, invokes `navigation.follow`, and
returns a blocker when plan evidence is missing.

- [ ] **Step 3: Add bounded exploration waypoints**

When the first material query returns no position, invoke `player.query`, build
bounded waypoints around the public player position, and try query-after-follow
for each waypoint. Use generic block goals only.

- [ ] **Step 4: Preserve public evidence blockers**

If no exploration attempt returns a material position, return
`insufficient-public-evidence:world.block.query.log` with the action log written
to artifacts. If `player.query` or `navigation.plan` lacks public evidence,
return the existing `insufficient-public-evidence:*` blocker.

- [ ] **Step 5: Verify focused tests pass**

Run:

```sh
mise exec -- gradle :testkit:test --tests '*PublicAgentGameplayRunnerTest*'
```

Expected: PASS.

### Task 3: Docs, Live Evidence, Verification, Push

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [ ] **Step 1: Add Phase 15 to the active phase list**

Document that Phase 15 composes generic exploration and material re-query
without introducing survival scenario APIs.

- [ ] **Step 2: Run live no-hold gameplay**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=0 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Expected: Gradle succeeds. Public-agent artifacts either show exploration
recovered to a material target and continued collection, or report an explicit
public-evidence blocker with repeated generic query/navigation evidence.

- [ ] **Step 3: Run gates**

Run:

```sh
git diff --check
mise run architecture-check
mise run ci
```

Expected: all pass.

- [ ] **Step 4: Commit and push**

Run:

```sh
git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-26-15-public-agent-material-exploration-design.md docs/superpowers/plans/2026-06-26-15-public-agent-material-exploration-plan.md testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt
git commit -m "feat: explore for public materials"
git push origin main
```

