# Shared Fabric Runtime Metadata Discovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Move Fabric Loader runtime metadata discovery out of lane-local driver modules and into a shared Fabric discovery module used by both current Yarn/remap and latest official lanes.

**Architecture:** Add a neutral `driver-fabric-discovery` module that depends only on Fabric Loader and Craftless driver API. Keep lane-specific mapping fingerprints, registry probes, server-feature probes, and gameplay execution inside the lane modules, while sharing loader/mod metadata snapshots and deterministic fingerprinting.

**Tech Stack:** Kotlin/JVM, Fabric Loader API, Gradle 9.6, mise-managed Java, kotlin.test.

---

### Task 1: Add shared-module red guards

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Write failing architecture assertions**

Extend the official-lane/shared-boundary architecture coverage so it asserts:

```kotlin
val settings = Files.readString(root.resolve("settings.gradle.kts"))
val fabricBuild = Files.readString(root.resolve("driver-fabric/build.gradle.kts"))
val officialBuild = Files.readString(root.resolve("driver-fabric-official/build.gradle.kts"))
val fabricBackend = Files.readString(
    root.resolve("driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt"),
)
val officialSources = Files.walk(root.resolve("driver-fabric-official/src/main/kotlin")).use { paths ->
    paths.filter { Files.isRegularFile(it) }
        .filter { it.toString().endsWith(".kt") }
        .joinToString("\n") { Files.readString(it) }
}

assertTrue(settings.contains("\"driver-fabric-discovery\""))
assertTrue(fabricBuild.contains("project(\":driver-fabric-discovery\")"))
assertTrue(officialBuild.contains("project(\":driver-fabric-discovery\")"))
assertFalse(fabricBackend.contains("internal data class FabricRuntimeMetadataSnapshot"))
assertFalse(fabricBackend.contains("internal class SnapshotFabricRuntimeMetadataProvider"))
assertFalse(fabricBackend.contains("private fun FabricLoader.installedMods()"))
assertFalse(officialSources.contains("OfficialFabricRuntimeMetadataProvider"))
assertFalse(officialSources.contains("OfficialFabricRuntimeMetadataSnapshot"))
assertTrue(officialSources.contains("SnapshotFabricRuntimeMetadataProvider"))
assertTrue(officialSources.contains("FabricLoaderRuntimeMetadataReader"))
```

- [x] **Step 2: Run red guard**

Run:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim'
```

Expected: FAIL because the shared module does not exist and both lanes still own local metadata provider types.

### Task 2: Add shared metadata tests

**Files:**
- Create: `driver-fabric-discovery/build.gradle.kts`
- Create: `driver-fabric-discovery/src/test/kotlin/com/minekube/craftless/driver/fabric/discovery/FabricRuntimeMetadataProviderTest.kt`

- [x] **Step 1: Add module shell**

Create `driver-fabric-discovery/build.gradle.kts`:

```kotlin
dependencies {
    api(project(":driver-api"))
    implementation("net.fabricmc:fabric-loader:0.19.3")
}
```

- [x] **Step 2: Write failing shared provider test**

Create a test that builds:

```kotlin
val snapshot =
    FabricRuntimeMetadataSnapshot(
        loaderVersion = "0.19.3",
        driver = "craftless-driver-fabric",
        driverVersion = "0.1.0-SNAPSHOT",
        mappings = "craftless-fabric-bindings-test",
        installedModsFingerprint =
            fabricRuntimeFingerprint(
                "mods",
                listOf("minecraft@26.2", "fabricloader@0.19.3", "fabric-api@0.153.0+26.2"),
            ),
        registryFingerprint = fabricRuntimeFingerprint("registries", listOf("block:minecraft:stone")),
        serverFeatureFingerprint = "server-features:not-connected",
        permissionsFingerprint = "permissions:local-client",
    )
```

Assert that `SnapshotFabricRuntimeMetadataProvider(snapshot).runtimeMetadata("client")`
returns exactly those values.

- [x] **Step 3: Write failing deterministic fingerprint test**

Assert:

```kotlin
val first = fabricRuntimeFingerprint("mods", listOf("b@2", "a@1"))
val reordered = fabricRuntimeFingerprint("mods", listOf("a@1", "b@2"))
val changed = fabricRuntimeFingerprint("mods", listOf("a@1", "b@3"))

