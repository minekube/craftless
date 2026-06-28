# Launch Attach Environment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pass Craftless attach environment into prepared Minecraft client launches.

**Architecture:** Add a small daemon data class for attach environment, thread it through `WorkspaceClientRuntimeDriverFactory.prepare` and `ClientRuntimeLauncher.launch`, and have `ProcessClientRuntimeLauncher` set process environment variables. `LocalSessionApiServer` supplies the server URL when handling `POST /clients`.

**Tech Stack:** Kotlin/JVM, Ktor server tests, java.lang.ProcessBuilder, Gradle tests through mise.

---

### Task 1: Add Red Recording Launcher Test

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`

- [x] **Step 1: Extend recorded launch state**

  Add an `attachEnvironment` field to `RecordedClientRuntimeLaunch` and assert
  in `server prepares and launches workspace client runtime without injected
  driver factory`:

  ```kotlin
  assertEquals("alice", launch.attachEnvironment?.clientId)
  assertEquals(server.url(""), launch.attachEnvironment?.daemonUrl)
  ```

- [x] **Step 2: Run red test**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.server prepares and launches workspace client runtime without injected driver factory*'
  ```

  Expected: compile failure because attach environment is not part of the
  launcher contract.

### Task 2: Thread Attach Environment Through Launcher Contract

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/LocalSessionApiServer.kt`
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`

- [x] **Step 1: Add data class**

  Add:

  ```kotlin
  data class ClientDriverAttachEnvironment(
      val clientId: String,
      val daemonUrl: String,
  )
  ```

- [x] **Step 2: Extend launcher contract**

  Add `attachEnvironment: ClientDriverAttachEnvironment? = null` to
  `WorkspaceClientRuntimeDriverFactory.prepare` and
  `ClientRuntimeLauncher.launch`, and pass it through to launchers.

- [x] **Step 3: Supply attach environment from server create route**

  In `LocalSessionApiServer`, call `workspaceRuntimeFactory.prepare(...,
  attachEnvironment = ClientDriverAttachEnvironment(request.id, url(""))`.

- [x] **Step 4: Run focused green**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.server prepares and launches workspace client runtime without injected driver factory*'
  ```

### Task 3: Prove Process Environment

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt`

- [x] **Step 1: Add red process launcher assertion**

  In `process client runtime launcher starts prepared command`, make the fake
  Java executable write `$CRAFTLESS_CLIENT_ID` and `$CRAFTLESS_DAEMON_URL` to
  files and pass `ClientDriverAttachEnvironment("alice",
  "http://127.0.0.1:12345")` to `ProcessClientRuntimeLauncher.launch`.

- [x] **Step 2: Run red process test**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.process client runtime launcher starts prepared command*'
  ```

- [x] **Step 3: Set process environment**

  In `ProcessClientRuntimeLauncher.launch`, set those env vars on the
  `ProcessBuilder` when attach environment is present.

- [x] **Step 4: Run focused green**

  ```sh
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.process client runtime launcher starts prepared command*'
  ```

### Task 4: Register Phase 99 And Verify

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-launch-attach-environment.md`

- [x] **Step 1: Register Phase 99**

  Add Phase 99 to `AGENTS.md` and the checklist as attach environment
  plumbing.

- [x] **Step 2: Run local gates**

  ```sh
  git diff --check
  mise exec -- gradle :daemon:test
  mise exec -- gradle :daemon:ktlintCheck :daemon:detekt
  ```

- [x] **Step 3: Record evidence**

  Write red/green and local gate outcomes to
  `docs/superpowers/evidence/2026-06-28-launch-attach-environment.md`.

### Task 5: Commit And Push

**Files:**
- All modified files from Tasks 1-4

- [x] **Step 1: Commit and push**

  ```sh
  git add AGENTS.md daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt daemon/src/main/kotlin/com/minekube/craftless/daemon/LocalSessionApiServer.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-99-launch-attach-environment-design.md docs/superpowers/plans/2026-06-28-99-launch-attach-environment-plan.md docs/superpowers/evidence/2026-06-28-launch-attach-environment.md
  git commit -m "daemon: pass driver attach environment to launches"
  git push origin main
  ```

## Self-Review

- Spec coverage: server-to-factory threading, launcher contract, process
  environment, tests, evidence, gates, and push are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no gameplay action catalog, static descriptor family, Fabric binding,
  scenario shortcut, version-specific API, or completion claim.
