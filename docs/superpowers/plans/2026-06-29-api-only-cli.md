# API-Only CLI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Replace generated route commands with one OpenAPI-backed `craftless api` command.

**Architecture:** Add a focused API invoker that matches REST endpoints against OpenAPI paths and builds request bodies from generic field flags. Remove `x-craftless-cli` emission and remove visible/dispatchable route shortcut commands from `CraftlessCli`.

**Tech Stack:** Kotlin/JVM, Ktor Client, kotlinx.serialization JSON, Clikt shell entrypoint tests, existing local Ktor test servers.

---

### Task 1: Pin Public CLI Shape

**Files:**
- Modify: `cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt`
- Modify: `protocol/src/test/kotlin/com/minekube/craftless/protocol/OpenApiGenerationTest.kt`

- [x] Update command registration and help tests so `registeredCommandPaths()` contains `api` and `daemon start`, and no longer contains `clients create`, `clients <id> run <action>`, `cache prepare`, or generated gameplay aliases.
- [x] Add a test that `craftless clients create ...` returns `unknown command`.
- [x] Replace the OpenAPI CLI metadata test with a test that serialized supervisor OpenAPI contains no `x-craftless-cli`.
- [x] Run `mise exec -- gradle :cli:test --tests com.minekube.craftless.cli.CraftlessCliTest` and `mise exec -- gradle :protocol:test --tests com.minekube.craftless.protocol.OpenApiGenerationTest`; expect failures proving production code still exposes the old shape.

### Task 2: Add `craftless api` Invocation

**Files:**
- Create: `cli/src/main/kotlin/com/minekube/craftless/cli/ApiCli.kt`
- Modify: `cli/src/main/kotlin/com/minekube/craftless/cli/Main.kt`
- Modify: `cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt`

- [x] Add failing tests for `api /version`, `api /clients -F id=bot -F version=latest-release -F loader=FABRIC`, and `api /clients/bot:run -F action=player.chat -F args[message]=hello`.
- [x] Implement endpoint parsing, method defaulting, Ktor request execution, response forwarding, typed `-F`, raw `-f`, and nested bracket field paths.
- [x] Validate enum fields against the matched OpenAPI schema when available.
- [x] Run the focused CLI tests; expect the new `api` tests to pass while old shortcut tests still fail until Task 3.

### Task 3: Remove Route Shortcut Dispatch

**Files:**
- Delete: `cli/src/main/kotlin/com/minekube/craftless/cli/GeneratedRouteCli.kt`
- Modify: `cli/src/main/kotlin/com/minekube/craftless/cli/Main.kt`
- Modify: `cli/src/test/kotlin/com/minekube/craftless/cli/CraftlessCliTest.kt`

- [x] Remove generated gameplay alias dispatch and `GeneratedRouteCli`.
- [x] Remove direct route dispatch for `clients`, `cache`, and `runtimes` from the visible command surface. Keep `daemon start` and the hidden `server start` compatibility alias.
- [x] Convert route-oriented CLI tests from old shortcuts to `craftless api` paths, or delete tests that only proved the removed alias grammar.
- [x] Run `mise exec -- gradle :cli:test`.

### Task 4: Remove CLI Extension Emission

**Files:**
- Modify: `protocol/src/main/kotlin/com/minekube/craftless/protocol/ApiRoute.kt`
- Modify: `protocol/src/main/kotlin/com/minekube/craftless/protocol/OpenApiDocument.kt`
- Modify: `protocol/src/test/kotlin/com/minekube/craftless/protocol/OpenApiGenerationTest.kt`

- [x] Remove `ApiRouteCli`, `ApiRouteCliBinding`, `OpenApiCliOperation`, `OpenApiCliBody`, and `OpenApiCliBinding`.
- [x] Stop passing CLI metadata from `ApiRouteCatalog.sessionDefaults()` and stop serializing `x-craftless-cli`.
- [x] Keep request schemas and descriptions intact so `craftless api --help` can infer fields.
- [x] Run `mise exec -- gradle :protocol:test`.

### Task 5: Evidence And Verification

**Files:**
- Add: `docs/superpowers/evidence/2026-06-29-api-only-cli.md`
- Modify: `docs/superpowers/phase-index.md`
- Modify: `docs/project-completion-checklist.md`

- [x] Record the final behavior and commands in the evidence file.
- [x] Add Phase 193 to the phase index and checklist evidence index.
- [x] Run `mise exec -- gradle :cli:test :protocol:test`.
- [x] Run `mise exec -- gradle :cli:ktlintCheck :protocol:ktlintCheck`.
- [x] Run `git diff --check`.
- [x] Commit, push `codex/api-only-cli`, and open a PR.
