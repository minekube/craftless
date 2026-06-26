# Final Gameplay Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete Craftless only after a real multiplayer gameplay session verifies the product with Robin in Minecraft chat.

**Architecture:** The final run uses the existing graph-backed Fabric driver path, not a new static gameplay API. The Gradle task starts the testkit Minecraft server, launches the visible Fabric client, captures generated per-client OpenAPI/action/resource projections, captures SSE evidence, invokes discovered operations through `POST /clients/{id}:run`, keeps the session open for Robin, and records artifacts under `driver-fabric/build/craftless-final-gameplay/artifacts/`.

**Tech Stack:** Gradle Kotlin DSL, Fabric Loom, Ktor daemon/client, testkit local server, Server-Sent Events, macOS `say`, mise.

---

### Task 1: Final Gameplay Plan Contract

**Files:**
- Test: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`
- Create: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricFinalGameplayPlan.kt`

- [x] **Step 1: Write the failing plan test**

Add `fabric final gameplay plan gates completion on graph streams artifacts and robin chat` to assert:

```kotlin
val plan = FabricFinalGameplayPlan.default()
assertEquals("CRAFTLESS_FINAL_GAMEPLAY", plan.environmentGate)
assertEquals(listOf(":driver-fabric:fabricFinalGameplay"), plan.gradleTasks)
assertTrue(plan.steps.any { it.kind == FabricFinalGameplayStepKind.FETCH_GRAPH_OPENAPI })
assertTrue(plan.steps.any { it.kind == FabricFinalGameplayStepKind.SUBSCRIBE_SSE })
assertTrue(plan.steps.any { it.kind == FabricFinalGameplayStepKind.WAIT_FOR_ROBIN_CHAT_CONFIRMATION })
assertTrue(plan.artifacts.contains("client-events-stream.sse"))
assertTrue(plan.completionGates.any { it.contains("Robin", ignoreCase = true) && it.contains("Minecraft chat", ignoreCase = true) })
```

- [x] **Step 2: Verify the test fails**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests 'com.minekube.craftless.driver.fabric.v1_21_6.FabricDriverModuleTest.fabric final gameplay plan gates completion on graph streams artifacts and robin chat'
```

Expected: FAIL at compile time because `FabricFinalGameplayPlan` does not exist.

- [x] **Step 3: Implement the plan model**

Create `FabricFinalGameplayPlan.kt` with `FabricFinalGameplayPlan`, `FabricFinalGameplayStep`, and `FabricFinalGameplayStepKind`. Include only harness steps, artifact names, and completion gates. Do not add public gameplay action descriptors.

- [x] **Step 4: Verify the focused test passes**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests 'com.minekube.craftless.driver.fabric.v1_21_6.FabricDriverModuleTest.fabric final gameplay plan gates completion on graph streams artifacts and robin chat'
```

Expected: PASS.

### Task 2: SSE Artifact Capture

**Files:**
- Test: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricClientSmokeController.kt`

- [x] **Step 1: Write the failing SSE artifact test**

Extend `fabric smoke controller invokes generated chat and movement through daemon api and writes artifacts`:

```kotlin
val eventStream = Files.readString(artifactsDir.resolve("client-events-stream.sse"))
assertTrue(eventStream.contains("event: player.chat"))
assertTrue(eventStream.contains("event: player.move"))
assertTrue(eventStream.contains("data:"))
```

- [~] **Step 2: Verify the test fails**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests 'com.minekube.craftless.driver.fabric.v1_21_6.FabricDriverModuleTest.fabric smoke controller invokes generated chat and movement through daemon api and writes artifacts'
```

Expected: FAIL before implementation because `client-events-stream.sse` is not written.

Actual audit note: this red check was attempted after Task 1's missing
`FabricFinalGameplayPlan` test had already been added, so the compile failure
was masked by the missing type in the same test class. The behavior is covered
by the focused GREEN run after implementation.

- [x] **Step 3: Implement SSE artifact capture**

After the JSON event artifact is written, fetch:

```kotlin
val eventStream = http.getText(api.url("/clients/$SMOKE_CLIENT_ID/events:stream"))
writeArtifact("client-events-stream.sse", eventStream)
```

- [x] **Step 4: Verify the focused test passes**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests 'com.minekube.craftless.driver.fabric.v1_21_6.FabricDriverModuleTest.fabric smoke controller invokes generated chat and movement through daemon api and writes artifacts'
```

Expected: PASS.

### Task 3: Final Session Hold

**Files:**
- Test: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricClientSmokeController.kt`

- [x] **Step 1: Write the failing hold-config test**

Add `fabric smoke controller can hold the final gameplay session open`:

