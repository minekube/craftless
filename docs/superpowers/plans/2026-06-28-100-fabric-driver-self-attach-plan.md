# Fabric Driver Self-Attach Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a launched Fabric driver expose its live `DriverSession` over a loopback Ktor endpoint and attach itself to the daemon.

**Architecture:** Add driver-fabric transport classes that parse attach environment, host a loopback Ktor server backed by a `DriverSession`, and post the endpoint URL to the daemon attach route. Wire the current-lane Fabric bootstrap to create a `BackendDriverSession` from the real backend and start self-attach asynchronously when environment is present.

**Tech Stack:** Kotlin/JVM, Fabric driver module, Ktor Server CIO, Ktor Client CIO, kotlinx.serialization, Gradle tests through mise.

---

### Task 1: Attach Environment Parsing

**Files:**
- Create: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/FabricDriverAttachEnvironment.kt`
- Test: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/FabricDriverSelfAttachTest.kt`

- [x] **Step 1: Write failing tests**

  Add tests:

  ```kotlin
  @Test
  fun `attach environment is absent unless both daemon url and client id exist`() {
      assertEquals(null, FabricDriverAttachEnvironment.from(emptyMap()))
      assertEquals(null, FabricDriverAttachEnvironment.from(mapOf("CRAFTLESS_CLIENT_ID" to "alice")))
      assertEquals(null, FabricDriverAttachEnvironment.from(mapOf("CRAFTLESS_DAEMON_URL" to "http://127.0.0.1:8080")))
  }

  @Test
  fun `attach environment trims required values`() {
      val environment =
          FabricDriverAttachEnvironment.from(
              mapOf(
                  "CRAFTLESS_CLIENT_ID" to " alice ",
                  "CRAFTLESS_DAEMON_URL" to " http://127.0.0.1:8080/ ",
              ),
          )

      assertEquals("alice", environment?.clientId)
      assertEquals("http://127.0.0.1:8080", environment?.daemonUrl)
  }
  ```

- [x] **Step 2: Run red tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.attach environment*'
  ```

- [x] **Step 3: Implement parser**

  Implement:

  ```kotlin
  internal data class FabricDriverAttachEnvironment(
      val clientId: String,
      val daemonUrl: String,
  ) {
      companion object {
          const val CLIENT_ID = "CRAFTLESS_CLIENT_ID"
          const val DAEMON_URL = "CRAFTLESS_DAEMON_URL"

          fun from(env: Map<String, String> = System.getenv()): FabricDriverAttachEnvironment? {
              val clientId = env[CLIENT_ID]?.trim()?.takeIf { it.isNotBlank() } ?: return null
              val daemonUrl = env[DAEMON_URL]?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() } ?: return null
              return FabricDriverAttachEnvironment(clientId = clientId, daemonUrl = daemonUrl)
          }
      }
  }
  ```

- [x] **Step 4: Run green tests**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.attach environment*'
  ```

### Task 2: Loopback DriverSession Endpoint

**Files:**
- Create: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/FabricDriverLoopbackEndpoint.kt`
- Modify: `driver-fabric/build.gradle.kts`
- Test: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/FabricDriverSelfAttachTest.kt`

- [x] **Step 1: Write failing endpoint contract test**

  Add a fake `DriverSession`, start `FabricDriverLoopbackEndpoint`, and verify
  daemon `HttpDriverSession` can call `snapshot`, `actions`,
  `runtimeMetadata`, `runtimeGraph`, `invoke`, and `events`.

- [x] **Step 2: Run red endpoint test**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.loopback endpoint exposes driver session contract*'
  ```

- [x] **Step 3: Add Ktor server dependencies**

  Add explicit driver-fabric dependencies:

  ```kotlin
  implementation("io.ktor:ktor-server-core-jvm:3.5.0")
  implementation("io.ktor:ktor-server-cio-jvm:3.5.0")
  ```

- [x] **Step 4: Implement loopback endpoint**

  Implement a loopback-only Ktor server that serializes/deserializes the stable
  driver DTOs and delegates every endpoint to the supplied `DriverSession`.

- [x] **Step 5: Run green endpoint test**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.loopback endpoint exposes driver session contract*'
  ```

### Task 3: Supervisor Self-Attach POST

**Files:**
- Create: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/FabricDriverSelfAttach.kt`
- Test: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/FabricDriverSelfAttachTest.kt`

- [x] **Step 1: Write failing self-attach test**

  Start a Ktor supervisor probe with `POST /clients/alice:attach`, call
  `FabricDriverSelfAttach().start(session, environment)`, and assert the probe
  receives a JSON body whose `endpoint` starts with `http://127.0.0.1:`.

- [x] **Step 2: Run red self-attach test**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.self attach posts loopback endpoint to daemon*'
  ```

- [x] **Step 3: Implement self-attach**

  Implement `FabricDriverSelfAttach.start(...)` to start the endpoint and post
  the daemon attach request with Ktor Client CIO. Close the endpoint if attach
  fails.

- [x] **Step 4: Run green self-attach test**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.self attach posts loopback endpoint to daemon*'
  ```

### Task 4: Wire Current Fabric Bootstrap

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCurrentLaneBootstrap.kt`
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [x] **Step 1: Write failing source-level bootstrap test**

  Assert the bootstrap source creates `BackendDriverSession` and calls
  `FabricDriverSelfAttach.startFromEnvironment`.

- [x] **Step 2: Run red bootstrap test**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.current lane bootstrap starts self attach from backend session*'
  ```

- [x] **Step 3: Wire bootstrap**

  After `FabricDriverBackend.install(backend)`, create
  `BackendDriverSession(clientId = environment.clientId, backend = backend)`
  through the self-attach helper and start asynchronously only when attach
  environment exists.

- [x] **Step 4: Run green bootstrap test**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.current lane bootstrap starts self attach from backend session*'
  ```

### Task 5: Register Phase 100 And Verify

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-fabric-driver-self-attach.md`

- [x] **Step 1: Register Phase 100**

  Add Phase 100 as Fabric driver self-attach transport plumbing. Keep the
  checklist explicit that Fabric endpoint startup is no longer open after this
  phase.

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise exec -- gradle :driver-fabric:test :daemon:test
  mise exec -- gradle :driver-fabric:ktlintCheck :driver-fabric:detekt :daemon:ktlintCheck :daemon:detekt
  ```

- [x] **Step 3: Record evidence**

  Write red/green and gate outcomes to
  `docs/superpowers/evidence/2026-06-28-fabric-driver-self-attach.md`.

### Task 6: Commit And Push

**Files:**
- All modified files from Tasks 1-5

- [x] **Step 1: Commit and push**

  ```sh
  git add AGENTS.md driver-fabric/build.gradle.kts driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCurrentLaneBootstrap.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-100-fabric-driver-self-attach-design.md docs/superpowers/plans/2026-06-28-100-fabric-driver-self-attach-plan.md docs/superpowers/evidence/2026-06-28-fabric-driver-self-attach.md
  git commit -m "driver-fabric: self attach launched driver"
  git push origin main
  ```

## Self-Review

- Spec coverage: env parsing, loopback endpoint, daemon attach POST, bootstrap
  wiring, docs, evidence, gates, and push are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no public gameplay action, static descriptor family, CLI gameplay
  catalog, Fabric gameplay binding, scenario shortcut, version support claim,
  or completion claim.