assertTrue(first.startsWith("mods:"))
assertEquals(first, reordered)
assertNotEquals(first, changed)
```

- [x] **Step 4: Run red**

Run:

```sh
mise exec -- gradle :driver-fabric-discovery:test
```

Expected: FAIL because the shared source types do not exist.

### Task 3: Implement shared discovery module

**Files:**
- Modify: `settings.gradle.kts`
- Create: `driver-fabric-discovery/AGENTS.md`
- Create: `driver-fabric-discovery/src/main/kotlin/com/minekube/craftless/driver/fabric/discovery/FabricRuntimeMetadataProvider.kt`

- [x] **Step 1: Include module**

Add `"driver-fabric-discovery"` to `settings.gradle.kts` next to the other
Fabric driver modules.

- [x] **Step 2: Add module instructions**

Create `driver-fabric-discovery/AGENTS.md` stating that the module owns shared
Fabric Loader/runtime discovery, must stay gameplay-catalog-free, and must keep
version-specific divergence in narrow lane adapters.

- [x] **Step 3: Implement shared provider**

Create `FabricRuntimeMetadataProvider.kt` with:

```kotlin
package com.minekube.craftless.driver.fabric.discovery

import com.minekube.craftless.driver.api.DriverRuntimeMetadata
import net.fabricmc.loader.api.FabricLoader
import java.security.MessageDigest

fun interface FabricRuntimeMetadataProvider {
    fun runtimeMetadata(clientId: String): DriverRuntimeMetadata
}

data class FabricRuntimeMetadataSnapshot(
    val loaderVersion: String,
    val driver: String,
    val driverVersion: String,
    val mappings: String,
    val installedModsFingerprint: String,
    val registryFingerprint: String,
    val serverFeatureFingerprint: String,
    val permissionsFingerprint: String = "permissions:local-client",
)

class SnapshotFabricRuntimeMetadataProvider(
    private val snapshot: FabricRuntimeMetadataSnapshot,
) : FabricRuntimeMetadataProvider {
    override fun runtimeMetadata(clientId: String): DriverRuntimeMetadata =
        DriverRuntimeMetadata(
            loaderVersion = snapshot.loaderVersion,
            driver = snapshot.driver,
            driverVersion = snapshot.driverVersion,
            mappings = snapshot.mappings,
            installedModsFingerprint = snapshot.installedModsFingerprint,
            registryFingerprint = snapshot.registryFingerprint,
            serverFeatureFingerprint = snapshot.serverFeatureFingerprint,
            permissionsFingerprint = snapshot.permissionsFingerprint,
        )
}

class FabricLoaderRuntimeMetadataReader(
    private val loader: FabricLoader = FabricLoader.getInstance(),
) {
    fun loaderVersion(loaderModId: String = "fabricloader"): String =
        versionFor(loaderModId) ?: "unknown"

    fun driverVersion(
        driverId: String,
        fallback: String,
    ): String = versionFor(driverId) ?: fallback

    fun installedModsFingerprint(): String =
        fabricRuntimeFingerprint("mods", installedModCoordinates())

    fun installedModCoordinates(): List<String> =
        loader.allMods.map { container ->
            "${container.metadata.id}@${container.metadata.version.friendlyString}"
        }

    fun isDevelopmentEnvironment(): Boolean =
        try {
            loader.isDevelopmentEnvironment
        } catch (_: NullPointerException) {
            false
        }

    private fun versionFor(modId: String): String? =
        loader
            .getModContainer(modId)
            .map { container -> container.metadata.version.friendlyString }
            .orElse(null)
}

