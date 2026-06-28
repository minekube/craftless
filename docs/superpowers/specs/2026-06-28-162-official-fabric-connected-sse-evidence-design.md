# Official Fabric Connected SSE Evidence Design

## Goal

Make the latest/current official Fabric attach probe capture public
`/clients/{id}/events:stream` SSE evidence from a real connected official client
without adding gameplay actions, action adapters, or static catalogs.

## Problem

The official 26.x lane now proves launch, attach, connected client-state,
server-feature metadata, registry metadata, and event-source metadata. Phase
161 also makes the generated OpenAPI advertise shared event nodes as available.
The connected official probe still records only the snapshot-style
`/events` JSON endpoint and generated OpenAPI artifacts. It does not write a
public client SSE artifact, so the latest/current lane still lacks direct
Codex-verifiable evidence that the generic daemon SSE route carries lifecycle
events for the attached official client.

## Design

- Reuse the existing daemon `GET /clients/{id}/events:stream` route.
- Extend the official attach probe to fetch that route after attach/connect.
- Write the raw SSE body to `client-events-stream.sse`.
- Parse the `event:` lines into `streamedEventTypes` in `probe-result.json` for
  machine-readable evidence.
- Keep the probe as evidence infrastructure only. Do not add driver events,
  gameplay actions, action descriptors, CLI commands, route families, scenario
  shortcuts, or official-lane invocation adapters.

## Boundaries

- No static gameplay actions.
- No copied Yarn/remap gameplay gateway.
- No new product SSE route or transport stack.
- No official driver packaging/support claim.
- No final latest/current gameplay support claim.

## Acceptance

- A red evidence check fails before implementation because the connected
  official probe does not write `client-events-stream.sse`.
- The connected official attach probe writes `client-events-stream.sse`.
- The SSE artifact includes `event: client.attached` and
  `event: client.connected`.
- `probe-result.json` includes `streamedEventTypes` containing
  `client.attached` and `client.connected`.
- The generated connected OpenAPI still reports `actions=0`.
- Focused official tests, latest official lane check, local CI, and
  `git diff --check` pass through mise.
