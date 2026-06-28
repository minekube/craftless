# Fabric Binding Descriptor Removal Design

## Problem

Phase 77 through Phase 80 moved public Fabric action descriptors, schemas, and
legacy invocation dispatch onto the runtime capability graph, but
`FabricActionBindings.kt` still carries `DriverActionDescriptor` metadata and
descriptor helper functions. `FabricDriverBackend.operationAdapters(...)` also
derives private adapter keys from `binding.descriptor.id`.

That preserves an old descriptor-shaped catalog inside the private execution
binding layer. Even though public `actions()` already projects graph
operations, future agents could keep adding descriptor/binding pairs in this
file and accidentally treat the private adapter layer as the source of truth.

## Goals

- Make `FabricActionBinding` a private execution adapter only.
- Replace `descriptor: DriverActionDescriptor` with a minimal `operationId`
  used only to attach private execution code to graph-discovered operations.
- Remove `DriverActionDescriptor`, `DriverActionArgument`,
  `DriverActionResultDescriptor`, and `DriverActionResultProperty` usage from
  `FabricActionBindings.kt`.
- Remove descriptor helper functions from `FabricActionBindings.kt`.
- Keep schemas, availability, resource ownership, and public descriptor
  projection in `FabricCapabilityProbe.kt` and `FabricDriverBackend.kt`.
- Keep existing Fabric behavior and generated public API output unchanged.

## Non-Goals

- Do not add new gameplay actions, route families, CLI commands, generated
  aliases, Fabric bindings, version lanes, or support claims.
- Do not remove private Fabric execution adapters yet; they are still needed
  for current executable graph operations.
- Do not solve generic future runtime discovery in this phase. This phase only
  removes descriptor/schema ownership from the private binding layer.

## Acceptance Criteria

- `FabricActionBinding` exposes `operationId: String` and no
  `DriverActionDescriptor`.
- `FabricActionBindings.kt` contains no `DriverActionDescriptor`,
  `DriverActionArgument`, `DriverActionResultDescriptor`, or
  `DriverActionResultProperty` imports/usages.
- `FabricActionBindings.kt` contains no descriptor helper functions such as
  `fabricPlayerQueryDescriptor`.
- `FabricDriverBackend.operationAdapters(...)` registers private adapters from
  `operationId`, not `descriptor.id`.
- Existing graph-projected action schemas and adapter invocation tests keep
  passing.
- Checklist, AGENTS, plan, and evidence record the phase and keep the broader
  binding-exit blocker active.
