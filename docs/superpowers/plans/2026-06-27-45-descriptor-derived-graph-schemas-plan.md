# Descriptor-Derived Graph Schemas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Fabric runtime graph operations derive argument and result schemas from already-discovered Craftless action descriptors.

**Architecture:** Keep the change inside `driver-fabric` graph projection. `FabricCapabilityProbeContext.operation()` resolves one descriptor, maps descriptor argument/result schema models into protocol `RuntimeSchema`, and preserves explicit runtime-only result schemas on operations that already provide them directly.

**Tech Stack:** Kotlin/JVM, Craftless driver API descriptors, Craftless protocol runtime graph models, Gradle through mise.

---

### Task 1: Add Result-Schema Regression Test

**Files:**
- Modify: `driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbeTest.kt`

- [x] **Step 1: Write the failing test**

  Add this test to `FabricCapabilityProbeTest`:

  ```kotlin
  @Test
  fun `fabric graph operations derive result schema from action descriptors`() {
      val graph =
          defaultFabricCapabilityDiscovery()
              .discover(
                  FabricCapabilityProbeContext(
                      clientId = "alice",
                      modeId = "metadata-only",
                      gateway = null,
                  ),
              )

      val raycast = graph.operations.single { it.id == "player.raycast" }

      assertEquals("object", raycast.result.type)
      assertEquals("string", raycast.result.properties["action"]?.type)
      assertEquals(true, raycast.result.properties["action"]?.required)
      assertEquals("string", raycast.result.properties["status"]?.type)
      assertEquals(true, raycast.result.properties["status"]?.required)
      assertEquals("string", raycast.result.properties["message"]?.type)
      assertEquals(false, raycast.result.properties["message"]?.required)
      assertEquals("object", raycast.result.properties["data"]?.type)
      assertEquals(false, raycast.result.properties["data"]?.required)
  }
  ```

- [x] **Step 2: Run the focused test and verify RED**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests 'com.minekube.craftless.driver.fabric.v1_21_6.FabricCapabilityProbeTest.fabric graph operations derive result schema from action descriptors'
  ```

  Expected: failure because `raycast.result.properties["action"]` is null on
  the current empty object result schema.

### Task 2: Map Action Descriptors To Runtime Schemas

**Files:**
- Modify: `driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt`

- [x] **Step 1: Add descriptor imports**

  Add:

  ```kotlin
  import com.minekube.craftless.driver.api.DriverActionArgument
  import com.minekube.craftless.driver.api.DriverActionDescriptor
  import com.minekube.craftless.driver.api.DriverActionResultDescriptor
  import com.minekube.craftless.driver.api.DriverActionResultProperty
  ```

- [x] **Step 2: Resolve the action descriptor once**

  Replace `actionDescriptorArguments(id)` with:

  ```kotlin
  private fun FabricCapabilityProbeContext.actionDescriptor(id: String): DriverActionDescriptor? =
      bindings[id]?.descriptor ?: fabricBootstrapDescriptor(id)
  ```

- [x] **Step 3: Derive arguments and result schemas**

  Change `operation(...)` to:

  ```kotlin
  private fun FabricCapabilityProbeContext.operation(
      id: String,
      resource: String,
      adapter: String,
      availability: RuntimeAvailability,
  ): RuntimeOperationNode {
      val descriptor = actionDescriptor(id)
      return RuntimeOperationNode(
          id = id,
          resource = resource,
          adapter = adapter,
          arguments =
              descriptor
                  ?.arguments
                  ?.mapValues { (_, argument) -> argument.toRuntimeSchema() }
                  .orEmpty(),
          result = descriptor?.result?.toRuntimeSchema() ?: RuntimeSchema.objectSchema(),
          availability = availability,
      )
  }
  ```

- [x] **Step 4: Add schema mapping helpers**

  Add:

  ```kotlin
  private fun DriverActionArgument.toRuntimeSchema(): RuntimeSchema =
      RuntimeSchema(
          type = type,
          required = required,
          properties = properties.mapValues { (_, argument) -> argument.toRuntimeSchema() },
          items = items?.toRuntimeSchema(),
      )

  private fun DriverActionResultDescriptor.toRuntimeSchema(): RuntimeSchema =
      RuntimeSchema(
          type = "object",
          properties =
              properties.mapValues { (name, property) ->
                  property.toRuntimeSchema(required = name in required)
              },
      )

  private fun DriverActionResultProperty.toRuntimeSchema(required: Boolean = false): RuntimeSchema =
      RuntimeSchema(
          type = type,
          required = required,
          properties = properties.mapValues { (_, property) -> property.toRuntimeSchema() },
          items = items?.toRuntimeSchema(),
      )
  ```

- [x] **Step 5: Run the focused test and verify GREEN**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests 'com.minekube.craftless.driver.fabric.v1_21_6.FabricCapabilityProbeTest.fabric graph operations derive result schema from action descriptors'
  ```

  Expected: pass.

### Task 3: Register Phase And Verify

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Register Phase 45 in `AGENTS.md`**

  Add `45. descriptor-derived graph schemas.` to the active phase list and
  note that the phase changes Fabric graph projection only.

- [x] **Step 2: Add checklist evidence**

  Add a Phase 44 checklist section with spec path, plan path, behavior, and
  verification commands.

- [x] **Step 3: Run focused module tests**

  Run:

  ```sh
  mise exec -- gradle :driver-fabric:test --tests '*FabricCapabilityProbeTest*'
  ```

- [x] **Step 4: Run repository quality gates**

  Run:

  ```sh
  git diff --check
  mise run lint
  mise run architecture-check
  mise run ci
  ```

### Task 4: Commit, Push, And Monitor

**Files:**
- Commit the phase files and implementation.

- [ ] **Step 1: Commit and push**

  Run:

  ```sh
  git add AGENTS.md docs/project-completion-checklist.md docs/superpowers/specs/2026-06-27-45-descriptor-derived-graph-schemas-design.md docs/superpowers/plans/2026-06-27-45-descriptor-derived-graph-schemas-plan.md driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbe.kt driver-fabric/src/test/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricCapabilityProbeTest.kt
  git commit -m "driver-fabric: derive graph schemas from descriptors"
  git push origin main
  ```

- [ ] **Step 2: Verify remote CI**

  Run:

  ```sh
  gh run list --repo minekube/craftless --branch main --limit 3
  gh run watch <latest-run-id> --repo minekube/craftless --exit-status
  ```

  Expected: the latest `main` CI run for the pushed commit passes.
