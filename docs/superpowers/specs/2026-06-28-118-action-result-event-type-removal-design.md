# Action Result Event Type Removal Design

## Problem

`DriverActionResult` still carries `eventType: DriverEventType?`. Fabric
bindings and backend tests use it to classify generated action results as
static event kinds such as `CHAT` and `MOVEMENT`.

That is stale design. Action invocation events already have the action id as
`operationId`, and live event projection can derive the public event type from
that operation id. Result DTOs should report result data, not classify
gameplay into a static enum.

## Goals

- Remove `eventType` from `DriverActionResult`.
- Make daemon `SessionEvent` creation for accepted action results use the
  explicit `operationId`.
- Stop runtime backend sessions from recording accepted action events from
  static result metadata.
- Preserve error event recording for rejected or failed backend actions.
- Keep existing SSE and JSON-RPC action events working through operation ids.

## Non-Goals

- Do not remove lifecycle/system `DriverEventType` values in this phase.
- Do not add a replacement action-event enum.
- Do not add gameplay actions, static route families, CLI gameplay catalogs,
  Fabric bindings, scenario shortcuts, version-specific APIs, or support
  claims.

## Acceptance Criteria

- A focused driver API contract test fails before implementation because
  `DriverActionResult` still exposes `eventType`.
- After implementation, no production or test code passes
  `eventType = DriverEventType.CHAT` or `eventType = DriverEventType.MOVEMENT`
  into `DriverActionResult`.
- Accepted action `SessionEvent` values use the invoked `operationId` as their
  type.
- Backend driver sessions no longer synthesize accepted driver events from
  action result metadata; failed/rejected actions still record `ERROR` events.
- AGENTS/checklist/evidence record Phase 118 and keep generated gameplay
  breadth owned by runtime graph operation ids.
