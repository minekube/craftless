# Official Fabric Registry Metadata Probe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make official Fabric connected OpenAPI report discovered registry metadata and available shared registry resources without adding gameplay operations.

**Architecture:** Add a small official-lane registry provider that reads official/Mojang-mapped `BuiltInRegistries` keys and feeds entries into the shared `FabricRuntimeMetadataSnapshot` fingerprint path. `OfficialFabricDriverBackend.runtimeGraph(...)` continues composing the shared registry graph and marks it available only when the metadata fingerprint is discovered. Generated actions remain empty.

**Tech Stack:** Kotlin/JVM, Fabric Loader, official/Mojang-mapped Minecraft 26.2, Craftless driver-fabric-discovery metadata and registry graph helpers, Gradle through mise.

---

### Task 1: Add Official Registry Provider Seam

**Files:**
- Modify: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt`
- Modify: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt`
- Create: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricRegistryProvider.kt`

- [x] **Step 1: Write the failing metadata-provider test**

Add this test to `OfficialFabricSharedRuntimeMetadataTest`:

```kotlin
@Test
fun `official runtime metadata uses lane registry provider`() {
    val provider =
        officialFabricRuntimeMetadataProvider(
            registryProvider =
                OfficialFabricRegistryProvider {
                    listOf("block:minecraft:stone", "item:minecraft:stick")
                },
            serverFeatureProvider =
                OfficialFabricServerFeatureProvider {
                    listOf("connection:connected", "server:remote", "feature-set:abc123")
                },
        )

    val metadata = provider.runtimeMetadata("official-probe")

    assertTrue(metadata.registryFingerprint.startsWith("registries:"))
    assertNotEquals("registries:not-discovered", metadata.registryFingerprint)
}
```

- [x] **Step 2: Verify the test fails for missing symbols/signature**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official runtime metadata uses lane registry provider*'
```

Expected: FAIL because `officialFabricRuntimeMetadataProvider` does not accept
`registryProvider`, and `OfficialFabricRegistryProvider` does not exist.

- [x] **Step 3: Implement the provider seam**

Create `OfficialFabricRegistryProvider.kt`:

```kotlin
package com.minekube.craftless.driver.fabric.official

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries

internal fun interface OfficialFabricRegistryProvider {
    fun registryEntries(): List<String>
}

internal class MinecraftOfficialFabricRegistryProvider : OfficialFabricRegistryProvider {
    override fun registryEntries(): List<String> =
        try {
            listOf(
                registryEntries("block", BuiltInRegistries.BLOCK),
                registryEntries("item", BuiltInRegistries.ITEM),
                registryEntries("entity-type", BuiltInRegistries.ENTITY_TYPE),
                registryEntries("screen-handler", BuiltInRegistries.MENU),
                registryEntries("status-effect", BuiltInRegistries.MOB_EFFECT),
                registryEntries("game-event", BuiltInRegistries.GAME_EVENT),
            ).flatten()
        } catch (_: IllegalArgumentException) {
            listOf("registry:unavailable-unbootstrapped")
        } catch (_: IllegalStateException) {
            listOf("registry:unavailable-unbootstrapped")
        } catch (_: ExceptionInInitializerError) {
            listOf("registry:unavailable-unbootstrapped")
        } catch (_: NoClassDefFoundError) {
            listOf("registry:unavailable-unbootstrapped")
        }

    private fun registryEntries(
        label: String,
        registry: Registry<*>,
    ): List<String> = registry.keySet().map { id -> "$label:$id" }
}
```

Update `OfficialFabricDriverBackend.kt`:

```kotlin
internal fun officialFabricRuntimeMetadataProvider(
    registryProvider: OfficialFabricRegistryProvider = MinecraftOfficialFabricRegistryProvider(),
    serverFeatureProvider: OfficialFabricServerFeatureProvider = MinecraftOfficialFabricServerFeatureProvider(),
): FabricRuntimeMetadataProvider {
    val reader = FabricLoaderRuntimeMetadataReader()
    return FabricRuntimeMetadataProvider { clientId ->
        SnapshotFabricRuntimeMetadataProvider(
            FabricRuntimeMetadataSnapshot(
                loaderVersion = reader.loaderVersion(),
                driver = OFFICIAL_FABRIC_DRIVER_ID,
                driverVersion = reader.driverVersion(OFFICIAL_FABRIC_DRIVER_ID, OFFICIAL_FABRIC_DRIVER_VERSION),
                mappings = OFFICIAL_FABRIC_MAPPINGS_FINGERPRINT,
                installedModsFingerprint = reader.installedModsFingerprint(),
                registryFingerprint = fabricRuntimeFingerprint("registries", registryProvider.registryEntries()),
                serverFeatureFingerprint =
                    fabricRuntimeFingerprint(
                        "server-features",
                        listOf("environment:${if (reader.isDevelopmentEnvironment()) "dev" else "runtime"}") +
                            serverFeatureProvider.serverFeatures(),
                    ),
                permissionsFingerprint = "permissions:local-client",
            ),
        ).runtimeMetadata(clientId)
    }
}
```

