# README Public Entrypoint Overhaul Design

## Problem

The README has stayed accurate through recent evidence phases, but it reads as
an accumulated implementation/status note. External users and agents need a
clean public entrypoint that quickly explains what Craftless is, how to install
and run it, how to discover and invoke the generated API, what is verified
today, and what is still open.

Phase 81 also made the HMC bridge lifecycle-only, so the README should make the
durable path explicit: Fabric runtime graph, generated per-client OpenAPI,
generic invocation, SSE, adaptive CLI, and no bridge gameplay adapter.

## Goals

- Reframe README around public usability: install, Docker, GitHub Actions,
  generated API workflow, status, roadmap, and development.
- Keep the Browserless-style positioning concise and early.
- Add a status-at-a-glance section that separates verified surfaces, explicit
  unsupported version lanes, and open completion work.
- Document HMC bridge code as lifecycle/launch evidence only, not a gameplay
  product path.
- Preserve install/Docker/GitHub Action quickstart strings covered by existing
  distribution tests.
- Add a README guard test for the Phase 81 bridge lifecycle-only rule and
  generated-API-first language.

## Non-Goals

- Do not add product behavior, route families, CLI commands, SDKs, generated
  clients, Fabric bindings, or gameplay actions.
- Do not claim latest `26.2` or representative older `1.20.6` client support.
  They remain explicit unsupported lanes until real support lands.
- Do not reintroduce TypeScript SDK, Homebrew, HeadlessMC/HMC-Specifics command
  names, or legacy survival task setup as active product surfaces.

## Acceptance Criteria

- README opens with a clear product description, install path, generated API
  model, current status, comparison, roadmap, and development commands.
- README states that gameplay breadth comes from the runtime capability graph
  and generated per-client OpenAPI.
- README states that bridge code is lifecycle/launch evidence only and not a
  gameplay adapter.
- README keeps “without server-provisioned inventory” final gameplay evidence
  language.
- Bun distribution tests pass, including the new README guard.
- `git diff --check`, `mise exec -- bun test playwright`, and `mise run ci`
  pass before push.
