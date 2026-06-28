# Shared Fabric Event Graph Projection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Move non-gameplay Fabric event resource/event projection into shared discovery infrastructure and expose official-lane event-source status through generated OpenAPI evidence.

**Architecture:** `driver-fabric-discovery` owns protocol-level event graph fragments. Fabric lanes still own actual Fabric API callbacks, mixin hooks, source evidence, and version-specific adapters.

**Tech Stack:** Kotlin/JVM, Craftless protocol runtime graph DTOs, Gradle 9.6, mise-managed Java.

---

### Task 1: Add red tests and guards

**Files:**
- Modify: `driver-fabric-discovery/src/test/kotlin/com/minekube/craftless/driver/fabric/discovery/FabricRuntimeGraphTest.kt`
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add shared event projection tests**

Add an available test:

```kotlin
val sourceEvidence = listOf(RuntimeSourceEvidence("event-source", "driver:test"))

val fragment =
    fabricEventGraphFragment(
        sourceEvidence = sourceEvidence,
        available = true,
    )

assertEquals(listOf("event"), fragment.resources.map { it.id })
assertEquals(RuntimeAvailability.available(), fragment.resources.single().availability)
assertEquals(
    listOf("event.action", "event.capability", "event.lifecycle"),
    fragment.events.map { it.id }.sorted(),
)
assertEquals(sourceEvidence, fragment.resources.single().sourceEvidence)
```

Add an unavailable test:

```kotlin
val fragment =
    fabricEventGraphFragment(
        sourceEvidence = emptyList(),
        available = false,
    )

val unavailable = RuntimeAvailability.unavailable("event-source-not-discovered")
assertEquals(unavailable, fragment.resources.single().availability)
assertTrue(fragment.events.all { event -> event.availability == unavailable })
assertTrue(
    fragment.resources
        .single()
        .sourceEvidence
        .any { evidence -> evidence.kind == "event-source" && evidence.fingerprint == "events:not-discovered" },
)
```

- [x] **Step 2: Add architecture guards**

In the official-lane architecture test, read
`driver-fabric-discovery/src/main/kotlin/com/minekube/craftless/driver/fabric/discovery/FabricEventGraph.kt`
and assert:

```kotlin
assertTrue(fabricEventGraph.contains("fabricEventGraphFragment"))
assertTrue(fabricEventGraph.contains("event.lifecycle"))
assertTrue(officialBackend.contains("fabricEventGraphFragment"))
assertFalse(officialBackend.contains("import com.minekube.craftless.protocol.RuntimeCapabilityGraph"))
assertFalse(fabricCapabilityProbe.contains("RuntimeResourceNode(\n                        id = \"event\""))
assertFalse(fabricCapabilityProbe.contains("RuntimeEventNode(\n                        id = \"event.$id\""))
```

- [x] **Step 3: Run red**

Run:

```sh
mise exec -- gradle :driver-fabric-discovery:test :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim'
```

Expected: FAIL because `fabricEventGraphFragment` and
`FabricEventGraph.kt` do not exist yet.

### Task 2: Implement shared event projection

**Files:**
- Create: `driver-fabric-discovery/src/main/kotlin/com/minekube/craftless/driver/fabric/discovery/FabricEventGraph.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt`
- Modify: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt`

- [x] **Step 1: Add shared event graph helper**

Create `FabricEventGraph.kt`:

```kotlin
package com.minekube.craftless.driver.fabric.discovery

import com.minekube.craftless.protocol.RuntimeAvailability
import com.minekube.craftless.protocol.RuntimeEventNode
import com.minekube.craftless.protocol.RuntimeResourceNode
import com.minekube.craftless.protocol.RuntimeSchema
import com.minekube.craftless.protocol.RuntimeSourceEvidence

fun fabricEventGraphFragment(
    sourceEvidence: List<RuntimeSourceEvidence>,
    available: Boolean,
): FabricRuntimeGraphFragment {
    val availability =
        if (available) {
            RuntimeAvailability.available()
        } else {
            RuntimeAvailability.unavailable("event-source-not-discovered")
        }
    val evidence =
        sourceEvidence.ifEmpty {
            listOf(RuntimeSourceEvidence("event-source", "events:not-discovered"))
        }
    return FabricRuntimeGraphFragment(
        resources =
            listOf(
                RuntimeResourceNode(
                    id = "event",
                    availability = availability,
                    sourceEvidence = evidence,
                ),
            ),
        events =
            fabricEventIds.map { eventId ->
                RuntimeEventNode(
                    id = eventId,
                    resource = "event",
                    payload = RuntimeSchema.objectSchema(),
                    availability = availability,
                    sourceEvidence = evidence,
                )
            },
    )
}

private val fabricEventIds =
    listOf(
        "event.lifecycle",
        "event.action",
        "event.capability",
    )
```

- [x] **Step 2: Wire Yarn/remap event-source probe**

In `FabricEventSourceCapabilityProbe.discover`, keep source evidence creation
inside the remap lane:

```kotlin
val eventSourceEvidence =
    listOf(RuntimeSourceEvidence("event-source", "driver:${context.runtimeMetadata.driverVersion}")) +
        FabricEventHooks.sourceEvidence() +
        FabricEventCallbacks.sourceEvidence()
```

Replace direct `RuntimeResourceNode` and `RuntimeEventNode` construction with:

```kotlin
return fabricEventGraphFragment(
    sourceEvidence = eventSourceEvidence,
    available = true,
)
```

Remove now-unused imports and `eventSourceIds`.

- [x] **Step 3: Wire official event-source status**

In `OfficialFabricDriverBackend.runtimeGraph`, add:

```kotlin
fabricEventGraphFragment(
    sourceEvidence = emptyList(),
    available = false,
)
```

to the shared fragment list after `fabricRegistryGraphFragment`.

### Task 3: Verify, document, commit, and push

**Files:**
- Modify: `AGENTS.md`
- Modify: `README.md`
- Modify: `driver-fabric-discovery/AGENTS.md`
- Modify: `driver-fabric-official/AGENTS.md`
- Modify: `driver-fabric/AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-shared-fabric-event-graph-projection.md`

- [x] **Step 1: Run focused tests**

Run:

```sh
mise exec -- gradle :driver-fabric-discovery:test :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim' :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Expected: PASS.

- [x] **Step 2: Run lint and whitespace**

Run:

```sh
mise exec -- gradle lint
git diff --check
```

Expected: PASS.

- [x] **Step 3: Run real official attach probe**

Run:

```sh
rm -rf driver-fabric-official/build/craftless-official-attach-probe
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=120000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
jq -r '"status=" + .status + " client=" + .clientId' \
  driver-fabric-official/build/craftless-official-attach-probe/probe-result.json
jq -r '"actions=" + ((."x-craftless-actions"|length)|tostring) + " resources=" + ((."x-craftless-resources"|length)|tostring) + " events=" + ((."x-craftless-events"|length)|tostring)' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi.json
```

Expected: `status=ATTACHED`, `actions=0`, `resources=3`, and `events=3`.

- [x] **Step 4: Clean generated probe runtime dirs**

Run:

```sh
rm -rf driver-fabric-official/logs driver-fabric-official/run
git status --short --branch
```

Expected: no generated `logs/` or `run/` dirs remain.

- [x] **Step 5: Commit and push**

Run:

```sh
git add .
git commit -m "refactor: share fabric event graph projection"
git push origin main
git status --short --branch
git rev-parse HEAD origin/main
```
