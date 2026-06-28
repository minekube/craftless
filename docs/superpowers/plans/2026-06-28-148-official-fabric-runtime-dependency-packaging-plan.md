# Official Fabric Runtime Dependency Packaging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the latest/current official Fabric probe jar carry the shared Craftless runtime dependencies required for metadata-only self-attach.

**Architecture:** Keep `driver-fabric-official` as an internal probe lane, not a packaged support lane. Add nested dependency packaging for shared runtime/attach/Ktor/Kotlin dependencies only, and guard that the official module does not depend on or include the Yarn/remap gameplay driver, daemon, or HMC bridge.

**Tech Stack:** Gradle 9.6, Fabric Loom include jars, Kotlin/JVM 2.4, Ktor 3.5, kotlinx.serialization 1.11, mise-managed Java 25 probe.

---

### Task 1: Add packaging guard tests

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Write the failing architecture test**

Add a test named:

```kotlin
@Test
fun `official lane packages shared attach runtime dependencies without yarn remap gameplay lane`()
```

The test should read `driver-fabric-official/build.gradle.kts` and assert:

- it contains `include(project(":protocol"))`;
- it contains `include(project(":driver-api"))`;
- it contains `include(project(":driver-runtime"))`;
- it contains `include(project(":driver-fabric-attach"))`;
- it does not contain `include(project(":driver-fabric"))`;
- it does not contain `include(project(":daemon"))`;
- it does not contain `include(project(":bridge-hmc"))`;
- it contains Ktor client/server core/CIO include coordinates;
- it contains Kotlin stdlib and kotlinx serialization include coordinates.

- [x] **Step 2: Run the guard red**

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane packages shared attach runtime dependencies without yarn remap gameplay lane'
```

Expected before implementation: FAIL because the official build has no
`include(...)` dependency packaging.

### Task 2: Add official nested runtime dependencies

**Files:**
- Modify: `driver-fabric-official/build.gradle.kts`

- [x] **Step 1: Add project includes**

Add:

```kotlin
include(project(":protocol"))
include(project(":driver-api"))
include(project(":driver-runtime"))
include(project(":driver-fabric-attach"))
```

- [x] **Step 2: Add external runtime includes**

Add the same minimal external runtime coordinates required by the shared attach
path:

```kotlin
include("io.ktor:ktor-client-core-jvm:3.5.0")
include("io.ktor:ktor-client-cio-jvm:3.5.0")
include("io.ktor:ktor-server-core-jvm:3.5.0")
include("io.ktor:ktor-server-cio-jvm:3.5.0")
include("org.jetbrains.kotlin:kotlin-stdlib:2.4.0")
include("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0")
include("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.11.0")
include("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0")
include("org.jetbrains.kotlinx:kotlinx-io-core-jvm:0.9.0")
include("org.jetbrains.kotlinx:kotlinx-io-bytestring-jvm:0.9.0")
include("io.ktor:ktor-http-jvm:3.5.0")
include("io.ktor:ktor-http-cio-jvm:3.5.0")
include("io.ktor:ktor-utils-jvm:3.5.0")
include("io.ktor:ktor-io-jvm:3.5.0")
include("io.ktor:ktor-events-jvm:3.5.0")
include("io.ktor:ktor-websocket-serialization-jvm:3.5.0")
include("io.ktor:ktor-serialization-jvm:3.5.0")
include("io.ktor:ktor-websockets-jvm:3.5.0")
include("io.ktor:ktor-sse-jvm:3.5.0")
include("io.ktor:ktor-network-jvm:3.5.0")
include("io.ktor:ktor-network-tls-jvm:3.5.0")
```

Do not include `driver-fabric`, `daemon`, or `bridge-hmc`.

- [x] **Step 3: Run the guard green**

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane packages shared attach runtime dependencies without yarn remap gameplay lane'
```

Expected: PASS.

### Task 3: Verify the packaged official jar

**Files:**
- Modify: `docs/project-completion-checklist.md`
- Modify: `README.md`
- Create: `docs/superpowers/evidence/2026-06-28-official-fabric-runtime-dependency-packaging.md`

- [x] **Step 1: Build the official jar**

```sh
mise exec -- gradle :driver-fabric-official:compileKotlin :driver-fabric-official:processResources :driver-fabric-official:jar
```

Expected: BUILD SUCCESSFUL.

- [x] **Step 2: Inspect nested jars**

```sh
jar tf driver-fabric-official/build/libs/driver-fabric-official-0.1.0-SNAPSHOT.jar | grep '^META-INF/jars/'
```

Expected: output includes nested `driver-fabric-attach`, `driver-runtime`,
`driver-api`, `protocol`, Kotlin stdlib, Ktor client/server, and serialization
jars.

- [x] **Step 3: Run latest official probe and lint**

```sh
mise run fabric-lane-check-latest-official
mise exec -- gradle lint
git diff --check
```

Expected: probe status contains `status=compiled`; lint and whitespace checks
pass.

- [x] **Step 4: Commit and push**

```sh
git add AGENTS.md README.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-148-official-fabric-runtime-dependency-packaging-design.md docs/superpowers/plans/2026-06-28-148-official-fabric-runtime-dependency-packaging-plan.md docs/superpowers/evidence/2026-06-28-official-fabric-runtime-dependency-packaging.md driver-fabric-official/build.gradle.kts driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
git commit -m "build: package official fabric runtime deps"
git push origin main
```
