# Backend Runtime Graph Action Default Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `DriverBackend.actions(clientId)` default to graph-derived action descriptors from `runtimeGraph(clientId).operations`.

**Architecture:** Keep `BackendDriverSession.actions()` as a backend delegation point, but make the backend default graph-native instead of empty. Remove Fabric's duplicate override because it already exposes its operations through `runtimeGraph`.

**Tech Stack:** Kotlin/JVM, driver-runtime, driver-api projection helpers, protocol runtime graph DTOs, Gradle through mise.

---

### Task 1: Add Backend Default Projection Test

**Files:**
- Modify: `driver-runtime/src/test/kotlin/com/minekube/craftless/driver/runtime/BackendDriverSessionTest.kt`

- [x] **Step 1: Add the failing test**

Add this test to `BackendDriverSessionTest`:

```kotlin
@Test
fun `driver backend default actions derive from runtime graph operations`() {
    val session =
        BackendDriverSession(
            clientId = "alice",
            backend = GraphOnlyDriverBackend(),
        )

    val action = session.actions().single()

    assertEquals("inventory.query", action.id)
    assertEquals(DriverActionSource.RUNTIME_PROBE, action.source)
    assertEquals(DriverActionAvailability.UNAVAILABLE, action.availability)
    assertEquals("client-not-connected", action.availabilityReason)
    assertEquals("object", action.arguments.getValue("filter").type)
    assertEquals("object", action.result.properties.getValue("data").type)
}
```

Add this fixture near the other private test backends:

```kotlin
private class GraphOnlyDriverBackend : DriverBackend {
    override fun connect(
        clientId: String,
        target: ConnectionTarget,
    ): DriverBackendResult = DriverBackendResult(DriverBackendAction.CONNECT)

    override fun runtimeGraph(clientId: String): RuntimeCapabilityGraph =
        RuntimeCapabilityGraph(
            clientId = clientId,
            resources = listOf(RuntimeResourceNode("inventory", RuntimeAvailability.unavailable("client-not-connected"))),
            operations =
                listOf(
                    RuntimeOperationNode(
                        id = "inventory.query",
                        resource = "inventory",
                        adapter = "test.inventory-query",
                        arguments =
                            mapOf(
                                "filter" to
                                    RuntimeSchema(
                                        type = "object",
                                        required = true,
                                        properties = mapOf("item" to RuntimeSchema("string")),
                                    ),
                            ),
                        result =
                            RuntimeSchema(
                                type = "object",
                                properties = mapOf("items" to RuntimeSchema(type = "array", items = RuntimeSchema("object"))),
                            ),
                        availability = RuntimeAvailability.unavailable("client-not-connected"),
                    ),
                ),
        )

    override fun stop(clientId: String): DriverBackendResult = DriverBackendResult(DriverBackendAction.STOP)
}
```

- [x] **Step 2: Run the red test**

Run:

```sh
mise exec -- gradle :driver-runtime:test --tests '*BackendDriverSessionTest.driver backend default actions derive from runtime graph operations*'
```

Expected: FAIL because `session.actions()` is empty.

### Task 2: Make DriverBackend Default Graph-Native

**Files:**
- Modify: `driver-runtime/src/main/kotlin/com/minekube/craftless/driver/runtime/BackendDriverSession.kt`
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt`

- [x] **Step 1: Change the backend default**

In `BackendDriverSession.kt`, import:

```kotlin
import com.minekube.craftless.driver.api.toDriverActionDescriptor
```

Replace:

```kotlin
fun actions(clientId: String): List<DriverActionDescriptor> = emptyList()
```

with:

```kotlin
fun actions(clientId: String): List<DriverActionDescriptor> =
    runtimeGraph(clientId)
        .operations
        .sortedBy { operation -> operation.id }
        .map { operation -> operation.toDriverActionDescriptor() }
```

- [x] **Step 2: Remove Fabric's duplicate override**

In `FabricDriverBackend.kt`, delete:

```kotlin
override fun actions(clientId: String): List<DriverActionDescriptor> =
    runtimeGraph(clientId).operations.sortedBy { operation -> operation.id }.map { operation -> operation.toDriverActionDescriptor() }
```

Remove the now-unused imports:

```kotlin
import com.minekube.craftless.driver.api.DriverActionDescriptor
import com.minekube.craftless.driver.api.toDriverActionDescriptor
```

- [x] **Step 3: Run green focused tests**

Run:

```sh
mise exec -- gradle :driver-runtime:test --tests '*BackendDriverSessionTest.driver backend default actions derive from runtime graph operations*'
mise exec -- gradle :driver-runtime:test :driver-fabric:test
```

Expected: PASS.

### Task 3: Evidence, Checklist, And Push

**Files:**
- Create: `docs/superpowers/evidence/2026-06-28-backend-runtime-graph-action-default.md`
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/superpowers/phase-index.md`
- Modify: `docs/superpowers/plans/2026-06-28-167-backend-runtime-graph-action-default-plan.md`

- [x] **Step 1: Record evidence and checklist status**

Record the red failure, focused green checks, CI, boundary notes, and duplicate
override removal in:

```text
docs/superpowers/evidence/2026-06-28-backend-runtime-graph-action-default.md
docs/project-completion-checklist.md
docs/superpowers/phase-index.md
```

- [x] **Step 2: Run final verification**

Run:

```sh
mise exec -- gradle :driver-runtime:test :driver-fabric:test
mise run ci
git diff --check
git status --short --branch
```

Expected: all commands pass and the worktree only contains Phase 167 files.

- [x] **Step 3: Commit and push**

Run:

```sh
git add driver-runtime/src/main/kotlin/com/minekube/craftless/driver/runtime/BackendDriverSession.kt driver-runtime/src/test/kotlin/com/minekube/craftless/driver/runtime/BackendDriverSessionTest.kt driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt docs/project-completion-checklist.md docs/superpowers/phase-index.md docs/superpowers/specs/2026-06-28-167-backend-runtime-graph-action-default-design.md docs/superpowers/plans/2026-06-28-167-backend-runtime-graph-action-default-plan.md docs/superpowers/evidence/2026-06-28-backend-runtime-graph-action-default.md
git commit -m "refactor: default backend actions to runtime graph"
git push origin main
```

## Self-Review

- Spec coverage: tasks add a failing backend-runtime test, implement the shared default, remove Fabric's duplicate override, update evidence/checklist/phase index, and run focused plus full verification.
- Placeholder scan: no task uses TBD/TODO/fill-in wording.
- Type consistency: helper names, DTOs, and command names match current source.
