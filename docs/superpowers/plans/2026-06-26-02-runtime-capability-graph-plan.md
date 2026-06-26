# Runtime Capability Graph Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the protocol/domain graph that becomes the source for per-client gameplay API generation.

**Architecture:** Add immutable Kotlin models in `protocol/`, tests for validation/fingerprints, and adapter functions that can represent current bootstrap actions as graph nodes without making them the durable catalog.

**Tech Stack:** Kotlin/JVM, kotlinx.serialization, Gradle, JUnit/Kotlin test, mise.

---

### Task 1: Graph Model

**Files:**
- Create: `protocol/src/main/kotlin/com/minekube/craftless/protocol/RuntimeCapabilityGraph.kt`
- Test: `protocol/src/test/kotlin/com/minekube/craftless/protocol/RuntimeCapabilityGraphTest.kt`

- [ ] **Step 1: Write failing tests for graph validation**

Test invalid duplicate resource ids, duplicate operation ids, invalid public ids, unavailable nodes without reasons, and raw namespace leakage.

- [ ] **Step 2: Run failing test**

Run: `mise exec -- gradle :protocol:test --tests com.minekube.craftless.protocol.RuntimeCapabilityGraphTest`

Expected: fails because graph types do not exist.

- [ ] **Step 3: Implement graph data classes**

Add `RuntimeCapabilityGraph`, `RuntimeResourceNode`, `RuntimeOperationNode`, `RuntimeEventNode`, `RuntimeHandleNode`, `RuntimeSchema`, `RuntimeAvailability`, and `RuntimeSourceEvidence`.

- [ ] **Step 4: Verify**

Run: `mise exec -- gradle :protocol:test --tests com.minekube.craftless.protocol.RuntimeCapabilityGraphTest`

Expected: pass.

### Task 2: Fingerprint Model

**Files:**
- Modify: `protocol/src/main/kotlin/com/minekube/craftless/protocol/RuntimeCapabilityGraph.kt`
- Test: `protocol/src/test/kotlin/com/minekube/craftless/protocol/RuntimeCapabilityGraphTest.kt`

- [ ] **Step 1: Add failing fingerprint tests**

Assert stable fingerprint ordering and changed fingerprints for schema, availability, permission, and operation changes.

- [ ] **Step 2: Implement deterministic fingerprint**

Use sorted graph node ids and schema values to produce a deterministic SHA-256-derived fingerprint string.

- [ ] **Step 3: Verify**

Run: `mise exec -- gradle :protocol:test`

Expected: pass.

### Task 3: Bootstrap Compatibility Input

**Files:**
- Create: `driver-api/src/main/kotlin/com/minekube/craftless/driver/api/DriverRuntimeGraph.kt`
- Modify: `driver-api/src/main/kotlin/com/minekube/craftless/driver/api/DriverSession.kt`
- Test: `driver-api/src/test/kotlin/com/minekube/craftless/driver/api/DriverRuntimeGraphTest.kt`

- [ ] **Step 1: Add `runtimeGraph()` as driver capability**

Add a defaultable driver contract for graph snapshots while preserving current `actions()` during transition.

- [ ] **Step 2: Verify driver tests**

Run: `mise exec -- gradle :driver-api:test`

Expected: pass.
