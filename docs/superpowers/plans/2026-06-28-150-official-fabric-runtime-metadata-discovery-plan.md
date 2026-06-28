# Official Fabric Runtime Metadata Discovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace hard-coded official Fabric probe metadata with live Fabric Loader-derived runtime metadata and prove it through generated OpenAPI evidence.

**Architecture:** Keep the official lane metadata-only for gameplay. Add a small official-lane metadata provider that can read Fabric Loader in production and accept deterministic snapshots in tests; wire `OfficialFabricDriverBackend` to the provider and keep OpenAPI projection through the existing driver/session/daemon graph path.

**Tech Stack:** Kotlin/JVM, Fabric Loader API, Ktor probe harness, Gradle 9.6, mise-managed Java 25.

---

### Task 1: Add guards for non-hard-coded official metadata

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Write failing guard**

Extend the existing official-lane architecture test to read
`driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt`
and assert it does not contain:

```kotlin
"mods:official-lane-probe"
"registries:unavailable"
"server-features:unavailable"
```

Also assert the source contains:

```kotlin
"OfficialFabricRuntimeMetadataProvider"
```

- [x] **Step 2: Run red**

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim'
```

Expected: FAIL because the backend still embeds `mods:official-lane-probe`.

### Task 2: Add official metadata provider tests

**Files:**
- Create: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt`

- [x] **Step 1: Write tests**

Create tests that instantiate a snapshot provider with:

```kotlin
OfficialFabricRuntimeMetadataSnapshot(
    loaderVersion = "0.19.3",
    installedMods = listOf(
        OfficialFabricInstalledMod("minecraft", "26.2"),
        OfficialFabricInstalledMod("fabricloader", "0.19.3"),
        OfficialFabricInstalledMod("fabric-api", "0.153.0+26.2"),
        OfficialFabricInstalledMod("craftless-driver-fabric-official", "0.1.0-SNAPSHOT"),
    ),
)
```

Assert:

- `loaderVersion == "0.19.3"`;
- `driver == "craftless-driver-fabric-official"`;
- `mappings == "craftless-official-bindings-26-2"`;
- `installedModsFingerprint` starts with `mods:`;
- the same snapshot produces the same fingerprint twice;
- reordering the installed mods produces the same fingerprint;
- changing one version changes the fingerprint;
- `registryFingerprint == "registries:not-discovered"`;
- `serverFeatureFingerprint == "server-features:not-connected"`;
- `permissionsFingerprint == "permissions:local-client"`.

- [x] **Step 2: Run red**

```sh
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Expected: FAIL because the provider does not exist.

### Task 3: Implement official metadata provider

**Files:**
- Create: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricRuntimeMetadataProvider.kt`
- Modify: `driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt`

- [x] **Step 1: Add provider types**

Create:

```kotlin
internal fun interface OfficialFabricRuntimeMetadataProvider {
    fun runtimeMetadata(clientId: String): DriverRuntimeMetadata
}

internal data class OfficialFabricInstalledMod(
    val id: String,
    val version: String,
)

internal data class OfficialFabricRuntimeMetadataSnapshot(
    val loaderVersion: String,
    val installedMods: List<OfficialFabricInstalledMod>,
)
```

- [x] **Step 2: Implement snapshot provider**

Add `SnapshotOfficialFabricRuntimeMetadataProvider` that sorts installed mods by
id/version and hashes `id@version` entries into a short `mods:<sha256-prefix>`
fingerprint. Return:

```kotlin
DriverRuntimeMetadata(
    loaderVersion = snapshot.loaderVersion,
    driver = "craftless-driver-fabric-official",
    driverVersion = "0.1.0-SNAPSHOT",
    mappings = "craftless-official-bindings-26-2",
    installedModsFingerprint = fingerprint("mods", entries),
    registryFingerprint = "registries:not-discovered",
    serverFeatureFingerprint = "server-features:not-connected",
    permissionsFingerprint = "permissions:local-client",
)
```

- [x] **Step 3: Implement Fabric Loader provider**

Add `FabricLoaderOfficialRuntimeMetadataProvider` that uses
`FabricLoader.getInstance().allMods` and each mod container's metadata id and
version friendly string to build the snapshot.

- [x] **Step 4: Wire backend**

Change `OfficialFabricDriverBackend` to accept:

```kotlin
private val runtimeMetadataProvider: OfficialFabricRuntimeMetadataProvider =
    FabricLoaderOfficialRuntimeMetadataProvider()
```

and implement `runtimeMetadata(clientId)` by delegating to the provider.

### Task 4: Verify real probe metadata

**Files:**
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/evidence/2026-06-28-official-fabric-launch-attach-probe.md`
- Modify: `README.md`
- Modify: `AGENTS.md`

- [x] **Step 1: Run green tests**

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim'
mise exec -- gradle :driver-fabric-official:test --tests '*OfficialFabricSharedRuntimeMetadataTest*'
```

Expected: both pass.

- [x] **Step 2: Run real enabled probe**

```sh
rm -rf driver-fabric-official/build/craftless-official-attach-probe
CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE=1 \
CRAFTLESS_OFFICIAL_ATTACH_PROBE_TIMEOUT_MS=120000 \
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
jq -r '."x-craftless"."x-craftless-installed-mods-fingerprint"' \
  driver-fabric-official/build/craftless-official-attach-probe/client-openapi.json
```

Expected: probe passes; fingerprint starts with `mods:` and is not
`mods:official-lane-probe`.

- [x] **Step 3: Run lint and whitespace checks**

```sh
mise exec -- gradle lint
git diff --check
```

Expected: both pass.

- [ ] **Step 4: Commit and push**

```sh
git add AGENTS.md README.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-150-official-fabric-runtime-metadata-discovery-design.md docs/superpowers/plans/2026-06-28-150-official-fabric-runtime-metadata-discovery-plan.md docs/superpowers/evidence/2026-06-28-official-fabric-launch-attach-probe.md driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricRuntimeMetadataProvider.kt driver-fabric-official/src/main/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricDriverBackend.kt driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/OfficialFabricSharedRuntimeMetadataTest.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
git commit -m "feat: discover official fabric runtime metadata"
git push origin main
```
