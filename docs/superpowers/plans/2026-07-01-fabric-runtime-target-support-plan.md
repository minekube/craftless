# Fabric Runtime Target Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Fabric support-target discovery expose loader/runtime support rows and make the packaged matrix proof consume those rows directly.

**Architecture:** Extend the protocol DTO and OpenAPI schema with `runtimeTargets`, populate it from the configured driver mod manifest in the daemon version discovery service, and update the packaged matrix runner to validate and probe supported runtime rows. Keep unsupported targets machine-readable with `NO_DRIVER_MOD`.

**Tech Stack:** Kotlin/JVM, kotlinx.serialization, Ktor daemon tests, Bun distribution tests, Bash matrix runner.

---

### Task 1: Protocol And Daemon Runtime Target Contract

**Files:**
- Modify: `protocol/src/main/kotlin/com/minekube/craftless/protocol/CacheModels.kt`
- Modify: `protocol/src/main/kotlin/com/minekube/craftless/protocol/OpenApiDocument.kt`
- Modify: `protocol/src/main/kotlin/com/minekube/craftless/protocol/ApiRoute.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/VersionDiscoveryService.kt`
- Test: `protocol/src/test/kotlin/com/minekube/craftless/protocol/OpenApiGenerationTest.kt`
- Test: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`

- [x] **Step 1: Add failing schema and route tests**

Assert that `/versions/support-targets` OpenAPI and HTTP responses include
`runtimeTargets` with loader, loader version, Java major version, support
status, driver mod, and unsupported reason.

- [x] **Step 2: Add DTO and OpenAPI schema**

Add `FabricSupportRuntimeTargetDescriptor` and a `runtimeTargets` array on
`FabricSupportTargetDescriptor`.

- [x] **Step 3: Populate runtime target rows**

Map matching Fabric driver mod entries to supported runtime target rows. When
no driver mod exists, emit one unsupported runtime target with `NO_DRIVER_MOD`.

### Task 2: Matrix Runner Alignment

**Files:**
- Modify: `scripts/packaged-fabric-supported-matrix-probe.sh`
- Test: `playwright/src/distribution.test.ts`

- [x] **Step 1: Add failing distribution guard**

Require the matrix runner to read `target.runtimeTargets`.

- [x] **Step 2: Generate probe jobs from runtime targets**

Validate supported runtime target rows against packaged `driver-mods.json` and
write `probe-jobs.json` entries containing the source `runtimeTarget`.

### Task 3: Verification And Evidence

**Files:**
- Create: `docs/superpowers/evidence/2026-07-01-fabric-runtime-target-support.md`
- Modify: `docs/superpowers/phase-index.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Run focused verification**

Run protocol tests, daemon tests, Bash syntax checks, distribution tests,
package build, discovery-only matrix planning, and whitespace checks.

- [x] **Step 2: Record evidence**

Record the exact commands and results in the evidence file, then index Phase
200.
