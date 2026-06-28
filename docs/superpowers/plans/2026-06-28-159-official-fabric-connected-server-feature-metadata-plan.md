# Official Fabric Connected Server Feature Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make official Fabric connected OpenAPI report real lane-provided server-feature metadata instead of stale `server-features:not-connected`.

**Architecture:** Keep the official lane as a narrow compatibility adapter. Add a small server-feature provider that reads official/Mojang-mapped lifecycle facts and inject it into the existing shared `FabricRuntimeMetadataSnapshot` path. Runtime graph and OpenAPI stay generated from shared projection; this phase adds no gameplay operations.

**Tech Stack:** Kotlin/JVM, Fabric Loader, official/Mojang-mapped Minecraft 26.2, Craftless driver-fabric-discovery metadata helpers, Gradle through mise.

---

### Task 1: Add Server-Feature Provider Seam

**Files:**
- Modify: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt`
- Modify: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt`
- Create: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricServerFeatureProvider.kt`

- [x] **Step 1: Write the failing metadata-provider test**

Add this test to `OfficialFabricSharedRuntimeMetadataTest`:

```kotlin
@Test
fun `official runtime metadata uses lane server feature provider`() {
    val provider =
        officialFabricRuntimeMetadataProvider(
            serverFeatureProvider =
                OfficialFabricServerFeatureProvider {
                    listOf("connection:connected", "server:remote", "feature-set:abc123")
                },
        )

    val metadata = provider.runtimeMetadata("official-probe")

    assertTrue(metadata.serverFeatureFingerprint.startsWith("server-features:"))
    assertNotEquals("server-features:not-connected", metadata.serverFeatureFingerprint)
}
```

- [x] **Step 2: Verify the test fails for missing symbols/signature**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official runtime metadata uses lane server feature provider*'
```

Expected: FAIL because `officialFabricRuntimeMetadataProvider` is private or
does not accept `serverFeatureProvider`, and
`OfficialFabricServerFeatureProvider` does not exist.

- [x] **Step 3: Implement the provider seam**

Create `OfficialFabricServerFeatureProvider.kt`:

```kotlin
package com.minekube.craftless.driver.fabric.official

import net.minecraft.client.Minecraft
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal fun interface OfficialFabricServerFeatureProvider {
    fun serverFeatures(): List<String>
}

internal class MinecraftOfficialFabricServerFeatureProvider(
    private val clientProvider: () -> Minecraft = Minecraft::getInstance,
) : OfficialFabricServerFeatureProvider {
    override fun serverFeatures(): List<String> =
        queryOnClient(clientProvider()) {
            val connection = getConnection()
            listOf(
                "connection:${if (connection != null && player != null) "connected" else "disconnected"}",
                "server:${serverKind()}",
                "local-server:$isLocalServer",
                "feature-set:${connection?.enabledFeatures()?.hashCode() ?: "none"}",
            )
        }

    private fun <T> queryOnClient(
        client: Minecraft,
        query: Minecraft.() -> T,
    ): T {
        if (client.isSameThread) {
            return client.query()
        }
        val result = CompletableFuture<T>()
        client.execute {
            runCatching {
                client.query()
            }.onSuccess(result::complete)
                .onFailure(result::completeExceptionally)
        }
        return result.get(CLIENT_QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }

    private fun Minecraft.serverKind(): String =
        when {
            hasSingleplayerServer() -> "singleplayer"
            currentServer?.isLan == true -> "local"
            currentServer != null -> "remote"
            else -> "none"
        }

    private companion object {
        const val CLIENT_QUERY_TIMEOUT_MS = 2_000L
    }
}
```

Update `OfficialFabricDriverBackend.kt` so `officialFabricRuntimeMetadataProvider`
is `internal` and accepts a server-feature provider:

```kotlin
internal fun officialFabricRuntimeMetadataProvider(
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
                registryFingerprint = "registries:not-discovered",
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
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest.official runtime metadata uses lane server feature provider*'
```

Expected: PASS.

### Task 2: Prove Graph/OpenAPI Boundary

**Files:**
- Modify: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt`
- Modify: `docs/superpowers/evidence/2026-06-28-official-fabric-connected-server-feature-metadata.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `AGENTS.md`

- [x] **Step 1: Add backend graph assertion**

Add or extend a test so an `OfficialFabricDriverBackend` created with a
runtime metadata provider from `officialFabricRuntimeMetadataProvider(fake)`
has:

```kotlin
assertEquals(emptyList(), graph.operations)
assertTrue(graph.sourceEvidence.any { it.kind == "server-features" || it.value.startsWith("server-features:") })
assertNotEquals("server-features:not-connected", metadata.serverFeatureFingerprint)
```

- [x] **Step 2: Run focused official tests**

Run:

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Expected: PASS.

- [x] **Step 3: Run connected official attach probe**

Run:

```sh
rm -rf driver-fabric-official/build/craftless-official-attach-probe
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_CONNECT=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=180000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 4: Inspect generated OpenAPI evidence**

Run:

```sh
jq -r '.["x-craftless"]["x-craftless-server-feature-fingerprint"], (.["x-craftless-actions"] | length)' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi-connected.json
```

Expected: first line starts with `server-features:` and is not
`server-features:not-connected`; second line is `0`.

- [x] **Step 5: Update docs and evidence**

Record the focused tests, connected probe result, OpenAPI server-feature
fingerprint, `actions=0`, and boundary notes in:

```text
docs/superpowers/evidence/2026-06-28-official-fabric-connected-server-feature-metadata.md
docs/project-completion-checklist.md
AGENTS.md
```

- [x] **Step 6: Run final verification and push**

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
git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-159-official-fabric-connected-server-feature-metadata-design.md docs/superpowers/plans/2026-06-28-159-official-fabric-connected-server-feature-metadata-plan.md docs/superpowers/evidence/2026-06-28-official-fabric-connected-server-feature-metadata.md driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricServerFeatureProvider.kt driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt
git commit -m "feat: probe official fabric server features"
git push origin main
```

## Self-Review

- Spec coverage: the plan adds a lane provider seam, routes it through shared
  runtime metadata, verifies generated OpenAPI, and preserves `actions=0`.
- Placeholder scan: no task contains TBD/TODO/fill-in placeholders.
- Type consistency: `OfficialFabricServerFeatureProvider.serverFeatures()`,
  `officialFabricRuntimeMetadataProvider(serverFeatureProvider = ...)`, and
  `FabricRuntimeMetadataSnapshot.serverFeatureFingerprint` are used
  consistently.
