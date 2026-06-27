# Local Server Action Environment Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the local-server smoke action-command environment boundary so final gameplay cannot recurse into outer ownership while still launching the Fabric smoke client controller.

**Architecture:** Keep `LocalMinecraftServerSmoke` as the owner of server lifecycle and action command launch. Before starting the action command, remove only outer owner/server setup variables, then inject the child server port, artifacts directory, and selected Java executable. Preserve Fabric smoke controller and public-agent variables because the action command is the client role.

**Tech Stack:** Kotlin/JVM, Gradle through mise, Craftless testkit.

---

### Task 1: RED Test For Action Command Environment Boundary

**Files:**
- Modify: `testkit/src/test/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmokeTest.kt`

- [x] **Step 1: Write failing test**

Add or update a test named:

```kotlin
@Test
fun `local server action command environment removes outer owner variables and keeps child smoke variables`() {
    val env =
        mutableMapOf(
            "PATH" to "/usr/bin",
            "CRAFTLESS_FINAL_GAMEPLAY" to "1",
            "CRAFTLESS_LOCAL_SERVER_SMOKE" to "1",
            "CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT" to "/tmp/craftless-final-gameplay",
            "CRAFTLESS_SMOKE_ACTION_COMMAND_JSON" to """["gradle",":driver-fabric:runClient"]""",
            "CRAFTLESS_SMOKE_EXPECT_CHAT_MESSAGE" to "hello from Craftless final gameplay",
            "CRAFTLESS_SMOKE_PROVISION_ITEM_ID" to "minecraft:iron_sword",
            "CRAFTLESS_FABRIC_CLIENT_SMOKE" to "1",
            "CRAFTLESS_FABRIC_SMOKE_ACTION_TIMEOUT_MS" to "120000",
            "CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS" to "1800000",
            "CRAFTLESS_PUBLIC_AGENT_COMMAND_JSON" to """["gradle",":testkit:publicAgentGameplay"]""",
        )

    env.removeInheritedLocalServerSmokeOwnerEnvironment()

    assertEquals("/usr/bin", env["PATH"])
        assertEquals("1", env["CRAFTLESS_FINAL_GAMEPLAY"])
    assertFalse("CRAFTLESS_LOCAL_SERVER_SMOKE" in env)
    assertFalse("CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT" in env)
    assertFalse("CRAFTLESS_SMOKE_ACTION_COMMAND_JSON" in env)
    assertFalse("CRAFTLESS_SMOKE_EXPECT_CHAT_MESSAGE" in env)
    assertFalse("CRAFTLESS_SMOKE_PROVISION_ITEM_ID" in env)
    assertEquals("1", env["CRAFTLESS_FABRIC_CLIENT_SMOKE"])
    assertEquals("120000", env["CRAFTLESS_FABRIC_SMOKE_ACTION_TIMEOUT_MS"])
    assertEquals("1800000", env["CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS"])
    assertEquals("""["gradle",":testkit:publicAgentGameplay"]""", env["CRAFTLESS_PUBLIC_AGENT_COMMAND_JSON"])
}
```

- [x] **Step 2: Verify RED**

Run:

```sh
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.LocalMinecraftServerSmokeTest.local server action command environment removes outer owner variables and keeps child smoke variables'
```

Expected before implementation: FAIL because the sanitizer does not exist or
because child/final variables are not preserved correctly.

### Task 2: Narrow The Owner Scrub

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmoke.kt`

- [x] **Step 1: Preserve Fabric client child variables**

In `inheritedLocalServerSmokeOwnerEnvironmentKeys`, remove
`CRAFTLESS_FABRIC_CLIENT_SMOKE` and all `CRAFTLESS_FABRIC_SMOKE_*` entries.
Keep removing:

```kotlin
"CRAFTLESS_LOCAL_SERVER_SMOKE",
"CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT",
"CRAFTLESS_LOCAL_SERVER_SMOKE_ACTION_TIMEOUT_MS",
"CRAFTLESS_FINAL_GAMEPLAY",
"CRAFTLESS_SMOKE_ACTION_COMMAND_JSON",
```

Do not remove `CRAFTLESS_FINAL_GAMEPLAY`, Fabric smoke variables, public-agent
variables, Java runtime selection variables, or the server/artifact variables
that the local-server boundary injects for the child.

- [x] **Step 2: Verify GREEN**

Run:

```sh
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.LocalMinecraftServerSmokeTest.local server action command environment removes outer owner variables and keeps child smoke variables'
```

Expected after implementation: PASS.

### Task 3: Guardrail Docs And Verification

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/specs/2026-06-27-61-local-server-action-env-boundary-design.md`
- Create: `docs/superpowers/plans/2026-06-27-61-local-server-action-env-boundary-plan.md`

- [x] **Step 1: Record Phase 61 guardrails**

Add Phase 61 to the active sequence and explain that it is a harness process
boundary correction only. State that it must not add public gameplay APIs,
scenario shortcuts, generated route families, or new version support claims.

- [x] **Step 2: Run focused gates**

Run:

```sh
mise exec -- gradle :testkit:test
git diff --check
mise run architecture-check
```

Expected: all pass.

- [x] **Step 3: Rerun final gameplay to the ready handoff**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 CRAFTLESS_FABRIC_SMOKE_CONNECT_TIMEOUT_MS=90000 CRAFTLESS_FABRIC_SMOKE_ACTION_TIMEOUT_MS=120000 CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS=1800000 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Actual: the action command starts the Fabric smoke controller, writes Craftless
final-gameplay artifacts including `server-evidence.jsonl`, reaches Robin's
confirmation hold, and writes `final-gameplay-confirmation-timeout.json` when
Robin does not join before the hold deadline.

- [ ] **Step 4: Commit and push**

Run:

```sh
git status --short
git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-27-61-local-server-action-env-boundary-design.md docs/superpowers/plans/2026-06-27-61-local-server-action-env-boundary-plan.md testkit/src/main/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmoke.kt testkit/src/test/kotlin/com/minekube/craftless/testkit/LocalMinecraftServerSmokeTest.kt
git commit -m "test: preserve fabric smoke child environment"
git push origin main
```

Expected: commit lands on `main`; remote CI starts for the pushed commit.
