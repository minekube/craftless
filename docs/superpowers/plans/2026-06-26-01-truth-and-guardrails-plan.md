# Truth And Guardrails Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reopen completion truthfully and make static gameplay drift fail review before implementation.

**Architecture:** This is a docs and policy phase. It updates root instructions, roadmap, and the active checklist so later code work is judged against the runtime graph and final gameplay gate.

**Tech Stack:** Markdown, `rg`, `git diff --check`, mise.

---

### Task 1: Reopen Completion Truth

**Files:**
- Modify: `docs/project-completion-checklist.md`
- Modify: `docs/roadmap.md`

- [ ] **Step 1: Replace false completion markers**

Set `Craftless is complete` to unchecked and add the final gate requiring Robin's Minecraft chat confirmation.

- [ ] **Step 2: Run marker scan**

Run: `rg -n "Craftless is complete|Robin.*Minecraft chat|runtime capability graph|SSE" docs/project-completion-checklist.md docs/roadmap.md`

Expected: output shows open completion gates, runtime graph gates, SSE gates, and Robin chat gate.

### Task 2: Harden Guardrails

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: Require spec and plan before each major phase**

Add a workflow rule that runtime graph, discovery, projection, invocation, SSE, adaptive consumer, and final gameplay phases require a spec and plan before code changes.

- [ ] **Step 2: Verify static action ban is explicit**

Run: `rg -n "hand-written public gameplay|descriptor/binding|work on the system" AGENTS.md`

Expected: output shows explicit guardrails against descriptor/binding growth.

### Task 3: Verify And Commit

**Files:**
- Verify: `AGENTS.md`
- Verify: `docs/project-completion-checklist.md`
- Verify: `docs/roadmap.md`

- [ ] **Step 1: Run docs verification**

Run: `git diff --check`

Expected: exit 0.

- [ ] **Step 2: Commit**

Run: `git add AGENTS.md docs/project-completion-checklist.md docs/roadmap.md docs/superpowers/specs docs/superpowers/plans && git commit -m "docs: define real product completion path"`

Expected: commit created.
