# Binding Operation Id Source Ownership Design

## Problem

Phase 84 isolated transitional bootstrap operation definitions into
`FabricBootstrapOperationDefinitions.kt`, but `FabricActionBindings.kt` still
duplicates a subset of those operation ids as string literals in private
execution bindings. That leaves two source files that can drift:

- graph bootstrap definitions own the operation id and schema;
- private execution bindings repeat the same operation id to attach adapter
  code.

Private bindings should attach to graph-defined operations, not own their own
operation-id list.

## Goals

- Make bootstrap operation ids in `FabricActionBindings.kt` reference
  constants from the bootstrap definition layer.
- Add guards that reject `operationId = "..."` literals in
  `FabricActionBindings.kt`.
- Keep protocol-level policy checks aligned with the new single-source shape.
- Preserve runtime graph, generated action descriptors, and invocation
  behavior.

## Non-Goals

- Do not add gameplay actions, route families, CLI commands, generated aliases,
  Fabric execution adapters, version lanes, or support claims.
- Do not remove private execution bindings yet.
- Do not claim the broader generated-discovery exit is complete.

## Acceptance Criteria

- `FabricActionBindings.kt` has no `operationId = "..."` literals.
- Binding operation ids resolve through internal bootstrap operation id
  constants.
- Protocol and Fabric tests still prove private bindings are transitional,
  descriptor-free, and represented in the runtime graph.
- Existing invocation and adapter behavior remains unchanged.
