# Official Fabric Event Source Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make official Fabric connected OpenAPI report available shared event resources/events from lane-provided event-source evidence, without adding gameplay operations.

**Architecture:** Add a small official-lane event-source provider that returns Craftless-owned `RuntimeSourceEvidence` and registers minimal Fabric lifecycle callbacks in the official entrypoint. `OfficialFabricDriverBackend.runtimeGraph(...)` continues composing the shared event graph and marks it available only when evidence is present. Generated actions remain empty.

**Tech Stack:** Kotlin/JVM, Fabric API client lifecycle/networking callbacks, official/Mojang-mapped Minecraft 26.2, Craftless driver-fabric-discovery event graph helpers, Gradle through mise.

---

### Task 1: Add Official Event Source Provider Seam

**Files:**
- Modify: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt`
- Modify: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt`
- Create: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricEventSourceProvider.kt`

- [x] **Step 1: Write the failing backend graph test**

Extend `official backend projects client state from lane provider without
adding operations` so the backend is constructed with:

```kotlin
eventSourceProvider =
    OfficialFabricEventSourceProvider {
        listOf(
            RuntimeSourceEvidence("event-source", "driver:0.1.0-SNAPSHOT"),
            RuntimeSourceEvidence("callback", "craftless-official-callback-play-join"),
        )
    },
```

Add assertions:

```kotlin
assertEquals(RuntimeAvailability.available(), resources.getValue("event").availability)
assertEquals(RuntimeAvailability.available(), events.getValue("event.lifecycle").availability)
assertEquals(RuntimeAvailability.available(), events.getValue("event.action").availability)
assertEquals(RuntimeAvailability.available(), events.getValue("event.capability").availability)
assertEquals(setOf("event-source", "callback"), resources.getValue("event").sourceEvidence.map { it.kind }.toSet())
```

- [x] **Step 2: Verify the test fails for missing symbols/signature**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official backend projects client state from lane provider without adding operations*'
```

Expected: FAIL because `OfficialFabricDriverBackend` does not accept
`eventSourceProvider`, and `OfficialFabricEventSourceProvider` does not exist.

- [x] **Step 3: Implement provider seam and graph wiring**

Create `OfficialFabricEventSourceProvider.kt`:

```kotlin
package com.minekube.craftless.driver.fabric.official

import com.minekube.craftless.protocol.RuntimeSourceEvidence
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal fun interface OfficialFabricEventSourceProvider {
    fun sourceEvidence(): List<RuntimeSourceEvidence>
}

internal object MinecraftOfficialFabricEventSources : OfficialFabricEventSourceProvider {
    private const val CLIENT_TICK_START = "craftless-official-callback-client-tick-start"
    private const val CLIENT_TICK_END = "craftless-official-callback-client-tick-end"
    private const val PLAY_JOIN = "craftless-official-callback-play-join"
    private const val PLAY_DISCONNECT = "craftless-official-callback-play-disconnect"

    private val registered = AtomicBoolean()
    private val clientTickStart = AtomicLong()
    private val clientTickEnd = AtomicLong()
    private val playJoin = AtomicLong()
    private val playDisconnect = AtomicLong()

    fun register() {
        if (!registered.compareAndSet(false, true)) {
            return
        }
        ClientTickEvents.START_CLIENT_TICK.register { clientTickStart.incrementAndGet() }
        ClientTickEvents.END_CLIENT_TICK.register { clientTickEnd.incrementAndGet() }
        ClientPlayConnectionEvents.JOIN.register { _, _, _ -> playJoin.incrementAndGet() }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> playDisconnect.incrementAndGet() }
    }

    override fun sourceEvidence(): List<RuntimeSourceEvidence> =
        listOf(
            RuntimeSourceEvidence("event-source", "driver:official"),
            RuntimeSourceEvidence("callback", CLIENT_TICK_START),
            RuntimeSourceEvidence("callback", CLIENT_TICK_END),
            RuntimeSourceEvidence("callback", PLAY_JOIN),
            RuntimeSourceEvidence("callback", PLAY_DISCONNECT),
        )
}
```

Update `OfficialFabricDriverBackend` constructor and graph composition:

```kotlin
private val eventSourceProvider: OfficialFabricEventSourceProvider = MinecraftOfficialFabricEventSources,
```

Use:

```kotlin
val eventSourceEvidence = eventSourceProvider.sourceEvidence()
fabricEventGraphFragment(
    sourceEvidence = eventSourceEvidence,
    available = eventSourceEvidence.isNotEmpty(),
)
```

- [x] **Step 4: Register official callbacks from entrypoint**

In `CraftlessFabricOfficialEntrypoint.onInitializeClient()`, call:

```kotlin
MinecraftOfficialFabricEventSources.register()
```

- [x] **Step 5: Verify focused official tests pass**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Expected: PASS.

### Task 2: Live OpenAPI Evidence And Docs

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-official-fabric-event-source-metadata.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/phase-index.md`
- Modify: `docs/superpowers/plans/2026-06-28-161-official-fabric-event-source-metadata-plan.md`

- [x] **Step 1: Run connected official attach probe**

Run:

```sh
rm -rf driver-fabric-official/build/craftless-official-attach-probe
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_CONNECT=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=180000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 2: Inspect generated OpenAPI evidence**

Run:

```sh
jq -r '(.["x-craftless-events"][] | [.id,.availability,(.availabilityReason // "")] | @tsv), ("actions=" + (.["x-craftless-actions"] | length | tostring))' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi-connected.json
```

Expected: event rows have `available` and the final line is `actions=0`.

- [x] **Step 3: Update docs and evidence**

Record red/green tests, connected probe result, event availability,
`actions=0`, and boundary notes in:

```text
docs/superpowers/evidence/2026-06-28-official-fabric-event-source-metadata.md
docs/project-completion-checklist.md
docs/superpowers/phase-index.md
```

- [x] **Step 4: Run final verification and push**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
mise run fabric-lane-check-latest-official
mise run ci
git diff --check
git status --short --branch
```

Expected: all verification commands pass and the worktree only contains this
phase's intended changes before commit.

Commit and push:

```sh
git add docs/project-completion-checklist.md docs/superpowers/phase-index.md docs/superpowers/specs/2026-06-28-161-official-fabric-event-source-metadata-design.md docs/superpowers/plans/2026-06-28-161-official-fabric-event-source-metadata-plan.md docs/superpowers/evidence/2026-06-28-official-fabric-event-source-metadata.md driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/CraftlessFabricOfficialEntrypoint.kt driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricEventSourceProvider.kt driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt
git commit -m "feat: probe official fabric event sources"
git push origin main
```

## Self-Review

- Spec coverage: the plan adds lane-provided event-source evidence, registers
  official Fabric callbacks, projects shared event graph availability, verifies
  generated OpenAPI, and preserves `actions=0`.
- Placeholder scan: no task contains TBD/TODO/fill-in placeholders.
- Type consistency: `OfficialFabricEventSourceProvider.sourceEvidence()`,
  `MinecraftOfficialFabricEventSources.register()`, and
  `fabricEventGraphFragment(sourceEvidence, available)` are used consistently.
