# Latest Driver Lane Preflight Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fail missing packaged latest/current Fabric driver lanes before heavy binary cache downloads.

**Architecture:** Add an early resolved driver-mod request path to cache preparation, use it from the workspace runtime factory before full cache preparation, and verify through a daemon route test that records binary fetches. Keep all version behavior as resolver data rather than static per-version route or action code.

**Tech Stack:** Kotlin, Gradle, Ktor, kotlinx.serialization, mise.

---

### Task 1: Add the failing route test

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`

- [x] **Step 1: Add a recording metadata fetcher**

Create `RecordingServerStaticCacheMetadataFetcher` next to the existing server
test fetcher. It records every `fetchBytes` URL in `binaryFetches` while still
serving deterministic test responses.

- [x] **Step 2: Add latest-release 26.2 fixture metadata**

Create `preparedRuntimeMetadataFetcherWithLatestRelease26()` with:

- Mojang latest release `26.2`;
- Java component `java-runtime-epsilon`;
- Java major version `25`;
- Fabric Loader `0.19.3`;
- Fabric API `0.153.0+26.2`.

- [x] **Step 3: Add the regression test**

Add:

```kotlin
@Test
fun `client creation rejects missing latest fabric driver lane before binary downloads`()
```

The test should create a driver-mod manifest with only a `1.21.6` Fabric entry,
post `/clients` with `version=latest-release`, and assert:

- HTTP 400;
- the body contains `26.2 0.19.3`;
- the body contains `fabricApiVersion=0.153.0+26.2`;
- the body contains `javaMajorVersion=25`;
- `launcher.launches` is empty;
- `metadataFetcher.binaryFetches` is empty;
- `cache/assets/objects` does not exist.

- [x] **Step 4: Run the test red**

Run:

```sh
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.client creation rejects missing latest fabric driver lane before binary downloads'
```

Expected before implementation: failure because binary fetches occurred before
the missing driver lane was rejected.

### Task 2: Implement the preflight

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt`

- [x] **Step 1: Add resolved driver-mod request helper**

Add `CachePreparationService.resolveClientRuntimeDriverModRequest(request)`.
It must resolve the Minecraft alias, version manifest, Fabric metadata, Fabric
API version, and Java major version, then return `ClientRuntimeDriverModRequest`.

- [x] **Step 2: Preserve Fabric API artifact identity**

Carry the selected Fabric API artifact version alongside its source URL in the
internal Fabric mod artifact type.

- [x] **Step 3: Check the driver manifest before full preparation**

In `WorkspaceClientRuntimeDriverFactory.prepare`, resolve the preferred loader
version, call `resolveClientRuntimeDriverModRequest`, and call
`driverModProvider.modFor(driverModRequest)` before `cachePreparationService.prepare`.

- [x] **Step 4: Run the test green**

Run:

```sh
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.client creation rejects missing latest fabric driver lane before binary downloads'
```

Expected after implementation: pass.

### Task 3: Verify neighboring behavior and docs

**Files:**
- Modify: `AGENTS.md`
- Modify: `daemon/AGENTS.md`
- Modify: `driver-api/AGENTS.md`
- Modify: `driver-runtime/AGENTS.md`
- Modify: `driver-fabric/AGENTS.md`
- Modify: `protocol/AGENTS.md`
- Modify: `cli/AGENTS.md`
- Modify: `README.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-latest-driver-lane-preflight.md`

- [x] **Step 1: Run focused regression coverage**

Run:

```sh
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.client creation rejects missing latest fabric driver lane before binary downloads' --tests '*LocalSessionApiServerTest.prepared runtime selects packaged older fabric lane from manifest' --tests '*ConfiguredClientRuntimeDriverModProviderTest*'
```

- [x] **Step 2: Run whitespace verification**

Run:

```sh
git diff --check
```

- [ ] **Step 3: Commit and push**

Run:

```sh
git add AGENTS.md README.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-144-latest-driver-lane-preflight-design.md docs/superpowers/plans/2026-06-28-144-latest-driver-lane-preflight-plan.md docs/superpowers/evidence/2026-06-28-latest-driver-lane-preflight.md daemon/AGENTS.md driver-api/AGENTS.md driver-runtime/AGENTS.md driver-fabric/AGENTS.md protocol/AGENTS.md cli/AGENTS.md daemon/src/main/kotlin/com/minekube/craftless/daemon/CachePreparationService.kt daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt
git commit -m "fix: preflight latest driver lane selection"
git push origin main
```
