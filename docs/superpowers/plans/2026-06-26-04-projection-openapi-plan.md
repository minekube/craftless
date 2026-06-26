# Projection And OpenAPI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate per-client OpenAPI, actions, resources, aliases, stream metadata, and fingerprints from the runtime capability graph.

**Architecture:** Protocol projection owns graph-to-OpenAPI conversion. Daemon serves graph-projected OpenAPI for each client. Projection endpoints remain views of that same document.

**Tech Stack:** Kotlin/JVM, kotlinx.serialization, Ktor Server, Gradle, mise.

---

### Task 1: Graph Projection

**Files:**
- Modify: `protocol/src/main/kotlin/com/minekube/craftless/protocol/OpenApiDocument.kt`
- Test: `protocol/src/test/kotlin/com/minekube/craftless/protocol/OpenApiDocumentTest.kt`

- [ ] **Step 1: Add failing graph projection tests**

Assert graph operations produce `x-craftless-actions`, resources produce `x-craftless-resources`, event nodes produce stream metadata, and aliases are generated from graph operations.

- [ ] **Step 2: Implement projection entrypoint**

Add `OpenApiDocument.fromRuntimeGraph(...)` while keeping current route catalog support.

- [ ] **Step 3: Verify**

Run: `mise exec -- gradle :protocol:test`

Expected: pass.

### Task 2: Daemon Uses Graph

**Files:**
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/ClientSessionService.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/LocalSessionApiServer.kt`
- Test: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`

- [ ] **Step 1: Add failing daemon tests**

Assert `/clients/{id}/openapi.json`, `/clients/{id}/actions`, and `/clients/{id}/resources` all derive from the same runtime graph fingerprint.

- [ ] **Step 2: Implement graph-backed service path**

Use driver graph snapshots when available, and isolate bootstrap action fallback as transitional.

- [ ] **Step 3: Verify**

Run: `mise exec -- gradle :daemon:test :protocol:test`

Expected: pass.
