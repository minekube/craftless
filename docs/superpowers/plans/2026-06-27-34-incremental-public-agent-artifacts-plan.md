# Incremental Public-Agent Artifacts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use
> superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make long-running generated public-agent actions observable while
they are in progress, so final gameplay blockers leave useful evidence even
when a generated action stalls or times out.

**Architecture:** Keep the change in `testkit` public-agent evidence plumbing.
The runner still drives only generated public actions through
`POST /clients/{id}:run`; it does not add product action ids, scenario
shortcuts, or pathfinder-specific public API.

**Tech Stack:** Kotlin/JVM, Ktor Client MockEngine tests,
kotlinx.serialization JSON, Gradle through mise.

---

### Task 1: RED Artifact Test

**Files:**
- Modify:
  `testkit/src/test/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunnerTest.kt`

- [x] **Step 1: Extend action request failure artifact test**

Assert that `public-agent-gameplay-results.jsonl` includes
`public-agent-action-started` for the generated action before the failed action
response and blocker.

- [x] **Step 2: Run the RED test**

Run:

```sh
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest.runner records blocked artifacts when generated action request fails'
```

Expected before implementation: FAIL because gameplay artifacts are written
only after the runner exits and contain no in-progress marker.

### Task 2: Incremental Artifact Writes

**Files:**
- Modify:
  `testkit/src/main/kotlin/com/minekube/craftless/testkit/PublicAgentGameplayRunner.kt`

- [x] **Step 1: Initialize artifacts after discovery**

Write state discovery lines and truncate the gameplay artifact immediately
after the runner fetches public specs, actions, and the event stream.

- [x] **Step 2: Append action lifecycle events**

Before each generated POST, append `public-agent-action-started`. After a
response or request failure, append `public-agent-action` with the serialized
response.

- [x] **Step 3: Append blockers without rewriting action evidence**

When the runner blocks after initialization, append `public-agent-blocked`
instead of truncating and rewriting gameplay evidence.

### Task 3: Verification And Final Gameplay

**Files:**
- Modify: `docs/project-completion-checklist.md`
- Modify: `AGENTS.md`

- [x] **Step 1: Run focused and module tests**

Run:

```sh
mise exec -- gradle :testkit:test --tests 'com.minekube.craftless.testkit.PublicAgentGameplayRunnerTest.runner records blocked artifacts when generated action request fails'
mise exec -- gradle :testkit:test
```

Expected after implementation: PASS.

- [ ] **Step 2: Run broader gates**

Run:

```sh
mise run lint
mise run jvm-test
```

Expected: all pass before pushing.

- [ ] **Step 3: Re-run final gameplay with bounded public-agent timeout**

Run final gameplay with an explicit
`CRAFTLESS_PUBLIC_AGENT_ACTION_REQUEST_TIMEOUT_MS` small enough to produce a
timely generated-action blocker if `navigation.follow` stalls. Expected:
incremental artifacts identify the exact in-flight action, and final gameplay
either reaches `publicAgentState=RAN` or records the next precise generic
public-evidence/action blocker.