fun fabricRuntimeFingerprint(
    label: String,
    values: List<String>,
): String {
    require(label.isNotBlank()) { "fingerprint label is required" }
    require(values.isNotEmpty()) { "fingerprint values are required" }
    val digest = MessageDigest.getInstance("SHA-256")
    values.sorted().forEach { value ->
        require(value.isNotBlank()) { "fingerprint value is required" }
        digest.update(value.encodeToByteArray())
        digest.update(0)
    }
    return "$label:" + digest.digest().joinToString("") { byte -> "%02x".format(byte) }.take(16)
}
```

### Task 4: Wire both Fabric lanes to the shared provider

**Files:**
- Modify: `driver-fabric/build.gradle.kts`
- Modify: `driver-fabric-official/build.gradle.kts`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt`
- Delete: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricRuntimeMetadataProvider.kt`
- Modify: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt`
- Modify: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt`
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Add dependencies**

Add `implementation(project(":driver-fabric-discovery"))` and
`include(project(":driver-fabric-discovery"))` to both Fabric driver builds.

- [x] **Step 2: Replace Yarn/remap local provider types**

Import the shared provider types in `FabricDriverBackend.kt`, delete the local
provider interface, snapshot data class, snapshot provider class, Fabric Loader
installed-mod helper, and local fingerprint helper. Build snapshots with
precomputed fingerprints:

```kotlin
FabricRuntimeMetadataSnapshot(
    loaderVersion = reader.loaderVersion(),
    driver = FABRIC_DRIVER_ID,
    driverVersion = reader.driverVersion(FABRIC_DRIVER_ID, FABRIC_DRIVER_VERSION),
    mappings = FabricCompiledLaneMetadata.MAPPINGS_FINGERPRINT,
    installedModsFingerprint = reader.installedModsFingerprint(),
    registryFingerprint = fabricRuntimeFingerprint("registries", safeRuntimeRegistryEntries()),
    serverFeatureFingerprint =
        fabricRuntimeFingerprint(
            "server-features",
            listOf("environment:${if (reader.isDevelopmentEnvironment()) "dev" else "runtime"}") +
                GatewayFabricServerFeatureProvider(gateway).serverFeatures(),
        ),
)
```

- [x] **Step 3: Replace official-only provider**

Delete `OfficialFabricRuntimeMetadataProvider.kt`. In `OfficialFabricDriverBackend.kt`, default to a private factory that uses shared types:

```kotlin
private fun officialFabricRuntimeMetadataProvider(): FabricRuntimeMetadataProvider {
    val reader = FabricLoaderRuntimeMetadataReader()
    return FabricRuntimeMetadataProvider {
        SnapshotFabricRuntimeMetadataProvider(
            FabricRuntimeMetadataSnapshot(
                loaderVersion = reader.loaderVersion(),
                driver = "craftless-driver-fabric-official",
                driverVersion = reader.driverVersion("craftless-driver-fabric-official", "0.1.0-SNAPSHOT"),
                mappings = "craftless-official-bindings-26-2",
                installedModsFingerprint = reader.installedModsFingerprint(),
                registryFingerprint = "registries:not-discovered",
                serverFeatureFingerprint = "server-features:not-connected",
                permissionsFingerprint = "permissions:local-client",
            ),
        ).runtimeMetadata(it)
    }
}
```

- [x] **Step 4: Update tests**

Move provider behavior assertions to the shared module test and keep the
official backend delegation test using shared `FabricRuntimeMetadataProvider`,
`FabricRuntimeMetadataSnapshot`, and `SnapshotFabricRuntimeMetadataProvider`.

### Task 5: Verify, document, commit, and push

**Files:**
- Modify: `AGENTS.md`
- Modify: `driver-fabric/AGENTS.md`
- Modify: `driver-fabric-official/AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-shared-fabric-runtime-metadata-discovery.md`

- [x] **Step 1: Run focused tests**

Run:

```sh
mise exec -- gradle :driver-fabric-discovery:test :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim' :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Expected: PASS.

- [x] **Step 2: Run real official attach probe**

Run:

```sh
rm -rf driver-fabric-official/build/craftless-official-attach-probe
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=120000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
jq -r '."x-craftless"."x-craftless-installed-mods-fingerprint"' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi.json
```

Expected: `ATTACHED`; fingerprint starts with `mods:`.

- [x] **Step 3: Run lint and whitespace**

Run:

```sh
mise exec -- gradle lint
git diff --check
```

Expected: PASS.

- [x] **Step 4: Document evidence**

Record the shared extraction, focused tests, official attach probe, and
non-goals in the checklist and evidence file.

- [x] **Step 5: Commit and push**

Run:

```sh
git status --short --branch
git add .
git commit -m "refactor: share fabric runtime metadata discovery"
git push origin main
```
