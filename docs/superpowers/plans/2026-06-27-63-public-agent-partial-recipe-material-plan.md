# Public Agent Partial Recipe Material Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the external public-agent runner continue to generic recipe composition after proving at least one material item, even if later collection navigation fails.

**Architecture:** Keep the product API unchanged. Adjust only the public-agent acceptance runner policy so recipe-capable clients can compose from partial public inventory evidence.

**Tech Stack:** Kotlin/JVM, Ktor testkit HTTP fixtures, Gradle via mise.

---

### Task 1: Partial Material Recipe Policy

**Files:**
- Modify: `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`
- Test: `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [x] **Step 1: Add regression test**

Add a test where:

```kotlin
actions = completeActionCatalog() + listOf("recipe.query", "recipe.craft")
```

The fixture returns one verified log, then later navigation failures. Assert
the runner returns `PublicAgentGameplayState.RAN`, invokes `recipe.query` and
`recipe.craft`, and sends no scenario shortcut request bodies.

- [x] **Step 2: Implement the policy**

Compute whether generic recipe composition is available. If material
collection reports a blocker after at least one log has already been observed
and recipe composition is available, break out of collection and continue to
the existing recipe query/craft path. Otherwise keep the existing blocker.

- [x] **Step 3: Verify**

Run:

```bash
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest' --rerun-tasks
```

Expected: PASS.

### Task 2: Guardrails

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/project-completion-checklist.md`

- [x] **Step 1: Register Phase 63**

Document that this is external public-agent policy only and must not add
product gameplay actions or scenario shortcuts.

- [x] **Step 2: Verify docs**

Run:

```bash
git diff --check
```

Expected: no whitespace errors.
