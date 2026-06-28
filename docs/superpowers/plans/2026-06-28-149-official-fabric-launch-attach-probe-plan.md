# Official Fabric Launch Attach Probe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an opt-in diagnostic probe that can launch the latest/current official Fabric client lane with a Craftless daemon attach environment and record attach evidence.

**Architecture:** Keep production mod runtime separate from probe tooling. Put the probe runner in `driver-fabric-official/src/test`, use a Gradle `JavaExec` task with test runtime classpath, and pass the daemon URL/client ID to the official `runClient` child process through environment variables.

**Tech Stack:** Gradle 9.6, Kotlin/JVM test source set, Ktor Client CIO, daemon in-memory API server, Fabric Loom `runClient`, mise-managed Java 25.

---

### Task 1: Add architecture guard

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Write failing guard**

Add a test named:

```kotlin
@Test
fun `official lane has opt in launch attach probe task without packaging support claim`()
```

Assert:

- `driver-fabric-official/build.gradle.kts` contains
  `officialFabricAttachProbe`;
- the task uses `sourceSets.test.get().runtimeClasspath`;
- the task main class is
  `com.minekube.craftless.driver.fabric.official.probe.OfficialFabricAttachProbeKt`;
- the default command contains `java@temurin-25.0.3+9.0.LTS`,
  `gradle@9.6.0`, and `:driver-fabric-official:runClient`;
- the task checks `CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE`;
- no product driver manifest file contains `driver-fabric-official`.

- [x] **Step 2: Run red**

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim'
```

Expected: FAIL before the task exists.

### Task 2: Add probe runner

**Files:**
- Modify: `driver-fabric-official/build.gradle.kts`
- Create: `driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/probe/OfficialFabricAttachProbe.kt`

- [x] **Step 1: Add test dependencies**

Add test dependencies on `:daemon`, Ktor Client CIO, and kotlinx serialization
JSON.

- [x] **Step 2: Implement the runner**

The runner should:

- skip unless `CRAFTLESS_OFFICIAL_FABRIC_ATTACH_PROBE` is true;
- start `LocalSessionApiServer.inMemory(driverFactory = metadata-only fake)`;
- POST a `CreateClientRequest` to `/clients`;
- launch the configured command with `CRAFTLESS_CLIENT_ID` and
  `CRAFTLESS_DAEMON_URL`;
- poll `/events` and `/clients/{id}/openapi.json` until `client.attached` is
  observed or timeout expires;
- write `probe-result.json`, `daemon-events.json`, `client-openapi.json`, and
  `client-command.log` under
  `driver-fabric-official/build/craftless-official-attach-probe/`.

- [x] **Step 3: Add the Gradle task**

Register `officialFabricAttachProbe` as a `JavaExec` task using test runtime
classpath and the probe main class. Default the client command to the mise
Java 25 Gradle `:driver-fabric-official:runClient` command.

### Task 3: Verify and document

**Files:**
- Modify: `docs/project-completion-checklist.md`
- Modify: `README.md`
- Create: `docs/superpowers/evidence/2026-06-28-official-fabric-launch-attach-probe.md`

- [x] **Step 1: Run focused tests**

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane has opt in launch attach probe task without packaging support claim'
mise exec -- gradle :driver-fabric-official:officialFabricAttachProbe
```

Expected: guard passes; probe task skips when opt-in is absent.

- [x] **Step 2: Run compile/lint checks**

```sh
mise exec -- gradle :driver-fabric-official:compileTestKotlin
mise exec -- gradle lint
git diff --check
```

Expected: all pass.

- [x] **Step 3: Commit and push**

```sh
git add AGENTS.md README.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-149-official-fabric-launch-attach-probe-design.md docs/superpowers/plans/2026-06-28-149-official-fabric-launch-attach-probe-plan.md docs/superpowers/evidence/2026-06-28-official-fabric-launch-attach-probe.md driver-fabric-official/build.gradle.kts driver-fabric-official/src/test/kotlin/com/minekube/craftless/driver/fabric/official/probe/OfficialFabricAttachProbe.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt
git commit -m "test: add official fabric attach probe"
git push origin main
```
