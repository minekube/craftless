# SSE, JSON-RPC, And Adaptive Consumers Design

**Goal:** Add live server-to-client event streaming and adaptive consumers that use graph-projected metadata at runtime.

**Architecture:** Craftless uses HTTP POST JSON-RPC-style requests for control and Server-Sent Events for one-way live event delivery. The daemon owns an event bus fed by lifecycle changes, graph changes, invocation results, driver events, and Fabric probes. CLI and helper clients use OpenAPI plus SSE metadata instead of static command catalogs.

**Transport Shape:**
- `GET /clients/{id}/events:stream`: SSE stream for one client.
- `GET /events:stream`: SSE stream for supervisor events.
- `POST /clients/{id}:rpc`: JSON-RPC-style control for subscribe, unsubscribe, query, and invoke.
- Existing `POST /clients/{id}:run` remains as a simple invocation convenience.

**Event Rules:**
- Event names and payload schemas come from the runtime capability graph.
- Server-side filters reduce event volume by type/resource/operation/correlation id.
- Client-side filters support local agent workflows.
- SSE messages include event id, type, client id, optional resource id, optional correlation id, payload, and timestamp.

**Consumer Rules:**
- CLI static commands remain lifecycle/discovery/control only.
- Gameplay help, invocation, event watching, and tool exports use per-client OpenAPI and stream metadata.
- Playwright helper supports invoke, resource discovery, and event streaming through the same metadata.

**Completion Gate:**
- Daemon tests prove SSE stream delivery, filtering, and JSON-RPC correlation.
- CLI tests prove live event watching and JSON-RPC control.
- Bun helper tests prove event subscription without npm/node tooling.
