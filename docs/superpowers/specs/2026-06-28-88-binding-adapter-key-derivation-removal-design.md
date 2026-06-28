# Binding Adapter Key Derivation Removal Design

## Problem

Phase 86 centralized bootstrap adapter keys, but `FabricDriverBackend` still
registers private binding adapters by deriving keys from operation ids:

```kotlin
binding.operationId.fabricOperationAdapterKey()
```

with a helper that turns `player.chat` into `fabric.player-chat`. That
convention is another hidden operation-to-adapter catalog in backend code. The
bootstrap definition layer already owns both the operation id and adapter key,
so backend registration should consume that source directly.

## Goals

- Remove backend adapter-key derivation from private binding registration.
- Register private binding adapters by looking up the binding operation id in
  bootstrap operation definitions.
- Add a source guard that fails if backend code reintroduces
  `fabricOperationAdapterKey` or the `replace(".", "-")` adapter derivation.
- Keep operation ids, adapter keys, generated OpenAPI, and invocation behavior
  unchanged.

## Non-Goals

- Do not add gameplay actions, route families, CLI commands, generated aliases,
  Fabric execution adapters, version lanes, or support claims.
- Do not remove private bootstrap execution adapters in this phase.
- Do not change navigation or task adapter ownership in this phase.
- Do not claim the broader generated-discovery exit is complete.

## Acceptance Criteria

- `FabricDriverBackend.kt` no longer contains `fabricOperationAdapterKey` or
  the `replace(".", "-")` adapter-key derivation.
- Private binding adapter registration uses adapter keys from
  `fabricBootstrapOperationDefinitions()`.
- Existing graph-owned invocation tests still prove graph operations dispatch to
  the correct private adapters.
- AGENTS, checklist, plan, and evidence record Phase 88 and keep the broader
  generated-discovery blocker active.
