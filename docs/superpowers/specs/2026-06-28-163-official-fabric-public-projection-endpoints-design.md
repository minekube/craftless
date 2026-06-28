# Official Fabric Public Projection Endpoints Design

## Goal

Make the latest/current official Fabric attach probe capture public
`/clients/{id}/actions` and `/clients/{id}/resources` evidence from a real
connected official client, proving the generated OpenAPI graph projections are
available through public API endpoints without adding gameplay actions.

## Problem

The official 26.x lane now records connected OpenAPI, runtime resources,
registry/event/client-state metadata, and public SSE lifecycle evidence. The
probe still does not write the public projection endpoint bodies for
`/clients/{id}/actions` and `/clients/{id}/resources`. That leaves the
latest/current lane with generated OpenAPI evidence, but weaker evidence that
adaptive consumers can use the same projection endpoints that CLI and agents
depend on.

## Design

- Reuse the existing daemon public projection endpoints:
  - `GET /clients/{id}/actions`
  - `GET /clients/{id}/resources`
- Extend only the official attach probe evidence harness.
- Write raw endpoint bodies to `client-actions.json` and
  `client-resources.json`.
- Record `publicActionCount` and `publicResourceIds` in `probe-result.json`.
- Preserve `actions=0`: this phase proves public projection plumbing for the
  official lane, not official gameplay breadth.

## Boundaries

- No static gameplay actions.
- No action descriptors, operation adapters, invocation adapters, CLI gameplay
  commands, route families, or scenario shortcuts.
- No copied Yarn/remap gameplay gateway.
- No new product endpoint; use the existing daemon routes.
- No official driver packaging/support claim.
- No final latest/current gameplay support claim.

## Acceptance

- A red artifact check fails before implementation because the connected
  official probe does not write `client-actions.json` or
  `client-resources.json`.
- The connected official attach probe writes both artifacts.
- `client-actions.json` is an empty JSON array.
- `client-resources.json` contains the connected official projected resources,
  including `runtime`, `registry`, `event`, `client`, `player`, `inventory`,
  `world`, and `entity`.
- `probe-result.json` includes `publicActionCount=0` and public resource ids.
- Focused official tests, latest official lane check, local CI, and
  `git diff --check` pass through mise.
