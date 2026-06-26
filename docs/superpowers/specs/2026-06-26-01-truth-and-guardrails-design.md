# Truth And Guardrails Design

**Goal:** Reopen Craftless completion truthfully and make repo instructions prevent static gameplay-action drift.

**Architecture:** This phase changes product governance, not gameplay behavior. `AGENTS.md`, `docs/roadmap.md`, and `docs/project-completion-checklist.md` become the active guardrails for every later phase. Existing hand-written Fabric gameplay bindings are classified as transitional bootstrap evidence until replaced by graph-backed discovery and invocation.

**Scope:**
- Mark Craftless as incomplete until the full runtime graph, SSE live stream, generic invocation, adaptive consumers, and final multiplayer gameplay gate are complete.
- Require every major phase to have a Superpowers spec and plan before implementation.
- Make "work on the system, not in the system" enforceable by docs and architecture checks.
- Preserve existing verified baseline work as useful scaffolding.

**Non-Goals:**
- Do not remove existing bootstrap actions in this phase.
- Do not add new gameplay actions.
- Do not mark the project complete from CI, smoke, or docs alone.

**Completion Gate:**
- Checklist has no false `Craftless is complete` claim.
- Roadmap points to the seven-phase product completion sequence.
- AGENTS requires specs/plans and forbids public gameplay growth by descriptor/binding pairs.
- `git diff --check` passes and changes are pushed to `main`.
