# Generic Invocation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dispatch invocations from graph operation metadata through generic client-thread adapters.

**Architecture:** Invocation validates against graph-projected OpenAPI, resolves an operation adapter key, executes on the driver boundary, validates results, and publishes events.

**Tech Stack:** Kotlin/JVM, Ktor Server, Fabric client-thread gateway, Gradle, mise.

---

### Task 1: Adapter Contract

**Files:**
- Create: `driver-api/src/main/kotlin/com/minekube/craftless/driver/api/DriverOperationAdapter.kt`
- Modify: `driver-api/src/main/kotlin/com/minekube/craftless/driver/api/DriverSession.kt`
- Test: `driver-api/src/test/kotlin/com/minekube/craftless/driver/api/DriverOperationAdapterTest.kt`

- [ ] **Step 1: Add failing adapter tests**

Assert adapters are resolved by graph operation adapter keys and do not require driver methods per gameplay action.

- [ ] **Step 2: Implement adapter contract**

Add generic adapter invocation types and keep current `invoke` compatibility during migration.

- [ ] **Step 3: Verify**

Run: `mise exec -- gradle :driver-api:test`

Expected: pass.

### Task 2: Daemon Invocation Uses Graph Metadata

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/LocalSessionApiServer.kt`
- Test: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`

- [ ] **Step 1: Add failing invocation tests**

Assert unavailable graph operations reject before adapter invocation, stale schema rejects, and result schema mismatch returns machine-readable error.

- [ ] **Step 2: Implement graph operation validation**

Validate action id, args, availability, permission, adapter key, and result schema from graph-projected metadata.

- [ ] **Step 3: Verify**

Run: `mise exec -- gradle :daemon:test`

Expected: pass.

### Task 3: Fabric Adapter Migration

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt`
- Test: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverModuleTest.kt`

- [ ] **Step 1: Add failing migration test**

Assert current bootstrap gameplay actions can be invoked through graph adapter metadata.

- [ ] **Step 2: Implement adapter bridge**

Wrap existing client-thread implementations as internal adapters without making their descriptors public catalog entries.

- [ ] **Step 3: Verify**

Run: `mise exec -- gradle :driver-fabric:test`

Expected: pass.
