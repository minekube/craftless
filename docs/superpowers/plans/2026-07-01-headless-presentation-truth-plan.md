# Headless Presentation Truth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `presentation.window = NONE` truthful by rejecting launches when no windowless strategy is available.

**Architecture:** Keep the current Craftless virtual-display wrapper strategy. Validate the strategy before instance materialization and process start, then surface the failure through the existing bad-request path.

**Tech Stack:** Kotlin/JVM, Ktor server tests, JUnit 5, Gradle through `mise`.

---

### Task 1: Launcher Fail-Closed Behavior

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`
- Modify: `daemon/src/main/kotlin/com/minekube/craftless/daemon/WorkspaceClientRuntimeDriverFactory.kt`

- [x] **Step 1: Write the failing test**

Add a test named `process client runtime launcher rejects windowless request when no windowless wrapper is available`. It creates a prepared launch with a fake Java executable and a Fabric mod handle, calls `ProcessClientRuntimeLauncher(windowlessCommandPrefix = emptyList()).launch(...)`, and asserts `IllegalArgumentException` contains `windowless presentation requires`.

- [x] **Step 2: Run test to verify it fails**

Run:

```sh
mise exec -- gradle :daemon:test --tests 'com.minekube.craftless.daemon.LocalSessionApiServerTest.process client runtime launcher rejects windowless request when no windowless wrapper is available'
```

Expected before implementation: FAIL because the launcher starts the fake Java command.

- [x] **Step 3: Implement the minimal fix**

Move command resolution and presentation validation before launch mod/options materialization. In `withPresentationWindow`, require a non-empty wrapper for `ClientWindowMode.NONE` and tell callers to configure `CRAFTLESS_WINDOWLESS_WRAPPER` or use `presentation.window=VISIBLE`.

- [x] **Step 4: Run test to verify it passes**

Run the same focused Gradle command. Expected after implementation: PASS.

### Task 2: HTTP And Documentation Surface

**Files:**
- Modify: `daemon/src/test/kotlin/com/minekube/craftless/daemon/LocalSessionApiServerTest.kt`
- Modify: `README.md`
- Modify: `docs-site/content/docs/cli.mdx`
- Modify: `docs/superpowers/phase-index.md`
- Modify: `docs/superpowers/evidence/2026-06-29-windowless-muted-defaults.md`
- Modify: `docs/project-completion-checklist.md`
- Create: `docs/superpowers/evidence/2026-07-01-headless-presentation-truth.md`

- [x] **Step 1: Add an HTTP regression**

Add `server rejects default windowless client creation when no windowless wrapper is available`. Use `LocalSessionApiServer.inMemory` with a workspace and `ProcessClientRuntimeLauncher(windowlessCommandPrefix = emptyList())`; assert `POST /clients` returns `400`, the body mentions `windowless presentation requires`, and no muted options file was written.

- [x] **Step 2: Update docs**

Replace stale wording that said hosts without wrappers launch directly. Document that `window=NONE` requires a real virtual-display strategy and otherwise fails closed.

- [x] **Step 3: Verify focused behavior**

Run the two new focused daemon tests and then the broader daemon test suite.

- [x] **Step 4: Commit and push**

After `git diff --check` and verification pass, commit the code/docs and push `main`.
