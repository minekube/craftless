# Phase 158: Official Fabric Connected Client State Probe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove the official/latest Fabric lane can attach, connect to a real local server, and project connected client-state through shared graph infrastructure without static gameplay actions.

**Architecture:** Add a narrow official-mapped lifecycle connector adapter and keep all graph/OpenAPI projection shared. Extend the opt-in official attach probe to exercise public daemon lifecycle and record connected client-state evidence.

**Tech Stack:** Kotlin, Gradle, Fabric Loom official mappings, Ktor, kotlinx.serialization, mise.

---

## File Structure

- Modify: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt`
  - delegate `connect(target)` to an injected connector.
- Create: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricClientConnector.kt`
  - narrow official-mapped lifecycle connector.
- Modify: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt`
  - focused backend tests for connect delegation and graph state.
- Modify: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricAttachProbe.kt`
  - connect to a real local server and record connected state.
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`
  - architecture guards for official lane boundaries.
- Create: `docs/superpowers/evidence/2026-06-28-official-fabric-connected-client-state-probe.md`
  - final evidence after verification.
- Modify: `docs/project-completion-checklist.md`
  - phase status and final gate.

## Task 1: Backend Connect Delegation Test

- [ ] **Step 1: Write the failing test**

Add a test to `OfficialFabricSharedRuntimeMetadataTest.kt`:

```kotlin
@Test
fun `official backend connect delegates to lifecycle connector`() {
    val target = ConnectionTarget(host = "127.0.0.1", port = 25565)
    val observedTargets = mutableListOf<ConnectionTarget>()
    val backend = OfficialFabricDriverBackend(
        clientId = "official-test",
        runtimeMetadataProvider = { FabricRuntimeMetadataSnapshot.empty() },
        clientStateProvider = { FabricClientStateGraphSnapshot.disconnected() },
        clientConnector = object : OfficialFabricClientConnector {
            override fun connect(target: ConnectionTarget): Boolean {
                observedTargets += target
                return true
            }
        },
    )

    val result = backend.connect(target)

    assertEquals(listOf(target), observedTargets)
    assertEquals(DriverBackendOperation.CONNECT, result.operation)
    assertTrue(result.observed)
    assertTrue(result.message.contains("127.0.0.1:25565"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official backend connect delegates to lifecycle connector*'
```

Expected: FAIL because `OfficialFabricDriverBackend` has no injectable connector and `connect` still reports unsupported.

- [ ] **Step 3: Implement minimal connector injection**

Add `OfficialFabricClientConnector.kt` with:

```kotlin
package com.minekube.craftless.driver.fabric.official

import com.minekube.craftless.driver.api.ConnectionTarget

internal interface OfficialFabricClientConnector {
    fun connect(target: ConnectionTarget): Boolean
}
```

Modify `OfficialFabricDriverBackend` to accept
`clientConnector: OfficialFabricClientConnector` and return observed connect
results from it.

- [ ] **Step 4: Run test to verify it passes**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official backend connect delegates to lifecycle connector*'
```

Expected: PASS.

## Task 2: Official-Mapped Client Connector

- [ ] **Step 1: Inspect official Minecraft connect API**

Run `javap` against the official 26.x client classpath for:

```sh
net.minecraft.client.gui.screens.ConnectScreen
net.minecraft.client.gui.screens.TitleScreen
net.minecraft.client.multiplayer.ServerData
net.minecraft.client.multiplayer.resolver.ServerAddress
```

Record the exact constructor/static method names in the evidence file.

- [ ] **Step 2: Write failing compile/use test**

Add a focused architecture or compile test that references the production
connector class by name and verifies the backend default is not a no-op.

Run:

```sh
mise exec -- gradle :driver-fabric-official:compileKotlin
```

Expected before implementation: FAIL because the production connector does not exist.

- [ ] **Step 3: Implement official connector**

Implement `MinecraftOfficialFabricClientConnector` in
`OfficialFabricClientConnector.kt`. It must:

- call `Minecraft.getInstance()`;
- schedule work on the Minecraft client thread;
- build official `ServerAddress`/`ServerData` values from `ConnectionTarget`;
- open the official connect screen;
- return `true` only when scheduling succeeds.

- [ ] **Step 4: Compile**

Run:

```sh
mise exec -- gradle :driver-fabric-official:compileKotlin
```

Expected: PASS.

## Task 3: Connected Official Attach Probe

- [ ] **Step 1: Write failing probe expectation**

Update `OfficialFabricAttachProbe.kt` so enabled probe mode requires connected
client-state evidence when `CRAFTLESS_OFFICIAL_ATTACH_PROBE_CONNECT=1`.

Run:

```sh
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 CRAFTLESS_OFFICIAL_ATTACH_PROBE_CONNECT=1 CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=120000 mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Expected before implementation: FAIL or report `client-not-connected`.

- [ ] **Step 2: Add local server/connect flow**

Reuse the existing local server/smoke helper if available. If no reusable helper
exists, add a small test-only helper inside the probe package that starts the
same repository-supported Minecraft server artifact path used by current Fabric
smokes.

The probe must call the daemon/client connect API rather than invoking the
connector directly.

- [ ] **Step 3: Verify connected evidence**

Run the enabled probe again:

```sh
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 CRAFTLESS_OFFICIAL_ATTACH_PROBE_CONNECT=1 CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=180000 mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Expected: PASS with connected client-state resources available in
`driver-fabric-official/build/craftless-official-attach-probe/client-openapi.json`.

## Task 4: Architecture And Docs

- [ ] **Step 1: Add architecture guards**

Extend `FabricDriverModuleTest` so it fails if the official lane depends on
`driver-fabric`, registers in `driver-mods.json`, or exposes public gameplay
actions while this phase is only connected-state evidence.

- [ ] **Step 2: Run focused architecture tests**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official*'
```

Expected: PASS.

- [ ] **Step 3: Write evidence and checklist**

Create
`docs/superpowers/evidence/2026-06-28-official-fabric-connected-client-state-probe.md`
with the exact commands and observed OpenAPI/resource summary. Update
`docs/project-completion-checklist.md`.

## Task 5: Final Verification And Commit

- [ ] **Step 1: Run local verification**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test :driver-fabric:test --tests '*FabricDriverModuleTest.official*'
mise run fabric-lane-check-latest-official
mise run ci
git diff --check
```

Expected: all PASS.

- [ ] **Step 2: Commit and push**

Run:

```sh
git add AGENTS.md */AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-158-official-fabric-connected-client-state-probe-design.md docs/superpowers/plans/2026-06-28-158-official-fabric-connected-client-state-probe-plan.md driver-fabric-official driver-fabric docs/superpowers/evidence/2026-06-28-official-fabric-connected-client-state-probe.md
git commit -m "feat: probe connected official fabric client state"
git push origin main
```

Expected: commit lands on `main` and pushes to `origin/main`.

## Self-Review

- No placeholders remain.
- Every code behavior task starts with a failing test/probe.
- The plan keeps official 26.x code narrow and private.
- The plan does not add static gameplay actions, public route families, CLI
  catalogs, or packaging support claims.