- [x] **Step 4: Verify focused test passes**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official runtime metadata uses lane registry provider*'
```

Expected: PASS.

### Task 2: Project Registry Availability Through Shared Graph

**Files:**
- Modify: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt`
- Modify: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt`
- Create: `docs/superpowers/evidence/2026-06-28-official-fabric-registry-metadata-probe.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/phase-index.md`

- [x] **Step 1: Add backend graph assertion**

Extend `official backend projects client state from lane provider without adding
operations` so the backend is created with:

```kotlin
val runtimeMetadataProvider =
    officialFabricRuntimeMetadataProvider(
        registryProvider =
            OfficialFabricRegistryProvider {
                listOf("block:minecraft:stone", "item:minecraft:stick")
            },
        serverFeatureProvider =
            OfficialFabricServerFeatureProvider {
                listOf("connection:connected", "server:remote", "feature-set:abc123")
            },
    )
```

Add assertions:

```kotlin
assertEquals(RuntimeAvailability.available(), resources.getValue("registry").availability)
assertEquals(RuntimeAvailability.available(), handles.getValue("registry.block").availability)
assertEquals(RuntimeAvailability.available(), handles.getValue("registry.item").availability)
assertEquals(metadata.registryFingerprint, runtimeEvidence.getValue("registry").fingerprint)
assertNotEquals("registries:not-discovered", metadata.registryFingerprint)
```

- [x] **Step 2: Verify the graph assertion fails before graph availability wiring**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official backend projects client state from lane provider without adding operations*'
```

Expected: FAIL because `fabricRegistryGraphFragment` is still called with
`available = false`.

- [x] **Step 3: Wire registry availability from runtime metadata**

In `OfficialFabricDriverBackend.runtimeGraph(...)`, call:

```kotlin
fabricRegistryGraphFragment(
    metadata = metadata,
    available = metadata.registryFingerprint != REGISTRIES_NOT_DISCOVERED,
)
```

Add:

```kotlin
private const val REGISTRIES_NOT_DISCOVERED = "registries:not-discovered"
```

- [x] **Step 4: Verify focused official tests pass**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Expected: PASS.

- [x] **Step 5: Run connected official attach probe**

Run:

```sh
rm -rf driver-fabric-official/build/craftless-official-attach-probe
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_CONNECT=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=180000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 6: Inspect generated OpenAPI evidence**

Run:

```sh
jq -r '."x-craftless"."x-craftless-registry-fingerprint", (."x-craftless-resources"[] | select(.id=="registry") | .availability), (."x-craftless-actions" | length)' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi-connected.json
```

Expected: first line starts with `registries:` and is not
`registries:not-discovered`; second line is `available`; third line is `0`.

- [x] **Step 7: Update docs and evidence**

Record the focused red/green tests, connected probe result, OpenAPI registry
fingerprint, registry availability, `actions=0`, and boundary notes in:

```text
docs/superpowers/evidence/2026-06-28-official-fabric-registry-metadata-probe.md
docs/project-completion-checklist.md
docs/superpowers/phase-index.md
```

- [x] **Step 8: Run final verification and push**

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
git add docs/project-completion-checklist.md docs/superpowers/phase-index.md docs/superpowers/specs/2026-06-28-160-official-fabric-registry-metadata-probe-design.md docs/superpowers/plans/2026-06-28-160-official-fabric-registry-metadata-probe-plan.md docs/superpowers/evidence/2026-06-28-official-fabric-registry-metadata-probe.md driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricRegistryProvider.kt driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt
git commit -m "feat: probe official fabric registries"
git push origin main
```

## Self-Review

- Spec coverage: the plan adds a lane registry provider, routes it through
  shared metadata, marks shared registry graph availability, verifies generated
  OpenAPI, and preserves `actions=0`.
- Placeholder scan: no task contains TBD/TODO/fill-in placeholders.
- Type consistency: `OfficialFabricRegistryProvider.registryEntries()`,
  `officialFabricRuntimeMetadataProvider(registryProvider = ...)`, and
  `FabricRuntimeMetadataSnapshot.registryFingerprint` are used consistently.