```kotlin
val controller =
    FabricClientSmokeController.fromEnvironment(
        mapOf(
            "CRAFTLESS_FABRIC_CLIENT_SMOKE" to "1",
            "CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS" to "60000",
        ),
    )
assertEquals(60_000.milliseconds, controller.holdAfterActions)
```

- [~] **Step 2: Verify the test fails**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests 'com.minekube.craftless.driver.fabric.v1_21_6.FabricDriverModuleTest.fabric smoke controller can hold the final gameplay session open'
```

Expected: FAIL before implementation because `holdAfterActions` is missing.

Actual audit note: this red check was not isolated before implementation
because the same test class still had the Task 1 missing-type compile failure.
The behavior is covered by the focused GREEN run after implementation.

- [x] **Step 3: Implement hold config**

Add `holdAfterActions: Duration`, read `CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS`, reject negative values, and `delay(holdAfterActions)` before stopping the client.

- [x] **Step 4: Verify the focused test passes**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests 'com.minekube.craftless.driver.fabric.v1_21_6.FabricDriverModuleTest.fabric smoke controller can hold the final gameplay session open'
```

Expected: PASS.

### Task 4: Final Gameplay Gradle Task

**Files:**
- Modify: `driver-fabric/build.gradle.kts`

- [x] **Step 1: Add `fabricFinalGameplay`**

Register a `JavaExec` task using `com.minekube.craftless.testkit.LocalMinecraftServerSmokeKt`.

Required defaults when `CRAFTLESS_FINAL_GAMEPLAY=1`:

```kotlin
environment("CRAFTLESS_FABRIC_CLIENT_SMOKE", "1")
environment("CRAFTLESS_LOCAL_SERVER_SMOKE_ROOT", layout.buildDirectory.dir("craftless-final-gameplay").get().asFile.absolutePath)
environment("CRAFTLESS_FABRIC_SMOKE_CHAT_MESSAGE", "hello from Craftless final gameplay")
environment("CRAFTLESS_SMOKE_EXPECT_CHAT_MESSAGE", "hello from Craftless final gameplay")
environment("CRAFTLESS_SMOKE_PROVISION_ITEM_ID", "minecraft:iron_sword")
environment("CRAFTLESS_SMOKE_PROVISION_ITEM_NAME", "Iron Sword")
environment("CRAFTLESS_SMOKE_PROVISION_ITEM_COUNT", "1")
environment("CRAFTLESS_FABRIC_SMOKE_REQUIRE_EQUIP_ITEM", "1")
environment("CRAFTLESS_FABRIC_SMOKE_STARTUP_SETTLE_MS", "3000")
environment("CRAFTLESS_FABRIC_SMOKE_HOLD_AFTER_ACTIONS_MS", "600000")
```

- [x] **Step 2: Verify task registration without launching Minecraft**

Run:

```sh
mise exec -- gradle :driver-fabric:tasks --group verification
```

Expected: output includes `fabricFinalGameplay`.

### Task 5: Final Gameplay Runbook And Checklist

**Files:**
- Create: `docs/final-gameplay-runbook.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Write runbook**

Include exact commands for:

```sh
mise run lint
mise run architecture-check
mise run ci
CRAFTLESS_FINAL_GAMEPLAY=1 mise exec -- gradle :driver-fabric:fabricFinalGameplay
say "Robin, join the Craftless test server now and confirm in Minecraft chat when the goal may be completed."
```

Also list required artifacts and the rule that Codex must not call `update_goal(status=complete)` until Robin's Minecraft chat confirmation exists.

- [x] **Step 2: Verify docs**

Run:

```sh
git diff --check
```

Expected: PASS.

### Task 6: Execute Real Gameplay

**Files:**
- Evidence: `driver-fabric/build/craftless-final-gameplay/artifacts/`

- [ ] **Step 1: Run final gameplay task**

Run:

```sh
CRAFTLESS_FINAL_GAMEPLAY=1 mise exec -- gradle :driver-fabric:fabricFinalGameplay
```

Expected: a real Fabric client joins the local server, sends chat, fetches graph-backed OpenAPI/actions/resources, captures SSE, equips the provisioned iron sword, invokes discovered movement/block operations, and records artifacts.

- [ ] **Step 2: Invite Robin**

Run when the server and client are ready:

```sh
say "Robin, join the Craftless test server now and confirm in Minecraft chat when the goal may be completed."
```

Expected: Robin joins or observes the session.

- [ ] **Step 3: Wait for Minecraft chat completion signal**

Expected server/chat evidence: Robin writes that the goal may be completed.

- [ ] **Step 4: Fix and reverify issues found during gameplay**

For each issue, write a failing test first unless it is purely docs/config, then run the focused test and `mise run ci`.

- [ ] **Step 5: Mark goal complete**

Only after Step 3 and Step 4, call:

```text
update_goal(status = "complete")
```
