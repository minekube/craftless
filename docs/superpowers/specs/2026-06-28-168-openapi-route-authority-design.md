# OpenAPI Route Authority Design

## Problem

`ClientSessionService.openApiFor(clientId)` can already use the runtime
capability graph as the source of truth when a driver exposes projection nodes.
`ClientSessionService.routesFor(clientId)` still asks the driver for a separate
sorted action list, which leaves another public projection path that can drift
from the generated per-client OpenAPI.

That violates the active contract: descriptor projections are convenience
views of generated OpenAPI, not an independent source of truth.

## Design

`routesFor(clientId)` must derive its route list from the same generated
per-client `OpenApiDocument` returned by `openApiFor(clientId)`.

For graph-backed clients, this means route aliases come from
`OpenApiDocument.fromRuntimeGraph(...)` and do not require `DriverSession.actions()`.
For legacy action-list fallback clients, `openApiFor(clientId)` may still build
one catalog from one already-captured action snapshot, and `routesFor(clientId)`
then projects routes from that generated document.

## Non-Goals

- Do not add gameplay actions.
- Do not change HTTP route shapes.
- Do not remove the transitional action-list fallback in this slice.
- Do not change invocation behavior.

## Acceptance

- A graph-backed session whose `actions()` method is unavailable can still
  expose action alias routes through `routesFor(clientId)`.
- `routesFor(clientId)` and `openApiFor(clientId).paths` agree on the route
  surface for the client.
- Existing daemon and protocol tests continue to pass.
