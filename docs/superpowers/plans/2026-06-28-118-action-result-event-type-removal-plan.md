# Action Result Event Type Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove static event-type classification from `DriverActionResult` and make accepted action events operation-id-owned.

**Architecture:** `DriverActionResult` remains a generic action outcome DTO with action id, status, message, and data. Daemon action invocation paths create `SessionEvent` values using the requested `operationId`; backend session event recording keeps lifecycle and error events only.

**Tech Stack:** Kotlin/JVM driver-api, driver-runtime, daemon, testkit, Fabric driver tests; Gradle through mise.

---

### Task 1: Add Red Driver API Contract Guard

**Files:**
- Modify: `driver-api/src/test/kotlin/com/minekube/craftless/driver/api/DriverSessionContractTest.kt`

- [x] **Step 1: Add contract test**

  Add a test named
  `driver action results do not carry static event type metadata`.

  ```kotlin
  val fieldNames =
      DriverActionResult::class.java
          .declaredFields
          .filterNot { field -> field.isSynthetic }
          .map { field -> field.name }
          .toSet()

  assertTrue("eventType" !in fieldNames, fieldNames.toString())
  ```

- [x] **Step 2: Run red test**

  ```sh
  mise exec -- gradle :driver-api:test --tests '*DriverSessionContractTest.driver action results do not carry static event type metadata*'
  ```

  Expected: fails before implementation because `DriverActionResult` still has
  an `eventType` field.

### Task 2: Remove Result Event Type From Production Code

**Files:**
- Modify: `driver-api/src/main/kotlin/com/minekube/craftless/driver/api/DriverSession.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/LocalSessionApiServer.kt`
- Modify: `driver-runtime/src/main/kotlin/com/minekube/craftless/driver/runtime/BackendDriverSession.kt`
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/FakeDriverSession.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt`

- [x] **Step 1: Remove DTO field**

  Delete `eventType: DriverEventType? = null` from `DriverActionResult`.

- [x] **Step 2: Use operation id for daemon action session events**

  In `DriverActionResult.toSessionEvent`, set accepted event `type` to
  `operationId`.

- [x] **Step 3: Stop backend accepted action event synthesis**

  In `BackendDriverSession.toDriverEvent`, return an error event only for
  non-accepted statuses with a message. Return `null` for accepted action
  results.

- [x] **Step 4: Remove named eventType arguments**

  Remove `eventType = ...` from fake driver and Fabric action result
  construction.

### Task 3: Update Tests For Operation-Owned Events

**Files:**
- Modify: `driver-runtime/src/test/kotlin/com/minekube/craftless/driver/runtime/BackendDriverSessionTest.kt`
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/FabricDriverSelfAttachTest.kt`

- [x] **Step 1: Update backend session tests**

  Remove `resultEventType` fixture plumbing and assert accepted backend action
  results do not add `CHAT`/`MOVEMENT` driver events.

- [x] **Step 2: Update daemon test fixtures**

  Remove `eventType = ...` from fixture `DriverActionResult` values. Existing
  SSE assertions should still pass because daemon events use operation ids.

- [x] **Step 3: Update self-attach test fixture**

  Remove `eventType = DriverEventType.CHAT` from the fake remote invoke result.

### Task 4: Run Focused Green Tests

- [x] **Step 1: Driver API guard**

  ```sh
  mise exec -- gradle :driver-api:test --tests '*DriverSessionContractTest.driver action results do not carry static event type metadata*'
  ```

- [x] **Step 2: Runtime and daemon regressions**

  ```sh
  mise exec -- gradle :driver-runtime:test --tests '*BackendDriverSessionTest.*'
  mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.server streams generic graph invocation results without legacy event metadata*' --tests '*LocalSessionApiServerTest.server dispatches graph operations through registered operation adapters*' --tests '*LocalSessionApiServerTest.server streams filtered live client events as sse*'
  ```

- [x] **Step 3: Fabric compile/test smoke**

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.*'
  ```

### Task 5: Update Governance And Evidence

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-06-28-action-result-event-type-removal.md`

- [x] **Step 1: Add Phase 118 to AGENTS**
- [x] **Step 2: Add Phase 118 checklist section**
- [x] **Step 3: Record red/green and local gate evidence**

### Task 6: Verify, Commit, Push

- [x] **Step 1: Run local gates**

  ```sh
  git diff --check
  mise run ci
  ```

- [x] **Step 2: Commit and push**

  ```sh
  git add AGENTS.md driver-api/src/main/kotlin/com/minekube/craftless/driver/api/DriverSession.kt driver-api/src/test/kotlin/com/minekube/craftless/driver/api/DriverSessionContractTest.kt daemon/src/main/kotlin/com/minekube/craftless/daemon/LocalSessionApiServer.kt daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt driver-runtime/src/main/kotlin/com/minekube/craftless/driver/runtime/BackendDriverSession.kt driver-runtime/src/test/kotlin/com/minekube/craftless/driver/runtime/BackendDriverSessionTest.kt testkit/src/main/kotlin/com/minekube/craftless/testkit/FakeDriverSession.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/FabricDriverSelfAttachTest.kt docs/project-completion-checklist.md docs/superpowers/specs/2026-06-28-118-action-result-event-type-removal-design.md docs/superpowers/plans/2026-06-28-118-action-result-event-type-removal-plan.md docs/superpowers/evidence/2026-06-28-action-result-event-type-removal.md
  git commit -m "fix: remove action result event type metadata"
  git push origin main
  ```

## Self-Review

- Spec coverage: DTO removal, operation-id-owned events, backend accepted
  action event behavior, governance, and verification are covered.
- Placeholder scan: no TODO/TBD placeholders.
- Scope: no new gameplay action, route family, CLI gameplay catalog, Fabric
  binding, scenario shortcut, version support claim, or replacement event enum.
